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

import static com.oracle.truffle.r.nodes.builtin.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.RAttributes.RAttribute;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin(name = "attr", kind = PRIMITIVE)
public abstract class Attr extends RBuiltinNode {

    private static Object searchKeyPartial(RAttributes attributes, String name) {
        Object val = RNull.instance;
        for (RAttribute e : attributes) {
            if (e.getName().startsWith(name)) {
                if (val == RNull.instance) {
                    val = e.getValue();
                } else {
                    // non-unique match
                    return RNull.instance;
                }
            }
        }
        return val;
    }

    @Specialization(order = 1, guards = "!isRowNamesAttr")
    public Object attr(RAbstractContainer container, String name) {
        controlVisibility();
        RAttributes attributes = container.getAttributes();
        if (attributes == null) {
            return RNull.instance;
        } else {
            Object result = attributes.get(name);
            if (result == null) {
                return searchKeyPartial(attributes, name);
            }
            return result;
        }
    }

    public static Object getFullRowNames(Object a) {
        if (a == RNull.instance) {
            return RNull.instance;
        } else {
            RAbstractVector rowNames = (RAbstractVector) a;
            return rowNames.getElementClass() == RInt.class && rowNames.getLength() == 2 && RRuntime.isNA(((RAbstractIntVector) rowNames).getDataAt(0)) ? RDataFactory.createIntSequence(1, 1,
                            Math.abs(((RAbstractIntVector) rowNames).getDataAt(1))) : a;
        }
    }

    @Specialization(order = 2, guards = "isRowNamesAttr")
    public Object attrRowNames(RAbstractContainer container, @SuppressWarnings("unused") String name) {
        controlVisibility();
        RAttributes attributes = container.getAttributes();
        if (attributes == null) {
            return RNull.instance;
        } else {
            return getFullRowNames(container.getRowNames());
        }
    }

    @Specialization(order = 10, guards = {"!emptyName", "isRowNamesAttr"})
    public Object attrRowNames(RAbstractContainer container, RStringVector name) {
        return attrRowNames(container, name.getDataAt(0));
    }

    @Specialization(order = 11, guards = {"!emptyName", "!isRowNamesAttr"})
    public Object attr(RAbstractContainer container, RStringVector name) {
        return attr(container, name.getDataAt(0));
    }

    @SuppressWarnings("unused")
    @Specialization(order = 12, guards = "emptyName")
    public Object attrEmtpyName(RAbstractContainer container, RStringVector name) {
        controlVisibility();
        CompilerDirectives.transferToInterpreter();
        throw RError.getExactlyOneWhich(getEncapsulatingSourceSection());
    }

    protected boolean isRowNamesAttr(@SuppressWarnings("unused") RAbstractContainer container, String name) {
        return name.equals(RRuntime.ROWNAMES_ATTR_KEY);
    }

    protected boolean isRowNamesAttr(RAbstractContainer container, RStringVector name) {
        return isRowNamesAttr(container, name.getDataAt(0));
    }

    protected boolean emptyName(@SuppressWarnings("unused") RAbstractContainer container, RStringVector name) {
        return name.getLength() == 0;
    }

}
