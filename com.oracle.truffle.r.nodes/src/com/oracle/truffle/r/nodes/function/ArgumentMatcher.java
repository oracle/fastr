/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.nodes.function;

import java.util.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.access.ConstantNode.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.RPromise.EvalPolicy;
import com.oracle.truffle.r.runtime.data.RPromise.PromiseType;
import com.oracle.truffle.r.runtime.data.RPromise.RPromiseFactory;

/**
 * <p>
 * {@link ArgumentMatcher} serves the purpose of matching {@link CallArgumentsNode} to
 * {@link FormalArguments} of a specific function, see
 * {@link #matchArguments(VirtualFrame, RFunction, CallArgumentsNode, SourceSection)}. The other
 * match functions are used for special cases, where builtins make it necessary to re-match
 * parameters, e.g.:
 * {@link #matchArgumentsEvaluated(VirtualFrame, RFunction, EvaluatedArguments, SourceSection)} for
 * 'UseMethod' and
 * {@link #matchArgumentsInlined(VirtualFrame, RFunction, CallArgumentsNode, SourceSection)} for
 * builtins which are implemented in Java ( @see {@link RBuiltinNode#inline(InlinedArguments)}
 * </p>
 */
public class ArgumentMatcher {

    /**
     * Cached, so it can be used on the FastPath.
     */
    protected SourceSection encapsulatingSrc = null;

    /**
     * Match arguments supplied for a specific function call to the formal arguments and wraps them
     * in {@link PromiseNode}s. Used for calls to all functions parsed from R code
     *
     * @param frame carrier for missing check
     * @param function The function which is to be called
     * @param suppliedArgs The arguments supplied to the call
     * @param encapsulatingSrc The source code encapsulating the arguments, for debugging purposes
     *
     * @return A fresh {@link MatchedArgumentsNode} containing the arguments in correct order and
     *         wrapped in {@link PromiseNode}s
     * @see #matchNodes(VirtualFrame, RFunction, RNode[], String[], SourceSection, boolean)
     */
    public static MatchedArgumentsNode matchArguments(VirtualFrame frame, RFunction function, CallArgumentsNode suppliedArgs, SourceSection encapsulatingSrc) {

        FormalArguments formals = ((RRootNode) function.getTarget().getRootNode()).getFormalArguments();
        RNode[] wrappedArgs = matchNodes(frame, function, suppliedArgs.getArguments(), suppliedArgs.getNames(), encapsulatingSrc, false);
        return MatchedArgumentsNode.create(wrappedArgs, formals.getNames(), suppliedArgs.getNames(), suppliedArgs.getSourceSection());
    }

    /**
     * Match arguments supplied for a specific function call to the formal arguments and wraps them
     * in special {@link PromiseNode}s. Used for calls to builtins which are built into FastR and
     * thus are implemented in Java
     *
     * @param function The function which is to be called
     * @param suppliedArgs The arguments supplied to the call
     * @param encapsulatingSrc The source code encapsulating the arguments, for debugging purposes
     *
     * @return A fresh {@link InlinedArguments} containing the arguments in correct order and
     *         wrapped in special {@link PromiseNode}s
     * @see #matchNodes(VirtualFrame, RFunction, RNode[], String[], SourceSection, boolean)
     */
    public static InlinedArguments matchArgumentsInlined(VirtualFrame frame, RFunction function, CallArgumentsNode suppliedArgs, SourceSection encapsulatingSrc) {
        RNode[] wrappedArgs = matchNodes(frame, function, suppliedArgs.getArguments(), suppliedArgs.getNames(), encapsulatingSrc, true);
        return new InlinedArguments(wrappedArgs, suppliedArgs.getNames());
    }

