/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.util.List;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.r.runtime.RRuntimeASTAccess.ArrayAttributeAccess;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RAttributesLayout;
import com.oracle.truffle.r.runtime.data.RAttributesLayout.AttrsLayout;
import com.oracle.truffle.r.runtime.data.RAttributesLayout.RAttribute;

@GenerateUncached
public abstract class ArrayAttributeNode extends AttributeIterativeAccessNode implements ArrayAttributeAccess {

    private static final RAttribute[] EMPTY = new RAttribute[0];

    public static ArrayAttributeNode create() {
        return ArrayAttributeNodeGen.create();
    }

    @Override
    public abstract RAttribute[] execute(Object attrs);

    @Specialization(limit = "getCacheLimit()", guards = {"attrsLayout != null", "attrsLayout.shape.check(attrs)"})
    @ExplodeLoop
    protected RAttribute[] getArrayFromConstantLayouts(DynamicObject attrs,
                    @Cached("findLayout(attrs, createLoopProfiles())") AttrsLayout attrsLayout) {
        final Property[] props = attrsLayout.properties;
        RAttribute[] result = new RAttribute[props.length];
        for (int i = 0; i < props.length; i++) {
            Object value = readProperty(attrs, attrsLayout.shape, props[i]);
            result[i] = new RAttributesLayout.AttrInstance((String) props[i].getKey(), value);
        }

        return result;
    }

    @Specialization(replaces = "getArrayFromConstantLayouts")
    protected RAttribute[] getArrayFallback(DynamicObject attrs) {
        Shape shape = attrs.getShape();
        List<Property> props = shape.getPropertyList();
        RAttribute[] result = new RAttribute[props.size()];
        for (int i = 0; i < result.length; i++) {
            Property p = props.get(i);
            Object value = readProperty(attrs, shape, p);
            result[i] = new RAttributesLayout.AttrInstance((String) p.getKey(), value);
        }

        return result;
    }

    @Specialization(guards = "hasAttributes(x)")
    protected RAttribute[] getArrayFallback(RAttributable x,
                    @Cached() ArrayAttributeNode recursive) {
        return recursive.execute(x.getAttributes());
    }

    @Specialization(guards = "!hasAttributes(x)")
    protected RAttribute[] getArrayFallback(@SuppressWarnings("unused") RAttributable x) {
        return EMPTY;
    }

    protected static boolean hasAttributes(RAttributable x) {
        return x.getAttributes() != null;
    }
}
