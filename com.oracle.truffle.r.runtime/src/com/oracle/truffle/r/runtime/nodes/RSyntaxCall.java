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
 * Represents a call with the given left hand side and arguments in the tree of elements that make
 * up an R closure.
 */
public interface RSyntaxCall extends RSyntaxElement {

    RSyntaxElement getSyntaxLHS();

    ArgumentsSignature getSyntaxSignature();

    RSyntaxElement[] getSyntaxArguments();

    /**
     * Helper function: creates a synthetic RSyntaxCall.
     */
    static RSyntaxCall createDummyCall(SourceSection originalSource, RSyntaxElement lhs, ArgumentsSignature signature, RSyntaxElement[] arguments) {
        return new RSyntaxCall() {
            public SourceSection getSourceSection() {
                return originalSource;
            }

            public RSyntaxElement getSyntaxLHS() {
                return lhs;
            }

            public ArgumentsSignature getSyntaxSignature() {
                return signature;
            }

            public RSyntaxElement[] getSyntaxArguments() {
                return arguments;
            }

            public void setSourceSection(SourceSection src) {
                // ignored
            }
        };
    }

    static boolean isCallTo(RSyntaxElement element, String functionName) {
        if (element instanceof RSyntaxCall) {
            RSyntaxCall call = (RSyntaxCall) element;
            if (call.getSyntaxLHS() instanceof RSyntaxLookup) {
                RSyntaxLookup lookup = (RSyntaxLookup) call.getSyntaxLHS();
                return functionName.equals(lookup.getIdentifier());
            }
        }
        return false;
    }
}