    /**
     * Used for the implementation of the 'UseMethod' builtin. Reorders the arguments passed into
     * the called, generic function and prepares them to be passed into the specific function
     *
     * @param frame Needed for eventual promise reduction
     * @param function The 'Method' which is going to be 'Use'd
     * @param evaluatedArgs The arguments which are already in evaluated form (as they are directly
     *            taken from the stack)
     * @param encapsulatingSrc The source code encapsulating the arguments, for debugging purposes
     *
     * @return A Fresh {@link EvaluatedArguments} containing the arguments rearranged and stuffed
     *         with default values (in the form of {@link RPromise}s where needed)
     */
    public static EvaluatedArguments matchArgumentsEvaluated(VirtualFrame frame, RFunction function, EvaluatedArguments evaluatedArgs, SourceSection encapsulatingSrc) {
        RRootNode rootNode = (RRootNode) function.getTarget().getRootNode();
        FormalArguments formals = rootNode.getFormalArguments();
        Object[] evaledArgs = permuteArguments(function, evaluatedArgs.getEvaluatedArgs(), evaluatedArgs.getNames(), formals, new VarArgsAsObjectArrayFactory(), new ObjectArrayFactory(),
                        encapsulatingSrc);

        // Replace RMissing with default value!
        RNode[] defaultArgs = formals.getDefaultArgs();
        for (int i = 0; i < defaultArgs.length; i++) {
            Object evaledArg = evaledArgs[i];
            if (evaledArg == null || evaledArg == RMissing.instance) {      // TODO Gero: Necessary??
                // This is the case whenever there is a new parameter introduced in front of a
                // vararg in the specific version of a generic
                // TODO STRICT!
                RNode defaultArg = formals.getDefaultArg(i);
                if (defaultArg == null) {
                    // If neither supplied nor default argument
                    evaledArgs[i] = RMissing.instance;
                } else {
                    // <null> for environment leads to it being fitted with the REnvironment on the
                    // callee side
                    evaledArgs[i] = RPromise.create(EvalPolicy.STRICT, PromiseType.ARG_DEFAULT, null, defaultArg);
                }
            } else if (function.isBuiltin() && evaledArg instanceof RPromise) {
                RPromise promise = (RPromise) evaledArg;
                evaledArgs[i] = promise.evaluate(frame);
            }
        }
        return new EvaluatedArguments(evaledArgs, formals.getNames());
    }

    /**
     * Matches the supplied arguments to the formal ones and returns them as consolidated
     * {@link MatchedArgumentsNode}. Handles named args and varargs.<br/>
     * <strong>Does not</strong> alter the given {@link CallArgumentsNode}
     *
     * @param frame carrier for missing check
     * @param function The function which is to be called
     * @param suppliedArgs The arguments supplied to the call
     * @param suppliedNames The names for the arguments supplied to the call
     * @param encapsulatingSrc The source code encapsulating the arguments, for debugging purposes
     * @param isForInlinedBuiltin Whether the arguments are passed into an inlined builtin and need
     *            special treatment
     *
     * @return A list of {@link RNode}s which consist of the given arguments in the correct order
     *         and wrapped into the proper {@link PromiseNode}s
     * @see #permuteArguments(RFunction, Object[], String[], FormalArguments, VarArgsFactory,
     *      ArrayFactory, SourceSection)
     */
    private static RNode[] matchNodes(VirtualFrame frame, RFunction function, RNode[] suppliedArgs, String[] suppliedNames, SourceSection encapsulatingSrc, boolean isForInlinedBuiltin) {
        FormalArguments formals = ((RRootNode) function.getTarget().getRootNode()).getFormalArguments();

        // Check for "missing" symbols
        RNode[] suppliedArgsChecked = new RNode[suppliedArgs.length];
        for (int i = 0; i < suppliedArgsChecked.length; i++) {
            RNode arg = suppliedArgs[i];

            // Check for 'missing' arguments: mark them 'missing' by replacing with 'null'
            suppliedArgsChecked[i] = isMissingSymbol(frame, arg) ? RMissingHelper.MISSING_ARGUMENT : arg;
        }

        // Rearrange arguments
        RNode[] resultArgs = permuteArguments(function, suppliedArgs, suppliedNames, formals, new VarArgsAsObjectArrayNodeFactory(), new RNodeArrayFactory(), encapsulatingSrc);
        PromiseWrapper wrapper = isForInlinedBuiltin ? new BuiltinInitPromiseWrapper() : new DefaultPromiseWrapper();
        return wrapInPromises(function, resultArgs, formals, wrapper);
    }

