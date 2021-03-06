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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.object.FinalLocationException;
import com.oracle.truffle.api.object.IncompatibleLocationException;
import com.oracle.truffle.api.object.Location;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.r.runtime.data.nodes.attributes.SetFixedPropertyNodeGen.SetClassPropertyNodeGen;
import com.oracle.truffle.r.runtime.data.nodes.attributes.SetFixedPropertyNodeGen.SetCommentPropertyNodeGen;
import com.oracle.truffle.r.runtime.data.nodes.attributes.SetFixedPropertyNodeGen.SetDimNamesPropertyNodeGen;
import com.oracle.truffle.r.runtime.data.nodes.attributes.SetFixedPropertyNodeGen.SetDimPropertyNodeGen;
import com.oracle.truffle.r.runtime.data.nodes.attributes.SetFixedPropertyNodeGen.SetGenericPropertyNodeGen;
import com.oracle.truffle.r.runtime.data.nodes.attributes.SetFixedPropertyNodeGen.SetNamesPropertyNodeGen;
import com.oracle.truffle.r.runtime.data.nodes.attributes.SetFixedPropertyNodeGen.SetRowNamesPropertyNodeGen;
import com.oracle.truffle.r.runtime.data.nodes.attributes.SetFixedPropertyNodeGen.SetTspPropertyNodeGen;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;

@ImportStatic(DSLConfig.class)
@GenerateUncached
public abstract class SetFixedPropertyNode extends PropertyAccessNode {

    protected String getPropertyName() {
        throw RInternalError.shouldNotReachHere();
    }

    public static SetGenericPropertyNode create(String name) {
        return SetGenericPropertyNodeGen.create(name);
    }

    public static SetNamesPropertyNode createNames() {
        return SetNamesPropertyNodeGen.create();
    }

    public static SetDimPropertyNode createDim() {
        return SetDimPropertyNodeGen.create();
    }

    public static SetDimNamesPropertyNode createDimNames() {
        return SetDimNamesPropertyNodeGen.create();
    }

    public static SetRowNamesPropertyNode createRowNames() {
        return SetRowNamesPropertyNodeGen.create();
    }

    public static SetClassPropertyNode createClass() {
        return SetClassPropertyNodeGen.create();
    }

    public static SetTspPropertyNode createTsp() {
        return SetTspPropertyNodeGen.create();
    }

    public static SetCommentPropertyNode createComment() {
        return SetCommentPropertyNodeGen.create();
    }

    public abstract void execute(DynamicObject attrs, Object value);

    @Specialization(limit = "getCacheSize(3)", //
                    guards = {"shapeCheck(shape, attrs)", "location != null", "canSet(location, value)"}, //
                    assumptions = {"shape.getValidAssumption()"})
    protected void setAttrCached(DynamicObject attrs, Object value,
                    @Cached("lookupShape(attrs)") Shape shape,
                    @Cached("lookupLocation(shape, getPropertyName())") Location location) {
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
                    @SuppressWarnings("unused") @Cached("lookupLocation(oldShape, getPropertyName())") Location oldLocation,
                    @Cached("defineProperty(oldShape, getPropertyName(), value)") Shape newShape,
                    @Cached("lookupLocation(newShape, getPropertyName())") Location newLocation) {
        try {
            newLocation.set(attrs, value, oldShape, newShape);
        } catch (IncompatibleLocationException ex) {
            throw RInternalError.shouldNotReachHere(ex);
        }
    }

    protected static Shape defineProperty(Shape oldShape, Object name, Object value) {
        return oldShape.defineProperty(name, value, 0);
    }

    @Specialization(replaces = {"setAttrCached", "setNewAttrCached"})
    @TruffleBoundary
    protected void setFallback(DynamicObject attrs, Object value) {
        DynamicObjectLibrary.getUncached().put(attrs, getPropertyName(), value);
    }

    public abstract static class SetGenericPropertyNode extends SetFixedPropertyNode {
        protected final String name;

        SetGenericPropertyNode(String name) {
            this.name = name;
        }

        @Override
        protected String getPropertyName() {
            return name;
        }
    }

    public abstract static class SetNamesPropertyNode extends SetFixedPropertyNode {
        @Override
        protected String getPropertyName() {
            return RRuntime.NAMES_ATTR_KEY;
        }
    }

    public abstract static class SetDimPropertyNode extends SetFixedPropertyNode {
        @Override
        protected String getPropertyName() {
            return RRuntime.DIM_ATTR_KEY;
        }
    }

    public abstract static class SetDimNamesPropertyNode extends SetFixedPropertyNode {
        @Override
        protected String getPropertyName() {
            return RRuntime.DIMNAMES_ATTR_KEY;
        }
    }

    public abstract static class SetRowNamesPropertyNode extends SetFixedPropertyNode {
        @Override
        protected String getPropertyName() {
            return RRuntime.ROWNAMES_ATTR_KEY;
        }
    }

    public abstract static class SetClassPropertyNode extends SetFixedPropertyNode {
        @Override
        protected String getPropertyName() {
            return RRuntime.CLASS_ATTR_KEY;
        }
    }

    public abstract static class SetTspPropertyNode extends SetFixedPropertyNode {
        @Override
        protected String getPropertyName() {
            return RRuntime.TSP_ATTR_KEY;
        }
    }

    public abstract static class SetCommentPropertyNode extends SetFixedPropertyNode {
        @Override
        protected String getPropertyName() {
            return RRuntime.COMMENT_ATTR_KEY;
        }
    }
}
