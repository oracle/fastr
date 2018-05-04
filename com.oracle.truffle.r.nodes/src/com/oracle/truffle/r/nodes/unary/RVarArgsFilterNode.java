/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.unary;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.r.nodes.builtin.casts.MessageData;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.REmpty;

public abstract class RVarArgsFilterNode extends CastNode {

    public static RVarArgsFilterNode create() {
        return RVarArgsFilterNodeGen.create();
    }

    @ExplodeLoop
    @Specialization(guards = "args.getLength() == cachedLen")
    protected Object cached(RArgsValuesAndNames args,
                    @Cached("create()") BranchProfile errorProfile,
                    @Cached("args.getLength()") int cachedLen) {
        Object[] vals = args.getArguments();
        for (int i = 0; i < cachedLen; i++) {
            if (vals[i] == REmpty.instance) {
                errorProfile.enter();
                throw handleArgumentError(args, new MessageData(Message.ARGUMENT_EMPTY, i + 1));
            }
        }
        return args;
    }

    @Specialization(replaces = "cached")
    protected Object generic(RArgsValuesAndNames args,
                    @Cached("create()") BranchProfile errorProfile) {
        Object[] vals = args.getArguments();
        for (int i = 0; i < vals.length; i++) {
            if (vals[i] == REmpty.instance) {
                errorProfile.enter();
                throw handleArgumentError(args, new MessageData(Message.ARGUMENT_EMPTY, i));
            }
        }
        return args;
    }

    @Fallback
    @TruffleBoundary
    protected Object others(Object obj) {
        String actualCls = obj == null ? "null" : obj.getClass().getSimpleName();
        throw RInternalError.shouldNotReachHere(String.format("RVarArgsFilter should be used only for %s, but used for %s", RArgsValuesAndNames.class.getSimpleName(), actualCls));
    }
}
