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
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.graphics;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.doubleValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.size;
import static com.oracle.truffle.r.nodes.builtin.casts.fluent.CastNodeBuilder.newCastBuilder;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.library.graphics.core.DrawingParameters;
import com.oracle.truffle.r.library.graphics.core.GraphicsDevice;
import com.oracle.truffle.r.library.graphics.core.GraphicsEngine;
import com.oracle.truffle.r.library.graphics.core.GraphicsEngineImpl;
import com.oracle.truffle.r.library.graphics.core.geometry.Coordinates;
import com.oracle.truffle.r.library.graphics.core.geometry.CoordinatesFactory;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.nodes.unary.CastNode;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RNull;

public class GraphicsCCalls {
    public static final class C_PlotXY extends RExternalBuiltinNode {

        @Child private CastNode castXYNode = newCastBuilder().mustBe(doubleValue().and(size(2))).asDoubleVector().buildCastNode();

        static {
            Casts.noCasts(C_PlotXY.class);
        }

        @Override
        @TruffleBoundary
        public RNull call(RArgsValuesAndNames args) {
            RDoubleVector xyVector = (RDoubleVector) castXYNode.execute(args.getArgument(0));
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

        static {
            Casts.noCasts(C_Par.class);
        }

        @Override
        @TruffleBoundary
        public Object call(RArgsValuesAndNames args) {
            // pch
            return RDataFactory.createIntVectorFromScalar(1);
        }
    }

    @SuppressWarnings("unused")
    public static final class C_mtext extends RExternalBuiltinNode {
        private Object text;
        private double side = 3.;
        private double line = 0.;
        private boolean outer = true;
        private double adj = RRuntime.DOUBLE_NA;
        private double at = RRuntime.DOUBLE_NA;
        private double padj = RRuntime.DOUBLE_NA;
        private double cex = RRuntime.DOUBLE_NA;
        private double col = RRuntime.DOUBLE_NA;
        private double font = RRuntime.DOUBLE_NA;

        @Child private CastNode firstDoubleCast = newCastBuilder().asDoubleVector().findFirst().buildCastNode();

        static {
            Casts.noCasts(C_mtext.class);
        }

        @Override
        @TruffleBoundary
        public Object call(RArgsValuesAndNames args) {
            extractArgumentsFrom(args);
            return RNull.instance;
        }

        private void extractArgumentsFrom(RArgsValuesAndNames args) {
            // text = args.getArgument(0); // postpone for now
            side = extractFirstDoubleValueFrom(args.getArgument(1));
            line = extractFirstDoubleValueFrom(args.getArgument(2));
            // outer = extractFirstDoubleValueFrom(args.getArgument(3));
            at = extractFirstDoubleValueFrom(args.getArgument(4));
            adj = extractFirstDoubleValueFrom(args.getArgument(5));
            padj = extractFirstDoubleValueFrom(args.getArgument(6));
            cex = extractFirstDoubleValueFrom(args.getArgument(7));
            // col = extractFirstDoubleValueFrom(args.getArgument(8));
            font = extractFirstDoubleValueFrom(args.getArgument(9));
        }

        private double extractFirstDoubleValueFrom(Object arg) {
            return (Double) firstDoubleCast.execute(arg);
        }
    }
}
