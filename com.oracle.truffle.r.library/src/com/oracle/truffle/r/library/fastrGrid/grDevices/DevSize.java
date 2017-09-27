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

import static com.oracle.truffle.r.library.fastrGrid.device.DrawingContext.INCH_TO_POINTS_FACTOR;

import com.oracle.truffle.r.library.fastrGrid.GridContext;
import com.oracle.truffle.r.library.fastrGrid.device.GridDevice;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;

/**
 * Unlike {@link com.oracle.truffle.r.library.fastrGrid.graphics.CPar} with argument {@code din},
 * which returns the size in inches, {@code dev.size} returns the size in pixels.
 */
public final class DevSize extends RExternalBuiltinNode.Arg0 {
    static {
        Casts.noCasts(DevSize.class);
    }

    @Override
    public RDoubleVector execute() {
        GridDevice dev = GridContext.getContext().getCurrentDevice();
        double width = dev.getWidth() * INCH_TO_POINTS_FACTOR;
        double height = dev.getHeight() * INCH_TO_POINTS_FACTOR;
        return RDataFactory.createDoubleVector(new double[]{width, height}, RDataFactory.COMPLETE_VECTOR);
    }
}
