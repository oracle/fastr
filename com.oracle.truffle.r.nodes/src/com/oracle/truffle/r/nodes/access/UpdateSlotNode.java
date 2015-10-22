/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995, 1996, 1997  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1995-2014, The R Core Team
 * Copyright (c) 2002-2008, The R Foundation
 * Copyright (c) 2015, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.access;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.attributes.PutAttributeNode;
import com.oracle.truffle.r.nodes.attributes.PutAttributeNodeGen;
import com.oracle.truffle.r.nodes.function.ClassHierarchyNode;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.nodes.*;

@NodeChildren({@NodeChild(value = "object", type = RNode.class), @NodeChild(value = "name", type = RNode.class), @NodeChild(value = "value", type = RNode.class)})
public abstract class UpdateSlotNode extends RNode {

    public abstract Object executeUpdate(Object object, String name, Object value);

    @CompilationFinal RFunction checkSlotAssign;
    @Child private ClassHierarchyNode objClassHierarchy;
    @Child private ClassHierarchyNode valClassHierarchy;

    protected PutAttributeNode createAttrUpdate(String name) {
        return PutAttributeNodeGen.create(name);
    }

    private static Object getActualValue(Object value) {
        if (value == RNull.instance) {
            return RRuntime.NULL_STR_VECTOR;
        } else {
            return value;
        }
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "name == cachedName")
    protected Object updateSlotS4Cached(RS4Object object, String name, Object value, @Cached("name") String cachedName, @Cached("createAttrUpdate(cachedName)") PutAttributeNode attributeUpdate) {
        Object actualValue = getActualValue(value);
        attributeUpdate.execute(object.initAttributes(), actualValue);
        return object;
    }

    @Specialization(contains = "updateSlotS4Cached")
    protected Object updateSlotS4(RS4Object object, String name, Object value) {
        Object actualValue = getActualValue(value);
        object.setAttr(name.intern(), actualValue);
        return object;
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "name == cachedName")
    protected Object updateSlotCached(RAbstractContainer object, String name, Object value, @Cached("name") String cachedName, @Cached("createAttrUpdate(cachedName)") PutAttributeNode attributeUpdate) {
        Object actualValue = getActualValue(value);
        attributeUpdate.execute(object.initAttributes(), actualValue);
        return object;
    }

    @Specialization(contains = "updateSlotCached")
    protected Object updateSlot(RAbstractContainer object, String name, Object value) {
        Object actualValue = getActualValue(value);
        object.setAttr(name.intern(), actualValue);
        return object;
    }

}
