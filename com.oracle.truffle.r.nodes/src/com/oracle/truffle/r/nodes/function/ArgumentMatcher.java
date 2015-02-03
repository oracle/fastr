/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.RPromise.Closure;
import com.oracle.truffle.r.runtime.data.RPromise.EvalPolicy;
import com.oracle.truffle.r.runtime.data.RPromise.PromiseType;
import com.oracle.truffle.r.runtime.data.RPromise.RPromiseFactory;

/**
 * <p>
 * {@link ArgumentMatcher} serves the purpose of matching {@link CallArgumentsNode} to
 * {@link FormalArguments} of a specific function, see
 * {@link #matchArguments(VirtualFrame, RFunction, UnmatchedArguments, SourceSection, SourceSection)}
 * . The other match functions are used for special cases, where builtins make it necessary to
 * re-match parameters, e.g.:
 * {@link #matchArgumentsEvaluated(VirtualFrame, RFunction, EvaluatedArguments, SourceSection, PromiseHelperNode, boolean)}
 * for 'UseMethod' and
 * {@link #matchArgumentsInlined(VirtualFrame, RFunction, UnmatchedArguments, SourceSection, SourceSection)}
 * for builtins which are implemented in Java ( @see {@link RBuiltinNode#inline(InlinedArguments)}
 * </p>
 *
 * <p>
 * Here are some details on how the argument processing and matching works. The formal arguments
 * list is constructed at the point when the {@link FunctionDefinitionNode} object is constructed.
 * The supplied (actual) arguments list is constructed at the point when the
 * {@link CallArgumentsNode} is constructed. At the point of executing the actual function call, the
 * matching procedure takes a list of formal arguments and a list of supplied (actual) arguments
 * (which actually has to be constructed by flattening "...", as it obviously may contain more then
 * one argument, all of which might have a name, see below) and applies the actual matching
 * algorithm taking into consideration the names and positions of arguments as well as their number.
 * After that, the resulting arguments (potentially reordered and eventually wrapped into "...") are
 * wrapped into additional {@link PromiseNode}s which are basically an abstraction layer for normal
 * and {@link EvalPolicy#INLINED} functions.<br/>
 * The resulting {@link RNode} are cached inside {@link RCallNode} and executed every call (the
 * cache is not invalidated): Depending on whether the function to be called is a normal or
 * {@link EvalPolicy#INLINED} function, or a separate argument needs special treatment, the
 * {@link PromiseNode} returns either a {@link RPromise} OR the directly evaluated value.<br/>
 * The final step of the function call execution is packaging of the resulting values
 * {@code Object[]} into an {@link RArguments} object that is stored in the callee's frame.
 * </p>
 *
 * <p>
 * One caveat here is related to the S3 dispatch procedure. In this case, we have in fact two
 * function calls, one to the "dispatch" function (the one containing the UseMethod call) and one to
 * the function that is ultimately selected. Both functions can have a different list of formal
 * arguments and may require running a separate argument matching procedure. For example, in the
 * following piece of R code, the name of argument b must be available when executing the call to
 * g() for proper argument reordering:
 *
 * f<-function(a,b) { UseMethod("f") }; f.numeric<-function(b,a) { a - b }; f(b=1,2)
 *
 * Consequently, argument names passed to the "dispatch" function are preserved as part of the
 * {@link RArguments} object and made this way available when executing the selected function.
 * </p>
 *
 * <p>
 * Another caveat is related to matching arguments for variadic functions (functions containing the
 * ... argument). On the caller's side, multiple supplied arguments (with their own names) can be
 * encapsulated as a single formal ... argument on the callee's side. In this case, however, R still
 * requires that the names of arguments encapsulated as ... are available to the callee for use in
 * the argument matching procedures down the call chain. For example, in the following piece of R
 * code, argument b is encapsulated as ... when executing the call to f() and yet its name has to be
 * available when executing the call to g() for proper argument reordering:
 *
 * f <- function(...) g(...); g <- function(a,b) { a - b }; f(b=1,2)
 *
 * Consequently, "non-executed" ... arguments are represented as {@link VarArgsNode}-s (inheriting
 * from {@link RNode}) and "executed" .. arguments are represented as a language level value of type
 * {@link RArgsValuesAndNames}, which can be passes directly in the {@link RArguments} object and
 * whose type is understood by the language's builtins (both representations are name-preserving).
 * </p>
 */
