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
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.FinalLocationException;
import com.oracle.truffle.api.object.IncompatibleLocationException;
import com.oracle.truffle.api.object.Location;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.RInternalError;

@ImportStatic(DSLConfig.class)
@GenerateUncached
public abstract class SetPropertyNode extends PropertyAccessNode {

    public abstract void execute(DynamicObject attrs, String name, Object value);

    public static SetPropertyNode create() {
        return SetPropertyNodeGen.create();
    }

    @Specialization(limit = "getCacheSize(3)", //
                    guards = {
                                    "cachedName.equals(name)",
                                    "shapeCheck(shape, attrs)",
                                    "location != null",
                                    "canSet(location, value)"
                    }, //
                    assumptions = {
                                    "shape.getValidAssumption()"
                    })
    protected static void setExistingAttrCached(DynamicObject attrs, @SuppressWarnings("unused") String name, Object value,
                    @Cached("name") @SuppressWarnings("unused") String cachedName,
                    @Cached("lookupShape(attrs)") Shape shape,
                    @Cached("lookupLocation(shape, name, value)") Location location) {
        try {
            location.set(attrs, value, shape);
        } catch (IncompatibleLocationException | FinalLocationException ex) {
            throw RInternalError.shouldNotReachHere(ex);
        }
    }

    @Specialization(limit = "getCacheSize(3)", //
                    guards = {
                                    "cachedName.equals(name)",
                                    "shapeCheck(oldShape, attrs)",
                                    "oldLocation == null",
                                    "canStore(newLocation, value)"
                    }, //
                    assumptions = {
                                    "oldShape.getValidAssumption()",
                                    "newShape.getValidAssumption()"
                    })
    @SuppressWarnings("unused")
    protected static void setNewAttrCached(DynamicObject attrs, String name, Object value,
                    @Cached("name") String cachedName,
                    @Cached("lookupShape(attrs)") Shape oldShape,
                    @Cached("lookupLocation(oldShape, name, value)") Location oldLocation,
                    @Cached("defineProperty(oldShape, name, value)") Shape newShape,
                    @Cached("lookupLocation(newShape, name)") Location newLocation) {
        try {
            newLocation.set(attrs, value, oldShape, newShape);
        } catch (IncompatibleLocationException ex) {
            throw RInternalError.shouldNotReachHere(ex);
        }
    }

    /**
     * The generic case is used if the number of shapes accessed overflows the limit of the
     * polymorphic inline cache.
     */
    @TruffleBoundary
    @Specialization(replaces = {"setExistingAttrCached", "setNewAttrCached"})
    protected static void setAttrFallback(DynamicObject receiver, String name, Object value) {
        receiver.define(name, value);
    }

    /**
     * Try to find the given property in the shape. Also returns null when the value cannot be store
     * into the location.
     */
    protected static Location lookupLocation(Shape shape, Object name, Object value) {
        Location location = lookupLocation(shape, name);
        if (location == null || !location.canSet(value)) {
            /* Existing property has an incompatible type, so a shape change is necessary. */
            return null;
        }

        return location;
    }
}
