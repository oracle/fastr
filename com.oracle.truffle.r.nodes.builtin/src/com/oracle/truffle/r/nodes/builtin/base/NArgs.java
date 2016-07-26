/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RPromise;

// TODO Figure out how to distinguish f(,,a) from f(a) - RMissing is used in both contexts
@RBuiltin(name = "nargs", kind = PRIMITIVE, parameterNames = {})
public abstract class NArgs extends RBuiltinNode {

    private final BranchProfile isPromiseProfile = BranchProfile.create();

    @Specialization
    protected int doNArgs(VirtualFrame frame) {
        int result = 0;
        if (RArguments.getFunction(frame) == null) {
            return RRuntime.INT_NA;
        }
        int l = RArguments.getArgumentsLength(frame);
        for (int i = 0; i < l; i++) {
            Object arg = RArguments.getArgument(frame, i);
            if (arg instanceof RPromise) {
                isPromiseProfile.enter();
                RPromise promise = (RPromise) arg;
                if (!promise.isDefaultArgument()) {
                    result++;
                }
            } else if (arg instanceof RArgsValuesAndNames) {
                result += ((RArgsValuesAndNames) arg).getLength();
            } else if (!(arg instanceof RMissing)) {
                result++;
            }
        }
        return result;
    }
}
