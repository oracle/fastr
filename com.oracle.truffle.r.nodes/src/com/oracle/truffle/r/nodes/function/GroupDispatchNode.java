/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2015, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.function;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.function.S3FunctionLookupNode.NoGenericMethodException;
import com.oracle.truffle.r.nodes.function.S3FunctionLookupNode.Result;
import com.oracle.truffle.r.nodes.runtime.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.RArguments.S3Args;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.env.*;
import com.oracle.truffle.r.runtime.gnur.*;

public final class GroupDispatchNode extends RNode implements RSyntaxNode {

    @Child private CallArgumentsNode callArgsNode;
    @Child private S3FunctionLookupNode functionLookupL;
    @Child private S3FunctionLookupNode functionLookupR;
    @Child private CallMatcherNode callMatcher = CallMatcherNode.create(false, true);

    private final String fixedGenericName;
    private final RGroupGenerics fixedGroup;
    private final RFunction fixedBuiltinFunction;

    private final RAttributeProfiles attrProfiles = RAttributeProfiles.create();
    private final ConditionProfile mismatchProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile isObjectProfile = ConditionProfile.createBinaryProfile();
    private final ValueProfile argTypeProfile = ValueProfile.createClassProfile();

    @CompilationFinal private boolean dynamicLookup;
    private final ConditionProfile exactEqualsProfile = ConditionProfile.createBinaryProfile();

    private GroupDispatchNode(String genericName, CallArgumentsNode callArgNode, RFunction builtinFunction) {
        this.fixedGenericName = genericName.intern();
        this.fixedGroup = RGroupGenerics.getGroup(genericName);
        this.callArgsNode = callArgNode;
        this.fixedBuiltinFunction = builtinFunction;
    }

    public static GroupDispatchNode create(String genericName, CallArgumentsNode callArgNode, SourceSection callSrc) {
        GroupDispatchNode gdcn = new GroupDispatchNode(genericName, callArgNode, RContext.lookupBuiltin(genericName));
        gdcn.assignSourceSection(callSrc);
        return gdcn;
    }

    public static GroupDispatchNode create(String genericName, CallArgumentsNode callArgNode, RFunction builtinFunction, SourceSection callSrc) {
        GroupDispatchNode gdcn = new GroupDispatchNode(genericName, callArgNode, builtinFunction);
        gdcn.assignSourceSection(callSrc);
        return gdcn;
    }

    public String getGenericName() {
        return fixedGenericName;
    }

    public SourceSection getCallSrc() {
        return getSourceSection();
    }

    @Override
    public void deparse(RDeparse.State state) {
        String name = getGenericName();
        RDeparse.Func func = RDeparse.getFunc(name);
        if (func != null) {
            // infix operator
            RASTDeparse.deparseInfixOperator(state, this, func);
        } else {
            state.append(name);
            callArgsNode.deparse(state);
        }
    }

    @Override
    public void serialize(RSerialize.State state) {
        String name = getGenericName();
        state.setAsBuiltin(name);
        state.serializeNodeSetCdr(callArgsNode, SEXPTYPE.LISTSXP);
    }

    @Override
    public RSyntaxNode substitute(REnvironment env) {
        // TODO substitute aDispatchNode
        return RSyntaxNode.cast(RASTUtils.createCall(this, (CallArgumentsNode) callArgsNode.substitute(env).asRNode()));
    }

    protected RStringVector getArgClass(Object arg) {
        Object profiledArg = argTypeProfile.profile(arg);
        if (profiledArg instanceof RAbstractContainer && isObjectProfile.profile(((RAbstractContainer) profiledArg).isObject(attrProfiles))) {
            return ((RAbstractContainer) profiledArg).getClassHierarchy();
        }
        return null;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        RArgsValuesAndNames argAndNames = callArgsNode.evaluateFlatten(frame);
        return executeInternal(frame, argAndNames, fixedGenericName, fixedGroup, fixedBuiltinFunction);
    }

    public Object executeDynamic(VirtualFrame frame, RArgsValuesAndNames argAndNames, String genericName, RGroupGenerics group, RFunction builtinFunction) {
        if (!dynamicLookup) {
            if (builtinFunction == fixedBuiltinFunction && (exactEqualsProfile.profile(fixedGenericName == genericName) || fixedGenericName.equals(genericName))) {
                return executeInternal(frame, argAndNames, fixedGenericName, fixedGroup, fixedBuiltinFunction);
            }
            CompilerDirectives.transferToInterpreterAndInvalidate();
            dynamicLookup = true;
        }
        return executeInternal(frame, argAndNames, genericName, group, builtinFunction);
    }

    private Object executeInternal(VirtualFrame frame, RArgsValuesAndNames argAndNames, String genericName, RGroupGenerics group, RFunction builtinFunction) {
        Object[] evaluatedArgs = argAndNames.getArguments();

        RStringVector typeL = evaluatedArgs.length == 0 ? null : getArgClass(evaluatedArgs[0]);

        Result resultL = null;
        if (typeL != null) {
            try {
                if (functionLookupL == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    functionLookupL = insert(S3FunctionLookupNode.create(false, false));
                }
                resultL = functionLookupL.execute(frame, genericName, typeL, group.getName(), frame.materialize(), null);
            } catch (NoGenericMethodException e) {
                // fall-through
            }
        }
        Result resultR = null;
        if (group == RGroupGenerics.Ops && argAndNames.getSignature().getLength() >= 2) {
            RStringVector typeR = getArgClass(evaluatedArgs[1]);
            if (typeR != null) {
                try {
                    if (functionLookupR == null) {
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                        functionLookupR = insert(S3FunctionLookupNode.create(false, false));
                    }
                    resultR = functionLookupR.execute(frame, genericName, typeR, group.getName(), frame.materialize(), null);
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
                    RError.warning(getSourceSection(), RError.Message.INCOMPATIBLE_METHODS, resultL.targetFunctionName, resultR.targetFunctionName, genericName);
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
        if (result == null) {
            s3Args = null;
            function = builtinFunction;
        } else {
            s3Args = new S3Args(genericName, result.clazz, dotMethod, frame.materialize(), null, result.groupMatch ? group.getName() : null);
            function = result.function;
        }
        return callMatcher.execute(frame, signature, evaluatedArgs, function, s3Args);
    }
}
