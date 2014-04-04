/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.impl.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

/**
 * Encapsulates all the builtins related to R environments as nested static classes.
 */
public class EnvFunctions {

    @RBuiltin("as.environment")
    public abstract static class AsEnvironment extends RBuiltinNode {

        @Specialization
        public REnvironment asEnvironment(REnvironment env) {
            return env;
        }

        @Specialization
        public REnvironment asEnvironment(VirtualFrame frame, double dpos) {
            return asEnvironment(frame, (int) dpos);
        }

        @Specialization
        public REnvironment asEnvironment(VirtualFrame frame, int pos) {
            if (pos == -1) {
                PackedFrame caller = frame.getCaller();
                if (caller == null) {
                    throw RError.getGenericError(getEncapsulatingSourceSection(), "no enclosing environment");
                } else {
                    // TODO handle parent properly
                    PackedFrame callerCaller = ((VirtualFrame) caller.unpack()).getCaller();
                    if (callerCaller == null) {
                        return REnvironment.globalEnv();
                    } else {
                        throw RError.getGenericError(getEncapsulatingSourceSection(), "not implemented");
                    }
                }

            }
            String[] searchPath = REnvironment.searchPath();
            if (pos == searchPath.length + 1) {
                // although the empty env does not appear in the result of "search", and it is
                // not accessible by name, GnuR allows it to be accessible by index
                return REnvironment.emptyEnv();
            } else if ((pos <= 0) || (pos > searchPath.length + 1)) {
                throw RError.getGenericError(getEncapsulatingSourceSection(), "invalid 'pos' argument");
            } else {
                return REnvironment.lookupBySearchName(searchPath[pos - 1]);
            }
        }

        @Specialization
        public REnvironment asEnvironment(String name) {
            String[] searchPath = REnvironment.searchPath();
            for (String e : searchPath) {
                if (e.equals(name)) {
                    return REnvironment.lookupBySearchName(e);
                }
            }
            throw RError.getGenericError(getEncapsulatingSourceSection(), "no item named '" + name + "' on the search list");
        }

        @Specialization(order = 100)
        public REnvironment asEnvironment(@SuppressWarnings("unused") Object x) {
            throw RError.getGenericError(getEncapsulatingSourceSection(), " invalid object for 'as.environment'");
        }

    }

    @RBuiltin("emptyenv")
    public abstract static class EmptyEnv extends RBuiltinNode {

        @Specialization
        public REnvironment emptyenv() {
            controlVisibility();
            return REnvironment.emptyEnv();
        }
    }

    @RBuiltin({"globalenv"})
    public abstract static class GlobalEnv extends RBuiltinNode {

        @Specialization
        public Object globalenv() {
            controlVisibility();
            return REnvironment.globalEnv();
        }
    }

    /**
     * Returns the "package:base" environment.
     */
    @RBuiltin("baseenv")
    public abstract static class BaseEnv extends RBuiltinNode {

        @Specialization
        public Object baseenv() {
            controlVisibility();
            return REnvironment.baseEnv();
        }
    }

    @RBuiltin(".Internal.parent.env")
    public abstract static class ParentEnv extends RBuiltinNode {

        @Specialization
        public REnvironment parentenv(REnvironment env) {
            controlVisibility();
            if (env == REnvironment.emptyEnv()) {
                controlVisibility();
                throw RError.getGenericError(getEncapsulatingSourceSection(), "the empty environment has no parent");
            }
            return env.getParent();
        }

        @Specialization(order = 100)
        public REnvironment parentenv(@SuppressWarnings("unused") Object x) {
            controlVisibility();
            throw RError.getGenericError(getEncapsulatingSourceSection(), "argument is not an environment");
        }
    }

    @RBuiltin("is.environment")
    public abstract static class IsEnvironment extends RBuiltinNode {

        @Specialization
        public byte isEnvironment(Object env) {
            controlVisibility();
            return env instanceof REnvironment ? RRuntime.LOGICAL_TRUE : RRuntime.LOGICAL_FALSE;
        }
    }

    @RBuiltin(".Internal.environment")
    public abstract static class Environment extends RBuiltinNode {

