/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1997-2014, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.fastrGrid.color;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.asIntegerVector;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.asStringVector;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.doubleValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.integerValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.logicalValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;

import java.util.Arrays;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.library.fastrGrid.GridColorUtils;
import com.oracle.truffle.r.library.fastrGrid.GridContext;
import com.oracle.truffle.r.library.fastrGrid.GridState.GridPalette;
import com.oracle.truffle.r.library.fastrGrid.device.GridColor;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetNamesAttributeNode;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.nodes.function.opt.ShareObjectNode;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

public abstract class Col2RGB extends RExternalBuiltinNode.Arg2 {

    private static final RStringVector NAMES = ShareObjectNode.sharePermanent(RDataFactory.createStringVector(new String[]{"red", "green", "blue"}, true));
    private static final RStringVector NAMES_ALPHA = ShareObjectNode.sharePermanent(RDataFactory.createStringVector(new String[]{"red", "green", "blue", "alpha"}, true));
    private static final GridColor TRANSPARENT_WHITE = new GridColor(255, 255, 255, 0);

    static {
        Casts casts = new Casts(Col2RGB.class);
        casts.arg(0).mapIf(integerValue().or(doubleValue()), asIntegerVector(), asStringVector());
        casts.arg(1).mustBe(logicalValue()).asLogicalVector().findFirst().map(toBoolean());
    }

    public static Col2RGB create() {
        return Col2RGBNodeGen.create();
    }

    @Specialization
    @TruffleBoundary
    Object execute(RAbstractVector col, boolean alpha,
                    @Cached("create()") GetNamesAttributeNode getNames) {
        int length = col.getLength();
        int columns = alpha ? 4 : 3;
        int[] result = new int[length * columns];

        GridPalette palette = GridContext.getContext().getGridState().getPalette();
        int pos = 0;
        if (col instanceof RAbstractIntVector) {
            RAbstractIntVector vector = (RAbstractIntVector) col;

            for (int i = 0; i < length; i++) {
                int value = vector.getDataAt(i);
                GridColor color;
                if (RRuntime.isNA(value) || value == 0) {
                    color = TRANSPARENT_WHITE;
                } else if (value < 0) {
                    throw error(Message.GENERIC, "numerical color values must be >= 0, found " + value);
                } else {
                    color = palette.colors[(value - 1) % palette.colors.length];
                }
                pos = addColor(alpha, result, pos, color);
            }
        } else if (col instanceof RAbstractStringVector) {
            RAbstractStringVector vector = (RAbstractStringVector) col;

            for (int i = 0; i < length; i++) {
                String value = vector.getDataAt(i);
                GridColor color;
                if (value.length() > 0 && value.charAt(0) >= '0' && value.charAt(0) <= '9') {
                    int index = RRuntime.parseIntWithNA(value);
                    if (RRuntime.isNA(index)) {
                        throw error(Message.GENERIC, "invalid color specification \"" + value + "\"");
                    } else if (index == 0) {
                        color = TRANSPARENT_WHITE;
                    } else {
                        color = palette.colors[(index - 1) % palette.colors.length];
                    }
                } else if ("transparent".equals(value)) {
                    color = TRANSPARENT_WHITE;
                } else {
                    color = GridColorUtils.gridColorFromString(value);
                }
                pos = addColor(alpha, result, pos, color);
            }
        } else {
            warning(Message.GENERIC, "supplied color is neither numeric nor character");
            Arrays.fill(result, RRuntime.INT_NA);
        }

        RStringVector names = getNames.getNames(col);
        RList dimNames = RDataFactory.createList(new Object[]{alpha ? NAMES_ALPHA : NAMES, names == null ? RNull.instance : names});
        return RDataFactory.createIntVector(result, false, new int[]{columns, length}, null, dimNames);
    }

    private static int addColor(boolean alpha, int[] result, int start, GridColor color) {
        int pos = start;
        result[pos++] = color.getRed();
        result[pos++] = color.getGreen();
        result[pos++] = color.getBlue();
        if (alpha) {
            result[pos++] = color.getAlpha();
        }
        return pos;
    }
}
