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

import static com.oracle.truffle.r.runtime.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.RBuiltinKind;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RFunction;

@RBuiltin(name = ".Primitive", kind = PRIMITIVE, parameterNames = "name")
public abstract class Primitive extends RBuiltinNode {
    private final BranchProfile errorProfile = BranchProfile.create();

    @Specialization
    protected RFunction primitive(String name) {
        RFunction function = RContext.lookupBuiltin(name);
        if (function == null || function.getRBuiltin() != null && function.getRBuiltin().getKind() != RBuiltinKind.PRIMITIVE) {
            errorProfile.enter();
            throw RError.error(this, RError.Message.NO_SUCH_PRIMITIVE, name);
        }

        // .Primitive function is validated
        return function;
    }

    @Fallback
    protected RFunction primitive(@SuppressWarnings("unused") Object name) {
        throw RError.error(this, RError.Message.STRING_ARGUMENT_REQUIRED);
    }
}
