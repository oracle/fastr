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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

// Part of this transcribed from GnuR src/main/deparse.c

@RBuiltin(name = "deparse", kind = RBuiltinKind.INTERNAL, parameterNames = {"expr", "width.cutoff", "backtick", "control", "nlines"})
public abstract class Deparse extends RBuiltinNode {

    @Override
    public RNode[] getParameterValues() {
        // expr, width.cutoff = 60L, backtick = mode(expr) %in% c("call", "expression", "(",
        // "function"), control = c("keepInteger", "showAttributes", "keepNA"), nlines = -1L
        return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(60), ConstantNode.create(RDataFactory.createLogicalVector(new byte[]{RRuntime.LOGICAL_TRUE}, true)),
                        ConstantNode.create(RDataFactory.createStringVector(new String[]{"keepInteger", "showAttributes", "keepNA"}, true)), ConstantNode.create(-1)};
    }

    @CreateCast("arguments")
    public RNode[] castArguments(RNode[] arguments) {
        arguments[2] = CastLogicalNodeFactory.create(arguments[2], false, false, false);
        // TODO Introduce default value for backtick if argument[3] == RMissing!
        arguments[3] = CastIntegerNodeFactory.create(arguments[3], false, false, false);
        return arguments;
    }

    @TruffleBoundary
    @Specialization
    protected RStringVector deparse(Object expr, int widthCutoffArg, RLogicalVector backtick, int nlines) {
        controlVisibility();
        int widthCutoff = widthCutoffArg;
        if (widthCutoff == RRuntime.INT_NA || widthCutoff < RDeparse.MIN_Cutoff || widthCutoff > RDeparse.MAX_Cutoff) {
            RContext.getInstance().setEvalWarning("invalid 'cutoff' value for 'deparse', using default");
            widthCutoff = RDeparse.DEFAULT_Cutoff;
        }

        String[] data = RDeparse.deparse(expr, widthCutoff, RRuntime.fromLogical(backtick.getDataAt(0)), nlines);
        return RDataFactory.createStringVector(data, RDataFactory.COMPLETE_VECTOR);
    }

}
