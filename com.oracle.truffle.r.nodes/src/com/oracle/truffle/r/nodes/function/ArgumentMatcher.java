/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Arrays;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.nodes.RRootNode;
import com.oracle.truffle.r.nodes.access.ConstantNode;
import com.oracle.truffle.r.nodes.function.PromiseNode.VarArgNode;
import com.oracle.truffle.r.parser.tools.EvaluatedArgumentsVisitor;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RBuiltinKind;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.data.FastPathFactory;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RBuiltinDescriptor;
import com.oracle.truffle.r.runtime.data.REmpty;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.data.RPromise.Closure;
import com.oracle.truffle.r.runtime.data.RPromise.PromiseType;
import com.oracle.truffle.r.runtime.data.RPromise.RPromiseFactory;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
import com.oracle.truffle.r.runtime.nodes.RNode;

/**
 * <p>
 * {@link ArgumentMatcher} serves the purpose of matching {@link CallArgumentsNode} to
 * {@link FormalArguments} of a specific function, see
 * {@link #matchArguments(RFunction, UnmatchedArguments, RBaseNode, boolean)} . The other match
 * functions are used for special cases, where builtins make it necessary to re-match parameters,
 * e.g.: {@link #matchArgumentsEvaluated(RFunction, RArgsValuesAndNames, RBaseNode, boolean)} for
 * 'UseMethod'.
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
 * and inlined functions.<br/>
 * The resulting {@link RNode} are cached inside {@link RCallNode} and executed every call (the
 * cache is not invalidated): Depending on whether the function to be called is a normal or inlined
 * function, or a separate argument needs special treatment, the {@link PromiseNode} returns either
 * a {@link RPromise} OR the directly evaluated value.<br/>
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
 * Consequently, "non-executed" ... arguments are represented as VarArgsNodes (inheriting from
 * {@link RNode}) and "executed" .. arguments are represented as a language level value of type
 * {@link RArgsValuesAndNames}, which can be passes directly in the {@link RArguments} object and
 * whose type is understood by the language's builtins (both representations are name-preserving).
 * </p>
 */
public class ArgumentMatcher {

    /**
     * Match arguments supplied for a specific function call to the formal arguments and wraps them
     * in {@link PromiseNode}s. Used for calls to all functions parsed from R code
     *
     * @param function The function which is to be called
     * @param suppliedArgs The arguments supplied to the call
     * @param callingNode The {@link RBaseNode} invoking the match
     * @return A fresh {@link MatchedArguments} containing the arguments in correct order and
     *         wrapped in {@link PromiseNode}s
     */
    public static RNode[] matchArguments(RFunction function, UnmatchedArguments suppliedArgs, RBaseNode callingNode, boolean noOpt) {
        return matchNodes(function, suppliedArgs.getArguments(), suppliedArgs.getSignature(), callingNode, suppliedArgs, noOpt);
    }

    public static MatchPermutation matchArguments(ArgumentsSignature supplied, ArgumentsSignature formal, RBaseNode callingNode, boolean forNextMethod, RBuiltinDescriptor builtin) {
        CompilerAsserts.neverPartOfCompilation();
        return permuteArguments(supplied, formal, callingNode, forNextMethod, index -> false, index -> supplied.getName(index) == null ? "" : supplied.getName(index), builtin);
    }

    public static ArgumentsSignature getFunctionSignature(RFunction function) {
        RRootNode rootNode = (RRootNode) function.getTarget().getRootNode();
        return rootNode.getFormalArguments().getSignature();
    }

    public static Object[] matchArgumentsEvaluated(MatchPermutation match, Object[] evaluatedArgs, FormalArguments formals) {
        Object[] evaledArgs = new Object[match.resultPermutation.length];
        permuteArguments(formals, match, evaluatedArgs, evaledArgs);
        return evaledArgs;
    }

