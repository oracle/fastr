/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.interop;

import com.oracle.truffle.r.runtime.context.RContext;

/**
 * Context-specific state relevant to the .fastr.interop.try builtins.
 */
@SuppressWarnings("serial")
public class FastrInteropTryContextState implements RContext.ContextState {
    /**
     * Values is either NULL or an RPairList, for {@code restarts}.
     */
    public Throwable lastException = null;
    /**
     * Determines if in scope of a .fastr.interop.try builtin call.
     */
    private int tryCounter = 0;

    public static FastrInteropTryContextState newContextState() {
        return new FastrInteropTryContextState();
    }

    public void stepIn() {
        tryCounter++;
    }

    public void stepOut() {
        tryCounter--;
        assert tryCounter >= 0;
    }

    public boolean isInTry() {
        return tryCounter > 0;
    }
}
