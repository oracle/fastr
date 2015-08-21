/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.profile;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.*;

public final class CountedLoopConditionProfile {

    @CompilationFinal private int count;
    @CompilationFinal private int sum;

    public void profileLength(int length) {
        if (CompilerDirectives.inInterpreter()) {
            sum += length;
            count++;
        }
    }

    public boolean inject(boolean condition) {
        if (CompilerDirectives.inCompiledCode()) {
            double averageLength = (double) sum / (double) count;
            return CompilerDirectives.injectBranchProbability(averageLength / (averageLength + 1.0), condition);
        } else {
            return condition;
        }
    }

    public static CountedLoopConditionProfile create() {
        return new CountedLoopConditionProfile();
    }
}
