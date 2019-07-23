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
import com.oracle.truffle.r.nodes.attributes.FixedAttributeAccessNode.GenericFixedAttributeAccessNode;
import com.oracle.truffle.r.nodes.attributes.SetFixedAttributeNodeFactory.SetGenericFixedAttributeNodeGen;
import com.oracle.truffle.r.nodes.function.opt.ShareObjectNode;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RSharingAttributeStorage;

/**
 * This node is responsible for setting a value to the predefined (fixed) attribute. Its
 * functionality should correspond to the function {@code setAttrib} in GNU-R. This node is not
 * responsible for handling the sharing state of the target vector, the callers should use
 * {@link com.oracle.truffle.r.runtime.data.nodes.VectorReuse} where applicable.
 *
 * There are specialized versions (subclasses) for some attributes (e.g. "names"), these should
 * match the corresponding specialized functions that GNU-R delegates to from inside
 * {@code setAttrib}. The specialized nodes should handle the same coercion and other functionality
 * and as GNU-R in its specialized functions.
 *
 * There is some additional functionality handled in the builtins, e.g. {@code do_namesgets} in
 * GNU-R, which we should also implement only in the builtins.
 */
public abstract class SetFixedAttributeNode extends GenericFixedAttributeAccessNode {

    private final BranchProfile fixupRHS = BranchProfile.create();

    protected SetFixedAttributeNode(String name) {
        super(name);
    }

    public static SetFixedAttributeNode create(String name) {
        if (SpecialAttributesFunctions.IsSpecialAttributeNode.isSpecialAttribute(name)) {
            return SpecialAttributesFunctions.createSetSpecialAttributeNode(name);
        } else {
            return SetGenericFixedAttributeNodeGen.create(name);
        }
    }

    public static SetFixedAttributeNode createNames() {
        return SpecialAttributesFunctions.SetNamesAttributeNode.create();
    }

    public static SetFixedAttributeNode createDim() {
        return SpecialAttributesFunctions.SetDimAttributeNode.create();
    }

    public static SetFixedAttributeNode createDimNames() {
        return SpecialAttributesFunctions.SetDimNamesAttributeNode.create();
    }

    public static SetFixedAttributeNode createClass() {
        return SpecialAttributesFunctions.SetClassAttributeNode.create();
    }

    public final void setAttr(RAttributable attr, Object valueIn) {
        Object value = valueIn;
        if (attr == value) {
            // TODO: in theory we should inspect the whole object (attributes and elements for
            // lists/envs/...) to see if there is potential cycle
            fixupRHS.enter();
            if (RSharingAttributeStorage.isShareable(value)) {
                value = ((RSharingAttributeStorage) value).deepCopy();
            }
        }
        execute(attr, castValue(value));
    }

    protected abstract void execute(Object attr, Object value);

    /**
     * This method can be used by the special attributes implementations to coerce the value.
     */
    protected Object castValue(Object value) {
        return value;
    }

    protected void setAttrInAttributableInternal(RAttributable x, Object value, BranchProfile attrNullProfile, SetFixedPropertyNode setFixedPropertyNode, ShareObjectNode updateRefCountNode) {
        DynamicObject attributes = x.getAttributes();

        if (attributes == null) {
            attrNullProfile.enter();
            attributes = x.initAttributes();
        }
        setFixedPropertyNode.execute(attributes, value);
        updateRefCountNode.execute(value);
    }

    abstract static class SetGenericFixedAttributeNode extends SetFixedAttributeNode {

        SetGenericFixedAttributeNode(String name) {
            super(name);
        }

        @Specialization()
        protected void setAttrInAttributable(RAttributable x, Object value,
                        @Cached("create()") BranchProfile attrNullProfile,
                        @Cached("create(getAttributeName())") SetFixedPropertyNode setFixedPropertyNode,
                        @Cached("create()") ShareObjectNode updateRefCountNode) {
            setAttrInAttributableInternal(x, value, attrNullProfile, setFixedPropertyNode, updateRefCountNode);
        }
    }
}
