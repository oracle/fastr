/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.runtime.RBuiltinKind.INTERNAL;
import static com.oracle.truffle.r.runtime.RBuiltinKind.PRIMITIVE;
import static com.oracle.truffle.r.runtime.RDispatch.INTERNAL_GENERIC;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameInstance.FrameAccess;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.RRootNode;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.RList2EnvNode;
import com.oracle.truffle.r.nodes.builtin.base.EnvFunctionsFactory.CopyNodeGen;
import com.oracle.truffle.r.nodes.function.GetCallerFrameNode;
import com.oracle.truffle.r.nodes.function.PromiseHelperNode;
import com.oracle.truffle.r.nodes.function.PromiseHelperNode.PromiseDeoptimizeFrameNode;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RVisibility;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.VirtualEvalFrame;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RAttributeProfiles;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RLanguage;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.data.RS4Object;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.env.frame.FrameSlotChangeMonitor;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
import com.oracle.truffle.r.runtime.nodes.RNode;

/**
 * Encapsulates all the builtins related to R environments as nested static classes.
 */
public class EnvFunctions {

    protected abstract static class Adapter extends RBuiltinNode {
        protected final BranchProfile errorProfile = BranchProfile.create();
    }

    @RBuiltin(name = "as.environment", kind = PRIMITIVE, parameterNames = {"fun"}, dispatch = INTERNAL_GENERIC)
    public abstract static class AsEnvironment extends Adapter {

        @Specialization
        protected REnvironment asEnvironment(@SuppressWarnings("unused") RNull rnull) {
            throw RError.error(this, RError.Message.AS_ENV_NULL_DEFUNCT);
        }

