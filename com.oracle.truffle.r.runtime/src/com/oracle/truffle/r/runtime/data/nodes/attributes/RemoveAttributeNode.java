/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.data.nodes.attributes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.r.runtime.data.RAttributable;

public abstract class RemoveAttributeNode extends AttributeAccessNode {

    protected RemoveAttributeNode() {
    }

    public static RemoveAttributeNode create() {
        return RemoveAttributeNodeGen.create();
    }

    public abstract void execute(RAttributable attrs, String name);

    @Specialization
    protected static void removeAttrFromAttributable(RAttributable x, String name,
                    @Cached("create()") BranchProfile attrNullProfile,
                    @Cached("create()") RemovePropertyNode removePropertyNode,
                    @Cached("create()") BranchProfile emptyAttrProfile) {
        DynamicObject attributes = x.getAttributes();

        if (attributes == null) {
            attrNullProfile.enter();
            return;
        }

        removePropertyNode.execute(attributes, name);

        if (attributes.getShape().getPropertyCount() == 0) {
            emptyAttrProfile.enter();
            x.initAttributes(null);
        }
    }
}