public class ArgumentMatcher {

    /**
     * Match arguments supplied for a specific function call to the formal arguments and wraps them
     * in {@link PromiseNode}s. Used for calls to all functions parsed from R code
     *
     * @param frame carrier for missing check
     * @param function The function which is to be called
     * @param suppliedArgs The arguments supplied to the call
     * @param callSrc The source of the function call currently executed
     * @param argsSrc The source code encapsulating the arguments, for debugging purposes
     *
     * @return A fresh {@link MatchedArguments} containing the arguments in correct order and
     *         wrapped in {@link PromiseNode}s
     * @see #matchNodes(VirtualFrame, RFunction, RNode[], String[], SourceSection, SourceSection,
     *      boolean, ClosureCache)
     */
    public static MatchedArguments matchArguments(VirtualFrame frame, RFunction function, UnmatchedArguments suppliedArgs, SourceSection callSrc, SourceSection argsSrc) {
        FormalArguments formals = ((RRootNode) function.getTarget().getRootNode()).getFormalArguments();
        RNode[] wrappedArgs = matchNodes(frame, function, suppliedArgs.getArguments(), suppliedArgs.getNames(), callSrc, argsSrc, false, suppliedArgs);
        return MatchedArguments.create(wrappedArgs, formals.getNames());
    }

