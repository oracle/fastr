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
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.function.ArgumentMatcher.MatchPermutation;
import com.oracle.truffle.r.nodes.function.signature.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.RArguments.S3Args;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.nodes.*;

public abstract class CallMatcherNode extends RBaseNode {

    private final RCaller caller = RDataFactory.createCaller(this);
    protected final boolean forNextMethod;
    protected final boolean argsAreEvaluated;

    @Child private PromiseHelperNode promiseHelper;
    @Child private RArgumentsNode argsNode = RArgumentsNode.create();

    protected final ConditionProfile missingArgProfile = ConditionProfile.createBinaryProfile();
    protected final ConditionProfile emptyArgProfile = ConditionProfile.createBinaryProfile();

    public CallMatcherNode(boolean forNextMethod, boolean argsAreEvaluated) {
        this.forNextMethod = forNextMethod;
        this.argsAreEvaluated = argsAreEvaluated;
    }

    protected static final int MAX_CACHE_DEPTH = 3;

    public static CallMatcherNode create(boolean forNextMethod, boolean argsAreEvaluated) {
        return new CallMatcherUninitializedNode(forNextMethod, argsAreEvaluated);
    }

    public abstract Object execute(VirtualFrame frame, ArgumentsSignature suppliedSignature, Object[] suppliedArguments, RFunction function, S3Args s3Args);

    private static CallMatcherCachedNode specialize(ArgumentsSignature suppliedSignature, Object[] suppliedArguments, RFunction function, CallMatcherNode specializer, boolean forNextMethod,
                    boolean argsAreEvaluated, CallMatcherNode next) {

        int argCount = suppliedArguments.length;
        int argListSize = argCount;

        // extract vararg signatures from the arguments
        ArgumentsSignature[] varArgSignatures = null;
        for (int i = 0; i < suppliedArguments.length; i++) {
            Object arg = suppliedArguments[i];
            if (arg instanceof RArgsValuesAndNames) {
                if (varArgSignatures == null) {
                    varArgSignatures = new ArgumentsSignature[suppliedArguments.length];
                }
                varArgSignatures[i] = ((RArgsValuesAndNames) arg).getSignature();
                argListSize += ((RArgsValuesAndNames) arg).getLength() - 1;
            }
        }

        long[] preparePermutation;
        ArgumentsSignature resultSignature;
        if (varArgSignatures != null) {
            resultSignature = ArgumentsSignature.flattenNames(suppliedSignature, varArgSignatures, argListSize);
            preparePermutation = ArgumentsSignature.flattenIndexes(varArgSignatures, argListSize);
        } else {
            preparePermutation = new long[argCount];
            for (int i = 0; i < argCount; i++) {
                preparePermutation[i] = i;
            }
            resultSignature = suppliedSignature;
        }

        assert resultSignature != null;
        ArgumentsSignature formalSignature = ArgumentMatcher.getFunctionSignature(function);
        MatchPermutation permutation = ArgumentMatcher.matchArguments(resultSignature, formalSignature, specializer, forNextMethod, function.getRBuiltin());

        return new CallMatcherCachedNode(suppliedSignature, varArgSignatures, function, preparePermutation, permutation, specializer, forNextMethod, argsAreEvaluated, next);
    }

    protected Object[] prepareArguments(VirtualFrame frame, Object[] reorderedArgs, ArgumentsSignature reorderedSignature, RFunction function, S3Args s3Args) {
        return argsNode.execute(function, caller, null, RArguments.getDepth(frame) + 1, reorderedArgs, reorderedSignature, s3Args);
    }

    protected final void evaluatePromises(VirtualFrame frame, RFunction function, Object[] args, int varArgIndex) {
        if (function.isBuiltin()) {
            if (!argsAreEvaluated) {
                for (int i = 0; i < args.length; i++) {
                    Object arg = args[i];
                    if (arg instanceof RPromise) {
                        if (promiseHelper == null) {
                            CompilerDirectives.transferToInterpreterAndInvalidate();
                            promiseHelper = insert(new PromiseHelperNode());
                        }
                        args[i] = promiseHelper.evaluate(frame, (RPromise) arg);
                    } else if (varArgIndex == i && arg instanceof RArgsValuesAndNames) {
                        evaluatePromises(frame, (RArgsValuesAndNames) arg);
                    }
                }
            }
            replaceMissingArguments(function, args);
        }
    }

