/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.ffi.impl.nodes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.CharSXPWrapper;

public abstract class RNCharNode extends FFIUpCallNode.Arg5 {

    public static RNCharNode create() {
        return RNCharNodeGen.create();
    }

    private static boolean isNAKeptIn(int keepNAInArg, int typeId) {
        if (RRuntime.isNA(keepNAInArg)) {
            return typeId != 2;
        } else {
            return keepNAInArg == RRuntime.LOGICAL_TRUE;
        }
    }

    @Specialization
    int handleString(CharSXPWrapper x, int type, @SuppressWarnings("unused") int allowNA, int keepNAIn, @SuppressWarnings("unused") String msgName,
                    @Cached("createBinaryProfile()") ConditionProfile keepNAProfile) {
        boolean keepNA = keepNAProfile.profile(isNAKeptIn(keepNAIn, type));
        int result;
        String s = x.getContents();
        if (RRuntime.isNA(s)) {
            result = keepNA ? RRuntime.INT_NA : 2;
        } else {
            result = s.length();
        }

        return result;
    }
}
