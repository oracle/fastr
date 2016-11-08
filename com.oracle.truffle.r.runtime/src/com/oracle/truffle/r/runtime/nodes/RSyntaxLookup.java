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

import com.oracle.truffle.api.source.SourceSection;

/**
 * Represents a symbol lookup in the tree of elements that make up an R closure.
 */
public interface RSyntaxLookup extends RSyntaxElement {

    /**
     * @return The identifier that this lookup represents - this needs to be an interned string.
     */
    String getIdentifier();

    boolean isFunctionLookup();

    /**
     * Helper function: creates a synthetic RSyntaxLookup. The first {@code identifier.length()}
     * characters of the original source section (if non-null) will be used as the new source
     * section.
     */
    static RSyntaxLookup createDummyLookup(SourceSection originalSource, String identifier, boolean isFunctionLookup) {
        SourceSection source = originalSource == null || originalSource.getCharEndIndex() == 0 ? null : originalSource.getSource().createSection(originalSource.getCharIndex(), 1);
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
