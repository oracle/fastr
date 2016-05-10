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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.function.opt.ReuseNonSharedNode;
import com.oracle.truffle.r.nodes.unary.CastIntegerNode;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RVisibility;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

@RBuiltin(name = "dim<-", visibility = RVisibility.OFF, kind = PRIMITIVE, parameterNames = {"x", "value"})
public abstract class UpdateDim extends RBuiltinNode {

    @Child private ReuseNonSharedNode reuse = ReuseNonSharedNode.create();

    @Specialization
    protected RAbstractVector updateDim(RAbstractVector vector, @SuppressWarnings("unused") RNull dimensions) {
        controlVisibility();
        RVector result = ((RAbstractVector) reuse.execute(vector)).materialize();
        result.resetDimensions(null);
        return result;
    }

    @Specialization
    protected RAbstractVector updateDim(RAbstractVector vector, RAbstractVector dimensions, //
                    @Cached("createPreserveNames()") CastIntegerNode castInteger) {
        controlVisibility();
        if (dimensions.getLength() == 0) {
            CompilerDirectives.transferToInterpreter();
            throw RError.error(this, RError.Message.LENGTH_ZERO_DIM_INVALID);
        }
        int[] dimsData = ((RAbstractIntVector) castInteger.execute(dimensions)).materialize().getDataCopy();
        RVector.verifyDimensions(vector.getLength(), dimsData, this);
        RVector result = ((RAbstractVector) reuse.execute(vector)).materialize();
        result.resetDimensions(dimsData);
        return result;
    }
}
