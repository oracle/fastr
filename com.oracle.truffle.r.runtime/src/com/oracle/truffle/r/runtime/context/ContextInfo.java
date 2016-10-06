/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.context;

import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;

import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.vm.PolyglotEngine;
import com.oracle.truffle.r.runtime.RStartParams;
import com.oracle.truffle.r.runtime.context.RContext.ContextKind;

/**
 * Represents custom initialization state for an R instance.
 *
 * Use {@link #createVM()} to apply this information to a newly-built {@link PolyglotEngine}
 * instance (it will be stored in the "fastrContextInfo" global symbol).
 */
public final class ContextInfo implements TruffleObject {
    public static final String GLOBAL_SYMBOL = "fastrContextInfo";

    private static final AtomicInteger contextInfoIds = new AtomicInteger();

    private final RStartParams startParams;
    private final String[] env;
    private final RContext.ContextKind kind;
    private final TimeZone systemTimeZone;

    /**
     * Any context created by another has a parent. When such a context is destroyed we must reset
     * the RContext.threadLocalContext to the parent.
     */
    private final RContext parent;
    private final ConsoleHandler consoleHandler;
    private final int id;
    private PolyglotEngine vm;

    private ContextInfo(RStartParams startParams, String[] env, ContextKind kind, RContext parent, ConsoleHandler consoleHandler, TimeZone systemTimeZone, int id) {
        this.startParams = startParams;
        this.env = env;
        this.kind = kind;
        this.parent = parent;
        this.consoleHandler = consoleHandler;
        this.systemTimeZone = systemTimeZone;
        this.id = id;
    }

    public PolyglotEngine createVM() {
        PolyglotEngine newVM = PolyglotEngine.newBuilder().globalSymbol(GLOBAL_SYMBOL, this).build();
        this.vm = newVM;
        return newVM;
    }

    public PolyglotEngine createVM(PolyglotEngine.Builder builder) {
        PolyglotEngine newVM = builder.globalSymbol(GLOBAL_SYMBOL, this).build();
        this.vm = newVM;
        return newVM;
    }

    /**
     * Create a context configuration object.
     *
     * @param startParams the start parameters passed this R session
     * @param env TODO
     * @param kind defines the degree to which this context shares base and package environments
     *            with its parent
     * @param parent if non-null {@code null}, the parent creating the context
     * @param consoleHandler a {@link ConsoleHandler} for output
     * @param systemTimeZone the system's time zone
     */
    public static ContextInfo create(RStartParams startParams, String[] env, ContextKind kind, RContext parent, ConsoleHandler consoleHandler, TimeZone systemTimeZone) {
        int id = contextInfoIds.incrementAndGet();
        return new ContextInfo(startParams, env, kind, parent, consoleHandler, systemTimeZone, id);
    }

    public static ContextInfo create(RStartParams startParams, String[] env, ContextKind kind, RContext parent, ConsoleHandler consoleHandler) {
        return create(startParams, env, kind, parent, consoleHandler, TimeZone.getDefault());
    }

    public static ContextInfo getContextInfo(PolyglotEngine vm) {
        return (ContextInfo) vm.findGlobalSymbol(ContextInfo.GLOBAL_SYMBOL).get();
    }

    public RStartParams getStartParams() {
        return startParams;
    }

    public String[] getEnv() {
        return env;
    }

    public ContextKind getKind() {
        return kind;
    }

    public RContext getParent() {
        return parent;
    }

    public ConsoleHandler getConsoleHandler() {
        return consoleHandler;
    }

    public TimeZone getSystemTimeZone() {
        return systemTimeZone;
    }

    public int getId() {
        return id;
    }

    public PolyglotEngine getVM() {
        return vm;
    }

    @Override
    public ForeignAccess getForeignAccess() {
        throw new IllegalStateException("cannot access " + ContextInfo.class.getSimpleName() + " via Truffle");
    }
}
