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
package com.oracle.truffle.r.nodes.function;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.runtime.data.nodes.attributes.HasFixedAttributeNode;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

/**
 * Helper node intended for fast-path check of whether an object may have some dispatch (S3/S4). The
 * contract is that if it gives {@code true}, then the object certainly does not have class and is
 * not S4 object, but otherwise, nothing is guaranteed.
 */
public abstract class IsNotObject extends RBaseNode {

    public abstract boolean execute(Object value);

    public static IsNotObject create() {
        return IsNotObjectNodeGen.create();
    }

    @Specialization
    protected static boolean doInt(@SuppressWarnings("unused") int value) {
        return true;
    }

    @Specialization
    protected static boolean doDouble(@SuppressWarnings("unused") double value) {
        return true;
    }

    @Specialization
    protected static boolean doLogical(@SuppressWarnings("unused") byte value) {
        return true;
    }

    @Specialization
    protected static boolean doString(@SuppressWarnings("unused") String value) {
        return true;
    }

    @Specialization
    public static boolean doOthers(RAttributable value,
                    @Cached("createClass()") HasFixedAttributeNode hasClassAttributeNode) {
        boolean result = !value.isS4() && !hasClassAttributeNode.execute(value);
        // result => !ClassHierarchyNode.hasClass
        assert !result || !ClassHierarchyNode.hasClass(value, RRuntime.CLASS_ATTR_KEY);
        return result;
    }

    @Fallback
    public static boolean doFallback(@SuppressWarnings("unused") Object value) {
        return false;
    }
}
