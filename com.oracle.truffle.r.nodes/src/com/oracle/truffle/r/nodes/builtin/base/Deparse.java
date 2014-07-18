/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2014, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import java.util.*;

import com.oracle.truffle.api.CompilerDirectives.SlowPath;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.gnur.*;

// Part of this transcribed from GnuR src/main/deparse.c

@SuppressWarnings("unused")
@RBuiltin(name = "deparse", kind = RBuiltinKind.INTERNAL)
public abstract class Deparse extends RBuiltinNode {
    private static final int KEEPINTEGER = 1;
    private static final int QUOTEEXPRESSIONS = 2;
    private static final int SHOWATTRIBUTES = 4;
    private static final int USESOURCE = 8;
    private static final int WARNINCOMPLETE = 16;
    private static final int DELAYPROMISES = 32;
    private static final int KEEPNA = 64;
    private static final int S_COMPAT = 128;
    /* common combinations of the above */
    private static final int SIMPLEDEPARSE = 0;
    private static final int DEFAULTDEPARSE = 65; /* KEEPINTEGER | KEEPNA, used for calls */

    private static final int MIN_Cutoff = 20;
    private static final int MAX_Cutoff = 500;
    private static final int DEFAULT_Cutoff = 60;

    @CreateCast("arguments")
    public RNode[] castArguments(RNode[] arguments) {
        arguments[2] = CastLogicalNodeFactory.create(arguments[2], false, false, false);
        arguments[4] = CastIntegerNodeFactory.create(arguments[4], false, false, false);
        return arguments;
    }

    @SlowPath
    @Specialization
    public RStringVector deparse(Object expr, int widthCutoffArg, RLogicalVector backtick, int nlines) {
        int widthCutoff = widthCutoffArg;
        if (widthCutoff == RRuntime.INT_NA || widthCutoff < MIN_Cutoff || widthCutoff > MAX_Cutoff) {
            RContext.getInstance().setEvalWarning("invalid 'cutoff' value for 'deparse', using default");
            widthCutoff = DEFAULT_Cutoff;
        }

        String[] data = new String[0];
        return RDataFactory.createStringVector(data, RDataFactory.COMPLETE_VECTOR);
    }

}
