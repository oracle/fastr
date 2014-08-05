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

import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.RPromise.EvalPolicy;
import com.oracle.truffle.r.runtime.data.RPromise.PromiseType;
import com.oracle.truffle.r.runtime.data.RPromise.RPromiseFactory;

/**
 * <p>
 * An {@link ArgumentMatcher} knows the {@link FormalArguments} of a specific function and can
 * {@link #matchArguments(VirtualFrame, RFunction, CallArgumentsNode, SourceSection)} supplied
 * arguments to the formal ones.
 * </p>
 * The other match functions are used for special cases, where builtins make it necessary to
 * re-match parameters, e.g.:
 * {@link #matchArgumentsEvaluated(VirtualFrame, RFunction, EvaluatedArguments, SourceSection)} for
 * 'UseMethod' and
 * {@link #matchArgumentsInlined(VirtualFrame, RFunction, CallArgumentsNode, SourceSection)} for
 * builtins which are implemented in Java ( @see {@link RBuiltinNode#inline(InlinedArguments)}
 *
 */
public class ArgumentMatcher {
    /**
     * Match arguments supplied for a specific function call to the formal arguments and wraps them
     * in {@link PromiseNode}s. Used for calls to all functions parsed from R code
     *
     * @param frame carrier for error reporting
     * @param function The function which is to be called
     * @param suppliedArgs The arguments supplied to the call
     * @param encapsulatingSrc The source code encapsulating the arguments, for debugging purposes
     *
     * @return A fresh {@link MatchedArgumentsNode} containing the arguments in correct order and
     *         wrapped in {@link PromiseNode}s
     * @see #matchNodes(VirtualFrame, RFunction, RNode[], String[], SourceSection, boolean)
     */
    public static MatchedArgumentsNode matchArguments(VirtualFrame frame, RFunction function, CallArgumentsNode suppliedArgs, SourceSection encapsulatingSrc) {

        // "Evaluate" supplied arguments
        RNode[] suppliedEvaled = new RNode[suppliedArgs.getArguments().length];
        for (int i = 0; i < suppliedEvaled.length; i++) {
            RNode arg = suppliedArgs.getArguments()[i];

            // Check for 'missing' arguments
            {
                RNode rvnArg = arg;

                // Eventually unfold WrapArgumentNode
                if (rvnArg instanceof WrapArgumentNode) {
                    rvnArg = ((WrapArgumentNode) rvnArg).getOperand();
                }

                // ReadVariableNode denotes a symbol
                if (rvnArg instanceof ReadVariableNode) {
                    ReadVariableNode rvn = (ReadVariableNode) rvnArg;
                    Symbol symbol = rvn.getSymbol();
                    if (RMissingHelper.isMissingArgument(frame, symbol)) {
                        suppliedEvaled[i] = null;
                        continue;
                    }
                }
            }

            suppliedEvaled[i] = arg;
        }

        FormalArguments formals = ((RRootNode) function.getTarget().getRootNode()).getFormalArguments();
        RNode[] wrappedArgs = matchNodes(frame, function, suppliedEvaled, suppliedArgs.getNames(), encapsulatingSrc, false);
        return MatchedArgumentsNode.create(wrappedArgs, formals.getNames(), suppliedArgs.getNames(), suppliedArgs.getSourceSection());
    }

    /**
     * Match arguments supplied for a specific function call to the formal arguments and wraps them
     * in special {@link PromiseNode}s. Used for calls to builtins which are built into FastR and
     * thus are implemented in Java
     *
     * @param frame carrier for error reporting
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
     * @param frame carrier for error reporting
     * @param function The 'Method' which is going to be 'Use'd
     * @param evaluatedArgs The arguments which are already in evaluated form (as they are directly
     *            taken from the stack)
     * @param encapsulatingSrc The source code encapsulating the arguments, for debugging purposes
     *
     * @return A Fresh {@link EvaluatedArguments} containing the arguments rearranged and stuffed
     *         with default values (in the form of {@link RPromise}s where needed)
     */
    public static EvaluatedArguments matchArgumentsEvaluated(VirtualFrame frame, RFunction function, EvaluatedArguments evaluatedArgs, SourceSection encapsulatingSrc) {
        FormalArguments formals = ((RRootNode) function.getTarget().getRootNode()).getFormalArguments();
        Object[] evaledArgs = permuteArguments(frame, function, evaluatedArgs.getEvaluatedArgs(), evaluatedArgs.getNames(), formals, new VarArgsAsObjectArrayFactory(), new ObjectArrayFactory(),
                        encapsulatingSrc);

        // Replace RMissing with default value!
        RNode[] defaultArgs = formals.getDefaultArgs();
        for (int i = 0; i < defaultArgs.length; i++) {
            Object evaledArg = evaledArgs[i];
            if (evaledArg == null) {
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
            }
        }
        return new EvaluatedArguments(evaledArgs, formals.getNames());
    }

