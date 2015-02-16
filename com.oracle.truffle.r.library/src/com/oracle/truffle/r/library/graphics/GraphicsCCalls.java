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
package com.oracle.truffle.r.library.graphics;

import com.oracle.truffle.r.library.graphics.core.*;
import com.oracle.truffle.r.library.graphics.core.geometry.*;
import com.oracle.truffle.r.runtime.data.*;

public class GraphicsCCalls {
    public static void plotXy(RDoubleVector xyVector) {
        assert xyVector.getLength() % 2 == 0 : "wrong size of vector";
        getGraphicsEngine().setCurrentGraphicsDeviceMode(GraphicsDevice.Mode.GRAPHICS_ON);
        drawWithLines(xyVector);
    }

    public static Object par(@SuppressWarnings("unused") RArgsValuesAndNames args) {
        // pch
        return RDataFactory.createIntVectorFromScalar(1);
    }

    private static void drawWithLines(RDoubleVector xyVector) {
        // todo implement coordinate systems units conversion like in GConvert (graphics.c)
        setClipRect();
        DrawingParameters adoptedParameters = adoptCurrentDeviceDrawingParameters();
        Coordinates coordinates = CoordinatesFactory.createByXYVector(xyVector);
        getGraphicsEngine().drawPolyline(coordinates, adoptedParameters);
    }

    private static DrawingParameters adoptCurrentDeviceDrawingParameters() {
        // todo Now adoption as for today. Transcribe from gcontextFromGM() (graphics.c)
        return getCurrentGraphicsDevice().getDrawingParameters();
    }

    private static void setClipRect() {
        // todo Transcrive from Gclip() (graphics.c)
        getGraphicsEngine().setCurrentGraphicsDeviceClipRect(0, 0, 0, 0);
    }

    private static GraphicsDevice getCurrentGraphicsDevice() {
        return getGraphicsEngine().getCurrentGraphicsDevice();
    }

    private static GraphicsEngine getGraphicsEngine() {
        return GraphicsEngineImpl.getInstance();
    }

}
