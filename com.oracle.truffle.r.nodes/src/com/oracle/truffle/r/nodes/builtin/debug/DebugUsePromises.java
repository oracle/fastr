package com.oracle.truffle.r.nodes.builtin.debug;

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

@RBuiltin(name = "debug.use.promises", kind = PRIMITIVE)
public abstract class DebugUsePromises extends RInvisibleBuiltinNode {
    private static final Object[] PARAMETER_NAMES = new Object[]{"function"};

    @Override
    public Object[] getParameterNames() {
        return PARAMETER_NAMES;
    }

    @Override
    public RNode[] getParameterValues() {
        return new RNode[]{ConstantNode.create(RMissing.instance)};
    }

    @Specialization
    public Object debugPromise(RFunction function) {
        controlVisibility();
        function.setUsePromises();
        return RNull.instance;
    }

}
