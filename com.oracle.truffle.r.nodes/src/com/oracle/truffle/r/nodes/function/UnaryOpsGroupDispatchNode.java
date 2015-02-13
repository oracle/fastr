/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014-2015, Purdue University
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.function;

import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

/*
 * Handles unary +, - and ! operators.
 */
public class UnaryOpsGroupDispatchNode extends GroupDispatchNode {

    public UnaryOpsGroupDispatchNode(String genericName, boolean hasVararg, SourceSection callSrc, SourceSection argSrc) {
        super(genericName, RGroupGenerics.GROUP_OPS, hasVararg, callSrc, argSrc);
    }

    @Override
    protected Object callBuiltin(VirtualFrame frame, Object[] evaluatedArgs, String[] argNames) {
        initBuiltin(frame);
        Object[] argObject = RArguments.create(builtinFunc, this.callSrc, RArguments.getDepth(frame) + 1, new Object[]{evaluatedArgs[0], RMissing.instance});
        return indirectCallNode.call(frame, builtinFunc.getTarget(), argObject);
    }
}

class GenericUnaryOpsGroupDispatchNode extends UnaryOpsGroupDispatchNode {

    public GenericUnaryOpsGroupDispatchNode(String genericName, boolean hasVararg, SourceSection callSrc, SourceSection argSrc) {
        super(genericName, hasVararg, callSrc, argSrc);
    }

    @Override
    public Object execute(VirtualFrame frame, final RArgsValuesAndNames argAndNames) {
        Object[] evaluatedArgs = argAndNames.getValues();
        String[] argNames = argAndNames.getNames();
        this.type = getArgClass(evaluatedArgs[0]);
        if (this.type == null) {
            return callBuiltin(frame, evaluatedArgs, argNames);
        }
        findTargetFunction(frame);
        if (targetFunction != null) {
            dotMethod = RDataFactory.createStringVector(new String[]{targetFunctionName, ""}, true);
        }
        if (targetFunction == null) {
            callBuiltin(frame, evaluatedArgs, argNames);
        }
        return executeHelper(frame, evaluatedArgs, argNames);
    }
}
