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
package com.oracle.truffle.r.nodes.function;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.NodeUtil;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.nodes.RASTUtils;
import com.oracle.truffle.r.nodes.RRootNode;
import com.oracle.truffle.r.nodes.access.ConstantNode;
import com.oracle.truffle.r.nodes.access.FrameSlotNode;
import com.oracle.truffle.r.nodes.access.variables.LocalReadVariableNode;
import com.oracle.truffle.r.nodes.access.variables.ReadVariableNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinRootNode;
import com.oracle.truffle.r.nodes.function.PromiseHelperNode.PromiseCheckHelperNode;
import com.oracle.truffle.r.nodes.function.RCallNodeGen.FunctionDispatchNodeGen;
import com.oracle.truffle.r.nodes.function.S3FunctionLookupNode.NoGenericMethodException;
import com.oracle.truffle.r.nodes.function.S3FunctionLookupNode.Result;
import com.oracle.truffle.r.nodes.function.call.PrepareArguments;
import com.oracle.truffle.r.nodes.profile.TruffleBoundaryNode;
import com.oracle.truffle.r.nodes.unary.CastNode;
import com.oracle.truffle.r.runtime.Arguments;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RArguments.S3Args;
import com.oracle.truffle.r.runtime.RBuiltinKind;
import com.oracle.truffle.r.runtime.RCaller;
import com.oracle.truffle.r.runtime.RDeparse;
import com.oracle.truffle.r.runtime.RDispatch;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RSerialize;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RBuiltinDescriptor;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.REmpty;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.data.RPromise.OptType;
import com.oracle.truffle.r.runtime.data.RPromise.PromiseType;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RTypedValue;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.gnur.SEXPTYPE;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
import com.oracle.truffle.r.runtime.nodes.RFastPathNode;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxCall;
import com.oracle.truffle.r.runtime.nodes.RSyntaxConstant;
import com.oracle.truffle.r.runtime.nodes.RSyntaxElement;
import com.oracle.truffle.r.runtime.nodes.RSyntaxLookup;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

class ForcePromiseNode extends RNode {

    @Child private RNode valueNode;
    @Child private PromiseHelperNode promiseHelper;
    private final BranchProfile nonPromiseProfile = BranchProfile.create();

    ForcePromiseNode(RNode valueNode) {
        this.valueNode = valueNode;
    }

    public RNode getValueNode() {
        return valueNode;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        Object value = valueNode.execute(frame);
        if (value instanceof RPromise) {
            if (promiseHelper == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                promiseHelper = insert(new PromiseHelperNode());
            }
            return promiseHelper.evaluate(frame, (RPromise) value);
        } else {
            nonPromiseProfile.enter();
            return value;
        }
    }
}

interface CallWithCallerFrame extends RCaller {
    boolean setNeedsCallerFrame();
}

@NodeInfo(cost = NodeCost.NONE)
@NodeChild(value = "function", type = ForcePromiseNode.class)
public abstract class RCallNode extends RNode implements RSyntaxNode, RSyntaxCall, RCaller {

    // currently cannot be RSourceSectionNode because of TruffleDSL restrictions

    @CompilationFinal private SourceSection sourceSectionR;

    @Override
    public final void setSourceSection(SourceSection sourceSection) {
        assert sourceSection != null;
        this.sourceSectionR = sourceSection;
    }

    @Override
    public final SourceSection getSourceSection() {
        return sourceSectionR;
    }

    public abstract Object execute(VirtualFrame frame, Object function);

    protected abstract ForcePromiseNode getFunction();

    private final RSyntaxNode[] arguments;
    private final int[] varArgIndexes;
    private final ArgumentsSignature signature;
    @Child private ReadVariableNode lookupVarArgs;
    protected final LocalReadVariableNode explicitArgs;

    // needed for INTERNAL_GENERIC calls:
    @Child private FunctionDispatch internalDispatchCall;

    @CompilationFinal private boolean needsCallerFrame;

    boolean setNeedsCallerFrame() {
        try {
            return needsCallerFrame;
        } finally {
            needsCallerFrame = true;
        }
    }

