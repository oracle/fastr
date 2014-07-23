/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.runtime.data.*;

public class OpsGroupDispatchNode extends GroupDispatchNode {

    private String targetFunctionNameR;
    private RFunction targetFunctionR;
    private RStringVector klassR;
    private RStringVector typeL;
    private RStringVector typeR;
    private boolean writeGroupR;

    public OpsGroupDispatchNode(final String genericName, final String grpName, CallArgumentsNode callArgNode) {
        super(genericName, grpName, callArgNode);
    }

    private void initDispatchTypes(VirtualFrame frame) {
        // This is kind of tricky. We want to evaluate args before we know the function for which
        // arguments should be matched. But as OpsGroupDispatchNode is for BinaryOperators, we can
        // assume that arguments are in correct order!
        RNode[] unevaluatedArgs = callArgsNode.getArguments();
        Object[] evaledArgs = new Object[callArgsNode.getArguments().length];
        for (int i = 0; i < evaledArgs.length; i++) {
            evaledArgs[i] = unevaluatedArgs[i].execute(frame);
        }
        // Delay assignment to allow recursion
        evaluatedArgs = evaledArgs;

        if (evaluatedArgs.length > 0) {
            this.typeL = getArgClass(evaluatedArgs[0]);
        }
        if (evaluatedArgs.length > 1) {
            this.typeR = getArgClass(evaluatedArgs[1]);
        }
    }

    @Override
    public Object execute(VirtualFrame frame) {
        initDispatchTypes(frame);
        if (this.typeL == null && this.typeR == null) {
            return callBuiltin(frame);
        }
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
            return callBuiltin(frame);
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
                    return callBuiltin(frame);
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
        String[] methods = new String[this.evaluatedArgs.length];
        for (int i = 0; i < methods.length; ++i) {
            RStringVector classHr = getArgClass(this.evaluatedArgs[i]);
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
        return executeHelper();
    }
}
