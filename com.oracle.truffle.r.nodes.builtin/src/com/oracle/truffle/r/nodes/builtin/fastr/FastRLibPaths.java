/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.fastr;

import static com.oracle.truffle.r.runtime.RVisibility.ON;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.builtins.RBehavior;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;

/**
 * Allows to show the actual location of the source section of a provided function.
 */
@RBuiltin(name = ".fastr.libPaths", visibility = ON, kind = PRIMITIVE, parameterNames = "new", behavior = RBehavior.MODIFIES_STATE)
public abstract class FastRLibPaths extends RBuiltinNode.Arg1 {

    static {
        Casts casts = new Casts(FastRLibPaths.class);
        casts.arg("new").allowMissing().mustBe(Predef.stringValue()).asStringVector();
    }

    @Specialization
    public Object setLibPaths(RAbstractStringVector paths) {
        RContext.getInstance().libraryPaths.clear();
        for (int i = 0; i < paths.getLength(); i++) {
            RContext.getInstance().libraryPaths.add(paths.getDataAt(i));
        }
        return paths;
    }

    @Specialization
    public Object srcInfo(@SuppressWarnings("unused") RMissing path) {
        String[] libPaths = RContext.getInstance().libraryPaths.toArray(new String[0]);
        return RDataFactory.createStringVector(libPaths, true);
    }
}
