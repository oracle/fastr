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

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.r.runtime.data.RAttributesLayout;
import com.oracle.truffle.r.runtime.data.RAttributesLayout.AttrsLayout;

public abstract class IterableAttributeNode extends AttributeAccessNode {

    public static IterableAttributeNode create() {
        return IterableAttributeNodeGen.create();
    }

    public abstract RAttributesLayout.RAttributeIterable execute(DynamicObject attrs);

    @Specialization(limit = "CACHE_LIMIT", guards = {"attrsLayout != null", "attrsLayout.shape.check(attrs)"})
    protected RAttributesLayout.RAttributeIterable getArrayFromConstantLayouts(DynamicObject attrs,
                    @Cached("findLayout(attrs)") AttrsLayout attrsLayout) {
        return RAttributesLayout.asIterable(attrs, attrsLayout);
    }

    @Specialization(contains = "getArrayFromConstantLayouts")
    protected RAttributesLayout.RAttributeIterable getArrayFallback(DynamicObject attrs) {
        return RAttributesLayout.asIterable(attrs);
    }

}
