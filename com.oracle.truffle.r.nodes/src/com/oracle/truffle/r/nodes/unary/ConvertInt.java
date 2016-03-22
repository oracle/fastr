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
package com.oracle.truffle.r.nodes.unary;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;

public abstract class ConvertInt extends CastNode {

    @Child private ConvertInt convertIntRecursive;

    public abstract int executeInteger(Object operand);

    private int convertIntRecursive(Object operand) {
        if (convertIntRecursive == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            convertIntRecursive = insert(ConvertIntNodeGen.create());
        }
        return executeInteger(operand);
    }

    @Specialization
    protected int doInt(int operand) {
        return operand;
    }

    @Specialization
    protected int doDouble(double operand) {
        return (int) operand;
    }

    @Specialization
    protected int doLogical(byte operand) {
        return RRuntime.logical2int(operand);
    }

    @Specialization(guards = "operand.getLength() == 1")
    protected int doLogical(RAbstractContainer operand) {
        return convertIntRecursive(operand.getDataAtAsObject(0));
    }

    @Fallback
    @TruffleBoundary
    protected int doOther(Object operand) {
        throw new ConversionFailedException(operand.getClass().getName());
    }
}
