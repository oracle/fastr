package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.RBuiltinKind.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.r.nodes.builtin.*;
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
