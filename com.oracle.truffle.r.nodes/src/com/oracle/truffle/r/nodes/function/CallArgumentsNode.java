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
import com.oracle.truffle.api.instrument.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.access.variables.*;
import com.oracle.truffle.r.nodes.function.PromiseHelperNode.PromiseCheckHelperNode;
import com.oracle.truffle.r.nodes.instrument.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.RPromise.Closure;
import com.oracle.truffle.r.runtime.env.*;

/**
 * This class denotes a list of {@link #getArguments()} together with their names given to a
 * specific function call. The arguments' order is the same as given at the call.<br/>
 * It additionally holds usage hints ({@link #modeChange}, {@link #modeChangeForAll}).
 * <p>
 * It also acts as {@link ClosureCache} for it's arguments, so there is effectively only ever one
 * {@link RootCallTarget} for every argument.
 * </p>
 */
@NeedsWrapper
public class CallArgumentsNode extends ArgumentsNode {

    @Child private FrameSlotNode varArgsSlotNode;
    @Child private PromiseCheckHelperNode promiseHelper;

    /**
     * If a supplied argument is a {@link ReadVariableNode} whose name is "...", this field contains
     * the index of the name. Otherwise it is an empty list.
     */
    @CompilationFinal private final int[] varArgsSymbolIndices;

    private static final int UNINITIALIZED = -1;
    private static final int VARIABLE = -2;

    @CompilationFinal private int cachedSignatureLength = UNINITIALIZED;

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

    private CallArgumentsNode(RNode[] arguments, ArgumentsSignature signature, int[] varArgsSymbolIndices, boolean modeChange, boolean modeChangeForAll) {
        super(arguments, signature);
        this.varArgsSymbolIndices = varArgsSymbolIndices;
        this.modeChange = modeChange;
        this.modeChangeForAll = modeChangeForAll;
        this.varArgsSlotNode = !containsVarArgsSymbol() ? null : FrameSlotNode.create(ArgumentsSignature.VARARG_NAME);
    }

    public static CallArgumentsNode createUnnamed(boolean modeChange, boolean modeChangeForAll, RNode... args) {
        return create(modeChange, modeChangeForAll, args, ArgumentsSignature.empty(args.length));
    }

    /**
     * Called only from the parser.
     */
    public static CallArgumentsNode create(SourceSection argsSource, boolean modeChange, RNode[] args, ArgumentsSignature signature) {
        return create(argsSource, modeChange, false, args, signature);
    }

    public static CallArgumentsNode create(boolean modeChange, boolean modeChangeForAll, RNode[] args, ArgumentsSignature signature) {
        return create(null, modeChange, modeChangeForAll, args, signature);
    }

    /**
     * @param argsSource the {@link SourceSection} object associated with the arguments (or
     *            {@code null})
     * @param modeChange {@link #modeChange}
     * @param modeChangeForAll {@link #modeChangeForAll}
     * @param args {@link #arguments}; new array gets created. Every {@link RNode} (except
     *            <code>null</code>) gets wrapped into a {@link WrapArgumentNode}.
     * @return A fresh {@link CallArgumentsNode}
     */
    private static CallArgumentsNode create(SourceSection argsSource, boolean modeChange, boolean modeChangeForAll, RNode[] args, ArgumentsSignature signature) {
        // Prepare arguments: wrap in WrapArgumentNode
        RNode[] wrappedArgs = new RNode[args.length];
        List<Integer> varArgsSymbolIndices = new ArrayList<>();
        for (int i = 0; i < wrappedArgs.length; i++) {
            RNode arg = args[i];
            if (arg == null) {
                wrappedArgs[i] = null;
            } else {
                if (arg instanceof ReadVariableNode) {
                    // Check for presence of "..." in the arguments
                    ReadVariableNode rvn = (ReadVariableNode) arg;
                    if (ArgumentsSignature.VARARG_NAME.equals(rvn.getIdentifier())) {
                        varArgsSymbolIndices.add(i);
                    }
                }
                wrappedArgs[i] = WrapArgumentNode.create(arg, i == 0 || modeChangeForAll ? modeChange : true);
            }
        }

        // Setup and return
        SourceSection src = Utils.sourceBoundingBox(argsSource, wrappedArgs);
        int[] varArgsSymbolIndicesArr = new int[varArgsSymbolIndices.size()];
        for (int i = 0; i < varArgsSymbolIndicesArr.length; i++) {
            varArgsSymbolIndicesArr[i] = varArgsSymbolIndices.get(i);
        }
        CallArgumentsNode callArgs = new CallArgumentsNode(wrappedArgs, signature, varArgsSymbolIndicesArr, modeChange, modeChangeForAll);
        callArgs.assignSourceSection(src);
        return callArgs;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        throw RInternalError.shouldNotReachHere("Execute has not semantic meaning for CallArgumentsNode");
    }