    @ExplodeLoop
    private static void permuteArguments(FormalArguments formals, MatchPermutation match, Object[] evaluatedArgs, Object[] evaledArgs) {
        for (int formalIndex = 0; formalIndex < match.resultPermutation.length; formalIndex++) {
            int suppliedIndex = match.resultPermutation[formalIndex];

            // Has varargs? Unfold!
            if (suppliedIndex == MatchPermutation.VARARGS) {
                int varArgsLen = match.varargsPermutation.length;
                Object[] newVarArgs = new Object[varArgsLen];
                if (permuteVarArgs(match, evaluatedArgs, varArgsLen, newVarArgs)) {
                    evaledArgs[formalIndex] = new RArgsValuesAndNames(newVarArgs, match.varargsSignature);
                } else {
                    evaledArgs[formalIndex] = RArgsValuesAndNames.EMPTY;
                }
            } else if (suppliedIndex == MatchPermutation.UNMATCHED) {
                evaledArgs[formalIndex] = formals.getInternalDefaultArgumentAt(formalIndex);
            } else {
                evaledArgs[formalIndex] = evaluatedArgs[suppliedIndex];
            }
        }
    }

    @ExplodeLoop
    private static boolean permuteVarArgs(MatchPermutation match, Object[] evaluatedArgs, int varArgsLen, Object[] newVarArgs) {
        CompilerAsserts.compilationConstant(varArgsLen);
        boolean nonNull = false;
        for (int i = 0; i < varArgsLen; i++) {
            newVarArgs[i] = evaluatedArgs[match.varargsPermutation[i]];
            nonNull |= newVarArgs[i] != null;
        }
        return nonNull;
    }

    /**
     * Used for the implementation of the 'UseMethod' builtin. Reorders the arguments passed into
     * the called, generic function and prepares them to be passed into the specific function
     *
     * @param function The 'Method' which is going to be 'Use'd
     * @param evaluatedArgs The arguments which are already in evaluated form (as they are directly
     *            taken from the stack)
     * @param callingNode The {@link Node} invoking the match
     * @param forNextMethod matching when evaluating NextMethod
     *
     * @return A Fresh {@link RArgsValuesAndNames} containing the arguments rearranged and stuffed
     *         with default values (in the form of {@link RPromise}s where needed)
     */
    public static RArgsValuesAndNames matchArgumentsEvaluated(RFunction function, RArgsValuesAndNames evaluatedArgs, RBaseNode callingNode, boolean forNextMethod) {
        RRootNode rootNode = (RRootNode) function.getTarget().getRootNode();
        FormalArguments formals = rootNode.getFormalArguments();
        MatchPermutation match = permuteArguments(evaluatedArgs.getSignature(), formals.getSignature(), callingNode, forNextMethod, index -> {
            throw RInternalError.unimplemented("S3Dispatch should not have arg length mismatch");
        }, index -> evaluatedArgs.getSignature().getName(index), null);

        Object[] evaledArgs = new Object[match.resultPermutation.length];

        for (int formalIndex = 0; formalIndex < match.resultPermutation.length; formalIndex++) {
            int suppliedIndex = match.resultPermutation[formalIndex];

            // Has varargs? Unfold!
            if (suppliedIndex == MatchPermutation.VARARGS) {
                int varArgsLen = match.varargsPermutation.length;
                Object[] newVarArgs = new Object[varArgsLen];
                boolean nonNull = false;
                for (int i = 0; i < varArgsLen; i++) {
                    newVarArgs[i] = evaluatedArgs.getArguments()[match.varargsPermutation[i]];
                    nonNull |= newVarArgs[i] != null;
                }
                if (nonNull) {
                    evaledArgs[formalIndex] = new RArgsValuesAndNames(newVarArgs, match.varargsSignature);
                } else {
                    evaledArgs[formalIndex] = RArgsValuesAndNames.EMPTY;
                }
            } else if (suppliedIndex == MatchPermutation.UNMATCHED || evaluatedArgs.getArgument(suppliedIndex) == null) {
                evaledArgs[formalIndex] = formals.getInternalDefaultArgumentAt(formalIndex);
            } else {
                evaledArgs[formalIndex] = evaluatedArgs.getArgument(suppliedIndex);
            }
        }
        return new RArgsValuesAndNames(evaledArgs, formals.getSignature());
    }

