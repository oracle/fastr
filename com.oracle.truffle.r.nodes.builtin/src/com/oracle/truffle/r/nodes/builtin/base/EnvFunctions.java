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

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.frame.FrameInstance.FrameAccess;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.builtin.base.EnvFunctionsFactory.CopyNodeFactory;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.nodes.function.PromiseHelperNode.PromiseDeoptimizeFrameNode;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.env.*;

/**
 * Encapsulates all the builtins related to R environments as nested static classes.
 */
public class EnvFunctions {

    protected abstract static class Adapter extends RBuiltinNode {
        protected final BranchProfile errorProfile = BranchProfile.create();
    }

    @RBuiltin(name = "as.environment", kind = PRIMITIVE, parameterNames = {"fun"})
    public abstract static class AsEnvironment extends Adapter {

        @Specialization
        protected REnvironment asEnvironment(REnvironment env) {
            controlVisibility();
            return env;
        }

        @Specialization
        protected REnvironment asEnvironment(VirtualFrame frame, RAbstractDoubleVector posVec) {
            return asEnvironmentInt(frame, (int) posVec.getDataAt(0));
        }

        @Specialization
        protected REnvironment asEnvironmentInt(VirtualFrame frame, RAbstractIntVector posVec) {
            return asEnvironmentInt(frame, posVec.getDataAt(0));
        }

        protected REnvironment asEnvironmentInt(VirtualFrame frame, int pos) {
            controlVisibility();
            if (pos == -1) {
                Frame callerFrame = Utils.getCallerFrame(frame, FrameAccess.MATERIALIZE);
                if (callerFrame == null) {
                    errorProfile.enter();
                    throw RError.error(getEncapsulatingSourceSection(), RError.Message.NO_ENCLOSING_ENVIRONMENT);
                } else {
                    return REnvironment.frameToEnvironment(callerFrame.materialize());
                }
            }
            String[] searchPath = REnvironment.searchPath();
            if (pos == searchPath.length + 1) {
                // although the empty env does not appear in the result of "search", and it is
                // not accessible by name, GnuR allows it to be accessible by index
                return REnvironment.emptyEnv();
            } else if ((pos <= 0) || (pos > searchPath.length + 1)) {
                errorProfile.enter();
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_ARGUMENT, "pos");
            } else {
                return REnvironment.lookupOnSearchPath(searchPath[pos - 1]);
            }
        }

