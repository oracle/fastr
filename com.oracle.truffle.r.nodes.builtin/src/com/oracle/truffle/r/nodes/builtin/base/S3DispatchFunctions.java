/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.RVisibility.CUSTOM;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.COMPLEX;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.SUBSTITUTE;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameInstance.FrameAccess;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.nodes.access.variables.LocalReadVariableNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.function.CallMatcherNode;
import com.oracle.truffle.r.nodes.function.ClassHierarchyNode;
import com.oracle.truffle.r.nodes.function.ClassHierarchyNodeGen;
import com.oracle.truffle.r.nodes.function.PromiseHelperNode;
import com.oracle.truffle.r.nodes.function.PromiseHelperNode.PromiseCheckHelperNode;
import com.oracle.truffle.r.nodes.function.S3FunctionLookupNode;
import com.oracle.truffle.r.nodes.function.S3FunctionLookupNode.Result;
import com.oracle.truffle.r.nodes.function.signature.CollectArgumentsNode;
import com.oracle.truffle.r.nodes.function.signature.CollectArgumentsNodeGen;
import com.oracle.truffle.r.nodes.function.signature.CombineSignaturesNode;
import com.oracle.truffle.r.nodes.function.signature.CombineSignaturesNodeGen;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RArguments.S3Args;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.ReturnException;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;

public abstract class S3DispatchFunctions extends RBuiltinNode {

    @Child private S3FunctionLookupNode methodLookup;
    @Child private CallMatcherNode callMatcher;

    private final ConditionProfile callerFrameSlowPath = ConditionProfile.createBinaryProfile();
    private final ConditionProfile topLevelFrameProfile = ConditionProfile.createBinaryProfile();

    protected S3DispatchFunctions(boolean nextMethod) {
        methodLookup = S3FunctionLookupNode.create(true, nextMethod);
        callMatcher = CallMatcherNode.create(false);
    }

    protected MaterializedFrame getCallerFrame(VirtualFrame frame) {
        MaterializedFrame funFrame = RArguments.getCallerFrame(frame);
        if (callerFrameSlowPath.profile(funFrame == null)) {
            funFrame = Utils.getCallerFrame(frame, FrameAccess.MATERIALIZE).materialize();
            RError.performanceWarning("slow caller frame access in UseMethod dispatch");
        }
        // S3 method can be dispatched from top-level where there is no caller frame
        return topLevelFrameProfile.profile(funFrame == null) ? frame.materialize() : funFrame;
    }

    protected Object dispatch(VirtualFrame frame, String generic, RStringVector type, String group, MaterializedFrame callerFrame, MaterializedFrame genericDefFrame,
                    ArgumentsSignature suppliedSignature, Object[] suppliedArguments) {
        Result lookupResult = methodLookup.execute(frame, generic, type, group, callerFrame, genericDefFrame);

        S3Args s3Args = new S3Args(lookupResult.generic, lookupResult.clazz, lookupResult.targetFunctionName, callerFrame, genericDefFrame, group);
        Object result = callMatcher.execute(frame, suppliedSignature, suppliedArguments, lookupResult.function, lookupResult.targetFunctionName, s3Args);
        return result;
    }

    @RBuiltin(name = "UseMethod", visibility = CUSTOM, kind = PRIMITIVE, parameterNames = {"generic", "object"}, behavior = COMPLEX)
    public abstract static class UseMethod extends S3DispatchFunctions {

        /*
         * TODO: If more than two parameters are passed to UseMethod the extra parameters are
         * ignored and a warning is generated.
         */

        @Child private ClassHierarchyNode classHierarchyNode = ClassHierarchyNodeGen.create(true, true);
        @Child private PromiseCheckHelperNode promiseCheckHelper;

        private final BranchProfile errorProfile = BranchProfile.create();
        private final BranchProfile firstArgMissing = BranchProfile.create();
        private final ConditionProfile argMissingProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile argsValueAndNamesProfile = ConditionProfile.createBinaryProfile();

        static {
            Casts.noCasts(UseMethod.class);
        }

        protected UseMethod() {
            super(false);
        }

