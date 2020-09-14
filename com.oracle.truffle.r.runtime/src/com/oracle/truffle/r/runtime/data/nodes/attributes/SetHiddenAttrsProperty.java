/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.data.nodes.attributes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.object.FinalLocationException;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.object.IncompatibleLocationException;
import com.oracle.truffle.api.object.Location;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPairList;

@ImportStatic(DSLConfig.class)
@GenerateUncached
public abstract class SetHiddenAttrsProperty extends PropertyAccessNode {
    protected static final HiddenKey KEY = RAttributable.ATTRS_KEY;

    public static Object executeUncached(RAttributable attrsHolder, Object attrsValue) {
        return SetHiddenAttrsPropertyNodeGen.getUncached().execute(attrsHolder, attrsValue);
    }

    public abstract Object execute(RAttributable attrsHolder, Object attrsValue);

    @Specialization(limit = "getCacheSize(3)", //
                    guards = {
                                    "shapeCheck(shape, attrs.getAttributes())",
                                    "location != null"
                    }, //
                    assumptions = "shape.getValidAssumption()")
    protected static RPairList setExistingCached(RAttributable attrs, RPairList value,
                    @Cached("lookupShape(attrs.getAttributes())") Shape shape,
                    @Cached("lookupLocation(shape, KEY)") Location location) {
        try {
            location.set(attrs.getAttributes(), value, shape);
            return value;
        } catch (IncompatibleLocationException | FinalLocationException ex) {
            throw RInternalError.shouldNotReachHere(ex);
        }
    }

    @Specialization(limit = "getCacheSize(3)", //
                    guards = {
                                    "shapeCheck(oldShape, attrs.getAttributes())",
                                    "oldLocation == null",
                    }, //
                    assumptions = {
                                    "oldShape.getValidAssumption()",
                                    "newShape.getValidAssumption()"
                    })
    @SuppressWarnings("unused")
    protected static RPairList setNewCached(RAttributable attrs, RPairList value,
                    @Cached("lookupShape(attrs.getAttributes())") Shape oldShape,
                    @Cached("lookupLocation(oldShape, KEY)") Location oldLocation,
                    @Cached("defineProperty(oldShape, KEY, null)") Shape newShape,
                    @Cached("lookupLocation(newShape, KEY)") Location newLocation) {
        try {
            newLocation.set(attrs.getAttributes(), value, oldShape, newShape);
            return value;
        } catch (IncompatibleLocationException ex) {
            throw RInternalError.shouldNotReachHere(ex);
        }
    }

    @Specialization(replaces = {"setExistingCached", "setNewCached"})
    protected static RPairList setGeneric(RAttributable attrs, RPairList value) {
        DynamicObjectLibrary.getUncached().put(attrs.getAttributes(), KEY, value);
        return value;
    }

    @Specialization(limit = "getCacheSize(3)", guards = "shapeCheck(shape, attrs.getAttributes())", assumptions = "shape.getValidAssumption()")
    protected static RNull removeCached(RAttributable attrs, @SuppressWarnings("unused") RNull value,
                    @Cached BranchProfile existsProfile,
                    @SuppressWarnings("unused") @Cached("lookupShape(attrs.getAttributes())") Shape shape,
                    @Cached("lookupLocation(shape, KEY)") Location location) {
        if (location != null) {
            existsProfile.enter();
            DynamicObjectLibrary.getUncached().removeKey(attrs.getAttributes(), KEY);
        }
        return RNull.instance;
    }

    @Specialization(replaces = "removeCached")
    protected static RNull removeGeneric(RAttributable attrs, @SuppressWarnings("unused") RNull value) {
        DynamicObject attrObj = attrs.getAttributes();
        DynamicObjectLibrary uncached = DynamicObjectLibrary.getUncached();
        if (uncached.containsKey(attrObj, KEY)) {
            DynamicObjectLibrary.getUncached().removeKey(attrObj, KEY);
        }
        return RNull.instance;
    }
}
