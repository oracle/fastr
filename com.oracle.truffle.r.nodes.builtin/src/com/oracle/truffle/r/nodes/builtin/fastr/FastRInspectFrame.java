/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RVisibility;
import com.oracle.truffle.r.runtime.builtins.RBehavior;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.builtins.RBuiltinKind;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.env.frame.FrameSlotChangeMonitor;

@RBuiltin(name = ".fastr.inspect.frame", visibility = RVisibility.ON, kind = RBuiltinKind.PRIMITIVE, parameterNames = {"name", "idx"}, behavior = RBehavior.COMPLEX)
public abstract class FastRInspectFrame extends RBuiltinNode.Arg2 {
    static {
        Casts casts = new Casts(FastRInspectFrame.class);
        casts.arg("name").allowMissing().asStringVector().findFirst();
        casts.arg("idx").allowMissing().asIntegerVector().findFirst();
    }

    @Specialization
    @TruffleBoundary
    public Object inspectFrame(String frameName, RMissing frameIdx) {
        String inspectionText = FrameSlotChangeMonitor.findAndLogFrameDescriptorByName(frameName);
        if (inspectionText == null) {
            return RNull.instance;
        } else {
            return RDataFactory.createStringVectorFromScalar(inspectionText);
        }
    }

    @Specialization
    @TruffleBoundary
    public Object inspectFrameByIdx(RMissing frameName, int frameIdx) {
        String inspectionText = FrameSlotChangeMonitor.findAndLogFrameDescriptorByIdx(frameIdx);
        if (inspectionText == null) {
            return RNull.instance;
        } else {
            return RDataFactory.createStringVectorFromScalar(inspectionText);
        }
    }

    @Fallback
    public Object fallBack(Object frameName, Object frameIdx) {
        throw error(Message.GENERIC, "Wrong arguments to .fastr.inspect.frame - exactly one of `name` or `idx` argument must not be missing");
    }
}
