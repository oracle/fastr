/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (C) 1995, 1996  Robert Gentleman and Ross Ihaka
 * Copyright (C) 1998 Ross Ihaka
 * Copyright (c) 1998--2011, The R Core Team
 * Copyright (c) 2002--2010, The R Foundation
 * Copyright (C) 2005--2006, Morten Welinder
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.grDevices;

import com.oracle.truffle.r.library.graphics.core.DrawingParameters;
import com.oracle.truffle.r.library.graphics.core.GraphicsDevice;
import com.oracle.truffle.r.library.graphics.core.geometry.Coordinates;

public final class NullGraphicsDevice implements GraphicsDevice {
    private static final NullGraphicsDevice instance = new NullGraphicsDevice();

    public static NullGraphicsDevice getInstance() {
        return instance;
    }

    @Override
    public void deactivate() {
        throw createExceptionForMethod("deactivate");
    }

    @Override
    public void activate() {
        throw createExceptionForMethod("activate");
    }

    @Override
    public void close() {
        throw createExceptionForMethod("close");
    }

    @Override
    public DrawingParameters getDrawingParameters() {
        throw createExceptionForMethod("getDrawingParameters");
    }

    @Override
    public void setMode(Mode newMode) {
        throw createExceptionForMethod("setMode");
    }

    @Override
    public Mode getMode() {
        throw createExceptionForMethod("getMode");
    }

    @Override
    public void setClipRect(double x1, double y1, double x2, double y2) {
        throw createExceptionForMethod("setClipRect");
    }

    @Override
    public void drawPolyline(Coordinates coordinates, DrawingParameters drawingParameters) {
        throw createExceptionForMethod("drawPolyline");
    }

    private static RuntimeException createExceptionForMethod(String methodName) {
        return new IllegalStateException("Call to " + methodName + " of Null-device");
    }
}
