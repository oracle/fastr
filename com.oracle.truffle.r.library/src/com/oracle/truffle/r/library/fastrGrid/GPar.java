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

import static com.oracle.truffle.r.library.fastrGrid.GridUtils.asAbstractContainer;
import static com.oracle.truffle.r.library.fastrGrid.GridUtils.asDouble;
import static com.oracle.truffle.r.library.fastrGrid.GridUtils.getDataAtMod;

import java.util.Arrays;

import com.oracle.truffle.r.library.fastrGrid.device.DrawingContext;
import com.oracle.truffle.r.library.fastrGrid.device.DrawingContextDefaults;
import com.oracle.truffle.r.library.fastrGrid.device.GridColor;
import com.oracle.truffle.r.library.fastrGrid.device.GridDevice;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

/**
 * In the context of grid package, GPar is a list that contains the parameters for the drawing, like
 * line style, color, etc. This class wraps the list and provides way to convert it to
 * {@link DrawingContext}. First create instance of {@link GPar} and then use
 * {@link #getDrawingContext(int)} to get the drawing context. Note that grid's gpar can
 * theoretically contain vector as the value of some graphical parameters, in such case, when
 * drawing i-th element, e.g. i-th rectangle in {@link LRect}, we should use i-th (mod length)
 * element of such vector. Note that this is sort of ignored in layout calculations, where we always
 * take the first element. In other words, instance of {@link GPar} represents grid's gpar where the
 * graphical parameter may be vectors, whereas {@link DrawingContext} is flattened view where it is
 * already determined which index is used to access the vectors.
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

    // Note: there is last slot "fontface" which is either unused at all, or only used in R code
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
                    "fontface"
    };
    private static final RStringVector NAMES_VECTOR = (RStringVector) RDataFactory.createStringVector(NAMES, RDataFactory.COMPLETE_VECTOR).makeSharedPermanent();
    private final RList gpar;
    // majority of gpar instances contains only scalar values, for those we make sure we do not
    // create a new drawing context instance for every index.
    private final boolean singleDrawingCtx;
    private DrawingContext indexZeroDrawingCtx;

    public GPar(RList gpar, boolean singleDrawingCtx) {
        this.gpar = gpar;
        this.gpar.makeSharedPermanent();
        this.singleDrawingCtx = singleDrawingCtx;
        indexZeroDrawingCtx = new GParDrawingContext(gpar, 0);
    }

    public static double getCex(RList gpar) {
        return asDouble(gpar.getDataAt(GP_CEX));
    }

    public static GPar create(RList gpar) {
        boolean singleDrawingCtx = true;
        for (int i = 0; i < gpar.getLength(); i++) {
            Object item = gpar.getDataAt(i);
            if (item instanceof RAbstractVector) {
                singleDrawingCtx &= ((RAbstractVector) item).getLength() == 1;
            }
        }
        return new GPar(gpar, singleDrawingCtx);
    }

    public DrawingContext getDrawingContext(int cyclicIndex) {
        if (singleDrawingCtx || cyclicIndex == 0) {
            return indexZeroDrawingCtx;
        }
        return new GParDrawingContext(gpar, cyclicIndex);
    }

    public static RList createNew(GridDevice device) {
        Object[] data = new Object[GP_LENGTH];
        DrawingContextDefaults defaults = device.getDrawingContextDefaults();
        Arrays.fill(data, RNull.instance);
        data[GP_COL] = defaults.color;
        data[GP_FILL] = defaults.fillColor;
        data[GP_GAMMA] = newDoubleVec(0);
        data[GP_LTY] = "solid";
        data[GP_LWD] = newDoubleVec(1);
        data[GP_CEX] = newDoubleVec(1);
        data[GP_FONTSIZE] = newDoubleVec(16);
        data[GP_LINEHEIGHT] = newDoubleVec(1.2);
        data[GP_FONT] = RDataFactory.createIntVectorFromScalar(1);  // TODO: font constants?
        data[GP_FONTFAMILY] = ""; // means default font (probably)
        data[GP_ALPHA] = newDoubleVec(1);
        data[GP_LINEEND] = "round";
        data[GP_LINEJOIN] = "round";
        data[GP_LINEMITRE] = newDoubleVec(10);
        data[GP_LEX] = newDoubleVec(1);
        RList result = RDataFactory.createList(data, NAMES_VECTOR);
        result.makeSharedPermanent();
        return result;
    }

    private static RAbstractDoubleVector newDoubleVec(double val) {
        return RDataFactory.createDoubleVectorFromScalar(val);
    }

    private static final class GParDrawingContext implements DrawingContext {
        private final Object[] data;
        private final int index;

        private GParDrawingContext(RList list, int index) {
            data = list.getDataWithoutCopying();
            this.index = index;
        }

        @Override
        public byte[] getLineType() {
            Object lty = data[GP_LTY];
            if (lty == null || lty == RNull.instance) {
                return DrawingContext.GRID_LINE_SOLID;
            }
            // convert string values
            if (lty instanceof String || lty instanceof RAbstractStringVector) {
                String name = GridUtils.asString(lty, index);
                if (name != null) {
                    return lineTypeFromName(name);
                }
            }
            // convert numeric values
            RAbstractContainer ltyVec = asAbstractContainer(lty);
            int num; // NA will indicate error
            if (ltyVec.getLength() == 0) {
                num = RRuntime.INT_NA;
            } else if (ltyVec instanceof RAbstractDoubleVector) {
                double realVal = getDataAtMod((RAbstractDoubleVector) ltyVec, index);
                num = RRuntime.isNA(realVal) ? RRuntime.INT_NA : (int) realVal;
            } else if (ltyVec instanceof RAbstractIntVector) {
                num = getDataAtMod((RAbstractIntVector) ltyVec, index);
            } else {
                num = RRuntime.INT_NA;
            }
            if (RRuntime.isNA(num) || num < 0 || num >= LINE_STYLES.length) {
                throw RError.error(RError.NO_CALLER, Message.GENERIC, "Invalid line type.");
            }
            return LINE_STYLES[num];
        }

        @Override
        public GridColor getColor() {
            return getGridColor(GP_COL);
        }

        @Override
        public double getFontSize() {
            return asDouble(data[GP_FONTSIZE], index) * asDouble(data[GP_CEX], index);
        }

        @Override
        public GridFontStyle getFontStyle() {
            return GridFontStyle.fromInt(GridUtils.asInt(data[GP_FONT], index));
        }

        @Override
        public String getFontFamily() {
            return GridUtils.asString(data[GP_FONTFAMILY], index);
        }

        @Override
        public double getLineHeight() {
            return asDouble(data[GP_LINEHEIGHT], index);
        }

        @Override
        public GridColor getFillColor() {
            return getGridColor(GP_FILL);
        }

        private GridColor getGridColor(int listIndex) {
            return GridColorUtils.gridColorFromString(GridUtils.asString(data[listIndex], index));
        }

        private static final byte[] DASHED_LINE = new byte[]{4, 4};
        private static final byte[] DOTTED_LINE = new byte[]{1, 3};
        private static final byte[] DOTDASH_LINE = new byte[]{1, 3, 4, 3};
        private static final byte[] LONGDASH_LINE = new byte[]{7, 3};
        private static final byte[] TWODASH_LINE = new byte[]{2, 2, 6, 2};
        private static final byte[][] LINE_STYLES = new byte[][]{DrawingContext.GRID_LINE_BLANK, DrawingContext.GRID_LINE_SOLID, DASHED_LINE, DOTTED_LINE, DOTDASH_LINE, LONGDASH_LINE, TWODASH_LINE};

        private byte[] lineTypeFromName(String name) {
            switch (name) {
                case "solid":
                    return DrawingContext.GRID_LINE_SOLID;
                case "dashed":
                    return DASHED_LINE;
                case "dotted":
                    return DOTTED_LINE;
                case "dotdashed":
                    return DOTDASH_LINE;
                case "longdash":
                    return LONGDASH_LINE;
                case "twodash":
                    return TWODASH_LINE;
                case "blank":
                    return DrawingContext.GRID_LINE_BLANK;
            }
            byte[] result = new byte[name.length()];
            for (int i = 0; i < name.length(); i++) {
                result[i] = (byte) Character.digit(name.charAt(i), 16);
                if (result[i] == -1) {
                    throw RError.error(RError.NO_CALLER, Message.GENERIC, "Unexpected line type '" + name + "'.");
                }
            }
            return result;
        }
    }
}
