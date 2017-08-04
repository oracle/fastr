/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.nodes.RASTUtils;
import com.oracle.truffle.r.nodes.RRootNode;
import com.oracle.truffle.r.nodes.access.ConstantNode;
import com.oracle.truffle.r.nodes.access.variables.ReadVariableNode;
import com.oracle.truffle.r.nodes.function.PromiseNode.VarArgNode;
import com.oracle.truffle.r.runtime.Arguments;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RArguments.S3DefaultArguments;
import com.oracle.truffle.r.runtime.RDeparse;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.builtins.FastPathFactory;
import com.oracle.truffle.r.runtime.builtins.RBuiltinDescriptor;
import com.oracle.truffle.r.runtime.builtins.RBuiltinKind;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.REmpty;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.data.RPromise.Closure;
import com.oracle.truffle.r.runtime.data.RPromise.PromiseState;
import com.oracle.truffle.r.runtime.data.RPromise.RPromiseFactory;
import com.oracle.truffle.r.runtime.env.frame.FrameSlotChangeMonitor;
import com.oracle.truffle.r.runtime.nodes.EvaluatedArgumentsVisitor;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxLookup;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

/**
 * <p>
 * {@link ArgumentMatcher} serves the purpose of matching {@link CallArgumentsNode} to
 * {@link FormalArguments} of a specific function, see
 * {@link #matchArguments(RRootNode, CallArgumentsNode, ArgumentsSignature, S3DefaultArguments, RBaseNode, boolean)}
 * . The other match functions are used for special cases, where builtins make it necessary to
 * re-match parameters, e.g.:
 * {@link #matchArgumentsEvaluated(RRootNode, RArgsValuesAndNames, S3DefaultArguments, RBaseNode)}
 * for 'UseMethod'.
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
 * f.numeric() for proper argument reordering:
 *
 * f<-function(a,b) { UseMethod("f") }; f.numeric<-function(b,a) { a - b }; f(b=1,2)
 *
 * Consequently, argument names passed to the "dispatch" function are preserved as part of the
 * {@link RArguments} object and made this way available when executing the selected function.
 * Moreover, we also need to preserve the original signature to distinguish between positional and
 * named parameters. This is also stored using {@link RArguments} as "supplied signature". Note:
 * this is only supplied signatures matched to formal parameter, if an actual parameter is
 * considered to be part of varargs (...) its name will be preserved as part of
 * {@link RArgsValuesAndNames}, see below.
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
     * @param target The function which is to be called
     * @param arguments The arguments supplied to the call
     * @param callingNode The {@link RBaseNode} invoking the match
     * @return A fresh {@link Arguments} containing the arguments in correct order and wrapped in
     *         {@link PromiseNode}s
     */
    public static Arguments<RNode> matchArguments(RRootNode target, CallArgumentsNode arguments, ArgumentsSignature varArgSignature, S3DefaultArguments s3DefaultArguments, RBaseNode callingNode,
                    boolean noOpt) {
        CompilerAsserts.neverPartOfCompilation();
        assert arguments.containsVarArgsSymbol() == (varArgSignature != null);

        RNode[] argNodes;
        ArgumentsSignature signature;
        if (!arguments.containsVarArgsSymbol()) {
            argNodes = arguments.getArguments();
            signature = arguments.getSignature();
        } else {
            Arguments<RNode> suppliedArgs = arguments.unrollArguments(varArgSignature);
            argNodes = suppliedArgs.getArguments();
            signature = suppliedArgs.getSignature();
        }
        return ArgumentMatcher.matchNodes(target, argNodes, signature, s3DefaultArguments, callingNode, arguments, noOpt);
    }

    public static MatchPermutation matchArguments(ArgumentsSignature supplied, ArgumentsSignature formal, RBaseNode callingNode, RBuiltinDescriptor builtin) {
        CompilerAsserts.neverPartOfCompilation();
        return permuteArguments(supplied, formal, callingNode, index -> false, index -> supplied.getName(index) == null ? "" : supplied.getName(index), builtin);
    }

    public static ArgumentsSignature getFunctionSignature(RFunction function) {
        RRootNode rootNode = (RRootNode) function.getTarget().getRootNode();
        return rootNode.getFormalArguments().getSignature();
    }

    public static RArgsValuesAndNames matchArgumentsEvaluated(MatchPermutation match, Object[] evaluatedArgs, S3DefaultArguments s3DefaultArguments, FormalArguments formals) {
        Object[] evaledArgs = new Object[match.resultPermutation.length];
        permuteArguments(formals, match, evaluatedArgs, evaledArgs, s3DefaultArguments);
        return new RArgsValuesAndNames(evaledArgs, match.resultSignature);
    }

    @ExplodeLoop
    private static void permuteArguments(FormalArguments formals, MatchPermutation match, Object[] evaluatedArgs, Object[] evaledArgs, S3DefaultArguments s3DefaultArguments) {
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
                Object defaultValue = getS3DefaultArgumentValue(s3DefaultArguments, formals, formalIndex);
                if (defaultValue == null) {
                    defaultValue = formals.getInternalDefaultArgumentAt(formalIndex);
                }
                evaledArgs[formalIndex] = defaultValue;
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
     * Used for matching varargs to an internally defined signature. Reorders the arguments passed
     * into the called, generic function and prepares them to be passed into the specific function
     *
     * @param formals The formal arguments to match to.
     * @param evaluatedArgs The arguments which are already in evaluated form (as they are directly
     *            taken from the stack)
     * @param s3DefaultArguments default values carried over from S3 group dispatch method (e.g.
     *            from max to Summary.factor). {@code null} if there are no such arguments.
     * @param callingNode The {@link Node} invoking the match
     * @return A Fresh {@link RArgsValuesAndNames} containing the arguments rearranged and stuffed
     *         with default values (in the form of {@link RPromise}s where needed)
     */
    public static RArgsValuesAndNames matchArgumentsEvaluated(FormalArguments formals, RArgsValuesAndNames evaluatedArgs, S3DefaultArguments s3DefaultArguments, RBaseNode callingNode) {
        MatchPermutation match = permuteArguments(evaluatedArgs.getSignature(), formals.getSignature(), callingNode, index -> {
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
                Object defaultValue = getS3DefaultArgumentValue(s3DefaultArguments, formals, formalIndex);
                if (defaultValue == null) {
                    defaultValue = formals.getInternalDefaultArgumentAt(formalIndex);
                }
                evaledArgs[formalIndex] = defaultValue;
            } else {
                evaledArgs[formalIndex] = evaluatedArgs.getArgument(suppliedIndex);
            }
        }
        return new RArgsValuesAndNames(evaledArgs, match.resultSignature);
    }

    /**
     * Used for the implementation of the 'UseMethod' builtin. Reorders the arguments passed into
     * the called, generic function and prepares them to be passed into the specific function
     *
     * @param target The 'Method' which is going to be 'Use'd
     * @param evaluatedArgs The arguments which are already in evaluated form (as they are directly
     *            taken from the stack)
     * @param s3DefaultArguments default values carried over from S3 group dispatch method (e.g.
     *            from max to Summary.factor). {@code null} if there are no such arguments.
     * @param callingNode The {@link Node} invoking the match
     * @return A Fresh {@link RArgsValuesAndNames} containing the arguments rearranged and stuffed
     *         with default values (in the form of {@link RPromise}s where needed)
     */
    public static RArgsValuesAndNames matchArgumentsEvaluated(RRootNode target, RArgsValuesAndNames evaluatedArgs, S3DefaultArguments s3DefaultArguments, RBaseNode callingNode) {
        return matchArgumentsEvaluated(target.getFormalArguments(), evaluatedArgs, s3DefaultArguments, callingNode);
    }

    private static String getErrorForArgument(RNode[] suppliedArgs, ArgumentsSignature suppliedSignature, int index) {
        CompilerAsserts.neverPartOfCompilation();
        RNode node = suppliedArgs[index];
        if (node instanceof VarArgNode) {
            CompilerAsserts.neverPartOfCompilation();
            Frame frame = Utils.getActualCurrentFrame();
            try {
                // TODO: this error handling code takes many assumptions about the argument types
                FrameSlot frameSlot = frame.getFrameDescriptor().findFrameSlot(ArgumentsSignature.VARARG_NAME);
                RArgsValuesAndNames varArg = (RArgsValuesAndNames) FrameSlotChangeMonitor.getObject(frameSlot, frame);
                RPromise promise = (RPromise) varArg.getArguments()[((VarArgNode) node).getIndex()];
                return RDeparse.deparseSyntaxElement(promise.getRep().asRSyntaxNode());
            } catch (FrameSlotTypeException | ClassCastException e) {
                throw RInternalError.shouldNotReachHere();
            }
        } else {
            String code;
            if (node.asRSyntaxNode().getSourceSection() != null) {
                code = RDeparse.deparseSyntaxElement(node.asRSyntaxNode());
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
     * @param target The function which is to be called
     * @param suppliedArgs The arguments supplied to the call
     * @param suppliedSignature The names for the arguments supplied to the call
     * @param callingNode The {@link RBaseNode} initiating the match
     * @param closureCache The {@link ClosureCache} for the supplied arguments
     * @return A list of {@link RNode}s which consist of the given arguments in the correct order
     *         and wrapped into the proper {@link PromiseNode}s and the supplied signature reordered
     *         accordingly.
     */
    private static Arguments<RNode> matchNodes(RRootNode target, RNode[] suppliedArgs, ArgumentsSignature suppliedSignature, S3DefaultArguments s3DefaultArguments, RBaseNode callingNode,
                    ClosureCache closureCache, boolean noOpt) {
        CompilerAsserts.neverPartOfCompilation();
        assert suppliedArgs.length == suppliedSignature.getLength();

        FormalArguments formals = target.getFormalArguments();

        // Rearrange arguments
        MatchPermutation match = permuteArguments(suppliedSignature, formals.getSignature(), callingNode,
                        index -> RASTUtils.isLookup(suppliedArgs[index], ArgumentsSignature.VARARG_NAME), index -> getErrorForArgument(suppliedArgs, suppliedSignature, index),
                        target.getBuiltin());

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
        RBuiltinDescriptor builtin = target.getBuiltin();
        FastPathFactory fastPath = target.getFastPath();

        boolean hasAssignment = false;
        for (int i = 0; !hasAssignment && i < suppliedArgs.length; i++) {
            if (suppliedArgs[i] != null) {
                hasAssignment = EvaluatedArgumentsVisitor.hasAssignmentCall(suppliedArgs[i].asRSyntaxNode());
            }
        }

        if (hasAssignment) {
            fastPath = null;
        }

        // int logicalIndex = 0; As our builtin's 'evalsArgs' is meant for FastR arguments (which
        // take "..." as one), we don't need a logicalIndex
        for (int formalIndex = 0; formalIndex < match.resultPermutation.length; formalIndex++) {
            int suppliedIndex = match.resultPermutation[formalIndex];

            // Has varargs? Unfold!
            if (suppliedIndex == MatchPermutation.VARARGS) {
                int varArgsLen = match.varargsPermutation.length;
                boolean shouldInline = shouldInlineArgument(builtin, formalIndex, fastPath);
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
                    newVarArgs[index] = shouldInline ? updateInlinedArg(varArg) : varArg;
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
                if (shouldInline) {
                    resArgs[formalIndex] = PromiseNode.createVarArgsInlined(newVarArgs, signature);
                } else {
                    boolean forcedEager = fastPath != null && fastPath.forcedEagerPromise(formalIndex);
                    resArgs[formalIndex] = PromiseNode.createVarArgs(newVarArgs, signature, closureCache, forcedEager);
                }
            } else if (suppliedIndex == MatchPermutation.UNMATCHED || suppliedArgs[suppliedIndex] == null) {
                Object defaultValue = getS3DefaultArgumentValue(s3DefaultArguments, formals, formalIndex);
                if (defaultValue == null) {
                    resArgs[formalIndex] = wrapUnmatched(formals, builtin, formalIndex, noOpt && hasAssignment);
                } else {
                    resArgs[formalIndex] = ConstantNode.create(defaultValue);
                }
            } else {
                resArgs[formalIndex] = wrapMatched(formals, builtin, closureCache, suppliedArgs[suppliedIndex], formalIndex, noOpt || hasAssignment, fastPath);
            }
        }
        return Arguments.create(resArgs, match.resultSignature);
    }

    /**
     * For given instance of {@link S3DefaultArguments} (can be {@code null}) returns the default
     * value if applicable (i.e. the signature matches). Note: currently, this logic is on purpose
     * semi-generic and semi-hardcoded, see {@code RCallNode.callGroupGeneric} for more details.
     */
    private static Object getS3DefaultArgumentValue(S3DefaultArguments s3DefaultArguments, FormalArguments formals, int formalIndex) {
        if (s3DefaultArguments == RArguments.SUMMARY_GROUP_DEFAULT_VALUE_NA_RM && formals.getSignature().getName(formalIndex) == RArguments.SUMMARY_GROUP_NA_RM_ARG_NAME) {
            return RRuntime.asLogical(false);
        }
        return null;
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

    /**
     * Reads of the {@link RMissing} values must not be reported as error in inlined varargs. This
     * method updates any wrapped ReadVariableNode to just return missing values without raising an
     * error.
     *
     * see {@code com.oracle.truffle.r.nodes.function.PromiseNode.InlineVarArgsNode}
     */
    private static RNode updateInlinedArg(RNode node) {
        if (!(node instanceof WrapArgumentNode)) {
            return node;
        }
        WrapArgumentNode wrapper = (WrapArgumentNode) node;
        RSyntaxNode syntaxNode = wrapper.getOperand().asRSyntaxNode();
        if (!(syntaxNode instanceof RSyntaxLookup)) {
            return node;
        }
        RSyntaxLookup lookup = (RSyntaxLookup) syntaxNode;
        ReadVariableNode newRvn = ReadVariableNode.createSilentMissing(lookup.getIdentifier(), lookup.isFunctionLookup() ? RType.Function : RType.Any);
        return WrapArgumentNode.create(ReadVariableNode.wrap(lookup.getLazySourceSection(), newRvn).asRNode(), wrapper.getIndex());
    }

    private static RNode wrapUnmatched(FormalArguments formals, RBuiltinDescriptor builtin, int formalIndex, boolean noOpt) {
        if (builtin != null && !builtin.evaluatesArg(formalIndex) && formals.getDefaultArgument(formalIndex) != null) {
            /*
             * this is a non-evaluated builtin argument, create a proper promise (might have been
             * missing for ~ operator), for which just go with RMissing value (see
             * InfixEmulationFunctions.tilde).
             */
            RNode defaultArg = formals.getDefaultArgument(formalIndex);
            Closure defaultClosure = formals.getOrCreateClosure(defaultArg);
            return PromiseNode.create(RPromiseFactory.create(PromiseState.Default, defaultClosure), noOpt, false);
        }
        return ConstantNode.create(formals.getInternalDefaultArgumentAt(formalIndex));
    }

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
            boolean forcedEager = fastPath != null && fastPath.forcedEagerPromise(formalIndex);
            return PromiseNode.create(RPromiseFactory.create(PromiseState.Supplied, closure), noOpt, forcedEager);
        }
    }

    public static final class MatchPermutation {
        public static final int UNMATCHED = -1;
        public static final int VARARGS = -2;

        @CompilationFinal(dimensions = 1) private final int[] resultPermutation;
        @CompilationFinal(dimensions = 1) private final int[] varargsPermutation;
        private final ArgumentsSignature varargsSignature;

        /**
         * For each formal argument we keep the its original call-site name. For example, when
         * formal named 'x' in 'foo <- function(x) {}' was invoked as 'foo(42)' we remember that 'x'
         * did not have explicit name, i.e. its signature would be {@code null}. This array is
         * ordered according to the formal signature.
         */
        private final ArgumentsSignature resultSignature;

        private MatchPermutation(int[] resultPermutation, ArgumentsSignature resultSignature, int[] varargsPermutation, ArgumentsSignature varargsSignature) {
            this.resultPermutation = resultPermutation;
            this.resultSignature = resultSignature;
            this.varargsPermutation = varargsPermutation;
            this.varargsSignature = varargsSignature;
        }
    }

    /**
     * This method does the heavy lifting of re-arranging arguments by their names and position,
     * also handling varargs.
     *
     * @param signature The signature (==names) of the supplied arguments
     * @param formalSignature The signature (==names) of the formal arguments
     * @param callingNode The {@link Node} invoking the match
     * @param builtin builtin function descriptor (or null if not a builtin)
     * @return An array of type <T> with the supplied arguments in the correct order
     */
    @TruffleBoundary
    private static MatchPermutation permuteArguments(ArgumentsSignature signature, ArgumentsSignature formalSignature, RBaseNode callingNode, IntPredicate isVarSuppliedVarargs,
                    IntFunction<String> errorString, RBuiltinDescriptor builtin) {
        // assert Arrays.stream(suppliedNames).allMatch(name -> name == null || !name.isEmpty());

        // Preparations
        int varArgIndex = formalSignature.getVarArgIndex();
        boolean hasVarArgs = varArgIndex != ArgumentsSignature.NO_VARARG;

        // MATCH by exact and partial name
        int[] resultPermutation = new int[formalSignature.getLength()];
        String[] resultSignature = new String[formalSignature.getLength()];
        Arrays.fill(resultPermutation, MatchPermutation.UNMATCHED);
        Arrays.fill(resultSignature, ArgumentsSignature.UNMATCHED);

        // MATCH in two phases: first by exact name (if we have actual: 'x', 'xa' and formal 'xa',
        // then actual 'x' should not steal the position of 'xa'), then by partial
        boolean[] matchedSuppliedArgs = new boolean[signature.getLength()];
        boolean[] formalsMatchedByExactName = new boolean[formalSignature.getLength()];
        for (boolean byExactName : new boolean[]{true, false}) {
            for (int suppliedIndex = 0; suppliedIndex < signature.getLength(); suppliedIndex++) {
                String suppliedName = signature.getName(suppliedIndex);
                boolean wasMatchedByExactName = !byExactName && matchedSuppliedArgs[suppliedIndex];
                if (wasMatchedByExactName || suppliedName == null || suppliedName.isEmpty()) {
                    continue;
                }

                // Search for argument name inside formal arguments
                int formalIndex = findParameterPosition(formalSignature, suppliedName, resultPermutation, suppliedIndex, hasVarArgs, callingNode, varArgIndex, errorString, builtin,
                                formalsMatchedByExactName, byExactName);
                if (formalIndex != MatchPermutation.UNMATCHED) {
                    resultPermutation[formalIndex] = suppliedIndex;
                    resultSignature[formalIndex] = suppliedName;
                    formalsMatchedByExactName[formalIndex] = byExactName;
                    matchedSuppliedArgs[suppliedIndex] = true;
                }
            }
        }

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
                        String suppliedName = signature.getName(suppliedIndex);
                        if (suppliedName == null || suppliedName.isEmpty() || formalSignature.getName(formalIndex).isEmpty()) {
                            // unnamed parameter, match by position
                            break;
                        }
                    }
                }
                resultPermutation[formalIndex] = suppliedIndex;
                resultSignature[formalIndex] = signature.getName(suppliedIndex);

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
            return new MatchPermutation(resultPermutation, ArgumentsSignature.get(resultSignature), varArgsPermutation, ArgumentsSignature.get(namesArray));
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
                        return new MatchPermutation(resultPermutation, ArgumentsSignature.get(resultSignature), null, null);
                    }

                    // one unused argument
                    CompilerDirectives.transferToInterpreter();
                    throw callingNode.error(RError.Message.UNUSED_ARGUMENT, errorString.apply(suppliedIndex));
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
                throw callingNode.error(RError.Message.UNUSED_ARGUMENTS, str);
            }
            return new MatchPermutation(resultPermutation, ArgumentsSignature.get(resultSignature), null, null);
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

    private static <T> int findParameterPosition(ArgumentsSignature formalsSignature, String suppliedName, int[] resultPermutation, int suppliedIndex, boolean hasVarArgs, RBaseNode callingNode,
                    int varArgIndex, IntFunction<String> errorString, RBuiltinDescriptor builtin, boolean[] formalsMatchedByExactName, boolean exactNameMatch) {
        if (exactNameMatch) {
            return findParameterPositionByExactName(formalsSignature, suppliedName, resultPermutation, hasVarArgs, callingNode, builtin);
        } else {
            return findParameterPositionByPartialName(formalsSignature, formalsMatchedByExactName, suppliedName, resultPermutation, suppliedIndex, hasVarArgs, callingNode, varArgIndex, errorString);
        }
    }

    /**
     * Searches for suppliedName inside formalNames and returns its (formal) index.
     *
     * @return The position of the given suppliedName inside the formalNames. Throws errors if the
     *         argument has been matched before
     */
    private static <T> int findParameterPositionByExactName(ArgumentsSignature formalsSignature, String suppliedName, int[] resultPermutation, boolean hasVarArgs,
                    RBaseNode callingNode, RBuiltinDescriptor builtin) {
        assert suppliedName != null && !suppliedName.isEmpty();
        for (int i = 0; i < formalsSignature.getLength(); i++) {
            String formalName = formalsSignature.getName(i);
            if (!formalsSignature.isVarArg(i) && formalName != null) {
                if (formalName.equals(suppliedName)) {
                    if (resultPermutation[i] != MatchPermutation.UNMATCHED) {
                        if (builtin != null && builtin.getKind() == RBuiltinKind.PRIMITIVE && hasVarArgs) {
                            // for primitives, the first argument is matched, and the others are
                            // folded into varargs, for example: x<-1:64; dim(x)<-c(4,4,2,2);
                            // x[1,drop=FALSE,1,drop=TRUE,-1]
                            return MatchPermutation.UNMATCHED;
                        } else {
                            // Has already been matched: Error!
                            throw callingNode.error(RError.Message.FORMAL_MATCHED_MULTIPLE, formalName);
                        }
                    }
                    return i;
                }
            }
        }
        return MatchPermutation.UNMATCHED;
    }

    /**
     * Searches for partial match of suppliedName inside formalNames and returns its (formal) index.
     *
     * @return The position of the given suppliedName inside the formalNames. Throws errors if the
     *         argument has been matched before
     */
    private static <T> int findParameterPositionByPartialName(ArgumentsSignature formalsSignature, boolean[] formalsMatchedByExactName, String suppliedName, int[] resultPermutation, int suppliedIndex,
                    boolean hasVarArgs, RBaseNode callingNode, int varArgIndex, IntFunction<String> errorString) {
        assert suppliedName != null && !suppliedName.isEmpty();
        int found = MatchPermutation.UNMATCHED;
        for (int i = 0; i < formalsSignature.getLength(); i++) {
            if (formalsMatchedByExactName[i]) {
                // was already matched by some exact match
                continue;
            }

            String formalName = formalsSignature.getName(i);
            if (formalName != null) {
                if (formalName.startsWith(suppliedName) && ((varArgIndex != ArgumentsSignature.NO_VARARG && i < varArgIndex) || varArgIndex == ArgumentsSignature.NO_VARARG)) {
                    // partial-match only if the formal argument is positioned before ...
                    if (found >= 0) {
                        throw callingNode.error(RError.Message.ARGUMENT_MATCHES_MULTIPLE, 1 + suppliedIndex);
                    }
                    found = i;
                    if (resultPermutation[found] != MatchPermutation.UNMATCHED) {
                        throw callingNode.error(RError.Message.FORMAL_MATCHED_MULTIPLE, formalName);
                    }
                }
            }
        }
        if (found >= 0 || hasVarArgs) {
            return found;
        }
        throw callingNode.error(RError.Message.UNUSED_ARGUMENT, errorString.apply(suppliedIndex));
    }
}
