/*
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates
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
package com.oracle.truffle.r.library.utils;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.RContext.ConsoleIO;
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
        ConsoleIO console = RContext.getInstance().getConsole();
        int first = choices.getLength() + 1;
        console.print("Selection: ");
        String line = console.readLine();
        assert line != null;
        String response = line.trim();
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
        throw error(RError.Message.INVALID_ARGUMENT, "choices");
    }
}
