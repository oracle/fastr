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
import com.oracle.truffle.r.nodes.builtin.base.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.RPromise.EvalPolicy;

/**
 * An {@link ArgumentMatcher} knows the {@link #formals} of a specific function and can
 * {@link #match(RFunction, REnvironment, CallArgumentsNode, SourceSection)} supplied arguments to
 * the formal ones.
 */
public class ArgumentMatcher {

    /**
     * The {@link FormalArguments} of a specific function
     */
    private final FormalArguments formals;

    private ArgumentMatcher(FormalArguments formals) {
        this.formals = formals;
    }

    /**
     * @param formals
     * @return A fresh {@link ArgumentMatcher}
     */
    public static ArgumentMatcher create(FormalArguments formals) {
        return new ArgumentMatcher(formals);
    }

    /**
     * Match supplied arguments to formal arguments
     *
     * @param function
     * @param suppliedArgs
     * @param encapsulatingSrc For precise error reporting
     * @return A fresh instance of {@link MatchedArgumentsNode}
     */
    public static MatchedArgumentsNode matchArguments(RFunction function, VirtualFrame frame, CallArgumentsNode suppliedArgs, SourceSection encapsulatingSrc) {
        FormalArguments formals = ((RRootNode) function.getTarget().getRootNode()).getFormalArguments();
        ArgumentMatcher matcher = ArgumentMatcher.create(formals);
        REnvironment env = EnvFunctions.frameToEnvironment(frame);
        return matcher.match(function, env, suppliedArgs, encapsulatingSrc);
    }

    /**
     * This method <strong>DOES NOT</strong> rearrange arguments in any sense, but simply 'converts'
     * {@link CallArgumentsNode} -> {@link MatchedArgumentsNode}. This is needed for some builtins!
     *
     * @param suppliedArgs
     * @return A {@link MatchedArgumentsNode} filled with {@link CallArgumentsNode#getArguments()}/
     *         {@link CallArgumentsNode#getNames()}
     */
    public static UnevaluatedArguments pseudoMatch(CallArgumentsNode suppliedArgs) {
        return new UnevaluatedArguments(suppliedArgs.getArguments(), suppliedArgs.getNames());
    }

    /**
     * Matches the supplied arguments to the {@link #formals} and returns them as consolidated
     * {@link MatchedArgumentsNode}.<br/>
     * <strong>Does not</strong> alter the given {@link CallArgumentsNode}
     *
     * @param function
     * @param suppliedArgs
     * @param encapsulatingSrc For precise error reporting
     * @return A {@link MatchedArgumentsNode} which contains a consolidated list of arguments, in
     *         the order of the {@link FormalArguments}
     */
    public MatchedArgumentsNode match(RFunction function, REnvironment env, CallArgumentsNode suppliedArgs, SourceSection encapsulatingSrc) {
        // Rearrange arguments
        RNode[] resultArgs = permuteArguments(function, suppliedArgs, new VarArgsAsObjectArrayNodeFactory(), encapsulatingSrc);
        RNode[] wrappedArgs = wrapInPromises(resultArgs, function, env, new DefaultPromiseWrapper());
        return MatchedArgumentsNode.create(wrappedArgs, formals.getNames(), suppliedArgs.getNames(), suppliedArgs.getSourceSection());
    }

    /**
     * Match supplied arguments to formal arguments
     *
     * @param function
     * @param suppliedArgs
     * @param encapsulatingSrc For precise error reporting
     * @return A fresh instance of {@link MatchedArgumentsNode}
     */
    public static UnevaluatedArguments matchArgumentsUnevaluated(RFunction function, VirtualFrame frame, CallArgumentsNode suppliedArgs, SourceSection encapsulatingSrc) {
        FormalArguments formals = ((RRootNode) function.getTarget().getRootNode()).getFormalArguments();
        ArgumentMatcher matcher = ArgumentMatcher.create(formals);
        REnvironment env = EnvFunctions.frameToEnvironment(frame);
        return matcher.matchUnevaluated(function, env, suppliedArgs, encapsulatingSrc);
    }

