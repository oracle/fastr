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
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RAttributesLayout;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

public abstract class FixedAttributeGetter extends RBaseNode {

    protected final String name;
    protected final Shape constantShape;
    protected final Location constantLocation;

    protected FixedAttributeGetter(String name, Shape constantShape, Location constantLocation) {
        this.name = name;
        this.constantShape = constantShape;
        this.constantLocation = constantLocation;
        assert constantShape == null || constantLocation != null;
    }

    public static FixedAttributeGetter create(String name) {
        return FixedAttributeGetterNodeGen.create(name, null, null);
    }

    public static FixedAttributeGetter createNames() {
        return FixedAttributeGetter.create(RRuntime.NAMES_ATTR_KEY);
    }

    public static FixedAttributeGetter createDim() {
        return FixedAttributeGetter.create(RRuntime.DIM_ATTR_KEY);
    }

    public static FixedAttributeGetter createClass() {
        return FixedAttributeGetter.create(RRuntime.CLASS_ATTR_KEY);
    }

    public abstract Object execute(DynamicObject attrs);

    @Specialization(guards = "attrs.getShape()==constantShape")
    protected Object getFromConstantLocation(DynamicObject attrs) {
        return constantLocation.get(attrs);
    }

    @Specialization(contains = "getFromConstantLocation", guards = {"cachedProperty != null", "attrs.getShape() == cachedShape"})
    protected Object getFromCachedLocation(DynamicObject attrs,
                    @SuppressWarnings("unused") @Cached("attrs.getShape()") Shape cachedShape,
                    @Cached("cachedShape.getProperty(name)") Property cachedProperty) {
        return cachedProperty.getLocation().get(attrs);
    }

    @Specialization(contains = "getFromCachedLocation")
    protected Object getFromObject(DynamicObject attrs, @Cached("create()") GetAttributeNode getter) {
        return getter.execute(attrs, name);
    }
}
