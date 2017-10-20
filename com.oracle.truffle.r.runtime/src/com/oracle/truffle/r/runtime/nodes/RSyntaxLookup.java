/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.runtime.Utils;

/**
 * Represents a symbol lookup in the tree of elements that make up an R closure.
 */
public interface RSyntaxLookup extends RSyntaxElement {

    /**
     * If the given identifier identifies a variadic argument component (i.e. identifier in form of
     * {@code ..X} or {@code .XY} where X and Y are digits), then this method returns the numeric
     * part of the identifier, otherwise {@code -1}.
     */
    static int getVariadicComponentIndex(String symbol) {
        int len = symbol.length();
        if (len > 1 && symbol.charAt(0) == '.') {
            int start = len > 2 && symbol.charAt(1) == '.' ? 2 : 1;
            int result = 0;
            for (int i = start; i < len; i++) {
                int digit = symbol.charAt(i) - '\u0030';
                if (digit < 0 || digit > 9) {
                    return -1;
                }
                result = result * 10 + digit;
            }
            return result;
        }
        return -1;
    }

    /**
     * @return The identifier that this lookup represents - this needs to be an interned string.
     */
    String getIdentifier();

    boolean isFunctionLookup();

    /**
     * Distinguishes between actual syntax nodes and disguised syntax node used as promise
     * expression for unrolled varargs. Support needed for the substitute builtin.
     */
    default boolean isPromiseLookup() {
        return false;
    }

    /**
     * Helper function: creates a synthetic RSyntaxLookup. The first {@code identifier.length()}
     * characters of the original source section (if non-null) will be used as the new source
     * section.
     */
    static RSyntaxLookup createDummyLookup(SourceSection source, String identifier, boolean isFunctionLookup) {
        return new RSyntaxLookup() {
            @Override
            public SourceSection getLazySourceSection() {
                return source;
            }

            @Override
            public SourceSection getSourceSection() {
                return source;
            }

            @Override
            public String getIdentifier() {
                assert Utils.isInterned(identifier);
                return identifier;
            }

            @Override
            public boolean isFunctionLookup() {
                return isFunctionLookup;
            }

            @Override
            public void setSourceSection(SourceSection src) {
                // ignored
            }

            @Override
            public String toString() {
                return "`" + identifier + "`";
            }
        };
    }
}
