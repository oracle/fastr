/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

// Part of this transcribed from GnuR src/main/deparse.c

@RBuiltin(name = "deparse", kind = RBuiltinKind.INTERNAL, parameterNames = {"expr", "width.cutoff", "backtick", "control", "nlines"})
public abstract class Deparse extends RBuiltinNode {

    @Override
    protected void createCasts(CastBuilder casts) {
        casts.toInteger(1);
        casts.toLogical(2);
        casts.toInteger(3);
        casts.toInteger(4);
    }

    @TruffleBoundary
    @Specialization
    protected RStringVector deparse(Object expr, int widthCutoffArg, RAbstractLogicalVector backtick, int control, int nlines) {
        controlVisibility();
        int widthCutoff = widthCutoffArg;
        if (widthCutoff == RRuntime.INT_NA || widthCutoff < RDeparse.MIN_Cutoff || widthCutoff > RDeparse.MAX_Cutoff) {
            RError.warning(this, RError.Message.DEPARSE_INVALID_CUTOFF);
            widthCutoff = RDeparse.DEFAULT_Cutoff;
        }

        String[] data = RDeparse.deparse(expr, widthCutoff, RRuntime.fromLogical(backtick.getDataAt(0)), control, nlines);
        return RDataFactory.createStringVector(data, RDataFactory.COMPLETE_VECTOR);
    }
}