    /**
     * Handles unwrapping of {@link WrapArgumentNode} and checks for {@link ReadVariableNode} which
     * denote symbols
     *
     * @param frame {@link VirtualFrame}
     * @param arg {@link RNode}
     * @return Whether the given argument denotes a 'missing' symbol in the context of the given
     *         frame
     */
    private static boolean isMissingSymbol(VirtualFrame frame, RNode arg) {
        Symbol symbol = RMissingHelper.unwrapSymbol(arg);
        if (symbol != null) {
            Object obj = RMissingHelper.getMissingValue(frame, symbol);

            // Symbol == missingArgument?
            if (obj == RMissing.instance) {
                return true;
            }
        }
        return false;
    }

    /**
     * This method does the heavy lifting of re-arranging arguments by their names and position,
     * also handling varargs.
     *
     * @param function The function which should be called
     * @param suppliedArgs The arguments given to this function call
     * @param suppliedNames The names the arguments might have
     * @param formals The {@link FormalArguments} this function has
     * @param listFactory An abstraction for the creation of list of different types
     * @param arrFactory An abstraction for the generic creation of type safe arrays
     * @param encapsulatingSrc The source code encapsulating the arguments, for debugging purposes
     *
     * @param <T> The type of the given arguments
     * @return An array of type <T> with the supplied arguments in the correct order
     */
    protected static <T> T[] permuteArguments(RFunction function, T[] suppliedArgs, String[] suppliedNames, FormalArguments formals, VarArgsFactory<T> listFactory, ArrayFactory<T> arrFactory,
                    SourceSection encapsulatingSrc) {
        String[] formalNames = formals.getNames();

        // Preparations
        int varArgIndex = formals.getVarArgIndex();
        boolean hasVarArgs = varArgIndex != FormalArguments.NO_VARARG;

        // MATCH by exact name
        T[] resultArgs = arrFactory.newArray(formalNames.length);
        BitSet matchedSuppliedArgs = new BitSet(suppliedNames.length);
        BitSet matchedFormalArgs = new BitSet(formalNames.length);
        int unmatchedNameCount = 0; // The nr of named supplied args that do not match
        // si = suppliedIndex, fi = formalIndex
        for (int si = 0; si < suppliedNames.length; si++) {
            if (suppliedNames[si] == null) {
                continue;
            }

            // Search for argument name inside formal arguments
            int fi = findParameterPosition(formalNames, suppliedNames[si], matchedFormalArgs, si, hasVarArgs, suppliedArgs[si], encapsulatingSrc);
            if (fi >= 0) {
                resultArgs[fi] = suppliedArgs[si];
                matchedSuppliedArgs.set(si);
            } else {
                // Named supplied arg that has no match: Vararg candidate!
                unmatchedNameCount++;
            }
        }

        // TODO MATCH by partial name

        // MATCH by position
        UnmatchedSuppliedIterator<T> siCursor = new UnmatchedSuppliedIterator<>(suppliedArgs, matchedSuppliedArgs);
        for (int fi = 0; fi < resultArgs.length; fi++) {
            // Unmatched?
            if (!matchedFormalArgs.get(fi)) {
                while (siCursor.hasNext() && siCursor.nextIndex() < suppliedNames.length && suppliedNames[siCursor.nextIndex()] != null) {
                    // Slide over named parameters and find subsequent location of unnamed parameter
                    siCursor.next();
                }
                boolean followsDots = hasVarArgs && fi >= varArgIndex;
                if (siCursor.hasNext() && !followsDots) {
                    resultArgs[fi] = siCursor.next();

                    // set formal status AND "remove" supplied arg from list
                    matchedFormalArgs.set(fi);
                    siCursor.remove();
                }
            }
        }

        // MATCH rest to vararg "..."
        if (hasVarArgs) {
            assert listFactory != null;
            int varArgCount = suppliedArgs.length - matchedSuppliedArgs.cardinality();

            // Create vararg array (+ names if necessary)
            T[] varArgsArray = arrFactory.newArray(varArgCount);
            String[] namesArray = null;
            if (unmatchedNameCount != 0) {
                namesArray = new String[varArgCount];
            }

            // Add every supplied argument that has not been matched
            int pos = 0;
            UnmatchedSuppliedIterator<T> si = new UnmatchedSuppliedIterator<>(suppliedArgs, matchedSuppliedArgs);
            while (si.hasNext()) {
                varArgsArray[pos] = si.next();
                si.remove();
                if (namesArray != null) {
                    String suppliedName = suppliedNames[si.lastIndex()];
                    namesArray[pos] = suppliedName != null ? suppliedName : "";
                }
                pos++;
            }
            resultArgs[varArgIndex] = listFactory.makeList(varArgsArray, namesArray);
        }

        // Error check: Unused argument?
        int leftoverCount = suppliedArgs.length - matchedSuppliedArgs.cardinality();
        if (leftoverCount > 0) {
            CompilerDirectives.transferToInterpreterAndInvalidate();

            UnmatchedSuppliedIterator<T> si = new UnmatchedSuppliedIterator<>(suppliedArgs, matchedSuppliedArgs);

            // UNUSED_ARGUMENT(S)?
            if (leftoverCount == 1) {
                // TODO Check for precise error messages
                String argStr = arrFactory.debugString(si.next());
                // TODO This is a HACK, remove it when proper vararg unwrapping is implemented!
                if (argStr.equals(ArgumentsTrait.VARARG_NAME)) {
                    return resultArgs;
                }
                throw RError.error(encapsulatingSrc, RError.Message.UNUSED_ARGUMENT, argStr);
            }

            // Create error message:
            T[] debugArgs = arrFactory.newArray(leftoverCount);
            int pos = 0;
            while (si.hasNext()) {
                debugArgs[pos++] = si.next();
            }

            // TODO Check for precise error messages
            String debugStr = arrFactory.debugString(debugArgs);
            throw RError.error(encapsulatingSrc, RError.Message.UNUSED_ARGUMENTS, debugStr);
        }

        return resultArgs;
    }

