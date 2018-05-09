/*
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates
 *
 * All rights reserved.
 */

package com.oracle.truffle.r.nodes.unary;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.r.nodes.function.ClassHierarchyNode;
import com.oracle.truffle.r.nodes.function.ClassHierarchyNodeGen;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RTypes;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

/**
 * Basic support for "inherits" that is used by the {@code inherits} builtin and others.
 */
@TypeSystemReference(RTypes.class)
public abstract class InheritsNode extends RBaseNode {

    public abstract Object execute(Object x, RAbstractStringVector what, boolean which);

    protected ClassHierarchyNode createClassHierarchy() {
        return ClassHierarchyNodeGen.create(true, true);
    }

    @Specialization(guards = "!which")
    protected byte doesInherit(Object x, RAbstractStringVector what, @SuppressWarnings("unused") boolean which,
                    @Cached("createClassHierarchy()") ClassHierarchyNode classHierarchy) {
        RStringVector hierarchy = classHierarchy.execute(x);
        for (int i = 0; i < what.getLength(); i++) {
            String whatString = what.getDataAt(i);
            for (int j = 0; j < hierarchy.getLength(); j++) {
                if (whatString.equals(hierarchy.getDataAt(j))) {
                    return RRuntime.LOGICAL_TRUE;
                }
            }
        }
        return RRuntime.LOGICAL_FALSE;
    }

    @Specialization(guards = "which")
    protected RIntVector doesInheritWhich(Object x, RAbstractStringVector what, @SuppressWarnings("unused") boolean which,
                    @Cached("createClassHierarchy()") ClassHierarchyNode classHierarchy) {
        RStringVector hierarchy = classHierarchy.execute(x);
        int[] data = new int[what.getLength()];
        for (int i = 0; i < what.getLength(); i++) {
            String whatString = what.getDataAt(i);
            for (int j = 0; j < hierarchy.getLength(); j++) {
                if (whatString.equals(hierarchy.getDataAt(j))) {
                    data[i] = j + 1;
                    break;
                }
            }
        }
        return RDataFactory.createIntVector(data, RDataFactory.COMPLETE_VECTOR);
    }
}
