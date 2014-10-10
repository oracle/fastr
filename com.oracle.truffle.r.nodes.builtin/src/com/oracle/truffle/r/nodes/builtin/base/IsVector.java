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
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin(name = "is.vector", kind = PRIMITIVE, parameterNames = {"x", "mode"})
public abstract class IsVector extends RBuiltinNode {

    @Override
    public RNode[] getParameterValues() {
        // x, mode = "any"
        return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(RType.Any.getName())};
    }

    @Specialization
    protected byte isType(@SuppressWarnings("unused") RMissing value, @SuppressWarnings("unused") String mode) {
        controlVisibility();
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.ARGUMENT_MISSING, "x");
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"namesOnlyOrNoAttr", "modeIsAnyOrMatches"})
    protected byte isList(RAbstractVector x, String mode) {
        controlVisibility();
        return RRuntime.LOGICAL_TRUE;
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"namesOnlyOrNoAttr", "!modeIsAnyOrMatches"})
    protected byte isNotVector(RAbstractVector x, String mode) {
        controlVisibility();
        return RRuntime.LOGICAL_FALSE;
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "!namesOnlyOrNoAttr")
    protected byte isVectorAttr(RAbstractVector x, String mode) {
        controlVisibility();
        return RRuntime.LOGICAL_FALSE;
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "namesOnlyOrNoAttr")
    protected byte isVector(RAbstractVector x, RMissing mode) {
        controlVisibility();
        return RRuntime.LOGICAL_TRUE;
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "!namesOnlyOrNoAttr")
    protected byte isVectorAttr(RAbstractVector x, RMissing mode) {
        controlVisibility();
        return RRuntime.LOGICAL_FALSE;
    }

    @SuppressWarnings("unused")
    @Fallback
    protected byte isVector(Object x, Object mode) {
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
        return RType.Any.getName().equals(mode) || RRuntime.classToString(x.getElementClass()).equals(mode) || (x.getElementClass() == RDouble.class && RType.Double.getName().equals(mode)) ||
                        (x.getElementClass() == Object.class && mode.equals("list"));
    }

}
