/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.util.HashMap;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.runtime.gnur.SEXPTYPE;

@GenerateUncached
public abstract class Str2TypeNode extends FFIUpCallNode.Arg1 {

    private static HashMap<String, Integer> name2typeTable;

    static {
        name2typeTable = new HashMap<>(26);
        name2typeTable.put("NULL", SEXPTYPE.NILSXP.code);
        /* real types */
        name2typeTable.put("symbol", SEXPTYPE.SYMSXP.code);
        name2typeTable.put("pairlist", SEXPTYPE.LISTSXP.code);
        name2typeTable.put("closure", SEXPTYPE.CLOSXP.code);
        name2typeTable.put("environment", SEXPTYPE.ENVSXP.code);
        name2typeTable.put("promise", SEXPTYPE.PROMSXP.code);
        name2typeTable.put("language", SEXPTYPE.LANGSXP.code);
        name2typeTable.put("special", SEXPTYPE.SPECIALSXP.code);
        name2typeTable.put("builtin", SEXPTYPE.BUILTINSXP.code);
        name2typeTable.put("char", SEXPTYPE.CHARSXP.code);
        name2typeTable.put("logical", SEXPTYPE.LGLSXP.code);
        name2typeTable.put("integer", SEXPTYPE.INTSXP.code);
        name2typeTable.put("double", SEXPTYPE.REALSXP.code);
        /*-  "real", for R <= 0.61.x */
        name2typeTable.put("complex", SEXPTYPE.CPLXSXP.code);
        name2typeTable.put("character", SEXPTYPE.STRSXP.code);
        name2typeTable.put("...", SEXPTYPE.DOTSXP.code);
        name2typeTable.put("any", SEXPTYPE.ANYSXP.code);
        name2typeTable.put("expression", SEXPTYPE.EXPRSXP.code);
        name2typeTable.put("list", SEXPTYPE.VECSXP.code);
        name2typeTable.put("externalptr", SEXPTYPE.EXTPTRSXP.code);
        name2typeTable.put("bytecode", SEXPTYPE.BCODESXP.code);
        name2typeTable.put("weakref", SEXPTYPE.WEAKREFSXP.code);
        name2typeTable.put("raw", SEXPTYPE.RAWSXP.code);
        name2typeTable.put("S4", SEXPTYPE.S4SXP.code);
        name2typeTable.put("numeric", SEXPTYPE.REALSXP.code);
        name2typeTable.put("name", SEXPTYPE.SYMSXP.code);
    }

    public static Str2TypeNode create() {
        return Str2TypeNodeGen.create();
    }

    public static Str2TypeNode getUncached() {
        return Str2TypeNodeGen.getUncached();
    }

    @Specialization
    Object handleString(Object n, @Cached("create()") NativeStringCastNode sc) {
        String name = sc.executeObject(n);
        if (name == null) {
            return -1;
        }
        Integer result = name2typeTable.get(name);
        return result != null ? result : -1;
    }

}
