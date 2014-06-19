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

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import com.oracle.truffle.api.CompilerDirectives.SlowPath;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

@RBuiltin(name = "toupper", kind = SUBSTITUTE)
// TODO INTERNAL
public abstract class ToUpper extends RBuiltinNode {

    @Specialization
    @SlowPath
    public String toUpper(String value) {
        controlVisibility();
        return value.toUpperCase();
    }

    @Specialization
    public RStringVector toUpper(RStringVector vector) {
        controlVisibility();
        String[] stringVector = new String[vector.getLength()];
        for (int i = 0; i < vector.getLength(); i++) {
            stringVector[i] = toUpper(vector.getDataAt(i));
        }
        RStringVector res = RDataFactory.createStringVector(stringVector, vector.isComplete());
        res.copyRegAttributesFrom(vector);
        return res;
    }

    @SuppressWarnings("unused")
    @Specialization
    public RStringVector toupper(RNull empty) {
        controlVisibility();
        return RDataFactory.createStringVector(0);
    }
}