    /**
     * Used in
     * {@link ArgumentMatcher#permuteArguments(RFunction, Object[], String[], FormalArguments, VarArgsFactory, ArrayFactory, SourceSection)}
     * for iteration over suppliedArgs
     *
     * @param <T>
     */
    private static class UnmatchedSuppliedIterator<T> implements Iterator<T> {
        private static final int NO_MORE_ARGS = -1;
        private int si = 0;
        private int lastSi = 0;
        private final T[] suppliedArgs;
        private final BitSet matchedSuppliedArgs;

        public UnmatchedSuppliedIterator(T[] suppliedArgs, BitSet matchedSuppliedArgs) {
            this.suppliedArgs = suppliedArgs;
            this.matchedSuppliedArgs = matchedSuppliedArgs;
        }

        /**
         * @return Index of the argument returned by the last {@link #next()} call.
         */
        public int lastIndex() {
            return lastSi;
        }

        /**
         * @return The argument which is going to be returned from the next {@link #next()} call.
         * @throws NoSuchElementException If {@link #hasNext()} == true!
         */
        public int nextIndex() {
            int next = getNextIndex(si);
            if (next == NO_MORE_ARGS) {
                throw new NoSuchElementException();
            }
            return next;
        }

        @Override
        public boolean hasNext() {
            return getNextIndex(si) != NO_MORE_ARGS;
        }

        private int getNextIndex(int from) {
            if (from == NO_MORE_ARGS) {
                return NO_MORE_ARGS;
            }
            int next = matchedSuppliedArgs.nextClearBit(from);
            if (next == NO_MORE_ARGS || next >= suppliedArgs.length) {
                return NO_MORE_ARGS;
            }
            return next;
        }

        @Override
        public T next() {
            int next = getNextIndex(si);
            if (next == NO_MORE_ARGS) {
                throw new NoSuchElementException();
            }
            lastSi = next;
            si = getNextIndex(next + 1);
            return suppliedArgs[lastSi];
        }

        @Override
        public void remove() {
            matchedSuppliedArgs.set(lastSi);
        }
    }

