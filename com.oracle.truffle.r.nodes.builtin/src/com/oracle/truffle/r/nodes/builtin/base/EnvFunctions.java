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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.asIntegerVector;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.instanceOf;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.numericValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;
import static com.oracle.truffle.r.runtime.RDispatch.INTERNAL_GENERIC;
import static com.oracle.truffle.r.runtime.RVisibility.OFF;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.COMPLEX;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.READS_FRAME;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.RRootNode;
import com.oracle.truffle.r.nodes.attributes.GetFixedAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SetFixedAttributeNode;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.RList2EnvNode;
import com.oracle.truffle.r.nodes.builtin.base.EnvFunctionsFactory.CopyNodeGen;
import com.oracle.truffle.r.nodes.function.GetCallerFrameNode;
import com.oracle.truffle.r.nodes.function.PromiseHelperNode;
import com.oracle.truffle.r.nodes.function.PromiseHelperNode.PromiseDeoptimizeFrameNode;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.VirtualEvalFrame;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
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
import com.oracle.truffle.r.runtime.data.RTypes;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.env.frame.FrameSlotChangeMonitor;

/**
 * Encapsulates all the builtins related to R environments as nested static classes.
 */
public class EnvFunctions {

    protected abstract static class Adapter extends RBuiltinNode {
        protected final BranchProfile errorProfile = BranchProfile.create();
    }

    @RBuiltin(name = "as.environment", kind = PRIMITIVE, parameterNames = {"fun"}, dispatch = INTERNAL_GENERIC, behavior = COMPLEX)
    public abstract static class AsEnvironment extends Adapter {

        @Override
        protected void createCasts(CastBuilder casts) {
            casts.arg("fun").mapIf(numericValue(), asIntegerVector());
        }

        @Specialization
        protected REnvironment asEnvironment(@SuppressWarnings("unused") RNull rnull) {
            throw RError.error(this, RError.Message.AS_ENV_NULL_DEFUNCT);
        }

        @Specialization
        protected REnvironment asEnvironment(REnvironment env) {
            return env;
        }

        @Specialization
        protected Object asEnvironmentInt(VirtualFrame frame, RAbstractIntVector pos,
                        @Cached("new()") GetCallerFrameNode getCallerFrame) {
            if (pos.getLength() == 0) {
                CompilerDirectives.transferToInterpreter();
                throw RError.error(this, Message.INVALID_ARGUMENT, "pos");
            }
            Object[] results = pos.getLength() == 1 ? null : new Object[pos.getLength()];
            for (int i = 0; i < pos.getLength(); i++) {
                REnvironment env;
                int p = pos.getDataAt(i);
                if (p == -1) {
                    if (RArguments.getDepth(frame) == 0) {
                        errorProfile.enter();
                        throw RError.error(this, RError.Message.NO_ENCLOSING_ENVIRONMENT);
                    }
                    Frame callerFrame = getCallerFrame.execute(frame);
                    env = REnvironment.frameToEnvironment(callerFrame.materialize());
                } else {
                    env = fromSearchpath(p);
                }
                if (pos.getLength() == 1) {
                    return env;
                }
            }
            return RDataFactory.createList(results);
        }

        @TruffleBoundary
        private REnvironment fromSearchpath(int p) {
            String[] searchPath = REnvironment.searchPath();
            if (p == searchPath.length + 1) {
                // although the empty env does not appear in the result of "search", and it
                // is
                // not accessible by name, GnuR allows it to be accessible by index
                return REnvironment.emptyEnv();
            } else if ((p <= 0) || (p > searchPath.length + 1)) {
                errorProfile.enter();
                throw RError.error(this, RError.Message.INVALID_ARGUMENT, "pos");
            } else {
                return REnvironment.lookupOnSearchPath(searchPath[p - 1]);
            }
        }

        @Specialization
        @TruffleBoundary
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

        protected GetFixedAttributeNode createGetXDataAttrNode() {
            return GetFixedAttributeNode.create(RRuntime.DOT_XDATA);
        }

