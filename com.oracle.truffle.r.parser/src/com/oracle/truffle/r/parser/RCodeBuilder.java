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
package com.oracle.truffle.r.parser;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.runtime.RError;

/**
 * Implementers of this interface can be used to generate a representation of an R closure, with
 * given types as node and argument node types.
 */
public interface RCodeBuilder<T, ArgT> {

    /**
     * Creates a function call argument.
     */
    ArgT argument(SourceSection source, String name, T expression);

    /**
     * Creates an unnamed function call argument.
     */
    ArgT argument(T expression);

    /**
     * Creates an empty function call argument.
     */
    ArgT argumentEmpty();

    /**
     * Helper function: create a call with no arguments.
     */
    default T call(SourceSection source, T lhs) {
        return call(source, lhs, Collections.emptyList());
    }

    /**
     * Helper function: create a call with one unnamed arguments.
     */
    default T call(SourceSection source, T lhs, T argument1) {
        return call(source, lhs, Arrays.asList(argument(argument1)));
    }

    /**
     * Helper function: create a call with two unnamed arguments.
     */
    default T call(SourceSection source, T lhs, T argument1, T argument2) {
        return call(source, lhs, Arrays.asList(argument(argument1), argument(argument2)));
    }

    /**
     * Helper function: create a call with three unnamed arguments.
     */
    default T call(SourceSection source, T lhs, T argument1, T argument2, T argument3) {
        return call(source, lhs, Arrays.asList(argument(argument1), argument(argument2), argument(argument3)));
    }

    /**
     * Create a call with an arbitrary number of named or unnamed arguments.
     */
    T call(SourceSection source, T lhs, List<ArgT> arguments);

    /**
     * Creates a constant, the value is expected to be one of FastR's scalar types (byte, int,
     * double, RComplex, String, RNull).
     */
    T constant(SourceSection source, Object value);

    /**
     * Creates a symbol lookup, either of mode "function" if {@code functionLookup} is true, and
     * "any" otherwise.
     */
    T lookup(SourceSection source, String symbol, boolean functionLookup);

    /**
     * Creates a function expression literal.
     */
    T function(SourceSection source, List<ArgT> arguments, T body);

    /**
     * This function can be used to emit warnings during parsing.
     */
    void warning(RError.Message message, Object... arguments);
}
