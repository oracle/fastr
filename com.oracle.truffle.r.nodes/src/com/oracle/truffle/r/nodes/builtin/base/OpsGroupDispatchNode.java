package com.oracle.truffle.r.nodes.builtin.base;

import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.runtime.data.*;

public class OpsGroupDispatchNode extends GroupDispatchNode {

    private String targetFunctionNameR;
    private RFunction targetFunctionR;
    private RStringVector klassR;
    private RStringVector typeL;
    private RStringVector typeR;
    private boolean writeGroupR;

    public OpsGroupDispatchNode(final String genericName, final String grpName, RNode[] args) {
        super(genericName, grpName, args);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        this.typeL = getArgClass(frame, 0);
        this.typeR = getArgClass(frame, 1);
        if (this.typeL == null && this.typeR == null) {
            return callBuiltin();
        }
        this.type = this.typeR;
        findTargetFunction(frame);
        targetFunctionNameR = targetFunctionName;
        targetFunctionR = targetFunction;
        klassR = klass;
        writeGroupR = writeGroup;
        this.type = this.typeL;
        findTargetFunction(frame);
        if (targetFunctionR == null && targetFunction == null) {
            return callBuiltin();
        }
        if (targetFunctionR != null && targetFunction != null) {
            if (targetFunctionName.equals("Ops.difftime") && targetFunctionNameR.equals("+.POSIXt") && targetFunctionNameR.equals("+.Date")) {
                targetFunction = null;
            } else if (!(targetFunctionNameR.equals("Ops.difftime") && targetFunctionName.equals("+.POSIXt") && targetFunctionName.equals("-.POSIXt") && targetFunctionNameR.equals("+.Date") && targetFunctionNameR.equals("-.Date"))) {
                /*
                 * TODO: throw warning
                 * "Incompatible methods (\"%s\", \"%s\") for \"%s\""),lname,rname, generic
                 */
                return callBuiltin();
            }
        }
        if (targetFunction == null) {
            targetFunction = targetFunctionR;
            targetFunctionName = targetFunctionNameR;
            klass = klassR;
            writeGroup = writeGroupR;
            this.type = this.typeR;
        }
        String methods[] = new String[this.argNodes.length];
        for (int i = 0; i < methods.length; ++i) {
            RStringVector classHr = this.getArgClass(frame, i);
            for (int j = 0; j < classHr.getLength(); ++j) {
                if (classHr.equals(klass.getDataAt(0))) {
                    methods[i] = targetFunctionName;
                    break;
                }
            }
        }
        dotMethod = RDataFactory.createStringVector(methods, true);
        return executeHelper();
    }
}
