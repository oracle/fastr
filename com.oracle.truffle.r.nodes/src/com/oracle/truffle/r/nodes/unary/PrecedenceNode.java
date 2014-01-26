/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.runtime.data.*;

@SuppressWarnings("unused")
public abstract class PrecedenceNode extends UnaryNode {

    public static final int NO_PRECEDENCE = -1;
    public static final int RAW_PRECEDENCE = 0;
    public static final int LOGICAL_PRECEDENCE = 1;
    public static final int INT_PRECEDENCE = 2;
    public static final int DOUBLE_PRECEDENCE = 3;
    public static final int COMPLEX_PRECEDENCE = 4;
    public static final int STRING_PRECEDENCE = 5;
    public static final int LIST_PRECEDENCE = 6;

    @Override
    public int executeInteger(VirtualFrame frame) {
        return RTypesGen.RTYPES.asInteger(execute(frame));
    }

    public abstract int executeInteger(VirtualFrame frame, Object object);

    @Specialization
    public int doNull(RNull val) {
        return NO_PRECEDENCE;
    }

    @Specialization
    public int doRaw(RRaw val) {
        return RAW_PRECEDENCE;
    }

    @Specialization
    public int doRawVector(RRawVector val) {
        return RAW_PRECEDENCE;
    }

    @Specialization
    public int doLogical(byte val) {
        return LOGICAL_PRECEDENCE;
    }

    @Specialization
    public int doLogical(RLogicalVector val) {
        return LOGICAL_PRECEDENCE;
    }

    @Specialization
    public int doInt(int val) {
        return INT_PRECEDENCE;
    }

    @Specialization
    public int doComplex(RComplex val) {
        return COMPLEX_PRECEDENCE;
    }

    @Specialization
    public int doInt(RIntVector val) {
        return INT_PRECEDENCE;
    }

    @Specialization
    public int doInt(RIntSequence val) {
        return INT_PRECEDENCE;
    }

    @Specialization
    public int doDouble(double val) {
        return DOUBLE_PRECEDENCE;
    }

    @Specialization
    public int doDouble(RDoubleVector val) {
        return DOUBLE_PRECEDENCE;
    }

    @Specialization
    public int doDouble(RDoubleSequence val) {
        return DOUBLE_PRECEDENCE;
    }

    @Specialization
    public int doComplex(RComplexVector val) {
        return COMPLEX_PRECEDENCE;
    }

    @Specialization
    public int doString(String val) {
        return STRING_PRECEDENCE;
    }

    @Specialization
    public int doString(RStringVector val) {
        return STRING_PRECEDENCE;
    }

    @Specialization
    public int doList(RList val) {
        return LIST_PRECEDENCE;
    }

}
