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

import com.oracle.truffle.r.runtime.*;

/**
 * Simple container class for holding arguments ({@link #getEvaluatedArgs()}) which are ready to be
 * pushed into {@link RArguments} (or are taken from there!). This is used by
 * {@link UseMethodDispatchNode}, e.g.
 *
 * @see #getNames()
 * @see #getEvaluatedArgs()
 */
public class EvaluatedArguments extends Arguments<Object> {

    /**
     * @param evaluatedArgs {@link #getEvaluatedArgs()}
     * @param names {@link #getNames()}
     */
    EvaluatedArguments(Object[] evaluatedArgs, String[] names) {
        super(evaluatedArgs, names);
    }

    public static EvaluatedArguments create(Object[] args, String[] names) {
        String[] argNames = names;
        if (argNames == null) {
            argNames = new String[args.length];
        }

        // Create EvaluatedArguments!
        return new EvaluatedArguments(args, argNames);
    }

    /**
     * @return The names of the arguments, in formal order. 'no name available' is denoted by
     *         <code>null</code>
     */
    public String[] getNames() {
        return names;
    }

    /**
     * @return The already evaluated arguments, in formal order. 'argument missing' is denoted by
     *         <code>null</code>
     */
    public Object[] getEvaluatedArgs() {
        return arguments;
    }
}
