/*
 * Copyright (c) 1995, 1996, 1997  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1995-2014, The R Core Team
 * Copyright (c) 2002-2008, The R Foundation
 * Copyright (c) 2013, 2022, Oracle and/or its affiliates
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, a copy is available at
 * https://www.R-project.org/Licenses/
 */
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.runtime.RVisibility.OFF;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.COMPLEX;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RCleanUp;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.gnur.SA_TYPE;

@RBuiltin(name = "quit", visibility = OFF, kind = INTERNAL, parameterNames = {"save", "status", "runLast"}, behavior = COMPLEX)
public abstract class Quit extends RBuiltinNode.Arg3 {

    static {
        Casts casts = new Casts(Quit.class);
        casts.arg("save").mustBe(stringValue(), RError.Message.QUIT_ASK).asStringVector().findFirst();
        casts.arg("status").asIntegerVector().findFirst();
        casts.arg("runLast").asLogicalVector().findFirst();
    }

    private SA_TYPE checkSaveValue(String save) throws RError {
        for (SA_TYPE saveValue : SA_TYPE.values()) {
            if (saveValue.getName().equals(save)) {
                return saveValue;
            }
        }
        throw error(RError.Message.QUIT_SAVE);
    }

    @Specialization
    @TruffleBoundary
    protected Object doQuit(String save, final int status, final byte runLastIn) {
        byte runLast = runLastIn;
        if (getRContext().stateInstrumentation.getBrowserState().inBrowser()) {
            warning(RError.Message.BROWSER_QUIT);
            return RNull.instance;
        }
        SA_TYPE ask = checkSaveValue(save);
        if (ask == SA_TYPE.SAVEASK && !getRContext().isInteractive()) {
            warning(RError.Message.QUIT_ASK_INTERACTIVE);
        }
        if (status == RRuntime.INT_NA) {
            warning(RError.Message.QUIT_INVALID_STATUS);
            runLast = RRuntime.LOGICAL_FALSE;
        }
        if (runLast == RRuntime.LOGICAL_NA) {
            warning(RError.Message.QUIT_INVALID_RUNLAST);
            runLast = RRuntime.LOGICAL_FALSE;
        }
        RCleanUp.cleanUp(getRContext(), ask, status, RRuntime.fromLogical(runLast));
        throw RInternalError.shouldNotReachHere("cleanup returned");
    }
}
