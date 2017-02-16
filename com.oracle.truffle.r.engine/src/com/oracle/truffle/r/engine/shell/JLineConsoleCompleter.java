/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.engine.shell;

import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.r.nodes.function.PromiseHelperNode;
import com.oracle.truffle.r.runtime.RCaller;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.context.ConsoleHandler;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import jline.console.completer.Completer;

public class JLineConsoleCompleter implements Completer {
    private final ConsoleHandler console;

    private static boolean isTesting = false;

    public static void testingMode() {
        isTesting = true;
    }

    public JLineConsoleCompleter(ConsoleHandler console) {
        this.console = console;
    }

    @Override
    public int complete(String buffer, int cursor, List<CharSequence> candidates) {
        try {
            return completeImpl(buffer, cursor, candidates);
        } catch (Throwable e) {
            if (isTesting) {
                throw e;
            }
            RInternalError.reportErrorAndConsoleLog(e, console, 0);
        }
        return cursor;
    }

    private int completeImpl(String buffer, int cursor, List<CharSequence> candidates) {
        if (buffer.isEmpty()) {
            return cursor;
        }

        REnvironment utils = REnvironment.getRegisteredNamespace("utils");
        Object o = utils.get(".completeToken");
        if (o instanceof RPromise) {
            o = PromiseHelperNode.evaluateSlowPath(null, (RPromise) o);
        }
        RFunction completeToken;
        if (o instanceof RFunction) {
            completeToken = (RFunction) o;
        } else {
            return cursor;
        }

        o = utils.get(".CompletionEnv");
        if (!(o instanceof RPromise)) {
            return cursor;
        }
        REnvironment env = (REnvironment) PromiseHelperNode.evaluateSlowPath(null, (RPromise) o);
        int start = getStart(buffer, env, cursor);
        env.safePut("start", start);
        env.safePut("end", cursor);
        env.safePut("linebuffer", buffer);
        env.safePut("token", buffer.substring(start, cursor));

        MaterializedFrame callingFrame = REnvironment.globalEnv().getFrame();
        RContext.getEngine().evalFunction(completeToken, callingFrame, RCaller.createInvalid(callingFrame), null, new Object[]{});

        o = env.get("comps");
        if (!(o instanceof RAbstractStringVector)) {
            return cursor;
        }

        RAbstractStringVector comps = (RAbstractStringVector) o;
        List<String> ret = new ArrayList<>(comps.getLength());
        for (int i = 0; i < comps.getLength(); i++) {
            ret.add(comps.getDataAt(i));
        }
        Collections.sort(ret, String.CASE_INSENSITIVE_ORDER);
        candidates.addAll(ret);
        return start;
    }

    private int getStart(String buffer, REnvironment env, int cursor) {
        int start = 0;
        Object o = env.get("options");
        if (o instanceof RList) {
            RList opt = (RList) o;
            start = lastIdxOf(buffer, opt, "funarg.suffix", start, cursor);
            start = lastIdxOf(buffer, opt, "function.suffix", start, cursor);
        }
        start = lastIdxOf(buffer, "\"", start, cursor);
        start = lastIdxOf(buffer, "'", start, cursor);
        return start;
    }

    private int lastIdxOf(String buffer, RList opt, String key, int start, int cursor) {
        int optIdx = opt.getElementIndexByName(key);
        if (optIdx > -1) {
            Object o = opt.getDataAt(optIdx);
            if (o instanceof RAbstractStringVector) {
                RAbstractStringVector v = (RAbstractStringVector) o;
                return lastIdxOf(buffer, v.getLength() > 0 ? v.getDataAt(0) : null, start, cursor);
            }
        }
        return start;
    }

    private int lastIdxOf(String buffer, String subs, int start, int cursor) {
        if (null != subs && !subs.isEmpty()) {
            int idx = buffer.lastIndexOf(subs, cursor);
            if (idx == cursor) {
                idx = buffer.lastIndexOf(subs, cursor - 1);
            }
            if (idx > -1) {
                idx += subs.length();
                return idx > start ? idx : start;
            }
        }
        return start;
    }
}
