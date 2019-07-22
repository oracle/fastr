/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.nodes.attributes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RAttributable;

public abstract class RemoveFixedAttributeNode extends FixedAttributeAccessNode {

    protected RemoveFixedAttributeNode(String name) {
        super(name);
    }

    public static RemoveFixedAttributeNode create(String name) {
        return RemoveFixedAttributeNodeGen.create(name);
    }

    public static RemoveFixedAttributeNode createNames() {
        return create(RRuntime.NAMES_ATTR_KEY);
    }

    public static RemoveFixedAttributeNode createRowNames() {
        return create(RRuntime.ROWNAMES_ATTR_KEY);
    }

    public static RemoveFixedAttributeNode createDim() {
        return create(RRuntime.DIM_ATTR_KEY);
    }

    public static RemoveFixedAttributeNode createDimNames() {
        return create(RRuntime.DIMNAMES_ATTR_KEY);
    }

    public static RemoveFixedAttributeNode createClass() {
        return create(RRuntime.CLASS_ATTR_KEY);
    }

    public static RemoveFixedAttributeNode createTsp() {
        return create(RRuntime.TSP_ATTR_KEY);
    }

    public static RemoveFixedAttributeNode createComment() {
        return create(RRuntime.COMMENT_ATTR_KEY);
    }

    public abstract void execute(RAttributable attrs);

    @Specialization
    protected static void removeAttrFromAttributable(RAttributable x,
                    @Cached("create()") BranchProfile attrNullProfile,
                    @Cached("create(name)") RemoveFixedPropertyNode removeFixedPropertyNode,
                    @Cached("create()") BranchProfile emptyAttrProfile) {
        DynamicObject attributes = x.getAttributes();

        if (attributes == null) {
            attrNullProfile.enter();
            return;
        }
        removeFixedPropertyNode.execute(attributes);

        if (attributes.isEmpty()) {
            emptyAttrProfile.enter();
            x.initAttributes(null);
        }
    }
}
