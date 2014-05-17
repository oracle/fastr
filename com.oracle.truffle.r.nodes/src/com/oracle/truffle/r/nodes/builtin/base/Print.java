/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.RTypesGen.*;

import java.text.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.SlowPath;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin("print")
@SuppressWarnings("unused")
public abstract class Print extends RInvisibleBuiltinNode {

    public static final int R_MAX_DIGITS_OPT = 22;
    public static final int R_MIN_DIGITS_OPT = 0;

    private static Config RPrint;

    public static Config setPrintDefaults() {
        if (RPrint == null) {
            RPrint = new Config();
        }
        RPrint.width = RContext.getInstance().getConsoleHandler().getWidth();
        RPrint.naWidth = RRuntime.STRING_NA.length();
        RPrint.naWidthNoQuote = RRuntime.NA_HEADER.length();
        RPrint.digits = 7 /* default */;
        RPrint.scipen = 0 /* default */;
        RPrint.gap = 1;
        RPrint.quote = 1;
        RPrint.right = Adjustment.LEFT;
        RPrint.max = 99999 /* default */;
        RPrint.naString = RRuntime.STRING_NA;
        RPrint.naStringNoQuote = RRuntime.NA_HEADER;
        RPrint.useSource = 8 /* default */;
        RPrint.cutoff = 60;
        return RPrint;
    }

    public static Config getRPrint() {
        return setPrintDefaults();
    }

    @Child protected PrettyPrinterNode prettyPrinter = PrettyPrinterNodeFactory.create(null, null, false);

    private static void printHelper(String string) {
        RContext.getInstance().getConsoleHandler().println(string);
    }

    @Specialization
    public Object print(VirtualFrame frame, Object o) {
        String s = (String) prettyPrinter.executeString(frame, o, null);
        if (s != null && !s.isEmpty()) {
            printHelper(s);
        }
        controlVisibility();
        return o;
    }

    public static class Config {
        public int width;
        public int naWidth;
        public int naWidthNoQuote;
        public int digits;
        public int scipen;
        public int gap;
        public int quote;
        public Adjustment right;
        public int max;
        public String naString;
        public String naStringNoQuote;
        public int useSource;
        public int cutoff;
    }

    public enum Adjustment {
        LEFT, RIGHT, CENTRE, NONE;
    }

}
