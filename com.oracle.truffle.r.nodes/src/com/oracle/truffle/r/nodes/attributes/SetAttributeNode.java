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
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.nodes.function.opt.ShareObjectNode;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RAttributeStorage;

/**
 * This node is responsible for setting a value to an arbitrary attribute. It accepts both
 * {@link DynamicObject} and {@link RAttributable} instances as the first argument. If the first
 * argument is {@link RAttributable} and the attribute is a special one (i.e. names, dims, dimnames,
 * rownames), a corresponding node defined in the {@link SpecialAttributesFunctions} class is
 * created and the processing is delegated to it. If the first argument is {@link RAttributable} and
 * the attribute is not a special one, it is made sure that the attributes in the first argument are
 * initialized. Then the recursive instance of this class is used to set the attribute value to the
 * attributes.
 */
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

    protected static SpecialAttributesFunctions.SetSpecialAttributeNode createSpecAttrNode(String name) {
        return SpecialAttributesFunctions.createSetSpecialAttributeNode(name);
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

    @Specialization(replaces = "setSpecAttrInAttributable", //
                    guards = "isSpecialAttributeNode.execute(name)")
    @SuppressWarnings("unused")
    protected void setSpecAttrInAttributable(RAttributable x, String name, Object value,
                    @Cached("create()") SpecialAttributesFunctions.IsSpecialAttributeNode isSpecialAttributeNode,
                    @Cached("create()") SpecialAttributesFunctions.GenericSpecialAttributeNode genericSpecialAttrNode) {
        genericSpecialAttrNode.execute(x, name, value);
    }

    @Specialization
    protected void setAttrInAttributable(RAttributable x, String name, Object value,
                    @Cached("create()") BranchProfile attrNullProfile,
                    @Cached("createBinaryProfile()") ConditionProfile attrStorageProfile,
                    @Cached("createClassProfile()") ValueProfile xTypeProfile,
                    @Cached("create()") ShareObjectNode updateRefCountNode) {
        DynamicObject attributes;
        if (attrStorageProfile.profile(x instanceof RAttributeStorage)) {
            attributes = ((RAttributeStorage) x).getAttributes();
        } else {
            attributes = xTypeProfile.profile(x).getAttributes();
        }

        if (attributes == null) {
            attrNullProfile.enter();
            attributes = x.initAttributes();
        }

        if (recursive == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            recursive = insert(create());
        }

        recursive.execute(attributes, name, value);

        // TODO: To verify: It might be beneficial to increment the reference counter only if the
        // old and new values differ. One should verify, though, whether the costs brought about by
        // reading the old value do not prevail in the end.
        updateRefCountNode.execute(value);
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
