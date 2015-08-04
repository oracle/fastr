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
package com.oracle.truffle.r.nodes.access.array.read;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.r.nodes.access.array.*;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.env.*;
import com.oracle.truffle.r.runtime.nodes.*;

public class PositionsArrayNode extends RNode implements RSyntaxNode {

    @Child private PositionsArrayConversionNodeMultiDimAdapter conversionAdapter;
    @Child private PositionsArrayNodeAdapter positionsAdapter;
    @Child private PromiseHelperNode promiseHelper;

    private final boolean hasVarArg;

    public PositionsArrayNode(boolean isSubset, RNode[] positions, boolean hasVarArg) {
        this.conversionAdapter = new PositionsArrayConversionNodeMultiDimAdapter(isSubset, positions.length);
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

    @ExplodeLoop
    public static Object[] expandVarArg(VirtualFrame frame, RArgsValuesAndNames varArg, int oldInd, Object[] oldEvaluatedElements, PromiseHelperNode promiseHelper) {
        int ind = oldInd;
        Object[] evaluatedElements = oldEvaluatedElements;
        Object[] varArgValues = varArg.getArguments();
        int varArgLength = varArg.getLength();
        if (varArgLength > 1) {
            evaluatedElements = Utils.resizeArray(evaluatedElements, evaluatedElements.length + varArgLength - 1);
        }
        for (int j = 0; j < varArgLength; j++) {
            evaluatedElements[ind] = varArgValues[j];
            if (evaluatedElements[ind] instanceof RPromise) {
                evaluatedElements[ind] = promiseHelper.evaluate(frame, (RPromise) evaluatedElements[ind]);
            }
            ind++;
        }
        return evaluatedElements;
    }

    public Object executeEval(VirtualFrame frame, Object vector, Object exact) {
        if (hasVarArg) {
            return doEvalVarArg(frame, vector, exact);
        } else {
            return doEvalNoVarArg(frame, vector, exact);
        }
    }

    @ExplodeLoop
    public static Object[] explodeLoopNoVarArg(VirtualFrame frame, PositionsArrayNodeAdapter positionsAdapter, int length) {
        Object[] evaluatedElements = new Object[length];
        for (int i = 0; i < length; i++) {
            evaluatedElements[i] = positionsAdapter.executePos(frame, i);
        }
        return evaluatedElements;
    }

    private Object doEvalNoVarArg(VirtualFrame frame, Object vector, Object exact) {
        int length = conversionAdapter.getLength();
        Object[] evaluatedElements = explodeLoopNoVarArg(frame, positionsAdapter, length);
        length = conversionAdapter.getLength(); // could have changed
        doEvalInternal(vector, exact, evaluatedElements, length);
        return conversionAdapter.getLength() == 1 ? evaluatedElements[0] : evaluatedElements;
    }

    @ExplodeLoop
    public static Object[] explodeLoopVarArg(VirtualFrame frame, PositionsArrayNodeAdapter positionsAdapter, int length, PromiseHelperNode promiseHelper) {
        Object[] evaluatedElements = new Object[length];
        int ind = 0;
        for (int i = 0; i < length; i++) {
            Object p = positionsAdapter.executePos(frame, i);
            if (p instanceof RArgsValuesAndNames) {
                RArgsValuesAndNames varArg = (RArgsValuesAndNames) p;
                evaluatedElements = expandVarArg(frame, varArg, ind, evaluatedElements, promiseHelper);
                ind += varArg.getLength();
            } else {
                evaluatedElements[ind++] = p;
            }
        }
        return evaluatedElements;
    }

    private Object doEvalVarArg(VirtualFrame frame, Object vector, Object exact) {
        int length = conversionAdapter.getLength();
        Object[] evaluatedElements = explodeLoopVarArg(frame, positionsAdapter, length, promiseHelper);
        if (evaluatedElements.length != conversionAdapter.getLength()) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            this.conversionAdapter = insert(new PositionsArrayConversionNodeMultiDimAdapter(this.conversionAdapter.isSubset(), evaluatedElements.length));
        }
        length = conversionAdapter.getLength(); // could have changed
        doEvalInternal(vector, exact, evaluatedElements, length);
        return conversionAdapter.getLength() == 1 ? evaluatedElements[0] : evaluatedElements;
    }

    @ExplodeLoop
    private void doEvalInternal(Object vector, Object exact, Object[] evaluatedElements, int length) {
        for (int i = 0; i < length; i++) {
            Object convertedOperator = conversionAdapter.executeConvert(vector, evaluatedElements[i], exact, i);
            evaluatedElements[i] = conversionAdapter.executeArg(vector, convertedOperator, i);
            if (conversionAdapter.multiDimOperatorConverters != null) {
                evaluatedElements[i] = conversionAdapter.executeMultiConvert(vector, evaluatedElements[i], i);
            }
        }

    }

    @Override
    public boolean isBackbone() {
        return true;
    }

    @Override
    public RSyntaxNode substituteImpl(REnvironment env) {
        return new PositionsArrayNode(conversionAdapter.isSubset(), positionsAdapter.substitutePositions(env), hasVarArg);
    }

    @Override
    public void deparseImpl(RDeparse.State state) {
        positionsAdapter.deparse(state);
    }

    @Override
    public void serializeImpl(RSerialize.State state) {
        positionsAdapter.serialize(state);
    }
}