    /**
     * Searches for suppliedName inside formalNames and returns its (formal) index.
     *
     * @param formalNames
     * @param suppliedName
     * @param matchedSuppliedArgs
     * @param suppliedIndex
     * @param hasVarArgs
     * @param debugArgNode
     * @param encapsulatingSrc
     *
     * @return The position of the given suppliedName inside the formalNames. Throws errors if the
     *         argument has been matched before
     */
    private static <T> int findParameterPosition(String[] formalNames, String suppliedName, BitSet matchedSuppliedArgs, int suppliedIndex, boolean hasVarArgs, T debugArgNode,
                    SourceSection encapsulatingSrc) {
        int found = -1;
        for (int i = 0; i < formalNames.length; i++) {
            if (formalNames[i] == null) {
                continue;
            }

            final String formalName = formalNames[i];
            if (formalName.equals(suppliedName)) {
                found = i;
                if (matchedSuppliedArgs.get(found)) {
                    // Has already been matched: Error!
                    throw RError.error(encapsulatingSrc, RError.Message.FORMAL_MATCHED_MULTIPLE, formalName);
                }
                matchedSuppliedArgs.set(found);
                break;
            } else if (formalName.startsWith(suppliedName)) {
                if (found >= 0) {
                    throw RError.error(encapsulatingSrc, RError.Message.ARGUMENT_MATCHES_MULTIPLE, 1 + suppliedIndex);
                }
                found = i;
                if (matchedSuppliedArgs.get(found)) {
                    throw RError.error(encapsulatingSrc, RError.Message.FORMAL_MATCHED_MULTIPLE, formalName);
                }
                matchedSuppliedArgs.set(found);
            }
        }
        if (found >= 0 || hasVarArgs) {
            return found;
        }
        // Error!
        String debugSrc = suppliedName;
        if (debugArgNode instanceof RNode) {
            debugSrc = ((RNode) debugArgNode).getSourceSection().getCode();
        }
        throw RError.error(encapsulatingSrc, RError.Message.UNUSED_ARGUMENT, debugSrc);
    }

    /**
     * Walks a list of given arguments ({@link RNode}s) and wraps them in {@link PromiseNode}s
     * individually by using promiseWrapper (unfolds varargs, too!) if necessary.
     *
     * @param function The function which is to be called
     * @param arguments The arguments passed to the function call, already in correct order
     * @param formals The {@link FormalArguments} for the given function
     * @param promiseWrapper The {@link PromiseWrapper} implementation which handles the wrapping of
     *            individual arguments
     * @return A list of {@link RNode} wrapped in {@link PromiseNode}s
     */
    private static RNode[] wrapInPromises(RFunction function, RNode[] arguments, FormalArguments formals, PromiseWrapper promiseWrapper) {
        RNode[] defaultArgs = formals.getDefaultArgs();

        RNode[] resArgs = arguments;

        // Check whether this is a builtin
        RootNode rootNode = function.getTarget().getRootNode();
        RBuiltinRootNode builtinRootNode = null;
        if (rootNode instanceof RBuiltinRootNode) {
            builtinRootNode = (RBuiltinRootNode) rootNode;
        }

        // Insert promises here!
        EnvProvider envProvider = new EnvProvider();
        int logicalIndex = 0;    // also counts arguments wrapped in vararg
        for (int fi = 0; fi < arguments.length; fi++) {
            RNode arg = arguments[fi];  // arg may be null, which denotes 'no arg supplied'

            // Has varargs? Unfold!
            if (arg instanceof VarArgsAsObjectArrayNode) {
                VarArgsAsObjectArrayNode varArgs = (VarArgsAsObjectArrayNode) arg;
                RNode[] modifiedVArgumentNodes = new RNode[varArgs.elementNodes.length];
                for (int j = 0; j < varArgs.elementNodes.length; j++) {
                    // Obviously single var args have no default values, so null
                    modifiedVArgumentNodes[j] = wrap(promiseWrapper, function, builtinRootNode, envProvider, varArgs.elementNodes[j], null, logicalIndex);
                    logicalIndex++;
                }
                resArgs[fi] = new VarArgsAsObjectArrayNode(modifiedVArgumentNodes, varArgs.getNames());
            } else {
                // Normal argument: just wrap in promise
                RNode defaultArg = fi < defaultArgs.length ? defaultArgs[fi] : null;
                resArgs[fi] = wrap(promiseWrapper, function, builtinRootNode, envProvider, arg, defaultArg, logicalIndex);
                logicalIndex++;
            }
        }
        return resArgs;
    }

