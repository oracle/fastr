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

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.r.runtime.Arguments;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
import com.oracle.truffle.r.runtime.nodes.RNode;

/**
 * <p>
 * This class denotes a list of name/argument pairs which are in the order of the
 * {@link FormalArguments} of a function. Each argument is either filled with the supplied argument,
 * the default argument or <code>null</code>, if neither is provided.
 * </p>
 * <p>
 * The {@link #doExecuteArray(VirtualFrame)} method executes the argument nodes and converts them
 * into a form that can be passed into functions.
 * </p>
 */
public final class MatchedArguments extends Arguments<RNode> {

    static final class MatchedArgumentsNode extends RBaseNode {
        @Children private final RNode[] arguments;
        private final ArgumentsSignature signature;

        private MatchedArgumentsNode(RNode[] arguments, ArgumentsSignature signature) {
            this.arguments = arguments;
            this.signature = signature;
        }

        @ExplodeLoop
        Object[] executeArray(VirtualFrame frame) {
            Object[] result = new Object[arguments.length];
            for (int i = 0; i < arguments.length; i++) {
                result[i] = arguments[i].execute(frame);
                if (i == 0 && result[i] instanceof String && result[i].equals(".startMsg")) {
                    System.console();
                }
            }
            return result;
        }

        public ArgumentsSignature getSignature() {
            return signature;
        }

        public RNode[] getArguments() {
            return arguments;
        }
    }

    private MatchedArguments(RNode[] arguments, ArgumentsSignature signature) {
        super(arguments, signature);
    }

    MatchedArgumentsNode createNode() {
        return new MatchedArgumentsNode(getArguments(), getSignature());
    }

    /**
     * @return A fresh {@link MatchedArguments}; arguments may contain <code>null</code> iff there
     *         is neither a supplied argument nor a default argument
     */
    static MatchedArguments create(RNode[] arguments, ArgumentsSignature signature) {
        return new MatchedArguments(arguments, signature);
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
    Object[] doExecuteArray(VirtualFrame frame) {
        Object[] result = new Object[getArguments().length];
        for (int i = 0; i < getArguments().length; i++) {
            result[i] = getArguments()[i].execute(frame);
        }
        return result;
    }
}
