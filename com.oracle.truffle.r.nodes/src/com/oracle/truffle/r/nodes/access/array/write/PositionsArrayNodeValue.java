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
import com.oracle.truffle.r.runtime.env.*;

public class PositionsArrayNodeValue extends RNode implements RSyntaxNode {

    @Child private PositionsArrayConversionValueNodeMultiDimAdapter conversionAdapter;
    @Child private PositionsArrayNodeAdapter positionsAdapter;
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

    @ExplodeLoop
    private Object executeEvalNoVarArg(VirtualFrame frame, Object vector, Object value) {
        int length = conversionAdapter.getLength();
        Object[] evaluatedElements = PositionsArrayNode.explodeLoopNoVarArg(frame, positionsAdapter, length);
        executeEvalInternal(vector, value, evaluatedElements, length);
        return conversionAdapter.getLength() == 1 ? evaluatedElements[0] : evaluatedElements;
    }

    private Object executeEvalVarArg(VirtualFrame frame, Object vector, Object value) {
        int length = conversionAdapter.getLength();
        Object[] evaluatedElements = PositionsArrayNode.explodeLoopVarArg(frame, positionsAdapter, length, promiseHelper);
        if (evaluatedElements.length != conversionAdapter.getLength()) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            this.conversionAdapter = insert(new PositionsArrayConversionValueNodeMultiDimAdapter(this.conversionAdapter.isSubset(), evaluatedElements.length));
        }
        length = conversionAdapter.getLength(); // could have changed
        executeEvalInternal(vector, value, evaluatedElements, length);
        return conversionAdapter.getLength() == 1 ? evaluatedElements[0] : evaluatedElements;
    }

    @ExplodeLoop
    private void executeEvalInternal(Object vector, Object value, Object[] evaluatedElements, int length) {
        for (int i = 0; i < length; i++) {
            Object convertedOperator = conversionAdapter.executeConvert(vector, evaluatedElements[i], RRuntime.LOGICAL_TRUE, i);
            evaluatedElements[i] = conversionAdapter.executeArg(vector, convertedOperator, i);
            if (conversionAdapter.multiDimOperatorConverters != null) {
                evaluatedElements[i] = conversionAdapter.executeMultiConvert(vector, value, evaluatedElements[i], i);
            }
        }
    }

    @Override
    public void deparseImpl(RDeparse.State state) {
        positionsAdapter.deparse(state);
    }

    @Override
    public void serializeImpl(RSerialize.State state) {
        positionsAdapter.serialize(state);
    }

    public RSyntaxNode substituteImpl(REnvironment env) {
        throw RInternalError.unimplemented();
    }
}
