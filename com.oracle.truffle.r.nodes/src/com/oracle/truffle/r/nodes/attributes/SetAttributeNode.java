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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.FinalLocationException;
import com.oracle.truffle.api.object.IncompatibleLocationException;
import com.oracle.truffle.api.object.Location;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.data.RAttributable;

public abstract class SetAttributeNode extends AttributeAccessNode {

    @Child SetAttributeNode recursive;

    protected SetAttributeNode() {
    }

    public static SetAttributeNode create() {
        return SetAttributeNodeGen.create();
    }

    public abstract void execute(Object attrs, String name, Object value);

    @Specialization(limit = "3", //
                    guards = {
                                    "cachedName.equals(name)",
                                    "shapeCheck(shape, attrs)",
                                    "location != null",
                                    "canSet(location, value)"
                    }, //
                    assumptions = {
                                    "shape.getValidAssumption()"
                    })
    @SuppressWarnings("unused")
    protected static void setExistingAttrCached(DynamicObject attrs, String name, Object value,
                    @Cached("name") String cachedName,
                    @Cached("lookupShape(attrs)") Shape shape,
                    @Cached("lookupLocation(shape, name, value)") Location location) {
        try {
            location.set(attrs, value);
        } catch (IncompatibleLocationException | FinalLocationException ex) {
            RInternalError.reportError(ex);
        }
    }

    @Specialization(limit = "3", //
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
            RInternalError.reportError(ex);
        }
    }

    /**
     * The generic case is used if the number of shapes accessed overflows the limit of the
     * polymorphic inline cache.
     */
    @TruffleBoundary
    @Specialization(contains = {"setExistingAttrCached", "setNewAttrCached"})
    protected static void setAttrFallback(DynamicObject receiver, String name, Object value) {
        receiver.define(name, value);
    }

    protected static SpecialAttributesFunctions.SetSpecialAttributeNode createSpecAttrNode(String name) {
        return SpecialAttributesFunctions.createSpecialAttributeNode(name);
    }

    @Specialization(limit = "3", //
                    guards = {
                                    "isSpecialAttributeNode.execute(name)",
                                    "cachedName.equals(name)"
                    })
    @SuppressWarnings("unused")
    protected void setSpecAttrInAttributable(RAttributable x, String name, Object value,
                    @Cached("create()") SpecialAttributesFunctions.IsSpecialAttributeNode isSpecialAttributeNode,
                    @Cached("name") String cachedName,
                    @Cached("createSpecAttrNode(cachedName)") SpecialAttributesFunctions.SetSpecialAttributeNode setSpecAttrNode) {
        setSpecAttrNode.execute(x, value);
    }

    @Specialization(contains = "setSpecAttrInAttributable", //
                    guards = "isSpecialAttributeNode.execute(name)")
    @SuppressWarnings("unused")
    protected void setSpecAttrInAttributable(RAttributable x, String name, Object value,
                    @Cached("create()") SpecialAttributesFunctions.IsSpecialAttributeNode isSpecialAttributeNode,
                    @Cached("create()") SpecialAttributesFunctions.GenericSpecialAttributeNode genericSpecialAttrNode) {
        genericSpecialAttrNode.execute(x, name, value);
    }

    @Specialization
    protected void setAttrInAttributable(RAttributable x, String name, Object value,
                    @Cached("create()") BranchProfile attrNullProfile) {
        DynamicObject attributes = x.getAttributes();
        if (attributes == null) {
            attrNullProfile.enter();
            attributes = x.initAttributes();
        }

        if (recursive == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            recursive = insert(create());
        }

        recursive.execute(attributes, name, value);
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

    protected static Shape defineProperty(Shape oldShape, Object name, Object value) {
        return oldShape.defineProperty(name, value, 0);
    }

    /**
     * There is a subtle difference between {@link Location#canSet} and {@link Location#canStore}.
     * We need {@link Location#canSet} for the guard of {@link #setExistingAttrCached} because there
     * we call {@link Location#set}. We use the more relaxed {@link Location#canStore} for the guard
     * of {@link #setNewAttrCached} because there we perform a shape transition, i.e., we are not
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
