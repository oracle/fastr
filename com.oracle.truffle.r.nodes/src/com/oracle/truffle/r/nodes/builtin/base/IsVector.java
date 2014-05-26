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

import static com.oracle.truffle.r.nodes.builtin.RBuiltinKind.*;

import java.text.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.SlowPath;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.sun.tools.javac.util.*;

@RBuiltin(name = "is.vector", kind = PRIMITIVE)
@SuppressWarnings("unused")
public abstract class IsVector extends RBuiltinNode {

    @Specialization(order = 1)
    public byte isNull(RNull operand, Object mode) {
        return RRuntime.LOGICAL_FALSE;
    }

    @Specialization(order = 10)
    public byte isNull(RDataFrame operand, String mode) {
        return RRuntime.LOGICAL_FALSE;
    }

    @Specialization(order = 11)
    public byte isNull(RDataFrame operand, RMissing mode) {
        return RRuntime.LOGICAL_FALSE;
    }

    @Specialization(order = 100, guards = {"isVectorInt", "namesOnlyOrNoAttr"})
    public byte isInt(RAbstractVector x, String mode) {
        controlVisibility();
        return RRuntime.LOGICAL_TRUE;
    }

    @Specialization(order = 200, guards = {"isVectorDouble", "namesOnlyOrNoAttr"})
    public byte isDouble(RAbstractVector x, String mode) {
        controlVisibility();
        return RRuntime.LOGICAL_TRUE;
    }

    @Specialization(order = 300, guards = {"isVectorComplex", "namesOnlyOrNoAttr"})
    public byte isComplex(RAbstractVector x, String mode) {
        controlVisibility();
        return RRuntime.LOGICAL_TRUE;
    }

    @Specialization(order = 400, guards = {"isVectorLogical", "namesOnlyOrNoAttr"})
    public byte isLogical(RAbstractVector x, String mode) {
        controlVisibility();
        return RRuntime.LOGICAL_TRUE;
    }

    @Specialization(order = 500, guards = {"isVectorString", "namesOnlyOrNoAttr"})
    public byte isString(RAbstractVector x, String mode) {
        controlVisibility();
        return RRuntime.LOGICAL_TRUE;
    }

    @Specialization(order = 600, guards = {"isVectorRaw", "namesOnlyOrNoAttr"})
    public byte isRaw(RAbstractVector x, String mode) {
        controlVisibility();
        return RRuntime.LOGICAL_TRUE;
    }

    @Specialization(order = 700, guards = {"isVectorList", "namesOnlyOrNoAttr"})
    public byte isList(RAbstractVector x, String mode) {
        controlVisibility();
        return RRuntime.LOGICAL_TRUE;
    }

    @Specialization(order = 900, guards = "!namesOnlyOrNoAttr")
    public byte isVectorAttr(RAbstractVector x, String mode) {
        controlVisibility();
        return RRuntime.LOGICAL_FALSE;
    }

    @Specialization(order = 1000, guards = "namesOnlyOrNoAttr")
    public byte isVector(RAbstractVector x, RMissing mode) {
        controlVisibility();
        return RRuntime.LOGICAL_TRUE;
    }

    @Specialization(order = 1001, guards = "!namesOnlyOrNoAttr")
    public byte isVectorAttr(RAbstractVector x, RMissing mode) {
        controlVisibility();
        return RRuntime.LOGICAL_FALSE;
    }

    @Specialization(order = 1100, guards = "!modeIsAnyOrMatches")
    public byte isNotVector(RAbstractVector x, String mode) {
        controlVisibility();
        return RRuntime.LOGICAL_FALSE;
    }

    protected static boolean namesOnlyOrNoAttrInternal(RAbstractVector x, Object mode) {
        // there should be no attributes other than names
        if (x.getNames() == RNull.instance) {
            assert x.getAttributes() == null || x.getAttributes().size() > 0;
            return x.getAttributes() == null ? true : false;
        } else {
            assert x.getAttributes() != null;
            return x.getAttributes().size() == 1 ? true : false;
        }
    }

    protected boolean namesOnlyOrNoAttr(RAbstractVector x, String mode) {
        return namesOnlyOrNoAttrInternal(x, mode);
    }

    protected boolean namesOnlyOrNoAttr(RAbstractVector x, RMissing mode) {
        return namesOnlyOrNoAttrInternal(x, mode);
    }

    protected boolean isVectorInt(RAbstractVector x, String mode) {
        return x.getElementClass() == RInt.class && RRuntime.TYPE_INTEGER.equals(mode);
    }

    protected boolean isVectorDouble(RAbstractVector x, String mode) {
        return x.getElementClass() == RDouble.class && (RRuntime.TYPE_NUMERIC.equals(mode) || RRuntime.TYPE_DOUBLE.equals(mode));
    }

    protected boolean isVectorComplex(RAbstractVector x, String mode) {
        return x.getElementClass() == RComplex.class && RRuntime.TYPE_COMPLEX.equals(mode);
    }

    protected boolean isVectorLogical(RAbstractVector x, String mode) {
        return x.getElementClass() == RLogical.class && RRuntime.TYPE_LOGICAL.equals(mode);
    }

    protected boolean isVectorString(RAbstractVector x, String mode) {
        return x.getElementClass() == RString.class && RRuntime.TYPE_CHARACTER.equals(mode);
    }

    protected boolean isVectorRaw(RAbstractVector x, String mode) {
        return x.getElementClass() == RRaw.class && RRuntime.TYPE_RAW.equals(mode);
    }

    protected boolean isVectorList(RAbstractVector x, String mode) {
        return x.getElementClass() == Object.class && mode.equals("list");
    }

    protected boolean modeIsAnyOrMatches(RAbstractVector x, String mode) {
        return RRuntime.TYPE_ANY.equals(mode) || RRuntime.classToString(x.getElementClass()).equals(mode) || x.getElementClass() == RDouble.class && RRuntime.TYPE_DOUBLE.equals(mode);
    }

}
