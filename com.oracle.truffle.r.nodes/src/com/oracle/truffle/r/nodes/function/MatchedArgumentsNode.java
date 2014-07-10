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
import com.oracle.truffle.r.runtime.data.*;

/**
 * <p>
 * This class denotes a list of {@link #getNames()}/{@link #getArguments()} pairs which are in the
 * order of the {@link FormalArguments} of a function. Each argument is either filled with the
 * supplied argument, the default argument or <code>null</code>, if neither is provided.
 * </p>
 * <p>
 * The {@link #executeArray(VirtualFrame, RFunction, EOutput)} method converts these arguments into
 * a form that can be passed into functions. E.g., wraps arguments into Promises etc.
 * </p>
 */
public class MatchedArgumentsNode extends ArgumentsNode {

    public enum EOutput {
        /**
         * Arguments are processed as is, not {@link RPromise} created (legacy!)
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
     * @param arguments {@link #getArguments()}
     * @param names {@link #getNames()}
     */
    private MatchedArgumentsNode(RNode[] arguments, String[] names) {
        super(arguments, names);
    }

    /**
     * @param arguments
     * @param names
     * @param src
     * @return A fresh {@link MatchedArgumentsNode}; arguments may contain <code>null</code> iff
     *         there is neither a supplied argument nor a default argument
     */
    public static MatchedArgumentsNode create(RNode[] arguments, String[] names, SourceSection src) {
        MatchedArgumentsNode matchedArgs = new MatchedArgumentsNode(arguments, names);
        matchedArgs.assignSourceSection(src);
        return matchedArgs;
    }

    // Mark unusable
    @Override
    @Deprecated
    public Object execute(VirtualFrame frame) {
        throw new AssertionError();
    }

    /**
     * Use {@link #executeArray(VirtualFrame, RFunction, EOutput)} instead!
     *
     * @see com.oracle.truffle.r.nodes.RNode#executeArray(com.oracle.truffle.api.frame.VirtualFrame)
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
    public Object[] executeArrayRaw(VirtualFrame frame) {
        return executeArray(frame, null, EOutput.RAW);
    }

    /**
     * This class does the heavy lifting in inspecting the
     *
     * @param frame
     * @return The wrapped arguments
     */
    @ExplodeLoop
    public Object[] executeArray(VirtualFrame frame, @SuppressWarnings("unused") RFunction function, EOutput type) {
        Object[] wrappedArguments = new Object[arguments.length];

        for (int i = 0; i < arguments.length; i++) {
            wrappedArguments[i] = wrapArgument(arguments[i], frame, type);
        }

        return wrappedArguments;
    }

    private static Object wrapArgument(RNode arg, VirtualFrame frame, EOutput type) {
        Object result = null;
        if (type == EOutput.RAW) {
            if (arg == null) {
                result = ConstantNode.create(RMissing.instance);
            } else {
                result = arg.execute(frame);
            }
        }

        assert result != null : "Arguments may never be <null>!";
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
}