    private void evaluatePromises(VirtualFrame frame, RArgsValuesAndNames argsValuesAndNames) {
        Object[] args = argsValuesAndNames.getArguments();
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if (arg instanceof RPromise) {
                if (promiseHelper == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    promiseHelper = insert(new PromiseHelperNode());
                }
                args[i] = promiseHelper.evaluate(frame, (RPromise) arg);
            }
        }
    }

    protected abstract void replaceMissingArguments(RFunction function, Object[] args);

    @NodeInfo(cost = NodeCost.UNINITIALIZED)
    private static final class CallMatcherUninitializedNode extends CallMatcherNode {
        public CallMatcherUninitializedNode(boolean forNextMethod, boolean argsAreEvaluated) {
            super(forNextMethod, argsAreEvaluated);
        }

        private int depth;

        @Override
        public Object execute(VirtualFrame frame, ArgumentsSignature suppliedSignature, Object[] suppliedArguments, RFunction function, S3Args s3Args) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            if (++depth > MAX_CACHE_DEPTH) {
                return replace(new CallMatcherGenericNode(forNextMethod, argsAreEvaluated)).execute(frame, suppliedSignature, suppliedArguments, function, s3Args);
            } else {
                CallMatcherCachedNode cachedNode = replace(specialize(suppliedSignature, suppliedArguments, function, this, forNextMethod, argsAreEvaluated, this));
                // for splitting if necessary
                if (cachedNode.call != null && RCallNode.needsSplitting(function)) {
                    cachedNode.call.cloneCallTarget();
                }
                return cachedNode.execute(frame, suppliedSignature, suppliedArguments, function, s3Args);
            }
        }

        @Override
        protected void replaceMissingArguments(RFunction function, Object[] args) {
            throw RInternalError.shouldNotReachHere();
        }
    }

    private static final class CallMatcherCachedNode extends CallMatcherNode {

        @Child private CallMatcherNode next;

        @Child private DirectCallNode call;

        @Child private RBuiltinNode builtin;
        @Children private final CastNode[] builtinArgumentCasts;

        private final ArgumentsSignature cachedSuppliedSignature;
        private final ArgumentsSignature[] cachedVarArgSignatures;
        private final RFunction cachedFunction;
        @CompilationFinal private final long[] preparePermutation;
        private final MatchPermutation permutation;
        private final FormalArguments formals;

        public CallMatcherCachedNode(ArgumentsSignature suppliedSignature, ArgumentsSignature[] varArgSignatures, RFunction function, long[] preparePermutation, MatchPermutation permutation,
                        CallMatcherNode specializer, boolean forNextMethod, boolean argsAreEvaluated, CallMatcherNode next) {
            super(forNextMethod, argsAreEvaluated);
            this.cachedSuppliedSignature = suppliedSignature;
            this.cachedVarArgSignatures = varArgSignatures;
            this.cachedFunction = function;
            this.preparePermutation = preparePermutation;
            this.permutation = permutation;
            this.next = next;
            this.formals = ((RRootNode) cachedFunction.getRootNode()).getFormalArguments();
            if (function.isBuiltin()) {
                RBuiltinRootNode builtinRoot = RCallNode.findBuiltinRootNode(function.getTarget());
                this.builtin = builtinRoot.inline(formals.getSignature(), null, specializer.getRSyntaxNode().getSourceSection());
                this.builtinArgumentCasts = builtin.getCasts();
            } else {
                this.call = Truffle.getRuntime().createDirectCallNode(function.getTarget());
                this.builtinArgumentCasts = null;
            }
        }

        @Override
        public Object execute(VirtualFrame frame, ArgumentsSignature suppliedSignature, Object[] suppliedArguments, RFunction function, S3Args s3Args) {
            if (suppliedSignature == cachedSuppliedSignature && function == cachedFunction && checkLastArgSignature(cachedSuppliedSignature, suppliedArguments)) {

                Object[] preparedArguments = prepareSuppliedArgument(preparePermutation, suppliedArguments);

                Object[] reorderedArgs = ArgumentMatcher.matchArgumentsEvaluated(permutation, preparedArguments, formals);
                evaluatePromises(frame, cachedFunction, reorderedArgs, formals.getSignature().getVarArgIndex());
                if (call != null) {
                    Object[] arguments = prepareArguments(frame, reorderedArgs, formals.getSignature(), cachedFunction, s3Args);
                    return call.call(frame, arguments);
                } else {
                    applyCasts(reorderedArgs);
                    return builtin.execute(frame, reorderedArgs);
                }
            } else {
                return next.execute(frame, suppliedSignature, suppliedArguments, function, s3Args);
            }
        }

        @ExplodeLoop
        private void applyCasts(Object[] reorderedArgs) {
            for (int i = 0; i < builtinArgumentCasts.length; i++) {
                CastNode cast = builtinArgumentCasts[i];
                if (cast != null) {
                    reorderedArgs[i] = cast.execute(reorderedArgs[i]);
                }
            }
        }

        @Override
        @ExplodeLoop
        protected void replaceMissingArguments(RFunction function, Object[] args) {
            for (int i = 0; i < formals.getSignature().getLength(); i++) {
                Object arg = args[i];
                if (formals.getInternalDefaultArgumentAt(i) != RMissing.instance && (missingArgProfile.profile(arg == RMissing.instance) || emptyArgProfile.profile(arg == REmpty.instance))) {
                    args[i] = formals.getInternalDefaultArgumentAt(i);
                }
            }
        }

        @ExplodeLoop
        private boolean checkLastArgSignature(ArgumentsSignature cachedSuppliedSignature2, Object[] arguments) {
            for (int i = 0; i < cachedSuppliedSignature2.getLength(); i++) {
                Object arg = arguments[i];
                if (arg instanceof RArgsValuesAndNames) {
                    if (cachedVarArgSignatures == null || cachedVarArgSignatures[i] != ((RArgsValuesAndNames) arg).getSignature()) {
                        return false;
                    }
                } else {
                    if (cachedVarArgSignatures != null && cachedVarArgSignatures[i] != null) {
                        return false;
                    }
                }
            }
            return true;
        }

        @ExplodeLoop
        private static Object[] prepareSuppliedArgument(long[] preparePermutation, Object[] arguments) {
            Object[] result = new Object[preparePermutation.length];
            for (int i = 0; i < result.length; i++) {
                long source = preparePermutation[i];
                if (source >= 0) {
                    result[i] = arguments[(int) source];
                } else {
                    source = -source - 1;
                    result[i] = ((RArgsValuesAndNames) arguments[(int) (source >> 32)]).getArguments()[(int) source];
                }
            }
            return result;
        }
    }

    private static final class CallMatcherGenericNode extends CallMatcherNode {

        public CallMatcherGenericNode(boolean forNextMethod, boolean argsAreEvaluated) {
            super(forNextMethod, argsAreEvaluated);
        }

        @Child private PromiseHelperNode promiseHelper;
        @Child private IndirectCallNode call = Truffle.getRuntime().createIndirectCallNode();

        private final ConditionProfile hasVarArgsProfile = ConditionProfile.createBinaryProfile();

        @Override
        public Object execute(VirtualFrame frame, ArgumentsSignature suppliedSignature, Object[] suppliedArguments, RFunction function, S3Args s3Args) {
            EvaluatedArguments reorderedArgs = reorderArguments(suppliedArguments, function, suppliedSignature);
            evaluatePromises(frame, function, reorderedArgs.getArguments(), reorderedArgs.getSignature().getVarArgIndex());
            Object[] arguments = prepareArguments(frame, reorderedArgs.getArguments(), reorderedArgs.getSignature(), function, s3Args);
            return call.call(frame, function.getTarget(), arguments);
        }

        @Override
        protected void replaceMissingArguments(RFunction function, Object[] args) {
            FormalArguments formals = ((RRootNode) function.getRootNode()).getFormalArguments();
            for (int i = 0; i < formals.getSignature().getLength(); i++) {
                Object arg = args[i];
                if (formals.getInternalDefaultArgumentAt(i) != RMissing.instance && (missingArgProfile.profile(arg == RMissing.instance) || emptyArgProfile.profile(arg == REmpty.instance))) {
                    args[i] = formals.getInternalDefaultArgumentAt(i);
                }
            }
        }

        @TruffleBoundary
        protected EvaluatedArguments reorderArguments(Object[] args, RFunction function, ArgumentsSignature paramSignature) {
            assert paramSignature.getLength() == args.length;

            int argCount = args.length;
            int argListSize = argCount;

            boolean hasVarArgs = false;
            for (int fi = 0; fi < argCount; fi++) {
                Object arg = args[fi];
                if (hasVarArgsProfile.profile(arg instanceof RArgsValuesAndNames)) {
                    hasVarArgs = true;
                    argListSize += ((RArgsValuesAndNames) arg).getLength() - 1;
                }
            }
            Object[] argValues;
            ArgumentsSignature signature;
            if (hasVarArgs) {
                argValues = new Object[argListSize];
                String[] argNames = new String[argListSize];
                int index = 0;
                for (int fi = 0; fi < argCount; fi++) {
                    Object arg = args[fi];
                    if (arg instanceof RArgsValuesAndNames) {
                        RArgsValuesAndNames varArgs = (RArgsValuesAndNames) arg;
                        Object[] varArgValues = varArgs.getArguments();
                        ArgumentsSignature varArgSignature = varArgs.getSignature();
                        for (int i = 0; i < varArgs.getLength(); i++) {
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
            EvaluatedArguments evaluated = ArgumentMatcher.matchArgumentsEvaluated(function, evaledArgs, this, forNextMethod);
            return evaluated;
        }

        protected static Object checkMissing(Object value) {
            return RMissingHelper.isMissing(value) || (value instanceof RPromise && RMissingHelper.isMissingName((RPromise) value)) ? null : value;
        }
    }
}
