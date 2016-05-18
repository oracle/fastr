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

import java.util.Arrays;
import java.util.function.Supplier;

import com.oracle.truffle.r.nodes.RASTUtils;
import com.oracle.truffle.r.nodes.access.ConstantNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RCaller;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
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
     */
    public static Supplier<RSyntaxNode> createFromArguments(final Object function, final RArgsValuesAndNames arguments) {
        return new Supplier<RSyntaxNode>() {

            RSyntaxNode syntaxNode = null;

            @Override
            public RSyntaxNode get() {
                if (syntaxNode == null) {
                    RSyntaxNode[] syntaxArguments = new RSyntaxNode[arguments.getLength()];
                    int index = 0;
                    // arguments are already ordered - once one is missing, all the remaining ones
                    // must
                    // be
                    // missing
                    boolean missing = false;
                    for (int i = 0; i < arguments.getLength(); i++) {
                        Object arg = arguments.getArgument(i);
                        if (arg instanceof RPromise) {
                            assert !missing;
                            RPromise p = (RPromise) arg;
                            syntaxArguments[index] = p.getRep().asRSyntaxNode();
                            index++;
                        } else if (arg instanceof RArgsValuesAndNames) {
                            RArgsValuesAndNames vararg = (RArgsValuesAndNames) arg;
                            if (vararg.getLength() == 0) {
                                // no var arg arguments
                                syntaxArguments = Arrays.copyOf(syntaxArguments, syntaxArguments.length - 1);

                            } else {
                                assert !missing;
                                Object[] additionalArgs = vararg.getArguments();
                                syntaxArguments = Arrays.copyOf(syntaxArguments, syntaxArguments.length + additionalArgs.length - 1);
                                for (int j = 0; j < additionalArgs.length; j++) {
                                    if (additionalArgs[j] instanceof RPromise) {
                                        RPromise p = (RPromise) additionalArgs[j];
                                        syntaxArguments[index] = p.getRep().asRSyntaxNode();
                                    } else {
                                        assert additionalArgs[j] != RMissing.instance;
                                        syntaxArguments[index] = ConstantNode.create(additionalArgs[j]);
                                    }
                                    index++;
                                }
                            }
                        } else {
                            if (arg instanceof RMissing) {
                                syntaxArguments = Arrays.copyOf(syntaxArguments, syntaxArguments.length - 1);
                            } else {
                                assert !missing;
                                syntaxArguments[index] = ConstantNode.create(arg);
                                index++;
                            }
                        }

                    }
                    syntaxNode = RASTUtils.createCall(function, true, ArgumentsSignature.empty(syntaxArguments.length), syntaxArguments);
                }
                return syntaxNode;
            }
        };
    }
}
