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
import com.oracle.truffle.r.nodes.function.ArgumentMatcher.VarArgsAsObjectArrayNode;
import com.oracle.truffle.r.runtime.*;

/**
 * <p>
 * This class denotes a list of {@link #getNames()}/{@link #getArguments()} pairs which are in the
 * order of the {@link FormalArguments} of a function. Each argument is either filled with the
 * supplied argument, the default argument or <code>null</code>, if neither is provided.
 * </p>
 * <p>
 * The {@link #executeArray(VirtualFrame)} method executes the argument nodes and converts them into
 * a form that can be passed into functions.
 * </p>
 *
 * @see #suppliedNames
 */
public final class MatchedArgumentsNode extends ArgumentsNode {

    /**
     * Holds the list of names for the supplied arguments this {@link MatchedArgumentsNode} was
     * create with. Needed for combine!
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
     * @return ad
     */
    public static MatchedArgumentsNode createUnnamed(RNode[] arguments) {
        String[] names = new String[arguments.length];
        for (int i = 0; i < names.length; i++) {
            names[i] = null;
        }
        SourceSection src = Utils.sourceBoundingBox(arguments);
        return create(arguments, names, names, src);
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
     * Use {@link #executeArray(VirtualFrame)} instead!
     */
    @Override
    @Deprecated
    public Object execute(VirtualFrame frame) {
        throw new AssertionError();
    }

    @Override
    public Object[] executeArray(VirtualFrame frame) {
        return doExecuteArray(frame);
    }

    /**
     * This method converts the list of arguments this list represents into an <code>Object[]</code>
     * which then can be passed into {@link RArguments} and used for a function call.
     *
     * @param frame
     * @return The <code>Object[]</code> containing the values of the arguments this class
     *         represents
     */
    @ExplodeLoop
    private Object[] doExecuteArray(VirtualFrame frame) {
        Object[] result = new Object[arguments.length];
        for (int i = 0; i < arguments.length; i++) {
            RNode arg = arguments[i];
            if (arg instanceof VarArgsAsObjectArrayNode) {
                // Unfold varargs into an Object[]
                VarArgsAsObjectArrayNode varArgs = (VarArgsAsObjectArrayNode) arg;
                Object[] newVarArgs = new Object[varArgs.elementNodes.length];
                for (int vi = 0; vi < newVarArgs.length; vi++) {
                    RNode varArg = varArgs.elementNodes[vi];
                    newVarArgs[vi] = varArg.execute(frame);
                }
                result[i] = new VarArgsContainer(newVarArgs, varArgs.getNames());
                continue;
            }

            result[i] = arg.execute(frame);
        }
        return result;
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

    /**
     * @return The names of the arguments when they were supplied to a function call, in that old
     *         order. 'No name defined' is denoted by <code>null</code>!
     */
    public String[] getSuppliedNames() {
        return suppliedNames;
    }
}