        @Specialization
        protected Object execute(VirtualFrame frame, String generic, Object arg) {
            Object dispatchedObject;
            if (argMissingProfile.profile(arg == RMissing.instance)) {
                // For S3Dispatch, we have to evaluate the the first argument
                dispatchedObject = getEnclosingArg(frame, generic);
            } else {
                dispatchedObject = arg;
            }

            RStringVector type = dispatchedObject == null ? RDataFactory.createEmptyStringVector() : classHierarchyNode.execute(dispatchedObject);
            MaterializedFrame callerFrame = getCallerFrame(frame);
            MaterializedFrame genericDefFrame = RArguments.getEnclosingFrame(frame);

            ArgumentsSignature suppliedSignature = RArguments.getSuppliedSignature(frame);
            Object[] suppliedArguments = RArguments.getArguments(frame);
            Object result = dispatch(frame, generic, type, null, callerFrame, genericDefFrame, suppliedSignature, suppliedArguments);
            throw new ReturnException(result, RArguments.getCall(frame));
        }

        /**
         * Get the first (logical) argument in the frame, and handle {@link RPromise}s and
         * {@link RArgsValuesAndNames}. If there is no actual argument, returns null. If there are
         * no formal arguments, throws the appropriate error.
         */
        private Object getEnclosingArg(VirtualFrame frame, String generic) {
            if (RArguments.getArgumentsLength(frame) == 0 || RArguments.getArgument(frame, 0) == null) {
                errorProfile.enter();
                throw RError.error(this, RError.Message.UNKNOWN_FUNCTION_USE_METHOD, generic, RRuntime.toString(RNull.instance));
            }
            Object enclosingArg = RArguments.getArgument(frame, 0);
            if (argsValueAndNamesProfile.profile(enclosingArg instanceof RArgsValuesAndNames)) {
                enclosingArg = getFirstVarArg((RArgsValuesAndNames) enclosingArg);
            } else if (enclosingArg == RMissing.instance) {
                firstArgMissing.enter();
                enclosingArg = getFirstNonMissingArg(frame, 1);
                if (enclosingArg == null) {
                    return null;
                }
            }

            if (promiseCheckHelper == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                promiseCheckHelper = insert(new PromiseCheckHelperNode());
            }
            return promiseCheckHelper.checkEvaluate(frame, enclosingArg);
        }

        private static Object getFirstNonMissingArg(VirtualFrame frame, int startIdx) {
            for (int i = startIdx; i < RArguments.getArgumentsLength(frame); i++) {
                Object arg = RArguments.getArgument(frame, i);
                if (arg instanceof RArgsValuesAndNames) {
                    return getFirstVarArg((RArgsValuesAndNames) arg);
                } else if (arg != RMissing.instance) {
                    return arg;
                }
            }
            return null;
        }

        private static Object getFirstVarArg(RArgsValuesAndNames varArgs) {
            return varArgs.isEmpty() ? null : varArgs.getArgument(0);
        }
    }

    @RBuiltin(name = "NextMethod", visibility = CUSTOM, kind = SUBSTITUTE, parameterNames = {"generic", "object", "..."}, behavior = COMPLEX)
    public abstract static class NextMethod extends S3DispatchFunctions {

        @Child private LocalReadVariableNode rvnGroup = LocalReadVariableNode.create(RRuntime.R_DOT_GROUP, false);
        @Child private LocalReadVariableNode rvnClass = LocalReadVariableNode.create(RRuntime.R_DOT_CLASS, false);
        @Child private LocalReadVariableNode rvnGeneric = LocalReadVariableNode.create(RRuntime.R_DOT_GENERIC, false);
        @Child private LocalReadVariableNode rvnCall = LocalReadVariableNode.create(RRuntime.R_DOT_GENERIC_CALL_ENV, false);
        @Child private LocalReadVariableNode rvnDef = LocalReadVariableNode.create(RRuntime.R_DOT_GENERIC_DEF_ENV, false);

        @Child private CombineSignaturesNode combineSignatures;
        @Child private CollectArgumentsNode collectArguments = CollectArgumentsNodeGen.create();

        @Child private PromiseHelperNode promiseHelper;

        private final BranchProfile errorProfile = BranchProfile.create();
        private final ConditionProfile emptyArgsProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile genericCallFrameNullProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile genericDefFrameNullProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile alternateClassHeaderProfile = ConditionProfile.createBinaryProfile();

        private final ValueProfile parameterSignatureProfile = ValueProfile.createIdentityProfile();
        private final ValueProfile suppliedParameterSignatureProfile = ValueProfile.createIdentityProfile();

        static {
            Casts.noCasts(NextMethod.class);
        }

        protected NextMethod() {
            super(true);
        }

        @Override
        public Object[] getDefaultParameterValues() {
            return new Object[]{RNull.instance, RNull.instance, RArgsValuesAndNames.EMPTY};
        }

