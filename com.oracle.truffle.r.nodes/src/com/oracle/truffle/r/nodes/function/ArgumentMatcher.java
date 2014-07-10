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

/**
 * An {@link ArgumentMatcher} knows the {@link #formals} of a specific function and can
 * {@link #match(RFunction, CallArgumentsNode, SourceSection)} supplied arguments to the formal
 * ones.
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
    public static MatchedArgumentsNode matchArguments(RFunction function, CallArgumentsNode suppliedArgs, SourceSection encapsulatingSrc) {
        FormalArguments formals = ((RRootNode) function.getTarget().getRootNode()).getFormalArguments();
        ArgumentMatcher matcher = ArgumentMatcher.create(formals);
        MatchedArgumentsNode matchedArgs = matcher.match(function, suppliedArgs, encapsulatingSrc);
        return matchedArgs;
    }

    /**
     * This method <strong>DOES NOT</strong> rearrange arguments in any sense, but simply 'converts'
     * {@link CallArgumentsNode} -> {@link MatchedArgumentsNode}. This is needed for some builtins!
     *
     * @param suppliedArgs
     * @return A {@link MatchedArgumentsNode} filled with {@link CallArgumentsNode#getArguments()}/
     *         {@link CallArgumentsNode#getNames()}
     */
    public static MatchedArgumentsNode pseudoMatch(CallArgumentsNode suppliedArgs) {
        //
        return MatchedArgumentsNode.create(suppliedArgs.getArguments(), suppliedArgs.getNames(), suppliedArgs.getSourceSection());
    }

    /**
     * Matches the supplied arguments to the {@link #formals} and returns them as consolidated list.<br/>
     * <strong>Does not</strong> alter the given {@link CallArgumentsNode}
     *
     * @param function
     * @param suppliedArgs
     * @param encapsulatingSrc For precise error reporting
     * @return A {@link MatchedArgumentsNode} which contains a consolidated list of arguments, in
     *         the order of the {@link FormalArguments}
     */
    public MatchedArgumentsNode match(RFunction function, CallArgumentsNode suppliedArgs, SourceSection encapsulatingSrc) {
        if (suppliedArgs.getNameCount() == 0) {
            // If there are no names used: Create new arrays and we're done
            RNode[] args = Arrays.copyOf(suppliedArgs.getArguments(), suppliedArgs.getArguments().length);
            String[] names = Arrays.copyOf(suppliedArgs.getNames(), suppliedArgs.getNames().length);
            return MatchedArgumentsNode.create(args, names, suppliedArgs.getSourceSection());
        }

        // Rearrange arguments
        RNode[] resultArgs = permuteArguments(function, suppliedArgs, new VarArgsAsObjectArrayNodeFactory(), encapsulatingSrc);
        return MatchedArgumentsNode.create(resultArgs, formals.getNames(), suppliedArgs.getSourceSection());
    }

    /**
     * @param function
     * @param supplied {@link CallArgumentsNode}
     * @param listFactory
     * @param encapsulatingSrc For precise error reporting
     * @return A consolidated list of arguments in the order of the {@link #formals}. If there is
     *         neither a supplied arg nor a default one, the argument is <code>null</code>. Handles
     *         named args and varargs.
     */
    protected RNode[] permuteArguments(RFunction function, CallArgumentsNode supplied, VarArgsFactory<RNode> listFactory, SourceSection encapsulatingSrc) {
        RNode[] suppliedArgs = supplied.getArguments();
        String[] suppliedNames = supplied.getNames();
        String[] formalNames = formals.getNames();
        RNode[] defaultArgs = formals.getDefaultArgs();

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

        // TODO Is this correct???
        // Now fill arguments that have not been set 'by name' by order of their appearance
        // (or default values!)
        int cursor = 0;
        for (int fi = 0; fi < resultArgs.length && (!hasVarArgs || fi < varArgIndex); fi++) {
            if (resultArgs[fi] == null) {
                while (cursor < suppliedNames.length && matchedSuppliedNames.get(cursor)) {
                    cursor++;
                }
                if (cursor < suppliedArgs.length) {
                    resultArgs[fi] = getArg(suppliedArgs[cursor++], defaultArgs[fi]);
                } else {
                    // If argument is not set and there are no more arguments supplied:
                    // Fill up with default values!
                    resultArgs[fi] = defaultArgs[fi];
                }
            }
        }
        return resultArgs;
    }

    /**
     * @param suppliedArg
     * @param defaultArg
     * @return If suppliedArg == <code>null</code> it's superseded by defaultArg
     */
    private static RNode getArg(RNode suppliedArg, RNode defaultArg) {
        return suppliedArg != null ? suppliedArg : defaultArg;
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

// private CallArgumentsNode permuteArguments(RFunction function, CallArgumentsNode arguments,
// Object[] actualNames, SourceSection encapsulatingSrc) {
// RRootNode rootNode = (RRootNode) function.getTarget().getRootNode();
// final boolean isBuiltin = rootNode instanceof RBuiltinRootNode;
// final boolean hasVarArgs = formals.hasVarArgs();
//
// // Error: Unused argument
// if (!isBuiltin && !hasVarArgs && arguments.getArguments().length > rootNode.getParameterCount())
// {
// RNode unusedArgNode = arguments.getArguments()[rootNode.getParameterCount()];
// throw RError.error(encapsulatingSrc, RError.Message.UNUSED_ARGUMENT,
// unusedArgNode.getSourceSection().getCode());
// }
//
// // Handle named args and varargs
// RNode[] argumentNodes = arguments.getArguments();
// RNode[] origArgumentNodes = argumentNodes;
// if (arguments.getNameCount() != 0 || hasVarArgs) {
// RNode[] permuted = permuteArguments(arguments, new VarArgsAsObjectArrayNodeFactory(),
// encapsulatingSrc);
// if (!isBuiltin) {
// for (int i = 0; i < permuted.length; i++) {
// if (permuted[i] == null) {
// permuted[i] = ConstantNode.create(RMissing.instance);
// }
// }
// }
// argumentNodes = permuted;
// }
// /*
// * This is a temporary fix to create promises just for builtin functions that do not
// * evaluate their arguments, e.g. expression, eval. We have do the check after permutation
// * to get the correct index position. Unfortunately, ... args have been swept up into an
// * array, so it's a bit trickier.
// */
// if (isBuiltin && !((RBuiltinRootNode) rootNode).evaluatesArgs()) {
// RBuiltinRootNode builtinRootNode = (RBuiltinRootNode) rootNode;
// RNode[] modifiedArgs = new RNode[argumentNodes.length];
// int lix = 0; // logical index position
// for (int i = 0; i < argumentNodes.length; i++) {
// RNode argumentNode = argumentNodes[i];
// if (argumentNode instanceof VarArgsAsObjectArrayNode) {
// VarArgsAsObjectArrayNode vArgumentNode = (VarArgsAsObjectArrayNode) argumentNode;
// RNode[] modifiedVArgumentNodes = new RNode[vArgumentNode.elementNodes.length];
// for (int j = 0; j < vArgumentNode.elementNodes.length; j++) {
// modifiedVArgumentNodes[j] = checkPromise(builtinRootNode, vArgumentNode.elementNodes[j], lix);
// lix++;
// }
// modifiedArgs[i] = new VarArgsAsObjectArrayNode(modifiedVArgumentNodes);
// } else {
// modifiedArgs[i] = checkPromise(builtinRootNode, argumentNode, lix);
// lix++;
// }
// }
// argumentNodes = modifiedArgs;
// }
// return origArgumentNodes == argumentNodes ? arguments :
// CallArgumentsNode.create(arguments.modeChange(), arguments.modeChangeForAll(), argumentNodes,
// arguments.getNames());
// }
//
// private static RNode checkPromise(RBuiltinRootNode builtinRootNode, RNode argNode, int lix) {
// if (!builtinRootNode.evaluatesArg(lix)) {
// return PromiseNode.create(argNode.getSourceSection(), new RLanguageRep(argNode));
// } else {
// return argNode;
// }
//
// }

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

    private static final class VarArgsAsListNode extends VarArgsNode {
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

    private static final class VarArgsAsObjectArrayNode extends VarArgsNode {
        protected VarArgsAsObjectArrayNode(RNode[] elements) {
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
