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

import com.oracle.truffle.api.CompilerDirectives.SlowPath;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.builtin.RBuiltin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin(value = "sprintf", lastParameterKind = LastParameterKind.VAR_ARGS_SPECIALIZE)
public abstract class Sprintf extends RBuiltinNode {

    private static final Object[] PARAMETER_NAMES = new Object[]{"fmt", "..."};

    @Override
    public Object[] getParameterNames() {
        return PARAMETER_NAMES;
    }

    @Override
    public RNode[] getParameterValues() {
        return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance)};
    }

    @Specialization
    public String sprintf(String fmt, int x) {
        return format(fmt, x);
    }

    @Specialization
    public RStringVector sprintf(String fmt, RAbstractIntVector x) {
        String[] r = new String[x.getLength()];
        for (int k = 0; k < r.length; ++k) {
            r[k] = format(fmt, x.getDataAt(k));
        }
        return RDataFactory.createStringVector(r, RDataFactory.COMPLETE_VECTOR);
    }

    @Specialization
    public String sprintf(String fmt, double x) {
        char f = Character.toLowerCase(firstFormatChar(fmt));
        if (f == 'x' || f == 'd') {
            if (Math.floor(x) == x) {
                return format(fmt, (long) x);
            }
            throw RError.getInvalidFormatDouble(null, fmt);
        }
        return format(fmt, x);
    }

    @Specialization
    public RStringVector sprintf(String fmt, RAbstractDoubleVector x) {
        String[] r = new String[x.getLength()];
        for (int k = 0; k < r.length; ++k) {
            r[k] = sprintf(fmt, x.getDataAt(k));
        }
        return RDataFactory.createStringVector(r, RDataFactory.COMPLETE_VECTOR);
    }

    @Specialization
    public String sprintf(String fmt, Object[] args) {
        return format(fmt, args);
    }

    @SlowPath
    protected String format(String fmt, Object... args) {
        return String.format(null, fmt, args);
    }

    private static char firstFormatChar(String fmt) {
        int pos = 0;
        char f;
        for (f = '\0'; f == '\0'; f = fmt.charAt(pos + 1)) {
            pos = fmt.indexOf('%', pos);
            if (pos == -1 || pos >= fmt.length() - 1) {
                return '\0';
            }
            if (fmt.charAt(pos + 1) == '%') {
                continue;
            }
            while (!Character.isLetter(fmt.charAt(pos + 1)) && pos < fmt.length() - 1) {
                ++pos;
            }
        }
        return f;
    }

}