    /**
     * @param function The function this argument is wrapped for
     * @param builtinRootNode The {@link RBuiltinRootNode} of the function
     * @param envProvider {@link EnvProvider}
     * @param suppliedArg The argument supplied for this parameter
     * @param defaultValue The default value for this argument
     * @param logicalIndex The logicalIndex of this argument, also counting individual arguments in
     *            varargs
     * @return Either suppliedArg or its defaultValue wrapped up into a {@link PromiseNode} (or
     *         {@link RMissing} in case neither is present!
     */
    private static RNode wrap(PromiseWrapper promiseWrapper, RFunction function, RBuiltinRootNode builtinRootNode, EnvProvider envProvider, RNode suppliedArg, RNode defaultValue, int logicalIndex) {
        // Check: If supplied is missing, forward this one
        if (suppliedArg instanceof ConstantMissingNode) {
            return suppliedArg;
        }

        // Determine whether to choose supplied argument or default value
        RNode expr = null;
        PromiseType promiseType = null;
        if (suppliedArg != null) {
            // Supplied arg
            expr = suppliedArg;
            promiseType = PromiseType.ARG_SUPPLIED;
        } else {
            // Default value
            if (defaultValue != null) {
                expr = defaultValue;
                promiseType = PromiseType.ARG_DEFAULT;
            } else {
                // In this case, we simply return RMissing (like R)
                return ConstantNode.create(RMissing.instance);
            }
        }

        // Create promise
        EvalPolicy evalPolicy = promiseWrapper.getEvalPolicy(function, builtinRootNode, logicalIndex);
        return PromiseNode.create(expr.getSourceSection(), RPromiseFactory.create(evalPolicy, promiseType, expr, defaultValue), envProvider);
    }

    /**
     * Interface for trading the cost of using reflection.
     *
     * <pre>
     * Class<?> argClass = suppliedArgs.getClass().getComponentClass();
     * @SuppressWarning("unchecked")
     * T[] resultArgs = (T[]) Array.newInstance(argClass, size)
     * </pre>
     *
     * against a type safe virtual function call.
     *
     * @param <T> The component type of the arrays to be created
     */
    private interface ArrayFactory<T> {
        /**
         * @param length
         * @return A fresh (type safe) array of type T
         */
        T[] newArray(int length);

        /**
         * @param args
         * @return A {@link String} containing debug names of all given args
         */
        String debugString(T[] args);

        default String debugString(T arg) {
            T[] args = newArray(1);
            args[0] = arg;
            return debugString(args);
        }
    }

    /**
     * {@link ArrayFactory} implementation for {@link RNode}.
     */
    private static class RNodeArrayFactory implements ArrayFactory<RNode> {
        public RNode[] newArray(int length) {
            return new RNode[length];
        }

        public String debugString(RNode[] args) {
            SourceSection src = Utils.sourceBoundingBox(args);
            return String.valueOf(src);
        }
    }

    /**
     * {@link ArrayFactory} implementation for {@link Object}.
     */
    private static class ObjectArrayFactory implements ArrayFactory<Object> {
        public Object[] newArray(int length) {
            return new Object[length];
        }

        public String debugString(Object[] args) {
            StringBuilder b = new StringBuilder();
            for (int i = 0; i < args.length; i++) {
                b.append(String.valueOf(args[i]));
                if (i != args.length - 1) {
                    b.append(", ");
                }
            }
            return b.toString();
        }
    }

    /**
     * This interface was introduced to reuse
     * {@link ArgumentMatcher#wrapInPromises(RFunction, RNode[], FormalArguments, PromiseWrapper)}
     * and encapsulates the wrapping of a single argument into a {@link PromiseNode}.
     */
    private interface PromiseWrapper {
        /**
         * @param function the {@link RFunction} being called
         * @param builtinRootNode The {@link RBuiltinRootNode} of the function
         * @param logicalIndex The logicalIndex of this argument, also counting individual arguments
         *            in varargs
         * @return A single suppliedArg and its corresponding defaultValue wrapped up into a
         *         {@link PromiseNode}
         */
        EvalPolicy getEvalPolicy(RFunction function, RBuiltinRootNode builtinRootNode, int logicalIndex);
    }

