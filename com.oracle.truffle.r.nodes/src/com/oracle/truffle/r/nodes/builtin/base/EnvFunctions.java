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

import static com.oracle.truffle.r.nodes.builtin.RBuiltinKind.*;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.frame.FrameInstance.FrameAccess;
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

    @RBuiltin(name = "as.environment", kind = PRIMITIVE)
    public abstract static class AsEnvironment extends RBuiltinNode {

        @Specialization
        public REnvironment asEnvironment(REnvironment env) {
            controlVisibility();
            return env;
        }

        @Specialization
        public REnvironment asEnvironment(double dpos) {
            controlVisibility();
            return asEnvironment((int) dpos);
        }

        @Specialization
        public REnvironment asEnvironment(int pos) {
            controlVisibility();
            if (pos == -1) {
                Frame callerFrame = Utils.getCallerFrame(FrameAccess.READ_ONLY);
                if (callerFrame == null) {
                    throw RError.getGenericError(getEncapsulatingSourceSection(), "no enclosing environment");
                } else {
                    return frameToEnvironment(callerFrame);
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
                return REnvironment.lookupOnSearchPath(searchPath[pos - 1]);
            }
        }

        @Specialization
        public REnvironment asEnvironment(String name) {
            controlVisibility();
            String[] searchPath = REnvironment.searchPath();
            for (String e : searchPath) {
                if (e.equals(name)) {
                    return REnvironment.lookupOnSearchPath(e);
                }
            }
            throw RError.getGenericError(getEncapsulatingSourceSection(), "no item named '" + name + "' on the search list");
        }

        @Specialization(order = 100)
        public REnvironment asEnvironment(@SuppressWarnings("unused") Object x) {
            throw RError.getGenericError(getEncapsulatingSourceSection(), " invalid object for 'as.environment'");
        }

    }

    @RBuiltin(name = "emptyenv", kind = PRIMITIVE)
    public abstract static class EmptyEnv extends RBuiltinNode {

        @Specialization
        public REnvironment emptyenv() {
            controlVisibility();
            return REnvironment.emptyEnv();
        }
    }

    @RBuiltin(name = "globalenv", kind = PRIMITIVE)
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
    @RBuiltin(name = "baseenv", kind = PRIMITIVE)
    public abstract static class BaseEnv extends RBuiltinNode {

        @Specialization
        public Object baseenv() {
            controlVisibility();
            return REnvironment.baseEnv();
        }
    }

    @RBuiltin(name = "parent.env", kind = INTERNAL)
    public abstract static class ParentEnv extends RBuiltinNode {

        @Specialization
        public REnvironment parentenv(REnvironment env) {
            controlVisibility();
            if (env == REnvironment.emptyEnv()) {
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

    @RBuiltin(name = "parent.env<-", kind = INTERNAL)
    public abstract static class SetParentEnv extends RBuiltinNode {

        @Specialization
        public REnvironment setParentenv(REnvironment env, REnvironment parent) {
            controlVisibility();
            if (env == REnvironment.emptyEnv()) {
                throw RError.getGenericError(getEncapsulatingSourceSection(), "cannot set the parent of the empty environment");
            }
            env.setParent(parent);
            return env;
        }

        @Specialization(order = 100)
        public REnvironment setParentenv(@SuppressWarnings("unused") Object x, @SuppressWarnings("unused") Object y) {
            controlVisibility();
            throw RError.getGenericError(getEncapsulatingSourceSection(), "argument is not an environment");
        }
    }

    @RBuiltin(name = "is.environment", kind = PRIMITIVE)
    public abstract static class IsEnvironment extends RBuiltinNode {

        @Specialization
        public byte isEnvironment(Object env) {
            controlVisibility();
            return env instanceof REnvironment ? RRuntime.LOGICAL_TRUE : RRuntime.LOGICAL_FALSE;
        }
    }

    @RBuiltin(name = "environment", kind = INTERNAL)
    public abstract static class Environment extends RBuiltinNode {

        @Specialization(order = 0)
        public Object environment(@SuppressWarnings("unused") RNull x) {
            controlVisibility();
            Frame callerFrame = Utils.getCallerFrame(FrameAccess.READ_ONLY);
            return frameToEnvironment(callerFrame);
        }

        /**
         * Returns the environment that {@code func} was created in.
         */
        @Specialization(order = 1)
        public REnvironment environment(RFunction func) {
            controlVisibility();
            Frame enclosing = func.getEnclosingFrame();
            REnvironment env = RArguments.getEnvironment(enclosing);
            return env == null ? lexicalChain(enclosing) : env;
        }

        @Specialization(order = 100)
        public RNull environment(@SuppressWarnings("unused") Object x) {
            controlVisibility();
            // Not an error according to GnuR
            return RNull.instance;
        }
    }

    @RBuiltin(name = "environmentName", kind = INTERNAL)
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

    @RBuiltin(name = "new.env", kind = SUBSTITUTE)
    // TOOD INTERNAL
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
            arguments[2] = CastIntegerNodeFactory.create(arguments[2], true, false, false);
            return arguments;
        }

        @Specialization
        @SuppressWarnings("unused")
        public REnvironment newEnv(VirtualFrame frame, byte hash, RMissing parent, int size) {
            controlVisibility();
            // FIXME don't ignore hash parameter
            return new REnvironment.NewEnv(frameToEnvironment(frame), size);
        }

        @Specialization
        public REnvironment newEnv(@SuppressWarnings("unused") VirtualFrame frame, @SuppressWarnings("unused") byte hash, REnvironment parent, int size) {
            controlVisibility();
            // FIXME don't ignore hash parameter
            return new REnvironment.NewEnv(parent, size);
        }
    }

    @RBuiltin(name = "search", kind = SUBSTITUTE)
    // TODO INTERNAL
    public abstract static class Search extends RBuiltinNode {
        @Specialization
        public RStringVector search() {
            return RDataFactory.createStringVector(REnvironment.searchPath(), RDataFactory.COMPLETE_VECTOR);
        }
    }

    @RBuiltin(name = "lockEnvironment", kind = INTERNAL)
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

    @RBuiltin(name = "environmentIsLocked", kind = INTERNAL)
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

    @RBuiltin(name = "lockBinding", kind = INTERNAL)
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

    @RBuiltin(name = "unlockBinding", kind = INTERNAL)
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

    @RBuiltin(name = "bindingIsLocked", kind = INTERNAL)
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

    @RBuiltin(name = "makeActiveBinding", kind = INTERNAL)
    public abstract static class MakeActiveBinding extends RInvisibleBuiltinNode {
        @SuppressWarnings("unused")
        @Specialization(order = 0)
        public Object makeActiveBinding(Object sym, Object fun, Object env) {
            // TODO implement
            controlVisibility();
            throw RError.getGenericError(getEncapsulatingSourceSection(), "makeActiveBinding not implemented");
        }
    }

    @RBuiltin(name = "bindingIsActive", kind = INTERNAL)
    public abstract static class BindingIsActive extends RInvisibleBuiltinNode {
        @SuppressWarnings("unused")
        @Specialization(order = 0)
        public Object bindingIsActive(Object sym, Object fun, Object env) {
            // TODO implement
            controlVisibility();
            return RDataFactory.createLogicalVectorFromScalar(false);
        }
    }

    /**
     * Check if a frame corresponds to a function. If there is no function associated with the
     * frame, then is it one of the package environments. Fortunately, we do not have to do a search
     * as, in this case, the {@link REnvironment} value is also stored in the frame.
     *
     * @return ({code null) if this is a function frame, else the associated environment
     */
    static REnvironment checkNonFunctionFrame(Frame frame) {
        RFunction callerFunc = RArguments.getFunction(frame);
        if (callerFunc == null) {
            REnvironment env = RArguments.getEnvironment(frame);
            assert env != null;
            return env;
        } else {
            return null;
        }
    }

    /**
     * Converts a {@link Frame} to an {@link REnvironment}.
     */
    static REnvironment frameToEnvironment(Frame frame) {
        REnvironment env = checkNonFunctionFrame(frame);
        if (env == null) {
            env = lexicalChain(frame);
        }
        return env;
    }

    /**
     * When functions are defined, the associated {@link FunctionDefinitionNode} contains an
     * {@link com.oracle.truffle.r.runtime.REnvironment.FunctionDefinition} environment instance
     * whose parent is the lexically enclosing environment. This chain can be followed back to
     * whichever "base" (i.e. non-function) environment the outermost function was defined in, e.g.
     * "global" or "base". The purpose of this method is to create an analogous lexical parent chain
     * of {@link com.oracle.truffle.r.runtime.REnvironment.Function} instances with the correct
     * {@link MaterializedFrame}.
     */
    static REnvironment lexicalChain(Frame frame) {
        REnvironment env = checkNonFunctionFrame(frame);
        if (env == null) {
            // parent is the env of the enclosing frame
            env = REnvironment.Function.create(lexicalChain(RArguments.getEnclosingFrame(frame)), frame.materialize());
        }
        return env;
    }

    private static RError notAnEnvironment(RBuiltinNode node) {
        return RError.getGenericError(node.getEncapsulatingSourceSection(), "not an envionment");
    }

}
