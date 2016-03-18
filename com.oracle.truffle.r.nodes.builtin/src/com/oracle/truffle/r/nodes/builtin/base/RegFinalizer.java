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
package com.oracle.truffle.r.nodes.builtin.base;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.RBuiltinKind;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RExternalPtr;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.env.REnvironment;

@RBuiltin(name = "reg.finalizer", kind = RBuiltinKind.INTERNAL, parameterNames = {"e", "f", "onexit"})
public abstract class RegFinalizer extends RBuiltinNode {
    @Specialization
    protected RNull doRegFinalizer(RExternalPtr ext, RFunction fun, byte onexit) {
        return doRegFinalizerEither(ext, fun, onexit);
    }

    @Specialization
    protected RNull doRegFinalizer(REnvironment env, RFunction fun, byte onexit) {
        return doRegFinalizerEither(env, fun, onexit);
    }

    @SuppressWarnings("unused")
    private RNull doRegFinalizerEither(Object env, RFunction fun, byte onexit) {
        if (onexit == RRuntime.LOGICAL_NA) {
            throw RError.error(this, RError.Message.REG_FINALIZER_THIRD);
        }
        // TODO the actual work
        return RNull.instance;
    }

    @SuppressWarnings("unused")
    @Fallback
    protected RNull doRegFinalizer(Object env, Object fun, byte onexit) {
        if (fun instanceof RFunction) {
            throw RError.error(this, RError.Message.REG_FINALIZER_FIRST);
        }
        throw RError.error(this, RError.Message.REG_FINALIZER_SECOND);
    }
}
