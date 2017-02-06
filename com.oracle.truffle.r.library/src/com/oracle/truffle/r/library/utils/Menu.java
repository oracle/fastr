/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.utils;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.context.ConsoleHandler;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;

// Translated from GnuR: library/utils/io.c

public abstract class Menu extends RExternalBuiltinNode.Arg1 {

    static {
        Casts casts = new Casts(Menu.class);
        casts.arg(0, "choices").mustBe(Predef.stringValue()).asStringVector().mustBe(Predef.notEmpty());
    }

    @Specialization
    @TruffleBoundary
    protected int menu(RAbstractStringVector choices) {
        ConsoleHandler ch = RContext.getInstance().getConsoleHandler();
        int first = choices.getLength() + 1;
        ch.print("Selection: ");
        String response = ch.readLine().trim();
        if (response.length() > 0) {
            if (Character.isDigit(response.charAt(0))) {
                try {
                    first = Integer.parseInt(response);
                } catch (NumberFormatException ex) {
                    //
                }
            } else {
                for (int i = 0; i < choices.getLength(); i++) {
                    String entry = choices.getDataAt(i);
                    if (entry.equals(response)) {
                        first = i + 1;
                        break;
                    }
                }
            }
        }
        return first;
    }

    @Fallback
    @TruffleBoundary
    protected int menu(@SuppressWarnings("unused") Object choices) {
        throw RError.error(this, RError.Message.INVALID_ARGUMENT, "choices");
    }
}
