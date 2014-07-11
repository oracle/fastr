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

import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.function.ArgumentMatcher.VarArgsAsObjectArrayNode;
import com.oracle.truffle.r.runtime.data.*;

/**
 * <p>
 * This class denotes a list of {@link #getNames()}/{@link #getArguments()} pairs which are in the
 * order of the {@link FormalArguments} of a function. Each argument is either filled with the
 * supplied argument, the default argument or <code>null</code>, if neither is provided.
 * </p>
 * <p>
 * The {@link #executeArray(VirtualFrame, RFunction)} method converts these arguments into a form
 * that can be passed into functions. E.g., wraps arguments into Promises etc.
 * </p>
 *
 * @see #suppliedNames
 */
public class MatchedArgumentsNode extends ArgumentsNode {

    /**
     * {@link EvalPolicy#RAW}<br/>
     * {@link EvalPolicy#PROMISE}<br/>
     * {@link EvalPolicy#FORCED}<br/>
     */
    public enum EvalPolicy {
        /**
         * Arguments are processed as is, {@link RPromise} are only created for certain builins
         * (legacy!)
         */
        RAW,

        /**
         * {@link RPromise} are created for every argument. If function is a builtin, its argument
         * semantics are maintained!
         */
        PROMISE,

        /**
         *
         */
        FORCED;
    }

    /**
     * @see EvalPolicy
     */
    public static final EvalPolicy ARG_EVAL_POLICY = EvalPolicy.RAW;

    /**
     * Holds the list of names for the supplied arguments this {@link MatchedArgumentsNode} was
     * create with
     */
    private final String[] suppliedNames;

    /**
     * @param arguments {@link #getArguments()}
     * @param names {@link #getNames()}
     */
    private MatchedArgumentsNode(RNode[] arguments, String[] names, String[] suppliedNames) {
        super(arguments, names);
        this.suppliedNames = suppliedNames;
    }

    /**
     * @param arguments
     * @param names
     * @param suppliedNames
     * @param src
     * @return A fresh {@link MatchedArgumentsNode}; arguments may contain <code>null</code> iff
     *         there is neither a supplied argument nor a default argument
     */
    public static MatchedArgumentsNode create(RNode[] arguments, String[] names, String[] suppliedNames, SourceSection src) {
        MatchedArgumentsNode matchedArgs = new MatchedArgumentsNode(arguments, names, suppliedNames);
        matchedArgs.assignSourceSection(src);
        return matchedArgs;
    }

    // Mark unusable
    /**
     * Use {@link #executeArray(VirtualFrame, RFunction)} instead!
     */
    @Override
    @Deprecated
    public Object execute(VirtualFrame frame) {
        throw new AssertionError();
    }

    /**
     * Use {@link #executeArray(VirtualFrame, RFunction)} instead!
     */
    @Override
    @Deprecated
    public Object[] executeArray(VirtualFrame frame) {
        throw new AssertionError();
    }

    /**
     * This class does the heavy lifting in inspecting the
     *
     * @param frame
     * @return The wrapped arguments
     */
    @ExplodeLoop
    public Object[] executeArray(VirtualFrame frame, RFunction function) {
        RNode[] wrappedArguments = getWrappedArguments(frame, function, ARG_EVAL_POLICY);

        Object[] evaluatedArguments = new Object[wrappedArguments.length];
        for (int i = 0; i < evaluatedArguments.length; i++) {
            RNode wrappedArg = wrappedArguments[i];
            if (wrappedArg != null) {
                evaluatedArguments[i] = wrappedArg.execute(frame);
            } else {
                evaluatedArguments[i] = RNull.instance;
            }
        }

        return evaluatedArguments;
    }

    public EvaluatedArguments getWrappedArguments(VirtualFrame frame, RFunction function) {
        return new EvaluatedArguments(getWrappedArguments(frame, function, ARG_EVAL_POLICY), suppliedNames);
    }

    private RNode[] getWrappedArguments(@SuppressWarnings("unused") VirtualFrame frame, RFunction function, EvalPolicy type) {
        RNode[] wrappedArguments = new RNode[arguments.length];

        int lix = 0;    // logical index
        for (int fi = 0; fi < arguments.length; fi++) {
            RNode arg = arguments[fi];
            RNode result = null;

// if (type == EOutput.RAW) {
            RRootNode rootNode = null;
            boolean isBuiltin = false;
            if (function == null) {
                isBuiltin = true;
            } else {
                rootNode = (RRootNode) function.getTarget().getRootNode();
                isBuiltin = rootNode instanceof RBuiltinRootNode;
            }

            if (arg == null) {
                result = ConstantNode.create(RMissing.instance);
                lix++;
            } else if (isBuiltin && (type == EvalPolicy.RAW || !((RBuiltinRootNode) rootNode).evaluatesArgs())) {
                RNode argOrPromise = null;
                RBuiltinRootNode builtinRootNode = (RBuiltinRootNode) rootNode;
                if (arg instanceof VarArgsAsObjectArrayNode) {
                    VarArgsAsObjectArrayNode vArgumentNode = (VarArgsAsObjectArrayNode) arg;
                    RNode[] modifiedVArgumentNodes = new RNode[vArgumentNode.elementNodes.length];
                    for (int j = 0; j < vArgumentNode.elementNodes.length; j++) {
                        modifiedVArgumentNodes[j] = checkPromise(builtinRootNode, vArgumentNode.elementNodes[j], lix);
                        lix++;
                    }
                    argOrPromise = new VarArgsAsObjectArrayNode(modifiedVArgumentNodes);
                } else {
                    argOrPromise = checkPromise(builtinRootNode, arg, lix);
                    lix++;
                }
                result = argOrPromise;
            } else {
                result = arg;
                lix++;
            }
// }

            wrappedArguments[fi] = result;
        }

        return wrappedArguments;
    }

    private static RNode checkPromise(RBuiltinRootNode builtinRootNode, RNode argNode, int formalIndex) {
        if (builtinRootNode != null && !builtinRootNode.evaluatesArg(formalIndex)) {
            return PromiseNode.create(argNode.getSourceSection(), new RPromise(argNode));
        } else {
            return argNode;
        }

    }

    /**
     * @return The consolidated list of arguments that should be passed to a function.
     *         <code>null</code> denotes 'no argument specified'
     */
    @Override
    public RNode[] getArguments() {
        return arguments;
    }

    /**
     * @return The nr of arguments there are to be passed into a function (vargs are counted as
     *         <u>one</u>, as they are wrapped into one object!)
     */
    public int getNrOfArgs() {
        return arguments.length;
    }

    /**
     * @return The name for every {@link #arguments}. May NOT contain <code>null</code>
     */
    @Override
    public String[] getNames() {
        return names;
    }
}
