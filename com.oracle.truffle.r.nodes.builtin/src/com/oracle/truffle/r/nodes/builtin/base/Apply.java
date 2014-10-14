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

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

@RBuiltin(name = "apply", kind = SUBSTITUTE, parameterNames = {"X", "MARGIN", "FUN", "..."})
// TODO Rewrite to accept any vector type but ideally not by simply repeating the code
public abstract class Apply extends RBuiltinNode {

    private final ConditionProfile rowMarginProfile = ConditionProfile.createBinaryProfile();

    @Child private CallInlineCacheNode callCache = CallInlineCacheNode.create(3);

    @Override
    public RNode[] getParameterValues() {
        return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance)};
    }

    @Specialization
    protected Object applyRows(VirtualFrame frame, RDoubleVector x, double margin, RFunction fun, @SuppressWarnings("unused") Object args) {
        controlVisibility();
        int[] xdim = x.getDimensions();
        final int rows = xdim == null ? x.getLength() : xdim[0];
        final int cols = xdim == null ? 1 : xdim[1];
        Object[] result;
        if (rowMarginProfile.profile(margin == 1.0)) {
            result = new Object[rows];
            for (int row = 0; row < rows; ++row) {
                double[] rowData = new double[cols];
                for (int i = 0; i < cols; ++i) {
                    rowData[i] = x.getDataAt(i * rows + row);
                }
                result[row] = callFunction(frame, fun, RDataFactory.createDoubleVector(rowData, RDataFactory.COMPLETE_VECTOR));
            }
        } else {
            result = new Object[cols];
            for (int col = 0; col < cols; ++col) {
                double[] colData = new double[rows];
                for (int i = 0; i < rows; ++i) {
                    colData[i] = x.getDataAt(col * rows + i);
                }
                result[col] = callFunction(frame, fun, RDataFactory.createDoubleVector(colData, RDataFactory.COMPLETE_VECTOR));
            }
        }
        return RDataFactory.createObjectVector(result, RDataFactory.COMPLETE_VECTOR);
    }

    private Object callFunction(VirtualFrame frame, RFunction fun, Object input) {
        FormalArguments formals = ((RRootNode) fun.getTarget().getRootNode()).getFormalArguments();

        // Create arguments: Set 1. to input and fill the rest with defaults
        Object[] evaluatedArgs = new Object[formals.getArgsCount()];
        if (evaluatedArgs.length > 0) {
            evaluatedArgs[0] = input;
        }
        for (int i = 1; i < evaluatedArgs.length; i++) {
            RNode node = formals.getDefaultArgs()[i];
            if (node == null) {
                evaluatedArgs[i] = RMissing.instance;
            } else if (node instanceof ConstantNode) {
                evaluatedArgs[i] = ((ConstantNode) node).getValue();
            } else {
                // TODO Set default values in AccessArgumentNode instead of this??
                Utils.nyi();
            }
        }
        Object[] args = RArguments.create(fun, getSourceSection(), RArguments.getDepth(frame) + 1, evaluatedArgs);
        return callCache.execute(frame, fun.getTarget(), args);
    }
}
