/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.nodes.attributes;

import java.util.List;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RAttributesLayout;
import com.oracle.truffle.r.runtime.data.RVector;
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
public abstract class CopyOfRegAttributesNode extends RBaseNode {

    private final ConditionProfile sizeOneProfile = ConditionProfile.createBinaryProfile();

    @Child private GetFixedAttributeNode dimAttrGetter = GetFixedAttributeNode.createDim();
    @Child private GetFixedAttributeNode namesAttrGetter = GetFixedAttributeNode.createNames();
    @Child private GetFixedAttributeNode classAttrGetter = GetFixedAttributeNode.createClass();

    public abstract void execute(RAbstractVector source, RVector<?> target);

    public static CopyOfRegAttributesNode create() {
        return CopyOfRegAttributesNodeGen.create();
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "source.getAttributes() == null")
    protected void copyNoAttributes(RAbstractVector source, RVector<?> target) {
        // nothing to do
    }

    protected static final boolean emptyAttributes(RAbstractVector source) {
        DynamicObject attributes = source.getAttributes();
        return attributes == null || attributes.isEmpty();
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "emptyAttributes(source)", contains = "copyNoAttributes")
    protected void copyEmptyAttributes(RAbstractVector source, RVector<?> target) {
        // nothing to do
    }

    protected final boolean onlyDimAttribute(RAbstractVector source) {
        DynamicObject attributes = source.getAttributes();
        return attributes != null && sizeOneProfile.profile(attributes.size() == 1) && dimAttrGetter.execute(attributes) != null;
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "onlyDimAttribute(source)")
    protected void copyDimOnly(RAbstractVector source, RVector<?> target) {
        // nothing to do
    }

    protected final boolean onlyNamesAttribute(RAbstractVector source) {
        DynamicObject attributes = source.getAttributes();
        return attributes != null && sizeOneProfile.profile(attributes.size() == 1) && namesAttrGetter.execute(attributes) != null;
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "onlyNamesAttribute(source)")
    protected void copyNamesOnly(RAbstractVector source, RVector<?> target) {
        // nothing to do
    }

    protected final boolean onlyClassAttribute(RAbstractVector source) {
        DynamicObject attributes = source.getAttributes();
        return attributes != null && sizeOneProfile.profile(attributes.size() == 1) && classAttrGetter.execute(attributes) != null;
    }

    @Specialization(guards = "onlyClassAttribute(source)")
    protected void copyClassOnly(RAbstractVector source, RVector<?> target) {
        Object classAttr = classAttrGetter.execute(source.getAttributes());
        target.initAttributes(RAttributesLayout.createClass(classAttr));
    }

    @Specialization
    protected void copyGeneric(RAbstractVector source, RVector<?> target) {
        DynamicObject orgAttributes = source.getAttributes();
        if (orgAttributes != null) {
            Shape shape = orgAttributes.getShape();
            List<Property> properties = shape.getPropertyList();
            for (int i = 0; i < properties.size(); i++) {
                Property p = properties.get(i);
                String name = (String) p.getKey();
                if (name != RRuntime.DIM_ATTR_KEY && name != RRuntime.DIMNAMES_ATTR_KEY && name != RRuntime.NAMES_ATTR_KEY) {
                    Object val = p.get(orgAttributes, shape);
                    target.initAttributes().define(name, val);
                }
            }
        }
    }
}
