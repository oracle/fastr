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

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin(name = "vapply", kind = INTERNAL)
public abstract class VApply extends RBuiltinNode {

    @Child protected IndirectCallNode funCall = Truffle.getRuntime().createIndirectCallNode();

    // TODO complete the implementation so that it works for all types of x and fun
    @Specialization
    public Object vapply(VirtualFrame frame, RAbstractVector x, RFunction fun, Object[] optionalArgs) {
        controlVisibility();
        // The first element of optionalArgs is FUN_VALUE
        Object funValue = optionalArgs[0];
        int optionalArgsLength = optionalArgs.length - 1;
        Object[] combinedArgs = new Object[optionalArgsLength + 1];
        System.arraycopy(optionalArgs, 0, combinedArgs, 1, optionalArgsLength);
        RVector xMat = x.materialize();
        Object[] applyResult = Lapply.applyHelper(frame, funCall, xMat, fun, combinedArgs);
        Object result = null;
        if (funValue instanceof Integer) {
            int[] data = new int[]{(Integer) applyResult[0]};
            result = RDataFactory.createIntVector(data, RDataFactory.COMPLETE_VECTOR);
        } else if (funValue instanceof Double) {
            double[] data = new double[]{(Double) applyResult[0]};
            result = RDataFactory.createDoubleVector(data, RDataFactory.COMPLETE_VECTOR);
        } else {
            assert false;
        }
        return result;
    }

    @Specialization
    public Object vapply(VirtualFrame frame, RAbstractVector x, RFunction fun, Object optionalArg) {
        Object[] optionalArgs = new Object[]{optionalArg};
        return vapply(frame, x, fun, optionalArgs);
    }

}
