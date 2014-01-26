/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.base;

import java.util.*;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin("attr")
public abstract class Attr extends RBuiltinNode {

    private static Object searchKeyPartial(Map<String, Object> attributes, String name) {
        Object val = RNull.instance;
        for (Map.Entry<String, Object> e : attributes.entrySet()) {
            if (e.getKey().startsWith(name)) {
                if (val == RNull.instance) {
                    val = e.getValue();
                } else {
                    // non-unique match
                    return RNull.instance;
                }
            }
        }
        return val;
    }

    @Specialization
    public Object attr(RAbstractVector vector, String name) {
        Map<String, Object> attributes = vector.getAttributes();
        if (attributes == null) {
            return RNull.instance;
        } else {
            Object result = attributes.get(name);
            if (result == null) {
                return searchKeyPartial(attributes, name);
            }
            return result;
        }
    }

    @Specialization
    public Object dim(RAbstractVector vector, RStringVector name) {
        return attr(vector, name.getDataAt(0));
    }
}
