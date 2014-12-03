package com.oracle.truffle.r.nodes.function;

import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

/*
 * Handles unary +, - and ! operators.
 */
public class UnaryOpsGroupDispatchNode extends GroupDispatchNode {

    protected UnaryOpsGroupDispatchNode(final String aGenericName, final CallArgumentsNode callArgNode) {
        super(aGenericName, RGroupGenerics.GROUP_OPS, callArgNode);
    }

    public static UnaryOpsGroupDispatchNode create(final String aGenericName, SourceSection callSrc, final CallArgumentsNode callArgNode) {
        UnaryOpsGroupDispatchNode result = new UnaryOpsGroupDispatchNode(aGenericName, callArgNode);
        result.assignSourceSection(callSrc);
        return result;
    }

    @Override
    protected Object callBuiltin(VirtualFrame frame) {
        initBuiltin(frame);
        Object[] argObject = RArguments.create(builtinFunc, funCallNode.getSourceSection(), new Object[]{evaluatedArgs[0], RMissing.instance});
        return funCallNode.call(frame, builtinFunc.getTarget(), argObject);
    }
}
