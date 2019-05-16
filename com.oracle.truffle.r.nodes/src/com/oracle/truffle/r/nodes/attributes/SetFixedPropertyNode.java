/*
 * Copyright (c) 2019, 2019, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.FinalLocationException;
import com.oracle.truffle.api.object.IncompatibleLocationException;
import com.oracle.truffle.api.object.Location;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;

@ImportStatic(DSLConfig.class)
public abstract class SetFixedPropertyNode extends PropertyAccessNode {

    protected final String name;

    public SetFixedPropertyNode(String name) {
        this.name = name;
    }

    public static SetFixedPropertyNode create(String name) {
        return SetFixedPropertyNodeGen.create(name);
    }

    public static SetFixedPropertyNode createNames() {
        return SetFixedPropertyNodeGen.create(RRuntime.NAMES_ATTR_KEY);
    }

    public static SetFixedPropertyNode createDim() {
        return SetFixedPropertyNodeGen.create(RRuntime.DIM_ATTR_KEY);
    }

    public static SetFixedPropertyNode createDimNames() {
        return SetFixedPropertyNodeGen.create(RRuntime.DIMNAMES_ATTR_KEY);
    }

    public static SetFixedPropertyNode createRowNames() {
        return SetFixedPropertyNodeGen.create(RRuntime.ROWNAMES_ATTR_KEY);
    }

    public static SetFixedPropertyNode createClass() {
        return SetFixedPropertyNodeGen.create(RRuntime.CLASS_ATTR_KEY);
    }

    public static SetFixedPropertyNode createTsp() {
        return SetFixedPropertyNodeGen.create(RRuntime.TSP_ATTR_KEY);
    }

    public static SetFixedPropertyNode createComment() {
        return SetFixedPropertyNodeGen.create(RRuntime.COMMENT_ATTR_KEY);
    }

    public abstract void execute(DynamicObject attrs, Object value);

    @Specialization(limit = "getCacheSize(3)", //
                    guards = {"shapeCheck(shape, attrs)", "location != null", "canSet(location, value)"}, //
                    assumptions = {"shape.getValidAssumption()"})
    protected void setAttrCached(DynamicObject attrs, Object value,
                    @Cached("lookupShape(attrs)") Shape shape,
                    @Cached("lookupLocation(shape, name)") Location location) {
        try {
            location.set(attrs, value, shape);
        } catch (IncompatibleLocationException | FinalLocationException ex) {
            throw RInternalError.shouldNotReachHere(ex);
        }
    }

    @Specialization(limit = "getCacheSize(3)", //
                    guards = {"shapeCheck(oldShape, attrs)", "oldLocation == null", "canStore(newLocation, value)"}, //
                    assumptions = {"oldShape.getValidAssumption()", "newShape.getValidAssumption()"})
    protected static void setNewAttrCached(DynamicObject attrs, Object value,
                    @Cached("lookupShape(attrs)") Shape oldShape,
                    @SuppressWarnings("unused") @Cached("lookupLocation(oldShape, name)") Location oldLocation,
                    @Cached("defineProperty(oldShape, name, value)") Shape newShape,
                    @Cached("lookupLocation(newShape, name)") Location newLocation) {
        try {
            newLocation.set(attrs, value, oldShape, newShape);
        } catch (IncompatibleLocationException ex) {
            throw RInternalError.shouldNotReachHere(ex);
        }
    }

    protected static Shape defineProperty(Shape oldShape, Object name, Object value) {
        return oldShape.defineProperty(name, value, 0);
    }

    @Specialization(replaces = "setAttrCached")
    @TruffleBoundary
    protected void setFallback(DynamicObject attrs, Object value) {
        attrs.define(name, value);
    }
}
