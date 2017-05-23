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
package com.oracle.truffle.r.library.fastrGrid;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.abstractVectorValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.library.fastrGrid.GridState.GridPalette;
import com.oracle.truffle.r.library.fastrGrid.PaletteExternalsFactory.CPalette2NodeGen;
import com.oracle.truffle.r.library.fastrGrid.PaletteExternalsFactory.CPaletteNodeGen;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

public final class PaletteExternals {
    private PaletteExternals() {
        // only static members
    }

    /**
     * Implements external {@code C_palette} used in palette R function.
     */
    public abstract static class CPalette extends RExternalBuiltinNode.Arg1 {

        static {
            Casts casts = new Casts(CPalette.class);
            casts.arg(0).mustBe(stringValue());
        }

        public static CPalette create() {
            return CPaletteNodeGen.create();
        }

        @Specialization
        @TruffleBoundary
        public RStringVector updatePalette(RAbstractStringVector palette) {
            GridState state = GridContext.getContext().getGridState();
            GridPalette newPalette = null;
            if (palette.getLength() == 1) {
                if (palette.getDataAt(0).toLowerCase().equals("default")) {
                    newPalette = GridColorUtils.getDefaultPalette();
                } else {
                    throw error(Message.GENERIC, "unknown palette (need >= 2 colors)");
                }
            } else if (palette.getLength() > 1) {
                newPalette = new GridPalette(palette.materialize().getDataCopy());
            }

            String[] original = state.getPalette().colorNames;
            if (newPalette != null) {
                // the contract is that if the argument's length is zero, we only return the palette
                // and
                // do not set anything.
                state.setPalette(newPalette);
            }
            // don't know the completeness, assume the worst rather than finding out
            RStringVector result = RDataFactory.createStringVector(original, RDataFactory.INCOMPLETE_VECTOR);
            result.makeSharedPermanent();
            return result;
        }
    }

    /**
     * Implements external {@code C_palette2} used only internally in grDevices, the parameter are
     * colors encoded in integer already.
     */
    public abstract static class CPalette2 extends RExternalBuiltinNode.Arg1 {
        static {
            Casts casts = new Casts(CPalette2.class);
            casts.arg(0).mustBe(abstractVectorValue());
        }

        public static CPalette2 create() {
            return CPalette2NodeGen.create();
        }

        @Specialization
        @TruffleBoundary
        public RIntVector updatePalette(RAbstractVector palette) {
            GridState state = GridContext.getContext().getGridState();
            int[] newPalette = null;
            if (palette.getLength() > 0) {
                RAbstractIntVector data = GridUtils.asIntVector(palette);
                newPalette = data.materialize().getDataCopy();
            }
            RIntVector result = getResult(state);
            if (newPalette != null) {
                state.setPalette(new GridPalette(newPalette));
            }
            return result;
        }

        private static RIntVector getResult(GridState state) {
            GridPalette palette = state.getPalette();
            int[] result = new int[palette.colors.length];
            boolean complete = true;
            for (int i = 0; i < result.length; i++) {
                result[i] = palette.colors[i].getRawValue();
                complete &= !RRuntime.isNA(result[i]);
            }
            return RDataFactory.createIntVector(result, complete);
        }
    }
}
