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

import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

public abstract class FixedAttributeAccessNode extends RBaseNode {

    protected static final int CACHE_LIMIT = 3;

    protected final String name;
    protected final Shape[] constantShapes;
    protected final Property[] constantProperties;
    private final ConditionProfile[] loopProfiles;

    protected FixedAttributeAccessNode(String name, Shape[] constantShapes, Property[] constantProperties) {
        assert name.intern() == name;
        assert constantShapes.length == constantProperties.length;
        this.name = name;
        this.constantShapes = constantShapes;
        this.constantProperties = constantProperties;
        this.loopProfiles = new ConditionProfile[constantShapes.length];
        for (int i = 0; i < constantShapes.length; i++) {
            loopProfiles[i] = ConditionProfile.createBinaryProfile();
        }
    }

    @ExplodeLoop
    protected final int findShapeIndex(DynamicObject attrs) {
        Shape shape = attrs.getShape();
        for (int i = 0; i < constantShapes.length; i++) {
            if (loopProfiles[i].profile(constantShapes[i] == shape)) {
                return i;
            }
        }
        return -1;
    }

    protected boolean shapeCheck(DynamicObject attrs, int shapeIndex) {
        return constantShapes[shapeIndex].check(attrs);
    }

}
