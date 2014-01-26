/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.base;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

@RBuiltin("sub")
@SuppressWarnings("unused")
public abstract class Sub extends RBuiltinNode {

    @Specialization(order = 1)
    public String sub(String pattern, String replacement, String x) {
        return replaceMatch(pattern, replacement, x);
    }

    @Specialization(order = 10)
    public RStringVector sub(String pattern, String replacement, RStringVector vector) {
        return doSub(pattern, replacement, vector);
    }

    @Specialization(order = 12)
    public RStringVector sub(RStringVector pattern, String replacement, RStringVector vector) {
        // FIXME print a warning that only pattern[1] is used
        return doSub(pattern.getDataAt(0), replacement, vector);
    }

    @Specialization(order = 13)
    public RStringVector sub(String pattern, RStringVector replacement, RStringVector vector) {
        // FIXME print a warning that only replacement[1] is used
        return doSub(pattern, replacement.getDataAt(0), vector);
    }

    protected RStringVector doSub(String pattern, String replacement, RStringVector vector) {
        int len = vector.getLength();
        String[] result = new String[len];
        for (int i = 0; i < len; i++) {
            String input = vector.getDataAt(i);
            result[i] = replaceMatch(pattern, replacement, input);
        }
        return RDataFactory.createStringVector(result, vector.isComplete());
    }

    protected String replaceMatch(String pattern, String replacement, String input) {
        return input.replaceFirst(pattern, replacement);
    }
}
