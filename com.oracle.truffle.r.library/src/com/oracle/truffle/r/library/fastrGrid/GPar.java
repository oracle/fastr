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

import static com.oracle.truffle.r.library.fastrGrid.GridUtils.asDouble;
import static com.oracle.truffle.r.library.fastrGrid.GridUtils.asString;
import static com.oracle.truffle.r.library.fastrGrid.GridUtils.getDataAtMod;

import java.util.Arrays;
import java.util.function.Function;

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
    /**
     * In fact means font style: bold, italic, bolditalic or normal.
     */
    private static final int GP_FONT = 8;
    private static final int GP_FONTFAMILY = 9;
    private static final int GP_ALPHA = 10;
    private static final int GP_LINEEND = 11;
    private static final int GP_LINEJOIN = 12;
    private static final int GP_LINEMITRE = 13;
    /**
     * Multiplier of line width {@link #GP_LWD}.
     */
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
    private final DrawingContext indexZeroDrawingCtx;

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
        data[GP_GAMMA] = newDoubleVec(0);   // Note: we do not use this parameter
        data[GP_LTY] = "solid";
        data[GP_LWD] = newDoubleVec(1);
        data[GP_CEX] = newDoubleVec(1);
        data[GP_FONTSIZE] = newDoubleVec(16);
        data[GP_LINEHEIGHT] = newDoubleVec(1.2);
        data[GP_FONT] = RDataFactory.createIntVectorFromScalar(1);
        data[GP_FONTFAMILY] = ""; // means default font
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
            return convertNamedValue(data[GP_LTY], LINE_STYLES.length - 1, "line type", GParDrawingContext::lineTypeFromName, num -> LINE_STYLES[num]);
        }

        @Override
        public double getLineWidth() {
            return asDouble(data[GP_LWD], index) * asDouble(data[GP_LEX], index);
        }

        @Override
        public GridLineJoin getLineJoin() {
            return convertNamedValue(data[GP_LINEJOIN], GridLineJoin.LAST_VALUE, "line join", GParDrawingContext::lineJoinFromName, GridLineJoin::fromInt);
        }

        @Override
        public GridLineEnd getLineEnd() {
            return convertNamedValue(data[GP_LINEEND], GridLineEnd.LAST_VALUE, "line end", GParDrawingContext::lineEndFromName, GridLineEnd::fromInt);
        }

        @Override
        public double getLineMitre() {
            double value = asDouble(data[GP_LINEMITRE], index);
            if (value < 1.) {
                throw RError.error(RError.NO_CALLER, Message.GENERIC, "Invalid line mitre.");
            }
            return value;
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

        /**
         * Converts value to given enum type using either {@code nameMapper} for String values or
         * {@code valueMapper} for integer value, which is first validated to be greater or equal to
         * 0 and less or equal to the {@code maxValue} parameter. If {@code nameMapper} returns
         * {@code null} or integer validation fails, error with given {@code propertyName} in the
         * message is thrown.
         */
        public <T> T convertNamedValue(Object value, int maxValue, String propertyName, Function<String, T> nameMapper, Function<Integer, T> valueMapper) {
            T result = null;
            if (isStringValue(value)) {
                String name = asString(value, index);
                if (name != null) {
                    result = nameMapper.apply(name);
                }
            } else {
                int num = getIntAtMod(value, index);
                if (RRuntime.isNA(num) || num < 0 || num > maxValue) {
                    result = null;
                } else {
                    result = valueMapper.apply(num);
                }
            }
            if (result == null) {
                throw RError.error(RError.NO_CALLER, Message.GENERIC, "Invalid " + propertyName);
            }
            return result;
        }

        private GridColor getGridColor(int listIndex) {
            Object value = data[listIndex];
            String strValue = null;
            if (value instanceof String) {
                strValue = (String) value;
            } else if (value instanceof RAbstractStringVector && ((RAbstractStringVector) value).getLength() > 0) {
                strValue = ((RAbstractStringVector) value).getDataAt(listIndex % ((RAbstractStringVector) value).getLength());
            } else {
                return GridColor.TRANSPARENT;
            }
            GridColor color = GridColorUtils.gridColorFromString(strValue);
            double alpha = asDouble(data[GP_ALPHA], index);
            if (alpha != 1.) {
                int newAlpha = Math.min(255, (int) (alpha * ((color.getAlpha() / 255.0) * 255)));
                return new GridColor(color.getRed(), color.getGreen(), color.getBlue(), newAlpha);
            } else {
                return color;
            }
        }

        private static final byte[] DASHED_LINE = new byte[]{4, 4};
        private static final byte[] DOTTED_LINE = new byte[]{1, 3};
        private static final byte[] DOTDASH_LINE = new byte[]{1, 3, 4, 3};
        private static final byte[] LONGDASH_LINE = new byte[]{7, 3};
        private static final byte[] TWODASH_LINE = new byte[]{2, 2, 6, 2};
        private static final byte[][] LINE_STYLES = new byte[][]{DrawingContext.GRID_LINE_BLANK, DrawingContext.GRID_LINE_SOLID, DASHED_LINE, DOTTED_LINE, DOTDASH_LINE, LONGDASH_LINE, TWODASH_LINE};

        private static byte[] lineTypeFromName(String name) {
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
                    return null;
                }
            }
            return result;
        }

        private static GridLineEnd lineEndFromName(String name) {
            switch (name) {
                case "round":
                    return GridLineEnd.ROUND;
                case "butt":
                    return GridLineEnd.BUTT;
                case "square":
                    return GridLineEnd.SQUARE;
                default:
                    return null;
            }
        }

        private static GridLineJoin lineJoinFromName(String name) {
            switch (name) {
                case "round":
                    return GridLineJoin.ROUND;
                case "mitre":
                    return GridLineJoin.MITRE;
                case "bevel":
                    return GridLineJoin.BEVEL;
                default:
                    return null;
            }
        }

        private static boolean isStringValue(Object lty) {
            return lty instanceof String || lty instanceof RAbstractStringVector;
        }

        // NA indicates error
        private static int getIntAtMod(Object obj, int index) {
            if (obj instanceof Integer) {
                return (int) obj;
            } else if (obj instanceof Double) {
                return (int) ((double) obj);
            } else if (!(obj instanceof RAbstractContainer)) {
                return RRuntime.INT_NA;
            }

            RAbstractContainer value = (RAbstractContainer) obj;
            if (value.getLength() == 0) {
                return RRuntime.INT_NA;
            } else if (value instanceof RAbstractDoubleVector) {
                double realVal = getDataAtMod((RAbstractDoubleVector) value, index);
                return RRuntime.isNA(realVal) ? RRuntime.INT_NA : (int) realVal;
            } else if (value instanceof RAbstractIntVector) {
                return getDataAtMod((RAbstractIntVector) value, index);
            } else {
                return RRuntime.INT_NA;
            }
        }
    }
}
