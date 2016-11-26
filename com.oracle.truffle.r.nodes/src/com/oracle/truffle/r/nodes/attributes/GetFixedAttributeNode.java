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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Location;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.r.runtime.RRuntime;

public abstract class GetFixedAttributeNode extends FixedAttributeAccessNode {

    protected GetFixedAttributeNode(String name) {
        super(name);
    }

    public static GetFixedAttributeNode create(String name) {
        return GetFixedAttributeNodeGen.create(name);
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

    @Specialization(limit = "3", //
                    guards = {"shapeCheck(shape, attrs)", "location != null"}, //
                    assumptions = {"shape.getValidAssumption()"})
    @SuppressWarnings("unused")
    protected Object getAttrCached(DynamicObject attrs,
                    @Cached("lookupShape(attrs)") Shape shape,
                    @Cached("lookupLocation(shape, name)") Location location) {
        return location.get(attrs);
    }

    @Specialization(contains = "getAttrCached")
    @TruffleBoundary
    protected Object getAttrFallback(DynamicObject attrs) {
        return attrs.get(name);
    }
}
