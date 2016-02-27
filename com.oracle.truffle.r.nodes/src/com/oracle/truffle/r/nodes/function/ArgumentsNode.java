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

import java.util.*;

import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.nodes.*;
import com.oracle.truffle.r.runtime.nodes.instrument.NeedsWrapper;

/**
 * Base class that represents a list of argument/name pairs with some convenience methods. Semantics
 * of {@link #arguments} and the signature have to be defined by subclasses!
 */
public abstract class ArgumentsNode extends RNode implements UnmatchedArguments {

    /**
     * A list of arguments. Single arguments may be <code>null</code>; semantics have to be
     * specified by implementing classes
     */
    @Children protected final RNode[] arguments;

    protected final ArgumentsSignature signature;

    protected ArgumentsNode(RNode[] arguments, ArgumentsSignature signature) {
        assert signature != null && signature.getLength() == arguments.length : Arrays.toString(arguments) + " " + signature;
        this.arguments = arguments;
        this.signature = signature;
        assert signature != null;
    }

    @NeedsWrapper
    public RNode[] getArguments() {
        return arguments;
    }

    @NeedsWrapper
    public ArgumentsSignature getSignature() {
        return signature;
    }

    /**
     * No-arg constructor for subclass wrapper.
     */
    protected ArgumentsNode() {
        arguments = new RNode[0];
        signature = null;
    }
}