        /**
         * When {@code NextMethod} is invoked with first argument which is not a string, the
         * argument is swallowed and ignored.
         */
        @Specialization(guards = "isNotString(ignoredGeneric)")
        protected Object nextMethod(VirtualFrame frame, @SuppressWarnings("unused") Object ignoredGeneric, Object obj, RArgsValuesAndNames args) {
            String generic = (String) rvnGeneric.execute(frame);
            if (generic == null || generic.isEmpty()) {
                errorProfile.enter();
                throw RError.error(this, RError.Message.GEN_FUNCTION_NOT_SPECIFIED);
            }
            return nextMethod(frame, generic, obj, args);
        }

        protected static boolean isNotString(Object obj) {
            // Note: if RAbstractStringVector becomes expected, then it must have length == 1, GnuR
            // ignores character vectors longer than 1 as the "generic" argument of NextMethod
            assert !(obj instanceof RAbstractStringVector) || ((RAbstractStringVector) obj).getLength() != 1 : "unexpected RAbstractStringVector with length != 1";
            return !(obj instanceof String);
        }

        @SuppressWarnings("unused")
        @Specialization
        protected Object nextMethod(VirtualFrame frame, String generic, Object obj, RArgsValuesAndNames args) {
            MaterializedFrame genericCallFrame = getCallFrame(frame);
            MaterializedFrame genericDefFrame = getDefFrame(frame);
            String group = (String) rvnGroup.execute(frame);

            // The signature that will be used for the target of NextMethod is concatenation of the
            // actual signature used when invoking the S3 dispatch function combined with any named
            // arguments passed to NextMethod, the later override the former on a name clash
            ArgumentsSignature finalSignature;
            ArgumentsSignature suppliedSignature = suppliedParameterSignatureProfile.profile(RArguments.getSuppliedSignature(frame));
            Object[] suppliedArguments = collectArguments.execute(frame, parameterSignatureProfile.profile(RArguments.getSignature(frame)));
            if (emptyArgsProfile.profile(args == RArgsValuesAndNames.EMPTY)) {
                finalSignature = suppliedSignature;
            } else {
                if (combineSignatures == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    combineSignatures = insert(CombineSignaturesNodeGen.create());
                }
                RArgsValuesAndNames combinedResult = combineSignatures.execute(suppliedSignature, suppliedArguments, args.getSignature(), args.getArguments());
                suppliedArguments = combinedResult.getArguments();
                finalSignature = combinedResult.getSignature();
            }
            return dispatch(frame, generic, readType(frame), group, genericCallFrame, genericDefFrame, finalSignature, suppliedArguments);
        }

        private MaterializedFrame getDefFrame(VirtualFrame frame) {
            MaterializedFrame genericDefFrame = (MaterializedFrame) rvnDef.execute(frame);
            if (genericDefFrameNullProfile.profile(genericDefFrame == null)) {
                genericDefFrame = RArguments.getEnclosingFrame(frame);
            }
            return genericDefFrame;
        }

        private MaterializedFrame getCallFrame(VirtualFrame frame) {
            MaterializedFrame genericCallFrame = (MaterializedFrame) rvnCall.execute(frame);
            if (genericCallFrameNullProfile.profile(genericCallFrame == null)) {
                genericCallFrame = frame.materialize();
            }
            return genericCallFrame;
        }

        private RStringVector readType(VirtualFrame frame) {
            Object storedClass = rvnClass.execute(frame);
            if (alternateClassHeaderProfile.profile(storedClass == null || storedClass == RNull.instance)) {
                return getAlternateClassHr(frame);
            } else {
                return (RStringVector) storedClass;
            }
        }

        private RStringVector getAlternateClassHr(VirtualFrame frame) {
            if (promiseHelper == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                promiseHelper = insert(new PromiseHelperNode());
            }
            if (RArguments.getArgumentsLength(frame) == 0 || RArguments.getArgument(frame, 0) == null ||
                            (!(RArguments.getArgument(frame, 0) instanceof RAbstractContainer) && !(RArguments.getArgument(frame, 0) instanceof RPromise))) {
                errorProfile.enter();
                throw RError.error(this, RError.Message.OBJECT_NOT_SPECIFIED);
            }
            Object arg = RArguments.getArgument(frame, 0);
            if (arg instanceof RPromise) {
                arg = promiseHelper.evaluate(frame, (RPromise) arg);
            }
            RAbstractContainer enclosingArg = (RAbstractContainer) arg;
            return enclosingArg.getClassHierarchy();
        }
    }
}
