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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.library.graphics.core.DrawingParameters;
import com.oracle.truffle.r.library.graphics.core.GraphicsDevice;
import com.oracle.truffle.r.library.graphics.core.GraphicsEngine;
import com.oracle.truffle.r.library.graphics.core.GraphicsEngineImpl;
import com.oracle.truffle.r.library.graphics.core.geometry.Coordinates;
import com.oracle.truffle.r.library.graphics.core.geometry.CoordinatesFactory;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;

public class GraphicsCCalls {
    public static final class C_PlotXY extends RExternalBuiltinNode {

        @Override
        @TruffleBoundary
        public RNull call(RArgsValuesAndNames args) {
            RDoubleVector xyVector = ((RAbstractDoubleVector) args.getArgument(0)).materialize();
            assert xyVector.getLength() % 2 == 0 : "wrong size of vector";
            getGraphicsEngine().setCurrentGraphicsDeviceMode(GraphicsDevice.Mode.GRAPHICS_ON);
            drawWithLines(xyVector);
            return RNull.instance;
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

    public static final class C_Par extends RExternalBuiltinNode {

        @Override
        @TruffleBoundary
        public Object call(RArgsValuesAndNames args) {
            // pch
            return RDataFactory.createIntVectorFromScalar(1);
        }
    }

    public static final class C_mtext extends RExternalBuiltinNode {
        private Object text;
        private RAbstractDoubleVector side;
        private RAbstractDoubleVector line;
        private RAbstractDoubleVector outer;
        private RAbstractDoubleVector adj;
        private RAbstractDoubleVector at;
        private RAbstractDoubleVector padj;
        private RAbstractDoubleVector cex;
        private RAbstractDoubleVector col;
        private RAbstractDoubleVector font;

        @Override
        @TruffleBoundary
        public Object call(RArgsValuesAndNames args) {
            extractArgumentsFrom(args);
            return RNull.instance;
        }

        private void extractArgumentsFrom(RArgsValuesAndNames args) {
            text = args.getArgument(0); // postpone for now
            side = extractDoubleVectorFrom(args.getArgument(1));
            line = extractDoubleVectorFrom(args.getArgument(2));
            outer = extractDoubleVectorFrom(args.getArgument(3));
            at = extractDoubleVectorFrom(args.getArgument(4));
            adj = extractDoubleVectorFrom(args.getArgument(5));
            padj = extractDoubleVectorFrom(args.getArgument(6));
            cex = extractDoubleVectorFrom(args.getArgument(7));
            col = extractDoubleVectorFrom(args.getArgument(8));
            font = extractDoubleVectorFrom(args.getArgument(9));
        }

        private RAbstractDoubleVector extractDoubleVectorFrom(Object arg) {
            boolean isNA = arg instanceof Byte && RRuntime.isNA((byte) arg);
            return isNA || arg == RNull.instance ? null : castDouble(castVector(arg));
        }
    }
}
