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
package com.oracle.truffle.r.nodes.builtin.fastr;

import static com.oracle.truffle.r.runtime.builtins.RBehavior.COMPLEX;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RSharingAttributeStorage;

/**
 * Created as primitive function to avoid incrementing reference count for the argument.
 *
 * returns -1 for non-shareable, 0 for private, 1 for temp, 2 for shared and SHARED_PERMANENT_VAL
 * for permanent shared
 */
@RBuiltin(name = ".fastr.refcountinfo", kind = PRIMITIVE, parameterNames = {""}, behavior = COMPLEX)
public abstract class FastRRefCountInfo extends RBuiltinNode.Arg1 {

    static {
        Casts.noCasts(FastRRefCountInfo.class);
    }

    @Specialization
    protected int refcount(Object x) {
        if (RSharingAttributeStorage.isShareable(x)) {
            RSharingAttributeStorage s = (RSharingAttributeStorage) x;
            if (s.isTemporary()) {
                return 0;
            } else if (s.isSharedPermanent()) {
                return RSharingAttributeStorage.SHARED_PERMANENT_VAL;
            } else if (s.isShared()) {
                return 2;
            } else {
                return 1;
            }
        } else {
            return -1;
        }
    }
}
