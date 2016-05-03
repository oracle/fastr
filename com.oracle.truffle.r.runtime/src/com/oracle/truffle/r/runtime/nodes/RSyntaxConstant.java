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
 * Represents a constant (logical, character, integer, double, complex, NULL or missing/empty) in
 * the tree of elements that make up an R closure.
 */
public interface RSyntaxConstant extends RSyntaxElement {

    Object getValue();

    /**
     * Helper function: creates a synthetic RSyntaxConstant.
     */
    static RSyntaxConstant createDummyConstant(SourceSection originalSource, Object value) {
        return new RSyntaxConstant() {
            @Override
            public SourceSection getSourceSection() {
                return originalSource;
            }

            @Override
            public Object getValue() {
                return value;
            }

            @Override
            public void setSourceSection(SourceSection src) {
                // ignored
            }
        };
    }

    /**
     * Helper function: extract an integer constant from an RSyntaxElement.
     */
    static Integer asIntConstant(RSyntaxElement argument, boolean castFromDouble) {
        if (argument instanceof RSyntaxConstant) {
            Object value = ((RSyntaxConstant) argument).getValue();
            if (value instanceof Integer) {
                return (int) value;
            } else if (castFromDouble && value instanceof Double) {
                return (int) (double) value;
            }
        }
        return null;
    }
}
