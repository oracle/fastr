/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.access.array.write;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.array.*;
import com.oracle.truffle.r.nodes.access.array.read.*;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.RDeparse.*;
import com.oracle.truffle.r.runtime.data.*;

public class PositionsArrayNodeValue extends RNode {

    @Child PositionsArrayConversionValueNodeMultiDimAdapter conversionAdapter;
    @Child PositionsArrayNodeAdapter positionsAdapter;
    @Child private PromiseHelperNode promiseHelper;
    private final boolean hasVarArg;

    public PositionsArrayNodeValue(boolean isSubset, RNode[] positions, boolean hasVarArg) {
        this.conversionAdapter = new PositionsArrayConversionValueNodeMultiDimAdapter(isSubset, positions.length);
        this.positionsAdapter = new PositionsArrayNodeAdapter(positions);
        this.hasVarArg = hasVarArg;
        if (hasVarArg) {
            this.promiseHelper = new PromiseHelperNode();
        }
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return conversionAdapter.execute(frame);
    }

    public Object executeEval(VirtualFrame frame, Object vector, Object value) {
        if (hasVarArg) {
            return executeEvalVarArg(frame, vector, value);
        } else {
            return executeEvalNoVarArg(frame, vector, value);
        }
    }

    public Object executeEvalNoVarArg(VirtualFrame frame, Object vector, Object value) {
        int length = conversionAdapter.getLength();
        Object[] evaluatedElements = new Object[length];
        for (int i = 0; i < length; i++) {
            evaluatedElements[i] = positionsAdapter.executePos(frame, i);
        }
        executeEvalInternal(frame, vector, value, evaluatedElements);
        return conversionAdapter.getLength() == 1 ? evaluatedElements[0] : evaluatedElements;
    }

    public Object executeEvalVarArg(VirtualFrame frame, Object vector, Object value) {
        int length = conversionAdapter.getLength();
        Object[] evaluatedElements = new Object[length];
        int ind = 0;
        for (int i = 0; i < length; i++) {
            Object p = positionsAdapter.executePos(frame, i);
            if (p instanceof RArgsValuesAndNames) {
                RArgsValuesAndNames varArg = (RArgsValuesAndNames) p;
                evaluatedElements = PositionsArrayNode.expandVarArg(frame, varArg, ind, evaluatedElements, promiseHelper);
                ind += varArg.length();
            } else {
                evaluatedElements[ind++] = p;
            }
        }
        if (evaluatedElements.length != conversionAdapter.getLength()) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            this.conversionAdapter = new PositionsArrayConversionValueNodeMultiDimAdapter(this.conversionAdapter.isSubset(), evaluatedElements.length);
        }
        executeEvalInternal(frame, vector, value, evaluatedElements);
        return conversionAdapter.getLength() == 1 ? evaluatedElements[0] : evaluatedElements;
    }

    @ExplodeLoop
    public void executeEvalInternal(VirtualFrame frame, Object vector, Object value, Object[] evaluatedElements) {
        for (int i = 0; i < conversionAdapter.getLength(); i++) {
            Object convertedOperator = conversionAdapter.executeConvert(frame, vector, evaluatedElements[i], RRuntime.LOGICAL_TRUE, i);
            evaluatedElements[i] = conversionAdapter.executeArg(frame, vector, convertedOperator, i);
            if (conversionAdapter.multiDimOperatorConverters != null) {
                evaluatedElements[i] = conversionAdapter.executeMultiConvert(frame, vector, value, evaluatedElements[i], i);
            }
        }
    }

    @Override
    public void deparse(State state) {
        conversionAdapter.deparse(state);
    }

}
