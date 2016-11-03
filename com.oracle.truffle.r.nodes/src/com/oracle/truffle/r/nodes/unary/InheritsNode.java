/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates
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
