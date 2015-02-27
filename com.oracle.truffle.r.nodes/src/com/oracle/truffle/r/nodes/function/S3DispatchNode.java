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
import com.oracle.truffle.api.frame.FrameInstance.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.access.variables.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

public abstract class S3DispatchNode extends DispatchNode {

    @Child protected PromiseHelperNode promiseHelper = new PromiseHelperNode();

    private final ConditionProfile topLevelFrameProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile callerFrameSlotPath = ConditionProfile.createBinaryProfile();
    protected final BranchProfile errorProfile = BranchProfile.create();
    private final ConditionProfile hasVarArgsProfile = ConditionProfile.createBinaryProfile();

    @CompilationFinal private String lastFun;
    @Child private ReadVariableNode lookup;
    protected String targetFunctionName;
    protected RFunction targetFunction;

    protected final ArgumentsSignature suppliedSignature;

    // TODO: the executeHelper methods share quite a bit of code, but is it better or worse from
    // having one method with a rather convoluted control flow structure?

    public S3DispatchNode(String genericName, ArgumentsSignature suppliedSignature) {
        super(genericName);
        this.suppliedSignature = suppliedSignature;
    }

    protected MaterializedFrame getCallerFrame(VirtualFrame frame) {
        MaterializedFrame funFrame = RArguments.getCallerFrame(frame);
        if (callerFrameSlotPath.profile(funFrame == null)) {
            funFrame = Utils.getCallerFrame(frame, FrameAccess.MATERIALIZE).materialize();
            RError.performanceWarning("slow caller frame access in UseMethod dispatch");
        }
        // S3 method can be dispatched from top-level where there is no caller frame
        return topLevelFrameProfile.profile(funFrame == null) ? frame.materialize() : funFrame;
    }

    @ExplodeLoop
    protected static Object[] extractArguments(VirtualFrame frame) {
        int argCount = RArguments.getArgumentsLength(frame);
        Object[] argValues = new Object[argCount];
        for (int i = 0; i < argCount; ++i) {
            argValues[i] = RArguments.getArgument(frame, i);
        }
        return argValues;
    }

    protected EvaluatedArguments reorderArguments(Object[] args, RFunction function, ArgumentsSignature paramSignature, SourceSection errorSourceSection) {
        assert paramSignature.getLength() == args.length;

        int argCount = args.length;
        int argListSize = argCount;

        boolean hasVarArgs = false;
        for (int fi = 0; fi < argCount; ++fi) {
            Object arg = args[fi];
            if (hasVarArgsProfile.profile(arg instanceof RArgsValuesAndNames)) {
                hasVarArgs = true;
                argListSize += ((RArgsValuesAndNames) arg).length() - 1;
            }
        }
        Object[] argValues;
        ArgumentsSignature signature;
        if (hasVarArgs) {
            argValues = new Object[argListSize];
            String[] argNames = new String[argListSize];
            int index = 0;
            for (int fi = 0; fi < argCount; ++fi) {
                Object arg = args[fi];
                if (arg instanceof RArgsValuesAndNames) {
                    RArgsValuesAndNames varArgs = (RArgsValuesAndNames) arg;
                    Object[] varArgValues = varArgs.getValues();
                    ArgumentsSignature varArgSignature = varArgs.getSignature();
                    for (int i = 0; i < varArgs.length(); i++) {
                        argNames[index] = varArgSignature.getName(i);
                        argValues[index++] = checkMissing(varArgValues[i]);
                    }
                } else {
                    argNames[index] = paramSignature.getName(fi);
                    argValues[index++] = checkMissing(arg);
                }
            }
            signature = ArgumentsSignature.get(argNames);
        } else {
            argValues = new Object[argCount];
            for (int i = 0; i < argCount; i++) {
                argValues[i] = checkMissing(args[i]);
            }
            signature = paramSignature;
        }

        // ...and use them as 'supplied' arguments...
        EvaluatedArguments evaledArgs = EvaluatedArguments.create(argValues, signature);

        // ...to match them against the chosen function's formal arguments
        EvaluatedArguments evaluated = ArgumentMatcher.matchArgumentsEvaluated(function, evaledArgs, errorSourceSection, false);
        return evaluated;
    }

    private final ValueProfile callerFrameProfile = ValueProfile.createClassProfile();

