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
package com.oracle.truffle.r.library.fastrGrid.graphics;

import com.oracle.truffle.r.library.fastrGrid.GridContext;
import com.oracle.truffle.r.library.fastrGrid.device.GridDevice;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.env.frame.FrameSlotChangeMonitor;

public class CPar extends RExternalBuiltinNode {
    static {
        Casts.noCasts(CPar.class);
    }

    @Override
    protected Object call(RArgsValuesAndNames args) {
        if (args.getSignature().getNonNullCount() > 0) {
            throw error(Message.GENERIC, "Using par for setting device parameters is not supported in FastR grid emulation mode.");
        }

        GridDevice device = GridContext.getContext().getCurrentDevice();
        Object[] names = args.getArguments();
        if (names.length == 1) {
            Object first = args.getArgument(0);
            if (first instanceof RList) {
                names = ((RList) first).getDataWithoutCopying();
            }
            if (names.length == 1) {
                // if there is a single name, do not wrap the result in a list
                return getParam(RRuntime.asString(names[0]), device);
            }
        }

        throw error(Message.GENERIC, "Querying par for anything else than 'din' is not supported in FastR grid emulation mode.");
    }

    private Object getParam(String name, GridDevice device) {
        switch (name) {
            case "din":
                return RDataFactory.createDoubleVector(new double[]{device.getWidth(), device.getHeight()}, RDataFactory.COMPLETE_VECTOR);
            default:
                throw RError.nyi(RError.NO_CALLER, "C_Par paramter '" + name + "'");
        }
    }
}
