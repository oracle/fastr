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
import com.oracle.truffle.r.nodes.control.*;
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

    @Child UseMethodRoot useMethodRoot;

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
        if (useMethodRoot == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            useMethodRoot = insert(new UseMethodUninitialized());
        }
        return useMethodRoot.execute(frame, generic, arg);
    }

    private static final class UseMethodUninitialized extends UseMethodRoot {
        @Override
        public Object execute(VirtualFrame frame, final String generic, Object obj) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            return specialize(obj).execute(frame, generic, obj);
        }

        private UseMethodRoot specialize(Object obj) {
            CompilerAsserts.neverPartOfCompilation();
            if (obj instanceof RMissing) {
                return this.replace(new UseMethodGenericOnly());
            }
            return this.replace(new UseMethodGenericNObject());
        }
    }

    /*
     * If only one argument is passed to UseMethod, the first argument of enclosing function is used
     * to resolve the generic.
     */
    private static final class UseMethodGenericOnly extends UseMethodRoot {

        @Override
        public Object execute(VirtualFrame frame, final String generic, Object obj) {
            if (RArguments.getArgumentsLength(frame) == 0 || RArguments.getArgument(frame, 0) == null) {
                CompilerDirectives.transferToInterpreter();
                throw RError.getUnknownFunctionUseMethod(getEncapsulatingSourceSection(), generic, RNull.instance.toString());
            }
            Object enclosingArg = RArguments.getArgument(frame, 0);
            initDispatchedCallNode(generic);
            throw new ReturnException(dispatchedCallNode.execute(frame, getClassHierarchy(enclosingArg)));
        }
    }

    private static final class UseMethodGenericNObject extends UseMethodRoot {

        @Override
        public Object execute(VirtualFrame frame, final String generic, Object obj) {
            initDispatchedCallNode(generic);
            throw new ReturnException(dispatchedCallNode.execute(frame, getClassHierarchy(obj)));
        }
    }

    private static abstract class UseMethodRoot extends RNode {

        @Child protected DispatchedCallNode dispatchedCallNode;
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

        protected RStringVector getClassHierarchy(Object anObj) {
            if (anObj instanceof RAbstractContainer) {
                return ((RAbstractContainer) anObj).getClassHierarchy();
            }
            if (anObj instanceof Byte) {
                return RDataFactory.createStringVector(RRuntime.TYPE_LOGICAL);
            }
            if (anObj instanceof String) {
                return RDataFactory.createStringVector(RRuntime.TYPE_CHARACTER);
            }
            if (anObj instanceof Integer) {
                return RDataFactory.createStringVector(RRuntime.TYPE_INTEGER);
            }
            if (anObj instanceof Double) {
                return RDataFactory.createStringVector(RRuntime.CLASS_DOUBLE, RDataFactory.COMPLETE_VECTOR);
            }
            if (anObj instanceof RComplex) {
                return RDataFactory.createStringVector(RRuntime.TYPE_COMPLEX);
            }
            throw new AssertionError();
        }

        public abstract Object execute(VirtualFrame frame, final String generic, final Object o);
    }
}
