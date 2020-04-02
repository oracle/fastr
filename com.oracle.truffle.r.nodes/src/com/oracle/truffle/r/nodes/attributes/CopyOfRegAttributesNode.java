/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.dsl.GenerateUncached;
import java.util.List;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.data.nodes.ShareObjectNode;
import com.oracle.truffle.r.runtime.data.nodes.UpdateShareableChildValueNode;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RAttributesLayout;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

/**
 * Copies all attributes from source to target except for 'dim', 'names' and 'dimNames' attribute.
 * Typical usage is when copying attributes to a vector created by one of
 * {@link com.oracle.truffle.r.runtime.data.RDataFactory} factory methods, because one can specify
 * the 'names' and 'dims' as parameters of the factory method and copy the rest of the attributes
 * using this node.
 *
 * @see UnaryCopyAttributesNode
 */
@GenerateUncached
public abstract class CopyOfRegAttributesNode extends RBaseNode {

    public abstract void execute(RAttributable source, RAttributable target);

    public static CopyOfRegAttributesNode create() {
        return CopyOfRegAttributesNodeGen.create();
    }

    public static CopyOfRegAttributesNode getUncached() {
        return CopyOfRegAttributesNodeGen.getUncached();
    }

    @Specialization(guards = "source.getAttributes() == null")
    protected void copyNoAttributes(@SuppressWarnings("unused") RAttributable source, @SuppressWarnings("unused") RAttributable target) {
        // nothing to do
    }

    protected static boolean emptyAttributes(RAttributable source) {
        DynamicObject attributes = source.getAttributes();
        return attributes == null || attributes.getShape().getPropertyCount() == 0;
    }

    @Specialization(guards = "emptyAttributes(source)", replaces = "copyNoAttributes")
    protected void copyEmptyAttributes(@SuppressWarnings("unused") RAttributable source, @SuppressWarnings("unused") RAttributable target) {
        // nothing to do
    }

    protected static final boolean onlyDimAttribute(RAttributable source, ConditionProfile sizeOneProfile, GetFixedAttributeNode dimAttrGetter) {
        DynamicObject attributes = source.getAttributes();
        return attributes != null && sizeOneProfile.profile(attributes.getShape().getPropertyCount() == 1) && dimAttrGetter.execute(source) != null;
    }

    @Specialization(guards = "onlyDimAttribute(source, sizeOneProfile, dimAttrGetter)")
    protected void copyDimOnly(@SuppressWarnings("unused") RAttributable source, @SuppressWarnings("unused") RAttributable target,
                    @SuppressWarnings("unused") @Cached("createBinaryProfile()") ConditionProfile sizeOneProfile,
                    @SuppressWarnings("unused") @Cached("createDim()") GetFixedAttributeNode dimAttrGetter) {
        // nothing to do
    }

    protected static final boolean onlyNamesAttribute(RAttributable source, ConditionProfile sizeOneProfile, GetFixedAttributeNode namesAttrGetter) {
        DynamicObject attributes = source.getAttributes();
        return attributes != null && sizeOneProfile.profile(attributes.getShape().getPropertyCount() == 1) && namesAttrGetter.execute(source) != null;
    }

    @Specialization(guards = "onlyNamesAttribute(source, sizeOneProfile, namesAttrGetter)")
    protected void copyNamesOnly(@SuppressWarnings("unused") RAttributable source, @SuppressWarnings("unused") RAttributable target,
                    @SuppressWarnings("unused") @Cached("createBinaryProfile()") ConditionProfile sizeOneProfile,
                    @SuppressWarnings("unused") @Cached("createNames()") GetFixedAttributeNode namesAttrGetter) {
        // nothing to do
    }

    protected static final boolean onlyClassAttribute(RAttributable source, ConditionProfile sizeOneProfile, GetFixedAttributeNode classAttrGetter) {
        DynamicObject attributes = source.getAttributes();
        return attributes != null && sizeOneProfile.profile(attributes.getShape().getPropertyCount() == 1) && classAttrGetter.execute(source) != null;
    }

    @Specialization(guards = "onlyClassAttribute(source, sizeOneProfile, classAttrGetter)")
    protected void copyClassOnly(RAttributable source, RAbstractVector target,
                    @Cached("create()") UpdateShareableChildValueNode updateChildRefCountNode,
                    @Cached("create()") ShareObjectNode updateRefCountNode,
                    @SuppressWarnings("unused") @Cached("createBinaryProfile()") ConditionProfile sizeOneProfile,
                    @Cached("createClass()") GetFixedAttributeNode classAttrGetter) {
        Object classAttr = classAttrGetter.execute(source);
        updateRefCountNode.execute(updateChildRefCountNode.updateState(source, classAttr));
        target.initAttributes(RAttributesLayout.createClass(classAttr));
    }

    @Specialization
    protected void copyGeneric(RAttributable source, RAttributable target,
                    @Cached("create()") UpdateShareableChildValueNode updateChildRefCountNode,
                    @Cached("create()") ShareObjectNode updateRefCountNode) {
        DynamicObject orgAttributes = source.getAttributes();
        if (orgAttributes != null) {
            Shape shape = orgAttributes.getShape();
            List<Property> properties = shape.getPropertyList();
            for (int i = 0; i < properties.size(); i++) {
                Property p = properties.get(i);
                String name = (String) p.getKey();
                if (!Utils.identityEquals(name, RRuntime.DIM_ATTR_KEY) && !Utils.identityEquals(name, RRuntime.DIMNAMES_ATTR_KEY) && !Utils.identityEquals(name, RRuntime.NAMES_ATTR_KEY)) {
                    Object val = p.get(orgAttributes, shape);
                    updateRefCountNode.execute(updateChildRefCountNode.updateState(source, val));
                    target.initAttributes().define(name, val);
                }
            }
        }
    }
}
