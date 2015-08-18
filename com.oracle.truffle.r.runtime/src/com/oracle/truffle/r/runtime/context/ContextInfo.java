/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.context.RContext.ContextKind;

public final class ContextInfo {
    private static final ConcurrentHashMap<Integer, ContextInfo> contextInfos = new ConcurrentHashMap<>();
    private static final AtomicInteger contextInfoIds = new AtomicInteger();

    private final RCmdOptions options;
    private final RContext.ContextKind kind;
    private final RContext parent;
    private final int id;

    public ContextInfo(RCmdOptions options, ContextKind kind, RContext parent, int id) {
        this.options = options;
        this.kind = kind;
        this.parent = parent;
        this.id = id;
    }

    public RContext newContext() {
        ContextInfo info = contextInfos.remove(id);
        assert info != null;
        return RContext.getRRuntimeASTAccess().create(parent, kind, options, parent.getConsoleHandler(), parent.getEnv());
    }

    public static int create(RCmdOptions options, ContextKind kind, RContext parent) {
        int id = contextInfoIds.incrementAndGet();
        contextInfos.put(id, new ContextInfo(options, kind, parent, id));
        return id;
    }

    public static ContextInfo get(int id) {
        return contextInfos.get(id);
    }
}