    @Override
    public RSyntaxNode getSyntaxNode() {
        return this;
    }

    protected RCaller createCaller(VirtualFrame frame, RFunction function) {
        if (explicitArgs == null) {
            return this;
        } else {
            return new RCallerHelper.Representation(function, (RArgsValuesAndNames) explicitArgs.execute(frame));
        }
    }

    protected RCallNode(SourceSection sourceSection, RSyntaxNode[] arguments, ArgumentsSignature signature) {
        assert sourceSection != null;
        this.sourceSectionR = sourceSection;
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
        this.sourceSectionR = sourceSection;
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
        return new Arguments<>(arguments, signature);
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
        return FunctionDispatchNodeGen.create(this, null, explicitArgs == null ? false : true);
    }

    protected FunctionDispatch createUninitializedExplicitCall() {
        return FunctionDispatchNodeGen.create(this, null, true);
    }

    /**
     * If there are no parameters, or the target function does not refer to a builtin, or the
     * builtin has no special dispatching, then we know that we will just call the function with no
     * special dispatch logic.
     */
    protected boolean isDefaultDispatch(RFunction function) {
        return (signature != null && signature.isEmpty()) || function.getRBuiltin() == null || function.getRBuiltin().getDispatch() == RDispatch.DEFAULT;
    }

    @Specialization(guards = "isDefaultDispatch(function)")
    public Object call(VirtualFrame frame, RFunction function, //
                    @Cached("createUninitializedCall()") FunctionDispatch call) {
        return call.execute(frame, function, lookupVarArgs(frame), null);
    }

