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

public final class GroupDispatchNode extends RNode {

    @Child private CallArgumentsNode callArgsNode;
    @Child private S3FunctionLookupNode functionLookupL;
    @Child private S3FunctionLookupNode functionLookupR;
    @Child private CallMatcherNode callMatcher = CallMatcherNode.create(false, true);

    private final String genericName;
    private final RGroupGenerics group;
    private final RFunction builtinFunc;
    private final boolean binaryLookup;

    private final RAttributeProfiles attrProfiles = RAttributeProfiles.create();
    private final ConditionProfile mismatchProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile isObjectProfile = ConditionProfile.createBinaryProfile();
    private final ValueProfile argTypeProfile = ValueProfile.createClassProfile();

    public GroupDispatchNode(String genericName, RGroupGenerics group, CallArgumentsNode callArgNode) {
        this.genericName = genericName.intern();
        this.group = group;
        this.callArgsNode = callArgNode;
        this.binaryLookup = group == RGroupGenerics.Ops && callArgsNode.getSignature().getLength() >= 2;

        this.builtinFunc = RContext.getEngine().lookupBuiltin(genericName);
    }

    public static GroupDispatchNode create(String aGenericName, RGroupGenerics group, CallArgumentsNode callArgNode, SourceSection callSrc) {
        GroupDispatchNode gdcn = new GroupDispatchNode(aGenericName, group, callArgNode);
        gdcn.assignSourceSection(callSrc);
        return gdcn;
    }

    public String getGenericName() {
        return genericName;
    }

    public RGroupGenerics getGroup() {
        return group;
    }

    public SourceSection getCallSrc() {
        return getSourceSection();
    }

    @Override
    public boolean isSyntax() {
        return true;
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
    public RNode substitute(REnvironment env) {
        // TODO substitute aDispatchNode
        return RASTUtils.createCall(this, (CallArgumentsNode) callArgsNode.substitute(env));
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
        Object[] evaluatedArgs = argAndNames.getValues();

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
        if (binaryLookup) {
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
        if (result == null) {
            return callMatcher.execute(frame, signature, evaluatedArgs, builtinFunc, null);
        } else {
            S3Args s3Args = new S3Args(genericName, result.clazz, dotMethod, frame.materialize(), null, result.groupMatch ? group.getName() : null);
            return callMatcher.execute(frame, signature, evaluatedArgs, result.function, s3Args);
        }
    }
}