    /**
     * Match arguments supplied for a specific function call to the formal arguments and wraps them
     * in special {@link PromiseNode}s. Used for calls to builtins which are built into FastR and
     * thus are implemented in Java
     *
     * @param function The function which is to be called
     * @param suppliedArgs The arguments supplied to the call
     * @param callSrc The source of the function call currently executed
     * @param argsSrc The source code encapsulating the arguments, for debugging purposes
     *
     * @return A fresh {@link InlinedArguments} containing the arguments in correct order and
     *         wrapped in special {@link PromiseNode}s
     * @see #matchNodes(VirtualFrame, RFunction, RNode[], String[], SourceSection, SourceSection,
     *      boolean, ClosureCache)
     */
    public static InlinedArguments matchArgumentsInlined(VirtualFrame frame, RFunction function, UnmatchedArguments suppliedArgs, SourceSection callSrc, SourceSection argsSrc) {
        RNode[] wrappedArgs = matchNodes(frame, function, suppliedArgs.getArguments(), suppliedArgs.getNames(), callSrc, argsSrc, true, suppliedArgs);
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
     * @param callSrc The source code of the call
     * @param forNextMethod matching when evaluating NextMethod
     *
     * @return A Fresh {@link EvaluatedArguments} containing the arguments rearranged and stuffed
     *         with default values (in the form of {@link RPromise}s where needed)
     */
    public static EvaluatedArguments matchArgumentsEvaluated(VirtualFrame frame, RFunction function, EvaluatedArguments evaluatedArgs, SourceSection callSrc, PromiseHelperNode promiseHelper,
                    boolean forNextMethod) {
        RRootNode rootNode = (RRootNode) function.getTarget().getRootNode();
        FormalArguments formals = rootNode.getFormalArguments();
        Object[] evaledArgs = permuteArguments(function, evaluatedArgs.getEvaluatedArgs(), evaluatedArgs.getNames(), formals, new VarArgsAsObjectArrayFactory(), new ObjectArrayFactory(), callSrc,
                        null, forNextMethod);

        // Replace RMissing with default value!
        RNode[] defaultArgs = formals.getDefaultArgs();
        for (int fi = 0; fi < defaultArgs.length; fi++) {
            Object evaledArg = evaledArgs[fi];
            if (evaledArg == null) {
                // This is the case whenever there is a new parameter introduced in front of a
                // vararg in the specific version of a generic
                RNode defaultArg = formals.getDefaultArg(fi);
                if (defaultArg == null) {
                    // If neither supplied nor default argument

                    if (formals.getVarArgIndex() == fi) {
                        // "...", but empty
                        evaledArgs[fi] = RArgsValuesAndNames.EMPTY;
                    } else {
                        evaledArgs[fi] = RMissing.instance;
                    }
                } else {
                    // <null> for environment leads to it being fitted with the REnvironment on the
                    // callee side
                    Closure defaultClosure = formals.getOrCreateClosure(defaultArg);
                    evaledArgs[fi] = RDataFactory.createPromise(EvalPolicy.INLINED, PromiseType.ARG_DEFAULT, null, defaultClosure);
                }
            } else if (function.isBuiltin() && evaledArg instanceof RPromise) {
                RPromise promise = (RPromise) evaledArg;
                evaledArgs[fi] = promiseHelper.evaluate(frame, promise);
            }
        }
        for (int i = 0; i < evaledArgs.length; ++i) {
            if (evaledArgs[i] == null) {
                evaledArgs[i] = RMissing.instance;
            }
        }
        return new EvaluatedArguments(evaledArgs, formals.getNames());
    }

    /**
     * Matches the supplied arguments to the formal ones and returns them as consolidated
     * {@code RNode[]}. Handles named args and varargs.<br/>
     * <strong>Does not</strong> alter the given {@link CallArgumentsNode}
     *
     * @param frame carrier for missing check
     * @param function The function which is to be called
     * @param suppliedArgs The arguments supplied to the call
     * @param suppliedNames The names for the arguments supplied to the call
     * @param callSrc The source of the function call currently executed
     * @param argsSrc The source code encapsulating the arguments, for debugging purposes
     * @param isForInlinedBuiltin Whether the arguments are passed into an inlined builtin and need
     *            special treatment
     * @param closureCache The {@link ClosureCache} for the supplied arguments
     *
     * @return A list of {@link RNode}s which consist of the given arguments in the correct order
     *         and wrapped into the proper {@link PromiseNode}s
     * @see #permuteArguments(RFunction, Object[], String[], FormalArguments, VarArgsFactory,
     *      ArrayFactory, SourceSection, SourceSection, boolean)
     */
    private static RNode[] matchNodes(VirtualFrame frame, RFunction function, RNode[] suppliedArgs, String[] suppliedNames, SourceSection callSrc, SourceSection argsSrc, boolean isForInlinedBuiltin,
                    ClosureCache closureCache) {
        assert suppliedArgs.length == suppliedNames.length;

        FormalArguments formals = ((RRootNode) function.getTarget().getRootNode()).getFormalArguments();

        // Rearrange arguments
        RNode[] resultArgs = permuteArguments(function, suppliedArgs, suppliedNames, formals, new VarArgsAsObjectArrayNodeFactory(), new RNodeArrayFactory(), callSrc, argsSrc, false);

        PromiseWrapper wrapper = isForInlinedBuiltin ? new BuiltinInitPromiseWrapper() : new DefaultPromiseWrapper();
        return wrapInPromises(function, resultArgs, formals, wrapper, closureCache, callSrc);
    }

    /**
     * /** This method does the heavy lifting of re-arranging arguments by their names and position,
     * also handling varargs.
     *
     * @param function The function which should be called
     * @param suppliedArgs The arguments given to this function call
     * @param suppliedNames The names the arguments might have
     * @param formals The {@link FormalArguments} this function has
     * @param listFactory An abstraction for the creation of list of different types
     * @param arrFactory An abstraction for the generic creation of type safe arrays
     * @param callSrc The source of the function call currently executed
     * @param argsSrc The source code encapsulating the arguments, for debugging purposes
     * @param forNextMethod matching when evaluating NextMethod
     *
     * @param <T> The type of the given arguments
     * @return An array of type <T> with the supplied arguments in the correct order
     */
    @TruffleBoundary
    private static <T> T[] permuteArguments(RFunction function, T[] suppliedArgs, String[] suppliedNames, FormalArguments formals, VarArgsFactory<T> listFactory, ArrayFactory<T> arrFactory,
                    SourceSection callSrc, SourceSection argsSrc, boolean forNextMethod) {
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
            if (suppliedNames[si] == null || suppliedNames[si].isEmpty()) {
                continue;
            }

            // Search for argument name inside formal arguments
            int fi = findParameterPosition(formalNames, suppliedNames[si], matchedFormalArgs, si, hasVarArgs, suppliedArgs[si], callSrc, argsSrc, varArgIndex, forNextMethod);
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
                while (siCursor.hasNext() && siCursor.nextIndex() < suppliedNames.length && suppliedNames[siCursor.nextIndex()] != null && !suppliedNames[siCursor.nextIndex()].isEmpty() &&
                                !forNextMethod) {
                    // Slide over named parameters and find subsequent location of unnamed parameter
                    // (if processing args for NextMethod, try to match yet unmatched named
                    // parameters - do not slide over them)
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
                    namesArray[pos] = suppliedName;
                }
                pos++;
            }
            resultArgs[varArgIndex] = listFactory.makeList(varArgsArray, namesArray);
        }