    private static String getErrorForArgument(RNode[] suppliedArgs, ArgumentsSignature suppliedSignature, int index) {
        RNode node = suppliedArgs[index];
        if (node instanceof VarArgNode) {
            CompilerAsserts.neverPartOfCompilation();
            Frame frame = Utils.getActualCurrentFrame();
            try {
                // TODO: this error handling code takes many assumptions about the argument types
                RArgsValuesAndNames varArg = (RArgsValuesAndNames) frame.getObject(frame.getFrameDescriptor().findFrameSlot(ArgumentsSignature.VARARG_NAME));
                RPromise promise = (RPromise) varArg.getArguments()[((VarArgNode) node).getIndex()];
                return promise.getRep().asRSyntaxNode().getSourceSection().getCode();
            } catch (FrameSlotTypeException | ClassCastException e) {
                throw RInternalError.shouldNotReachHere();
            }
        } else {
            String code;
            if (node.asRSyntaxNode().getSourceSection() != null) {
                code = node.asRSyntaxNode().getSourceSection().getCode();
            } else {
                code = "<unknown>"; // RDeparse.deparseForPrint(node.asRSyntaxNode());
            }
            String name = suppliedSignature.getName(index);
            return name == null ? code : name + " = " + code;
        }
    }

    /**
     * Matches the supplied arguments to the formal ones and returns them as consolidated
     * {@code RNode[]}. Handles named args and varargs.<br/>
     * <strong>Does not</strong> alter the given {@link CallArgumentsNode}
     *
     * @param function The function which is to be called
     * @param suppliedArgs The arguments supplied to the call
     * @param suppliedSignature The names for the arguments supplied to the call
     * @param callingNode The {@link RBaseNode} initiating the match
     * @param closureCache The {@link ClosureCache} for the supplied arguments
     * @return A list of {@link RNode}s which consist of the given arguments in the correct order
     *         and wrapped into the proper {@link PromiseNode}s
     */
    private static RNode[] matchNodes(RFunction function, RNode[] suppliedArgs, ArgumentsSignature suppliedSignature, RBaseNode callingNode, ClosureCache closureCache, boolean noOpt) {
        assert suppliedArgs.length == suppliedSignature.getLength();

        FormalArguments formals = ((RRootNode) function.getTarget().getRootNode()).getFormalArguments();

        // Rearrange arguments
        MatchPermutation match = permuteArguments(suppliedSignature, formals.getSignature(), callingNode, false,
                        index -> ArgumentsSignature.VARARG_NAME.equals(RMissingHelper.unwrapName(suppliedArgs[index])), index -> getErrorForArgument(suppliedArgs, suppliedSignature, index),
                        function.getRBuiltin());

        RNode[] resArgs = new RNode[match.resultPermutation.length];

        /**
         * Walks a list of given arguments ({@link RNode}s) and wraps them in {@link PromiseNode}s
         * individually by using promiseWrapper (unfolds varargs, too!) if necessary.
         *
         * @param function The function which is to be called
         * @param arguments The arguments passed to the function call, already in correct order
         * @param formals The {@link FormalArguments} for the given function
         * @param promiseWrapper The {@link PromiseWrapper} implementation which handles the
         *            wrapping of individual arguments
         * @param closureCache The {@link ClosureCache} for the supplied arguments
         * @return A list of {@link RNode} wrapped in {@link PromiseNode}s
         */

        // Check whether this is a builtin
        RBuiltinDescriptor builtin = function.getRBuiltin();
        FastPathFactory fastPath = function.getFastPath();

        // int logicalIndex = 0; As our builtin's 'evalsArgs' is meant for FastR arguments (which
        // take "..." as one), we don't need a logicalIndex
        for (int formalIndex = 0; formalIndex < match.resultPermutation.length; formalIndex++) {
            int suppliedIndex = match.resultPermutation[formalIndex];

            // Has varargs? Unfold!
            if (suppliedIndex == MatchPermutation.VARARGS) {
                int varArgsLen = match.varargsPermutation.length;
                String[] newNames = new String[varArgsLen];
                RNode[] newVarArgs = new RNode[varArgsLen];
                int index = 0;
                for (int i = 0; i < varArgsLen; i++) {
                    RNode varArg = suppliedArgs[match.varargsPermutation[i]];
                    if (varArg == null) {
                        if (match.varargsSignature.getName(i) == null) {
                            // Skip all missing values (important for detection of emtpy "...",
                            // which consequently collapse
                            continue;
                        } else {
                            // But do not skip parameters ala "[...], builtins =, [...]"
                            varArg = ConstantNode.create(RMissing.instance);
                        }
                    }
                    newNames[index] = match.varargsSignature.getName(i);
                    newVarArgs[index] = varArg;
                    index++;
                }

                // "Delete and shrink": Shrink only if necessary
                int newLength = index;
                if (newLength == 0) {
                    // Corner case: "f <- function(...) g(...); g <- function(...)"
                    // Insert correct "missing"!
                    if (formals.getSignature().getVarArgIndex() == formalIndex) {
                        // "...", but empty
                        resArgs[formalIndex] = ConstantNode.create(RArgsValuesAndNames.EMPTY);
                    } else {
                        // In this case, we simply return RMissing (like R)
                        resArgs[formalIndex] = ConstantNode.create(RMissing.instance);
                    }
                    continue;
                }
                if (newNames.length > newLength) {
                    newNames = Arrays.copyOf(newNames, newLength);
                    newVarArgs = Arrays.copyOf(newVarArgs, newLength);
                }

                ArgumentsSignature signature = ArgumentsSignature.get(newNames);
                if (shouldInlineArgument(builtin, formalIndex, fastPath)) {
                    resArgs[formalIndex] = PromiseNode.createVarArgsInlined(newVarArgs, signature);
                } else {
                    boolean forcedEager = fastPath != null && fastPath.forcedEagerPromise(formalIndex);
                    resArgs[formalIndex] = PromiseNode.createVarArgs(newVarArgs, signature, closureCache, forcedEager);
                }
            } else if (suppliedIndex == MatchPermutation.UNMATCHED || suppliedArgs[suppliedIndex] == null) {
                resArgs[formalIndex] = wrapUnmatched(formals, builtin, formalIndex, noOpt);
            } else {
                resArgs[formalIndex] = wrapMatched(formals, builtin, closureCache, suppliedArgs[suppliedIndex], formalIndex, noOpt, fastPath);
            }
        }
        return resArgs;
    }