        @Specialization
        protected Object asEnvironment(RS4Object obj, @Cached("createGetXDataAttrNode()") GetFixedAttributeNode getXDataAttrNode) {
            // generic dispatch tried already
            Object xData = getXDataAttrNode.execute(obj);
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

    @RBuiltin(name = "emptyenv", kind = PRIMITIVE, parameterNames = {}, behavior = PURE)
    public abstract static class EmptyEnv extends RBuiltinNode {

        @Specialization
        protected REnvironment emptyenv() {
            return REnvironment.emptyEnv();
        }
    }

    @RBuiltin(name = "globalenv", kind = PRIMITIVE, parameterNames = {}, behavior = PURE)
    public abstract static class GlobalEnv extends RBuiltinNode {

        @Specialization
        protected Object globalenv() {
            return REnvironment.globalEnv();
        }
    }

    /**
     * Returns the "package:base" environment.
     */
    @RBuiltin(name = "baseenv", kind = PRIMITIVE, parameterNames = {}, behavior = PURE)
    public abstract static class BaseEnv extends RBuiltinNode {

        @Specialization
        protected Object baseenv() {
            return REnvironment.baseEnv();
        }
    }

    @RBuiltin(name = "topenv", kind = INTERNAL, parameterNames = {"envir", "matchThisEnv"}, behavior = COMPLEX)
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
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    parentFrameNode = insert(FrameFunctionsFactory.ParentFrameNodeGen.create());
                }
                env = parentFrameNode.execute(frame, 2);
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

    @RBuiltin(name = "parent.env", kind = INTERNAL, parameterNames = {"env"}, behavior = READS_FRAME)
    public abstract static class ParentEnv extends Adapter {

        @Override
        protected void createCasts(CastBuilder casts) {
            casts.arg("env").mustBe(instanceOf(REnvironment.class), RError.SHOW_CALLER, Message.ARGUMENT_NOT_ENVIRONMENT);
        }

        @Specialization
        protected REnvironment parentenv(REnvironment env) {
            if (env == REnvironment.emptyEnv()) {
                errorProfile.enter();
                throw RError.error(RError.SHOW_CALLER, RError.Message.EMPTY_NO_PARENT);
            }
            return env.getParent();
        }
    }

    @RBuiltin(name = "parent.env<-", kind = INTERNAL, parameterNames = {"env", "value"}, behavior = COMPLEX)
    public abstract static class SetParentEnv extends Adapter {

        @Override
        protected void createCasts(CastBuilder casts) {
            casts.arg("env").mustBe(instanceOf(REnvironment.class), Message.NON_LANG_ASSIGNMENT_TARGET);
            casts.arg("value").mustNotBeNull(Message.USE_NULL_ENV_DEFUNCT, "NULL").mustBe(instanceOf(REnvironment.class), Message.ARGUMENT_NAME_NOT_ENVIRONMENT, "parent");
        }

        @Specialization
        @TruffleBoundary
        protected REnvironment setParentenv(REnvironment env, REnvironment parent) {
            if (env == REnvironment.emptyEnv()) {
                throw RError.error(RError.SHOW_CALLER, RError.Message.CANNOT_SET_PARENT);
            }
            env.setParent(parent);
            return env;
        }
    }

    @RBuiltin(name = "is.environment", kind = PRIMITIVE, parameterNames = {"x"}, behavior = PURE)
    public abstract static class IsEnvironment extends RBuiltinNode {

        @Specialization
        protected byte isEnvironment(Object env) {
            return env instanceof REnvironment ? RRuntime.LOGICAL_TRUE : RRuntime.LOGICAL_FALSE;
        }
    }

    @RBuiltin(name = "environment", kind = INTERNAL, parameterNames = {"fun"}, behavior = COMPLEX)
    public abstract static class Environment extends RBuiltinNode {

        private final ConditionProfile attributable = ConditionProfile.createBinaryProfile();
        @Child private GetFixedAttributeNode getEnvAttrNode;

