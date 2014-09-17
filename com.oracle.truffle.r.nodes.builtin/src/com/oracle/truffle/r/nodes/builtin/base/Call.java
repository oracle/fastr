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

import com.oracle.truffle.api.CompilerDirectives.SlowPath;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

/**
 * Construct a call object from a name and optional arguments.
 */
@RBuiltin(name = "call", kind = PRIMITIVE, parameterNames = {"name", "..."})
public abstract class Call extends RBuiltinNode {

    @Override
    public RNode[] getParameterValues() {
        return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance)};
    }

    @Specialization
    protected RLanguage call(String name, @SuppressWarnings("unused") RMissing args) {
        return makeCall(name, null);
    }

    @Specialization
    protected RLanguage call(String name, RArgsValuesAndNames args) {
        return makeCall(name, args);
    }

    @Specialization
    @SuppressWarnings("unused")
    protected RLanguage call(Object name, Object args) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.FIRST_ARG_MUST_BE_STRING);
    }

    @SlowPath
    protected static RLanguage makeCall(String name, RArgsValuesAndNames args) {
        return makeCall0(new RSymbol(name), args);
    }

    @SlowPath
    protected static RLanguage makeCall(RFunction function, RArgsValuesAndNames args) {
        return makeCall0(function, args);
    }

    @SlowPath
    private static RLanguage makeCall0(Object fn, RArgsValuesAndNames args) {
        int dataLen = args == null ? 1 : args.length() + 1;
        Object[] data = new Object[dataLen];
        String[] names = null;
        data[0] = fn;
        if (dataLen > 1) {
            Object[] argValues = args.getValues();
            String[] argNames = args.getNames();
            if (argNames != null) {
                names = new String[dataLen];
            }
            for (int i = 1; i < dataLen; i++) {
                data[i] = argValues[i - 1];
                if (argNames != null) {
                    names[i] = argNames[i - 1] == null ? RRuntime.NAMES_ATTR_EMPTY_VALUE : argNames[i - 1];
                }
            }
        }
        return RDataFactory.createLanguage(RDataFactory.createList(data, names == null ? null : RDataFactory.createStringVector(names, RDataFactory.COMPLETE_VECTOR)), RLanguage.Type.FUNCALL);
    }

}
