/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.RBuiltinKind;
import com.oracle.truffle.r.runtime.RDeparse;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;

// Part of this transcribed from GnuR src/main/deparse.c

@RBuiltin(name = "deparse", kind = RBuiltinKind.INTERNAL, parameterNames = {"expr", "width.cutoff", "backtick", "control", "nlines"})
public abstract class Deparse extends RBuiltinNode {

    @Override
    protected void createCasts(CastBuilder casts) {
        casts.firstIntegerWithError(1, null, null);
        casts.toLogical(2);
        casts.toInteger(3);
        casts.toInteger(4);
    }

    @Specialization
    @TruffleBoundary
    protected RStringVector deparse(Object expr, int widthCutoffArg, RAbstractLogicalVector backtick, int control, int nlines) {
        int widthCutoff = widthCutoffArg;
        if (widthCutoff == RRuntime.INT_NA || widthCutoff < RDeparse.MIN_Cutoff || widthCutoff > RDeparse.MAX_Cutoff) {
            RError.warning(this, RError.Message.DEPARSE_INVALID_CUTOFF);
            widthCutoff = RDeparse.DEFAULT_Cutoff;
        }

        String[] data = RDeparse.deparse(expr, widthCutoff, RRuntime.fromLogical(backtick.getDataAt(0)), control, nlines).split("\n");
        return RDataFactory.createStringVector(data, RDataFactory.COMPLETE_VECTOR);
    }
}
