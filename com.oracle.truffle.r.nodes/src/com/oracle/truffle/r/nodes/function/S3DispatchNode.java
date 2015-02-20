/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates
 *
 * All rights reserved.
 */

package com.oracle.truffle.r.nodes.function;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.access.variables.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

public abstract class S3DispatchNode extends DispatchNode {

    protected final BranchProfile errorProfile = BranchProfile.create();

    @Child private ReadVariableNode lookup;
    @CompilationFinal private String lastFun;
    @Child protected PromiseHelperNode promiseHelper = new PromiseHelperNode();
    @Child protected IndirectCallNode indirectCallNode = Truffle.getRuntime().createIndirectCallNode();
    protected String targetFunctionName;
    protected RFunction targetFunction;
    protected RStringVector klass;
    protected FunctionCall funCall;
    protected MaterializedFrame genCallEnv;
    protected MaterializedFrame genDefEnv;
    protected boolean isFirst;

    // TODO: the executeHelper methods share quite a bit of code, but is it better or worse from
    // having one method with a rather convoluted control flow structure?

    public S3DispatchNode(String genericName) {
        super(genericName);
    }

    protected EvaluatedArguments reorderArgs(VirtualFrame frame, RFunction func, Object[] evaluatedArgs, String[] argNames, boolean hasVarArgs, SourceSection callSrc) {
        String[] evaluatedArgNames = null;
        Object[] evaluatedArgsValues = evaluatedArgs;
        int argCount = evaluatedArgs.length;
        if (hasVarArgs) {
            evaluatedArgNames = argNames;
        } else {
            boolean hasNames = argNames != null;
            evaluatedArgNames = hasNames ? new String[argNames.length] : null;
            int fi = 0;
            int index = 0;
            int argListSize = evaluatedArgsValues.length;
            for (; fi < argCount; ++fi) {
                Object arg = evaluatedArgs[fi];
                if (arg instanceof RArgsValuesAndNames) {
                    RArgsValuesAndNames varArgsContainer = (RArgsValuesAndNames) arg;
                    argListSize += varArgsContainer.length() - 1;
                    evaluatedArgsValues = Utils.resizeArray(evaluatedArgsValues, argListSize);
                    // argNames can be null if no names for arguments have been specified
                    evaluatedArgNames = evaluatedArgNames == null ? new String[argListSize] : Utils.resizeArray(evaluatedArgNames, argListSize);
                    Object[] varArgsValues = varArgsContainer.getValues();
                    String[] varArgsNames = varArgsContainer.getNames();
                    for (int i = 0; i < varArgsContainer.length(); i++) {
                        addArg(evaluatedArgsValues, varArgsValues[i], index);
                        String name = varArgsNames == null ? null : varArgsNames[i];
                        evaluatedArgNames[index] = name;
                        index++;
                    }
                } else {
                    addArg(evaluatedArgsValues, arg, index);
                    if (hasNames) {
                        evaluatedArgNames[index] = argNames[fi];
                    }
                    index++;
                }
            }
        }
        EvaluatedArguments evaledArgs = EvaluatedArguments.create(evaluatedArgsValues, evaluatedArgNames);
        // ...to match them against the chosen function's formal arguments
        EvaluatedArguments reorderedArgs = ArgumentMatcher.matchArgumentsEvaluated(frame, func, evaledArgs, callSrc, promiseHelper, false);
        return reorderedArgs;
    }

    private static void addArg(Object[] values, Object value, int index) {
        if (RMissingHelper.isMissing(value) || (value instanceof RPromise && RMissingHelper.isMissingName((RPromise) value))) {
            values[index] = null;
        } else {
            values[index] = value;
        }
    }

    public static boolean isEqualType(RStringVector one, RStringVector two) {
        if (one == null && two == null) {
            return true;
        }
        if (one == null || two == null) {
            return false;
        }

        if (one.getLength() != two.getLength()) {
            return false;
        }
        for (int i = 0; i < one.getLength(); ++i) {
            if (!one.getDataAt(i).equals(two.getDataAt(i))) {
                return false;
            }
        }
        return true;
    }

    protected void findFunction(String functionName, Frame frame) {
        if (lookup == null || !functionName.equals(lastFun)) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            lastFun = functionName;
            ReadVariableNode rvn = ReadVariableNode.createFunctionLookup(functionName, false);
            lookup = lookup == null ? insert(rvn) : lookup.replace(rvn);
        }
        targetFunction = null;
        targetFunctionName = null;
        Object func;
        if (frame instanceof VirtualFrame) {
            func = lookup.execute((VirtualFrame) frame);
        } else {
            func = lookup.execute(null, (MaterializedFrame) frame);
        }
        if (func != null) {
            assert func instanceof RFunction;
            targetFunctionName = functionName;
            targetFunction = (RFunction) func;
        }
    }

    protected void findFunction(String generic, String className, Frame frame) {
        checkLength(className, generic);
        findFunction(functionName(generic, className), frame);
    }

    @TruffleBoundary
    private static String functionName(String generic, String className) {
        return new StringBuilder(generic).append(RRuntime.RDOT).append(className).toString();
    }

    protected void defineVarsAsArguments(Object[] args) {
        RArguments.setS3Generic(args, genericName);
        RArguments.setS3Class(args, klass);
        RArguments.setS3CallEnv(args, genCallEnv);
        RArguments.setS3DefEnv(args, genDefEnv);
    }

    private void checkLength(final String className, final String generic) {
        // The magic number two taken from src/main/objects.c
        if (className.length() + generic.length() + 2 > RRuntime.LEN_METHOD_NAME) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.TOO_LONG_CLASS_NAME, generic);
        }
    }
}
