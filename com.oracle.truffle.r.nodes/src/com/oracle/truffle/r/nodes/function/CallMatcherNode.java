/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */

package com.oracle.truffle.r.nodes.function;

import java.util.function.Supplier;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.RRootNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.function.ArgumentMatcher.MatchPermutation;
import com.oracle.truffle.r.nodes.function.signature.RArgumentsNode;
import com.oracle.truffle.r.nodes.unary.CastNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RArguments.DispatchArgs;
import com.oracle.truffle.r.runtime.RCaller;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RBuiltinDescriptor;
import com.oracle.truffle.r.runtime.data.REmpty;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

public abstract class CallMatcherNode extends RBaseNode {

    protected final boolean forNextMethod;
    protected final boolean argsAreEvaluated;

    @Child private PromiseHelperNode promiseHelper;
    @Child private RArgumentsNode argsNode = RArgumentsNode.create();

    protected final ConditionProfile missingArgProfile = ConditionProfile.createBinaryProfile();
    protected final ConditionProfile emptyArgProfile = ConditionProfile.createBinaryProfile();

    private CallMatcherNode(boolean forNextMethod, boolean argsAreEvaluated) {
        this.forNextMethod = forNextMethod;
        this.argsAreEvaluated = argsAreEvaluated;
    }

    private static final int MAX_CACHE_DEPTH = 3;

    public static CallMatcherNode create(boolean forNextMethod, boolean argsAreEvaluated) {
        return new CallMatcherUninitializedNode(forNextMethod, argsAreEvaluated);
    }

    public abstract Object execute(VirtualFrame frame, ArgumentsSignature suppliedSignature, Object[] suppliedArguments, RFunction function, String functionName, DispatchArgs dispatchArgs);

