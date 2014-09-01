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
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.runtime.*;

/**
 * <p>
 * This class denotes a list of {@link #getNames()}/{@link #getArguments()} pairs which are in the
 * order of the {@link FormalArguments} of a function. Each argument is either filled with the
 * supplied argument, the default argument or <code>null</code>, if neither is provided.
 * </p>
 * <p>
 * The {@link #doExecuteArray(VirtualFrame)} method executes the argument nodes and converts them
 * into a form that can be passed into functions.
 * </p>
 */
public final class MatchedArguments extends Arguments<RNode> {

    /**
     * @param arguments {@link #getArguments()}
     * @param names {@link #getNames()}
     */
    private MatchedArguments(RNode[] arguments, String[] names) {
        super(arguments, names);
    }

    /**
     * @param arguments
     * @return ad
     */
    public static MatchedArguments createUnnamed(RNode[] arguments) {
        String[] names = new String[arguments.length];
        for (int i = 0; i < names.length; i++) {
            names[i] = null;
        }
        return create(arguments, names);
    }

    /**
     * @param arguments
     * @param names
     * @return A fresh {@link MatchedArguments}; arguments may contain <code>null</code> iff there
     *         is neither a supplied argument nor a default argument
     */
    public static MatchedArguments create(RNode[] arguments, String[] names) {
        MatchedArguments matchedArgs = new MatchedArguments(arguments, names);
        return matchedArgs;
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
    public Object[] doExecuteArray(VirtualFrame frame) {
        Object[] result = new Object[arguments.length];
        for (int i = 0; i < arguments.length; i++) {
            RNode arg = arguments[i];
            result[i] = arg.execute(frame);
        }
        return result;
    }

    /**
     * @return The consolidated list of arguments that should be passed to a function.
     *         <code>null</code> denotes 'no argument specified'
     */
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
