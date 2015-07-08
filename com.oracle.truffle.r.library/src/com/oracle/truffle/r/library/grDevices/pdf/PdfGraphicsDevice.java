/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.library.grDevices.pdf;

import com.oracle.truffle.r.library.graphics.core.DrawingParameters;
import com.oracle.truffle.r.library.graphics.core.GraphicsDevice;
import com.oracle.truffle.r.library.graphics.core.geometry.Coordinates;
import com.oracle.truffle.r.runtime.RRuntime;

public class PdfGraphicsDevice implements GraphicsDevice{
    private final Parameters deviceParameters;

    public PdfGraphicsDevice(Parameters deviceParameters){
        this.deviceParameters = deviceParameters;
    }

    @Override
    public void deactivate() {

    }

    @Override
    public void activate() {

    }

    @Override
    public void close() {

    }

    @Override
    public DrawingParameters getDrawingParameters() {
        return null;
    }

    @Override
    public void setMode(Mode newMode) {

    }

    @Override
    public Mode getMode() {
        return null;
    }

    @Override
    public void setClipRect(double x1, double y1, double x2, double y2) {

    }

    @Override
    public void drawPolyline(Coordinates coordinates, DrawingParameters drawingParameters) {

    }

    public static class Parameters {
        public String filePath;
        public String paperSize = "special";
        public String fontFamily = "Helvetica";
        public String encoding = "default";
        public String bg = "transparent";
        public String fg = "black";
        public double width = 7.;
        public double height = 7.;
        public double pointSize = 12.;
        public byte oneFile = RRuntime.LOGICAL_TRUE;
        public byte pageCenter = RRuntime.LOGICAL_TRUE;
        public String title = "R Graphics Output";
        public String[] fonts;
        public int majorVersion = 1;
        public int minorVersion = 4;
        public String colormodel = "srgb";
        public byte useDingbats = RRuntime.LOGICAL_TRUE;
        public byte useKerning = RRuntime.LOGICAL_TRUE;
        public byte fillOddEven = RRuntime.LOGICAL_FALSE;
        public byte compress = RRuntime.LOGICAL_TRUE;
    }
}
