/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin(name = "vapply", kind = INTERNAL, parameterNames = {"x", "fun", "..."})
public abstract class VApply extends RBuiltinNode {

    @Child private CallInlineCacheNode callCache = CallInlineCacheNode.create(3);

    // TODO complete the implementation so that it works for all types of x and fun
    @Specialization
    protected Object vapply(VirtualFrame frame, RAbstractVector x, RFunction fun, RArgsValuesAndNames optionalArgs) {
        controlVisibility();
        Object[] optionalArgValues = optionalArgs.getValues();
        // The first element of optionalArgs is FUN_VALUE
        Object funValue = optionalArgValues[0];
        int optionalArgsLength = optionalArgValues.length - 1;
        Object[] combinedArgs = new Object[optionalArgsLength + 1];
        System.arraycopy(optionalArgValues, 0, combinedArgs, 1, optionalArgsLength);
        RVector xMat = x.materialize();
        Object[] applyResult;
        if (x.getLength() > 0) {
            applyResult = Lapply.applyHelper(frame, callCache, xMat, fun, combinedArgs);
        } else {
            applyResult = new Object[0];
        }
        Object result = null;
        boolean applyResultZeroLength = applyResult.length == 0;
        if (funValue instanceof Integer) {
            int[] data = applyResultZeroLength ? new int[0] : new int[]{(Integer) applyResult[0]};
            result = RDataFactory.createIntVector(data, RDataFactory.COMPLETE_VECTOR);
        } else if (funValue instanceof Double) {
            double[] data = applyResultZeroLength ? new double[0] : new double[]{(Double) applyResult[0]};
            result = RDataFactory.createDoubleVector(data, RDataFactory.COMPLETE_VECTOR);
        } else if (funValue instanceof Byte) {
            byte[] data = applyResultZeroLength ? new byte[0] : new byte[]{(Byte) applyResult[0]};
            result = RDataFactory.createLogicalVector(data, RDataFactory.COMPLETE_VECTOR);
        } else if (funValue instanceof String) {
            String[] data = applyResultZeroLength ? new String[0] : new String[]{(String) applyResult[0]};
            result = RDataFactory.createStringVector(data, RDataFactory.COMPLETE_VECTOR);
        } else {
            assert false;
        }
        return result;
    }

}
