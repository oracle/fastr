/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.nodes;

import java.util.Arrays;

import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RVisibility;
import com.oracle.truffle.r.runtime.builtins.FastPathFactory;
import com.oracle.truffle.r.runtime.context.RContext;

final class EvaluatedArgumentsFastPath implements FastPathFactory {

    private final boolean[] forcedArguments;

    EvaluatedArgumentsFastPath(boolean[] forcedArguments) {
        this.forcedArguments = forcedArguments;
    }

    @Override
    public RFastPathNode create() {
        return null;
    }

    @Override
    public RVisibility getVisibility() {
        return null;
    }

    @Override
    public boolean evaluatesArgument(int index) {
        return false;
    }

    @Override
    public boolean forcedEagerPromise(int index) {
        return RContext.getInstance().noEagerEvalOption() ? false : forcedArguments[index];
    }

    public String toString(ArgumentsSignature signature) {
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < signature.getLength(); i++) {
            if (forcedArguments[i]) {
                str.append(signature.getName(i)).append(' ');
            }
        }
        return str.toString();
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(forcedArguments);
    }

    @Override
    public boolean equals(Object obj) {
        EvaluatedArgumentsFastPath other = (EvaluatedArgumentsFastPath) obj;
        return forcedArguments == null && other.forcedArguments == null || forcedArguments != null && other.forcedArguments != null && Arrays.equals(forcedArguments, other.forcedArguments);
    }
}
