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
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RAttributeStorage;

/**
 * This node is responsible for retrieving a value from an arbitrary attribute. It accepts both
 * {@link DynamicObject} and {@link RAttributable} instances as the first argument. If the first
 * argument is {@link RAttributable} and its attributes are initialized, the recursive instance of
 * this class is used to get the attribute value from the attributes.
 */
public abstract class GetAttributeNode extends AttributeAccessNode {

    @Child private GetAttributeNode recursive;

    protected GetAttributeNode() {
    }

    public static GetAttributeNode create() {
        return GetAttributeNodeGen.create();
    }

    public abstract Object execute(Object attrs, String name);

    @Specialization(limit = "3", //
                    guards = {"cachedName.equals(name)", "shapeCheck(shape, attrs)"}, //
                    assumptions = {"shape.getValidAssumption()"})
    @SuppressWarnings("unused")
    protected Object getAttrCached(DynamicObject attrs, String name,
                    @Cached("name") String cachedName,
                    @Cached("lookupShape(attrs)") Shape shape,
                    @Cached("lookupLocation(shape, name)") Location location) {
        return location == null ? null : location.get(attrs);
    }

    @TruffleBoundary
    @Specialization(contains = {"getAttrCached"})
    protected Object getAttrFallback(DynamicObject attrs, String name) {
        return attrs.get(name);
    }

    @Specialization
    protected Object getAttrFromAttributable(RAttributable x, String name,
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
            return null;
        }

        if (recursive == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            recursive = insert(create());
        }

        return recursive.execute(attributes, name);
    }

}
