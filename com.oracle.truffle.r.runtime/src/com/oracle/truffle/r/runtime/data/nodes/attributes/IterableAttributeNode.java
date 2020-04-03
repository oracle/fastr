/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RAttributesLayout;
import com.oracle.truffle.r.runtime.data.RAttributesLayout.AttrsLayout;

@GenerateUncached
public abstract class IterableAttributeNode extends AttributeIterativeAccessNode {

    public static IterableAttributeNode create() {
        return IterableAttributeNodeGen.create();
    }

    public abstract RAttributesLayout.RAttributeIterable execute(Object attr);

    @Specialization(limit = "getCacheLimit()", guards = {"attrsLayout != null", "shapeCheck(attrsLayout.shape, attrs)"})
    protected RAttributesLayout.RAttributeIterable getArrayFromConstantLayouts(DynamicObject attrs,
                    @Cached("findLayout(attrs, createLoopProfiles())") AttrsLayout attrsLayout) {
        return RAttributesLayout.asIterable(attrs, attrsLayout);
    }

    @Specialization(replaces = "getArrayFromConstantLayouts")
    @TruffleBoundary
    protected RAttributesLayout.RAttributeIterable getArrayFallback(DynamicObject attrs) {
        return RAttributesLayout.asIterable(attrs);
    }

    @Specialization
    protected RAttributesLayout.RAttributeIterable getArrayFallback(RAttributable x,
                    @Cached("create()") BranchProfile attrNullProfile,
                    @Cached IterableAttributeNode recursive) {
        DynamicObject attributes = x.getAttributes();

        if (attributes == null) {
            attrNullProfile.enter();
            return RAttributesLayout.RAttributeIterable.EMPTY;
        }

        return recursive.execute(attributes);
    }
}