        @Specialization
        protected Object environment(VirtualFrame frame, @SuppressWarnings("unused") RNull fun,
                        @Cached("new()") GetCallerFrameNode callerFrame,
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
        protected Object environment(RFunction fun,
                        @Cached("createBinaryProfile()") ConditionProfile noEnvProfile,
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
        protected Object environment(RLanguage formula) {
            if (getEnvAttrNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getEnvAttrNode = insert(GetFixedAttributeNode.create(RRuntime.DOT_ENVIRONMENT));
            }

            Object result = getEnvAttrNode.execute(formula);
            return result == null ? RNull.instance : result;
        }

        @Specialization(guards = {"!isRNull(fun)", "!isRFunction(fun)", "!isRFormula(fun)"})
        protected Object environment(Object fun) {
            if (attributable.profile(fun instanceof RAttributable)) {
                if (getEnvAttrNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    getEnvAttrNode = insert(GetFixedAttributeNode.create(RRuntime.DOT_ENVIRONMENT));
                }
                Object attr = getEnvAttrNode.execute(fun);
                return attr == null ? RNull.instance : attr;
            } else {
                // Not an error according to GnuR
                return RNull.instance;
            }
        }
    }

    @RBuiltin(name = "environment<-", kind = PRIMITIVE, parameterNames = {"env", "value"}, behavior = COMPLEX)
    public abstract static class UpdateEnvironment extends RBuiltinNode {

        private final RAttributeProfiles attributeProfile = RAttributeProfiles.create();

        @Override
        protected void createCasts(CastBuilder casts) {
            casts.arg("value").allowNull().mustBe(REnvironment.class, Message.REPLACEMENT_NOT_ENVIRONMENT);
        }

        @Specialization
        @TruffleBoundary
        protected static Object updateEnvironment(RFunction fun, REnvironment env) {
            if (env.getFrame() == fun.getEnclosingFrame()) {
                return fun;
            }
            MaterializedFrame enclosingFrame = env.getFrame();
            assert !(enclosingFrame instanceof VirtualEvalFrame);

            RRootNode root = (RRootNode) fun.getTarget().getRootNode();
            RootCallTarget target = root.duplicateWithNewFrameDescriptor();
            FrameSlotChangeMonitor.initializeEnclosingFrame(target.getRootNode().getFrameDescriptor(), enclosingFrame);
            return RDataFactory.createFunction(fun.getName(), fun.getPackageName(), target, null, enclosingFrame);
        }

        @Specialization
        @TruffleBoundary
        protected Object updateEnvironment(@SuppressWarnings("unused") RFunction fun, @SuppressWarnings("unused") RNull env) {
            throw RError.error(this, RError.Message.USE_NULL_ENV_DEFUNCT);
        }

        protected SetFixedAttributeNode createSetEnvAttrNode() {
            return SetFixedAttributeNode.create(RRuntime.DOT_ENVIRONMENT);
        }

        @Specialization
        @TruffleBoundary
        protected static Object updateEnvironment(RAbstractContainer obj, REnvironment env,
                        @Cached("createSetEnvAttrNode()") SetFixedAttributeNode setEnvAttrNode) {
            return updateEnvironment((RAttributable) obj, env, setEnvAttrNode);
        }

        @Specialization
        @TruffleBoundary
        protected static Object updateEnvironment(RAttributable obj, REnvironment env,
                        @Cached("createSetEnvAttrNode()") SetFixedAttributeNode setEnvAttrNode) {
            setEnvAttrNode.execute(obj, env);
            return obj;
        }

        @Specialization
        @TruffleBoundary
        protected Object updateEnvironment(RAbstractContainer obj, RNull env) {
            return updateEnvironment((RAttributable) obj, env);
        }

        @Specialization
        @TruffleBoundary
        protected Object updateEnvironment(RAttributable obj, @SuppressWarnings("unused") RNull env) {
            obj.removeAttr(attributeProfile, RRuntime.DOT_ENVIRONMENT);
            return obj;
        }

        @Specialization
        @TruffleBoundary
        protected static Object updateEnvironment(RNull obj, @SuppressWarnings("unused") RNull env) {
            return obj;
        }

        @Specialization
        @TruffleBoundary
        protected Object updateEnvironment(@SuppressWarnings("unused") RNull obj, @SuppressWarnings("unused") REnvironment env) {
            throw RError.error(this, Message.SET_ATTRIBUTES_ON_NULL);
        }
    }

    @RBuiltin(name = "environmentName", kind = INTERNAL, parameterNames = {"fun"}, behavior = PURE)
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

    @RBuiltin(name = "new.env", kind = INTERNAL, parameterNames = {"hash", "parent", "size"}, behavior = COMPLEX)
    public abstract static class NewEnv extends RBuiltinNode {

        @Override
        protected void createCasts(CastBuilder casts) {
            casts.arg("hash").mustNotBeNull().asLogicalVector().findFirst(RRuntime.LOGICAL_FALSE).map(toBoolean());
            casts.arg("parent").mustNotBeNull().mustBe(REnvironment.class, Message.MUST_BE_ENVIRON);
            casts.arg("size").mustNotBeNull().asIntegerVector().findFirst(0);
        }

        @Specialization
        @TruffleBoundary
        protected REnvironment newEnv(boolean hash, REnvironment parent, int size) {
            REnvironment env = RDataFactory.createNewEnv(null, hash, size);
            RArguments.initializeEnclosingFrame(env.getFrame(), parent.getFrame());
            return env;
        }
    }

    @RBuiltin(name = "search", kind = INTERNAL, parameterNames = {}, behavior = COMPLEX)
    public abstract static class Search extends RBuiltinNode {
        @Specialization
        protected RStringVector search() {
            return RDataFactory.createStringVector(REnvironment.searchPath(), RDataFactory.COMPLETE_VECTOR);
        }
    }

    @RBuiltin(name = "lockEnvironment", visibility = OFF, kind = INTERNAL, parameterNames = {"env", "bindings"}, behavior = COMPLEX)
    public abstract static class LockEnvironment extends RBuiltinNode {

        @Override
        protected void createCasts(CastBuilder casts) {
            casts.arg("env").mustNotBeNull().mustBe(REnvironment.class, RError.SHOW_CALLER, Message.NOT_AN_ENVIRONMENT);
            // TODO: the actual interpretation of this parameter remains dubious
            casts.arg("bindings").mustNotBeNull().asLogicalVector().findFirst(RRuntime.LOGICAL_FALSE).map(toBoolean());
        }

        @Specialization
        protected Object lockEnvironment(REnvironment env, boolean bindings) {
            env.lock(bindings);
            return RNull.instance;
        }
    }

    @RBuiltin(name = "environmentIsLocked", kind = INTERNAL, parameterNames = {"env"}, behavior = PURE)
    public abstract static class EnvironmentIsLocked extends RBuiltinNode {

        @Override
        protected void createCasts(CastBuilder casts) {
            casts.arg("env").mustNotBeNull().mustBe(REnvironment.class, RError.SHOW_CALLER, Message.NOT_AN_ENVIRONMENT);
        }

        @Specialization
        protected Object lockEnvironment(REnvironment env) {
            return RDataFactory.createLogicalVectorFromScalar(env.isLocked());
        }
    }

    @RBuiltin(name = "lockBinding", visibility = OFF, kind = INTERNAL, parameterNames = {"sym", "env"}, behavior = COMPLEX)
    public abstract static class LockBinding extends RBuiltinNode {

        @Override
        protected void createCasts(CastBuilder casts) {
            casts.arg("sym").mustNotBeNull().mustBe(RSymbol.class, RError.SHOW_CALLER, Message.NOT_A_SYMBOL);
            casts.arg("env").mustNotBeNull().mustBe(REnvironment.class, RError.SHOW_CALLER, Message.NOT_AN_ENVIRONMENT);
        }

        @Specialization
        protected Object lockBinding(RSymbol sym, REnvironment env) {
            env.lockBinding(sym.getName());
            return RNull.instance;
        }
    }

    @RBuiltin(name = "unlockBinding", visibility = OFF, kind = INTERNAL, parameterNames = {"sym", "env"}, behavior = COMPLEX)
    public abstract static class UnlockBinding extends RBuiltinNode {

        @Override
        protected void createCasts(CastBuilder casts) {
            casts.arg("sym").mustNotBeNull().mustBe(RSymbol.class, RError.SHOW_CALLER, Message.NOT_A_SYMBOL);
            casts.arg("env").mustNotBeNull().mustBe(REnvironment.class, RError.SHOW_CALLER, Message.NOT_AN_ENVIRONMENT);
        }

        @Specialization
        protected RNull unlockBinding(RSymbol sym, REnvironment env) {
            env.unlockBinding(sym.getName());
            return RNull.instance;
        }
    }

    @RBuiltin(name = "bindingIsLocked", kind = INTERNAL, parameterNames = {"sym", "env"}, behavior = PURE)
    public abstract static class BindingIsLocked extends RBuiltinNode {

        @Override
        protected void createCasts(CastBuilder casts) {
            casts.arg("sym").mustNotBeNull().mustBe(RSymbol.class, RError.SHOW_CALLER, Message.NOT_A_SYMBOL);
            casts.arg("env").mustNotBeNull().mustBe(REnvironment.class, RError.SHOW_CALLER, Message.NOT_AN_ENVIRONMENT);
        }

        @Specialization
        protected Object bindingIsLocked(RSymbol sym, REnvironment env) {
            return RDataFactory.createLogicalVectorFromScalar(env.bindingIsLocked(sym.getName()));
        }
    }

    @RBuiltin(name = "makeActiveBinding", visibility = OFF, kind = INTERNAL, parameterNames = {"sym", "fun", "env"}, behavior = COMPLEX)
    public abstract static class MakeActiveBinding extends RBuiltinNode {

        @Override
        protected void createCasts(CastBuilder casts) {
            casts.arg("sym").mustNotBeNull().mustBe(RSymbol.class, RError.SHOW_CALLER, Message.NOT_A_SYMBOL);
            casts.arg("fun").mustNotBeNull().mustBe(RFunction.class, RError.SHOW_CALLER, Message.NOT_A_FUNCTION);
            casts.arg("env").mustNotBeNull().mustBe(REnvironment.class, RError.SHOW_CALLER, Message.NOT_AN_ENVIRONMENT);
        }

        @SuppressWarnings("unused")
        @Specialization
        protected Object makeActiveBinding(Object sym, Object fun, Object env) {
            // TODO implement
            throw RError.nyi(this, "makeActiveBinding");
        }
    }

    @RBuiltin(name = "bindingIsActive", kind = INTERNAL, parameterNames = {"sym", "env"}, behavior = PURE)
    public abstract static class BindingIsActive extends RBuiltinNode {

        @Override
        protected void createCasts(CastBuilder casts) {
            casts.arg("sym").mustNotBeNull().mustBe(RSymbol.class, RError.SHOW_CALLER, Message.NOT_A_SYMBOL);
            casts.arg("env").mustNotBeNull().mustBe(REnvironment.class, RError.SHOW_CALLER, Message.NOT_AN_ENVIRONMENT);
        }

        @SuppressWarnings("unused")
        @Specialization
        protected Object bindingIsActive(Object sym, Object env) {
            // TODO implement
            throw RError.nyi(this, "bindingIsActive");
        }
    }

    @RBuiltin(name = "env2list", kind = INTERNAL, parameterNames = {"x", "all.names", "sorted"}, behavior = PURE)
    public abstract static class EnvToList extends RBuiltinNode {

        @Child private CopyNode copy;

        @Override
        protected void createCasts(CastBuilder casts) {
            casts.arg("x").mustNotBeNull().mustBe(REnvironment.class, RError.SHOW_CALLER, Message.NOT_AN_ENVIRONMENT);
            casts.arg("all.names").mustNotBeNull().asLogicalVector().findFirst(RRuntime.LOGICAL_FALSE).map(toBoolean());
            casts.arg("sorted").mustNotBeNull().asLogicalVector().findFirst(RRuntime.LOGICAL_FALSE).map(toBoolean());
        }

        private Object copy(VirtualFrame frame, Object operand) {
            if (copy == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                copy = insert(CopyNodeGen.create());
            }
            return copy.execute(frame, operand);
        }

        @Specialization
        protected RList envToListAllNames(VirtualFrame frame, REnvironment env, boolean allNames, boolean sorted) {
            // according to the docs it is expected to be slow as it creates a copy of environment
            // objects
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

    @TypeSystemReference(RTypes.class)
    protected abstract static class CopyNode extends Node {

        protected abstract Object execute(VirtualFrame frame, Object o);

        @Child private CopyNode recursiveCopy;
        @Child private PromiseHelperNode promiseHelper;

        private Object recursiveCopy(VirtualFrame frame, Object operand) {
            if (recursiveCopy == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                recursiveCopy = insert(CopyNodeGen.create());
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