        @Specialization
        protected REnvironment asEnvironment(REnvironment env) {
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

        private REnvironment asEnvironmentInt(VirtualFrame frame, int pos) {
            if (pos == -1) {
                Frame callerFrame = Utils.getCallerFrame(frame, FrameAccess.MATERIALIZE);
                if (callerFrame == null) {
                    errorProfile.enter();
                    throw RError.error(this, RError.Message.NO_ENCLOSING_ENVIRONMENT);
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
                throw RError.error(this, RError.Message.INVALID_ARGUMENT, "pos");
            } else {
                return REnvironment.lookupOnSearchPath(searchPath[pos - 1]);
            }
        }

        @Specialization
        protected REnvironment asEnvironment(RAbstractStringVector nameVec) {
            String name = nameVec.getDataAt(0);
            String[] searchPath = REnvironment.searchPath();
            for (String e : searchPath) {
                if (e.equals(name)) {
                    return REnvironment.lookupOnSearchPath(e);
                }
            }
            errorProfile.enter();
            throw RError.error(this, RError.Message.NO_ITEM_NAMED, name);
        }

        @Specialization
        protected REnvironment asEnvironment(RList list, //
                        @Cached("new()") RList2EnvNode list2Env) {
            REnvironment env = RDataFactory.createNewEnv(null);
            env.setParent(REnvironment.emptyEnv());
            return list2Env.execute(list, env);
        }

        @Specialization
        protected Object asEnvironment(RS4Object obj) {
            // generic dispatch tried already
            Object xData = obj.getAttr(RRuntime.DOT_XDATA);
            if (xData == null || !(xData instanceof REnvironment)) {
                throw RError.error(this, RError.Message.S4OBJECT_NX_ENVIRONMENT);
            } else {
                return xData;
            }
        }

        @Fallback
        protected REnvironment asEnvironment(@SuppressWarnings("unused") Object object) {
            throw RError.error(this, RError.Message.INVALID_OBJECT);
        }
    }

    @RBuiltin(name = "emptyenv", kind = PRIMITIVE, parameterNames = {})
    public abstract static class EmptyEnv extends RBuiltinNode {

        @Specialization
        protected REnvironment emptyenv() {
            return REnvironment.emptyEnv();
        }
    }

    @RBuiltin(name = "globalenv", kind = PRIMITIVE, parameterNames = {})
    public abstract static class GlobalEnv extends RBuiltinNode {

        @Specialization
        protected Object globalenv() {
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
            return REnvironment.baseEnv();
        }
    }

    @RBuiltin(name = "topenv", kind = INTERNAL, parameterNames = {"envir", "matchThisEnv"})
    public abstract static class TopEnv extends Adapter {

        @Child private FrameFunctions.ParentFrame parentFrameNode;

        @Specialization
        protected REnvironment topEnv(REnvironment env, REnvironment matchThisEnv) {
            return doTopEnv(matchThisEnv, env);
        }

        @Specialization
        protected REnvironment topEnv(REnvironment envir, @SuppressWarnings("unused") RNull matchThisEnv) {
            return doTopEnv(null, envir);
        }

        @Fallback
        protected REnvironment topEnv(VirtualFrame frame, Object envir, Object matchThisEnv) {
            REnvironment env;
            REnvironment target;
            if (!(envir instanceof REnvironment)) {
                if (parentFrameNode == null) {
                    parentFrameNode = insert(FrameFunctionsFactory.ParentFrameNodeGen.create(null));
                }
                env = (REnvironment) parentFrameNode.execute(frame, 2);
            } else {
                env = (REnvironment) envir;
            }
            if (!(matchThisEnv instanceof REnvironment)) {
                target = null;
            } else {
                target = (REnvironment) matchThisEnv;
            }
            return doTopEnv(target, env);
        }

        @TruffleBoundary
        private static REnvironment doTopEnv(REnvironment target, final REnvironment envArg) {
            REnvironment env = envArg;
            while (env != REnvironment.emptyEnv()) {
                if (env == target || env == REnvironment.globalEnv() || env == REnvironment.baseEnv() || env == REnvironment.baseNamespaceEnv() || env.isPackageEnv() != null || env.isNamespaceEnv() ||
                                env.get(".packageName") != null) {
                    return env;
                }
                env = env.getParent();
            }
            return REnvironment.globalEnv();
        }
    }

    @RBuiltin(name = "parent.env", kind = INTERNAL, parameterNames = {"env"})
    public abstract static class ParentEnv extends Adapter {

        @Specialization
        protected REnvironment parentenv(REnvironment env) {
            if (env == REnvironment.emptyEnv()) {
                errorProfile.enter();
                throw RError.error(this, RError.Message.EMPTY_NO_PARENT);
            }
            return env.getParent();
        }
    }

    @RBuiltin(name = "parent.env<-", kind = INTERNAL, parameterNames = {"env", "value"})
    public abstract static class SetParentEnv extends Adapter {

        @Specialization
        @TruffleBoundary
        protected REnvironment setParentenv(REnvironment env, REnvironment parent) {
            if (env == REnvironment.emptyEnv()) {
                throw RError.error(this, RError.Message.CANNOT_SET_PARENT);
            }
            env.setParent(parent);
            return env;
        }
    }

    @RBuiltin(name = "is.environment", kind = PRIMITIVE, parameterNames = {"x"})
    public abstract static class IsEnvironment extends RBuiltinNode {

        @Specialization
        protected byte isEnvironment(Object env) {
            return env instanceof REnvironment ? RRuntime.LOGICAL_TRUE : RRuntime.LOGICAL_FALSE;
        }
    }

    @RBuiltin(name = "environment", kind = INTERNAL, parameterNames = {"fun"})
    public abstract static class Environment extends RBuiltinNode {

        @Specialization
        protected Object environment(VirtualFrame frame, @SuppressWarnings("unused") RNull fun, //
                        @Cached("new()") GetCallerFrameNode callerFrame, //
                        @Cached("new()") PromiseDeoptimizeFrameNode deoptFrameNode) {
            MaterializedFrame matFrame = callerFrame.execute(frame);

            matFrame = matFrame instanceof VirtualEvalFrame ? ((VirtualEvalFrame) matFrame).getOriginalFrame() : matFrame;
            deoptFrameNode.deoptimizeFrame(matFrame);
            return REnvironment.frameToEnvironment(matFrame);
        }

        /**
         * Returns the environment that {@code func} was created in.
         */
        @Specialization
        protected Object environment(RFunction fun, //
                        @Cached("createBinaryProfile()") ConditionProfile noEnvProfile, //
                        @Cached("createBinaryProfile()") ConditionProfile createProfile) {
            Frame enclosing = fun.getEnclosingFrame();
            if (noEnvProfile.profile(enclosing == null)) {
                return RNull.instance;
            }
            REnvironment env = RArguments.getEnvironment(enclosing);
            if (createProfile.profile(env == null)) {
                return REnvironment.createEnclosingEnvironments(enclosing.materialize());
            }
            return env;
        }

        @Specialization(guards = "isRFormula(formula)")
        protected Object environment(RLanguage formula, //
                        @Cached("create()") RAttributeProfiles attrProfiles) {
            Object result = formula.getAttr(attrProfiles, RRuntime.DOT_ENVIRONMENT);
            return result == null ? RNull.instance : result;
        }

        @Specialization(guards = {"!isRNull(fun)", "!isRFunction(fun)", "!isRFormula(fun)"})
        protected Object environment(@SuppressWarnings("unused") Object fun) {
            // Not an error according to GnuR
            return RNull.instance;
        }
    }

    @RBuiltin(name = "environment<-", kind = PRIMITIVE, parameterNames = {"env", "value"})
    public abstract static class UpdateEnvironment extends RBuiltinNode {

        private static RAttributeProfiles attributeProfile = RAttributeProfiles.create();

        @Specialization
        @TruffleBoundary
        protected Object updateEnvironment(RFunction fun, REnvironment env) {
            MaterializedFrame enclosingFrame = env.getFrame();
            assert !(enclosingFrame instanceof VirtualEvalFrame);

            RRootNode root = (RRootNode) fun.getTarget().getRootNode();
            RootCallTarget target = root.duplicateWithNewFrameDescriptor();
            FrameSlotChangeMonitor.initializeEnclosingFrame(target.getRootNode().getFrameDescriptor(), enclosingFrame);
            return RDataFactory.createFunction(fun.getName(), target, null, enclosingFrame);
        }

        @SuppressWarnings("unused")
        @Specialization
        @TruffleBoundary
        protected Object updateEnvironment(RFunction fun, RNull env) {
            throw RError.error(this, RError.Message.USE_NULL_ENV_DEFUNCT);
        }

        protected Object updateEnvironmentNonFunction(Object obj, Object env) {
            if (env == RNull.instance || env instanceof REnvironment) {
                if (obj instanceof RAttributable) {
                    RAttributable attributable = (RAttributable) obj;
                    if (env == RNull.instance) {
                        attributable.removeAttr(attributeProfile, RRuntime.DOT_ENVIRONMENT);
                    } else {
                        attributable.setAttr(RRuntime.DOT_ENVIRONMENT, env);
                    }
                    return obj;
                } else {
                    throw RInternalError.shouldNotReachHere("environment<- called on non-attributable object");
                }
            } else {
                throw RError.error(this, RError.Message.REPLACEMENT_NOT_ENVIRONMENT);
            }
        }

        @Specialization
        @TruffleBoundary
        protected Object updateEnvironment(RAbstractContainer obj, Object env) {
            return updateEnvironmentNonFunction(obj, env);
        }

        @Fallback
        @TruffleBoundary
        protected Object updateEnvironment(Object obj, Object env) {
            return updateEnvironmentNonFunction(obj, env);
        }

    }

    @RBuiltin(name = "environmentName", kind = INTERNAL, parameterNames = {"fun"})
    public abstract static class EnvironmentName extends RBuiltinNode {

        @Specialization
        protected String environmentName(REnvironment env) {
            return env.getName();
        }

        @Specialization(guards = "!isREnvironment(env)")
        protected String environmentName(@SuppressWarnings("unused") Object env) {
            // Not an error according to GnuR
            return "";
        }
    }

    @RBuiltin(name = "new.env", kind = INTERNAL, parameterNames = {"hash", "parent", "size"})
    public abstract static class NewEnv extends RBuiltinNode {

        @Override
        protected void createCasts(CastBuilder casts) {
            casts.toInteger(2);
        }

        @Specialization
        @TruffleBoundary
        protected REnvironment newEnv(byte hash, REnvironment parent, int size) {
            REnvironment env = RDataFactory.createNewEnv(null, RRuntime.fromLogical(hash), size);
            RArguments.initializeEnclosingFrame(env.getFrame(), parent.getFrame());
            return env;
        }
    }

    @RBuiltin(name = "search", kind = INTERNAL, parameterNames = {})
    public abstract static class Search extends RBuiltinNode {
        @Specialization
        protected RStringVector search() {
            return RDataFactory.createStringVector(REnvironment.searchPath(), RDataFactory.COMPLETE_VECTOR);
        }
    }

    @RBuiltin(name = "lockEnvironment", visibility = RVisibility.OFF, kind = INTERNAL, parameterNames = {"env", "bindings"})
    public abstract static class LockEnvironment extends RBuiltinNode {

        @Specialization
        protected Object lockEnvironment(REnvironment env, byte bindings) {
            env.lock(bindings == RRuntime.LOGICAL_TRUE);
            return RNull.instance;
        }
    }

    @RBuiltin(name = "environmentIsLocked", kind = INTERNAL, parameterNames = {"env"})
    public abstract static class EnvironmentIsLocked extends RBuiltinNode {
        @Specialization
        protected Object lockEnvironment(REnvironment env) {
            return RDataFactory.createLogicalVectorFromScalar(env.isLocked());
        }
    }

    private static RuntimeException typeError(RBaseNode invokingNode, Object sym, Object env) {
        if (!(sym instanceof RSymbol)) {
            throw RError.error(invokingNode, RError.Message.NOT_A_SYMBOL);
        } else {
            assert !(env instanceof REnvironment);
            throw RError.error(invokingNode, RError.Message.NOT_AN_ENVIRONMENT);
        }
    }

    @RBuiltin(name = "lockBinding", visibility = RVisibility.OFF, kind = INTERNAL, parameterNames = {"sym", "env"})
    public abstract static class LockBinding extends RBuiltinNode {
        @Specialization
        protected Object lockBinding(RSymbol sym, REnvironment env) {
            env.lockBinding(sym.getName());
            return RNull.instance;
        }

        @Fallback
        protected Object lockBinding(Object sym, Object env) {
            throw typeError(this, sym, env);
        }
    }

    @RBuiltin(name = "unlockBinding", visibility = RVisibility.OFF, kind = INTERNAL, parameterNames = {"sym", "env"})
    public abstract static class UnlockBinding extends RBuiltinNode {
        @Specialization
        protected RNull unlockBinding(RSymbol sym, REnvironment env) {
            env.unlockBinding(sym.getName());
            return RNull.instance;
        }

        @Fallback
        protected Object unlockBindings(Object sym, Object env) {
            throw typeError(this, sym, env);
        }
    }

    @RBuiltin(name = "bindingIsLocked", kind = INTERNAL, parameterNames = {"sym", "env"})
    public abstract static class BindingIsLocked extends RBuiltinNode {
        @Specialization
        protected Object bindingIsLocked(RSymbol sym, REnvironment env) {
            return RDataFactory.createLogicalVectorFromScalar(env.bindingIsLocked(sym.getName()));
        }

        @Fallback
        protected Object bindingIsLocked(Object sym, Object env) {
            throw typeError(this, sym, env);
        }
    }

    @RBuiltin(name = "makeActiveBinding", visibility = RVisibility.OFF, kind = INTERNAL, parameterNames = {"sym", "fun", "env"})
    public abstract static class MakeActiveBinding extends RBuiltinNode {
        @SuppressWarnings("unused")
        @Specialization
        protected Object makeActiveBinding(Object sym, Object fun, Object env) {
            // TODO implement
            throw RError.nyi(this, "makeActiveBinding");
        }
    }

    @RBuiltin(name = "bindingIsActive", kind = INTERNAL, parameterNames = {"sym", "env"})
    public abstract static class BindingIsActive extends RBuiltinNode {
        @SuppressWarnings("unused")
        @Specialization
        protected Object bindingIsActive(Object sym, Object fun, Object env) {
            // TODO implement
            throw RError.nyi(this, "bindingIsActive");
        }
    }

    @RBuiltin(name = "env2list", kind = INTERNAL, parameterNames = {"x", "all.names", "sorted"})
    public abstract static class EnvToList extends RBuiltinNode {

        @Child private CopyNode copy;

        private Object copy(VirtualFrame frame, Object operand) {
            if (copy == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                copy = insert(CopyNodeGen.create(null));
            }
            return copy.execute(frame, operand);
        }

        @Specialization
        protected RList envToListAllNames(VirtualFrame frame, REnvironment env, RAbstractLogicalVector allNamesVec, RAbstractLogicalVector sortedVec) {
            // according to the docs it is expected to be slow as it creates a copy of environment
            // objects
            boolean allNames = allNamesVec.getLength() == 0 || allNamesVec.getDataAt(0) == RRuntime.LOGICAL_FALSE ? false : true;
            boolean sorted = sortedVec.getLength() == 0 || sortedVec.getDataAt(0) == RRuntime.LOGICAL_FALSE ? false : true;
            RStringVector keys = envls(env, allNames, sorted);
            Object[] data = new Object[keys.getLength()];
            for (int i = 0; i < data.length; i++) {
                // TODO: not all types are handled (e.g. copying environments)
                String key = keys.getDataAt(i);
                Object o = env.get(key);
                data[i] = copy(frame, o);
            }
            return RDataFactory.createList(data, keys.getLength() == 0 ? null : keys);
        }

        @TruffleBoundary
        private static RStringVector envls(REnvironment env, boolean allNames, boolean sorted) {
            return env.ls(allNames, null, sorted);
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
                recursiveCopy = insert(CopyNodeGen.create(null));
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

        @Specialization
        Object copy(REnvironment env) {
            return env;
        }

        @Fallback
        Object copy(@SuppressWarnings("unused") Object o) {
            throw RInternalError.unimplemented("copying of object in the environment not supported");
        }
    }
}
