/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Location;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

/**
 * The base class for the nodes that get/set/remove attributes. It encapsulates the common methods
 * used in guards and for caching.
 */
public abstract class AttributeAccessNode extends RBaseNode {

    protected AttributeAccessNode() {
    }

    protected static Shape lookupShape(DynamicObject attrs) {
        CompilerAsserts.neverPartOfCompilation();
        return attrs.getShape();
    }

    protected static Property lookupProperty(Shape shape, Object name) {
        /* Initialization of cached values always happens in a slow path. */
        CompilerAsserts.neverPartOfCompilation();

        Property property = shape.getProperty(name);
        if (property == null) {
            /* Property does not exist. */
            return null;
        }

        return property;
    }

    protected static Location lookupLocation(Shape shape, Object name) {
        Property p = lookupProperty(shape, name);
        return p == null ? null : p.getLocation();
    }

    protected static boolean shapeCheck(Shape shape, DynamicObject attrs) {
        return shape != null && shape.check(attrs);
    }

    protected static Shape defineProperty(Shape oldShape, Object name, Object value) {
        return oldShape.defineProperty(name, value, 0);
    }

    /**
     * There is a subtle difference between {@link Location#canSet} and {@link Location#canStore}.
     * We need {@link Location#canSet} for the guard of {@code setExistingAttrCached} because there
     * we call {@link Location#set}. We use the more relaxed {@link Location#canStore} for the guard
     * of {@code setNewAttrCached} because there we perform a shape transition, i.e., we are not
     * actually setting the value of the new location - we only transition to this location as part
     * of the shape change.
     */
    protected static boolean canSet(Location location, Object value) {
        return location.canSet(value);
    }

    /** See {@link #canSet} for the difference between the two methods. */
    protected static boolean canStore(Location location, Object value) {
        return location.canStore(value);
    }
}
