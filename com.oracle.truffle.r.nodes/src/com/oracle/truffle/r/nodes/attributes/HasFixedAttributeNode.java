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
import com.oracle.truffle.api.object.Location;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RAttributeStorage;

/**
 * This node is responsible for determining the existence of the predefined (fixed) attribute. It
 * accepts both {@link DynamicObject} and {@link RAttributable} instances as the first argument. If
 * the first argument is {@link RAttributable} and its attributes are initialized, the recursive
 * instance of this class is used to determine the existence from the attributes.
 */
public abstract class HasFixedAttributeNode extends FixedAttributeAccessNode {

    @Child private HasFixedAttributeNode recursive;

    protected HasFixedAttributeNode(String name) {
        super(name);
    }

    public static HasFixedAttributeNode create(String name) {
        return HasFixedAttributeNodeGen.create(name);
    }

    public static HasFixedAttributeNode createDim() {
        return HasFixedAttributeNodeGen.create(RRuntime.DIM_ATTR_KEY);
    }

    public abstract boolean execute(Object attr);

    protected boolean hasProperty(Shape shape) {
        return shape.hasProperty(name);
    }

    @Specialization(limit = "3", //
                    guards = {"shapeCheck(shape, attrs)"}, //
                    assumptions = {"shape.getValidAssumption()"})
    @SuppressWarnings("unused")
    protected boolean hasAttrCached(DynamicObject attrs,
                    @Cached("lookupShape(attrs)") Shape shape,
                    @Cached("lookupLocation(shape, name)") Location location) {
        return location != null;
    }

    @Specialization(contains = "hasAttrCached")
    @TruffleBoundary
    protected boolean hasAttrFallback(DynamicObject attrs) {
        return attrs.containsKey(name);
    }

    @Specialization
    protected boolean hasAttrFromAttributable(RAttributable x,
                    @Cached("create()") BranchProfile attrNullProfile,
                    @Cached("createBinaryProfile()") ConditionProfile attrStorageProfile,
                    @Cached("createClassProfile()") ValueProfile xTypeProfile) {

        DynamicObject attributes;
        if (attrStorageProfile.profile(x instanceof RAttributeStorage)) {
            attributes = ((RAttributeStorage) x).getAttributes();
        } else {
            attributes = xTypeProfile.profile(x).getAttributes();
        }

        if (attributes == null) {
            attrNullProfile.enter();
            return false;
        }

        if (recursive == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            recursive = insert(create(name));
        }

        return recursive.execute(attributes);
    }

}
