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

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin(name = "cat", kind = SUBSTITUTE, parameterNames = {"...", "file", "sep", "fill", "labels", "append"})
// TODO Should be INTERNAL
@SuppressWarnings("unused")
public abstract class Cat extends RInvisibleBuiltinNode {
    @Override
    public RNode[] getParameterValues() {
        return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(""), ConstantNode.create(" "), ConstantNode.create(RRuntime.LOGICAL_FALSE), ConstantNode.create(RNull.instance),
                        ConstantNode.create(RRuntime.LOGICAL_FALSE)};
    }

    @Child private ToStringNode toString;

    @CompilationFinal private String currentSep;

    private void ensureToString(String sep) {
        if (toString == null || !sep.equals(currentSep)) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            toString = insert(ToStringNodeFactory.create(null, false, sep, false));
            currentSep = sep;
        }
    }

    @Specialization
    protected Object cat(RNull arg, String file, String sep, byte fill, Object labels, byte append) {
        controlVisibility();
        return RNull.instance;
    }

    @Specialization
    protected RNull cat(RMissing arg, String file, String sep, byte fill, Object labels, byte append) {
        controlVisibility();
        return RNull.instance;
    }

    @Specialization
    protected RNull cat(VirtualFrame frame, RAbstractVector arg, String file, String sep, byte fill, Object labels, byte append) {
        ensureToString(sep);
        catIntl(toString.executeString(frame, arg));
        controlVisibility();
        return RNull.instance;
    }

    @Specialization(guards = "!isNull")
    protected RNull cat(VirtualFrame frame, RArgsValuesAndNames args, String file, String sep, byte fill, Object labels, byte append) {
        ensureToString(sep);
        Object[] argValues = args.getValues();
        for (int i = 0; i < argValues.length; ++i) {
            catIntl(toString.executeString(frame, argValues[i]));
            catSep(sep, argValues, i);
        }
        controlVisibility();
        return RNull.instance;
    }

    @Specialization(guards = "isNull")
    protected RNull catNull(VirtualFrame frame, RArgsValuesAndNames args, String file, String sep, byte fill, Object labels, byte append) {
        controlVisibility();
        return RNull.instance;
    }

    protected boolean isNull(RArgsValuesAndNames args) {
        return args.length() == 1 && args.getValues()[0] == RNull.instance;
    }

    private static void catSep(String sep, Object[] os, int j) {
        if (j < os.length - 1 || sep.indexOf('\n') != -1) {
            catIntl(sep);
        }
    }

    @TruffleBoundary
    private static void catIntl(String s) {
        RContext.getInstance().getConsoleHandler().print(s);
    }
}
