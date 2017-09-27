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

import static com.oracle.truffle.r.library.fastrGrid.device.DrawingContext.INCH_TO_POINTS_FACTOR;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.library.fastrGrid.GPar;
import com.oracle.truffle.r.library.fastrGrid.GridContext;
import com.oracle.truffle.r.library.fastrGrid.device.DrawingContext;
import com.oracle.truffle.r.library.fastrGrid.device.GridDevice;
import com.oracle.truffle.r.library.fastrGrid.grDevices.OpenDefaultDevice;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;

public final class CPar extends RExternalBuiltinNode {
    static {
        Casts.noCasts(CPar.class);
    }

    @Child private OpenDefaultDevice openDefaultDevice = new OpenDefaultDevice();

    @Override
    public Object call(RArgsValuesAndNames args) {
        if (args.getSignature().getNonNullCount() > 0) {
            throw error(Message.GENERIC, "Using par for setting device parameters is not supported in FastR grid emulation mode.");
        }

        openDefaultDevice.execute();
        return getPar(args);
    }

    @TruffleBoundary
    private static Object getPar(RArgsValuesAndNames args) {
        GridDevice device = GridContext.getContext().getCurrentDevice();
        RList names = RDataFactory.createList(args.getArguments());
        // unwrap list if it is the first argument
        if (names.getLength() == 1) {
            Object first = args.getArgument(0);
            if (first instanceof RList) {
                names = (RList) first;
            }
        }

        Object[] result = new Object[names.getLength()];
        String[] resultNames = new String[names.getLength()];
        for (int i = 0; i < names.getLength(); i++) {
            resultNames[i] = RRuntime.asString(names.getDataAt(i));
            result[i] = getParam(resultNames[i], device);
        }
        return RDataFactory.createList(result, RDataFactory.createStringVector(resultNames, RDataFactory.COMPLETE_VECTOR));
    }

    private static Object getParam(String name, GridDevice device) {
        if (name == null) {
            // TODO: a hot-fix to enable package tests (e.g. cluster)
            return RNull.instance;
        }
        switch (name) {
            case "din":
                return RDataFactory.createDoubleVector(new double[]{device.getWidth(), device.getHeight()}, RDataFactory.COMPLETE_VECTOR);
            case "cin":
                /*
                 * character size ‘(width, height)’ in inches. These are the same measurements as
                 * ‘cra’, expressed in different units.
                 *
                 * Note: cin/cra is used in dev.size() to figure out the conversion ratio between
                 * pixels and inches. For the time being what is important is to choose the values
                 * to keep this ratio!
                 */
                double cin = getCurrentDrawingContext().getFontSize() / INCH_TO_POINTS_FACTOR;
                return RDataFactory.createDoubleVector(new double[]{cin, cin}, RDataFactory.COMPLETE_VECTOR);
            case "cra":
                /*
                 * size of default character ‘(width, height)’ in ‘rasters’ (pixels). Some devices
                 * have no concept of pixels and so assume an arbitrary pixel size, usually 1/72
                 * inch. These are the same measurements as ‘cin’, expressed in different units.
                 */
                double cra = getCurrentDrawingContext().getFontSize();
                return RDataFactory.createDoubleVector(new double[]{cra, cra}, RDataFactory.COMPLETE_VECTOR);
            default:
                throw RError.nyi(RError.NO_CALLER, "C_Par parameter '" + name + "'");
        }
    }

    private static DrawingContext getCurrentDrawingContext() {
        return GPar.create(GridContext.getContext().getGridState().getGpar()).getDrawingContext(0);
    }
}
