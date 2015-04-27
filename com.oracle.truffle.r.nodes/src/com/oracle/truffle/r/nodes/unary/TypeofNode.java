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
package com.oracle.truffle.r.nodes.unary;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.conn.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.env.*;

@SuppressWarnings("unused")
public abstract class TypeofNode extends UnaryNode {

    protected static final int NUMBER_OF_CACHED_CLASSES = 5;

    public abstract RType execute(Object x);

    @Specialization
    protected static RType doLogical(byte x) {
        return RType.Logical;
    }

    @Specialization
    protected static RType doInt(int s) {
        return RType.Integer;
    }

    @Specialization
    protected static RType doDouble(double x) {
        return RType.Double;
    }

    @Specialization
    protected static RType doString(String x) {
        return RType.Character;
    }

    @Specialization
    protected static RType doString(RMissing x) {
        return RType.Missing;
    }

    @Specialization(guards = {"operand.getClass() == cachedClass"}, limit = "NUMBER_OF_CACHED_CLASSES")
    protected static RType doCachedTyped(Object operand, //
                    @Cached("getTypedValueClass(operand)") Class<? extends RTypedValue> cachedClass) {
        return cachedClass.cast(operand).getRType();
    }

    protected static Class<? extends RTypedValue> getTypedValueClass(Object operand) {
        CompilerAsserts.neverPartOfCompilation();
        if (operand instanceof RTypedValue) {
            return ((RTypedValue) operand).getClass();
        } else {
            throw new AssertionError("Invalid untyped value " + operand.getClass() + ".");
        }
    }

    @Specialization(contains = {"doCachedTyped"})
    protected static RType doGenericTyped(RTypedValue operand) {
        return operand.getRType();
    }

}
