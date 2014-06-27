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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.Node.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin(name = "dim<-", kind = PRIMITIVE)
@SuppressWarnings("unused")
public abstract class UpdateDim extends RInvisibleBuiltinNode {

    @Child private CastIntegerNode castInteger;

    private RAbstractIntVector castInteger(VirtualFrame frame, RAbstractVector vector) {
        if (castInteger == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castInteger = insert(CastIntegerNodeFactory.create(null, true, false, false));
        }
        return (RAbstractIntVector) castInteger.executeCast(frame, vector);
    }

    @Specialization(order = 1)
    public RAbstractVector updateDim(RAbstractVector vector, RNull dimensions) {
        controlVisibility();
        RVector result = vector.materialize();
        result.resetDimensions(null);
        return result;
    }

    @Specialization(order = 2)
    public RAbstractVector updateDim(VirtualFrame frame, RAbstractVector vector, RAbstractVector dimensions) {
        controlVisibility();
        if (dimensions.getLength() == 0) {
            CompilerDirectives.transferToInterpreter();
            throw RError.getLengthZeroDimInvalid(getEncapsulatingSourceSection());
        }
        int[] dimsData = castInteger(frame, dimensions).materialize().getDataCopy();
        vector.verifyDimensions(dimsData, getEncapsulatingSourceSection());
        RVector result = vector.materialize();
        result.resetDimensions(dimsData);
        return result;
    }

}
