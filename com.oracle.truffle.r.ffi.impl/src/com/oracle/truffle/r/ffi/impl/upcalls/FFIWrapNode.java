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
package com.oracle.truffle.r.ffi.impl.upcalls;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RObject;

public abstract class FFIWrapNode extends Node {

    public abstract Object execute(Object value);

    @Specialization
    protected static Object wrap(int value) {
        return wrap(RDataFactory.createIntVectorFromScalar(value));
    }

    @Specialization
    protected static Object wrap(double value) {
        return wrap(RDataFactory.createDoubleVectorFromScalar(value));
    }

    @Specialization
    protected static Object wrap(byte value) {
        return wrap(RDataFactory.createLogicalVectorFromScalar(value));
    }

    @Specialization
    protected static Object wrap(String value) {
        return wrap(RDataFactory.createStringVectorFromScalar(value));
    }

    @Specialization
    protected static Object wrap(RObject value) {
        return value;
    }

    @Fallback
    protected static Object wrap(Object value) {
        System.out.println("invalid wrapping: " + value.getClass().getSimpleName());
        return value;
    }

    public static FFIWrapNode create() {
        return FFIWrapNodeGen.create();
    }

    public static FFIWrapNode[] create(int count) {
        FFIWrapNode[] result = new FFIWrapNode[count];
        for (int i = 0; i < count; i++) {
            result[i] = FFIWrapNode.create();
        }
        return result;
    }
}
