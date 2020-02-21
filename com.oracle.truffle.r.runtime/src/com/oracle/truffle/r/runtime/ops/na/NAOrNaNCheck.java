/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.runtime.ops.na;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.nodes.NodeCloneable;

/**
 * Specialized variant of {@link NACheck} for NA or NaN checks that do not require the other
 * machinery of {@link NACheck}.
 */
public final class NAOrNaNCheck extends NodeCloneable {
    private static final NAOrNaNCheck DISABLED = new NAOrNaNCheck(true);
    @CompilationFinal boolean seenNaN;

    private NAOrNaNCheck(boolean seenNaN) {
        this.seenNaN = seenNaN;
    }

    public static NAOrNaNCheck create() {
        return new NAOrNaNCheck(false);
    }

    public static NAOrNaNCheck getUncached() {
        return DISABLED;
    }

    public boolean checkNAorNaN(double value) {
        if (Double.isNaN(value)) {
            if (!this.seenNaN) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                this.seenNaN = true;
            }
            return true;
        }
        return false;
    }

    public boolean seenNAorNaN() {
        return seenNaN;
    }
}