    protected RNode createDispatchArgument(int index) {
        return new ForcePromiseNode(NodeUtil.cloneNode(arguments[index].asRNode()));
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
    public Object callInternalGeneric(VirtualFrame frame, RFunction function, //
                    @Cached("createDispatchArgument(0)") RNode dispatchArgument, //
                    @Cached("new()") TemporarySlotNode dispatchTempSlot, //
                    @Cached("create()") ClassHierarchyNode classHierarchyNode, //
                    @Cached("createWithError()") S3FunctionLookupNode dispatchLookup, //
                    @Cached("createIdentityProfile()") ValueProfile builtinProfile, //
                    @Cached("createBinaryProfile()") ConditionProfile implicitTypeProfile, //
                    @Cached("createBinaryProfile()") ConditionProfile resultIsBuiltinProfile) {
        RBuiltinDescriptor builtin = builtinProfile.profile(function.getRBuiltin());
        Object dispatchObject = dispatchArgument.execute(frame);
        FrameSlot slot = dispatchTempSlot.initialize(frame, dispatchObject, () -> internalDispatchCall = null);
        try {
            RStringVector type = classHierarchyNode.execute(dispatchObject);
            S3Args s3Args;
            RFunction resultFunction;
            if (implicitTypeProfile.profile(type != null)) {
                Result result = dispatchLookup.execute(frame, builtin.getName(), type, null, frame.materialize(), null);
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
            if (internalDispatchCall == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                internalDispatchCall = insert(FunctionDispatchNodeGen.create(this, new Object[]{dispatchTempSlot.getIdentifier()}, false));
            }
            return internalDispatchCall.execute(frame, resultFunction, lookupVarArgs(frame), s3Args);
        } finally {
            dispatchTempSlot.cleanup(frame, slot);
        }
    }

    @Specialization(guards = {"explicitArgs != null", "isInternalGenericDispatch(function)"})
    public Object callInternalGenericExplicit(VirtualFrame frame, RFunction function, //
                    @Cached("create()") ClassHierarchyNode classHierarchyNode, //
                    @Cached("createWithError()") S3FunctionLookupNode dispatchLookup, //
                    @Cached("createIdentityProfile()") ValueProfile builtinProfile, //
                    @Cached("createBinaryProfile()") ConditionProfile implicitTypeProfile, //
                    @Cached("createBinaryProfile()") ConditionProfile resultIsBuiltinProfile, //
                    @Cached("createPromiseHelper()") PromiseCheckHelperNode promiseHelperNode, //
                    @Cached("createUninitializedExplicitCall()") FunctionDispatch call) {
        RBuiltinDescriptor builtin = builtinProfile.profile(function.getRBuiltin());
        RArgsValuesAndNames argAndNames = (RArgsValuesAndNames) explicitArgs.execute(frame);

        RStringVector type = argAndNames.isEmpty() ? null : classHierarchyNode.execute(promiseHelperNode.checkEvaluate(frame, argAndNames.getArgument(0)));
        S3Args s3Args;
        RFunction resultFunction;
        if (implicitTypeProfile.profile(type != null)) {
            Result result = dispatchLookup.execute(frame, builtin.getName(), type, null, frame.materialize(), null);
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
        return call.execute(frame, resultFunction, argAndNames, s3Args);
    }

    protected CallArgumentsNode createArguments() {
        return signature == null ? null : createArguments(null, false, false);
    }

    protected ReadVariableNode createVarArgRead(CallArgumentsNode callArguments) {
        return callArguments.containsVarArgsSymbol() ? ReadVariableNode.createSilent(ArgumentsSignature.VARARG_NAME, RType.Any) : null;
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

    @Specialization(guards = "isGroupGenericDispatch(function)")
    public Object callGroupGeneric(VirtualFrame frame, RFunction function, //
                    @Cached("createArguments()") CallArgumentsNode callArguments, //
                    @Cached("create()") ClassHierarchyNode classHierarchyNodeX, //
                    @Cached("createWithException()") S3FunctionLookupNode dispatchLookupX, //
                    @Cached("create()") ClassHierarchyNode classHierarchyNodeY, //
                    @Cached("createWithException()") S3FunctionLookupNode dispatchLookupY, //
                    @Cached("createIdentityProfile()") ValueProfile builtinProfile, //
                    @Cached("createBinaryProfile()") ConditionProfile implicitTypeProfileX, //
                    @Cached("createBinaryProfile()") ConditionProfile implicitTypeProfileY, //
                    @Cached("createBinaryProfile()") ConditionProfile mismatchProfile, //
                    @Cached("createBinaryProfile()") ConditionProfile resultIsBuiltinProfile, //
                    @Cached("createPromiseHelper()") PromiseCheckHelperNode promiseHelperNode, //
                    @Cached("createUninitializedExplicitCall()") FunctionDispatch call) {

        Object[] args = explicitArgs != null ? ((RArgsValuesAndNames) explicitArgs.execute(frame)).getArguments() : callArguments.evaluateFlattenObjects(frame, lookupVarArgs(frame));

        RBuiltinDescriptor builtin = builtinProfile.profile(function.getRBuiltin());
        RDispatch dispatch = builtin.getDispatch();

        RStringVector typeX = classHierarchyNodeX.execute(promiseHelperNode.checkEvaluate(frame, args[0]));
        Result resultX = null;
        if (implicitTypeProfileX.profile(typeX != null)) {
            try {
                resultX = dispatchLookupX.execute(frame, builtin.getName(), typeX, dispatch.getGroupGenericName(), frame.materialize(), null);
            } catch (NoGenericMethodException e) {
                // fall-through
            }
        }
        Result resultY = null;
        if (args.length > 1 && dispatch == RDispatch.OPS_GROUP_GENERIC) {
            RStringVector typeY = classHierarchyNodeY.execute(promiseHelperNode.checkEvaluate(frame, args[1]));
            if (implicitTypeProfileY.profile(typeY != null)) {
                try {
                    resultY = dispatchLookupY.execute(frame, builtin.getName(), typeY, dispatch.getGroupGenericName(), frame.materialize(), null);
                } catch (NoGenericMethodException e) {
                    // fall-through
                }
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
        ArgumentsSignature argsSignature = explicitArgs != null ? ((RArgsValuesAndNames) explicitArgs.execute(frame)).getSignature() : callArguments.flattenNames(lookupVarArgs(frame));
        return call.execute(frame, resultFunction, new RArgsValuesAndNames(args, argsSignature), s3Args);
    }

    protected class ForeignCall extends Node {

        @Child private CallArgumentsNode arguments;
        @Child private Node foreignCall;
        @CompilationFinal private int foreignCallArgCount;

        private final BranchProfile errorProfile = BranchProfile.create();

        public ForeignCall(CallArgumentsNode arguments) {
            this.arguments = arguments;
        }

        @SuppressWarnings("deprecation")
        public Object execute(VirtualFrame frame, TruffleObject function) {
            Object[] argumentsArray = explicitArgs != null ? ((RArgsValuesAndNames) explicitArgs.execute(frame)).getArguments() : arguments.evaluateFlattenObjects(frame, lookupVarArgs(frame));
            if (foreignCall == null || foreignCallArgCount != argumentsArray.length) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                foreignCall = insert(Message.createExecute(argumentsArray.length).createNode());
                foreignCallArgCount = argumentsArray.length;
            }
            try {
                return ForeignAccess.execute(foreignCall, frame, function, argumentsArray);
            } catch (Throwable e) {
                errorProfile.enter();
                throw RError.error(this, RError.Message.GENERIC, "Foreign function failed: " + e.getMessage() != null ? e.getMessage() : e.toString());
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
    public Object call(VirtualFrame frame, TruffleObject function, //
                    @Cached("createForeignCall()") ForeignCall foreignCall) {
        return foreignCall.execute(frame, function);
    }

    @TruffleBoundary
    @Fallback
    public Object call(@SuppressWarnings("unused") Object function) {
        throw RError.error(RError.SHOW_CALLER, RError.Message.APPLY_NON_FUNCTION);
    }

    public RNode getFunctionNode() {
        if (getFunction() != null) {
            // Only the very 1. RootCallNode on the top level contains the one-and-only function
            // node
            return getFunction().getValueNode();
        }
        return getParentCallNode().getFunctionNode();
    }

    public CallArgumentsNode createArguments(Object[] dispatchTempIdentifiers, boolean modeChange, boolean modeChangeAppliesToAll) {
        RNode[] args = new RNode[arguments.length];
        for (int i = 0; i < arguments.length; i++) {
            if (dispatchTempIdentifiers != null && i < dispatchTempIdentifiers.length) {
                args[i] = new GetTempNode(dispatchTempIdentifiers[i], arguments[i]);
            } else {
                args[i] = arguments[i] == null ? null : RASTUtils.cloneNode(arguments[i].asRNode());
            }
        }
        return CallArgumentsNode.create(modeChange, modeChangeAppliesToAll, args, signature, varArgIndexes);
    }

    @Override
    public void serializeImpl(RSerialize.State state) {
        state.setAsLangType();
        state.serializeNodeSetCar(getFunctionNode());
        if (isColon(getFunctionNode())) {
            // special case, have to translate Strings to Symbols
            state.openPairList();
            state.setCarAsSymbol(((ReadVariableNode) arguments[0]).getIdentifier());
            state.openPairList();
            state.setCarAsSymbol(((ReadVariableNode) arguments[1]).getIdentifier());
            state.linkPairList(2);
            state.setCdr(state.closePairList());
        } else {
            RSyntaxNode f = getFunctionNode().asRSyntaxNode();
            boolean infixFieldAccess = false;
            if (f instanceof RSyntaxLookup) {
                RSyntaxLookup lookup = (RSyntaxLookup) f;
                infixFieldAccess = "$".equals(lookup.getIdentifier()) || "@".equals(lookup.getIdentifier());
            }
            serializeArguments(state, arguments, signature, infixFieldAccess);
        }
    }

    static void serializeArguments(RSerialize.State state, RSyntaxNode[] arguments, ArgumentsSignature signature, boolean infixFieldAccess) {
        state.openPairList(SEXPTYPE.LISTSXP);
        if (arguments.length == 0) {
            state.setNull();
        } else {
            for (int i = 0; i < arguments.length; i++) {
                RSyntaxNode argument = arguments[i];
                String name = signature.getName(i);
                if (name != null) {
                    state.setTagAsSymbol(name);
                }
                if (argument == null) {
                    state.setCarMissing();
                } else {
                    if (infixFieldAccess && i == 1 && argument instanceof RSyntaxConstant) {
                        RSyntaxConstant c = (RSyntaxConstant) argument;
                        String identifier = RRuntime.asStringLengthOne(c.getValue());
                        assert identifier != null;
                        state.setCarAsSymbol(identifier);
                    } else {
                        state.serializeNodeSetCar(argument);
                    }
                }
                if (i != arguments.length - 1) {
                    state.openPairList();
                }

            }
            state.linkPairList(arguments.length);
        }
        state.setCdr(state.closePairList());
    }

    private static boolean isColon(RNode node) {
        if (node instanceof ReadVariableNode) {
            ReadVariableNode rvn = (ReadVariableNode) node;
            String name = rvn.getIdentifier();
            return name.equals("::") || name.equals(":::");
        }
        return false;
    }

    @Override
    public RSyntaxNode substituteImpl(REnvironment env) {
        RNode functionSub = getFunctionNode().substitute(env).asRNode();

        Arguments<RSyntaxNode> argsSub = substituteArguments(env, arguments, signature);
        return RASTUtils.createCall(functionSub, false, argsSub.getSignature(), argsSub.getArguments());
    }

    static Arguments<RSyntaxNode> substituteArguments(REnvironment env, RSyntaxNode[] arguments, ArgumentsSignature signature) {
        ArrayList<RSyntaxNode> newArguments = new ArrayList<>();
        ArrayList<String> newNames = new ArrayList<>();
        for (int i = 0; i < arguments.length; i++) {
            RSyntaxNode argNodeSubs = arguments[i].substituteImpl(env);
            if (argNodeSubs instanceof RASTUtils.MissingDotsNode) {
                // nothing to do
            } else if (argNodeSubs instanceof RASTUtils.ExpandedDotsNode) {
                RASTUtils.ExpandedDotsNode expandedDotsNode = (RASTUtils.ExpandedDotsNode) argNodeSubs;
                newArguments.addAll(Arrays.asList(expandedDotsNode.nodes));
                for (int j = 0; j < expandedDotsNode.nodes.length; j++) {
                    newNames.add(null);
                }
            } else {
                newArguments.add(argNodeSubs);
                newNames.add(signature.getName(i));
            }
        }
        RSyntaxNode[] newArgumentsArray = newArguments.stream().toArray(RSyntaxNode[]::new);
        String[] newNamesArray = newNames.stream().toArray(String[]::new);
        return new Arguments<>(newArgumentsArray, ArgumentsSignature.get(newNamesArray));
    }

    /**
     * Creates a call to a resolved {@link RBuiltinKind#INTERNAL} that will be used to replace the
     * original call.
     *
     * @param internalCallArg the {@link RCallNode} corresponding to the argument to the {code
     *            .Internal}.
     * @param function the resolved {@link RFunction}.
     */
    public static RNode createInternalCall(RCallNode internalCallArg, RFunction function) {
        CompilerAsserts.neverPartOfCompilation();
        return new InternalNode(FunctionDispatchNodeGen.create(internalCallArg, null, false), function, internalCallArg.lookupVarArgs != null);
    }

    @NodeInfo(cost = NodeCost.NONE)
    private static final class InternalNode extends RNode {
        @Child private FunctionDispatch rootCallNode;
        @Child private ReadVariableNode lookupVarArgs;
        private final RFunction function;

        InternalNode(FunctionDispatch rootCallNode, RFunction function, boolean needsVarArgLookup) {
            this.rootCallNode = rootCallNode;
            this.function = function;
            this.lookupVarArgs = needsVarArgLookup ? ReadVariableNode.createSilent(ArgumentsSignature.VARARG_NAME, RType.Any) : null;
        }

        private RArgsValuesAndNames lookupVarArgs(VirtualFrame frame) {
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

        @Override
        public Object execute(VirtualFrame frame) {
            return rootCallNode.execute(frame, function, lookupVarArgs(frame), null);
        }
    }

    /**
     * Creates a modified call in which the first N arguments are replaced by
     * {@code replacementArgs}. This is, for example, to support
     * {@code HiddenInternalFunctions.MakeLazy}, and condition handling.
     */
    @TruffleBoundary
    public static RCallNode createCloneReplacingArgs(RCallNode call, RSyntaxNode... replacementArgs) {
        RSyntaxNode[] args = new RSyntaxNode[call.arguments.length];
        for (int i = 0; i < args.length; i++) {
            args[i] = i < replacementArgs.length ? replacementArgs[i] : call.arguments[i];
        }
        return RCallNodeGen.create(call.getSourceSection(), args, call.signature, new ForcePromiseNode(RASTUtils.cloneNode(call.getFunction())));
    }

    /**
     * The standard way to create a call to {@code function} with given arguments. If
     * {@code src == RSyntaxNode.EAGER_DEPARSE} we force a deparse.
     */
    public static RCallNode createCall(SourceSection src, RNode function, ArgumentsSignature signature, RSyntaxNode... arguments) {
        RCallNode call = RCallNodeGen.create(src, arguments, signature, new ForcePromiseNode(function));
        if (src == RSyntaxNode.EAGER_DEPARSE) {
            RDeparse.ensureSourceSection(call);
        }
        return call;
    }

    public static RCallNode createExplicitCall(Object explicitArgsIdentifier) {
        return RCallNodeGen.create(RSyntaxNode.INTERNAL, explicitArgsIdentifier, null);
    }

    static RBuiltinRootNode findBuiltinRootNode(RootCallTarget callTarget) {
        RootNode root = callTarget.getRootNode();
        if (root instanceof RBuiltinRootNode) {
            return (RBuiltinRootNode) root;
        }
        return null;
    }

    private RCallNode getParentCallNode() {
        RNode parent = (RNode) unwrapParent();
        if (!(parent instanceof RCallNode)) {
            throw RInternalError.shouldNotReachHere();
        }
        return (RCallNode) parent;
    }

    static boolean needsSplitting(RFunction function) {
        RootNode root = function.getRootNode();
        if (function.containsDispatch()) {
            return true;
        } else if (root instanceof RRootNode) {
            return ((RRootNode) root).needsSplitting();
        }
        return false;
    }

    private static final class GetTempNode extends RNode {

        @Child private FrameSlotNode slot;
        private final RSyntaxNode arg;

        GetTempNode(Object identifier, RSyntaxNode arg) {
            slot = FrameSlotNode.createTemp(identifier, false);
            this.arg = arg;
        }

        @Override
        protected RSyntaxNode getRSyntaxNode() {
            return arg;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            try {
                return frame.getObject(slot.executeFrameSlot(frame));
            } catch (FrameSlotTypeException e) {
                throw RInternalError.shouldNotReachHere();
            }
        }
    }

    public abstract static class FunctionDispatch extends Node {

        public abstract Object execute(VirtualFrame frame, RFunction function, Object varArgs, Object s3Args);

        protected static final int CACHE_SIZE = 4;

        private final RCallNode originalCall;
        private final Object[] dispatchTempIdentifiers;
        private final boolean explicitArgs;

        public FunctionDispatch(RCallNode originalCall, Object[] dispatchTempIdentifiers, boolean explicitArgs) {
            this.originalCall = originalCall;
            this.dispatchTempIdentifiers = dispatchTempIdentifiers;
            this.explicitArgs = explicitArgs;
        }

        protected LeafCallNode createCacheNode(RFunction function) {
            CompilerAsserts.neverPartOfCompilation();
            FormalArguments formals = ((RRootNode) function.getTarget().getRootNode()).getFormalArguments();
            if (function.isBuiltin()) {
                return new BuiltinCallNode(RBuiltinNode.inline(function.getRBuiltin(), null), function.getRBuiltin(), formals, originalCall);
            } else {
                return new DispatchedCallNode(function, formals.getSignature(), originalCall);
            }
        }

        protected PrepareArguments createArguments(RFunction function, boolean noOpt) {
            if (explicitArgs) {
                return PrepareArguments.createExplicit(function);
            } else {
                CallArgumentsNode args = originalCall.createArguments(dispatchTempIdentifiers, !function.isBuiltin(), true);
                return PrepareArguments.create(function, args, noOpt);
            }
        }

        protected PrepareArguments createArguments(RFunction function) {
            return createArguments(function, false);
        }

        @Specialization(limit = "CACHE_SIZE", guards = "function == cachedFunction")
        protected Object dispatch(VirtualFrame frame, @SuppressWarnings("unused") RFunction function, Object varArgs, Object s3Args, //
                        @Cached("function") RFunction cachedFunction, //
                        @Cached("createCacheNode(cachedFunction)") LeafCallNode leafCall, //
                        @Cached("createArguments(cachedFunction)") PrepareArguments prepareArguments) {
            Object[] orderedArguments = prepareArguments.execute(frame, (RArgsValuesAndNames) varArgs, originalCall);
            return leafCall.execute(frame, cachedFunction, orderedArguments, (S3Args) s3Args);
        }

        /*
         * Use a TruffleBoundaryNode to be able to switch child nodes without invalidating the whole
         * method.
         */
        protected final class GenericCall extends TruffleBoundaryNode {

            private RFunction cachedFunction;
            @Child private LeafCallNode leafCall;
            @Child private PrepareArguments prepareArguments;

            @TruffleBoundary
            public Object execute(MaterializedFrame materializedFrame, RFunction function, Object varArgs, Object s3Args) {
                if (cachedFunction != function) {
                    cachedFunction = function;
                    leafCall = insert(createCacheNode(function));
                    prepareArguments = insert(createArguments(function, true));
                }
                VirtualFrame frame = SubstituteVirtualFrame.create(materializedFrame);
                Object[] orderedArguments = prepareArguments.execute(frame, (RArgsValuesAndNames) varArgs, originalCall);
                return leafCall.execute(frame, cachedFunction, orderedArguments, (S3Args) s3Args);
            }
        }

        protected GenericCall createGenericCall() {
            return new GenericCall();
        }

        @Specialization
        protected Object dispatchFallback(VirtualFrame frame, RFunction function, Object varArgs, Object s3Args, //
                        @Cached("createGenericCall()") GenericCall generic) {
            return generic.execute(frame.materialize(), function, varArgs, s3Args);
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

        public abstract Object execute(VirtualFrame frame, RFunction function, Object[] orderedArguments, S3Args s3Args);
    }

    @NodeInfo(cost = NodeCost.NONE)
    private static final class BuiltinCallNode extends LeafCallNode {

        @Child private RBuiltinNode builtin;
        @Child private PromiseCheckHelperNode promiseHelper;
        @Children private final CastNode[] casts;

        private final BranchProfile emptyProfile = BranchProfile.create();
        private final BranchProfile varArgsProfile = BranchProfile.create();
        private final ConditionProfile wrapProfile = ConditionProfile.createBinaryProfile();
        private final FormalArguments formals;
        private final RBuiltinDescriptor builtinDescriptor;

        BuiltinCallNode(RBuiltinNode builtin, RBuiltinDescriptor builtinDescriptor, FormalArguments formalArguments, RCallNode originalCall) {
            super(originalCall);
            this.builtin = builtin;
            this.builtinDescriptor = builtinDescriptor;
            this.casts = builtin.getCasts();
            this.formals = formalArguments;
        }

        @ExplodeLoop
        public Object[] castArguments(VirtualFrame frame, Object[] args) {
            int argCount = formals.getLength();
            Object[] result = new Object[argCount];
            for (int i = 0; i < argCount; i++) {
                Object arg = args[i];
                if (arg == REmpty.instance) {
                    emptyProfile.enter();
                    arg = formals.getInternalDefaultArgumentAt(i);
                }
                if (arg instanceof RArgsValuesAndNames) {
                    varArgsProfile.enter();
                    RArgsValuesAndNames varArgs = (RArgsValuesAndNames) arg;
                    if (builtinDescriptor.evaluatesArg(i)) {
                        forcePromises(frame, varArgs);
                    } else {
                        wrapPromises(varArgs);
                    }
                } else {
                    if (builtinDescriptor.evaluatesArg(i)) {
                        if (arg instanceof RPromise) {
                            if (promiseHelper == null) {
                                CompilerDirectives.transferToInterpreterAndInvalidate();
                                promiseHelper = insert(new PromiseCheckHelperNode());
                            }
                            arg = promiseHelper.checkEvaluate(frame, arg);

                        }
                        if (i < casts.length && casts[i] != null) {
                            assert builtinDescriptor.evaluatesArg(i);
                            arg = casts[i].execute(arg);
                        }
                    } else {
                        assert casts.length <= i || casts[i] == null : "no casts allowed on non-evaluated arguments";
                        if (wrapProfile.profile(!(arg instanceof RPromise || arg instanceof RMissing))) {
                            arg = createPromise(arg);
                        }
                    }
                }
                result[i] = arg;
            }
            return result;
        }

        private void forcePromises(VirtualFrame frame, RArgsValuesAndNames varArgs) {
            Object[] array = varArgs.getArguments();
            for (int i = 0; i < array.length; i++) {
                if (promiseHelper == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    promiseHelper = insert(new PromiseCheckHelperNode());
                }
                array[i] = promiseHelper.checkEvaluate(frame, array[i]);
            }
        }

        @TruffleBoundary
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
            return RDataFactory.createPromise(PromiseType.NO_ARG, OptType.PROMISED, ConstantNode.create(arg), arg);
        }

        @Override
        public Object execute(VirtualFrame frame, RFunction currentFunction, Object[] orderedArguments, S3Args s3Args) {
            return builtin.execute(frame, castArguments(frame, orderedArguments));
        }
    }

    private static final class DispatchedCallNode extends LeafCallNode {

        @Child private DirectCallNode call;
        @Child private RFastPathNode fastPath;

        private final ArgumentsSignature signature;
        private final RFunction cachedFunction;

        DispatchedCallNode(RFunction function, ArgumentsSignature signature, RCallNode originalCall) {
            super(originalCall);
            this.cachedFunction = function;
            this.signature = signature;
            this.fastPath = function.getFastPath() == null ? null : function.getFastPath().create();
            originalCall.needsCallerFrame |= cachedFunction.containsDispatch();
        }

        @Override
        public Object execute(VirtualFrame frame, RFunction currentFunction, Object[] orderedArguments, S3Args s3Args) {
            if (fastPath != null) {
                Object result = fastPath.execute(frame, orderedArguments);
                if (result != null) {
                    return result;
                }
                CompilerDirectives.transferToInterpreterAndInvalidate();
                fastPath = null;
            }

            if (call == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                call = insert(Truffle.getRuntime().createDirectCallNode(cachedFunction.getTarget()));
                if (needsSplitting(cachedFunction)) {
                    call.cloneCallTarget();
                }
            }
            MaterializedFrame callerFrame = /* CompilerDirectives.inInterpreter() || */originalCall.needsCallerFrame ? frame.materialize() : null;
            Object[] argsObject = RArguments.create(cachedFunction, originalCall.createCaller(frame, cachedFunction), callerFrame, RArguments.getDepth(frame) + 1, RArguments.getPromiseFrame(frame),
                            orderedArguments, signature, cachedFunction.getEnclosingFrame(), s3Args);
            return call.call(frame, argsObject);
        }
    }

    @Override
    public RSyntaxElement getSyntaxLHS() {
        return getFunction() == null ? RSyntaxLookup.createDummyLookup(RSyntaxNode.LAZY_DEPARSE, "FUN", true) : getFunctionNode().asRSyntaxNode();
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