        @Specialization
        protected REnvironment asEnvironment(RAbstractStringVector nameVec) {
            controlVisibility();
            String name = nameVec.getDataAt(0);
            String[] searchPath = REnvironment.searchPath();
            for (String e : searchPath) {
                if (e.equals(name)) {
                    return REnvironment.lookupOnSearchPath(e);
                }
            }
            errorProfile.enter();
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.NO_ITEM_NAMED, name);
        }

    }

    @RBuiltin(name = "emptyenv", kind = PRIMITIVE, parameterNames = {})
    public abstract static class EmptyEnv extends RBuiltinNode {

        @Specialization
        protected REnvironment emptyenv() {
            controlVisibility();
            return REnvironment.emptyEnv();
        }
    }

    @RBuiltin(name = "globalenv", kind = PRIMITIVE, parameterNames = {})
    public abstract static class GlobalEnv extends RBuiltinNode {

        @Specialization
        protected Object globalenv() {
            controlVisibility();
            return REnvironment.globalEnv();
        }
    }

    /**
     * Returns the "package:base" environment.
     */
    @RBuiltin(name = "baseenv", kind = PRIMITIVE, parameterNames = {})
    public abstract static class BaseEnv extends RBuiltinNode {

        @Specialization
        protected Object baseenv() {
            controlVisibility();
            return REnvironment.baseEnv();
        }
    }

    @RBuiltin(name = "parent.env", kind = INTERNAL, parameterNames = {"env"})
    public abstract static class ParentEnv extends Adapter {

        @Specialization
        protected REnvironment parentenv(REnvironment env) {
            controlVisibility();
            if (env == REnvironment.emptyEnv()) {
                errorProfile.enter();
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.EMPTY_NO_PARENT);
            }
            return env.getParent();
        }

    }

    @RBuiltin(name = "parent.env<-", kind = INTERNAL, parameterNames = {"env", ""})
    // 2nd parameter is "value", but should not be matched to so it's empty
    public abstract static class SetParentEnv extends Adapter {

        @Specialization
        protected REnvironment setParentenv(REnvironment env, REnvironment parent) {
            controlVisibility();
            if (env == REnvironment.emptyEnv()) {
                errorProfile.enter();
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.CANNOT_SET_PARENT);
            }
            env.setParent(parent);
            return env;
        }

    }

    @RBuiltin(name = "is.environment", kind = PRIMITIVE, parameterNames = {"x"})
    public abstract static class IsEnvironment extends RBuiltinNode {

        @Specialization
        protected byte isEnvironment(Object env) {
            controlVisibility();
            return env instanceof REnvironment ? RRuntime.LOGICAL_TRUE : RRuntime.LOGICAL_FALSE;
        }
    }

    @RBuiltin(name = "environment", kind = INTERNAL, parameterNames = {"fun"})
    public abstract static class Environment extends RBuiltinNode {

        private final ConditionProfile isFunctionProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile createEnvironmentProfile = ConditionProfile.createBinaryProfile();
        private final PromiseDeoptimizeFrameNode deoptFrameNode = new PromiseDeoptimizeFrameNode();

        @Specialization
        protected Object environment(VirtualFrame frame, @SuppressWarnings("unused") RNull x) {
            controlVisibility();
            Frame callerFrame = Utils.getCallerFrame(frame, FrameAccess.MATERIALIZE);
            MaterializedFrame matFrame = callerFrame.materialize();
            deoptFrameNode.deoptimizeFrame(matFrame);
            return REnvironment.frameToEnvironment(matFrame);
        }

        /**
         * Returns the environment that {@code func} was created in. N.B. In current Truffle we
         * cannot both have a specialization for {@link RFunction} and one for {@link Object}, but
         * an object that is not an {@link RFunction} is legal and must return {@code NULL}.
         */
        @Specialization
        protected Object environment(Object funcArg) {
            controlVisibility();
            if (isFunctionProfile.profile(funcArg instanceof RFunction)) {
                RFunction func = (RFunction) funcArg;
                Frame enclosing = func.getEnclosingFrame();
                REnvironment env = RArguments.getEnvironment(enclosing);
                if (createEnvironmentProfile.profile(env == null)) {
                    return REnvironment.createEnclosingEnvironments(enclosing.materialize());
                } else {
                    return env;
                }
            } else {
                // Not an error according to GnuR
                return RNull.instance;
            }
        }

    }

    @RBuiltin(name = "environmentName", kind = INTERNAL, parameterNames = {"fun"})
    public abstract static class EnvironmentName extends RBuiltinNode {

        @Specialization
        protected String environmentName(REnvironment env) {
            controlVisibility();
            return env.getName();
        }

        @Specialization
        protected String environmentName(@SuppressWarnings("unused") Object env) {
            controlVisibility();
            // Not an error according to GnuR
            return "";
        }
    }

    @RBuiltin(name = "new.env", kind = INTERNAL, parameterNames = {"hash", "parent", "size"})
    public abstract static class NewEnv extends RBuiltinNode {
        @Specialization
        protected REnvironment newEnv(@SuppressWarnings("unused") byte hash, REnvironment parent, int size) {
            controlVisibility();
            // Ignore hash == FALSE
            return RDataFactory.createNewEnv(parent, size);
        }
    }

    @RBuiltin(name = "search", kind = INTERNAL, parameterNames = {})
    public abstract static class Search extends RBuiltinNode {
        @Specialization
        protected RStringVector search() {
            return RDataFactory.createStringVector(REnvironment.searchPath(), RDataFactory.COMPLETE_VECTOR);
        }
    }

    @RBuiltin(name = "lockEnvironment", kind = INTERNAL, parameterNames = {"env", "bindings"})
    public abstract static class LockEnvironment extends RInvisibleBuiltinNode {

        @Specialization
        protected Object lockEnvironment(REnvironment env, byte bindings) {
            controlVisibility();
            env.lock(bindings == RRuntime.LOGICAL_TRUE);
            return RNull.instance;
        }

    }

    @RBuiltin(name = "environmentIsLocked", kind = INTERNAL, parameterNames = {"env"})
    public abstract static class EnvironmentIsLocked extends RBuiltinNode {
        @Specialization
        protected Object lockEnvironment(REnvironment env) {
            controlVisibility();
            return RDataFactory.createLogicalVectorFromScalar(env.isLocked());
        }

    }

    private interface BindingErrorMixin {
        default void check(SourceSection source, Object sym, Object env) throws RError {
            if (!(sym instanceof RSymbol)) {
                throw RError.error(source, RError.Message.NOT_A_SYMBOL);
            }
            if (!(env instanceof REnvironment)) {
                throw RError.error(source, RError.Message.NOT_AN_ENVIRONMENT);
            }
        }
    }

    @RBuiltin(name = "lockBinding", kind = INTERNAL, parameterNames = {"sym", "env"})
    public abstract static class LockBinding extends RInvisibleBuiltinNode implements BindingErrorMixin {
        @Specialization
        protected Object lockBinding(RSymbol sym, REnvironment env) {
            controlVisibility();
            env.lockBinding(sym.getName());
            return RNull.instance;
        }

        @Fallback
        protected Object lockBinding(Object sym, Object env) {
            check(getEncapsulatingSourceSection(), sym, env);
            return RNull.instance;
        }
    }

    @RBuiltin(name = "unlockBinding", kind = INTERNAL, parameterNames = {"sym", "env"})
    public abstract static class UnlockBinding extends RInvisibleBuiltinNode implements BindingErrorMixin {
        @Specialization
        protected Object unlockBinding(RSymbol sym, REnvironment env) {
            controlVisibility();
            env.unlockBinding(sym.getName());
            return RNull.instance;
        }

        @Fallback
        protected Object unlockBinding(Object sym, Object env) {
            check(getEncapsulatingSourceSection(), sym, env);
            return RNull.instance;
        }
    }

    @RBuiltin(name = "bindingIsLocked", kind = INTERNAL, parameterNames = {"sym", "env"})
    public abstract static class BindingIsLocked extends RBuiltinNode implements BindingErrorMixin {
        @Specialization
        protected Object bindingIsLocked(RSymbol sym, REnvironment env) {
            controlVisibility();
            return RDataFactory.createLogicalVectorFromScalar(env.bindingIsLocked(sym.getName()));
        }

        @Fallback
        protected Object bindingIsLocked(Object sym, Object env) {
            check(getEncapsulatingSourceSection(), sym, env);
            return RNull.instance;
        }
    }

    @RBuiltin(name = "makeActiveBinding", kind = INTERNAL, parameterNames = {"sym", "fun", "env"})
    public abstract static class MakeActiveBinding extends RInvisibleBuiltinNode {
        @SuppressWarnings("unused")
        @Specialization
        protected Object makeActiveBinding(Object sym, Object fun, Object env) {
            // TODO implement
            controlVisibility();
            throw RError.nyi(getEncapsulatingSourceSection(), "makeActiveBinding not implemented");
        }
    }

    @RBuiltin(name = "bindingIsActive", kind = INTERNAL, parameterNames = {"sym", "env"})
    public abstract static class BindingIsActive extends RInvisibleBuiltinNode {
        @SuppressWarnings("unused")
        @Specialization
        protected Object bindingIsActive(Object sym, Object fun, Object env) {
            // TODO implement
            controlVisibility();
            return RDataFactory.createLogicalVectorFromScalar(false);
        }
    }

    @RBuiltin(name = "env2list", kind = INTERNAL, parameterNames = {"x", "all.names"})
    public abstract static class EnvToList extends RBuiltinNode {

        @Child private CopyNode copy;

        private Object copy(VirtualFrame frame, Object operand) {
            if (copy == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                copy = insert(CopyNodeFactory.create(null));
            }
            return copy.execute(frame, operand);
        }

        @Specialization
        protected RList envToListAllNames(VirtualFrame frame, REnvironment env, RAbstractLogicalVector allNamesVec) {
            // according to the docs it is expected to be slow as it creates a copy of environment
            // objects
            controlVisibility();
            boolean allNames = allNamesVec.getLength() == 0 || allNamesVec.getDataAt(0) == RRuntime.LOGICAL_FALSE ? false : true;
            RStringVector keys = env.ls(allNames, null);
            Object[] data = new Object[keys.getLength()];
            for (int i = 0; i < data.length; i++) {
                // TODO: not all types are handled (e.g. copying environments)
                String key = keys.getDataAt(i);
                Object o = env.get(key);
                data[i] = copy(frame, o);
            }
            return RDataFactory.createList(data, keys.getLength() == 0 ? null : keys.copy());
        }

    }

    @NodeChild("operand")
    protected abstract static class CopyNode extends RNode {

        protected abstract Object execute(VirtualFrame frame, Object o);

        @Child private CopyNode recursiveCopy;
        @Child private PromiseHelperNode promiseHelper;

        private Object recursiveCopy(VirtualFrame frame, Object operand) {
            if (recursiveCopy == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                recursiveCopy = insert(CopyNodeFactory.create(null));
            }
            return recursiveCopy.execute(frame, operand);
        }

        @Specialization
        RNull copy(RNull n) {
            return n;
        }

        @Specialization
        RAbstractVector copy(RAbstractVector v) {
            return v.copy();
        }

        @Specialization
        RLanguage copy(RLanguage l) {
            return l.copy();
        }

        @Specialization
        RFunction copy(RFunction f) {
            return f.copy();
        }

        @Specialization
        RSymbol copy(RSymbol s) {
            return s;
        }

        @Specialization
        RSymbol copy(@SuppressWarnings("unused") RMissing m) {
            return RDataFactory.createSymbol(" ");
        }

        @Specialization
        Object copy(VirtualFrame frame, RPromise promise) {
            if (promiseHelper == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                promiseHelper = insert(new PromiseHelperNode());
            }
            return recursiveCopy(frame, promiseHelper.evaluate(frame, promise));
        }

        @Fallback
        Object copy(@SuppressWarnings("unused") Object o) {
            throw Utils.nyi("copying of object in the environment not supported");
        }

    }

}