    /**
     * Matches the supplied arguments to the formal ones and returns them as consolidated
     * {@link MatchedArgumentsNode}. Handles named args and varargs.<br/>
     * <strong>Does not</strong> alter the given {@link CallArgumentsNode}
     *
     * @param frame carrier for error reporting
     * @param function The function which is to be called
     * @param suppliedArgs The arguments supplied to the call
     * @param suppliedNames The names for the arguments supplied to the call
     * @param encapsulatingSrc The source code encapsulating the arguments, for debugging purposes
     * @param isForInlinedBuilin Whether the arguments are passed into an inlined builtin and need
     *            special treatment
     *
     * @return A list of {@link RNode}s which consist of the given arguments in the correct order
     *         and wrapped into the proper {@link PromiseNode}s
     * @see #permuteArguments(VirtualFrame, RFunction, Object[], String[], FormalArguments,
     *      VarArgsFactory, ArrayFactory, SourceSection)
     */
    private static RNode[] matchNodes(VirtualFrame frame, RFunction function, RNode[] suppliedArgs, String[] suppliedNames, SourceSection encapsulatingSrc, boolean isForInlinedBuilin) {
        FormalArguments formals = ((RRootNode) function.getTarget().getRootNode()).getFormalArguments();

        // Rearrange arguments
        RNode[] resultArgs = permuteArguments(frame, function, suppliedArgs, suppliedNames, formals, new VarArgsAsObjectArrayNodeFactory(), new RNodeArrayFactory(), encapsulatingSrc);
        PromiseWrapper wrapper = isForInlinedBuilin ? new BuiltinInitPromiseWrapper() : new DefaultPromiseWrapper();
        return wrapInPromises(function, resultArgs, formals, wrapper);
    }

