package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;
import static com.oracle.truffle.r.runtime.RDispatch.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.builtin.base.GetFunctionsFactory.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.nodes.*;

@RBuiltin(name = "xtfrm", kind = PRIMITIVE, parameterNames = {"x"}, dispatch = INTERNAL_GENERIC)
public abstract class Xtfrm extends RBuiltinNode {
    @Child private GetFunctions.Get getNode;

    @Specialization
    protected Object xtfrm(VirtualFrame frame, Object x) {
        /*
         * Although this is a PRIMITIVE, there is an xtfrm.default that we must call if "x" is not
         * of a class that already has an xtfrm.class function defined. We only get here in the
         * default case.
         */
        if (getNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            getNode = insert(GetNodeGen.create(new RNode[4], null, null));
        }
        RFunction func = (RFunction) getNode.execute(frame, "xtfrm.default", RArguments.getEnvironment(frame), RType.Function.getName(), RRuntime.LOGICAL_TRUE);
        return RContext.getEngine().evalFunction(func, x);
    }
}
