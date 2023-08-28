/*
 * Copyright (c) 2013, 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.runtime.context;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleContext;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.r.runtime.ExitException;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.context.Engine.ParseException;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;

/**
 * A thread for performing an evaluation (used by {@code .fastr} builtins).
 */
public final class EvalThread extends Thread {

    private final Source source;
    private final ChildContextInfo info;
    private final TruffleContext truffleContext;
    private RList evalResult;
    private final Semaphore init = new Semaphore(0);

    private final Map<Integer, Thread> threadMap;

    /** This table is required to create several bunches of child contexts. */
    public static final Map<Integer, Integer> idToMultiSlotTable = new ConcurrentHashMap<>();

    /** We use a separate counter for threads since ConcurrentHashMap.size() is not reliable. */
    public static final AtomicInteger threadCnt = new AtomicInteger(0);

    public EvalThread(Map<Integer, Thread> threadMap, ChildContextInfo info, Source source) {
        this.threadMap = threadMap;
        this.info = info;
        this.source = source;
        threadCnt.incrementAndGet();
        threadMap.put(info.getId(), this);
        idToMultiSlotTable.put(info.getId(), info.getMultiSlotInd());
        this.truffleContext = info.createTruffleContext();
    }

    @Override
    public void run() {
        init.release();
        try {
            evalResult = run(truffleContext, info, source);
        } finally {
            threadMap.remove(info.getId());
            threadCnt.decrementAndGet();
        }
    }

    /*
     * Parent context uses this method to wait for initialization of the child to complete to
     * prevent potential updates to runtime's meta data from interfering with program's execution.
     */
    public void waitForInit() {
        try {
            init.acquire();
        } catch (InterruptedException x) {
            throw new RInternalError(x, "error waiting to initialize eval thread");
        }
    }

    /**
     * Convenience method for {@code .fastr.context.eval} in same thread.
     */
    public static RList run(TruffleContext truffleContext, ChildContextInfo info, Source source) {
        RList result = null;
        Object parent = null;
        try {
            parent = truffleContext.enter(null);
            // this is the engine for the new child context
            Engine rEngine = RContext.getEngine();
            // Object eval = rEngine.eval(rEngine.parse(source), rEngine.getGlobalFrame());
            Object evalResult = rEngine.parseAndEval(source, rEngine.getGlobalFrame(), false);
            result = RDataFactory.createList(new Object[]{evalResult == null ? RNull.instance : evalResult});
        } catch (ParseException e) {
            e.report(info.getStdout());
            result = createErrorResult(e.getMessage());
        } catch (ExitException e) {
            // termination, treat this as "success"
            result = RDataFactory.createList(new Object[]{e.getStatus()});
        } catch (RError e) {
            // nothing to do
            result = RDataFactory.createList(new Object[]{RNull.instance});
        } catch (Throwable t) {
            // some internal error
            RInternalError.reportErrorAndConsoleLog(t, info.getId());
            result = createErrorResult(t.getClass().getSimpleName());
        } finally {
            truffleContext.leave(null, parent);
            truffleContext.close();
        }
        return result;
    }

    @TruffleBoundary
    public static RList createErrorResult(String errorMsg) {
        RList list = RDataFactory.createList(new Object[]{RRuntime.LOGICAL_NA});
        list.setAttr("error", errorMsg);
        return list;

    }

    public RList getEvalResult() {
        return evalResult;
    }

    public ChildContextInfo getContextInfo() {
        return info;
    }
}
