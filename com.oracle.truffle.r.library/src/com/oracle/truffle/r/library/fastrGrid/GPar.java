/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (C) 2001-3 Paul Murrell
 * Copyright (c) 1998-2013, The R Core Team
 * Copyright (c) 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.fastrGrid;

import java.util.Arrays;

import com.oracle.truffle.r.library.fastrGrid.device.DrawingContext;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;

/**
 * In the context of grid package, GPar is a list that contains the parameters for the drawing, like
 * line style, color, etc. This class wraps the list and provides type-safe access to its elements.
 */
public final class GPar {
    private static final int GP_FILL = 0;
    private static final int GP_COL = 1;
    private static final int GP_GAMMA = 2;
    private static final int GP_LTY = 3;
    private static final int GP_LWD = 4;

    /**
     * Multiplier added to the final font size.
     */
    private static final int GP_CEX = 5;

    /**
     * Font size in points, however, the real font size will be this multiplied by {@link #GP_CEX}.
     */
    private static final int GP_FONTSIZE = 6;

    /**
     * Size of the line in terms of a multiply of "one line". The final real size of a line is
     * fontsize*cex*lineheight.
     */
    private static final int GP_LINEHEIGHT = 7;
    private static final int GP_FONT = 8;
    private static final int GP_FONTFAMILY = 9;
    private static final int GP_ALPHA = 10;
    private static final int GP_LINEEND = 11;
    private static final int GP_LINEJOIN = 12;
    private static final int GP_LINEMITRE = 13;
    private static final int GP_LEX = 14;
    private static final int GP_FONTFACE = 15;
    private static final int GP_LENGTH = 16;
    private static final String[] NAMES = new String[]{
                    "fill",
                    "col",
                    "gamma",
                    "lty",
                    "lwd",
                    "cex",
                    "fontsize",
                    "lineheight",
                    "font",
                    "fontfamily",
                    "alpha",
                    "lineend",
                    "linejoin",
                    "linemitre",
                    "lex",
                    "fontface"  // TODO: could not find this name in grid sources
    };
    private static final RStringVector NAMES_VECTOR = (RStringVector) RDataFactory.createStringVector(NAMES, RDataFactory.COMPLETE_VECTOR).makeSharedPermanent();

    public static RList createNew() {
        Object[] data = new Object[GP_LENGTH];
        Arrays.fill(data, RNull.instance);
        data[GP_FILL] = "black";
        data[GP_COL] = "black";
        data[GP_GAMMA] = newDoubleVec(0);
        data[GP_LTY] = "solid"; // TODO: LineType enum...
        data[GP_LWD] = newDoubleVec(1);
        data[GP_CEX] = newDoubleVec(1);
        data[GP_FONTSIZE] = newDoubleVec(12);
        data[GP_LINEHEIGHT] = newDoubleVec(1.0);
        data[GP_FONT] = RDataFactory.createIntVectorFromScalar(1);  // TODO: font constants?
        data[GP_FONTFAMILY] = ""; // means default font (probably)
        data[GP_ALPHA] = newDoubleVec(1);
        data[GP_LINEEND] = "round";
        data[GP_LINEJOIN] = "round";
        data[GP_LINEMITRE] = newDoubleVec(10);
        data[GP_LEX] = newDoubleVec(1);
        return RDataFactory.createList(data, NAMES_VECTOR);
    }

    public static DrawingContext asDrawingContext(RList gpar) {
        return new GParDrawingContext(gpar);
    }

    private static RAbstractDoubleVector newDoubleVec(double val) {
        return RDataFactory.createDoubleVectorFromScalar(val);
    }

    private static final class GParDrawingContext implements DrawingContext {
        private final Object[] data;

        private GParDrawingContext(RList list) {
            data = list.getDataWithoutCopying();
        }

        @Override
        public String getColor() {
            String result = RRuntime.asString(data[GP_COL]);
            if (!result.startsWith("#")) {
                result = ColorNames.findByName(result);
            }
            return result == null ? "#FFFFFF" : result;
        }

        @Override
        public double getFontSize() {
            return asDouble(data[GP_FONTSIZE]) * asDouble(data[GP_CEX]);
        }

        @Override
        public double getLineHeight() {
            return asDouble(data[GP_LINEHEIGHT]);
        }

        private static double asDouble(Object val) {
            if (val instanceof Double) {
                return (double) val;
            } else if (val instanceof RAbstractDoubleVector) {
                if (((RAbstractDoubleVector) val).getLength() > 0) {
                    return ((RAbstractDoubleVector) val).getDataAt(0);
                }
            } else if (val instanceof Integer) {
                return (int) val;
            } else if (val instanceof RAbstractIntVector) {
                if (((RAbstractIntVector) val).getLength() > 0) {
                    return ((RAbstractIntVector) val).getDataAt(0);
                }
            }
            throw RError.error(RError.NO_CALLER, Message.GENERIC, "Unexpected non double/integer value in GPar.");
        }
    }
}
