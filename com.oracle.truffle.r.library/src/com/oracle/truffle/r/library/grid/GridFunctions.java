/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (C) 2001-3 Paul Murrell
 * Copyright (c) 1998-2013, The R Core Team
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.grid;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.notEmpty;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.ffi.RFFIFactory;

/**
 * The .Call support for the grid package.
 */
public class GridFunctions {
    public abstract static class InitGrid extends RExternalBuiltinNode.Arg1 {
        @Specialization
        @TruffleBoundary
        protected Object initGrid(REnvironment gridEvalEnv) {
            return RFFIFactory.getRFFI().getGridRFFI().initGrid(gridEvalEnv);
        }
    }

    public static final class KillGrid extends RExternalBuiltinNode {
        @Override
        @TruffleBoundary
        public Object call(RArgsValuesAndNames args) {
            return RFFIFactory.getRFFI().getGridRFFI().killGrid();
        }
    }

    public abstract static class ValidUnits extends RExternalBuiltinNode.Arg1 {
        @Override
        protected void createCasts(CastBuilder casts) {
            casts.arg(0).mustBe(stringValue(), RError.Message.GENERIC, "'units' must be character").asStringVector().mustBe(notEmpty(), RError.Message.GENERIC, "'units' must be of length > 0");
        }

        @Specialization
        protected RIntVector validUnits(RAbstractStringVector units) {
            int[] data = new int[units.getLength()];
            for (int i = 0; i < data.length; i++) {
                int code = convertUnit(units.getDataAt(i));
                if (code < 0) {
                    throw RError.error(this, RError.Message.GENERIC, "Invalid unit");
                }
                data[i] = code;
            }
            return RDataFactory.createIntVector(data, RDataFactory.COMPLETE_VECTOR);
        }

        private enum UnitTab {
            npc(0);

            private final int code;

            UnitTab(int code) {
                this.code = code;
            }
        }

        private static int convertUnit(String unit) {
            for (UnitTab unitTab : UnitTab.values()) {
                if (unit.equals(unitTab.name())) {
                    return unitTab.code;
                }
            }
            return -1;
        }
    }
}
