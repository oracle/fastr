/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.text.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.SlowPath;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.Node.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin("row.names")
@SuppressWarnings("unused")
public abstract class RowNames extends RBuiltinNode {

    @Child private CastStringNode castString;

    // from GNU R's documentation:
    // "for backwards compatibility (with R <= 2.4.0) row.names will always return a character vector"
    private Object castString(VirtualFrame frame, Object operand) {
        if (castString == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castString = insert(CastStringNodeFactory.create(null, false, false, false, false));
        }
        return castString.executeCast(frame, operand);
    }

    @Specialization
    public RNull getNames(RNull vector) {
        controlVisibility();
        return RNull.instance;
    }

    @Specialization
    public RNull getNames(byte operand) {
        controlVisibility();
        return RNull.instance;
    }

    @Specialization
    public RNull getNames(int operand) {
        controlVisibility();
        return RNull.instance;
    }

    @Specialization
    public RNull getNames(double operand) {
        controlVisibility();
        return RNull.instance;
    }

    @Specialization
    public RNull getNames(RComplex operand) {
        controlVisibility();
        return RNull.instance;
    }

    @Specialization
    public RNull getNames(String operand) {
        controlVisibility();
        return RNull.instance;
    }

    @Specialization
    public RNull getNames(RRaw operand) {
        controlVisibility();
        return RNull.instance;
    }

    @Specialization
    public RNull getNames(RFunction function) {
        controlVisibility();
        return RNull.instance;
    }

    @Specialization(guards = "!hasDimNames")
    public RNull getEmptyNames(RAbstractVector vector) {
        controlVisibility();
        return RNull.instance;
    }

    @Specialization(guards = "hasDimNames")
    public Object getNames(VirtualFrame frame, RAbstractVector vector) {
        controlVisibility();
        return castString(frame, vector.getDimNames().getDataAt(0));
    }

    @Specialization(guards = "!hasNames")
    public RStringVector getEmptyNames(RDataFrame vector) {
        controlVisibility();
        return RDataFactory.createEmptyStringVector();
    }

    @Specialization(guards = "hasNames")
    public Object getNames(VirtualFrame frame, RAbstractContainer vector) {
        controlVisibility();
        return castString(frame, vector.getRowNames());
    }

    public static boolean hasDimNames(RAbstractVector vector) {
        return vector.getDimNames() != null;
    }

    public static boolean hasNames(RAbstractContainer container) {
        return container.getRowNames() != RNull.instance;
    }

}
