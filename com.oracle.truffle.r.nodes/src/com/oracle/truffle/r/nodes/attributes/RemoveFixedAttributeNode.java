/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RAttributesLayout;
import com.oracle.truffle.r.runtime.data.RAttributesLayout.ConstantShapesAndProperties;

public abstract class RemoveFixedAttributeNode extends FixedAttributeAccessNode {

    protected RemoveFixedAttributeNode(String name, Shape[] constantShapes, Property[] constantProperties) {
        super(name, constantShapes, constantProperties);
    }

    public static RemoveFixedAttributeNode create(String name, Shape[] constantShapes, Property[] constantProperties) {
        return RemoveFixedAttributeNodeGen.create(name, constantShapes, constantProperties);
    }

    public static RemoveFixedAttributeNode create(String name) {
        ConstantShapesAndProperties csl = RAttributesLayout.getConstantShapesAndProperties(name);
        return RemoveFixedAttributeNodeGen.create(name, csl.getConstantShapes(), csl.getConstantProperties());
    }

    public static RemoveFixedAttributeNode createNames() {
        return RemoveFixedAttributeNode.create(RRuntime.NAMES_ATTR_KEY);
    }

    public static RemoveFixedAttributeNode createDim() {
        return RemoveFixedAttributeNode.create(RRuntime.DIM_ATTR_KEY);
    }

    public static RemoveFixedAttributeNode createDimNames() {
        return RemoveFixedAttributeNode.create(RRuntime.DIMNAMES_ATTR_KEY);
    }

    public static RemoveFixedAttributeNode createClass() {
        return RemoveFixedAttributeNode.create(RRuntime.CLASS_ATTR_KEY);
    }

    public abstract void execute(DynamicObject attrs);

    @Specialization(limit = "constantShapes.length", guards = {"shapeIndex >= 0", "shapeCheck(attrs, shapeIndex)"})
    protected void getFromConstantLocation(DynamicObject attrs,
                    @Cached("findShapeIndex(attrs)") int shapeIndex) {
        Shape oldShape = attrs.getShape();
        Shape newShape = attrs.getShape().removeProperty(constantProperties[shapeIndex]);
        attrs.setShapeAndResize(oldShape, newShape);
    }

    @Specialization(contains = "getFromConstantLocation")
    protected void getFromObject(DynamicObject attrs) {
        attrs.delete(this.name);
    }
}
