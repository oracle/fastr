/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.nodes.Node;

/**
 * Support for the {.C} and {.Fortran} calls.
 */
public interface CRFFI {
    abstract class InvokeCNode extends Node {
        /**
         * Invoke the native method identified by {@code symbolInfo} passing it the arguments in
         * {@code args}. The values in {@code args} should be native types,e.g., {@code double[]}
         * not {@code RDoubleVector}. Strings are already converted to 2-dimensional byte arrays.
         *
         * @param hasStrings if {@code true}, then the {@code args} array may contain one or more
         *            values of type {@code byte[][]}, which represent arrays of strings in ASCII
         *            encoding.
         */
        public abstract void execute(NativeCallInfo nativeCallInfo, Object[] args, boolean hasStrings);
    }

    InvokeCNode createInvokeCNode();
}
