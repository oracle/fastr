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
package com.oracle.truffle.r.nodes.access.array.read;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.array.*;
import com.oracle.truffle.r.nodes.access.array.ArrayPositionCast.OperatorConverterNode;
import com.oracle.truffle.r.runtime.*;

@NodeChild(value = "vector", type = RNode.class)
public class PositionsArrayNode extends RNode {
    @Children private final ArrayPositionCast[] elements;
    @Children private final RNode[] positions;
    @Children private final OperatorConverterNode[] operatorConverters;
    @Children private final MultiDimPosConverterNode[] multiDimOperatorConverters;

    @ExplodeLoop
    public Object executeEval(VirtualFrame frame, Object vector) {
        Object[] evaluatedElements = new Object[elements.length];
        for (int i = 0; i < elements.length; i++) {
            Object p = positions[i].execute(frame);
            Object convertedOperator = operatorConverters[i].executeConvert(frame, vector, p);
            evaluatedElements[i] = elements[i].executeArg(frame, p, vector, convertedOperator);
            if (multiDimOperatorConverters != null && multiDimOperatorConverters.length > 1) {
                evaluatedElements[i] = multiDimOperatorConverters[i].executeConvert(frame, vector, evaluatedElements[i]);
            }
        }
        return elements.length == 1 ? evaluatedElements[0] : evaluatedElements;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        // this node is meant to be used only for evaluation by "executeWith" that uses
        // executeEvaluated method above
        Utils.fail("should never be executed");
        return null;
    }

    public PositionsArrayNode(ArrayPositionCast[] elements, RNode[] positions, OperatorConverterNode[] operatorConverters, MultiDimPosConverterNode[] multiDimOperatorConverters) {
        this.elements = elements;
        this.positions = positions;
        this.operatorConverters = operatorConverters;
        this.multiDimOperatorConverters = multiDimOperatorConverters;
    }
}
