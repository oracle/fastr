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

import com.oracle.truffle.r.nodes.*;

/**
 * Base class that represents a list of argument/name pairs with some convenience methods. Semantics
 * of {@link #arguments} and {@link #names} have to be defined by subclasses!
 */
public abstract class ArgumentsNode extends RNode implements ArgumentsTrait {

    /**
     * A list of arguments. Single arguments may be <code>null</code>; semantics have to be
     * specified by implementing classes
     */
    @Children protected final RNode[] arguments;

    /**
     * A list of arguments. Single names may be <code>null</code>; semantics have to be specified by
     * implementing classes
     */
    protected final String[] names;

    /**
     * The number of {@link #names} given (i.e., not <code>null</code>).
     *
     * @see ArgumentsTrait#countNonNull(String[])
     */
    private final int nameCount;

    protected ArgumentsNode(RNode[] arguments, String[] names) {
        super();
        this.arguments = arguments;
        this.names = names;
        this.nameCount = ArgumentsTrait.countNonNull(names);
    }

    /**
     * @return {@link #arguments}
     */
    public RNode[] getArguments() {
        return arguments;
    }

    /**
     * @return {@link #names}
     */
    public String[] getNames() {
        return names;
    }

    /**
     * @return {@link #nameCount}
     */
    public int getNameCount() {
        return nameCount;
    }
}