    protected final Object[] prepareArguments(Frame callerFrame, MaterializedFrame genericDefFrame, EvaluatedArguments reorderedArgs, RFunction function, RStringVector clazz, String functionName) {
        Frame profiledCallerFrame = callerFrameProfile.profile(callerFrame);
        Object[] argObject = RArguments.createS3Args(function, getSourceSection(), null, RArguments.getDepth(profiledCallerFrame) + 1, reorderedArgs.getEvaluatedArgs(), reorderedArgs.getSignature());
        defineVarsAsArguments(argObject, genericName, clazz, profiledCallerFrame.materialize(), genericDefFrame);
        RArguments.setS3Method(argObject, functionName);
        return argObject;
    }

    protected EvaluatedArguments reorderArgs(VirtualFrame frame, RFunction func, Object[] evaluatedArgs, ArgumentsSignature signature, boolean hasVarArgs, SourceSection callSrc) {
        Object[] evaluatedArgsValues = evaluatedArgs;
        ArgumentsSignature evaluatedSignature;
        int argCount = evaluatedArgs.length;
        if (hasVarArgs) {
            evaluatedSignature = signature;
        } else {
            String[] evaluatedArgNames = null;
            evaluatedArgNames = new String[signature.getLength()];
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
                    for (int i = 0; i < varArgsContainer.length(); i++) {
                        evaluatedArgsValues[index] = checkMissing(varArgsValues[i]);
                        String name = varArgsContainer.getSignature().getName(i);
                        evaluatedArgNames[index] = name;
                        index++;
                    }
                } else {
                    evaluatedArgsValues[index] = checkMissing(arg);
                    evaluatedArgNames[index] = signature.getName(fi);
                    index++;
                }
            }
            evaluatedSignature = ArgumentsSignature.get(evaluatedArgNames);
        }
        EvaluatedArguments evaledArgs = EvaluatedArguments.create(evaluatedArgsValues, evaluatedSignature);
        // ...to match them against the chosen function's formal arguments
        EvaluatedArguments reorderedArgs = ArgumentMatcher.matchArgumentsEvaluated(func, evaledArgs, callSrc, false);
        if (func.isBuiltin()) {
            ArgumentMatcher.evaluatePromises(frame, promiseHelper, reorderedArgs);
        }
        return reorderedArgs;
    }

    protected static Object checkMissing(Object value) {
        return RMissingHelper.isMissing(value) || (value instanceof RPromise && RMissingHelper.isMissingName((RPromise) value)) ? null : value;
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
            func = lookup.execute(null, frame);
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

    protected static void defineVarsAsArguments(Object[] args, String genericName, RStringVector klass, MaterializedFrame genCallEnv, MaterializedFrame genDefEnv) {
        RArguments.setS3Generic(args, genericName);
        RArguments.setS3Class(args, klass);
        RArguments.setS3CallEnv(args, genCallEnv);
        RArguments.setS3DefEnv(args, genDefEnv);
    }

    private void checkLength(String className, String generic) {
        // The magic number two taken from src/main/objects.c
        if (className.length() + generic.length() + 2 > RRuntime.LEN_METHOD_NAME) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.TOO_LONG_CLASS_NAME, generic);
        }
    }
}

abstract class S3DispatchLegacyNode extends S3DispatchNode {

    protected RStringVector type;

    @Child protected IndirectCallNode indirectCallNode = Truffle.getRuntime().createIndirectCallNode();
    protected RStringVector klass;
    protected MaterializedFrame genCallEnv;
    protected MaterializedFrame genDefEnv;
    protected boolean isFirst;

    public S3DispatchLegacyNode(String genericName, ArgumentsSignature suppliedSignature) {
        super(genericName, suppliedSignature);
    }
}

abstract class S3DispatchCachedNode extends S3DispatchNode {
    protected final RStringVector type;

    public S3DispatchCachedNode(String genericName, RStringVector type, ArgumentsSignature suppliedSignature) {
        super(genericName, suppliedSignature);
        this.type = type;
    }
}

abstract class S3DispatchGenericNode extends S3DispatchNode {

    @Child protected IndirectCallNode indirectCallNode = Truffle.getRuntime().createIndirectCallNode();
    protected RStringVector klass;
    protected MaterializedFrame genCallEnv;
    protected MaterializedFrame genDefEnv;
    protected boolean isFirst;

    public S3DispatchGenericNode(String genericName, ArgumentsSignature suppliedSignature) {
        super(genericName, suppliedSignature);
    }
}
