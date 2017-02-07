/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.function;

import java.util.ArrayList;
import java.util.List;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.nodes.EmptyTypeSystemFlatLayout;
import com.oracle.truffle.r.nodes.RASTUtils;
import com.oracle.truffle.r.nodes.RRootNode;
import com.oracle.truffle.r.nodes.access.ConstantNode;
import com.oracle.truffle.r.nodes.access.variables.LocalReadVariableNode;
import com.oracle.truffle.r.nodes.access.variables.ReadVariableNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinRootNode;
import com.oracle.truffle.r.nodes.function.PromiseHelperNode.PromiseCheckHelperNode;
import com.oracle.truffle.r.nodes.function.RCallNodeGen.FunctionDispatchNodeGen;
import com.oracle.truffle.r.nodes.function.S3FunctionLookupNode.Result;
import com.oracle.truffle.r.nodes.function.call.CallRFunctionNode;
import com.oracle.truffle.r.nodes.function.call.PrepareArguments;
import com.oracle.truffle.r.nodes.function.visibility.SetVisibilityNode;
import com.oracle.truffle.r.nodes.profile.TruffleBoundaryNode;
import com.oracle.truffle.r.nodes.profile.VectorLengthProfile;
import com.oracle.truffle.r.nodes.unary.CastNode;
import com.oracle.truffle.r.runtime.Arguments;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RArguments.S3Args;
import com.oracle.truffle.r.runtime.RArguments.S3DefaultArguments;
import com.oracle.truffle.r.runtime.RCaller;
import com.oracle.truffle.r.runtime.RDeparse;
import com.oracle.truffle.r.runtime.RDispatch;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.RVisibility;
import com.oracle.truffle.r.runtime.SubstituteVirtualFrame;
import com.oracle.truffle.r.runtime.builtins.FastPathFactory;
import com.oracle.truffle.r.runtime.builtins.RBuiltinDescriptor;
import com.oracle.truffle.r.runtime.conn.RConnection;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.REmpty;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.data.RPromise.Closure;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RTypedValue;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
import com.oracle.truffle.r.runtime.nodes.RFastPathNode;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxCall;
import com.oracle.truffle.r.runtime.nodes.RSyntaxElement;
import com.oracle.truffle.r.runtime.nodes.RSyntaxLookup;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

@TypeSystemReference(EmptyTypeSystemFlatLayout.class)
@NodeInfo(cost = NodeCost.NONE)
@NodeChild(value = "function", type = RNode.class)
public abstract class RCallNode extends RCallBaseNode implements RSyntaxNode, RSyntaxCall {

    // currently cannot be RSourceSectionNode because of TruffleDSL restrictions

    @CompilationFinal private SourceSection sourceSection;

    @Override
    public final void setSourceSection(SourceSection sourceSection) {
        assert sourceSection != null;
        this.sourceSection = sourceSection;
    }

    @Override
    public final SourceSection getLazySourceSection() {
        return sourceSection;
    }

    @Override
    public final SourceSection getSourceSection() {
        RDeparse.ensureSourceSection(this);
        return sourceSection;
    }

    public abstract RNode getFunction();

    private final RSyntaxNode[] arguments;
    private final int[] varArgIndexes;
    private final ArgumentsSignature signature;
    @Child private ReadVariableNode lookupVarArgs;
    protected final LocalReadVariableNode explicitArgs;

    private final ConditionProfile nullBuiltinProfile = ConditionProfile.createBinaryProfile();

    // needed for INTERNAL_GENERIC calls:
    @Child private FunctionDispatch internalDispatchCall;

    private final Assumption needsNoCallerFrame = Truffle.getRuntime().createAssumption("no caller frame");

    public boolean setNeedsCallerFrame() {
        boolean value = !needsNoCallerFrame.isValid();
        needsNoCallerFrame.invalidate();
        return value;
    }

    protected RCaller createCaller(VirtualFrame frame, RFunction function) {
        if (explicitArgs == null) {
            return RCaller.create(frame, this);
        } else {
            return RCaller.create(frame, RCallerHelper.createFromArguments(function, (RArgsValuesAndNames) explicitArgs.execute(frame)));
        }
    }

    protected RCallNode(SourceSection sourceSection, RSyntaxNode[] arguments, ArgumentsSignature signature) {
        assert sourceSection != null;
        this.sourceSection = sourceSection;
        this.arguments = arguments;
        this.explicitArgs = null;
        this.varArgIndexes = getVarArgIndexes(arguments);
        this.lookupVarArgs = varArgIndexes.length == 0 ? null : ReadVariableNode.createSilent(ArgumentsSignature.VARARG_NAME, RType.Any);

        for (String name : signature) {
            if (name != null && name.isEmpty()) {
                /*
                 * In GnuR this is evidently output by the parser, so very early, and never with a
                 * caller in the message.
                 */
                throw RError.error(RError.NO_CALLER, RError.Message.ZERO_LENGTH_VARIABLE);
            }
        }
        this.signature = signature;
    }

