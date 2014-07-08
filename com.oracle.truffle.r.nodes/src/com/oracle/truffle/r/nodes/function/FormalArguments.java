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

import java.util.*;

import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.runtime.data.*;

/**
 * This class denotes a list of {@link FormalArgument}s which consist of the triple
 * <ul>
 * <li>argument name (String, {@link #argumentsNames})</li>
 * <li>expression ({@link RNode}, {@link #defaultArguments})</li>
 * <li>index (int >= 0), implicit here</li>
 * </ul>
 * The order is always the one defined by the function definition.
 */
public class FormalArguments {

    public static final FormalArguments NO_ARGS = new FormalArguments(new String[0], new RNode[0]);

    /**
     * The list of argument names a function definition specifies
     */
    private final String[] argumentsNames;
    /**
     * The list of default arguments a function body specifies. 'No default value' is denoted by
     * {@link RMissing#instance}.
     */
    @SuppressWarnings("unused") private final RNode[] defaultArguments;
    private final ArrayList<FormalArgument> formals;

    /**
     * @param argumentsNames {@link #argumentsNames}
     * @param defaultArguments {@link #defaultArguments}
     */
    public FormalArguments(String[] argumentsNames, RNode[] defaultArguments) {
        assert argumentsNames.length == defaultArguments.length;
        this.argumentsNames = argumentsNames;
        this.defaultArguments = defaultArguments;

        this.formals = new ArrayList<>(argumentsNames.length);
        for (int i = 0; i < argumentsNames.length; i++) {
            formals.add(new FormalArgument(i, defaultArguments[i], argumentsNames[i]));
        }
    }

    /**
     * @param argumentsNames {@link #argumentsNames}
     * @param defaultArguments {@link #defaultArguments}, but handles <code>null</code>
     * @return A fresh {@link FormalArguments}
     */
    public static FormalArguments createHandleNullArgs(String[] argumentsNames, RNode[] defaultArguments) {
        RNode[] newDefaults = new RNode[defaultArguments.length];
        for (int i = 0; i < newDefaults.length; i++) {
            RNode defArg = defaultArguments[i];
            newDefaults[i] = defArg == null ? ConstantNode.create(RMissing.instance) : defArg;
        }
        return new FormalArguments(argumentsNames, newDefaults);
    }

    /**
     * @return {@link #argumentsNames}
     */
    public Object[] getNamesArray() {
        return argumentsNames;
    }

    /**
     * @return {@link #formals}
     */
    public List<FormalArgument> getDefaultValues() {
        return formals;
    }

    /**
     * @return The number of {@link FormalArgument}s in {link #formals}
     */
    public int getNrOfArgs() {
        return formals.size();
    }

    /**
     * @return Whether this function has arguments or not
     */
    public boolean hasArguments() {
        return formals.size() > 0;
    }

    public static class FormalArgument {
        private final int index;
        private final RNode expr;
        private final String name;

        public FormalArgument(int index, RNode expr, String name) {
            this.index = index;
            this.expr = expr;
            this.name = name;
        }

        public int getIndex() {
            return index;
        }

        public RNode getExpr() {
            return expr;
        }

        public String getName() {
            return name;
        }
    }
}
