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
import com.oracle.truffle.api.CompilerDirectives.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.instrument.CreateWrapper;
import com.oracle.truffle.api.instrument.ProbeNode;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.access.variables.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.RPromise.Closure;
import com.oracle.truffle.r.runtime.env.REnvironment;

/**
 * This class denotes a list of {@link #getArguments()} together with their {@link #getNames()}
 * given to a specific function call. The arguments' order is the same as given at the call.<br/>
 * It additionally holds usage hints ({@link #modeChange}, {@link #modeChangeForAll}).
 * <p>
 * It also acts as {@link ClosureCache} for it's arguments, so there is effectively only ever one
 * {@link RootCallTarget} for every argument.
 * </p>
 */
@CreateWrapper
public class CallArgumentsNode extends ArgumentsNode implements UnmatchedArguments {

    @Child private FrameSlotNode varArgsSlotNode;

    /**
     * If a supplied argument is a {@link ReadVariableNode} whose name is "...", this field contains
     * the index of the name. Otherwise it is an empty list.
     */
    @CompilationFinal private final Integer[] varArgsSymbolIndices;

    private final IdentityHashMap<RNode, Closure> closureCache = new IdentityHashMap<>();

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

    private CallArgumentsNode(RNode[] arguments, String[] names, Integer[] varArgsSymbolIndices, boolean modeChange, boolean modeChangeForAll) {
        super(arguments, names);
        this.varArgsSymbolIndices = varArgsSymbolIndices;
        this.modeChange = modeChange;
        this.modeChangeForAll = modeChangeForAll;
        this.varArgsSlotNode = !containsVarArgsSymbol() ? null : FrameSlotNode.create(ArgumentsTrait.VARARG_NAME);
        ArgumentsTrait.internalize(names);
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
     * @param names {@link #names}, set directly. If <code>null</code>, an empty array is created.
     * @return A fresh {@link CallArgumentsNode}
     */
    public static CallArgumentsNode create(boolean modeChange, boolean modeChangeForAll, RNode[] args, String[] names) {
        // Prepare arguments: wrap in WrapArgumentNode
        RNode[] wrappedArgs = new RNode[args.length];
        List<Integer> varArgsSymbolIndices = new ArrayList<>();
        for (int i = 0; i < wrappedArgs.length; ++i) {
            RNode arg = args[i];
            if (arg == null) {
                wrappedArgs[i] = null;
            } else {
                if (arg instanceof ReadVariableNode) {
                    // Check for presence of "..." in the arguments
                    ReadVariableNode rvn = (ReadVariableNode) arg;
                    if (ArgumentsTrait.isVarArg(rvn.getIdentifier())) {
                        varArgsSymbolIndices.add(i);
                    }
                }
                wrappedArgs[i] = WrapArgumentNode.create(arg, i == 0 || modeChangeForAll ? modeChange : true);
            }
        }

        // Check names
        String[] resolvedNames = names;
        if (resolvedNames == null) {
            resolvedNames = new String[args.length];
        } else if (resolvedNames.length < args.length) {
            resolvedNames = Arrays.copyOf(names, args.length);
        }

        // Setup and return
        SourceSection src = Utils.sourceBoundingBox(wrappedArgs);
        Integer[] varArgsSymbolIndicesArr = varArgsSymbolIndices.toArray(new Integer[varArgsSymbolIndices.size()]);
        CallArgumentsNode callArgs = new CallArgumentsNode(wrappedArgs, resolvedNames, varArgsSymbolIndicesArr, modeChange, modeChangeForAll);
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

    /**
     * @param frame
     * @return The {@link VarArgsSignature} of these arguments, or
     *         {@link VarArgsSignature#TAKES_NO_VARARGS} if ! {@link #containsVarArgsSymbol()}
     */
    public VarArgsSignature createSignature(VirtualFrame frame) {
        if (!containsVarArgsSymbol()) {
            return VarArgsSignature.TAKES_NO_VARARGS;
        }

        // Unroll "..."s and insert their arguments into VarArgsSignature
        int times = varArgsSymbolIndices.length;
        return createSignature(getVarargsAndNames(frame), times, true);
    }

    private RArgsValuesAndNames getVarargsAndNames(VirtualFrame frame) {
        RArgsValuesAndNames varArgsAndNames;
        try {
            varArgsAndNames = (RArgsValuesAndNames) frame.getObject(varArgsSlotNode.executeFrameSlot(frame));
        } catch (FrameSlotTypeException | ClassCastException e) {
            throw RInternalError.shouldNotReachHere("'...' should always be represented by RArgsValuesAndNames");
        }
        return varArgsAndNames;
    }

    public static VarArgsSignature createSignature(RArgsValuesAndNames varArgsAndNames, int times, boolean allowConstants) {
        Object[] content;
        // "..." empty?
        if (varArgsAndNames.isEmpty()) {
            content = new Object[]{VarArgsSignature.NO_VARARGS};
        } else {

            // Arguments wrapped into "..."
            Object[] varArgs = varArgsAndNames.getValues();
            content = new Object[varArgs.length];

            // As we want to check on expression identity later on:
            for (int i = 0; i < varArgs.length; i++) {
                Object varArg = varArgs[i];
                if (varArg instanceof RPromise) {
                    // Unwrap expression (one instance per argument/call site)
                    content[i] = ((RPromise) varArg).getRep();
                } else if (RMissingHelper.isMissing(varArg)) {
                    // Use static symbol for "missing" instead of ConstantNode.create
                    content[i] = VarArgsSignature.NO_VARARGS;
                } else {
                    assert allowConstants;
                    content[i] = varArg;
                }
            }
        }
        return VarArgsSignature.create(content, times);
    }

    @ExplodeLoop
    public UnrolledVariadicArguments executeFlatten(VirtualFrame frame) {
        if (!containsVarArgsSymbol()) {
            return UnrolledVariadicArguments.create(getArguments(), getNames(), this);
        } else {
            RNode[] values = new RNode[arguments.length];
            String[] newNames = new String[arguments.length];

            int vargsSymbolsIndex = 0;
            int index = 0;
            for (int i = 0; i < arguments.length; i++) {
                if (vargsSymbolsIndex < varArgsSymbolIndices.length && varArgsSymbolIndices[vargsSymbolsIndex] == i) {
                    // Vararg "..." argument. "execute" to retrieve RArgsValuesAndNames. This is the
                    // reason for this whole method: Before argument matching, we have to unroll
                    // passed "..." every time, as their content might change per call site each
                    // call!
                    RArgsValuesAndNames varArgInfo = getVarargsAndNames(frame);
                    if (varArgInfo.isEmpty()) {
                        // An empty "..." vanishes
                        values = Utils.resizeArray(values, values.length - 1);
                        newNames = Utils.resizeArray(newNames, newNames.length - 1);
                        continue;
                    }

                    // length == 0 cannot happen, in that case RMissing is caught above
                    values = Utils.resizeArray(values, values.length + varArgInfo.length() - 1);
                    newNames = Utils.resizeArray(newNames, newNames.length + varArgInfo.length() - 1);
                    for (int j = 0; j < varArgInfo.length(); j++) {
                        // TODO SourceSection necessary here?
                        // VarArgInfo may contain two types of values here: RPromises and
                        // RMissing.instance. Both need to be wrapped into ConstantNodes, so
                        // they might get wrapped into new promises later on
                        Object varArgValue = varArgInfo.getValues()[j];
                        values[index] = wrapVarArgValue(varArgValue);
                        String newName = varArgInfo.getNames()[j];
                        newNames[index] = newName;
                        index++;
                    }
                    vargsSymbolsIndex++;
                } else {
                    values[index] = arguments[i];
                    newNames[index] = names[i];
                    index++;
                }
            }

            return UnrolledVariadicArguments.create(values, newNames, this);
        }
    }

    @TruffleBoundary
    public static RNode wrapVarArgValue(Object varArgValue) {
        if (varArgValue instanceof RPromise) {
            return PromiseNode.createVarArg((RPromise) varArgValue);
        } else {
            return ConstantNode.create(varArgValue);
        }
    }

    /**
     * @return {@link #containsVarArgsSymbol}
     */
    public boolean containsVarArgsSymbol() {
        return varArgsSymbolIndices.length > 0;
    }

    @Override
    public IdentityHashMap<RNode, Closure> getContent() {
        return closureCache;
    }

    @Override
    public int getVarArgIndex() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasVarArgs() {
        throw new UnsupportedOperationException();
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

    @TruffleBoundary
    @Override
    public RNode substitute(REnvironment env) {
        boolean changed = false;
        RNode[] argNodesNew = new RNode[arguments.length];
        int missingCount = 0;
        int j = 0;
        for (int i = 0; i < arguments.length; i++) {
            RNode argNode = arguments[i];
            RNode argNodeSubs = argNode.substitute(env);
            if (argNodeSubs instanceof RASTUtils.MissingDotsNode) {
                // in this case we remove the argument altogether, leave slot as null
                missingCount++;
                changed = true;
            } else if (argNodeSubs instanceof RASTUtils.ExpandedDotsNode) {
                // 2 or more
                RASTUtils.ExpandedDotsNode expandedDotsNode = (RASTUtils.ExpandedDotsNode) argNodeSubs;
                RNode[] argNodesNewer = new RNode[argNodesNew.length + expandedDotsNode.nodes.length - 1];
                if (i > 0) {
                    System.arraycopy(argNodesNew, 0, argNodesNewer, 0, j);
                }
                System.arraycopy(expandedDotsNode.nodes, 0, argNodesNewer, j, expandedDotsNode.nodes.length);
                argNodesNew = argNodesNewer;
                changed = true;
                j += expandedDotsNode.nodes.length - 1;
            } else {
                argNodesNew[j] = argNodeSubs;
                changed = changed || argNode != argNodeSubs;
            }
            j++;
        }
        if (!changed) {
            return this;
        } else {
            if (missingCount > 0) {
                // Strip out the Missing ... instances
                RNode[] argNodesNewer = new RNode[argNodesNew.length - missingCount];
                j = 0;
                for (int i = 0; i < argNodesNew.length; i++) {
                    RNode argNode = argNodesNew[i];
                    if (argNode == null) {
                        continue;
                    }
                    argNodesNewer[j++] = argNode;
                }
                argNodesNew = argNodesNewer;
            }
            return CallArgumentsNode.create(false, false, argNodesNew, names);
        }

    }

    @Override
    public ProbeNode.WrapperNode createWrapperNode() {
        return new CallArgumentsNodeWrapper(this);
    }

    protected CallArgumentsNode() {
        this.varArgsSymbolIndices = null;
        this.modeChange = false;
        this.modeChangeForAll = false;
    }
}
