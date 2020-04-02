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
package com.oracle.truffle.r.runtime.data.nodes.attributes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.runtime.data.nodes.ShareObjectNode;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.data.RAttributable;

/**
 * This node is responsible for setting a value to an arbitrary attribute.
 *
 * @see SetFixedAttributeNode
 */
@ImportStatic(Utils.class)
public abstract class SetAttributeNode extends AttributeAccessNode {

    protected SetAttributeNode() {
    }

    public static SetAttributeNode create() {
        return SetAttributeNodeGen.create();
    }

    public abstract void execute(RAttributable attrs, String name, Object value);

    protected static SpecialAttributesFunctions.SetSpecialAttributeNode createSpecAttrNode(String name) {
        return SpecialAttributesFunctions.createSetSpecialAttributeNode(name);
    }

    @Specialization(limit = "getCacheSize(3)", //
                    guards = {
                                    "isSpecialAttributeNode.execute(cachedName)",
                                    "cachedName.equals(name)"
                    })
    @SuppressWarnings("unused")
    protected void setSpecAttrInAttributable(RAttributable x, String name, Object value,
                    @Cached("create()") SpecialAttributesFunctions.IsSpecialAttributeNode isSpecialAttributeNode,
                    @Cached("intern(name)") String cachedName,
                    @Cached("createSpecAttrNode(cachedName)") SpecialAttributesFunctions.SetSpecialAttributeNode setSpecAttrNode) {
        setSpecAttrNode.setAttr(x, value);
    }

    @Specialization(replaces = "setSpecAttrInAttributable", //
                    guards = "isSpecialAttributeNode.execute(name)")
    @SuppressWarnings("unused")
    protected void setSpecAttrInAttributableGeneric(RAttributable x, String name, Object value,
                    @Cached("create()") SpecialAttributesFunctions.IsSpecialAttributeNode isSpecialAttributeNode,
                    @Cached("create()") SpecialAttributesFunctions.GenericSpecialAttributeNode genericSpecialAttrNode) {
        genericSpecialAttrNode.execute(x, name, value);
    }

    @Specialization(guards = "!isSpecialAttributeNode.execute(name)")
    @SuppressWarnings("unused")
    protected void setAttrInAttributable(RAttributable x, String name, Object value,
                    @Cached("create()") BranchProfile attrNullProfile,
                    @Cached("create()") SetPropertyNode setPropertyNode,
                    @Cached("createBinaryProfile()") ConditionProfile attrStorageProfile,
                    @Cached("createClassProfile()") ValueProfile xTypeProfile,
                    @Cached("create()") SpecialAttributesFunctions.IsSpecialAttributeNode isSpecialAttributeNode,
                    @Cached("create()") ShareObjectNode updateRefCountNode) {
        DynamicObject attributes = x.getAttributes();

        if (attributes == null) {
            attrNullProfile.enter();
            attributes = x.initAttributes();
        }

        setPropertyNode.execute(attributes, name, value);
        // TODO: To verify: It might be beneficial to increment the reference counter only if the
        // old and new values differ. One should verify, though, whether the costs brought about by
        // reading the old value do not prevail in the end.
        updateRefCountNode.execute(value);
    }
}