    protected CallMatcherCachedNode specialize(ArgumentsSignature suppliedSignature, Object[] suppliedArguments, RFunction function, CallMatcherNode next) {

        // Note: suppliedSignature and suppliedArguments are in the form as they would be
        // used for the dispatch method (e.g. the method that invokes UseMethod, supplied arguments
        // contains supplied names for the arguments, but in the order according to the formal
        // signature, see ArgumentMatcher and MatchPermutation for details).
        //
        // So if the dispatch method had three formal arguments, these two arrays will have three
        // elements each, but some of these elements may be varargs effectively carrying another
        // array of arguments and their signature in them. What we do here is that we unfold varargs
        // which gives us all the arguments and their signature in one flat array, then we match
        // this to the formal signature of the next method that we want to invoke (e.g. what
        // UseMethod invokes), and we save the information about how we permuted the arguments to
        // the newly created CallMatcherCachedNode.

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
            } else if (suppliedSignature.isUnmatched(i)) {
                argListSize--;
            }
        }

        // see flattenIndexes for the interpretation of the values
        long[] preparePermutation;
        ArgumentsSignature resultSignature;
        if (varArgSignatures != null) {
            resultSignature = ArgumentsSignature.flattenNames(suppliedSignature, varArgSignatures, argListSize);
            preparePermutation = ArgumentsSignature.flattenIndexes(varArgSignatures, suppliedSignature, argListSize);
        } else {
            preparePermutation = new long[argListSize];
            String[] newSuppliedSignature = new String[argListSize];
            int index = 0;
            for (int i = 0; i < argCount; i++) {
                if (!suppliedSignature.isUnmatched(i)) {
                    preparePermutation[index] = i;
                    newSuppliedSignature[index] = suppliedSignature.getName(i);
                    index++;
                }
            }
            resultSignature = ArgumentsSignature.get(newSuppliedSignature);
        }

        assert resultSignature != null;
        ArgumentsSignature formalSignature = ArgumentMatcher.getFunctionSignature(function);
        MatchPermutation permutation = ArgumentMatcher.matchArguments(resultSignature, formalSignature, this, forNextMethod, function.getRBuiltin());

        return new CallMatcherCachedNode(suppliedSignature, varArgSignatures, function, preparePermutation, permutation, forNextMethod, argsAreEvaluated, next);
    }

    protected Object[] prepareArguments(Object[] reorderedArgs, ArgumentsSignature reorderedSignature, RFunction function, DispatchArgs dispatchArgs, RCaller caller) {
        return argsNode.execute(function, caller, null, reorderedArgs, reorderedSignature, dispatchArgs);
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
        CallMatcherUninitializedNode(boolean forNextMethod, boolean argsAreEvaluated) {
            super(forNextMethod, argsAreEvaluated);
        }

        private int depth;

        @Override
        public Object execute(VirtualFrame frame, ArgumentsSignature suppliedSignature, Object[] suppliedArguments, RFunction function, String functionName, DispatchArgs dispatchArgs) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            if (++depth > MAX_CACHE_DEPTH) {
                return replace(new CallMatcherGenericNode(forNextMethod, argsAreEvaluated)).execute(frame, suppliedSignature, suppliedArguments, function, functionName, dispatchArgs);
            } else {
                CallMatcherCachedNode cachedNode = replace(specialize(suppliedSignature, suppliedArguments, function, this));
                // for splitting if necessary
                if (cachedNode.call != null && RCallNode.needsSplitting(function.getTarget())) {
                    cachedNode.call.cloneCallTarget();
                }
                return cachedNode.execute(frame, suppliedSignature, suppliedArguments, function, functionName, dispatchArgs);
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

        private final RBuiltinDescriptor builtinDescriptor;
        private final ArgumentsSignature cachedSuppliedSignature;
        private final ArgumentsSignature[] cachedVarArgSignatures;
        private final RFunction cachedFunction;
        /**
         * {@link ArgumentsSignature#flattenNames(ArgumentsSignature, ArgumentsSignature[], int)}
         * for the interpretation of the values.
         */
        @CompilationFinal private final long[] preparePermutation;
        private final MatchPermutation permutation;
        private final FormalArguments formals;

        CallMatcherCachedNode(ArgumentsSignature suppliedSignature, ArgumentsSignature[] varArgSignatures, RFunction function, long[] preparePermutation, MatchPermutation permutation,
                        boolean forNextMethod, boolean argsAreEvaluated, CallMatcherNode next) {
            super(forNextMethod, argsAreEvaluated);
            this.cachedSuppliedSignature = suppliedSignature;
            this.cachedVarArgSignatures = varArgSignatures;
            this.cachedFunction = function;
            this.preparePermutation = preparePermutation;
            this.permutation = permutation;
            this.next = next;
            this.formals = ((RRootNode) cachedFunction.getRootNode()).getFormalArguments();
            if (function.isBuiltin()) {
                this.builtinDescriptor = function.getRBuiltin();
                this.builtin = RBuiltinNode.inline(builtinDescriptor, null);
                this.builtinArgumentCasts = builtin.getCasts();
            } else {
                this.call = Truffle.getRuntime().createDirectCallNode(function.getTarget());
                this.builtinArgumentCasts = null;
                this.builtinDescriptor = null;
            }
        }

        @Override
        public Object execute(VirtualFrame frame, ArgumentsSignature suppliedSignature, Object[] suppliedArguments, RFunction function, String functionName, DispatchArgs dispatchArgs) {
            if (suppliedSignature == cachedSuppliedSignature && function == cachedFunction && checkLastArgSignature(cachedSuppliedSignature, suppliedArguments)) {

                // Note: see CallMatcherNode#specialize for details on suppliedSignature/Arguments

                // this unrolls all varargs instances in suppliedArgs into a flat array of arguments
                RArgsValuesAndNames preparedArguments = prepareSuppliedArgument(preparePermutation, suppliedArguments, suppliedSignature);

                // This is then matched to formal signature: the result is non-flat array of
                // arguments possibly containing varargs -- something that argument matching for a
                // direct function call would create would this be a direct function call
                RArgsValuesAndNames matchedArgs = ArgumentMatcher.matchArgumentsEvaluated(permutation, preparedArguments.getArguments(), null, formals);
                Object[] reorderedArgs = matchedArgs.getArguments();
                evaluatePromises(frame, cachedFunction, reorderedArgs, formals.getSignature().getVarArgIndex());
                if (call != null) {
                    RCaller parent = RArguments.getCall(frame).getParent();
                    String genFunctionName = functionName == null ? function.getName() : functionName;
                    Supplier<RSyntaxNode> argsSupplier = RCallerHelper.createFromArguments(genFunctionName, preparedArguments);
                    RCaller caller = genFunctionName == null ? RCaller.createInvalid(frame, parent) : RCaller.create(frame, parent, argsSupplier);
                    Object[] arguments = prepareArguments(reorderedArgs, matchedArgs.getSignature(), cachedFunction, dispatchArgs, caller);
                    return call.call(frame, arguments);
                } else {
                    applyCasts(reorderedArgs);
                    Object result = builtin.execute(frame, reorderedArgs);
                    RContext.getInstance().setVisible(builtinDescriptor.getVisibility());
                    return result;
                }
            } else {
                return next.execute(frame, suppliedSignature, suppliedArguments, function, functionName, dispatchArgs);
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
        private static RArgsValuesAndNames prepareSuppliedArgument(long[] preparePermutation, Object[] arguments, ArgumentsSignature suppliedSignature) {
            Object[] values = new Object[preparePermutation.length];
            String[] names = new String[preparePermutation.length];
            for (int i = 0; i < values.length; i++) {
                long source = preparePermutation[i];
                if (!ArgumentsSignature.isVarArgsIndex(source)) {
                    values[i] = arguments[(int) source];
                    names[i] = suppliedSignature.getName((int) source);
                } else {
                    int varArgsIdx = ArgumentsSignature.extractVarArgsIndex(source);
                    int argsIdx = ArgumentsSignature.extractVarArgsArgumentIndex(source);
                    RArgsValuesAndNames varargs = (RArgsValuesAndNames) arguments[varArgsIdx];
                    values[i] = varargs.getArguments()[argsIdx];
                    names[i] = varargs.getSignature().getName(argsIdx);
                }
            }
            return new RArgsValuesAndNames(values, ArgumentsSignature.get(names));
        }
    }

    private static final class CallMatcherGenericNode extends CallMatcherNode {

        CallMatcherGenericNode(boolean forNextMethod, boolean argsAreEvaluated) {
            super(forNextMethod, argsAreEvaluated);
        }

        @Child private IndirectCallNode call = Truffle.getRuntime().createIndirectCallNode();

        private final ConditionProfile hasVarArgsProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile hasUnmatchedProfile = ConditionProfile.createBinaryProfile();

        @Override
        public Object execute(VirtualFrame frame, ArgumentsSignature suppliedSignature, Object[] suppliedArguments, RFunction function, String functionName, DispatchArgs dispatchArgs) {
            RArgsValuesAndNames reorderedArgs = reorderArguments(suppliedArguments, function, suppliedSignature);
            evaluatePromises(frame, function, reorderedArgs.getArguments(), reorderedArgs.getSignature().getVarArgIndex());

            RCaller parent = RArguments.getCall(frame).getParent();
            String genFunctionName = functionName == null ? function.getName() : functionName;
            RCaller caller = genFunctionName == null ? RCaller.createInvalid(frame, parent)
                            : RCaller.create(frame, RCallerHelper.createFromArguments(genFunctionName,
                                            new RArgsValuesAndNames(reorderedArgs.getArguments(), ArgumentsSignature.empty(reorderedArgs.getLength()))));
            Object[] arguments = prepareArguments(reorderedArgs.getArguments(), reorderedArgs.getSignature(), function, dispatchArgs, caller);
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
        protected RArgsValuesAndNames reorderArguments(Object[] args, RFunction function, ArgumentsSignature paramSignature) {
            assert paramSignature.getLength() == args.length;

            int argCount = args.length;
            int argListSize = argCount;

            boolean hasVarArgs = false;
            boolean hasUnmatched = false;
            for (int fi = 0; fi < argCount; fi++) {
                Object arg = args[fi];
                if (hasVarArgsProfile.profile(arg instanceof RArgsValuesAndNames)) {
                    hasVarArgs = true;
                    argListSize += ((RArgsValuesAndNames) arg).getLength() - 1;
                } else if (hasUnmatchedProfile.profile(paramSignature.isUnmatched(fi))) {
                    hasUnmatched = true;
                    argListSize--;
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
                    } else if (!paramSignature.isUnmatched(fi)) {
                        argNames[index] = paramSignature.getName(fi);
                        argValues[index++] = checkMissing(arg);
                    }
                }
                signature = ArgumentsSignature.get(argNames);
            } else {
                argValues = new Object[argListSize];
                String[] newSignature = hasUnmatched ? new String[argListSize] : null;
                int index = 0;
                for (int i = 0; i < argCount; i++) {
                    if (!hasUnmatched || !paramSignature.isUnmatched(i)) {
                        argValues[index] = checkMissing(args[i]);
                        if (hasUnmatched) {
                            newSignature[index] = paramSignature.getName(i);
                        }
                        index++;
                    }
                }
                signature = hasUnmatched ? ArgumentsSignature.get(newSignature) : paramSignature;
            }

            // ...and use them as 'supplied' arguments...
            RArgsValuesAndNames evaledArgs = new RArgsValuesAndNames(argValues, signature);

            // ...to match them against the chosen function's formal arguments
            RArgsValuesAndNames evaluated = ArgumentMatcher.matchArgumentsEvaluated((RRootNode) function.getRootNode(), evaledArgs, null, forNextMethod, this);
            return evaluated;
        }

        protected static Object checkMissing(Object value) {
            return RMissingHelper.isMissing(value) || (value instanceof RPromise && RMissingHelper.isMissingName((RPromise) value)) ? null : value;
        }
    }
}
