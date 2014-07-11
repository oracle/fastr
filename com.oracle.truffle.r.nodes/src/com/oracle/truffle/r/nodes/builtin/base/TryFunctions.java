package com.oracle.truffle.r.nodes.builtin.base;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.RBuiltin.*;
import com.oracle.truffle.r.runtime.data.*;

/**
 * Temporary substitutions that just evaluates the expression for package loading and assume no
 * errors or finallys.
 */
public class TryFunctions {
    @RBuiltin(name = "try", kind = RBuiltinKind.SUBSTITUTE, nonEvalArgs = {-1})
    public abstract static class Try extends RBuiltinNode {

        private static final String[] PARAMETER_NAMES = new String[]{"expr", "silent"};

        @Override
        public String[] getParameterNames() {
            return PARAMETER_NAMES;
        }

        @Override
        public RNode[] getParameterValues() {
            return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(RRuntime.LOGICAL_FALSE)};
        }

        @Specialization
        public Object doTry(VirtualFrame frame, RPromise expr, @SuppressWarnings("unused") byte silent) {
            controlVisibility();
            return expr.getValue(frame);
        }
    }

    @RBuiltin(name = "tryCatch", kind = RBuiltinKind.SUBSTITUTE, nonEvalArgs = {-1}, lastParameterKind = LastParameterKind.VAR_ARGS_ALWAYS_ARRAY)
    public abstract static class TryCatch extends RBuiltinNode {

        // Ignoring finally completely
        private static final String[] PARAMETER_NAMES = new String[]{"expr", "..."};

        @Override
        public String[] getParameterNames() {
            return PARAMETER_NAMES;
        }

        @Override
        public RNode[] getParameterValues() {
            return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(EMPTY_OBJECT_ARRAY)};
        }

        @Specialization
        public Object doTryCatch(VirtualFrame frame, RPromise expr, RPromise arg) {
            return doTryCatch(frame, expr, new Object[]{arg});
        }

        @SuppressWarnings("unused")
        @Specialization
        public Object doTryCatch(VirtualFrame frame, RPromise expr, Object[] args) {
            controlVisibility();
            return expr.getValue(frame);
        }
    }
}
