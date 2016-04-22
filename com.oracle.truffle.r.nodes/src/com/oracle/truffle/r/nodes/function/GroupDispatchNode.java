/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.function;

import java.util.Arrays;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.nodes.RASTUtils;
import com.oracle.truffle.r.nodes.access.variables.ReadVariableNode;
import com.oracle.truffle.r.nodes.function.S3FunctionLookupNode.NoGenericMethodException;
import com.oracle.truffle.r.nodes.function.S3FunctionLookupNode.Result;
import com.oracle.truffle.r.runtime.Arguments;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RArguments.S3Args;
import com.oracle.truffle.r.runtime.RDispatch;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RSerialize;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.nodes.RSourceSectionNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxCall;
import com.oracle.truffle.r.runtime.nodes.RSyntaxElement;
import com.oracle.truffle.r.runtime.nodes.RSyntaxLookup;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

public final class GroupDispatchNode extends RSourceSectionNode implements RSyntaxNode, RSyntaxCall {

    @Child private CallArgumentsNode callArgsNode;
    @Child private S3FunctionLookupNode functionLookupL;
    @Child private S3FunctionLookupNode functionLookupR;
    @Child private ClassHierarchyNode classHierarchyL;
    @Child private ClassHierarchyNode classHierarchyR;
    @Child private CallMatcherNode callMatcher = CallMatcherNode.create(false, true);
    @Child private ReadVariableNode lookupVarArgs;

    private final String fixedGenericName;
    private final RDispatch fixedDispatch;
    private final RFunction fixedBuiltinFunction;

    private final ConditionProfile mismatchProfile = ConditionProfile.createBinaryProfile();

    @CompilationFinal private boolean dynamicLookup;
    private final ConditionProfile exactEqualsProfile = ConditionProfile.createBinaryProfile();

    private GroupDispatchNode(SourceSection sourceSection, String genericName, CallArgumentsNode callArgNode, RFunction builtinFunction) {
        super(sourceSection);
        this.fixedGenericName = genericName.intern();
        this.fixedDispatch = builtinFunction.getRBuiltin().getDispatch();
        this.callArgsNode = callArgNode;
        this.fixedBuiltinFunction = builtinFunction;
    }

    public static GroupDispatchNode create(String genericName, SourceSection sourceSection, ArgumentsSignature signature, RSyntaxNode... arguments) {
        CallArgumentsNode callArgNode = CallArgumentsNode.create(false, true, Arrays.copyOf(arguments, arguments.length, RNode[].class), signature);
        GroupDispatchNode gdcn = new GroupDispatchNode(sourceSection, genericName, callArgNode, RContext.lookupBuiltin(genericName));
        return gdcn;
    }

    public static GroupDispatchNode create(String genericName, CallArgumentsNode callArgNode, RFunction builtinFunction, SourceSection sourceSection) {
        GroupDispatchNode gdcn = new GroupDispatchNode(sourceSection, genericName, callArgNode, builtinFunction);
        return gdcn;
    }

    public Arguments<RSyntaxNode> getArguments() {
        return new Arguments<>(callArgsNode.getSyntaxArguments(), callArgsNode.getSignature());
    }

    public String getGenericName() {
        return fixedGenericName;
    }

    public SourceSection getCallSrc() {
        return getSourceSection();
    }

    @Override
    public void serializeImpl(RSerialize.State state) {
        String name = getGenericName();
        state.setAsBuiltin(name);
        RCallNode.serializeArguments(state, callArgsNode.getSyntaxArguments(), callArgsNode.signature, false);
    }

    @Override
    public RSyntaxNode substituteImpl(REnvironment env) {
        // TODO substitute aDispatchNode
        Arguments<RSyntaxNode> substituteArguments = RCallNode.substituteArguments(env, callArgsNode.getSyntaxArguments(), callArgsNode.signature);
        return RASTUtils.createCall(this, false, substituteArguments.getSignature(), substituteArguments.getArguments());
    }

    @Override
    public Object execute(VirtualFrame frame) {
        RArgsValuesAndNames varArgs = null;
        if (callArgsNode.containsVarArgsSymbol()) {
            if (lookupVarArgs == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                lookupVarArgs = insert(ReadVariableNode.createSilent(ArgumentsSignature.VARARG_NAME, RType.Any));
            }
            try {
                varArgs = lookupVarArgs.executeRArgsValuesAndNames(frame);
            } catch (UnexpectedResultException e) {
                throw RInternalError.shouldNotReachHere(e, "'...' should always be represented by RArgsValuesAndNames");
            }
        }
        RArgsValuesAndNames argAndNames = callArgsNode.evaluateFlatten(frame, varArgs);
        return executeInternal(frame, argAndNames, fixedGenericName, fixedDispatch, fixedBuiltinFunction);
    }