    /**
     * This method does the heavy lifting of re-arranging arguments by their names and position,
     * also handling varargs.
     *
     * @param frame carrier for error reporting
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
    protected static <T> T[] permuteArguments(VirtualFrame frame, RFunction function, T[] suppliedArgs, String[] suppliedNames, FormalArguments formals, VarArgsFactory<T> listFactory,
                    ArrayFactory<T> arrFactory, SourceSection encapsulatingSrc) {
        String[] formalNames = formals.getNames();

        // Preparations
        int varArgIndex = formals.getVarArgIndex();
        boolean hasVarArgs = varArgIndex != FormalArguments.NO_VARARG;

        // Error check: Unused argument
        RRootNode rootNode = (RRootNode) function.getTarget().getRootNode();
        final boolean isBuiltin = rootNode instanceof RBuiltinRootNode;
        if (!isBuiltin && !hasVarArgs && suppliedArgs.length > rootNode.getParameterCount()) {
            RNode unusedArgNode = (RNode) suppliedArgs[rootNode.getParameterCount()];
            throw RError.error(frame, encapsulatingSrc, RError.Message.UNUSED_ARGUMENT, unusedArgNode.getSourceSection().getCode());
        }

        // Start by finding a matching arguments by name
        T[] resultArgs = arrFactory.newArray(hasVarArgs ? formalNames.length : Math.max(formalNames.length, suppliedArgs.length));
        BitSet matchedSuppliedNames = new BitSet(suppliedNames.length);
        BitSet matchedFormalArgs = new BitSet(formalNames.length);
        int unmatchedNameCount = 0; // The nr of formal arguments that have no supplied argument
        int varArgMatches = 0;  // The nr of arguments that matches the vararg "..."
        // si = suppliedIndex, fi = formalIndex
        for (int si = 0; si < suppliedNames.length; si++) {
            if (suppliedNames[si] == null) {
                continue;
            }

            // Search for argument name inside formal arguments
            int fi = findParameterPosition(frame, formalNames, suppliedNames[si], matchedFormalArgs, si, hasVarArgs, suppliedArgs[si], encapsulatingSrc);
            if (fi >= 0) {
                // Supplied argument is matched!
                if (fi >= varArgIndex) {
                    // This argument matches to "..."
                    ++varArgMatches;
                }
                resultArgs[fi] = suppliedArgs[si];
                matchedSuppliedNames.set(si);
            } else {
                // Formal argument's name was not found in supplied list
                unmatchedNameCount++;
            }
        }

        /*
         * Next, check for supplied arguments for vararg: To find the remaining arguments that can
         * match to ... we should subtract sum of varArgIndex and number of variable arguments
         * already matched from total number of arguments.
         */
        int varArgCount = suppliedArgs.length - (varArgIndex + varArgMatches);
        if (varArgIndex >= 0 && varArgCount >= 0) {
            // Create new nodes and names for vararg
            T[] varArgsArray = arrFactory.newArray(varArgCount);
            String[] namesArray = null;
            if (unmatchedNameCount != 0) {
                namesArray = new String[varArgCount];
            }

            // Every supplied argument that has not been matched or is longer then names list:
            // Add to vararg!
            int pos = 0;
            for (int i = varArgIndex; i < suppliedArgs.length; i++) {
                if (i > suppliedNames.length || !matchedSuppliedNames.get(i)) {
                    varArgsArray[pos] = suppliedArgs[i];
                    if (namesArray != null) {
                        namesArray[pos] = suppliedNames[i] != null ? suppliedNames[i] : "";
                    }
                    pos++;
                }
            }
            resultArgs[varArgIndex] = listFactory.makeList(varArgsArray, namesArray);
        }

        // Now fill arguments that have not been set 'by name' by order of their appearance
        int cursor = 0;
        for (int fi = 0; fi < resultArgs.length && (!hasVarArgs || fi < varArgIndex); fi++) {
            if (resultArgs[fi] == null) {
                while (cursor < suppliedNames.length && matchedSuppliedNames.get(cursor)) {
                    cursor++;
                }
                if (cursor < suppliedArgs.length) {
                    resultArgs[fi] = suppliedArgs[cursor++];
                }
            }
        }

