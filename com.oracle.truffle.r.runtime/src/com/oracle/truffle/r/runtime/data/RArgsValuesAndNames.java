/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.data;

import java.util.*;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.r.runtime.*;

/**
 * A simple wrapper class for passing the ... argument through RArguments
 */
public class RArgsValuesAndNames {
    /**
     * Default instance for empty "..." ("..." that resolve to contain no expression at runtime).
     * The {@link RMissing#instance} for "...".
     */
    public static final RArgsValuesAndNames EMPTY = new RArgsValuesAndNames(new Object[0], ArgumentsSignature.empty(0));

    @CompilationFinal private final Object[] values;
    private final ArgumentsSignature signature;

    public RArgsValuesAndNames(Object[] values, ArgumentsSignature signature) {
        assert signature != null && signature.getLength() == values.length : Arrays.toString(values) + " " + signature;
        this.values = values;
        this.signature = signature;
    }

    public Object[] getValues() {
        return values;
    }

    public ArgumentsSignature getSignature() {
        return signature;
    }

    public int length() {
        return values.length;
    }

    /**
     * @return The same as {@link #isMissing()}, kept for semantic context.
     */
    public boolean isEmpty() {
        return length() == 0;
    }

    /**
     * @return The same as {@link #isEmpty()}, kept for semantic context.
     */
    public boolean isMissing() {
        return length() == 0;
    }
}
