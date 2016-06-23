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
package com.oracle.truffle.r.runtime.nodes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.runtime.ArgumentsSignature;

/**
 * Implementers of this interface can be used to generate a representation of an R closure.
 */
public interface RCodeBuilder<T> {

    final class Argument<T> {
        public final SourceSection source;
        public final String name;
        public final T value;

        private Argument(SourceSection source, String name, T value) {
            this.source = source;
            this.name = name;
            this.value = value;
        }
    }

    /**
     * Creates a function call argument.
     */
    static <T> Argument<T> argument(SourceSection source, String name, T expression) {
        return new Argument<>(source, name, expression);
    }

    /**
     * Creates an unnamed function call argument.
     */
    static <T> Argument<T> argument(T expression) {
        return new Argument<>(null, null, expression);
    }

    /**
     * Creates an empty function call argument.
     */
    static <T> Argument<T> argumentEmpty() {
        return new Argument<>(null, null, null);
    }

    /**
     * Create a call with an arbitrary number of named or unnamed arguments.
     */
    T call(SourceSection source, T lhs, List<Argument<T>> arguments);

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
     * Creates a function expression literal. {@code assignedTo} can be used to determine function
     * names - if it is non-null, it represents the left hand side that this function is assigned
     * to.
     */
    T function(SourceSection source, List<Argument<T>> arguments, T body, Object assignedTo);

    /**
     * Creates a new call target from a given function expression literal.
     */
    RootCallTarget rootFunction(SourceSection source, List<Argument<T>> arguments, T body, String name);

    /**
     * This method returns a newly created AST fragment for the given original element. This
     * functionality can be used to quickly create new AST snippets for existing code.
     */
    default T process(RSyntaxElement original) {
        return new RSyntaxVisitor<T>() {

            @Override
            protected T visit(RSyntaxCall element) {
                ArrayList<Argument<T>> args = createArguments(element.getSyntaxSignature(), element.getSyntaxArguments());
                return call(element.getSourceSection(), accept(element.getSyntaxLHS()), args);
            }

            private ArrayList<Argument<T>> createArguments(ArgumentsSignature signature, RSyntaxElement[] arguments) {
                ArrayList<Argument<T>> args = new ArrayList<>(arguments.length);
                for (int i = 0; i < arguments.length; i++) {
                    args.add(RCodeBuilder.argument(arguments[i] == null ? null : arguments[i].getSourceSection(), signature.getName(i), arguments[i] == null ? null : accept(arguments[i])));
                }
                return args;
            }

            @Override
            protected T visit(RSyntaxConstant element) {
                return constant(element.getSourceSection(), element.getValue());
            }

            @Override
            protected T visit(RSyntaxLookup element) {
                return lookup(element.getSourceSection(), element.getIdentifier(), element.isFunctionLookup());
            }

            @Override
            protected T visit(RSyntaxFunction element) {
                ArrayList<Argument<T>> params = createArguments(element.getSyntaxSignature(), element.getSyntaxArgumentDefaults());
                return function(element.getSourceSection(), params, accept(element.getSyntaxBody()), element.getSyntaxDebugName());
            }
        }.accept(original);
    }

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
}
