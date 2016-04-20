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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.r.nodes.access.FrameSlotNode;
import com.oracle.truffle.r.nodes.access.variables.ReadVariableNode;
import com.oracle.truffle.r.nodes.function.PromiseHelperNode.PromiseCheckHelperNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RPromise.Closure;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

/**
 * This class denotes a list of {@link #getArguments()} together with their names given to a
 * specific function call. The arguments' order is the same as given at the call.<br/>
 * <p>
 * It also acts as {@link ClosureCache} for it's arguments, so there is effectively only ever one
 * {@link RootCallTarget} for every argument.
 * </p>
 */
public final class CallArgumentsNode extends RBaseNode implements UnmatchedArguments {
    /**
     * A list of arguments. Single arguments may be <code>null</code>; semantics have to be
     * specified by implementing classes
     */
    @Children protected final RNode[] arguments;

    protected final ArgumentsSignature signature;

    @Child private FrameSlotNode varArgsSlotNode;
    @Child private PromiseCheckHelperNode promiseHelper;

    /**
     * If a supplied argument is a {@link ReadVariableNode} whose name is "...", this field contains
     * the index of the name. Otherwise it is an empty list.
     */
    @CompilationFinal private final int[] varArgsSymbolIndices;

    private final IdentityHashMap<RNode, Closure> closureCache = new IdentityHashMap<>();

    private CallArgumentsNode(RNode[] arguments, ArgumentsSignature signature, int[] varArgsSymbolIndices) {
        assert signature != null && signature.getLength() == arguments.length : Arrays.toString(arguments) + " " + signature;
        this.arguments = arguments;
        this.signature = signature;
        assert signature != null;
        this.varArgsSymbolIndices = varArgsSymbolIndices;
        this.varArgsSlotNode = !containsVarArgsSymbol() ? null : FrameSlotNode.create(ArgumentsSignature.VARARG_NAME);
    }

    /**
     * the two flags below are used in cases when we know that either a builtin is not going to
     * modify the arguments which are not meant to be modified (like in the case of binary
     * operators) or that its intention is to actually update the argument (as in the case of
     * replacement forms, such as dim(x)<-1; in these cases the mode change
     * (temporary->non-temporary->shared) does not need to happen, which is what the first flag
     * (modeChange) determines, with the second (modeChangeForAll) flat telling the runtime if this
     * affects only the first argument (replacement functions) or all arguments (binary operators).
     *
     * @param modeChange
     * @param modeChangeForAll
     * @param args {@link #arguments}; new array gets created. Every {@link RNode} (except
     *            <code>null</code>) gets wrapped into a {@link WrapArgumentNode}.
     * @return A fresh {@link CallArgumentsNode}
     */
    public static CallArgumentsNode create(boolean modeChange, boolean modeChangeForAll, RNode[] args, ArgumentsSignature signature) {
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
                wrappedArgs[i] = WrapArgumentNode.create(arg, i == 0 || modeChangeForAll ? modeChange : true, i);
            }
        }

        // Setup and return
        int[] varArgsSymbolIndicesArr = new int[varArgsSymbolIndices.size()];
        for (int i = 0; i < varArgsSymbolIndicesArr.length; i++) {
            varArgsSymbolIndicesArr[i] = varArgsSymbolIndices.get(i);
        }
        return new CallArgumentsNode(wrappedArgs, signature, varArgsSymbolIndicesArr);
    }

    public static RArgsValuesAndNames getVarargsAndNames(Frame frame) {
        CompilerAsserts.neverPartOfCompilation();
        RArgsValuesAndNames varArgs = ReadVariableNode.lookupVarArgs(frame);
        if (varArgs == null) {
            RError.error(RError.SHOW_CALLER, RError.Message.NO_DOT_DOT_DOT);
        }
        return varArgs;
    }

    /**
     * This methods unrolls all "..." in the argument list. The result varies if the number of
     * arguments in the varargs or their names change.
     */
    public UnrolledVariadicArguments executeFlatten(Frame frame) {
        CompilerAsserts.neverPartOfCompilation();
        if (!containsVarArgsSymbol()) {
            return UnrolledVariadicArguments.create(getArguments(), getSignature(), this);
        } else {
            RNode[] values = new RNode[arguments.length];
            String[] newNames = new String[arguments.length];

            int vargsSymbolsIndex = 0;
            int index = 0;
            for (int i = 0; i < arguments.length; i++) {
                if (vargsSymbolsIndex < varArgsSymbolIndices.length && varArgsSymbolIndices[vargsSymbolsIndex] == i) {
                    RArgsValuesAndNames varArgInfo = getVarargsAndNames(frame);
                    if (varArgInfo.isEmpty()) {
                        // An empty "..." vanishes
                        values = Utils.resizeArray(values, values.length - 1);
                        newNames = Utils.resizeArray(newNames, newNames.length - 1);
                        continue;
                    }

                    values = Utils.resizeArray(values, values.length + varArgInfo.getLength() - 1);
                    newNames = Utils.resizeArray(newNames, newNames.length + varArgInfo.getLength() - 1);
                    for (int j = 0; j < varArgInfo.getLength(); j++) {
                        values[index] = PromiseNode.createVarArg(j);
                        newNames[index] = varArgInfo.getSignature().getName(j);
                        index++;
                    }
                    vargsSymbolsIndex++;
                } else {
                    values[index] = arguments[i];
                    newNames[index] = signature.getName(i);
                    index++;
                }
            }

            return UnrolledVariadicArguments.create(values, ArgumentsSignature.get(newNames), this);
        }
    }

    @ExplodeLoop
    public RArgsValuesAndNames evaluateFlatten(VirtualFrame frame, RArgsValuesAndNames varArgInfo) {
        int size = arguments.length;
        ArgumentsSignature resultSignature = null;
        String[] names = null;
        if (containsVarArgsSymbol()) {
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

    @ExplodeLoop
    public Object[] evaluateFlattenObjects(VirtualFrame frame) {
        int size = arguments.length;
        RArgsValuesAndNames varArgInfo = null;
        if (containsVarArgsSymbol()) {
            varArgInfo = getVarargsAndNames(frame);
            size += (varArgInfo.getLength() - 1) * varArgsSymbolIndices.length;
        }
        Object[] values = new Object[size];
        int vargsSymbolsIndex = 0;
        int index = 0;
        for (int i = 0; i < arguments.length; i++) {
            if (vargsSymbolsIndex < varArgsSymbolIndices.length && varArgsSymbolIndices[vargsSymbolsIndex] == i) {
                index = flattenVarArgsObject(frame, varArgInfo, values, index);
                vargsSymbolsIndex++;
            } else {
                values[index] = arguments[i] == null ? RMissing.instance : arguments[i].execute(frame);
                index++;
            }
        }
        return values;
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

    @Override
    public ArgumentsSignature getSignature() {
        return signature;
    }

    public RSyntaxNode[] getSyntaxArguments() {
        RSyntaxNode[] result = new RSyntaxNode[arguments.length];
        for (int i = 0; i < result.length; i++) {
            RNode argument = arguments[i];
            result[i] = argument.asRSyntaxNode();
        }
        return result;
    }

    private int flattenVarArgsObject(VirtualFrame frame, RArgsValuesAndNames varArgInfo, Object[] values, int startIndex) {
        int index = startIndex;
        for (int j = 0; j < varArgInfo.getLength(); j++) {
            if (promiseHelper == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                promiseHelper = insert(new PromiseCheckHelperNode());
            }
            values[index] = promiseHelper.checkEvaluate(frame, varArgInfo.getArgument(j));
            index++;
        }
        return index;
    }
}
