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

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.data.RAttributesLayout;
import com.oracle.truffle.r.runtime.data.RAttributesLayout.AttrsLayout;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

public abstract class AttributeAccessNode extends RBaseNode {

    protected static final int CACHE_LIMIT = RAttributesLayout.LAYOUTS.length;

    private final ConditionProfile[] loopProfiles;
    private final BranchProfile nullLayoutProfile = BranchProfile.create();

    protected AttributeAccessNode() {
        this.loopProfiles = new ConditionProfile[RAttributesLayout.LAYOUTS.length];
        for (int i = 0; i < RAttributesLayout.LAYOUTS.length; i++) {
            loopProfiles[i] = ConditionProfile.createBinaryProfile();
        }
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

    protected int findAttrIndexInLayout(String name, AttrsLayout attrsLayout) {
        if (attrsLayout == null) {
            nullLayoutProfile.enter();
            return -1;
        }

        for (int i = 0; i < attrsLayout.properties.length; i++) {
            if (name.equals(attrsLayout.properties[i])) {
                return i;
            }
        }
        return -1;
    }

    protected static boolean shapeCheck(Shape shape, DynamicObject attrs) {
        return shape != null && shape.check(attrs);
    }

    protected static Shape lookupShape(DynamicObject attrs) {
        CompilerAsserts.neverPartOfCompilation();
        return attrs.getShape();
    }

}
