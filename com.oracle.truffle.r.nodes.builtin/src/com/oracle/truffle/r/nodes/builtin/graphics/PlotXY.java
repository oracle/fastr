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
 * Copyright (c) 2014, 2014, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.graphics;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RInvisibleBuiltinNode;
import com.oracle.truffle.r.nodes.graphics.core.*;
import com.oracle.truffle.r.nodes.graphics.core.geometry.Coordinates;
import com.oracle.truffle.r.nodes.graphics.core.geometry.CoordinatesFactory;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RNull;

import static com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import static com.oracle.truffle.r.runtime.RBuiltinKind.INTERNAL;

@RBuiltin(name = "plotXY", kind = INTERNAL, parameterNames = {"xy"})
public abstract class PlotXY extends RInvisibleBuiltinNode {

    @Specialization
    @TruffleBoundary
    protected RNull plotXy(RDoubleVector xyVector) {
        controlVisibility();
        assert xyVector.getLength() % 2 == 0 : "wrong size of vector";
        getGraphicsEngine().setCurrentGraphicsDeviceMode(GraphicsDevice.Mode.GRAPHICS_ON);
        drawWithLines(xyVector);
        return RNull.instance;
    }

    private void drawWithLines(RDoubleVector xyVector) {
        // todo implement coordinate systems units conversion like in GConvert (graphics.c)
        setClipRect();
        DrawingParameters adoptedParameters = adoptCurrentDeviceDrawingParameters();
        Coordinates coordinates = CoordinatesFactory.createByXYVector(xyVector);
        getGraphicsEngine().drawPolyline(coordinates, adoptedParameters);
    }

    private DrawingParameters adoptCurrentDeviceDrawingParameters() {
        //todo Now adoption as for today. Transcribe from gcontextFromGM() (graphics.c)
        return getCurrentGraphicsDevice().getDrawingParameters();
    }

    private void setClipRect() {
        //todo Transcrive from Gclip() (graphics.c)
        getGraphicsEngine().setCurrentGraphicsDeviceClipRect(0, 0, 0, 0);
    }

    private GraphicsDevice getCurrentGraphicsDevice() {
        return getGraphicsEngine().getCurrentGraphicsDevice();
    }

    private GraphicsEngine getGraphicsEngine() {
        return GraphicsEngineImpl.getInstance();
    }
}
