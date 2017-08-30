/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.RemoveClassAttributeNode;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RAttributeStorage;

/**
 * Remove all regular attributes. This node is in particular useful if we reuse containers with
 * attributes but the reusing builtin should actually return a fresh container with no regular
 * attributes.
 */
public abstract class RemoveRegAttributesNode extends AttributeAccessNode {

    private final ConditionProfile sizeOneProfile = ConditionProfile.createBinaryProfile();

    @Child private GetFixedAttributeNode dimAttrGetter = GetFixedAttributeNode.createDim();
    @Child private GetFixedAttributeNode namesAttrGetter = GetFixedAttributeNode.createNames();
    @Child private GetFixedAttributeNode classAttrGetter = GetFixedAttributeNode.createClass();
    @Child private RemoveClassAttributeNode removeClassAttributeNode;

    protected RemoveRegAttributesNode() {
    }

    public static RemoveRegAttributesNode create() {
        return RemoveRegAttributesNodeGen.create();
    }

    public abstract void execute(RAttributable attrs);

    @Specialization(guards = "source.getAttributes() == null")
    protected void copyNoAttributes(@SuppressWarnings("unused") RAttributeStorage source) {
        // nothing to do
    }

    protected static final boolean emptyAttributes(RAttributeStorage source) {
        DynamicObject attributes = source.getAttributes();
        return attributes == null || attributes.isEmpty();
    }

    @Specialization(guards = "emptyAttributes(source)", replaces = "copyNoAttributes")
    protected void copyEmptyAttributes(@SuppressWarnings("unused") RAttributeStorage source) {
        // nothing to do
    }

    protected final boolean onlyDimAttribute(RAttributeStorage source) {
        DynamicObject attributes = source.getAttributes();
        return attributes != null && sizeOneProfile.profile(attributes.size() == 1) && dimAttrGetter.execute(attributes) != null;
    }

    @Specialization(guards = "onlyDimAttribute(source)")
    protected void copyDimOnly(@SuppressWarnings("unused") RAttributeStorage source) {
        // nothing to do
    }

    protected final boolean onlyNamesAttribute(RAttributeStorage source) {
        DynamicObject attributes = source.getAttributes();
        return attributes != null && sizeOneProfile.profile(attributes.size() == 1) && namesAttrGetter.execute(attributes) != null;
    }

    @Specialization(guards = "onlyNamesAttribute(source)")
    protected void copyNamesOnly(@SuppressWarnings("unused") RAttributeStorage source) {
        // nothing to do
    }

    protected final boolean onlyClassAttribute(RAttributeStorage source) {
        DynamicObject attributes = source.getAttributes();
        return attributes != null && sizeOneProfile.profile(attributes.size() == 1) && classAttrGetter.execute(attributes) != null;
    }

    @Specialization(guards = "onlyClassAttribute(source)")
    protected void copyClassOnly(RAttributeStorage source) {
        if (removeClassAttributeNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            removeClassAttributeNode = insert(RemoveClassAttributeNode.create());
        }
        removeClassAttributeNode.execute(source);
    }

    @Specialization
    protected void removeGeneric(RAttributeStorage source) {
        DynamicObject orgAttributes = source.getAttributes();
        assert orgAttributes != null;
        Shape shape = orgAttributes.getShape();
        List<Property> properties = shape.getPropertyList();
        for (int i = 0; i < properties.size(); i++) {
            Property p = properties.get(i);
            String name = (String) p.getKey();
            if (name != RRuntime.DIM_ATTR_KEY && name != RRuntime.DIMNAMES_ATTR_KEY && name != RRuntime.NAMES_ATTR_KEY) {
                removeAttrFallback(orgAttributes, name);
            }
        }
    }

    @TruffleBoundary
    protected void removeAttrFallback(DynamicObject attrs, String name) {
        attrs.delete(name);
    }

    @Specialization(guards = "!isAttributeStorage(source)")
    @SuppressWarnings("unused")
    protected void copyNothing(RAttributable source) {
        // do nothing, just pass
    }

    protected static boolean isAttributeStorage(RAttributable o) {
        return o instanceof RAttributeStorage;
    }
}
