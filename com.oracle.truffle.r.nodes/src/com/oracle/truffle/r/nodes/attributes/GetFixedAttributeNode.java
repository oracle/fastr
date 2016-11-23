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
import com.oracle.truffle.api.object.Location;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RAttributesLayout;
import com.oracle.truffle.r.runtime.data.RAttributesLayout.ConstantShapesAndLocations;

public abstract class GetFixedAttributeNode extends FixedAttributeAccessNode {

    protected GetFixedAttributeNode(String name, Shape[] constantShapes, Location[] constantLocations) {
        super(name, constantShapes, constantLocations);
    }

    public static GetFixedAttributeNode create(String name, Shape[] constantShapes, Location[] constantLocations) {
        return GetFixedAttributeNodeGen.create(name, constantShapes, constantLocations);
    }

    public static GetFixedAttributeNode create(String name) {
        ConstantShapesAndLocations csl = RAttributesLayout.getConstantShapesAndLocations(name);
        return GetFixedAttributeNodeGen.create(name, csl.getConstantShapes(), csl.getConstantLocations());
    }

    public static GetFixedAttributeNode createNames() {
        return GetFixedAttributeNode.create(RRuntime.NAMES_ATTR_KEY);
    }

    public static GetFixedAttributeNode createDim() {
        return GetFixedAttributeNode.create(RRuntime.DIM_ATTR_KEY);
    }

    public static GetFixedAttributeNode createClass() {
        return GetFixedAttributeNode.create(RRuntime.CLASS_ATTR_KEY);
    }

    public abstract Object execute(DynamicObject attrs);

    @Specialization(limit = "constantShapes.length", guards = {"shapeIndex >= 0", "shapeCheck(attrs, shapeIndex)"})
    protected Object getFromConstantLocation(DynamicObject attrs, @Cached("findShapeIndex(attrs)") int shapeIndex) {
        return constantLocations[shapeIndex].get(attrs);
    }

    @Specialization(contains = "getFromConstantLocation")
    protected Object getFromObject(DynamicObject attrs, @Cached("create()") GetAttributeNode getter) {
        return getter.execute(attrs, name);
    }
}
