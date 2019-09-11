/*
 * Copyright (c) 2019, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.ffi.impl.nodes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.data.CharSXPWrapper;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.gnur.SEXPTYPE;

@ReportPolymorphism
@GenerateUncached
public abstract class TYPEOFNode extends FFIUpCallNode.Arg1 {
    public abstract Object execute(Object value);

    public static TYPEOFNode create() {
        return TYPEOFNodeGen.create();
    }

    protected static SEXPTYPE getTypeForClass(Object value) {
        if (value instanceof CharSXPWrapper) {
            return SEXPTYPE.CHARSXP;
        } else {
            return SEXPTYPE.typeForClass(value);
        }
    }

    protected static boolean isNotSpecial(Class<?> clazz) {
        return !clazz.isAssignableFrom(RPairList.class) && !clazz.isAssignableFrom(RFunction.class);
    }

    @Specialization
    protected static int doPairlist(RPairList pl) {
        return pl.isLanguage() ? SEXPTYPE.LANGSXP.code : SEXPTYPE.LISTSXP.code;
    }

    @Specialization
    protected static int doFun(RFunction fun) {
        return fun.isBuiltin() ? SEXPTYPE.BUILTINSXP.code : SEXPTYPE.CLOSXP.code;
    }

    @Specialization(guards = {"clazz == value.getClass()", "isNotSpecial(clazz)"}, limit = "getCacheSize(16)")
    protected static int getTypeCached(@SuppressWarnings("unused") Object value,
                    @Cached("getTypeForClass(value)") SEXPTYPE typeForClass,
                    @SuppressWarnings("unused") @Cached("value.getClass()") Class<?> clazz) {
        return typeForClass.code;
    }

    @Specialization(replaces = "getTypeCached", guards = "isNotSpecial(value.getClass())")
    protected static int getType(Object value) {
        return SEXPTYPE.gnuRType(getTypeForClass(value), value).code;
    }
}
