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

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.EmptyTypeSystemFlatLayout;
import com.oracle.truffle.r.runtime.data.RAttributesLayout;
import com.oracle.truffle.r.runtime.data.RAttributesLayout.AttrsLayout;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

/**
 * The base class for the nodes obtaining a collection of attributes from an object. This class uses
 * the so-called <i>constant layouts</i> to optimize the retrieval of the list of attributes. The
 * constant layouts is a static collection of the known attribute sets in FastR stored in
 * {@link RAttributesLayout#LAYOUTS}. Since each constant layout has a field providing the list of
 * properties (i.e. attributes) it is unnecessary to invoke method {@link Shape#getPropertyList()},
 * which would be more expensive.
 */
@TypeSystemReference(EmptyTypeSystemFlatLayout.class)
public abstract class AttributeIterativeAccessNode extends RBaseNode {

    protected static final int CACHE_LIMIT = RAttributesLayout.LAYOUTS.length;

    private final ConditionProfile[] loopProfiles;

    protected AttributeIterativeAccessNode() {
        this.loopProfiles = new ConditionProfile[RAttributesLayout.LAYOUTS.length];
        for (int i = 0; i < RAttributesLayout.LAYOUTS.length; i++) {
            loopProfiles[i] = ConditionProfile.createBinaryProfile();
        }
    }

    protected static Shape lookupShape(DynamicObject attrs) {
        CompilerAsserts.neverPartOfCompilation();
        return attrs.getShape();
    }

    @ExplodeLoop
    protected final AttrsLayout findLayout(DynamicObject attrs) {
        Shape attrsShape = attrs.getShape();
        for (int i = 0; i < RAttributesLayout.LAYOUTS.length; i++) {
            AttrsLayout attrsLayout = RAttributesLayout.LAYOUTS[i];
            if (loopProfiles[i].profile(attrsLayout.shape == attrsShape)) {
                return attrsLayout;
            }
        }
        return null;
    }

    protected static boolean shapeCheck(Shape shape, DynamicObject attrs) {
        return shape != null && shape.check(attrs);
    }

    @TruffleBoundary
    protected static Object readProperty(DynamicObject attrs, Shape shape, final Property prop) {
        return prop.get(attrs, shape);
    }
}
