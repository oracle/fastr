/*
 * Copyright (c) 2014, 2014, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import java.io.*;

import com.oracle.truffle.api.CompilerDirectives.SlowPath;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.RContext.Engine.ParseException;
import com.oracle.truffle.r.runtime.data.*;

/**
 * Internal component of the {@code parse} base package function.
 *
 * <pre>
 * parse(file, n, text, prompt, srcfile, encoding)
 * </pre>
 */
@RBuiltin(name = "parse", kind = INTERNAL, parameterNames = {"conn", "n", "text", "prompt", "srcfile", "encoding"})
public abstract class Parse extends RBuiltinNode {

    @SuppressWarnings("unused")
    @Specialization
    protected Object parse(RConnection conn, RNull n, RNull text, String prompt, Object srcFile, String encoding) {
        controlVisibility();
        try {
            String[] lines = conn.readLines(0);
            return doParse(coalesce(lines));
        } catch (IOException | ParseException ex) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.PARSE_ERROR);
        }
    }

    @SuppressWarnings("unused")
    @Specialization
    protected Object parse(RConnection conn, double n, RNull text, String prompt, Object srcFile, String encoding) {
        controlVisibility();
        try {
            String[] lines = conn.readLines((int) n);
            return doParse(coalesce(lines));
        } catch (IOException | ParseException ex) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.PARSE_ERROR);
        }
    }

    @SuppressWarnings("unused")
    @Specialization
    protected Object parse(RConnection conn, RNull n, String text, String prompt, Object srcFile, String encoding) {
        controlVisibility();
        if (text.length() == 0) {
            return RDataFactory.createExpression(RDataFactory.createList());
        }
        try {
            return doParse(text);
        } catch (ParseException ex) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.PARSE_ERROR);
        }
    }

    @SlowPath
    private static RExpression doParse(String script) throws ParseException {
        return RContext.getEngine().parse(script);
    }

    @SlowPath
    private static String coalesce(String[] lines) {
        StringBuffer sb = new StringBuffer();
        for (String line : lines) {
            sb.append(line);
            sb.append('\n');
        }
        return sb.toString();
    }

}
