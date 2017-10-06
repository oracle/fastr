/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.numericValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;
import static com.oracle.truffle.r.runtime.RError.Message.INVALID_ARGUMENT;
import static com.oracle.truffle.r.runtime.RError.Message.INVALID_FIRST_ARGUMENT;
import static com.oracle.truffle.r.runtime.RError.Message.USE_NULL_ENV_DEFUNCT;
import static com.oracle.truffle.r.runtime.RVisibility.OFF;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.COMPLEX;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.env.REnvironment.PutException;
import com.oracle.truffle.r.runtime.env.frame.FrameSlotChangeMonitor;

/**
 * Note: remove is invoked from builtin wrappers 'rm' and 'remove' that are identical.
 */
@RBuiltin(name = "remove", visibility = OFF, kind = INTERNAL, parameterNames = {"list", "envir", "inherits"}, behavior = COMPLEX)
public abstract class Rm extends RBuiltinNode.Arg3 {

    private final BranchProfile invalidateProfile = BranchProfile.create();

    static {
        Casts casts = new Casts(Rm.class);
        casts.arg("list").mustBe(stringValue(), INVALID_FIRST_ARGUMENT);
        casts.arg("envir").mustNotBeNull(USE_NULL_ENV_DEFUNCT).mustBe(REnvironment.class, INVALID_ARGUMENT, "envir");
        casts.arg("inherits").mustBe(numericValue(), INVALID_ARGUMENT, "inherits").asLogicalVector().findFirst().map(toBoolean());
    }

    @Specialization
    @TruffleBoundary
    protected Object rm(RAbstractStringVector list, REnvironment envir, @SuppressWarnings("unused") boolean inherits) {
        try {
            for (int i = 0; i < list.getLength(); i++) {
                if (envir == REnvironment.globalEnv()) {
                    removeFromFrame(envir.getFrame(), list.getDataAt(i));
                } else {
                    envir.rm(list.getDataAt(i));
                }
            }
        } catch (PutException ex) {
            throw error(ex);
        }
        return RNull.instance;
    }

    private void removeFromFrame(Frame frame, String x) {
        // standard case for lookup in current frame
        Frame frm = frame;
        FrameSlot fs = frame.getFrameDescriptor().findFrameSlot(x);
        while (fs == null && frm != null) {
            frm = RArguments.getEnclosingFrame(frm);
            if (frm != null) {
                fs = frm.getFrameDescriptor().findFrameSlot(x);
            }
        }
        if (fs == null) {
            warning(RError.Message.UNKNOWN_OBJECT, x);
        } else {
            // use null (not an R value) to represent "undefined"
            FrameSlotChangeMonitor.setObjectAndInvalidate(frm, fs, null, false, invalidateProfile);
        }
    }
}
