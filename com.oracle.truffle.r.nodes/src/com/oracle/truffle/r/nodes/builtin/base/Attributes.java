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

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.RAttributes.RAttribute;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin("attributes")
public abstract class Attributes extends RBuiltinNode {

    @Specialization(guards = "!hasAttributes")
    public RNull attributesNull(@SuppressWarnings("unused") RAbstractVector vector) {
        controlVisibility();
        return RNull.instance;
    }

    @Specialization(guards = "hasAttributes")
    public RList attributes(RAbstractContainer container) {
        controlVisibility();
        RAttributes attributes = container.getAttributes();
        int size = attributes.size();
        String[] names = new String[size];
        Object[] values = new Object[size];
        int z = 0;
        for (RAttribute attr : attributes) {
            names[z] = attr.getName();
            if (names[z] == RRuntime.ROWNAMES_ATTR_KEY) {
                values[z] = Attr.getFullRowNames(attr.getValue());
            } else {
                values[z] = attr.getValue();
            }
            ++z;
        }
        RList result = RDataFactory.createList(values);
        result.setNames(RDataFactory.createStringVector(names, true));
        return result;
    }

    public static boolean hasAttributes(RAbstractContainer container) {
        return container.getAttributes() != null && container.getAttributes().size() > 0;
    }
}
