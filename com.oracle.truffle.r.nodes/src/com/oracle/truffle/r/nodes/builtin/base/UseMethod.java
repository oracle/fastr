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

import static com.oracle.truffle.r.nodes.builtin.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.builtin.base.UseMethodFactory.*;
import com.oracle.truffle.r.nodes.control.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin(name = "UseMethod", kind = PRIMITIVE)
public abstract class UseMethod extends RBuiltinNode {
    /*
     * TODO: If more than two parameters are passed to UseMethod the extra parameters are ignored
     * and a warning is generated.
     */
    private static final Object[] PARAMETER_NAMES = new Object[]{"generic", "object"};

    @Child UseMethodNode useMethodNode;

    @Override
    public Object[] getParameterNames() {
        return PARAMETER_NAMES;
    }

    @Override
    public RNode[] getArguments() {
        return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance)};
    }

    @Specialization
    public Object execute(VirtualFrame frame, String generic, Object arg) {
        controlVisibility();
        if (useMethodNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            if (arg instanceof RMissing) {
                useMethodNode = insert(new UseMethodGenericOnlyNode());
            } else {
                useMethodNode = insert(new UseMethodGenericAndObjectNode());
            }
        }
        throw new ReturnException(useMethodNode.execute(frame, generic, arg));
    }

    /*
     * If only one argument is passed to UseMethod, the first argument of enclosing function is used
     * to resolve the generic.
     */
    private static final class UseMethodGenericOnlyNode extends UseMethodNode {

        @Override
        public Object execute(VirtualFrame frame, final String generic, Object obj) {
            if (RArguments.getArgumentsLength(frame) == 0 || RArguments.getArgument(frame, 0) == null) {
                CompilerDirectives.transferToInterpreter();
                throw RError.getUnknownFunctionUseMethod(getEncapsulatingSourceSection(), generic, RNull.instance.toString());
            }
            Object enclosingArg = RArguments.getArgument(frame, 0);
            initDispatchedCallNode(generic);
            return dispatchedCallNode.execute(frame, classHierarchyNode.execute(frame, enclosingArg));
        }
    }

    private static final class UseMethodGenericAndObjectNode extends UseMethodNode {

        @Override
        public Object execute(VirtualFrame frame, final String generic, Object obj) {
            initDispatchedCallNode(generic);
            return dispatchedCallNode.execute(frame, classHierarchyNode.execute(frame, obj));
        }
    }

    private abstract static class UseMethodNode extends RNode {

        @Child protected DispatchedCallNode dispatchedCallNode;
        @Child ClassHierarchyNode classHierarchyNode = ClassHierarchyNodeFactory.create(null);
        protected String lastGenericName;

        @Override
        public Object execute(VirtualFrame frame) {
            throw new AssertionError();
        }

        protected void initDispatchedCallNode(final String generic) {
            if (dispatchedCallNode == null || !lastGenericName.equals(generic)) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                DispatchedCallNode dcn = DispatchedCallNode.create(generic, RRuntime.USE_METHOD);
                dispatchedCallNode = dispatchedCallNode == null ? insert(dcn) : dispatchedCallNode.replace(dcn);
                lastGenericName = generic;
            }
        }

        public abstract Object execute(VirtualFrame frame, final String generic, final Object o);
    }

    protected abstract static class ClassHierarchyNode extends UnaryNode {

        public abstract RStringVector execute(VirtualFrame frame, Object arg);

        @Specialization(order = 0)
        public RStringVector getClassHr(RAbstractContainer arg) {
            return arg.getClassHierarchy();
        }

        @Specialization
        public RStringVector getClassHr(@SuppressWarnings("unused") byte arg) {
            return RDataFactory.createStringVector(RRuntime.TYPE_LOGICAL);
        }

        @Specialization
        public RStringVector getClassHr(@SuppressWarnings("unused") String arg) {
            return RDataFactory.createStringVector(RRuntime.TYPE_CHARACTER);
        }

        @Specialization
        public RStringVector getClassHr(@SuppressWarnings("unused") int arg) {
            return RDataFactory.createStringVector(RRuntime.TYPE_INTEGER);
        }

        @Specialization
        public RStringVector getClassHr(@SuppressWarnings("unused") double arg) {
            return RDataFactory.createStringVector(RRuntime.CLASS_DOUBLE, RDataFactory.COMPLETE_VECTOR);
        }

        @Specialization
        public RStringVector getClassHr(@SuppressWarnings("unused") RComplex arg) {
            return RDataFactory.createStringVector(RRuntime.TYPE_COMPLEX);
        }
    }
}