        return resultArgs;
    }

    /**
     * Searches for suppliedName inside formalNames and returns its (formal) index.
     *
     * @param frame carrier for error reporting
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
    private static <T> int findParameterPosition(VirtualFrame frame, String[] formalNames, String suppliedName, BitSet matchedSuppliedArgs, int suppliedIndex, boolean hasVarArgs, T debugArgNode,
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
                    throw RError.error(frame, encapsulatingSrc, RError.Message.FORMAL_MATCHED_MULTIPLE, formalName);
                }
                matchedSuppliedArgs.set(found);
                break;
            } else if (formalName.startsWith(suppliedName)) {
                if (found >= 0) {
                    throw RError.error(frame, encapsulatingSrc, RError.Message.ARGUMENT_MATCHES_MULTIPLE, 1 + suppliedIndex);
                }
                found = i;
                if (matchedSuppliedArgs.get(found)) {
                    throw RError.error(frame, encapsulatingSrc, RError.Message.FORMAL_MATCHED_MULTIPLE, formalName);
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
        throw RError.error(frame, encapsulatingSrc, RError.Message.UNUSED_ARGUMENT, debugSrc);
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
                arguments[fi] = new VarArgsAsObjectArrayNode(modifiedVArgumentNodes);
                continue;
            }

            // Normal argument: just wrap in promise
            RNode defaultArg = fi < defaultArgs.length ? defaultArgs[fi] : null;
            arguments[fi] = wrap(promiseWrapper, function, builtinRootNode, envProvider, arg, defaultArg, logicalIndex);
            logicalIndex++;
        }
        return arguments;
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
    }

    /**
     * {@link ArrayFactory} implementation for {@link RNode}.
     */
    private static class RNodeArrayFactory implements ArrayFactory<RNode> {
        public RNode[] newArray(int length) {
            return new RNode[length];
        }
    }

    /**
     * {@link ArrayFactory} implementation for {@link Object}.
     */
    private static class ObjectArrayFactory implements ArrayFactory<Object> {
        public Object[] newArray(int length) {
            return new Object[length];
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
            // TODO Strict!
            return builtinRootNode != null && builtinRootNode.evaluatesArg(logicalIndex) ? EvalPolicy.STRICT : function.getUsePromises() ? EvalPolicy.PROMISED : EvalPolicy.STRICT;  // EvalPolicy.PROMISED;
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
            return !builtinRootNode.evaluatesArg(logicalIndex) ? EvalPolicy.PROMISED : EvalPolicy.RAW;
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
     * {@link VarArgsFactory} implementation that returns varargs as {@link RList}.
     */
    public static final class VarArgsAsListFactory implements VarArgsFactory<Object> {
        public Object makeList(final Object[] elements, final String[] names) {
            RList argList = RDataFactory.createList(elements);
            if (names != null) {
                argList.setNames(RDataFactory.createStringVector(names, true));
            }
            return argList;
        }
    }

    /**
     * {@link VarArgsFactory} implementation that returns varargs as <code>Object[]</code>.
     */
    public static final class VarArgsAsObjectArrayFactory implements VarArgsFactory<Object> {
        public Object makeList(final Object[] elements, final String[] names) {
            if (elements.length > 1) {
                return elements;
            } else if (elements.length == 1) {
                return elements[0];
            } else {
                return RMissing.instance;
            }
        }
    }

    /**
     * {@link VarArgsFactory} implementation that returns varargs as {@link VarArgsAsListNode}.
     */
    public static final class VarArgsAsListNodeFactory implements VarArgsFactory<RNode> {
        public RNode makeList(final RNode[] elements, final String[] names) {
            if (elements.length > 1 || elements.length == 0) {
                return new VarArgsAsListNode(elements, names);
            } else if (elements.length == 1) {
                return elements[0];
            } else {
                return null;    // ConstantNode.create(RMissing.instance);
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
     * A {@link RNode} that encapsulates a list of varargs with names (as {@link RNode}).
     */
    public static final class VarArgsAsListNode extends VarArgsNode {
        private final String[] names;

        private VarArgsAsListNode(RNode[] elements, String[] names) {
            super(elements);
            this.names = names;
        }

        @Override
        public RList execute(VirtualFrame frame) {
            Object[] evaluatedElements = new Object[elementNodes.length];
            if (elementNodes.length > 0) {
                executeElementNodes(frame, elementNodes, evaluatedElements);
            }
            RList argList = RDataFactory.createList(evaluatedElements);
            if (names != null) {
                argList.setNames(RDataFactory.createStringVector(names, true));
            }
            return argList;
        }
    }

    /**
     * {@link VarArgsFactory} implementation that returns varargs as
     * {@link VarArgsAsObjectArrayNode}.
     */
    public static final class VarArgsAsObjectArrayNodeFactory implements VarArgsFactory<RNode> {
        public RNode makeList(final RNode[] elements, final String[] names) {
            if (elements.length > 1) {
                return new VarArgsAsObjectArrayNode(elements);
            } else if (elements.length == 1) {
                return elements[0];
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
        public VarArgsAsObjectArrayNode(RNode[] elements) {
            super(elements);
        }

        @Override
        public Object[] execute(VirtualFrame frame) {
            Object[] evaluatedElements = new Object[elementNodes.length];
            if (elementNodes.length > 0) {
                executeElementNodes(frame, elementNodes, evaluatedElements);
            }
            return evaluatedElements;
        }
    }

    @ExplodeLoop
    protected static void executeElementNodes(VirtualFrame frame, RNode[] elementNodes, Object[] evaluatedElements) {
        for (int i = 0; i < elementNodes.length; i++) {
            evaluatedElements[i] = elementNodes[i].execute(frame);
        }
    }
}
