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

public abstract class RemoveFixedAttributeNode extends FixedAttributeAccessNode {

    protected RemoveFixedAttributeNode(String name) {
        super(name);
    }

    public static RemoveFixedAttributeNode create(String name) {
        return RemoveFixedAttributeNodeGen.create(name);
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

    @Specialization(limit = "3", //
                    guards = {"shapeCheck(shape, attrs)", "property != null"}, //
                    assumptions = {"shape.getValidAssumption()"})
    protected void removeAttrCached(DynamicObject attrs,
                    @Cached("lookupShape(attrs)") Shape shape,
                    @Cached("lookupProperty(shape, name)") Property property) {
        Shape newShape = attrs.getShape().removeProperty(property);
        attrs.setShapeAndResize(shape, newShape);
    }

    @Specialization(contains = "removeAttrCached")
    protected void removeAttrFallback(DynamicObject attrs) {
        attrs.delete(this.name);
    }
}
