/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime;

public enum RDispatch {
    DEFAULT(null),
    INTERNAL_GENERIC(null),
    MATH_GROUP_GENERIC("Math"),
    OPS_GROUP_GENERIC("Ops"),
    SUMMARY_GROUP_GENERIC("Summary"),
    COMPLEX_GROUP_GENERIC("Complex"),
    /**
     * The "special" dispatch type is (at the logical level) the same as the default dispatch, but
     * it allows the node to take full control of its arguments (no wrapping in promises will take
     * place). This is only useful in special cases for basic language constructs like "missing".
     */
    SPECIAL(null);

    private final String groupGenericName;

    RDispatch(String groupGenericName) {
        this.groupGenericName = groupGenericName;
    }

    public boolean isGroupGeneric() {
        return groupGenericName != null;
    }

    public String getGroupGenericName() {
        return groupGenericName;
    }
}
