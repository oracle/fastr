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

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin(name = "lapply", kind = INTERNAL)
public abstract class Lapply extends RBuiltinNode {

    @Child protected IndirectCallNode funCall = Truffle.getRuntime().createIndirectCallNode();

    private static final Object[] PARAMETER_NAMES = new Object[]{"X", "FUN", "..."};

    @Override
    public Object[] getParameterNames() {
        return PARAMETER_NAMES;
    }

    @Override
    public RNode[] getParameterValues() {
        return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance)};
    }

    @Specialization
    public Object lapply(VirtualFrame frame, RAbstractVector x, RFunction fun, Object[] optionalArgs) {
        Object[] combinedArgs = new Object[optionalArgs.length + 1];
        System.arraycopy(optionalArgs, 0, combinedArgs, 1, optionalArgs.length);
        return lapplyHelper(frame, x, fun, combinedArgs);
    }

    @Specialization
    public Object lapply(VirtualFrame frame, RAbstractVector x, RFunction fun, Object optionalArg) {
        Object[] combinedArgs = new Object[]{null, optionalArg};
        return lapplyHelper(frame, x, fun, combinedArgs);
    }

    @Specialization
    public Object lapply(VirtualFrame frame, RAbstractVector x, RFunction fun, @SuppressWarnings("unused") RMissing optionalArg) {
        Object[] combinedArgs = new Object[]{null};
        return lapplyHelper(frame, x, fun, combinedArgs);
    }

    private Object lapplyHelper(VirtualFrame frame, RAbstractVector x, RFunction fun, Object[] combinedArgs) {
        controlVisibility();
        RVector xMat = x.materialize();
        Object[] callResult = applyHelper(frame, funCall, xMat, fun, combinedArgs);
        return RDataFactory.createList(callResult, xMat.getNames());
    }

    static Object[] applyHelper(VirtualFrame frame, IndirectCallNode funCall, RVector xMat, RFunction fun, Object[] combinedArgs) {
        /* TODO: R switches to double if x.getLength() is greater than 2^31-1 */
        Object[] result = new Object[xMat.getLength()];
        Object[] arguments = RArguments.create(fun, combinedArgs);
        int firstArgOffset = arguments.length - combinedArgs.length;
        for (int i = 0; i < result.length; ++i) {
            // FIXME breaks encapsulation.
            arguments[firstArgOffset] = xMat.getDataAtAsObject(i);
            result[i] = funCall.call(frame, fun.getTarget(), arguments);
        }
        return result;
    }
}
