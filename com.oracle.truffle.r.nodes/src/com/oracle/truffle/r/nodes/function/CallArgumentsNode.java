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
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

/**
 * This class denotes a list of {@link #getArguments()} together with their {@link #getNames()}
 * given to a specific function call. The arguments' order is the same as given at the call.<br/>
 * It additionally holds usage hints ({@link #modeChange}, {@link #modeChangeForAll}).
 */
public final class CallArgumentsNode extends ArgumentsNode {

    private static final String[] NO_NAMES = new String[0];

    /**
     * If a supplied argument is a {@link ReadVariableNode} whose {@link Symbol} is "...", this
     * field is set to the index of the symbol. Otherwise it is {@link #NO_VARARG} (
     * {@value #NO_VARARG}).
     */
    private final int varArgsSymbolIndex;

    /**
     * the two flags below are used in cases when we know that either a builtin is not going to
     * modify the arguments which are not meant to be modified (like in the case of binary
     * operators) or that its intention is to actually update the argument (as in the case of
     * replacement forms, such as dim(x)<-1; in these cases the mode change
     * (temporary->non-temporary->shared) does not need to happen, which is what the first flag (
     * {@link #modeChange}) determines, with the second ({@link #modeChangeForAll}) flat telling the
     * runtime if this affects only the first argument (replacement functions) or all arguments
     * (binary operators).
     */
    private final boolean modeChange;

    /**
     * @see #modeChange
     */
    private final boolean modeChangeForAll;

    private CallArgumentsNode(RNode[] arguments, String[] names, int varArgsSymbolIndex, boolean modeChange, boolean modeChangeForAll) {
        super(arguments, names);
        this.varArgsSymbolIndex = varArgsSymbolIndex;
        this.modeChange = modeChange;
        this.modeChangeForAll = modeChangeForAll;
    }

    /**
     * @return {@link #create(boolean, boolean, RNode[], String[])} with <code>null</code> as last
     *         argument
     */
    public static CallArgumentsNode createUnnamed(boolean modeChange, boolean modeChangeForAll, RNode... args) {
        return create(modeChange, modeChangeForAll, args, null);
    }

    /**
     * @param modeChange {@link #modeChange}
     * @param modeChangeForAll {@link #modeChangeForAll}
     * @param args {@link #arguments}; new array gets created. Every {@link RNode} (except
     *            <code>null</code>) gets wrapped into a {@link WrapArgumentNode}.
     * @param names {@link #names}, set directly. If <code>null</code>, {@link #NO_NAMES} is used.
     * @return A fresh {@link CallArgumentsNode}
     */
    public static CallArgumentsNode create(boolean modeChange, boolean modeChangeForAll, RNode[] args, String[] names) {
        // Prepare arguments: wrap in WrapArgumentNode
        RNode[] wrappedArgs = new RNode[args.length];
        int varArgsSymbolIndex = NO_VARARG;
        for (int i = 0; i < wrappedArgs.length; ++i) {
            RNode arg = args[i];
            if (arg == null) {
                wrappedArgs[i] = null;
            } else {
                if (varArgsSymbolIndex == NO_VARARG && arg instanceof ReadVariableNode) {
                    // Check for presence of "..." in the arguments
                    ReadVariableNode rvn = (ReadVariableNode) arg;
                    if (rvn.getSymbol().isVarArg()) {
                        varArgsSymbolIndex = i;
                    }
                }
                wrappedArgs[i] = WrapArgumentNode.create(arg, i == 0 || modeChangeForAll ? modeChange : true);
            }
        }

        // Check names
        String[] resolvedNames = names;
        if (resolvedNames == null) {
            resolvedNames = NO_NAMES;
        }

        // Setup and return
        SourceSection src = Utils.sourceBoundingBox(wrappedArgs);
        CallArgumentsNode callArgs = new CallArgumentsNode(wrappedArgs, resolvedNames, varArgsSymbolIndex, modeChange, modeChangeForAll);
        callArgs.assignSourceSection(src);
        return callArgs;
    }

    @Override
    @Deprecated
    public Object execute(VirtualFrame frame) {
        // Execute has not semantic meaning for CallArgumentsNode
        throw new AssertionError();
    }

    @Override
    @Deprecated
    public Object[] executeArray(VirtualFrame frame) throws UnexpectedResultException {
        // Execute has not semantic meaning for CallArgumentsNode
        throw new AssertionError();
    }

    @ExplodeLoop
    public RArgsValuesAndNames executeFlatten(VirtualFrame frame) {
        if (!containsVarArgsSymbol()) {
            Object[] values = new Object[arguments.length];
            for (int i = 0; i < values.length; i++) {
                values[i] = arguments[i] == null ? null : arguments[i].execute(frame);
            }
            return new RArgsValuesAndNames(values, this.getNames());
        } else {
            Object[] values = new Object[arguments.length];
            String[] newNames = new String[arguments.length];

            int index = 0;
            boolean allNamesNull = true;
            for (int i = 0; i < arguments.length; i++) {
                Object argEvaluated = arguments[i].execute(frame);
                if (argEvaluated instanceof RArgsValuesAndNames) {
                    // variadic argument
                    RArgsValuesAndNames varArgInfo = (RArgsValuesAndNames) argEvaluated;
                    values = Utils.resizeObjectsArray(values, values.length + varArgInfo.length() - 1);
                    newNames = Utils.resizeStringsArray(newNames, newNames.length + varArgInfo.length() - 1);
                    for (int j = 0; j < varArgInfo.length(); j++) {
                        values[index] = varArgInfo.getValues()[j];
                        String newName = varArgInfo.getNames() == null ? null : varArgInfo.getNames()[j];
                        allNamesNull &= newName == null;
                        newNames[index] = newName;
                        index++;
                    }
                } else {
                    values[index] = argEvaluated;
                    String newName = this.getNames()[i];
                    allNamesNull &= newName == null;
                    newNames[index] = newName;
                    index++;
                }
            }

            return new RArgsValuesAndNames(values, allNamesNull ? null : newNames);
        }
    }

    /**
     * @return {@link #containsVarArgsSymbol}
     */
    public boolean containsVarArgsSymbol() {
        return varArgsSymbolIndex != NO_VARARG;
    }

    /**
     * @return The {@link RNode}s of the arguments given to a function call, in the same order. A
     *         single argument being <code>null</code> means 'argument not provided'.
     */
    @Override
    public RNode[] getArguments() {
        return arguments;
    }

    /**
     * @return The names of the {@link #arguments}, in the same order. <code>null</code> means 'no
     *         name given'
     */
    @Override
    public String[] getNames() {
        return names;
    }

    /**
     * @return {@link #modeChange}
     */
    public boolean modeChange() {
        return modeChange;
    }

    /**
     * @return {@link #modeChangeForAll}
     */
    public boolean modeChangeForAll() {
        return modeChangeForAll;
    }
}
