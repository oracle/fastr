/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014-2015, Purdue University
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.function;

import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

public class BinaryOpsGroupDispatchNode extends GroupDispatchNode {

    private String targetFunctionNameR;
    protected RFunction targetFunctionR;
    private RStringVector klassR;
    private RStringVector typeL;
    private RStringVector typeR;
    private boolean writeGroupR;
    protected boolean isBuiltinCalled;

    public BinaryOpsGroupDispatchNode(final String aGenericName, boolean hasVararg, SourceSection callSrc, SourceSection argSrc) {
        super(aGenericName, RGroupGenerics.GROUP_OPS, hasVararg, callSrc, argSrc);
    }

    private void initDispatchTypes(final Object[] evaluatedArgs) {
        // This is kind of tricky. We want to evaluate args before we know the function for which
        // arguments should be matched. But as OpsGroupDispatchNode is for BinaryOperators, we can
        // assume that arguments are in correct order!
        this.typeL = getArgClass(evaluatedArgs[0]);
        this.typeR = getArgClass(evaluatedArgs[1]);
    }

    @Override
    public Object execute(VirtualFrame frame, final RArgsValuesAndNames argAndNames) {
        Object[] evaluatedArgs = argAndNames.getValues();
        ArgumentsSignature signature = argAndNames.getSignature();
        if (!isExecuted) {
            isExecuted = true;
            executeNoCache(frame, evaluatedArgs);
        }
        if (isBuiltinCalled || (targetFunctionR == null && targetFunction == null)) {
            return callBuiltin(frame, evaluatedArgs, signature);
        }
        return executeHelper(frame, evaluatedArgs, signature);
    }

    @Override
    public boolean isSameType(Object[] args) {
        return !isExecuted || isEqualType(getArgClass(args[0]), this.typeL) && isEqualType(getArgClass(args[1]), this.typeR);
    }

    protected void executeNoCache(VirtualFrame frame, Object[] evaluatedArgs) {
        initDispatchTypes(evaluatedArgs);
        if (this.typeR != null) {
            this.type = this.typeR;
            findTargetFunction(frame);
            targetFunctionNameR = targetFunctionName;
            targetFunctionR = targetFunction;
            klassR = klass;
            writeGroupR = writeGroup;
        } else {
            targetFunctionR = null;
        }
        if (this.typeL != null) {
            this.type = this.typeL;
            findTargetFunction(frame);
        } else {
            targetFunction = null;
        }
        if (targetFunctionR == null && targetFunction == null) {
            isBuiltinCalled = true;
            return;
        }
        if (targetFunctionR != targetFunction) {
            if (targetFunctionR != null && targetFunction != null) {
                if (targetFunctionName.equals("Ops.difftime") && targetFunctionNameR.equals("+.POSIXt") && targetFunctionNameR.equals("+.Date")) {
                    targetFunction = null;
                } else if (!(targetFunctionNameR.equals("Ops.difftime") && targetFunctionName.equals("+.POSIXt") && targetFunctionName.equals("-.POSIXt") && targetFunctionNameR.equals("+.Date") && targetFunctionNameR.equals("-.Date"))) {
                    /*
                     * TODO: throw warning
                     * "Incompatible methods (\"%s\", \"%s\") for \"%s\""),lname,rname, generic
                     */
                    isBuiltinCalled = true;
                    return;
                }
            }
            if (targetFunction == null) {
                targetFunction = targetFunctionR;
                targetFunctionName = targetFunctionNameR;
                klass = klassR;
                writeGroup = writeGroupR;
                this.type = this.typeR;
            }
        }
        String[] methods = new String[evaluatedArgs.length];
        for (int i = 0; i < methods.length; ++i) {
            methods[i] = "";
            RStringVector classHr = getArgClass(evaluatedArgs[i]);
            if (classHr == null) {
                continue;
            }
            for (int j = 0; j < classHr.getLength(); ++j) {
                if (classHr.getDataAt(j).equals(klass.getDataAt(0))) {
                    methods[i] = targetFunctionName;
                    break;
                }
            }
        }
        dotMethod = RDataFactory.createStringVector(methods, true);
    }
}

class GenericBinarysOpsGroupDispatchNode extends BinaryOpsGroupDispatchNode {

    public GenericBinarysOpsGroupDispatchNode(String aGenericName, boolean hasVararg, SourceSection callSrc, SourceSection argSrc) {
        super(aGenericName, hasVararg, callSrc, argSrc);
    }

    @Override
    public Object execute(VirtualFrame frame, final RArgsValuesAndNames argAndNames) {
        Object[] evaluatedArgs = argAndNames.getValues();
        executeNoCache(frame, evaluatedArgs);
        if (isBuiltinCalled || (targetFunctionR == null && targetFunction == null)) {
            return callBuiltin(frame, evaluatedArgs, argAndNames.getSignature());
        }
        return executeHelper(frame, evaluatedArgs, argAndNames.getSignature());
    }

}