    /**
     * @param builtin The {@link RBuiltinDescriptor} of the function
     * @param formalIndex The formalIndex of this argument
     * @param fastPath
     * @return A single suppliedArg and its corresponding defaultValue wrapped up into a
     *         {@link PromiseNode}
     */
    private static boolean shouldInlineArgument(RBuiltinDescriptor builtin, int formalIndex, FastPathFactory fastPath) {
        if (fastPath != null && fastPath.evaluatesArgument(formalIndex)) {
            return true;
        }
        // This is for actual function calls. However, if the arguments are meant for a
        // builtin, we have to consider whether they should be forced or not!
        return builtin != null && builtin.evaluatesArg(formalIndex);
    }

    @TruffleBoundary
    private static RNode wrapUnmatched(FormalArguments formals, RBuiltinDescriptor builtin, int formalIndex, boolean noOpt) {
        if (builtin != null && !builtin.evaluatesArg(formalIndex) && formals.getDefaultArgument(formalIndex) != null) {
            /*
             * this is a non-evaluated builtin argument, create a proper promise (might have been
             * missing for ~ operator), for which just go with RMissing value (see
             * InfixEmulationFunctions.tilde).
             */
            RNode defaultArg = formals.getDefaultArgument(formalIndex);
            Closure defaultClosure = formals.getOrCreateClosure(defaultArg);
            return PromiseNode.create(RPromiseFactory.create(PromiseType.ARG_DEFAULT, defaultClosure), noOpt, false);
        }
        return ConstantNode.create(formals.getInternalDefaultArgumentAt(formalIndex));
    }

    @TruffleBoundary
    private static RNode wrapMatched(FormalArguments formals, RBuiltinDescriptor builtin, ClosureCache closureCache, RNode suppliedArg, int formalIndex, boolean noOpt, FastPathFactory fastPath) {
        // Create promise, unless it's the empty value
        if (suppliedArg instanceof ConstantNode) {
            ConstantNode a = (ConstantNode) suppliedArg;
            if (a.getValue() == REmpty.instance) {
                return a;
            }
        }
        if (shouldInlineArgument(builtin, formalIndex, fastPath)) {
            return PromiseNode.createInlined(suppliedArg, formals.getInternalDefaultArgumentAt(formalIndex), builtin == null || builtin.getKind() == RBuiltinKind.PRIMITIVE);
        } else {
            Closure closure = closureCache.getOrCreateClosure(suppliedArg);
            boolean forcedEager = fastPath != null && fastPath.forcedEagerPromise(formalIndex) && EvaluatedArgumentsVisitor.isSimpleArgument(suppliedArg.asRSyntaxNode());
            return PromiseNode.create(RPromiseFactory.create(PromiseType.ARG_SUPPLIED, closure), noOpt, forcedEager);
        }
    }

    public static final class MatchPermutation {
        public static final int UNMATCHED = -1;
        public static final int VARARGS = -2;