    protected RCallNode(SourceSection sourceSection, Object explicitArgsIdentifier) {
        assert sourceSection != null;
        this.sourceSection = sourceSection;
        this.arguments = null;
        this.explicitArgs = LocalReadVariableNode.create(explicitArgsIdentifier, false);
        this.varArgIndexes = null;
        this.lookupVarArgs = null;
        this.signature = null;
    }

    public static int[] getVarArgIndexes(RSyntaxNode[] arguments) {
        List<Integer> varArgsSymbolIndices = new ArrayList<>();
        for (int i = 0; i < arguments.length; i++) {
            RSyntaxNode arg = arguments[i];
            if (arg instanceof RSyntaxLookup) {
                RSyntaxLookup lookup = (RSyntaxLookup) arg;
                // Check for presence of "..." in the arguments
                if (ArgumentsSignature.VARARG_NAME.equals(lookup.getIdentifier())) {
                    varArgsSymbolIndices.add(i);
                }
            }
        }
        // Setup and return
        int[] varArgsSymbolIndicesArr = new int[varArgsSymbolIndices.size()];
        for (int i = 0; i < varArgsSymbolIndicesArr.length; i++) {
            varArgsSymbolIndicesArr[i] = varArgsSymbolIndices.get(i);
        }
        return varArgsSymbolIndicesArr;
    }

    public Arguments<RSyntaxNode> getArguments() {
        return Arguments.create(arguments, signature);
    }

    private RArgsValuesAndNames lookupVarArgs(VirtualFrame frame) {
        if (explicitArgs != null) {
            return (RArgsValuesAndNames) explicitArgs.execute(frame);
        }
        RArgsValuesAndNames varArgs;
        if (lookupVarArgs == null) {
            varArgs = null;
        } else {
            try {
                varArgs = lookupVarArgs.executeRArgsValuesAndNames(frame);
            } catch (UnexpectedResultException e) {
                throw RError.error(RError.SHOW_CALLER, RError.Message.NO_DOT_DOT_DOT);
            }
        }
        return varArgs;
    }

    protected FunctionDispatch createUninitializedCall() {
        return FunctionDispatchNodeGen.create(this, explicitArgs != null, null);
    }

    protected FunctionDispatch createUninitializedExplicitCall() {
        return FunctionDispatchNodeGen.create(this, true, null);
    }

    /**
     * If there are no parameters, or the target function does not refer to a builtin, or the
     * builtin has no special dispatching, then we know that we will just call the function with no
     * special dispatch logic.
     */
    protected boolean isDefaultDispatch(RFunction function) {
        return (signature != null && signature.isEmpty()) || nullBuiltinProfile.profile(function.getRBuiltin() == null) || function.getRBuiltin().getDispatch() == RDispatch.DEFAULT;
    }

    @Specialization(guards = "isDefaultDispatch(function)")
    public Object call(VirtualFrame frame, RFunction function,
                    @Cached("createUninitializedCall()") FunctionDispatch call) {
        return call.execute(frame, function, lookupVarArgs(frame), null, null);
    }

    protected RNode createDispatchArgument(int index) {
        return RContext.getASTBuilder().process(arguments[index]).asRNode();
    }

    /**
     * If the target function refers to a builtin that requires internal generic dispatch and there
     * are actual parameters to dispatch on, then we will do an internal generic dispatch on the
     * first parameter.
     */
    protected boolean isInternalGenericDispatch(RFunction function) {
        if (signature != null && signature.isEmpty()) {
            return false;
        }
        RBuiltinDescriptor builtin = function.getRBuiltin();
        return builtin != null && builtin.getDispatch() == RDispatch.INTERNAL_GENERIC;
    }

