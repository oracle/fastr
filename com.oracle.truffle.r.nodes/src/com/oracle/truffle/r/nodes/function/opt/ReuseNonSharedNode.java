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
package com.oracle.truffle.r.nodes.function.opt;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.runtime.data.RShareable;
import com.oracle.truffle.r.runtime.data.RSharingAttributeStorage;

/**
 * Internal node that should be used whenever you want to alter some data: if the data is shared,
 * then it creates a copy, otherwise it returns the data. It does not increment the reference count
 * of the result in either case, but that is typically handled by write variable node, put container
 * element node or by put attribute node.
 */
public abstract class ReuseNonSharedNode extends Node {

    public static ReuseNonSharedNode create() {
        return ReuseNonSharedNodeGen.create();
    }

    public abstract Object execute(Object value);

    @Specialization
    protected RShareable getStorage(RSharingAttributeStorage value,
                    @Cached("createBinaryProfile()") ConditionProfile isSharedProfile,
                    @Cached("createClassProfile()") ValueProfile copyProfile) {
        if (isSharedProfile.profile(value.isShared())) {
            RShareable res = copyProfile.profile(value).copy();
            assert res.isTemporary();
            return res;
        }
        return value;
    }

    @Specialization(replaces = "getStorage")
    protected static RShareable getRShareable(RShareable value,
                    @Cached("createBinaryProfile()") ConditionProfile isSharedProfile) {
        if (isSharedProfile.profile(value.isShared())) {
            RShareable res = value.copy();
            return res;
        }
        return value;
    }

    protected static boolean isRShareable(Object value) {
        return value instanceof RShareable;
    }

    @Specialization(guards = "!isRShareable(value)")
    protected static Object getNonShareable(Object value) {
        return value;
    }
}
