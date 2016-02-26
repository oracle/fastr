/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.r.runtime.RInternalError;

/**
 * This abstract can be used to write visitors over {@link RSyntaxElement}s. For an example see
 * {@link RSyntaxUtils#verifyFunction(String, com.oracle.truffle.r.runtime.ArgumentsSignature, RSyntaxNode[], RSyntaxNode)}
 * .
 */
public abstract class RSyntaxArgVisitor<T, ArgT> {

    /**
     * Call this function to process child nodes.
     */
    public final T accept(RSyntaxElement element, ArgT arg) {
        if (element instanceof RSyntaxCall) {
            return visit((RSyntaxCall) element, arg);
        } else if (element instanceof RSyntaxConstant) {
            return visit((RSyntaxConstant) element, arg);
        } else if (element instanceof RSyntaxLookup) {
            return visit((RSyntaxLookup) element, arg);
        } else if (element instanceof RSyntaxFunction) {
            return visit((RSyntaxFunction) element, arg);
        } else {
            throw RInternalError.shouldNotReachHere("unexpected RSyntaxElement: " + element.getClass().getCanonicalName());
        }
    }

    protected abstract T visit(RSyntaxCall element, ArgT arg);

    protected abstract T visit(RSyntaxConstant element, ArgT arg);

    protected abstract T visit(RSyntaxLookup element, ArgT arg);

    protected abstract T visit(RSyntaxFunction element, ArgT arg);

}