        @Specialization(order = 0)
        public Object environment(VirtualFrame frame, @SuppressWarnings("unused") RNull x) {
            controlVisibility();
            // Owing to the .Internal, the caller is environment(),
            // so we need to find the caller of that
            VirtualFrame envFrame = (VirtualFrame) frame.getCaller().unpack();
            return enclosingEnvironment(this, envFrame);
        }

        /**
         * Returns the environment that {@code func} was created in.
         */
        @Specialization(order = 1)
        public REnvironment environment(RFunction func) {
            controlVisibility();
            /*
             * This is the simplest way to access the enclosing environment, as it already exists in
             * the FunctionDefinitionNode (although, obviously, without a frame). We could instead
             * use the MaterializedFrame in the RFunction instance and create a new
             * REnvironment.Function instance. That would remove the need to special case builtins.
             */
            RootNode rootNode = ((DefaultCallTarget) func.getTarget()).getRootNode();
            if (rootNode instanceof RBuiltinRootNode) {
                return REnvironment.baseNamespaceEnv();
            } else {
                REnvironment funcEnv = ((FunctionDefinitionNode) rootNode).getDescriptor();
                return funcEnv.getParent();
            }
        }

        @Specialization(order = 100)
        public RNull environment(@SuppressWarnings("unused") Object x) {
            controlVisibility();
            // Not an error according to GnuR
            return RNull.instance;
        }
    }

    @RBuiltin(".Internal.environmentName")
    public abstract static class EnvironmentName extends RBuiltinNode {

        @Specialization
        public String environmentName(REnvironment env) {
            controlVisibility();
            return env.getName();
        }

        @Specialization(order = 100)
        public String environmentName(@SuppressWarnings("unused") Object env) {
            controlVisibility();
            // Not an error according to GnuR
            return "";
        }
    }

    @RBuiltin("new.env")
    public abstract static class NewEnv extends RBuiltinNode {

        private static final Object[] PARAMETER_NAMES = new Object[]{"hash", "parent", "size"};

        @Override
        public Object[] getParameterNames() {
            return PARAMETER_NAMES;
        }

        @Override
        public RNode[] getParameterValues() {
            return new RNode[]{ConstantNode.create(RRuntime.LOGICAL_TRUE), ConstantNode.create(RMissing.instance), ConstantNode.create(29)};
        }

        @CreateCast("arguments")
        protected RNode[] castStatusArgument(RNode[] arguments) {
            // size argument is at index 2, and an int
            arguments[2] = CastIntegerNodeFactory.create(arguments[2], true, false);
            return arguments;
        }

        @Specialization
        @SuppressWarnings("unused")
        public REnvironment newEnv(VirtualFrame frame, byte hash, RMissing parent, int size) {
            controlVisibility();
            // FIXME don't ignore hash parameter
            REnvironment.Function currentEnv = REnvironment.Function.create(null, frame.materialize());
            return new REnvironment.NewEnv(enclosingEnvironment(this, frame), REnvironment.UNNAMED, size);
        }

        @Specialization
        public REnvironment newEnv(@SuppressWarnings("unused") VirtualFrame frame, @SuppressWarnings("unused") byte hash, REnvironment parent, int size) {
            controlVisibility();
            // FIXME don't ignore hash parameter
            return new REnvironment.NewEnv(parent, REnvironment.UNNAMED, size);
        }
    }

    @RBuiltin("search")
    public abstract static class Search extends RBuiltinNode {
        @Specialization
        public RStringVector search() {
            return RDataFactory.createStringVector(REnvironment.searchPath(), RDataFactory.COMPLETE_VECTOR);
        }
    }

    /**
     * TODO implement.
     */
    @RBuiltin(".Internal.parent.frame")
    public abstract static class ParentFrame extends RBuiltinNode {
        @Specialization
        public Object parentFrame(@SuppressWarnings("unused") Object x) {
            controlVisibility();
            return RNull.instance;
        }
    }

    @RBuiltin(".Internal.lockEnvironment")
    public abstract static class LockEnvironment extends RInvisibleBuiltinNode {
        @Specialization(order = 0)
        public Object lockEnvironment(REnvironment env, byte bindings) {
            controlVisibility();
            env.lock(bindings == RRuntime.LOGICAL_TRUE);
            return RNull.instance;
        }

