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

import static com.oracle.truffle.r.runtime.env.frame.FrameSlotChangeMonitor.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.access.variables.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

public abstract class S3DispatchNode extends DispatchNode {

    protected final BranchProfile errorProfile = BranchProfile.create();

    @Child private ReadVariableNode lookup;
    @CompilationFinal private String lastFun;
    @Child private WriteVariableNode wvnCallEnv;
    @Child private WriteVariableNode wvnGeneric;
    @Child private WriteVariableNode wvnClass;
    @Child protected WriteVariableNode wvnMethod;
    @Child private WriteVariableNode wvnDefEnv;
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

    protected WriteVariableNode initWvn(WriteVariableNode wvn, String name) {
        WriteVariableNode node = wvn;
        if (node == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            node = WriteVariableNode.create(name, null, false, false);
            insert(node);
        }
        return node;
    }

    protected WriteVariableNode defineVarInFrame(VirtualFrame frame, WriteVariableNode wvn, String varName, Object value) {
        addVar(frame, varName);
        WriteVariableNode wvnCopy = initWvn(wvn, varName);
        wvnCopy.execute(frame, value);
        return wvnCopy;
    }

    private static void addVar(VirtualFrame frame, final String varName) {
        addVarHelper(frame.getFrameDescriptor(), varName);
    }

    @TruffleBoundary
    private static void addVarHelper(FrameDescriptor frameDescriptor, final String varName) {
        findOrAddFrameSlot(frameDescriptor, varName);
    }

    protected void defineVarsInFrame(VirtualFrame frame) {
        addVars(frame);
        wvnGeneric = defineVarInFrame(frame, wvnGeneric, RRuntime.RDotGeneric, genericName);
        wvnClass = defineVarInFrame(frame, wvnClass, RRuntime.RDotClass, klass);
        wvnCallEnv = defineVarInFrame(frame, wvnCallEnv, RRuntime.RDotGenericCallEnv, genCallEnv);
        wvnDefEnv = defineVarInFrame(frame, wvnDefEnv, RRuntime.RDotGenericDefEnv, genDefEnv);
    }

    protected void defineVarsAsArguments(VirtualFrame frame) {
        RArguments.setS3Generic(frame, genericName);
        RArguments.setS3Class(frame, klass);
        RArguments.setS3CallEnv(frame, genCallEnv);
        RArguments.setS3DefEnv(frame, genDefEnv);
    }

    protected void addVars(VirtualFrame frame) {
        addVars0(frame.getFrameDescriptor());
    }

    @TruffleBoundary
    private static void addVars0(FrameDescriptor fDesc) {
        findOrAddFrameSlot(fDesc, RRuntime.RDotGeneric);
        findOrAddFrameSlot(fDesc, RRuntime.RDotMethod);
        findOrAddFrameSlot(fDesc, RRuntime.RDotClass);
        findOrAddFrameSlot(fDesc, RRuntime.RDotGenericCallEnv);
        findOrAddFrameSlot(fDesc, RRuntime.RDotGenericDefEnv);
    }

    protected void removeVars(Frame frame) {
        removeVar(frame.getFrameDescriptor(), RRuntime.RDotGeneric);
        removeVar(frame.getFrameDescriptor(), RRuntime.RDotMethod);
        removeVar(frame.getFrameDescriptor(), RRuntime.RDotClass);
        removeVar(frame.getFrameDescriptor(), RRuntime.RDotGenericCallEnv);
        removeVar(frame.getFrameDescriptor(), RRuntime.RDotGenericDefEnv);

    }

    @TruffleBoundary
    private static void removeVars0(FrameDescriptor fDesc) {
        fDesc.removeFrameSlot(RRuntime.RDotGeneric);
        fDesc.removeFrameSlot(RRuntime.RDotMethod);
        fDesc.removeFrameSlot(RRuntime.RDotClass);
        fDesc.removeFrameSlot(RRuntime.RDotGenericCallEnv);
        fDesc.removeFrameSlot(RRuntime.RDotGenericDefEnv);
    }

    @TruffleBoundary
    protected static void removeVar(FrameDescriptor fDesc, final String varName) {
        if (fDesc.findFrameSlot(varName) != null) {
            fDesc.removeFrameSlot(varName);
        }
    }

    private void checkLength(final String className, final String generic) {
        // The magic number two taken from src/main/objects.c
        if (className.length() + generic.length() + 2 > RRuntime.LEN_METHOD_NAME) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.TOO_LONG_CLASS_NAME, generic);
        }
    }
}