        @CompilationFinal private final int[] resultPermutation;
        @CompilationFinal private final int[] varargsPermutation;
        private final ArgumentsSignature varargsSignature;

        private MatchPermutation(int[] resultPermutation, int[] varargsPermutation, ArgumentsSignature varargsSignature) {
            this.resultPermutation = resultPermutation;
            this.varargsPermutation = varargsPermutation;
            this.varargsSignature = varargsSignature;
        }
    }

    /**
     * /** This method does the heavy lifting of re-arranging arguments by their names and position,
     * also handling varargs.
     *
     * @param signature The signature (==names) of the supplied arguments
     * @param formalSignature The signature (==names) of the formal arguments
     * @param callingNode The {@link Node} invoking the match
     * @param forNextMethod matching when evaluating NextMethod
     * @param builtin builtin function descriptor (or null if not a builtin)
     * @return An array of type <T> with the supplied arguments in the correct order
     */
    @TruffleBoundary
    private static MatchPermutation permuteArguments(ArgumentsSignature signature, ArgumentsSignature formalSignature, RBaseNode callingNode, boolean forNextMethod, IntPredicate isVarSuppliedVarargs,
                    IntFunction<String> errorString, RBuiltinDescriptor builtin) {
        // assert Arrays.stream(suppliedNames).allMatch(name -> name == null || !name.isEmpty());

        // Preparations
        int varArgIndex = formalSignature.getVarArgIndex();
        boolean hasVarArgs = varArgIndex != ArgumentsSignature.NO_VARARG;

        // MATCH by exact name
        int[] resultPermutation = new int[formalSignature.getLength()];
        Arrays.fill(resultPermutation, MatchPermutation.UNMATCHED);

        boolean[] matchedSuppliedArgs = new boolean[signature.getLength()];
        for (int suppliedIndex = 0; suppliedIndex < signature.getLength(); suppliedIndex++) {
            if (signature.getName(suppliedIndex) == null || signature.getName(suppliedIndex).isEmpty()) {
                continue;
            }

            // Search for argument name inside formal arguments
            int formalIndex = findParameterPosition(formalSignature, signature.getName(suppliedIndex), resultPermutation, suppliedIndex, hasVarArgs, callingNode, varArgIndex, forNextMethod,
                            errorString, builtin);
            if (formalIndex != MatchPermutation.UNMATCHED) {
                resultPermutation[formalIndex] = suppliedIndex;
                matchedSuppliedArgs[suppliedIndex] = true;
            }
        }

        // TODO MATCH by partial name (up to the vararg, which consumes all non-exact matches)

        // MATCH by position
        int suppliedIndex = -1;
        int regularArgumentCount = hasVarArgs ? varArgIndex : formalSignature.getLength();
        outer: for (int formalIndex = 0; formalIndex < regularArgumentCount; formalIndex++) {
            // Unmatched?
            if (resultPermutation[formalIndex] == MatchPermutation.UNMATCHED) {
                while (true) {
                    suppliedIndex++;
                    if (suppliedIndex == signature.getLength()) {
                        // no more unmatched supplied arguments
                        break outer;
                    }
                    if (!matchedSuppliedArgs[suppliedIndex]) {
                        if (forNextMethod) {
                            // for NextMethod, unused parameters are matched even when named
                            break;
                        }
                        if (signature.getName(suppliedIndex) == null || signature.getName(suppliedIndex).isEmpty()) {
                            // unnamed parameter, match by position
                            break;
                        }
                    }
                }
                resultPermutation[formalIndex] = suppliedIndex;

                // set formal status AND "remove" supplied arg from list
                matchedSuppliedArgs[suppliedIndex] = true;
            }
        }

        // MATCH rest to vararg "..."
        if (hasVarArgs) {
            int varArgCount = signature.getLength() - cardinality(matchedSuppliedArgs);

            // Create vararg array
            int[] varArgsPermutation = new int[varArgCount];
            String[] namesArray = new String[varArgCount];

            // Add every supplied argument that has not been matched
            int pos = 0;
            for (suppliedIndex = 0; suppliedIndex < signature.getLength(); suppliedIndex++) {
                if (!matchedSuppliedArgs[suppliedIndex]) {
                    // match only non-missing arguments to vararg
                    matchedSuppliedArgs[suppliedIndex] = true;
                    varArgsPermutation[pos] = suppliedIndex;
                    namesArray[pos] = signature.getName(suppliedIndex);
                    pos++;
                }
            }
            varArgsPermutation = Arrays.copyOf(varArgsPermutation, pos);
            namesArray = Arrays.copyOf(namesArray, pos);

            resultPermutation[varArgIndex] = MatchPermutation.VARARGS;
            return new MatchPermutation(resultPermutation, varArgsPermutation, ArgumentsSignature.get(namesArray));
        } else {
            // Error check: Unused argument? (can only happen when there are no varargs)

            suppliedIndex = 0;
            while (suppliedIndex < signature.getLength() && matchedSuppliedArgs[suppliedIndex]) {
                suppliedIndex++;
            }

            if (suppliedIndex < signature.getLength()) {
                int leftoverCount = signature.getLength() - cardinality(matchedSuppliedArgs);
                if (leftoverCount == 1) {
                    if (isVarSuppliedVarargs.test(suppliedIndex)) {
                        return new MatchPermutation(resultPermutation, null, null);
                    }

                    // one unused argument
                    CompilerDirectives.transferToInterpreter();
                    throw RError.error(callingNode, RError.Message.UNUSED_ARGUMENT, errorString.apply(suppliedIndex));
                }

                CompilerDirectives.transferToInterpreter();
                // multiple unused arguments
                StringBuilder str = new StringBuilder();
                int cnt = 0;
                for (; suppliedIndex < signature.getLength(); suppliedIndex++) {
                    if (!matchedSuppliedArgs[suppliedIndex]) {
                        if (cnt++ > 0) {
                            str.append(", ");
                        }
                        str.append(errorString.apply(suppliedIndex));
                    }
                }
                throw RError.error(callingNode, RError.Message.UNUSED_ARGUMENTS, str);
            }
            return new MatchPermutation(resultPermutation, null, null);
        }
    }

