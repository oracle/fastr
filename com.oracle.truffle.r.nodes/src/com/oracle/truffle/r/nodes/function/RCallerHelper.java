/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.util.function.Supplier;

import com.oracle.truffle.r.nodes.RASTUtils;
import com.oracle.truffle.r.nodes.access.ConstantNode;
import com.oracle.truffle.r.nodes.access.variables.ReadVariableNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RCaller;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

/**
 * It's a helper for constructing {@link RCaller} representations (placed here due to inter-package
 * dependencies).
 */
public final class RCallerHelper {

    private RCallerHelper() {
        // container class
    }

    /**
     * This function can be used to construct a proper RCaller instance whenever a name of the
     * function called by the CallMatcherNode is available (e.g. when CallMatcherNode is used by
     * S3/S4 dispatch), and that can then be used to retrieve correct syntax nodes.
     *
     * @param arguments array with arguments and corresponding names. This method strips any
     *            {@code RMissing} arguments and unrolls all varargs within the arguments array.
     */
    public static Supplier<RSyntaxNode> createFromArguments(RFunction function, RArgsValuesAndNames arguments) {
        return createFromArgumentsInternal(function, arguments);
    }

    /**
     * @see #createFromArguments(RFunction, RArgsValuesAndNames)
     */
    public static Supplier<RSyntaxNode> createFromArguments(String function, RArgsValuesAndNames arguments) {
        return createFromArgumentsInternal(function, arguments);
    }

    public static Supplier<RSyntaxNode> createFromArgumentsInternal(final Object function, final RArgsValuesAndNames arguments) {
        return new Supplier<RSyntaxNode>() {

            RSyntaxNode syntaxNode = null;

            @Override
            public RSyntaxNode get() {
                if (syntaxNode == null) {
                    int length = 0;
                    for (int i = 0; i < arguments.getLength(); i++) {
                        Object arg = arguments.getArgument(i);
                        if (arg instanceof RArgsValuesAndNames) {
                            length += ((RArgsValuesAndNames) arg).getLength();
                        } else if (arguments.getArgument(i) != RMissing.instance) {
                            length++;
                        }
                    }

                    RSyntaxNode[] syntaxArguments = new RSyntaxNode[length];
                    String[] signature = new String[length];
                    int index = 0;
                    for (int i = 0; i < arguments.getLength(); i++) {
                        Object arg = arguments.getArgument(i);
                        if (arg instanceof RArgsValuesAndNames) {
                            RArgsValuesAndNames varargs = (RArgsValuesAndNames) arg;
                            for (int j = 0; j < varargs.getLength(); j++) {
                                syntaxArguments[index] = getArgumentNode(varargs.getArgument(j));
                                signature[index] = varargs.getSignature().getName(j);
                                index++;
                            }
                        } else if (arg != RMissing.instance) {
                            syntaxArguments[index] = getArgumentNode(arg);
                            signature[index] = arguments.getSignature().getName(i);
                            index++;
                        }
                    }
                    Object replacedFunction = function instanceof String ? ReadVariableNode.createFunctionLookup(RSyntaxNode.EAGER_DEPARSE, (String) function) : function;
                    syntaxNode = RASTUtils.createCall(replacedFunction, true, ArgumentsSignature.get(signature), syntaxArguments);
                }
                return syntaxNode;
            }
        };
    }

    private static RSyntaxNode getArgumentNode(Object arg) {
        if (arg instanceof RPromise) {
            RPromise p = (RPromise) arg;
            return p.getRep().asRSyntaxNode();
        } else if (!(arg instanceof RMissing)) {
            return ConstantNode.create(arg);
        }
        return null;
    }

    /**
     * This method calculates the signature of the permuted arguments lazily.
     */
    public static Supplier<RSyntaxNode> createFromArguments(String function, long[] preparePermutation, Object[] suppliedArguments, ArgumentsSignature suppliedSignature) {
        return new Supplier<RSyntaxNode>() {

            RSyntaxNode syntaxNode = null;

            @Override
            public RSyntaxNode get() {
                if (syntaxNode == null) {
                    Object[] values = new Object[preparePermutation.length];
                    String[] names = new String[preparePermutation.length];
                    for (int i = 0; i < values.length; i++) {
                        long source = preparePermutation[i];
                        if (!ArgumentsSignature.isVarArgsIndex(source)) {
                            values[i] = suppliedArguments[(int) source];
                            names[i] = suppliedSignature.getName((int) source);
                        } else {
                            int varArgsIdx = ArgumentsSignature.extractVarArgsIndex(source);
                            int argsIdx = ArgumentsSignature.extractVarArgsArgumentIndex(source);
                            RArgsValuesAndNames varargs = (RArgsValuesAndNames) suppliedArguments[varArgsIdx];
                            values[i] = varargs.getArguments()[argsIdx];
                            names[i] = varargs.getSignature().getName(argsIdx);
                        }
                    }
                    RArgsValuesAndNames arguments = new RArgsValuesAndNames(values, ArgumentsSignature.get(names));
                    syntaxNode = createFromArguments(function, arguments).get();
                }
                return syntaxNode;
            }
        };
    }
}