    public Object executeDynamic(VirtualFrame frame, RArgsValuesAndNames argAndNames, String genericName, RDispatch dispatch, RFunction builtinFunction) {
        if (!dynamicLookup) {
            if (builtinFunction == fixedBuiltinFunction && (exactEqualsProfile.profile(fixedGenericName == genericName) || fixedGenericName.equals(genericName))) {
                return executeInternal(frame, argAndNames, fixedGenericName, fixedDispatch, fixedBuiltinFunction);
            }
            CompilerDirectives.transferToInterpreterAndInvalidate();
            dynamicLookup = true;
        }
        return executeInternal(frame, argAndNames, genericName, dispatch, builtinFunction);
    }

    private Object executeInternal(VirtualFrame frame, RArgsValuesAndNames argAndNames, String genericName, RDispatch dispatch, RFunction builtinFunction) {
        assert dispatch.isGroupGeneric();
        Object[] evaluatedArgs = argAndNames.getArguments();

        if (classHierarchyL == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            classHierarchyL = insert(ClassHierarchyNodeGen.create(false, true));
        }
        RStringVector typeL = evaluatedArgs.length == 0 ? null : classHierarchyL.execute(evaluatedArgs[0]);

        Result resultL = null;
        if (typeL != null) {
            try {
                if (functionLookupL == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    functionLookupL = insert(S3FunctionLookupNode.create(false, false));
                }
                resultL = functionLookupL.execute(frame, genericName, typeL, dispatch.getGroupGenericName(), frame.materialize(), null);
            } catch (NoGenericMethodException e) {
                // fall-through
            }
        }
        Result resultR = null;
        if (dispatch == RDispatch.OPS_GROUP_GENERIC && argAndNames.getSignature().getLength() >= 2) {
            if (classHierarchyR == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                classHierarchyR = insert(ClassHierarchyNodeGen.create(false, true));
            }
            RStringVector typeR = classHierarchyR.execute(evaluatedArgs[1]);
            if (typeR != null) {
                try {
                    if (functionLookupR == null) {
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                        functionLookupR = insert(S3FunctionLookupNode.create(false, false));
                    }
                    resultR = functionLookupR.execute(frame, genericName, typeR, dispatch.getGroupGenericName(), frame.materialize(), null);
                } catch (NoGenericMethodException e) {
                    // fall-through
                }
            }
        }

        Result result;
        RStringVector dotMethod;
        if (resultL == null) {
            if (resultR == null) {
                result = null;
                dotMethod = null;
            } else {
                result = resultR;
                dotMethod = RDataFactory.createStringVector(new String[]{"", result.targetFunctionName}, true);
            }
        } else {
            if (resultR == null) {
                result = resultL;
                dotMethod = RDataFactory.createStringVector(new String[]{result.targetFunctionName, ""}, true);
            } else {
                if (mismatchProfile.profile(resultL.function != resultR.function)) {
                    RError.warning(this, RError.Message.INCOMPATIBLE_METHODS, resultL.targetFunctionName, resultR.targetFunctionName, genericName);
                    result = null;
                    dotMethod = null;
                } else {
                    result = resultL;
                    dotMethod = RDataFactory.createStringVector(new String[]{result.targetFunctionName, result.targetFunctionName}, true);
                }
            }
        }
        ArgumentsSignature signature = argAndNames.getSignature();
        S3Args s3Args;
        RFunction function;
        String functionName;
        if (result == null) {
            s3Args = null;
            function = builtinFunction;
            functionName = function.getName();
        } else {
            s3Args = new S3Args(genericName, result.clazz, dotMethod, frame.materialize(), null, result.groupMatch ? dispatch.getGroupGenericName() : null);
            function = result.function;
            functionName = result.targetFunctionName;
        }
        if (function == null) {
            CompilerDirectives.transferToInterpreter();
            throw RError.nyi(this, "missing builtin function '" + genericName + "'");
        }
        return callMatcher.execute(frame, signature, evaluatedArgs, function, functionName, s3Args);
    }

    @Override
    public RSyntaxElement getSyntaxLHS() {
        return RSyntaxLookup.createDummyLookup(null, fixedGenericName, true);
    }

    @Override
    public RSyntaxElement[] getSyntaxArguments() {
        return callArgsNode.getSyntaxArguments();
    }

    @Override
    public ArgumentsSignature getSyntaxSignature() {
        return callArgsNode.getSignature();
    }
}
