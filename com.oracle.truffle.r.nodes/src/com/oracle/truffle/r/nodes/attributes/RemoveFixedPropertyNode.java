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
import com.oracle.truffle.api.object.Location;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.r.nodes.attributes.RemoveFixedPropertyNodeGen.RemoveClassPropertyAccessNodeGen;
import com.oracle.truffle.r.nodes.attributes.RemoveFixedPropertyNodeGen.RemoveCommentPropertyAccessNodeGen;
import com.oracle.truffle.r.nodes.attributes.RemoveFixedPropertyNodeGen.RemoveDimNamesPropertyAccessNodeGen;
import com.oracle.truffle.r.nodes.attributes.RemoveFixedPropertyNodeGen.RemoveDimPropertyAccessNodeGen;
import com.oracle.truffle.r.nodes.attributes.RemoveFixedPropertyNodeGen.RemoveGenericPropertyAccessNodeGen;
import com.oracle.truffle.r.nodes.attributes.RemoveFixedPropertyNodeGen.RemoveNamesPropertyAccessNodeGen;
import com.oracle.truffle.r.nodes.attributes.RemoveFixedPropertyNodeGen.RemoveRowNamesPropertyAccessNodeGen;
import com.oracle.truffle.r.nodes.attributes.RemoveFixedPropertyNodeGen.RemoveTspPropertyAccessNodeGen;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;

@ImportStatic(DSLConfig.class)
@GenerateUncached
public abstract class RemoveFixedPropertyNode extends PropertyAccessNode {

    protected String getPropertyName() {
        throw RInternalError.shouldNotReachHere();
    }

    public static RemoveFixedPropertyNode create(String name) {
        return RemoveGenericPropertyAccessNodeGen.create(name);
    }

    public static RemoveFixedPropertyNode createNames() {
        return RemoveNamesPropertyAccessNodeGen.create();
    }

    public static RemoveFixedPropertyNode createRowNames() {
        return RemoveRowNamesPropertyAccessNodeGen.create();
    }

    public static RemoveFixedPropertyNode createDim() {
        return RemoveDimPropertyAccessNodeGen.create();
    }

    public static RemoveFixedPropertyNode createDimNames() {
        return RemoveDimNamesPropertyAccessNodeGen.create();
    }

    public static RemoveFixedPropertyNode createClass() {
        return RemoveClassPropertyAccessNodeGen.create();
    }

    public static RemoveFixedPropertyNode createTsp() {
        return RemoveTspPropertyAccessNodeGen.create();
    }

    public static RemoveFixedPropertyNode createComment() {
        return RemoveCommentPropertyAccessNodeGen.create();
    }

    public abstract void execute(DynamicObject attrs);

    @Specialization(limit = "getCacheSize(3)", //
                    guards = {"shapeCheck(shape, attrs)", "location == null"}, //
                    assumptions = {"shape.getValidAssumption()"})
    protected void removeNonExistantAttr(@SuppressWarnings("unused") DynamicObject attrs,
                    @SuppressWarnings("unused") @Cached("lookupShape(attrs)") Shape shape,
                    @SuppressWarnings("unused") @Cached("lookupLocation(shape, getPropertyName())") Location location) {
        // do nothing
    }

    @Specialization(replaces = "removeNonExistantAttr")
    @TruffleBoundary
    protected void removeAttrFallback(DynamicObject attrs) {
        attrs.delete(getPropertyName());
    }

    public abstract static class RemoveGenericPropertyAccessNode extends RemoveFixedPropertyNode {
        private final String name;

        public RemoveGenericPropertyAccessNode(String name) {
            this.name = name;
        }

        @Override
        protected String getPropertyName() {
            return name;
        }
    }

    public abstract static class RemoveNamesPropertyAccessNode extends RemoveFixedPropertyNode {
        @Override
        protected String getPropertyName() {
            return RRuntime.NAMES_ATTR_KEY;
        }
    }

    public abstract static class RemoveRowNamesPropertyAccessNode extends RemoveFixedPropertyNode {
        @Override
        protected String getPropertyName() {
            return RRuntime.ROWNAMES_ATTR_KEY;
        }
    }

    public abstract static class RemoveDimPropertyAccessNode extends RemoveFixedPropertyNode {
        @Override
        protected String getPropertyName() {
            return RRuntime.DIM_ATTR_KEY;
        }
    }

    public abstract static class RemoveDimNamesPropertyAccessNode extends RemoveFixedPropertyNode {
        @Override
        protected String getPropertyName() {
            return RRuntime.DIMNAMES_ATTR_KEY;
        }
    }

    public abstract static class RemoveClassPropertyAccessNode extends RemoveFixedPropertyNode {
        @Override
        protected String getPropertyName() {
            return RRuntime.CLASS_ATTR_KEY;
        }
    }

    public abstract static class RemoveTspPropertyAccessNode extends RemoveFixedPropertyNode {
        @Override
        protected String getPropertyName() {
            return RRuntime.TSP_ATTR_KEY;
        }
    }

    public abstract static class RemoveCommentPropertyAccessNode extends RemoveFixedPropertyNode {
        @Override
        protected String getPropertyName() {
            return RRuntime.COMMENT_ATTR_KEY;
        }
    }
}