    /**
     * Matches the supplied arguments to the {@link #formals} and returns them as consolidated
     * {@link MatchedArgumentsNode}.<br/>
     * <strong>Does not</strong> alter the given {@link CallArgumentsNode}
     *
     * @param function
     * @param suppliedArgs
     * @param encapsulatingSrc For precise error reporting
     * @return A {@link MatchedArgumentsNode} which contains a consolidated list of arguments, in
     *         the order of the {@link FormalArguments}
     */
    public UnevaluatedArguments matchUnevaluated(RFunction function, REnvironment env, CallArgumentsNode suppliedArgs, SourceSection encapsulatingSrc) {
        // Rearrange arguments
        RNode[] resultArgs = permuteArguments(function, suppliedArgs, new VarArgsAsObjectArrayNodeFactory(), encapsulatingSrc);
        RNode[] wrappedArgs = wrapInPromises(resultArgs, function, env, new BuiltinInitPromiseWrapper());
        return new UnevaluatedArguments(wrappedArgs, suppliedArgs.getNames());
    }

    /**
     * @param function
     * @param supplied {@link CallArgumentsNode}
     * @param listFactory
     * @param encapsulatingSrc For precise error reporting
     * @return A consolidated list of arguments in the order of the {@link #formals}. If there is
     *         neither a supplied arg nor a default one, the argument is <code>null</code>. Handles
     *         named args and varargs, but does not insert default values! The resulting
     *         <code>RNode[]</code> thus may contain <code>null</code>s.
     */
    protected RNode[] permuteArguments(RFunction function, CallArgumentsNode supplied, VarArgsFactory<RNode> listFactory, SourceSection encapsulatingSrc) {
        RNode[] suppliedArgs = supplied.getArguments();
        String[] suppliedNames = supplied.getNames();
        String[] formalNames = formals.getNames();

        // Preparations
        int varArgIndex = formals.getVarArgIndex();
        boolean hasVarArgs = varArgIndex != FormalArguments.NO_VARARG;

        // Error check
        RRootNode rootNode = (RRootNode) function.getTarget().getRootNode();
        final boolean isBuiltin = rootNode instanceof RBuiltinRootNode;

        // Error: Unused argument
        if (!isBuiltin && !hasVarArgs && suppliedArgs.length > rootNode.getParameterCount()) {
            RNode unusedArgNode = suppliedArgs[rootNode.getParameterCount()];
            throw RError.error(encapsulatingSrc, RError.Message.UNUSED_ARGUMENT, unusedArgNode.getSourceSection().getCode());
        }

        // Start by finding a matching arguments by name
        RNode[] resultArgs = new RNode[hasVarArgs ? formalNames.length : Math.max(formalNames.length, suppliedArgs.length)];
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
            int fi = findParameterPosition(formalNames, suppliedNames[si], matchedFormalArgs, si, hasVarArgs, suppliedArgs[si], encapsulatingSrc);
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
            RNode[] varArgsArray = new RNode[varArgCount];
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
     * Searches for suppliedName inside formalNames and returns its (formal) index
     *
     * @param formalNames
     * @param suppliedName
     * @param matchedSuppliedArgs
     * @param suppliedIndex
     * @param hasVarArgs
     * @param debugArgNode
     * @param encapsulatingSrc
     * @return The position of the given suppliedName inside the formalNames. Throws errors if the
     *         argument has been matched before
     */
    private static int findParameterPosition(String[] formalNames, String suppliedName, BitSet matchedSuppliedArgs, int suppliedIndex, boolean hasVarArgs, RNode debugArgNode,
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
        throw RError.error(encapsulatingSrc, RError.Message.UNUSED_ARGUMENT, debugArgNode != null ? debugArgNode.getSourceSection().getCode() : suppliedName);
    }

    /**
     * @param arguments
     * @param function
     * @return A list of {@link RNode} wrapped in {@link PromiseNode}s
     */
    private RNode[] wrapInPromises(RNode[] arguments, RFunction function, REnvironment env, PromiseWrapper promiseWrapper) {
        RNode[] defaultArgs = formals.getDefaultArgs();

        // Check whether this is a builtin
        RootNode rootNode = function.getTarget().getRootNode();
        RBuiltinRootNode builtinRootNode = null;
        if (rootNode instanceof RBuiltinRootNode) {
            builtinRootNode = (RBuiltinRootNode) rootNode;
        }

        // Insert promises here!
        int logicalIndex = 0;    // also counts arguments wrapped in vararg
        for (int fi = 0; fi < arguments.length; fi++) {
            RNode arg = arguments[fi];
            if (arg == null) {
                arg = ConstantNode.create(RMissing.instance);
            } else if (arg instanceof VarArgsAsObjectArrayNode) {
                VarArgsAsObjectArrayNode varArgs = (VarArgsAsObjectArrayNode) arg;
                RNode[] modifiedVArgumentNodes = new RNode[varArgs.elementNodes.length];
                for (int j = 0; j < varArgs.elementNodes.length; j++) {
                    modifiedVArgumentNodes[j] = promiseWrapper.wrap(builtinRootNode, env, varArgs.elementNodes[j], null, logicalIndex);
                    logicalIndex++;
                }
                arguments[fi] = new VarArgsAsObjectArrayNode(modifiedVArgumentNodes);
                continue;
            }

            RNode defaultArg = fi < defaultArgs.length ? defaultArgs[fi] : null;
            arguments[fi] = promiseWrapper.wrap(builtinRootNode, env, arg, defaultArg, logicalIndex);
            logicalIndex++;
        }
        return arguments;
    }

    private interface PromiseWrapper {
        /**
         * @param builtinRootNode
         * @param suppliedArg
         * @param defaultValue
         * @param logicalIndex
         * @return TODO Gero, add comment!
         */
        RNode wrap(RBuiltinRootNode builtinRootNode, REnvironment env, RNode suppliedArg, RNode defaultValue, int logicalIndex);
    }

    private static class DefaultPromiseWrapper implements PromiseWrapper {
        /**
         * TODO Gero, add comment!
         */
        public RNode wrap(RBuiltinRootNode builtinRootNode, REnvironment env, RNode suppliedArg, RNode defaultValue, int logicalIndex) {
            // This is for actual calls. However, if the arguments are meant for a builtin, we have
            // to consider whether they should be forced or not!
            // TODO Strict!
            EvalPolicy policy = builtinRootNode != null && builtinRootNode.evaluatesArg(logicalIndex) ? EvalPolicy.STRICT : EvalPolicy.STRICT;  // EvalPolicy.PROMISED;
            RNode defaultValueNode = defaultValue == null ? ConstantNode.create(RMissing.instance) : defaultValue;
            return PromiseNode.create(suppliedArg.getSourceSection(), new RPromise(suppliedArg, defaultValueNode, env, policy));
        }
    }

    private static class BuiltinInitPromiseWrapper implements PromiseWrapper {
        /**
         * TODO Gero, add comment!
         */
        public RNode wrap(RBuiltinRootNode builtinRootNode, REnvironment env, RNode suppliedArg, RNode defaultValue, int logicalIndex) {
            // This is for the initialization of builtins
            assert builtinRootNode != null;
            RNode defaultValueNode = defaultValue == null ? ConstantNode.create(RMissing.instance) : defaultValue;
            if (!builtinRootNode.evaluatesArg(logicalIndex)) {
                return PromiseNode.create(suppliedArg.getSourceSection(), new RPromise(suppliedArg, defaultValueNode, env, EvalPolicy.PROMISED));
            } else {
                return PromiseNode.create(suppliedArg.getSourceSection(), new RPromise(suppliedArg, defaultValueNode, env, EvalPolicy.RAW));
            }
        }
    }

    public interface VarArgsFactory<T> {
        T makeList(T[] elements, String[] names);
    }

    public static final class VarArgsAsListFactory implements VarArgsFactory<Object> {
        public Object makeList(final Object[] elements, final String[] names) {
            RList argList = RDataFactory.createList(elements);
            if (names != null) {
                argList.setNames(RDataFactory.createStringVector(names, true));
            }
            return argList;
        }
    }

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

    public static final class VarArgsAsListNodeFactory implements VarArgsFactory<RNode> {
        public RNode makeList(final RNode[] elements, final String[] names) {
            if (elements.length > 1) {
                return new VarArgsAsListNode(elements, names);
            } else if (elements.length == 1) {
                return elements[0];
            } else {
                return ConstantNode.create(RMissing.instance);
            }
        }
    }

    public abstract static class VarArgsNode extends RNode {
        @Children protected final RNode[] elementNodes;

        protected VarArgsNode(RNode[] elements) {
            elementNodes = elements;
        }

        public final RNode[] getArgumentNodes() {
            return elementNodes;
        }
    }

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

    public static final class VarArgsAsObjectArrayNodeFactory implements VarArgsFactory<RNode> {
        public RNode makeList(final RNode[] elements, final String[] names) {
            if (elements.length > 1) {
                return new VarArgsAsObjectArrayNode(elements);
            } else if (elements.length == 1) {
                return elements[0];
            } else {
                return ConstantNode.create(RMissing.instance);
            }
        }
    }

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

    /**
     * @return {@link #formals}
     */
    public FormalArguments getFormals() {
        return formals;
    }
}
