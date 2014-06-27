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

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin(name = "is.vector", kind = PRIMITIVE)
public abstract class IsVector extends RBuiltinNode {

    @SuppressWarnings("unused")
    @Specialization(order = 1)
    public byte isNull(RNull operand, Object mode) {
        return RRuntime.LOGICAL_FALSE;
    }

    @SuppressWarnings("unused")
    @Specialization(order = 10)
    public byte isNull(RDataFrame operand, Object mode) {
        return RRuntime.LOGICAL_FALSE;
    }

    @SuppressWarnings("unused")
    @Specialization(order = 50, guards = {"namesOnlyOrNoAttr", "modeIsAnyOrMatches"})
    public byte isList(RAbstractVector x, String mode) {
        controlVisibility();
        return RRuntime.LOGICAL_TRUE;
    }

    @SuppressWarnings("unused")
    @Specialization(order = 51, guards = {"namesOnlyOrNoAttr", "!modeIsAnyOrMatches"})
    public byte isNotVector(RAbstractVector x, String mode) {
        controlVisibility();
        return RRuntime.LOGICAL_FALSE;
    }

    @SuppressWarnings("unused")
    @Specialization(order = 52, guards = "!namesOnlyOrNoAttr")
    public byte isVectorAttr(RAbstractVector x, String mode) {
        controlVisibility();
        return RRuntime.LOGICAL_FALSE;
    }

    @SuppressWarnings("unused")
    @Specialization(order = 1000, guards = "namesOnlyOrNoAttr")
    public byte isVector(RAbstractVector x, RMissing mode) {
        controlVisibility();
        return RRuntime.LOGICAL_TRUE;
    }

    @SuppressWarnings("unused")
    @Specialization(order = 1001, guards = "!namesOnlyOrNoAttr")
    public byte isVectorAttr(RAbstractVector x, RMissing mode) {
        controlVisibility();
        return RRuntime.LOGICAL_FALSE;
    }

    protected static boolean namesOnlyOrNoAttrInternal(RAbstractVector x, @SuppressWarnings("unused") Object mode) {
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

    protected boolean modeIsAnyOrMatches(RAbstractVector x, String mode) {
        return RRuntime.TYPE_ANY.equals(mode) || RRuntime.classToString(x.getElementClass()).equals(mode) || (x.getElementClass() == RDouble.class && RRuntime.TYPE_DOUBLE.equals(mode)) ||
                        (x.getElementClass() == Object.class && mode.equals("list"));
    }

}
