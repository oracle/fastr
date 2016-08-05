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
package com.oracle.truffle.r.runtime;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxElement;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

/**
 * Simple generic base class for pairs of values and signature.
 *
 * @param <T> The type of values
 */
public class Arguments<T> {

    public static Arguments<RNode> create(RNode[] arguments, ArgumentsSignature signature) {
        return new Arguments<>(arguments, signature);
    }

    public static Arguments<RSyntaxNode> create(RSyntaxNode[] arguments, ArgumentsSignature signature) {
        return new Arguments<>(arguments, signature);
    }

    public static Arguments<RSyntaxElement> create(RSyntaxElement[] arguments, ArgumentsSignature signature) {
        return new Arguments<>(arguments, signature);
    }

    /**
     * Array of arguments; semantics have to be specified by child classes.
     */
    @CompilationFinal private final T[] values;

    /**
     * Array of arguments; semantics have to be specified by child classes.
     */
    private final ArgumentsSignature signature;

    protected Arguments(T[] arguments, ArgumentsSignature signature) {
        this.values = arguments;
        this.signature = signature;
    }

    public final ArgumentsSignature getSignature() {
        return signature;
    }

    public final int getLength() {
        return signature.getLength();
    }

    public final T[] getArguments() {
        return values;
    }

    public final T getArgument(int index) {
        return values[index];
    }

    public boolean isEmpty() {
        return signature.isEmpty();
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder().append(getClass().getSimpleName()).append(": ");
        for (int i = 0; i < values.length; i++) {
            str.append(i == 0 ? "" : ", ").append(signature.getName(i)).append(" = ").append(values[i]);
        }
        return str.toString();
    }
}