        @SuppressWarnings("unused")
        @Specialization(order = 100)
        public Object lockEnvironment(Object x, byte y) {
            controlVisibility();
            throw notAnEnvironment(this);
        }
    }

    @RBuiltin(".Internal.environmentIsLocked")
    public abstract static class EnvironmentIsLocked extends RBuiltinNode {
        @Specialization(order = 0)
        public Object lockEnvironment(REnvironment env) {
            controlVisibility();
            return RDataFactory.createLogicalVectorFromScalar(env.isLocked());
        }

        @Specialization(order = 100)
        public Object lockEnvironment(@SuppressWarnings("unused") Object env) {
            controlVisibility();
            throw notAnEnvironment(this);
        }
    }

    @RBuiltin(".Internal.lockBinding")
    public abstract static class LockBinding extends RInvisibleBuiltinNode {
        @Specialization(order = 0)
        public Object lockBinding(String sym, REnvironment env) {
            controlVisibility();
            env.lockBinding(sym);
            return RNull.instance;
        }

        @SuppressWarnings("unused")
        @Specialization(order = 100)
        public Object lockBinding(Object x, Object y) {
            controlVisibility();
            throw RError.getGenericError(getEncapsulatingSourceSection(), "invalid or unimplemented arguments");
        }
    }

    @RBuiltin(".Internal.unlockBinding")
    public abstract static class UnlockBinding extends RInvisibleBuiltinNode {
        @Specialization(order = 0)
        public Object unlockBinding(String sym, REnvironment env) {
            controlVisibility();
            env.unlockBinding(sym);
            return RNull.instance;
        }

        @SuppressWarnings("unused")
        @Specialization(order = 100)
        public Object unlockBinding(Object x, Object y) {
            controlVisibility();
            throw RError.getGenericError(getEncapsulatingSourceSection(), "invalid or unimplemented arguments");
        }
    }

    @RBuiltin(".Internal.bindingIsLocked")
    public abstract static class BindingIsLocked extends RBuiltinNode {
        @Specialization(order = 0)
        public Object bindingIsLocked(String sym, REnvironment env) {
            controlVisibility();
            return RDataFactory.createLogicalVectorFromScalar(env.bindingIsLocked(sym));
        }

        @SuppressWarnings("unused")
        @Specialization(order = 100)
        public Object bindingIsLocked(Object x, Object y) {
            controlVisibility();
            throw RError.getGenericError(getEncapsulatingSourceSection(), "invalid or unimplemented arguments");
        }
    }

    @RBuiltin(".Internal.makeActiveBinding")
    public abstract static class MakeActiveBinding extends RInvisibleBuiltinNode {
        @SuppressWarnings("unused")
        @Specialization(order = 0)
        public Object makeActiveBinding(Object sym, Object fun, Object env) {
            // TODO implement
            throw RError.getGenericError(getEncapsulatingSourceSection(), "makeActiveBinding not implemented");
        }
    }

    @RBuiltin(".Internal.bindingIsActive")
    public abstract static class BindingIsActive extends RInvisibleBuiltinNode {
        @SuppressWarnings("unused")
        @Specialization(order = 0)
        public Object bindingIsActive(Object sym, Object fun, Object env) {
            // TODO implement
            return RDataFactory.createLogicalVectorFromScalar(false);
        }
    }

    private static REnvironment enclosingEnvironment(RBuiltinNode node, VirtualFrame frame) {
        PackedFrame callerFrame = frame.getCaller();
        if (callerFrame == null) {
            // invoked at the top level
            return REnvironment.globalEnv();
        } else {
            // in some function, needs its lexically enclosing environment
            // currently, we do not have a way to map from a VirtualFrame to the associated function
            throw RError.getGenericError(node.getEncapsulatingSourceSection(), "unable to locate enclosing environment");
        }

    }

    private static RError notAnEnvironment(RBuiltinNode node) {
        return RError.getGenericError(node.getEncapsulatingSourceSection(), "not an envionment");
    }

}