    private static int cardinality(boolean[] array) {
        int sum = 0;
        for (boolean b : array) {
            if (b) {
                sum++;
            }
        }
        return sum;
    }

    /**
     * Searches for suppliedName inside formalNames and returns its (formal) index.
     *
     * @return The position of the given suppliedName inside the formalNames. Throws errors if the
     *         argument has been matched before
     */
    private static <T> int findParameterPosition(ArgumentsSignature formalsSignature, String suppliedName, int[] resultPermutation, int suppliedIndex, boolean hasVarArgs, RBaseNode callingNode,
                    int varArgIndex, boolean forNextMethod, IntFunction<String> errorString, RBuiltinDescriptor builtin) {
        int found = MatchPermutation.UNMATCHED;
        for (int i = 0; i < formalsSignature.getLength(); i++) {
            String formalName = formalsSignature.getName(i);
            if (formalName == null) {
                continue;
            }

            if (formalName.equals(suppliedName)) {
                found = i;
                if (resultPermutation[found] != MatchPermutation.UNMATCHED) {
                    if (builtin != null && builtin.getKind() == RBuiltinKind.PRIMITIVE && hasVarArgs) {
                        // for primitives, the first argument is matched, and the others are folded
                        // into varargs, for example:
                        // x<-1:64; dim(x)<-c(4,4,2,2); x[1,drop=FALSE,1,drop=TRUE,-1]
                        found = MatchPermutation.UNMATCHED;
                    } else {
                        // Has already been matched: Error!
                        throw RError.error(callingNode, RError.Message.FORMAL_MATCHED_MULTIPLE, formalName);
                    }
                }
                break;
            } else if (!suppliedName.isEmpty() && formalName.startsWith(suppliedName) &&
                            ((varArgIndex != ArgumentsSignature.NO_VARARG && i < varArgIndex) || varArgIndex == ArgumentsSignature.NO_VARARG)) {
                // partial-match only if the formal argument is positioned before ...
                if (found >= 0) {
                    throw RError.error(callingNode, RError.Message.ARGUMENT_MATCHES_MULTIPLE, 1 + suppliedIndex);
                }
                found = i;
                if (resultPermutation[found] != MatchPermutation.UNMATCHED) {
                    throw RError.error(callingNode, RError.Message.FORMAL_MATCHED_MULTIPLE, formalName);
                }
            }
        }
        if (found >= 0 || hasVarArgs || forNextMethod) {
            return found;
        }
        throw RError.error(callingNode, RError.Message.UNUSED_ARGUMENT, errorString.apply(suppliedIndex));
    }
}
