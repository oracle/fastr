/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.ffi;

/**
 * Support for the {.Call} and {.External} calls.
 */
public interface CallRFFI {
    /**
     * Invoke the native method identified by {@code symbolInfo} passing it the arguments in
     * {@code args}. The values in {@code args} can be any of the types used to represent {@code R}
     * values in the implementation.
     *
     * @param symbolInfo identifies the symbol and the defining library
     * @param args arguments
     */
    Object invokeCall(DLL.SymbolInfo symbolInfo, Object[] args);

    /**
     * Variant that does not return a result (primarily for library "init" methods.
     */
    void invokeVoidCall(DLL.SymbolInfo symbolInfo, Object[] args);

    /**
     * Variant of {@link #invokeCall} for {@code .External}, where args are wrapped up as a single
     * argument to the native call.
     *
     * @param symbolInfo
     * @param args
     */
    Object invokeExternal(DLL.SymbolInfo symbolInfo, Object[] args);

}