    @Override
    public Object[] executeArray(VirtualFrame frame) throws UnexpectedResultException {
        throw RInternalError.shouldNotReachHere("Execute has not semantic meaning for CallArgumentsNode");
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
        RArgsValuesAndNames varArgsAndNames = getVarargsAndNames(frame);

        // "..." empty?
        if (varArgsAndNames.isEmpty()) {
            return VarArgsSignature.NO_VARARGS_GIVEN;
        } else {
            // Arguments wrapped into "..."
            Object[] varArgs = varArgsAndNames.getArguments();
            Object[] content;
            if (cachedSignatureLength != VARIABLE && cachedSignatureLength != varArgs.length) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                cachedSignatureLength = cachedSignatureLength == UNINITIALIZED ? varArgs.length : VARIABLE;
            }
            if (cachedSignatureLength == VARIABLE) {
                content = new Object[varArgs.length];
                createSignatureLoop(content, varArgs);
            } else {
                content = new Object[cachedSignatureLength];
                createSignatureLoopUnrolled(content, varArgs, cachedSignatureLength);
            }
            return VarArgsSignature.create(content, times);
        }
    }

    public RArgsValuesAndNames getVarargsAndNames(VirtualFrame frame) {
        RArgsValuesAndNames varArgsAndNames;
        try {
            varArgsAndNames = (RArgsValuesAndNames) frame.getObject(varArgsSlotNode.executeFrameSlot(frame));
        } catch (FrameSlotTypeException | ClassCastException e) {
            throw RInternalError.shouldNotReachHere("'...' should always be represented by RArgsValuesAndNames");
        }
        return varArgsAndNames;
    }

    public static VarArgsSignature createSignature(RArgsValuesAndNames varArgsAndNames, int times) {
        // "..." empty?
        if (varArgsAndNames.isEmpty()) {
            return VarArgsSignature.NO_VARARGS_GIVEN;
        } else {
            // Arguments wrapped into "..."
            Object[] varArgs = varArgsAndNames.getArguments();
            Object[] content = new Object[varArgs.length];

            createSignatureLoop(content, varArgs);
            return VarArgsSignature.create(content, times);
        }
    }

    private static void createSignatureLoop(Object[] content, Object[] varArgs) {
        // As we want to check on expression identity later on:
        for (int i = 0; i < varArgs.length; i++) {
            createSignatureLoopContents(content, varArgs, i);
        }
    }

    @ExplodeLoop
    private static void createSignatureLoopUnrolled(Object[] content, Object[] varArgs, int length) {
        // As we want to check on expression identity later on:
        for (int i = 0; i < length; i++) {
            createSignatureLoopContents(content, varArgs, i);
        }
    }

    private static void createSignatureLoopContents(Object[] content, Object[] varArgs, int i) {
        Object varArg = varArgs[i];
        if (varArg instanceof RPromise) {
            // Unwrap expression (one instance per argument/call site)
            content[i] = ((RPromise) varArg).getRep();
        } else if (RMissingHelper.isMissing(varArg)) {
            // Use static symbol for "missing" instead of ConstantNode.create
            content[i] = VarArgsSignature.NO_VARARGS;
        } else {
            content[i] = varArg;
        }
    }

    @ExplodeLoop
    public UnrolledVariadicArguments executeFlatten(VirtualFrame frame) {
        if (!containsVarArgsSymbol()) {
            return UnrolledVariadicArguments.create(getArguments(), getSignature(), this);
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
                    values = Utils.resizeArray(values, values.length + varArgInfo.getLength() - 1);
                    newNames = Utils.resizeArray(newNames, newNames.length + varArgInfo.getLength() - 1);
                    for (int j = 0; j < varArgInfo.getLength(); j++) {
                        // TODO SourceSection necessary here?
                        // VarArgInfo may contain two types of values here: RPromises and
                        // RMissing.instance. Both need to be wrapped into ConstantNodes, so
                        // they might get wrapped into new promises later on
                        Object varArgValue = varArgInfo.getArgument(j);
                        values[index] = wrapVarArgValue(varArgValue, j);
                        String newName = varArgInfo.getSignature().getName(j);
                        newNames[index] = newName;
                        index++;
                    }
                    vargsSymbolsIndex++;
                } else {
                    values[index] = arguments[i];
                    newNames[index] = signature.getName(i);
                    index++;
                }
            }

            ArgumentsSignature newSignature = ArgumentsSignature.get(newNames);
            return UnrolledVariadicArguments.create(values, newSignature, this);
        }
    }

    @ExplodeLoop
    public RArgsValuesAndNames evaluateFlatten(VirtualFrame frame) {
        int size = arguments.length;
        RArgsValuesAndNames varArgInfo = null;
        ArgumentsSignature resultSignature = null;
        String[] names = null;
        if (containsVarArgsSymbol()) {
            varArgInfo = getVarargsAndNames(frame);
            size += (varArgInfo.getLength() - 1) * varArgsSymbolIndices.length;
            names = new String[size];
        } else {
            resultSignature = signature;
        }
        Object[] values = new Object[size];
        int vargsSymbolsIndex = 0;
        int index = 0;
        for (int i = 0; i < arguments.length; i++) {
            if (vargsSymbolsIndex < varArgsSymbolIndices.length && varArgsSymbolIndices[vargsSymbolsIndex] == i) {
                index = flattenVarArgs(frame, varArgInfo, names, values, index);
                vargsSymbolsIndex++;
            } else {
                values[index] = arguments[i] == null ? RMissing.instance : arguments[i].execute(frame);
                if (names != null) {
                    names[index] = signature.getName(i);
                }
                index++;
            }
        }
        if (resultSignature == null) {
            resultSignature = ArgumentsSignature.get(names);
        }
        return new RArgsValuesAndNames(values, resultSignature);
    }

    private int flattenVarArgs(VirtualFrame frame, RArgsValuesAndNames varArgInfo, String[] names, Object[] values, int startIndex) {
        int index = startIndex;
        for (int j = 0; j < varArgInfo.getLength(); j++) {
            if (promiseHelper == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                promiseHelper = insert(new PromiseCheckHelperNode());
            }
            values[index] = promiseHelper.checkEvaluate(frame, varArgInfo.getArgument(j));
            names[index] = varArgInfo.getSignature().getName(j);
            index++;
        }
        return index;
    }

    @TruffleBoundary
    public static RNode wrapVarArgValue(Object varArgValue, int varArgIndex) {
        if (varArgValue instanceof RPromise) {
            return PromiseNode.createVarArg(varArgIndex);
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

    public int[] getVarArgsSymbolIndices() {
        return varArgsSymbolIndices;
    }

    @Override
    public IdentityHashMap<RNode, Closure> getContent() {
        return closureCache;
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
    public RSyntaxNode substitute(REnvironment env) {
        RNode[] argNodesNew = new RNode[arguments.length];
        boolean layoutChanged = false;
        boolean contentChanged = false;
        int size = arguments.length;
        for (int i = 0; i < arguments.length; i++) {
            RNode argNodeSubs = RSyntaxNode.cast(arguments[i]).substitute(env).asRNode();
            argNodesNew[i] = argNodeSubs;
            if (argNodeSubs instanceof RASTUtils.MissingDotsNode) {
                // in this case we remove the argument altogether
                layoutChanged = true;
                size--;
            } else if (argNodeSubs instanceof RASTUtils.ExpandedDotsNode) {
                layoutChanged = true;
                size += ((RASTUtils.ExpandedDotsNode) argNodeSubs).nodes.length - 1;
            } else {
                contentChanged |= arguments[i] != argNodeSubs;
            }
        }
        if (!layoutChanged) {
            if (contentChanged) {
                return CallArgumentsNode.create(false, false, argNodesNew, signature);
            } else {
                return this;
            }
        }
        String[] names = new String[size];
        RNode[] argNodesFinal = new RNode[size];
        int pos = 0;
        for (int i = 0; i < arguments.length; i++) {
            RNode argNodeSubs = argNodesNew[i];
            if (argNodeSubs instanceof RASTUtils.MissingDotsNode) {
                // nothing to do
            } else if (argNodeSubs instanceof RASTUtils.ExpandedDotsNode) {
                RASTUtils.ExpandedDotsNode expandedDotsNode = (RASTUtils.ExpandedDotsNode) argNodeSubs;
                System.arraycopy(expandedDotsNode.nodes, 0, argNodesFinal, pos, expandedDotsNode.nodes.length);
                pos += expandedDotsNode.nodes.length;
            } else {
                names[pos] = signature.getName(i);
                argNodesFinal[pos++] = argNodesNew[i];
            }
        }
        return CallArgumentsNode.create(false, false, argNodesFinal, ArgumentsSignature.get(names));
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
