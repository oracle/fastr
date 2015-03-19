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
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.gnur.*;

// Part of this transcribed from GnuR src/main/deparse.c

@RBuiltin(name = "deparse", kind = RBuiltinKind.INTERNAL, parameterNames = {"expr", "width.cutoff", "backtick", "control", "nlines"})
public abstract class Deparse extends RBuiltinNode {

    @CreateCast("arguments")
    public RNode[] castArguments(RNode[] arguments) {
        arguments[1] = CastIntegerNodeGen.create(arguments[1], false, false, false);
        arguments[2] = CastLogicalNodeGen.create(arguments[2], false, false, false);
        arguments[3] = CastIntegerNodeGen.create(arguments[3], false, false, false);
        arguments[4] = CastIntegerNodeGen.create(arguments[4], false, false, false);
        return arguments;
    }

    @TruffleBoundary
    @Specialization(guards = "!isSource(expr)")
    protected RStringVector deparse(Object expr, int widthCutoffArg, RLogicalVector backtick, int control, int nlines) {
        controlVisibility();
        int widthCutoff = widthCutoffArg;
        if (widthCutoff == RRuntime.INT_NA || widthCutoff < RDeparse.MIN_Cutoff || widthCutoff > RDeparse.MAX_Cutoff) {
            RError.warning(getEncapsulatingSourceSection(), RError.Message.DEPARSE_INVALID_CUTOFF);
            widthCutoff = RDeparse.DEFAULT_Cutoff;
        }

        String[] data = RDeparse.deparse(expr, widthCutoff, RRuntime.fromLogical(backtick.getDataAt(0)), control, nlines);
        return RDataFactory.createStringVector(data, RDataFactory.COMPLETE_VECTOR);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "isSource(expr)")
    protected RStringVector deparse(RPairList expr, int widthCutoffArg, RLogicalVector backtick, int control, int nlines) {
        return RDataFactory.createStringVectorFromScalar(((SourceSection) expr.getTag()).getCode());
    }

    public static boolean isSource(Object expr) {
        return (expr instanceof RPairList) && ((RPairList) expr).getType() == SEXPTYPE.FASTR_SOURCESECTION;
    }

}
