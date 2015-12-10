/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates
 *
 * All rights reserved.
 */

package com.oracle.truffle.r.nodes.unary;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.nodes.*;

/**
 * Basic support for "inherits" that is used by the {@code inherits} builtin and others.
 */
@TypeSystemReference(RTypes.class)
public abstract class InheritsNode extends RBaseNode {

    protected final RAttributeProfiles attrProfiles = RAttributeProfiles.create();

    public abstract Object executeObject(Object x, Object what, byte which);

    protected ClassHierarchyNode createClassHierarchy() {
        return ClassHierarchyNodeGen.create(true);
    }

    @Specialization(guards = "!isTrue(which)")
    protected byte doesInherit(Object x, RAbstractStringVector what, @SuppressWarnings("unused") byte which, @Cached("createClassHierarchy()") ClassHierarchyNode classHierarchy) {
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

    @Specialization(guards = "isTrue(which)")
    protected RIntVector doesInheritWhich(Object x, RAbstractStringVector what, @SuppressWarnings("unused") byte which, @Cached("createClassHierarchy()") ClassHierarchyNode classHierarchy) {
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

    protected static boolean isTrue(byte value) {
        return RRuntime.fromLogical(value);
    }
}
