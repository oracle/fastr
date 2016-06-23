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
import com.oracle.truffle.r.runtime.ArgumentsSignature;

/**
 * Represents a function expression with the given body and arguments in the tree of elements that
 * make up an R closure.
 */
public interface RSyntaxFunction extends RSyntaxElement {

    ArgumentsSignature getSyntaxSignature();

    RSyntaxElement[] getSyntaxArgumentDefaults();

    RSyntaxElement getSyntaxBody();

    String getSyntaxDebugName();

    /**
     * Helper function: creates a synthetic RSyntaxFunction.
     */
    static RSyntaxFunction createDummyFunction(SourceSection originalSource, ArgumentsSignature signature, RSyntaxElement[] arguments, RSyntaxElement body, String debugName) {
        return new RSyntaxFunction() {
            @Override
            public SourceSection getSourceSection() {
                return originalSource;
            }

            @Override
            public ArgumentsSignature getSyntaxSignature() {
                return signature;
            }

            @Override
            public RSyntaxElement[] getSyntaxArgumentDefaults() {
                return arguments;
            }

            @Override
            public RSyntaxElement getSyntaxBody() {
                return body;
            }

            @Override
            public void setSourceSection(SourceSection src) {
                // ignored
            }

            @Override
            public String getSyntaxDebugName() {
                return debugName;
            }
        };
    }
}
