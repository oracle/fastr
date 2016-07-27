/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995, 1996, 1997  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1995-2014, The R Core Team
 * Copyright (c) 2002-2008, The R Foundation
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RCleanUp;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RStartParams;
import com.oracle.truffle.r.runtime.RStartParams.SA_TYPE;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.RVisibility;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;

@RBuiltin(name = "quit", visibility = RVisibility.OFF, kind = INTERNAL, parameterNames = {"save", "status", "runLast"})
public abstract class Quit extends RBuiltinNode {

    @Override
    protected void createCasts(CastBuilder casts) {
        casts.toInteger(1);
    }

    private SA_TYPE checkSaveValue(String save) throws RError {
        for (String saveValue : SA_TYPE.SAVE_VALUES) {
            if (saveValue.equals(save)) {
                return SA_TYPE.fromString(save);
            }
        }
        throw RError.error(this, RError.Message.QUIT_SAVE);
    }

    @Specialization
    @TruffleBoundary
    protected Object doQuit(RAbstractStringVector saveArg, final int status, final byte runLastIn) {
        if (RContext.getInstance().stateInstrumentation.getBrowserState().inBrowser()) {
            RError.warning(this, RError.Message.BROWSER_QUIT);
            return RNull.instance;
        }
        String save = saveArg.getDataAt(0);
        RStartParams.SA_TYPE ask = checkSaveValue(save);
        if (ask == SA_TYPE.SAVEASK && !RContext.getInstance().getConsoleHandler().isInteractive()) {
            RError.warning(this, RError.Message.QUIT_ASK_INTERACTIVE);
        }
        if (status == RRuntime.INT_NA) {
            RError.warning(this, RError.Message.QUIT_INVALID_STATUS);
        }
        byte runLast = runLastIn;
        if (runLast == RRuntime.LOGICAL_NA) {
            RError.warning(this, RError.Message.QUIT_INVALID_RUNLAST);
            runLast = RRuntime.LOGICAL_FALSE;
        }
        RCleanUp.cleanUp(ask, status, RRuntime.fromLogical(runLast));
        throw RInternalError.shouldNotReachHere("cleanup returned");
    }

    @SuppressWarnings("unused")
    @Fallback
    protected Object doQuit(Object saveArg, Object status, Object runLast) {
        if (RRuntime.asString(saveArg) == null) {
            throw RError.error(this, RError.Message.QUIT_ASK);
        }
        throw RError.error(this, RError.Message.INVALID_OR_UNIMPLEMENTED_ARGUMENTS);
    }

}
