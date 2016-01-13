/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.CastDoubleNode;
import com.oracle.truffle.r.nodes.unary.CastDoubleNodeGen;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.ops.na.*;

@RBuiltin(name = "Conj", kind = PRIMITIVE, parameterNames = {"z"})
public abstract class Conj extends RBuiltinNode {

    private NACheck naCheck = NACheck.create();

    @Child private CastDoubleNode castDouble;

    protected Object castDouble(Object value) {
        if (castDouble == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castDouble = insert(CastDoubleNodeGen.create(true, true, true));
        }
        return castDouble.executeDouble(value);
    }

    @Specialization
    protected RComplexVector conj(RAbstractComplexVector vector) {
        RComplexVector result = (RComplexVector) vector.copy();
        naCheck.enable(vector);
        for (int i = 0; i < vector.getLength(); i++) {
            RComplex el = vector.getDataAt(i);
            if (!naCheck.check(el)) {
                result.updateDataAt(i, RDataFactory.createComplex(el.getRealPart(), -el.getImaginaryPart()), naCheck);
            }
        }
        return result;
    }

    @Specialization
    protected RDoubleVector conj(RAbstractDoubleVector vector) {
        return (RDoubleVector) vector.copy();
    }

    @Specialization
    protected Object conj(RAbstractIntVector vector) {
        return castDouble(vector);
    }

    @Specialization
    protected Object conj(RAbstractLogicalVector vector) {
        return castDouble(vector);
    }

    @Fallback
    protected Object conj(@SuppressWarnings("unused") Object o) {
        throw RError.error(this, RError.Message.NON_NUMERIC_ARGUMENT_FUNCTION);
    }
}
