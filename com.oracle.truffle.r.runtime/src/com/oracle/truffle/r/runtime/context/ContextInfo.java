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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.vm.PolyglotEngine;
import com.oracle.truffle.r.runtime.RCmdOptions;
import com.oracle.truffle.r.runtime.context.RContext.ContextKind;

/**
 * Represents custom initialization state for an R instance.
 *
 * Use {@link #apply(com.oracle.truffle.api.vm.PolyglotEngine.Builder)} to apply this information to
 * a newly-built {@link PolyglotEngine} instance (it will be stored in the "fastrContextInfo" global
 * symbol).
 */
public final class ContextInfo implements TruffleObject {
    static final String GLOBAL_SYMBOL = "fastrContextInfo";

    private static final ConcurrentHashMap<Integer, ContextInfo> contextInfos = new ConcurrentHashMap<>();
    private static final AtomicInteger contextInfoIds = new AtomicInteger();

    private final RCmdOptions options;
    private final RContext.ContextKind kind;
    private final TimeZone systemTimeZone;

    /**
     * Any context created by another has a parent. When such a context is destroyed we must reset
     * the RContext.threadLocalContext to the parent.
     */
    private final RContext parent;
    private final ConsoleHandler consoleHandler;
    private final int id;

    private ContextInfo(RCmdOptions options, ContextKind kind, RContext parent, ConsoleHandler consoleHandler, TimeZone systemTimeZone, int id) {
        this.options = options;
        this.kind = kind;
        this.parent = parent;
        this.consoleHandler = consoleHandler;
        this.systemTimeZone = systemTimeZone;
        this.id = id;
    }

    public PolyglotEngine.Builder apply(PolyglotEngine.Builder builder) {
        return builder.globalSymbol(GLOBAL_SYMBOL, this);
    }

    /**
     * Create a context configuration object.
     *
     * @param parent if non-null {@code null}, the parent creating the context
     * @param kind defines the degree to which this context shares base and package environments
     *            with its parent
     * @param options the command line arguments passed this R session
     * @param consoleHandler a {@link ConsoleHandler} for output
     * @param systemTimeZone the system's time zone
     */
    public static ContextInfo create(RCmdOptions options, ContextKind kind, RContext parent, ConsoleHandler consoleHandler, TimeZone systemTimeZone) {
        int id = contextInfoIds.incrementAndGet();
        return new ContextInfo(options, kind, parent, consoleHandler, systemTimeZone, id);
    }

    public static ContextInfo create(RCmdOptions options, ContextKind kind, RContext parent, ConsoleHandler consoleHandler) {
        return create(options, kind, parent, consoleHandler, TimeZone.getDefault());
    }

    public static int createDeferred(RCmdOptions options, ContextKind kind, RContext parent, ConsoleHandler consoleHandler) {
        ContextInfo info = create(options, kind, parent, consoleHandler, TimeZone.getDefault());
        contextInfos.put(info.id, info);
        return info.id;
    }

    public static ContextInfo get(int id) {
        return contextInfos.get(id);
    }

    public RCmdOptions getOptions() {
        return options;
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

    @Override
    public ForeignAccess getForeignAccess() {
        throw new IllegalStateException("cannot access " + ContextInfo.class.getSimpleName() + " via Truffle");
    }
}
