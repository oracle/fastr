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
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.r.runtime.data.nodes.attributes.SpecialAttributesFunctions.GetNamesAttributeNode;
import com.oracle.truffle.r.runtime.data.nodes.attributes.SpecialAttributesFunctions.GetRowNamesAttributeNode;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RAttributable;

/**
 * This node is responsible for retrieving a value from an arbitrary attribute. Use
 * {@link GetFixedPropertyNode} is the attribute name is fixed, i.e. can be passed in constructor.
 */
@GenerateUncached
public abstract class GetAttributeNode extends AttributeAccessNode {

    protected GetAttributeNode() {
    }

    public static GetAttributeNode create() {
        return GetAttributeNodeGen.create();
    }

    /**
     * Returns the attribute value or {@code null} if the attribute is not present.
     */
    public abstract Object execute(RAttributable attrs, String name);

    @Specialization(guards = "isRowNamesAttr(name)")
    protected static Object getRowNames(RAttributable x, @SuppressWarnings("unused") String name,
                    @Cached("create()") GetRowNamesAttributeNode getRowNamesNode) {
        Object result = getRowNamesNode.execute(x);
        return result == null ? null : GetRowNamesAttributeNode.convertRowNamesToSeq(result);
    }

    @Specialization(guards = "isNamesAttr(name)")
    protected static Object getNames(RAttributable x, @SuppressWarnings("unused") String name,
                    @Cached("create()") GetNamesAttributeNode getNamesAttributeNode) {
        return getNamesAttributeNode.execute(x);
    }

    @Specialization(guards = "!isSpecialAttribute(name)")
    protected static Object getAttrFromAttributable(RAttributable x, String name,
                    @Cached("create()") BranchProfile attrNullProfile,
                    @Cached("create()") GetPropertyNode getPropertyNode) {
        DynamicObject attributes = x.getAttributes();
        if (attributes == null) {
            attrNullProfile.enter();
            return null;
        }

        return getPropertyNode.execute(attributes, name);
    }

    public static boolean isRowNamesAttr(String name) {
        return name.equals(RRuntime.ROWNAMES_ATTR_KEY);
    }

    public static boolean isNamesAttr(String name) {
        return name.equals(RRuntime.NAMES_ATTR_KEY);
    }

    public static boolean isSpecialAttribute(String name) {
        return isRowNamesAttr(name) || isNamesAttr(name);
    }
}