    @Specialization(guards = {"explicitArgs == null", "isInternalGenericDispatch(function)"})
    public Object callInternalGeneric(VirtualFrame frame, RFunction function,
                    @Cached("createDispatchArgument(0)") RNode dispatchArgument,
                    @Cached("new()") TemporarySlotNode dispatchTempSlot,
                    @Cached("create()") ClassHierarchyNode classHierarchyNode,
                    @Cached("createWithError()") S3FunctionLookupNode dispatchLookup,
                    @Cached("createIdentityProfile()") ValueProfile builtinProfile,
                    @Cached("createBinaryProfile()") ConditionProfile implicitTypeProfile,
                    @Cached("createBinaryProfile()") ConditionProfile resultIsBuiltinProfile,
                    @Cached("create()") GetBaseEnvFrameNode getBaseEnvFrameNode) {
        RBuiltinDescriptor builtin = builtinProfile.profile(function.getRBuiltin());
        Object dispatchObject = dispatchArgument.execute(frame);
        // Cannot dispatch on REmpty
        if (dispatchObject == REmpty.instance) {
            CompilerDirectives.transferToInterpreter();
            throw RError.error(this, RError.Message.ARGUMENT_EMPTY, 1);
        }
        FrameSlot slot = dispatchTempSlot.initialize(frame, dispatchObject);
        try {
            RStringVector type = classHierarchyNode.execute(dispatchObject);
            S3Args s3Args;
            RFunction resultFunction;
            if (implicitTypeProfile.profile(type != null)) {
                Result result = dispatchLookup.execute(frame, builtin.getGenericName(), type, null, frame.materialize(), getBaseEnvFrameNode.execute());
                if (resultIsBuiltinProfile.profile(result.function.isBuiltin())) {
                    s3Args = null;
                } else {
                    s3Args = new S3Args(result.generic, result.clazz, result.targetFunctionName, frame.materialize(), null, null);
                }
                resultFunction = result.function;
            } else {
                s3Args = null;
                resultFunction = function;
            }
            if (internalDispatchCall == null || internalDispatchCall.tempFrameSlot != slot) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                internalDispatchCall = insert(FunctionDispatchNodeGen.create(this, false, slot));
            }
            return internalDispatchCall.execute(frame, resultFunction, lookupVarArgs(frame), s3Args, null);
        } finally {
            TemporarySlotNode.cleanup(frame, dispatchObject, slot);
        }
    }

    @Specialization(guards = {"explicitArgs != null", "isInternalGenericDispatch(function)"})
    public Object callInternalGenericExplicit(VirtualFrame frame, RFunction function,
                    @Cached("create()") ClassHierarchyNode classHierarchyNode,
                    @Cached("createWithError()") S3FunctionLookupNode dispatchLookup,
                    @Cached("createIdentityProfile()") ValueProfile builtinProfile,
                    @Cached("createBinaryProfile()") ConditionProfile implicitTypeProfile,
                    @Cached("createBinaryProfile()") ConditionProfile resultIsBuiltinProfile,
                    @Cached("createPromiseHelper()") PromiseCheckHelperNode promiseHelperNode,
                    @Cached("createUninitializedExplicitCall()") FunctionDispatch call,
                    @Cached("create()") GetBaseEnvFrameNode getBaseEnvFrameNode) {
        RBuiltinDescriptor builtin = builtinProfile.profile(function.getRBuiltin());
        RArgsValuesAndNames argAndNames = (RArgsValuesAndNames) explicitArgs.execute(frame);

        RStringVector type = argAndNames.isEmpty() ? null : classHierarchyNode.execute(promiseHelperNode.checkEvaluate(frame, argAndNames.getArgument(0)));
        S3Args s3Args;
        RFunction resultFunction;
        if (implicitTypeProfile.profile(type != null)) {
            Result result = dispatchLookup.execute(frame, builtin.getName(), type, null, frame.materialize(), getBaseEnvFrameNode.execute());
            if (resultIsBuiltinProfile.profile(result.function.isBuiltin())) {
                s3Args = null;
            } else {
                s3Args = new S3Args(result.generic, result.clazz, result.targetFunctionName, frame.materialize(), null, null);
            }
            resultFunction = result.function;
        } else {
            s3Args = null;
            resultFunction = function;
        }
        return call.execute(frame, resultFunction, argAndNames, s3Args, null);
    }

    protected CallArgumentsNode createArguments() {
        return signature == null ? null : createArguments(null, false, false);
    }

    protected boolean isGroupGenericDispatch(RFunction function) {
        if (signature != null && signature.isEmpty()) {
            return false;
        }
        RBuiltinDescriptor builtin = function.getRBuiltin();
        return builtin != null && builtin.getDispatch().isGroupGeneric();
    }

    protected PromiseCheckHelperNode createPromiseHelper() {
        return new PromiseCheckHelperNode();
    }

    @CompilationFinal private ArgumentsSignature summaryGroupSignatureCached = null;
    @CompilationFinal private boolean summaryGroupHasNaRmCached;

    @Specialization(guards = "isGroupGenericDispatch(function)")
    public Object callGroupGeneric(VirtualFrame frame, RFunction function,
                    @Cached("createArguments()") CallArgumentsNode callArguments,
                    @Cached("create()") ClassHierarchyNode classHierarchyNodeX,
                    @Cached("createWithException()") S3FunctionLookupNode dispatchLookupX,
                    @Cached("create()") ClassHierarchyNode classHierarchyNodeY,
                    @Cached("createWithException()") S3FunctionLookupNode dispatchLookupY,
                    @Cached("createIdentityProfile()") ValueProfile builtinProfile,
                    @Cached("createBinaryProfile()") ConditionProfile implicitTypeProfileX,
                    @Cached("createBinaryProfile()") ConditionProfile implicitTypeProfileY,
                    @Cached("createBinaryProfile()") ConditionProfile mismatchProfile,
                    @Cached("createBinaryProfile()") ConditionProfile resultIsBuiltinProfile,
                    @Cached("createBinaryProfile()") ConditionProfile summaryGroupNaRmProfile,
                    @Cached("createBinaryProfile()") ConditionProfile summaryGroupProfile,
                    @Cached("createPromiseHelper()") PromiseCheckHelperNode promiseHelperNode,
                    @Cached("createUninitializedExplicitCall()") FunctionDispatch call,
                    @Cached("create()") GetBaseEnvFrameNode getBaseEnvFrameNode) {

        Object[] args = explicitArgs != null ? ((RArgsValuesAndNames) explicitArgs.execute(frame)).getArguments() : callArguments.evaluateFlattenObjects(frame, lookupVarArgs(frame));
        ArgumentsSignature argsSignature = explicitArgs != null ? ((RArgsValuesAndNames) explicitArgs.execute(frame)).getSignature() : callArguments.flattenNames(lookupVarArgs(frame));

        RBuiltinDescriptor builtin = builtinProfile.profile(function.getRBuiltin());
        RDispatch dispatch = builtin.getDispatch();

        // max(na.rm=TRUE,arg1) dispatches to whatever is class of arg1 not taking the
        // named argument 'na.rm' into account. Note: signatures should be interned, identity
        // comparison is enough. Signature length > 0, because we dispatched on at least one arg
        int typeXIdx = 0;
        if (summaryGroupNaRmProfile.profile(dispatch == RDispatch.SUMMARY_GROUP_GENERIC &&
                        argsSignature.getName(typeXIdx) == RArguments.SUMMARY_GROUP_NA_RM_ARG_NAME)) {
            typeXIdx = 1;
        }

        RStringVector typeX = classHierarchyNodeX.execute(promiseHelperNode.checkEvaluate(frame, args[typeXIdx]));
        Result resultX = null;
        if (implicitTypeProfileX.profile(typeX != null)) {
            resultX = dispatchLookupX.execute(frame, builtin.getName(), typeX, dispatch.getGroupGenericName(), frame.materialize(), getBaseEnvFrameNode.execute());
        }
        Result resultY = null;
        if (args.length > 1 && dispatch == RDispatch.OPS_GROUP_GENERIC) {
            RStringVector typeY = classHierarchyNodeY.execute(promiseHelperNode.checkEvaluate(frame, args[1]));
            if (implicitTypeProfileY.profile(typeY != null)) {
                resultY = dispatchLookupY.execute(frame, builtin.getName(), typeY, dispatch.getGroupGenericName(), frame.materialize(), getBaseEnvFrameNode.execute());
            }
        }

        Result result;
        RStringVector dotMethod;
        if (resultX == null) {
            if (resultY == null) {
                result = null;
                dotMethod = null;
            } else {
                result = resultY;
                dotMethod = RDataFactory.createStringVector(new String[]{"", result.targetFunctionName}, true);
            }
        } else {
            if (resultY == null) {
                result = resultX;
                dotMethod = RDataFactory.createStringVector(new String[]{result.targetFunctionName, ""}, true);
            } else {
                if (mismatchProfile.profile(resultX.function != resultY.function)) {
                    RError.warning(this, RError.Message.INCOMPATIBLE_METHODS, resultX.targetFunctionName, resultY.targetFunctionName, dispatch.getGroupGenericName());
                    result = null;
                    dotMethod = null;
                } else {
                    result = resultX;
                    dotMethod = RDataFactory.createStringVector(new String[]{result.targetFunctionName, result.targetFunctionName}, true);
                }
            }
        }
        final S3Args s3Args;
        final RFunction resultFunction;
        if (result == null) {
            s3Args = null;
            resultFunction = function;
        } else {
            if (resultIsBuiltinProfile.profile(result.function.isBuiltin())) {
                s3Args = null;
            } else {
                s3Args = new S3Args(builtin.getName(), result.clazz, dotMethod, frame.materialize(), null, result.groupMatch ? dispatch.getGroupGenericName() : null);
            }
            resultFunction = result.function;
        }

        // Note: since we are actually not executing the function prologue which would write default
        // values to local variables representing arguments with default values, we have to check
        // whether we need to provide default values ourselves. The next call invoked by the
        // call.execute statement thinks that the formal signature is formal signature of the
        // concrete method, e.g. max.data.frame, which may not have default value for the same
        // arguments as the entry method, e.g. max(...,na.rm=TRUE). Unfortunately, in GnuR this is
        // inconsistent and only applies for 'Summary' group and not, for example, for Math group
        // with its default value for 'digits'
        S3DefaultArguments s3DefaulArguments = null;
        if (summaryGroupProfile.profile(dispatch == RDispatch.SUMMARY_GROUP_GENERIC)) {
            if (argsSignature != summaryGroupSignatureCached) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                summaryGroupSignatureCached = argsSignature;
                summaryGroupHasNaRmCached = false;
                for (int i = 0; i < argsSignature.getLength(); i++) {
                    if (argsSignature.getName(i) == RArguments.SUMMARY_GROUP_NA_RM_ARG_NAME) {
                        summaryGroupHasNaRmCached = true;
                        break;
                    }
                }
            }
            if (!summaryGroupHasNaRmCached) {
                s3DefaulArguments = RArguments.SUMMARY_GROUP_DEFAULT_VALUE_NA_RM;
            }
        }

        return call.execute(frame, resultFunction, new RArgsValuesAndNames(args, argsSignature), s3Args, s3DefaulArguments);
    }

    protected final class ForeignCall extends Node {

        @Child private CallArgumentsNode arguments;
        @Child private Node foreignCall;
        @CompilationFinal private int foreignCallArgCount;

        private final BranchProfile errorProfile = BranchProfile.create();

        public ForeignCall(CallArgumentsNode arguments) {
            this.arguments = arguments;
        }

        public Object execute(VirtualFrame frame, TruffleObject function) {
            Object[] argumentsArray = explicitArgs != null ? ((RArgsValuesAndNames) explicitArgs.execute(frame)).getArguments() : arguments.evaluateFlattenObjects(frame, lookupVarArgs(frame));
            if (foreignCall == null || foreignCallArgCount != argumentsArray.length) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                foreignCall = insert(Message.createExecute(argumentsArray.length).createNode());
                foreignCallArgCount = argumentsArray.length;
            }
            try {
                Object result = ForeignAccess.sendExecute(foreignCall, function, argumentsArray);
                if (result instanceof Boolean) {
                    // convert to R logical
                    // TODO byte/short convert to int?
                    result = RRuntime.asLogical((boolean) result);
                }
                return result;
            } catch (Throwable e) {
                errorProfile.enter();
                throw RError.interopError(RError.findParentRBase(this), e, function);
            }
        }
    }

    protected ForeignCall createForeignCall() {
        return new ForeignCall(createArguments(null, true, true));
    }

    protected static boolean isRTypedValue(Object value) {
        return value instanceof RTypedValue;
    }

    @Specialization(guards = "!isRTypedValue(function)")
    public Object call(VirtualFrame frame, TruffleObject function,
                    @Cached("createForeignCall()") ForeignCall foreignCall) {
        return foreignCall.execute(frame, function);
    }

    @TruffleBoundary
    @Fallback
    public Object call(@SuppressWarnings("unused") Object function) {
        throw RError.error(RError.SHOW_CALLER, RError.Message.APPLY_NON_FUNCTION);
    }

    public CallArgumentsNode createArguments(FrameSlot tempFrameSlot, boolean modeChange, boolean modeChangeAppliesToAll) {
        RNode[] args = new RNode[arguments.length];
        for (int i = 0; i < arguments.length; i++) {
            if (tempFrameSlot != null && i == 0) {
                args[i] = new GetTempNode(tempFrameSlot, arguments[i]);
            } else {
                args[i] = arguments[i] == null ? null : RASTUtils.cloneNode(arguments[i].asRNode());
            }
        }
        return CallArgumentsNode.create(modeChange, modeChangeAppliesToAll, args, signature, varArgIndexes);
    }

    /**
     * Creates a modified call in which the first N arguments are replaced by
     * {@code replacementArgs}. This is only used to support
     * {@code HiddenInternalFunctions.MakeLazy}.
     */
    @TruffleBoundary
    public static RCallNode createCloneReplacingArgs(RCallNode call, RSyntaxNode... replacementArgs) {
        RSyntaxNode[] args = new RSyntaxNode[call.arguments.length];
        for (int i = 0; i < args.length; i++) {
            args[i] = i < replacementArgs.length ? replacementArgs[i] : call.arguments[i];
        }
        return RCallNodeGen.create(call.getLazySourceSection(), args, call.signature, RASTUtils.cloneNode(call.getFunction()));
    }

    /**
     * The standard way to create a call to {@code function} with given arguments. If
     * {@code src == RSyntaxNode.EAGER_DEPARSE} we force a deparse.
     */
    public static RCallNode createCall(SourceSection src, RNode function, ArgumentsSignature signature, RSyntaxNode... arguments) {
        return RCallNodeGen.create(src, arguments, signature, function);
    }

    /**
     * Creates a call that reads its explicit arguments from the frame under given identifier. This
     * allows to invoke a function with argument(s) supplied by hand. Consider using
     * {@link com.oracle.truffle.r.nodes.function.call.RExplicitCallNode} instead.
     */
    public static RCallNode createExplicitCall(Object explicitArgsIdentifier) {
        return RCallNodeGen.create(RSyntaxNode.INTERNAL, explicitArgsIdentifier, null);
    }

    static boolean needsSplitting(RootCallTarget target) {
        RRootNode root = (RRootNode) target.getRootNode();
        return root.containsDispatch() || root.needsSplitting();
    }

    private static final class GetTempNode extends RNode {

        private final FrameSlot slot;
        private final RSyntaxNode arg;

        GetTempNode(FrameSlot slot, RSyntaxNode arg) {
            this.slot = slot;
            this.arg = arg;
        }

        @Override
        protected RSyntaxNode getRSyntaxNode() {
            return arg;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            try {
                return frame.getObject(slot);
            } catch (FrameSlotTypeException e) {
                throw RInternalError.shouldNotReachHere();
            }
        }
    }

    @TypeSystemReference(EmptyTypeSystemFlatLayout.class)
    public abstract static class FunctionDispatch extends Node {

        /**
         * Note: s3DefaultArguments is intended to carry default arguments from
         * {@link RCallNode#callGroupGeneric} if the R dispatch method has some. Currently this is
         * only the case for 'summary' group so this argument is either null or set to
         * {@link RArguments#SUMMARY_GROUP_DEFAULT_VALUE_NA_RM}
         */
        public abstract Object execute(VirtualFrame frame, RFunction function, Object varArgs, Object s3Args, Object s3DefaultArguments);

        protected static final int CACHE_SIZE = 4;

        private final RCallNode originalCall;
        private final boolean explicitArgs;

        private final FrameSlot tempFrameSlot;

        public FunctionDispatch(RCallNode originalCall, boolean explicitArgs, FrameSlot tempFrameSlot) {
            this.originalCall = originalCall;
            this.explicitArgs = explicitArgs;
            this.tempFrameSlot = tempFrameSlot;
        }

        protected LeafCallNode createCacheNode(RootCallTarget cachedTarget) {
            CompilerAsserts.neverPartOfCompilation();
            RRootNode root = (RRootNode) cachedTarget.getRootNode();
            FormalArguments formals = root.getFormalArguments();
            if (root instanceof RBuiltinRootNode) {
                RBuiltinRootNode builtinRoot = (RBuiltinRootNode) root;
                return new BuiltinCallNode(RBuiltinNode.inline(builtinRoot.getBuiltin()), builtinRoot.getBuiltin(), formals, originalCall, explicitArgs);
            } else {
                return new DispatchedCallNode(cachedTarget, originalCall);
            }
        }

        protected PrepareArguments createArguments(RootCallTarget cachedTarget, boolean noOpt) {
            RRootNode root = (RRootNode) cachedTarget.getRootNode();
            if (explicitArgs) {
                return PrepareArguments.createExplicit(root);
            } else {
                CallArgumentsNode args = originalCall.createArguments(tempFrameSlot, root.getBuiltin() == null, true);
                return PrepareArguments.create(root, args, noOpt);
            }
        }

        protected PrepareArguments createArguments(RootCallTarget cachedTarget) {
            return createArguments(cachedTarget, false);
        }

        @Specialization(limit = "CACHE_SIZE", guards = "function.getTarget() == cachedTarget")
        protected Object dispatch(VirtualFrame frame, RFunction function, Object varArgs, Object s3Args, Object s3DefaultArguments,
                        @Cached("function.getTarget()") @SuppressWarnings("unused") RootCallTarget cachedTarget,
                        @Cached("createCacheNode(cachedTarget)") LeafCallNode leafCall,
                        @Cached("createArguments(cachedTarget)") PrepareArguments prepareArguments) {
            RArgsValuesAndNames orderedArguments = prepareArguments.execute(frame, (RArgsValuesAndNames) varArgs, (S3DefaultArguments) s3DefaultArguments, originalCall);
            return leafCall.execute(frame, function, orderedArguments, (S3Args) s3Args);
        }

        private static final class GenericCallEntry extends Node {
            private final RootCallTarget cachedTarget;
            @Child private LeafCallNode leafCall;
            @Child private PrepareArguments prepareArguments;

            GenericCallEntry(RootCallTarget cachedTarget, LeafCallNode leafCall, PrepareArguments prepareArguments) {
                this.cachedTarget = cachedTarget;
                this.leafCall = leafCall;
                this.prepareArguments = prepareArguments;
            }
        }

        /*
         * Use a TruffleBoundaryNode to be able to switch child nodes without invalidating the whole
         * method.
         */
        protected final class GenericCall extends TruffleBoundaryNode {

            @Child private GenericCallEntry entry;

            @TruffleBoundary
            public Object execute(MaterializedFrame materializedFrame, RFunction function, Object varArgs, Object s3Args, Object s3DefaultArguments) {
                GenericCallEntry e = entry;
                RootCallTarget cachedTarget = function.getTarget();
                if (e == null || e.cachedTarget != cachedTarget) {
                    entry = e = insert(new GenericCallEntry(cachedTarget, createCacheNode(cachedTarget), createArguments(cachedTarget)));
                }
                VirtualFrame frame = SubstituteVirtualFrame.create(materializedFrame);
                RArgsValuesAndNames orderedArguments = e.prepareArguments.execute(frame, (RArgsValuesAndNames) varArgs, (S3DefaultArguments) s3DefaultArguments, originalCall);
                return e.leafCall.execute(frame, function, orderedArguments, (S3Args) s3Args);
            }
        }

        protected GenericCall createGenericCall() {
            return new GenericCall();
        }

        @Specialization
        protected Object dispatchFallback(VirtualFrame frame, RFunction function, Object varArgs, Object s3Args, Object s3DefaultArguments,
                        @Cached("createGenericCall()") GenericCall generic) {
            return generic.execute(frame.materialize(), function, varArgs, s3Args, s3DefaultArguments);
        }
    }

    public abstract static class LeafCallNode extends RBaseNode {
        /**
         * The original {@link RSyntaxNode} this derives from.
         */
        protected final RCallNode originalCall;

        private LeafCallNode(RCallNode originalCall) {
            this.originalCall = originalCall;
        }

        @Override
        public RSyntaxNode getRSyntaxNode() {
            return originalCall;
        }

        /**
         * @param orderedArguments arguments values and the original call signature reordered in the
         *            same way as the arguments.
         */
        public abstract Object execute(VirtualFrame frame, RFunction function, RArgsValuesAndNames orderedArguments, S3Args s3Args);
    }

    @NodeInfo(cost = NodeCost.NONE)
    public static final class BuiltinCallNode extends LeafCallNode {

        @Child private RBuiltinNode builtin;
        @Child private PromiseCheckHelperNode varArgsPromiseHelper;
        @Children private final PromiseHelperNode[] promiseHelpers;
        @Children private final CastNode[] casts;
        @Child private SetVisibilityNode visibility = SetVisibilityNode.create();

        // not using profiles to save overhead
        @CompilationFinal private final boolean[] argEmptySeen;
        @CompilationFinal private final boolean[] varArgSeen;
        @CompilationFinal private final boolean[] nonWrapSeen;
        @CompilationFinal private final boolean[] wrapSeen;

        private final FormalArguments formals;
        private final RBuiltinDescriptor builtinDescriptor;
        private final boolean explicitArgs;

        public BuiltinCallNode(RBuiltinNode builtin, RBuiltinDescriptor builtinDescriptor, FormalArguments formalArguments, RCallNode originalCall, boolean explicitArgs) {
            super(originalCall);
            this.builtin = builtin;
            this.builtinDescriptor = builtinDescriptor;
            this.explicitArgs = explicitArgs;
            this.casts = builtin.getCasts();
            this.formals = formalArguments;
            promiseHelpers = new PromiseHelperNode[formals.getLength()];
            argEmptySeen = new boolean[formals.getLength()];
            varArgSeen = new boolean[formals.getLength()];
            nonWrapSeen = new boolean[formals.getLength()];
            wrapSeen = new boolean[formals.getLength()];
        }

        @ExplodeLoop
        public Object[] castArguments(VirtualFrame frame, Object[] args) {
            int argCount = formals.getLength();
            int varArgIndex = formals.getSignature().getVarArgIndex();
            Object[] result = new Object[argCount];
            for (int i = 0; i < argCount; i++) {
                Object arg = args[i];
                if (explicitArgs && arg == REmpty.instance) {
                    if (!argEmptySeen[i]) {
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                        argEmptySeen[i] = true;
                    }
                    arg = formals.getInternalDefaultArgumentAt(i);
                }
                if (varArgIndex == i && arg instanceof RArgsValuesAndNames) {
                    if (!varArgSeen[i]) {
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                        varArgSeen[i] = true;
                    }
                    RArgsValuesAndNames varArgs = (RArgsValuesAndNames) arg;
                    if (builtinDescriptor.evaluatesArg(i)) {
                        forcePromises(frame, varArgs);
                    } else {
                        wrapPromises(varArgs);
                    }
                } else {
                    if (builtinDescriptor.evaluatesArg(i)) {
                        if (arg instanceof RPromise) {
                            if (promiseHelpers[i] == null) {
                                CompilerDirectives.transferToInterpreterAndInvalidate();
                                promiseHelpers[i] = insert(new PromiseHelperNode());
                            }
                            arg = promiseHelpers[i].evaluate(frame, (RPromise) arg);
                        }
                        if (i < casts.length && casts[i] != null) {
                            assert builtinDescriptor.evaluatesArg(i);
                            arg = casts[i].execute(arg);
                        }
                    } else {
                        assert casts.length <= i || casts[i] == null : "no casts allowed on non-evaluated arguments in builtin " + builtinDescriptor.getName();
                        if (arg instanceof RPromise || arg instanceof RMissing) {
                            if (!nonWrapSeen[i]) {
                                CompilerDirectives.transferToInterpreterAndInvalidate();
                                nonWrapSeen[i] = true;
                            }
                        } else {
                            if (!wrapSeen[i]) {
                                CompilerDirectives.transferToInterpreterAndInvalidate();
                                wrapSeen[i] = true;
                            }
                            arg = createPromise(arg);
                        }
                    }
                }
                result[i] = arg;
            }
            return result;
        }

        private final VectorLengthProfile varArgProfile = VectorLengthProfile.create();

        private void forcePromises(VirtualFrame frame, RArgsValuesAndNames varArgs) {
            if (varArgsPromiseHelper == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                varArgsPromiseHelper = insert(new PromiseCheckHelperNode());
            }
            varArgProfile.profile(varArgs.getLength());
            int cachedLength = varArgProfile.getCachedLength();
            if (cachedLength >= 0) {
                forcePromisesUnrolled(frame, varArgs, cachedLength);
            } else {
                forcePromisesDynamic(frame, varArgs);
            }
        }

        @ExplodeLoop
        private void forcePromisesUnrolled(VirtualFrame frame, RArgsValuesAndNames varArgs, int length) {
            Object[] array = varArgs.getArguments();
            for (int i = 0; i < length; i++) {
                array[i] = varArgsPromiseHelper.checkEvaluate(frame, array[i]);
            }
        }

        private void forcePromisesDynamic(VirtualFrame frame, RArgsValuesAndNames varArgs) {
            Object[] array = varArgs.getArguments();
            for (int i = 0; i < array.length; i++) {
                array[i] = varArgsPromiseHelper.checkEvaluate(frame, array[i]);
            }
        }

        private static void wrapPromises(RArgsValuesAndNames varArgs) {
            Object[] array = varArgs.getArguments();
            for (int i = 0; i < array.length; i++) {
                Object arg = array[i];
                if (!(arg instanceof RPromise || arg instanceof RMissing)) {
                    array[i] = createPromise(arg);
                }
            }
        }

        @TruffleBoundary
        private static Object createPromise(Object arg) {
            return RDataFactory.createEvaluatedPromise(Closure.create(ConstantNode.create(arg)), arg);
        }

        @Override
        public Object execute(VirtualFrame frame, RFunction currentFunction, RArgsValuesAndNames orderedArguments, S3Args s3Args) {
            Object result = builtin.executeBuiltin(frame, castArguments(frame, orderedArguments.getArguments()));
            assert result != null : "builtins cannot return 'null': " + builtinDescriptor.getName();
            assert !(result instanceof RConnection) : "builtins cannot return connection': " + builtinDescriptor.getName();
            visibility.execute(frame, builtinDescriptor.getVisibility());
            return result;
        }
    }

    private static final class DispatchedCallNode extends LeafCallNode {

        @Child private CallRFunctionNode call;
        @Child private RFastPathNode fastPath;
        @Child private SetVisibilityNode visibility;

        private final RootCallTarget cachedTarget;
        private final FastPathFactory fastPathFactory;
        private final RVisibility fastPathVisibility;

        DispatchedCallNode(RootCallTarget cachedTarget, RCallNode originalCall) {
            super(originalCall);
            RRootNode root = (RRootNode) cachedTarget.getRootNode();
            this.cachedTarget = cachedTarget;
            this.fastPathFactory = root.getFastPath();
            this.fastPath = fastPathFactory == null ? null : fastPathFactory.create();
            this.fastPathVisibility = fastPathFactory == null ? null : fastPathFactory.getVisibility();
            this.visibility = fastPathFactory == null ? null : SetVisibilityNode.create();
            if (root.containsDispatch()) {
                originalCall.setNeedsCallerFrame();
            }
        }

        @Override
        public Object execute(VirtualFrame frame, RFunction function, RArgsValuesAndNames orderedArguments, S3Args s3Args) {
            if (fastPath != null) {
                Object result = fastPath.execute(frame, orderedArguments.getArguments());
                if (result != null) {
                    assert fastPathVisibility != null;
                    visibility.execute(frame, fastPathVisibility);
                    return result;
                }
                CompilerDirectives.transferToInterpreterAndInvalidate();
                fastPath = null;
                visibility = null;
            }

            if (call == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                call = insert(CallRFunctionNode.create(cachedTarget));
                if (needsSplitting(cachedTarget)) {
                    call.getCallNode().cloneCallTarget();
                }
            }
            MaterializedFrame callerFrame = /* CompilerDirectives.inInterpreter() || */originalCall.needsNoCallerFrame.isValid() ? null : frame.materialize();

            return call.execute(frame, function, originalCall.createCaller(frame, function), callerFrame, orderedArguments.getArguments(), orderedArguments.getSignature(),
                            function.getEnclosingFrame(), s3Args);
        }
    }

    @Override
    public RSyntaxElement getSyntaxLHS() {
        return getFunction() == null ? RSyntaxLookup.createDummyLookup(RSyntaxNode.LAZY_DEPARSE, "FUN", true) : getFunction().asRSyntaxNode();
    }

    @Override
    public ArgumentsSignature getSyntaxSignature() {
        return signature == null ? ArgumentsSignature.empty(1) : signature;
    }

    @Override
    public RSyntaxElement[] getSyntaxArguments() {
        return arguments == null ? new RSyntaxElement[]{RSyntaxLookup.createDummyLookup(RSyntaxNode.LAZY_DEPARSE, "...", false)} : arguments;
    }
}
