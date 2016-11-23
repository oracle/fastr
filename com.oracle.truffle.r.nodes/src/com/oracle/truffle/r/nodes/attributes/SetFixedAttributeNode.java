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
import com.oracle.truffle.api.object.FinalLocationException;
import com.oracle.truffle.api.object.IncompatibleLocationException;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RAttributesLayout;
import com.oracle.truffle.r.runtime.data.RAttributesLayout.ConstantShapesAndProperties;

public abstract class SetFixedAttributeNode extends FixedAttributeAccessNode {

    protected SetFixedAttributeNode(String name, Shape[] constantShapes, Property[] constantProperties) {
        super(name, constantShapes, constantProperties);
    }

    public static SetFixedAttributeNode create(String name, Shape[] constantShapes, Property[] constantLocations) {
        return SetFixedAttributeNodeGen.create(name, constantShapes, constantLocations);
    }

    public static SetFixedAttributeNode create(String name) {
        ConstantShapesAndProperties csl = RAttributesLayout.getConstantShapesAndProperties(name);
        return SetFixedAttributeNodeGen.create(name, csl.getConstantShapes(), csl.getConstantProperties());
    }

    public static SetFixedAttributeNode createNames() {
        return SetFixedAttributeNode.create(RRuntime.NAMES_ATTR_KEY);
    }

    public static SetFixedAttributeNode createDim() {
        return SetFixedAttributeNode.create(RRuntime.DIM_ATTR_KEY);
    }

    public static SetFixedAttributeNode createDimNames() {
        return SetFixedAttributeNode.create(RRuntime.DIMNAMES_ATTR_KEY);
    }

    public static SetFixedAttributeNode createClass() {
        return SetFixedAttributeNode.create(RRuntime.CLASS_ATTR_KEY);
    }

    public abstract void execute(DynamicObject attrs, Object value);

    @Specialization(limit = "constantShapes.length", guards = {"shapeIndex >= 0", "shapeCheck(attrs, shapeIndex)"})
    protected void setFromConstantLocation(DynamicObject attrs, Object value,
                    @Cached("findShapeIndex(attrs)") int shapeIndex) {
        constantProperties[shapeIndex].getLocation().get(attrs);
        try {
            constantProperties[shapeIndex].set(attrs, value, constantShapes[shapeIndex]);
        } catch (IncompatibleLocationException | FinalLocationException ex) {
            RInternalError.reportError(ex);
        }

    }

    @Specialization(contains = "setFromConstantLocation")
    protected void setFromObject(DynamicObject attrs, Object value,
                    @Cached("create()") SetAttributeNode setter) {
        setter.execute(attrs, name, value);
    }

}