    /**
     * {@link PromiseWrapper} implementation for 'normal' function calls.
     */
    private static class DefaultPromiseWrapper implements PromiseWrapper {
        public EvalPolicy getEvalPolicy(RFunction function, RBuiltinRootNode builtinRootNode, int logicalIndex) {
            // This is for actual function calls. However, if the arguments are meant for a builtin,
            // we have to consider whether they should be forced or not!
            return builtinRootNode != null && builtinRootNode.evaluatesArg(logicalIndex) ? EvalPolicy.STRICT : EvalPolicy.PROMISED;
        }
    }

    /**
     * {@link PromiseWrapper} implementation for arguments that are going to be used for 'inlined'
     * builtins.
     *
     * @see RBuiltinRootNode#inline(InlinedArguments)
     */
    private static class BuiltinInitPromiseWrapper implements PromiseWrapper {
        public EvalPolicy getEvalPolicy(RFunction function, RBuiltinRootNode builtinRootNode, int logicalIndex) {
            // This is used for arguments that are going inlined for builtins
            return !builtinRootNode.evaluatesArg(logicalIndex) ? EvalPolicy.PROMISED : EvalPolicy.INLINED;
        }
    }

    /**
     * Abstraction for the generation of varargs.
     *
     * @param <T> The type of the resulting vararg
     */
    public interface VarArgsFactory<T> {
        T makeList(T[] elements, String[] names);
    }

    /**
     * {@link VarArgsFactory} implementation that returns varargs as <code>Object[]</code>.
     */
    public static final class VarArgsAsObjectArrayFactory implements VarArgsFactory<Object> {
        public Object makeList(final Object[] elements, final String[] names) {
            if (elements.length > 0) {
                return new RArgsValuesAndNames(elements, names == null ? new String[elements.length] : names);
            } else {
                return RMissing.instance;
            }
        }
    }

    /**
     * A {@link RNode} that encapsulates a list of varargs (as {@link RNode}).
     */
    public abstract static class VarArgsNode extends RNode {
        @Children protected final RNode[] elementNodes;

        protected VarArgsNode(RNode[] elements) {
            elementNodes = elements;
        }

        public final RNode[] getArgumentNodes() {
            return elementNodes;
        }
    }

    /**
     * {@link VarArgsFactory} implementation that returns varargs as
     * {@link VarArgsAsObjectArrayNode}.
     */
    public static final class VarArgsAsObjectArrayNodeFactory implements VarArgsFactory<RNode> {
        public RNode makeList(final RNode[] elements, final String[] names) {
            if (elements.length > 0) {
                return new VarArgsAsObjectArrayNode(elements, names);
            } else {
                // STRICT: This has to be revised!
                return null;    // ConstantNode.create(RMissing.instance);
            }
        }
    }

    /**
     * {@link VarArgsNode} that executes all its elements and returns the resulting value array.
     */
    public static final class VarArgsAsObjectArrayNode extends VarArgsNode {
        private String[] names;

        public VarArgsAsObjectArrayNode(RNode[] elements, String[] names) {
            super(elements);
            this.names = names;
        }

        public String[] getNames() {
            return names;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            Object[] evaluatedArgs = executeArguments(frame, elementNodes);
            return new RArgsValuesAndNames(evaluatedArgs, names);
        }
    }

    @ExplodeLoop
    private static Object[] executeArguments(VirtualFrame frame, RNode[] elementNodes) {
        Object[] evaluatedArgs = new Object[elementNodes.length];
        int index = 0;
        for (int i = 0; i < elementNodes.length; i++) {
            Object argValue = elementNodes[i].execute(frame);
            if (argValue instanceof RArgsValuesAndNames) {
                // this can happen if ... is simply passed around (in particular when the call chain
                // contains two functions with just the ... argument)
                RArgsValuesAndNames argsValuesAndNames = (RArgsValuesAndNames) argValue;
                evaluatedArgs = Utils.resizeObjectsArray(evaluatedArgs, evaluatedArgs.length + argsValuesAndNames.length() - 1);
                Object[] varargValues = argsValuesAndNames.getValues();
                for (int j = 0; j < argsValuesAndNames.length(); j++) {
                    evaluatedArgs[index++] = varargValues[j];
                }
            } else {
                evaluatedArgs[index++] = argValue;
            }
        }
        return evaluatedArgs;
    }
}