        // Error check: Unused argument?
        int leftoverCount = suppliedArgs.length - matchedSuppliedArgs.cardinality();
        if (leftoverCount > 0) {
            // Check if this is really an error. Might be an inlined "..."!
            UnmatchedSuppliedIterator<T> si = new UnmatchedSuppliedIterator<>(suppliedArgs, matchedSuppliedArgs);
            if (leftoverCount == 1) {
                T arg = si.next();
                if (arrFactory.isVararg(arg)) {
                    return resultArgs;
                }
            }

            // Definitely an error: Prepare error message
            si.reset();
            throwUnusedArgumentError(leftoverCount, si, arrFactory, callSrc);
        }

        return resultArgs;
    }

    @TruffleBoundary
    private static <T> void throwUnusedArgumentError(int leftoverCount, UnmatchedSuppliedIterator<T> si, ArrayFactory<T> arrFactory, SourceSection callSrc) {
        // UNUSED_ARGUMENT(S)?
        if (leftoverCount == 1) {
            CompilerDirectives.transferToInterpreter();
            String argStr = arrFactory.debugString(si.next());
            throw RError.error(callSrc, RError.Message.UNUSED_ARGUMENT, argStr);
        }

        // Create error message
        T[] debugArgs = arrFactory.newArray(leftoverCount);
        int pos = 0;
        while (si.hasNext()) {
            debugArgs[pos++] = si.next();
        }

        CompilerDirectives.transferToInterpreter();
        String argStr = arrFactory.debugString(debugArgs);
        throw RError.error(callSrc, RError.Message.UNUSED_ARGUMENTS, argStr);
    }

    /**
     * Used in
     * {@link ArgumentMatcher#permuteArguments(RFunction, Object[], String[], FormalArguments, VarArgsFactory, ArrayFactory, SourceSection, SourceSection, boolean)}
     * for iteration over suppliedArgs.
     *
     * @param <T>
     */
    private static class UnmatchedSuppliedIterator<T> implements Iterator<T> {
        private static final int NO_MORE_ARGS = -1;
        private int si;
        private int lastSi;
        @CompilationFinal private final T[] suppliedArgs;
        private final BitSet matchedSuppliedArgs;

        public UnmatchedSuppliedIterator(T[] suppliedArgs, BitSet matchedSuppliedArgs) {
            this.suppliedArgs = suppliedArgs;
            this.matchedSuppliedArgs = matchedSuppliedArgs;
            reset();
        }

        public void reset() {
            si = 0;
            lastSi = 0;
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
     * @param callSrc
     * @param argsSrc
     * @param varArgIndex
     * @param forNextMethod
     *
     * @return The position of the given suppliedName inside the formalNames. Throws errors if the
     *         argument has been matched before
     */
    private static <T> int findParameterPosition(String[] formalNames, String suppliedName, BitSet matchedSuppliedArgs, int suppliedIndex, boolean hasVarArgs, T debugArgNode, SourceSection callSrc,
                    SourceSection argsSrc, int varArgIndex, boolean forNextMethod) {
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
                    throw RError.error(argsSrc, RError.Message.FORMAL_MATCHED_MULTIPLE, formalName);
                }
                matchedSuppliedArgs.set(found);
                break;
            } else if (!suppliedName.isEmpty() && formalName.startsWith(suppliedName) && ((varArgIndex != FormalArguments.NO_VARARG && i < varArgIndex) || varArgIndex == FormalArguments.NO_VARARG)) {
                // partial-match only if the formal argument is positioned before ...
                if (found >= 0) {
                    throw RError.error(argsSrc, RError.Message.ARGUMENT_MATCHES_MULTIPLE, 1 + suppliedIndex);
                }
                found = i;
                if (matchedSuppliedArgs.get(found)) {
                    throw RError.error(argsSrc, RError.Message.FORMAL_MATCHED_MULTIPLE, formalName);
                }
                matchedSuppliedArgs.set(found);
            }
        }
        if (found >= 0 || hasVarArgs || forNextMethod) {
            return found;
        }
        // Error!
        String debugSrc = suppliedName;
        if (debugArgNode instanceof RNode) {
            SourceSection ss = ((RNode) debugArgNode).getSourceSection();
            if (ss != null && ss.getCode() != null) {
                debugSrc = ((RNode) debugArgNode).getSourceSection().getCode();
            }
        }
        throw RError.error(callSrc, RError.Message.UNUSED_ARGUMENT, debugSrc);
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
     * @param closureCache The {@link ClosureCache} for the supplied arguments
     * @return A list of {@link RNode} wrapped in {@link PromiseNode}s
     */
    @TruffleBoundary
    private static RNode[] wrapInPromises(RFunction function, RNode[] arguments, FormalArguments formals, PromiseWrapper promiseWrapper, ClosureCache closureCache, SourceSection callSrc) {
        RNode[] defaultArgs = formals.getDefaultArgs();
        RNode[] resArgs = arguments;

        // Check whether this is a builtin
        RootNode rootNode = function.getTarget().getRootNode();
        final RBuiltinRootNode builtinRootNode = rootNode instanceof RBuiltinRootNode ? (RBuiltinRootNode) rootNode : null;

        // int logicalIndex = 0; As our builtin's 'evalsArgs' is meant for FastR arguments (which
        // take "..." as one), we don't need a logicalIndex
        for (int fi = 0; fi < arguments.length; fi++) {
            RNode arg = arguments[fi];  // arg may be null, which denotes 'no arg supplied'

            // Has varargs? Unfold!
            if (arg instanceof VarArgsAsObjectArrayNode) {
                VarArgsAsObjectArrayNode varArgs = (VarArgsAsObjectArrayNode) arg;
                int varArgsLen = varArgs.getArgumentNodes().length;
                String[] newNames = varArgs.getNames() == null ? new String[varArgsLen] : Arrays.copyOf(varArgs.getNames(), varArgsLen);
                RNode[] newVarArgs = Utils.resizeArray(varArgs.getArgumentNodes(), varArgsLen);
                int index = 0;
                for (int i = 0; i < varArgs.getArgumentNodes().length; i++) {
                    RNode varArg = varArgs.getArgumentNodes()[i];
                    if (varArg == null) {
                        if (newNames[i] == null) {
                            // Skip all missing values (important for detection of emtpy "...",
                            // which consequently collapse
                            continue;
                        } else {
                            // But do not skip parameters ala "[...], builtins =, [...]"
                            varArg = ConstantNode.create(RMissing.instance);
                        }
                    }
                    newNames[index] = varArgs.getNames() == null ? null : varArgs.getNames()[i];
                    newVarArgs[index] = varArg;
                    index++;
                }

                // "Delete and shrink": Shrink only if necessary
                int newLength = index;
                if (newLength == 0) {
                    // Corner case: "f <- function(...) g(...); g <- function(...)"
                    // Insert correct "missing"!
                    resArgs[fi] = promiseWrapper.wrap(function, formals, builtinRootNode, closureCache, null, null, fi);
                    continue;
                }
                if (newNames.length > newLength) {
                    newNames = Arrays.copyOf(newNames, newLength);
                    newVarArgs = Arrays.copyOf(newVarArgs, newLength);
                }

                EvalPolicy evalPolicy = promiseWrapper.getEvalPolicy(function, builtinRootNode, fi);
                resArgs[fi] = PromiseNode.createVarArgs(varArgs.getSourceSection(), evalPolicy, newVarArgs, newNames, closureCache, callSrc);
            } else {
                // Normal argument: just wrap in promise
                RNode defaultArg = fi < defaultArgs.length ? defaultArgs[fi] : null;
                resArgs[fi] = promiseWrapper.wrap(function, formals, builtinRootNode, closureCache, arg, defaultArg, fi);
            }
        }
        return resArgs;
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
         * @param arg
         * @return Whether arg represents a <i>formal</i> "..." which carries no content
         */
        default boolean isVararg(T arg) {
            throw Utils.nyi("S3Dispatch should not have arg length mismatch!?");
        }

        /**
         * @param args
         * @return A {@link String} containing debug names of all given args
         */
        String debugString(T[] args);

        @TruffleBoundary
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

        @Override
        public boolean isVararg(RNode arg) {
            // Empty varargs get passed in as "...", and not unrolled. Thus we only have to check
            // the RVNs name
            String name = RMissingHelper.unwrapName(arg);
            return name != null && ArgumentsTrait.isVarArg(name);
        }

        @TruffleBoundary
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

        @TruffleBoundary
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
     * {@link ArgumentMatcher#wrapInPromises(RFunction, RNode[], FormalArguments, PromiseWrapper, ClosureCache, SourceSection)}
     * and encapsulates the wrapping of a single argument into a {@link PromiseNode}.
     */
    private interface PromiseWrapper {
        /**
         * @param function the {@link RFunction} being called
         * @param builtinRootNode The {@link RBuiltinRootNode} of the function
         * @param formalIndex The formalIndex of this argument
         * @return A single suppliedArg and its corresponding defaultValue wrapped up into a
         *         {@link PromiseNode}
         */
        EvalPolicy getEvalPolicy(RFunction function, RBuiltinRootNode builtinRootNode, int formalIndex);

        /**
         * @param function The function this argument is wrapped for
         * @param formals {@link FormalArguments} as {@link ClosureCache}
         * @param builtinRootNode The {@link RBuiltinRootNode} of the function
         * @param closureCache {@link ClosureCache}
         * @param suppliedArg The argument supplied for this parameter
         * @param defaultValue The default value for this argument
         * @param formalIndex The logicalIndex of this argument, also counting individual arguments
         *            in varargs
         * @return Either suppliedArg or its defaultValue wrapped up into a {@link PromiseNode} (or
         *         {@link RMissing} in case neither is present!
         */
        RNode wrap(RFunction function, FormalArguments formals, RBuiltinRootNode builtinRootNode, ClosureCache closureCache, RNode suppliedArg, RNode defaultValue, int formalIndex);
    }

    /**
     * {@link PromiseWrapper} implementation for 'normal' function calls.
     */
    private static class DefaultPromiseWrapper implements PromiseWrapper {
        public EvalPolicy getEvalPolicy(RFunction function, RBuiltinRootNode builtinRootNode, int formalIndex) {
            // This is for actual function calls. However, if the arguments are meant for a builtin,
            // we have to consider whether they should be forced or not!
            return builtinRootNode != null && builtinRootNode.evaluatesArg(formalIndex) ? EvalPolicy.INLINED : EvalPolicy.PROMISED;
        }

        @TruffleBoundary
        public RNode wrap(RFunction function, FormalArguments formals, RBuiltinRootNode builtinRootNode, ClosureCache closureCache, RNode suppliedArg, RNode defaultValue, int formalIndex) {
            // Determine whether to choose supplied argument or default value
            RNode expr = null;
            PromiseType promiseType = null;
            if (suppliedArg != null) {
                // Supplied arg
                expr = suppliedArg;
                promiseType = PromiseType.ARG_SUPPLIED;
            } else {
                if (formals.getVarArgIndex() == formalIndex) {
                    // "...", but empty
                    return ConstantNode.create(RArgsValuesAndNames.EMPTY);
                } else {
                    // In this case, we simply return RMissing (like R)
                    return ConstantNode.create(RMissing.instance);
                }
            }

            // Create promise
            EvalPolicy evalPolicy = getEvalPolicy(function, builtinRootNode, formalIndex);
            Closure closure = closureCache.getOrCreateClosure(expr);
            Closure defaultClosure = formals.getOrCreateClosure(defaultValue);
            return PromiseNode.create(expr.getSourceSection(), RPromiseFactory.create(evalPolicy, promiseType, closure, defaultClosure));
        }
    }

    /**
     * {@link PromiseWrapper} implementation for arguments that are going to be used for 'inlined'
     * builtins.
     *
     * @see RBuiltinRootNode#inline(InlinedArguments)
     */
    private static class BuiltinInitPromiseWrapper implements PromiseWrapper {
        public EvalPolicy getEvalPolicy(RFunction function, RBuiltinRootNode builtinRootNode, int formalIndex) {
            // This is used for arguments that are going inlined for builtins
            return !builtinRootNode.evaluatesArg(formalIndex) ? EvalPolicy.PROMISED : EvalPolicy.INLINED;
        }

        /**
         * @param function The function this argument is wrapped for
         * @param formals {@link FormalArguments} as {@link ClosureCache}
         * @param builtinRootNode The {@link RBuiltinRootNode} of the function
         * @param closureCache {@link ClosureCache}
         * @param suppliedArg The argument supplied for this parameter
         * @param defaultValue The default value for this argument
         * @param formalIndex The logicalIndex of this argument, also counting individual arguments
         *            in varargs
         * @return Either suppliedArg or its defaultValue wrapped up into a {@link PromiseNode} (or
         *         {@link RMissing} in case neither is present!
         */
        @TruffleBoundary
        public RNode wrap(RFunction function, FormalArguments formals, RBuiltinRootNode builtinRootNode, ClosureCache closureCache, RNode suppliedArg, RNode defaultValue, int formalIndex) {
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
                    if (formals.getVarArgIndex() == formalIndex) {
                        // "...", but empty
                        return ConstantNode.create(RArgsValuesAndNames.EMPTY);
                    } else {
                        // In this case, we simply return RMissing (like R)
                        return ConstantNode.create(RMissing.instance);
                    }
                }
            }

            // Create promise
            EvalPolicy evalPolicy = getEvalPolicy(function, builtinRootNode, formalIndex);
            Closure closure = closureCache.getOrCreateClosure(expr);
            Closure defaultClosure = formals.getOrCreateClosure(defaultValue);
            return PromiseNode.create(expr.getSourceSection(), RPromiseFactory.create(evalPolicy, promiseType, closure, defaultClosure));
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
     *
     */
    public static final class VarArgsAsObjectArrayFactory implements VarArgsFactory<Object> {
        /**
         * The call of {@link #nonNull} and the assertion in the "else" clause prevents the creation
         * of an {@link RArgsValuesAndNames} containing any {@code null} values. Experimentally,
         * only length 1 arrays ever contain {@code null} (from the conversion of {@link RMissing}
         * into {@code null} in {@link S3DispatchNode#addArg}). Should this ever change perhaps
         * these should be turned back into {@link RMissing}. Ideally, this invariant should be
         * enforced by the caller(s).
         */
        public Object makeList(Object[] elements, String[] names) {
            if (elements.length > 0 && nonNull(elements)) {
                return new RArgsValuesAndNames(elements, names);
            } else {
                assert elements.length == 0 || elements.length == 1;
                return RArgsValuesAndNames.EMPTY;   // RMissing.instance;
            }
        }

        private static boolean nonNull(Object[] elements) {
            for (int i = 0; i < elements.length; i++) {
                if (elements[i] == null) {
                    return false;
                }
            }
            return true;
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
        public RNode makeList(RNode[] elements, String[] names) {
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
        @Deprecated
        public Object execute(VirtualFrame frame) {
            // Simple container
            throw new UnsupportedOperationException();
        }
    }
}
