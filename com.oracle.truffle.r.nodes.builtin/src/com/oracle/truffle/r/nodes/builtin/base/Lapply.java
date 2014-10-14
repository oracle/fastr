/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, Oracle and/or its affiliates
 *
 * All rights reserved.
 */

package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin(name = "lapply", kind = INTERNAL, parameterNames = {"X", "FUN", "..."})
public abstract class Lapply extends RBuiltinNode {

    @Child private CallInlineCacheNode callCache = CallInlineCacheNode.create(3);

    @Override
    public RNode[] getParameterValues() {
        return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance)};
    }

    @Specialization(guards = "!argMissing")
    protected Object lapply(VirtualFrame frame, RAbstractVector x, RFunction fun, RArgsValuesAndNames optionalArgs) {
        Object[] optionalArgValues = optionalArgs.getValues();
        Object[] combinedArgs = new Object[optionalArgValues.length + 1];
        System.arraycopy(optionalArgValues, 0, combinedArgs, 1, optionalArgValues.length);
        return lapplyHelper(frame, x, fun, combinedArgs);
    }

    @Specialization(guards = "argMissing")
    protected Object lapplyMissing(VirtualFrame frame, RAbstractVector x, RFunction fun, @SuppressWarnings("unused") RArgsValuesAndNames optionalArgs) {
        Object[] combinedArgs = new Object[]{null};
        return lapplyHelper(frame, x, fun, combinedArgs);
    }

    @Specialization
    protected Object lapply(VirtualFrame frame, RAbstractVector x, RFunction fun, @SuppressWarnings("unused") RMissing optionalArg) {
        Object[] combinedArgs = new Object[]{null};
        return lapplyHelper(frame, x, fun, combinedArgs);
    }

    private Object lapplyHelper(VirtualFrame frame, RAbstractVector x, RFunction fun, Object[] combinedArgs) {
        controlVisibility();
        RVector xMat = x.materialize();
        Object[] callResult = applyHelper(frame, callCache, xMat, fun, combinedArgs);
        return RDataFactory.createList(callResult, xMat.getNames());
    }

    static Object[] applyHelper(VirtualFrame frame, CallInlineCacheNode callCache, RVector xMat, RFunction fun, Object[] combinedArgs) {
        /* TODO: R switches to double if x.getLength() is greater than 2^31-1 */
        Object[] result = new Object[xMat.getLength()];
        Object[] arguments = RArguments.create(fun, callCache.getSourceSection(), RArguments.getDepth(frame) + 1, combinedArgs);
        int firstArgOffset = arguments.length - combinedArgs.length;
        for (int i = 0; i < result.length; ++i) {
            // FIXME breaks encapsulation.
            arguments[firstArgOffset] = xMat.getDataAtAsObject(i);
            result[i] = callCache.execute(frame, fun.getTarget(), arguments);
        }
        return result;
    }

    @SuppressWarnings("unused")
    protected boolean argMissing(RAbstractVector x, RFunction fun, RArgsValuesAndNames optionalArg) {
        return optionalArg.length() == 1 && optionalArg.getValues()[0] == RMissing.instance;
    }
}
