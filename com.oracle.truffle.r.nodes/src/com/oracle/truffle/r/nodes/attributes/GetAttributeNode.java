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
import com.oracle.truffle.api.profiles.ConditionProfile;

public abstract class GetAttributeNode extends AttributeAccessNode {

    protected GetAttributeNode() {
    }

    public static GetAttributeNode create() {
        return GetAttributeNodeGen.create();
    }

    public abstract Object execute(DynamicObject attrs, String name);

    @Specialization(limit = "3", //
                    guards = {"cachedName.equals(name)", "shapeCheck(shape, attrs)"}, //
                    assumptions = {"shape.getValidAssumption()"})
    @SuppressWarnings("unused")
    protected Object getAttrCached(DynamicObject attrs, String name,
                    @Cached("name") String cachedName,
                    @Cached("lookupShape(attrs)") Shape shape,
                    @Cached("lookupLocation(shape, name)") Location location,
                    @Cached("createBinaryProfile()") ConditionProfile nullProfile) {
        return nullProfile.profile(location == null) ? null : location.get(attrs);
    }

    @TruffleBoundary
    @Specialization(contains = {"getAttrCached"})
    protected Object getAttrFallback(DynamicObject attrs, String name) {
        return attrs.get(name);
    }

}
