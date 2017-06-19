/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.library.fastrGrid.grDevices;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.numericValue;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.library.fastrGrid.GridContext;
import com.oracle.truffle.r.library.fastrGrid.GridState;
import com.oracle.truffle.r.library.fastrGrid.device.GridDevice;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;

public abstract class DevHoldFlush extends RExternalBuiltinNode.Arg1 {
    static {
        Casts casts = new Casts(DevHoldFlush.class);
        casts.arg(0).mustBe(numericValue()).asIntegerVector().findFirst();
    }

    public static DevHoldFlush create() {
        return DevHoldFlushNodeGen.create();
    }

    @Specialization
    @TruffleBoundary
    int doInteger(int num) {
        GridState gridState = GridContext.getContext().getGridState();
        int result = gridState.getDevHoldCount();
        GridDevice currentDevice = GridContext.getContext().getCurrentDevice();
        if (currentDevice == null) {
            return result;
        }
        if (num < 0) {
            result = gridState.setDevHoldCount(Math.max(0, result + num));
            if (result == 0) {
                currentDevice.flush();
            }
        } else if (num > 0) {
            if (result == 0) {
                currentDevice.hold();
            }
            result = gridState.setDevHoldCount(result + num);
        }
        return result;
    }
}
