/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (C) 1995, 1996  Robert Gentleman and Ross Ihaka
 * Copyright (C) 1998 Ross Ihaka
 * Copyright (c) 1998--2014, The R Core Team
 * Copyright (c) 2002--2010, The R Foundation
 * Copyright (C) 2005--2006, Morten Welinder
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.grDevices.pdf;

import com.oracle.truffle.r.library.graphics.core.DrawingParameters;
import com.oracle.truffle.r.library.graphics.core.GraphicsDevice;
import com.oracle.truffle.r.library.graphics.core.geometry.Coordinates;
import com.oracle.truffle.r.runtime.RRuntime;

public class PdfGraphicsDevice implements GraphicsDevice {
    @SuppressWarnings("unused") private final Parameters deviceParameters;

    public PdfGraphicsDevice(Parameters deviceParameters) {
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
