/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.r.runtime.data.nodes.attributes.SpecialAttributesFunctions.GetClassAttributeNode;
import com.oracle.truffle.r.runtime.data.nodes.attributes.SpecialAttributesFunctions.GetDimAttributeNode;
import com.oracle.truffle.r.runtime.data.nodes.attributes.SpecialAttributesFunctions.GetNamesAttributeNode;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.nodes.attributes.SpecialAttributesFunctionsFactory.GetClassAttributeNodeGen;

/**
 * This node is responsible for retrieving a value from the predefined (fixed) attribute.
 */
public abstract class GetFixedAttributeNode extends FixedAttributeAccessNode {

    public static GetFixedAttributeNode createFor(String name) {
        if (SpecialAttributesFunctions.IsSpecialAttributeNode.isSpecialAttribute(name)) {
            return SpecialAttributesFunctions.createGetSpecialAttributeNode(name);
        } else {
            return GetFixedAttributeNodeFactory.GenericGetFixedAttributeAccessNodeGen.create(name);
        }
    }

    public static GetNamesAttributeNode createNames() {
        return GetNamesAttributeNode.create();
    }

    public static GetDimAttributeNode createDim() {
        return GetDimAttributeNode.create();
    }

    public static GetClassAttributeNode createClass() {
        return GetClassAttributeNodeGen.create();
    }

    /**
     * Returns the attribute value or {@code null} if the attribute is not present.
     */
    public abstract Object execute(RAttributable attr);

    protected Object getAttrFromAttributable(RAttributable x,
                    BranchProfile attrNullProfile,
                    GetPropertyNode getPropertyNode) {
        DynamicObject attributes = x.getAttributes();
        if (attributes == null) {
            attrNullProfile.enter();
            return null;
        }

        return getPropertyNode.execute(attributes, getAttributeName());
    }

    protected Object getAttrFromAttributable(RAttributable x) {
        return getAttrFromAttributable(x, BranchProfile.getUncached(), GetPropertyNodeGen.getUncached());
    }

    public abstract static class GenericGetFixedAttributeAccessNode extends GetFixedAttributeNode {
        private final String name;

        protected GenericGetFixedAttributeAccessNode(String name) {
            assert Utils.isInterned(name);
            this.name = name;
        }

        @Override
        protected String getAttributeName() {
            return name;
        }

        @Specialization
        protected Object fallback(RAttributable x) {
            return getAttrFromAttributable(x);
        }
    }
}
