/*
 * Copyright (c) 1995, 1996, 1997  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1995-2014, The R Core Team
 * Copyright (c) 2002-2008, The R Foundation
 * Copyright (c) 2015, 2022, Oracle and/or its affiliates
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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.COMPLEX;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RStringVector;

@RBuiltin(name = ".cache_class", kind = PRIMITIVE, parameterNames = {"class", "extends"}, behavior = COMPLEX)
public abstract class CacheClass extends RBuiltinNode.Arg2 {

    static {
        Casts casts = new Casts(CacheClass.class);
        casts.arg("class").defaultError(RError.Message.GENERIC, "invalid class argument to internal .class_cache").mustBe(stringValue()).asStringVector().findFirst();
        // apparently, "extends" does not have to be a string vector (GNU R will not signal this
        // error) - but it does not seem to make much sense and it's doubtful if it's worth
        // supporting since this is internal function
        casts.arg("extends").defaultError(RError.Message.GENERIC, "invalid extends argument to internal .class_cache").allowNull().mustBe(stringValue()).asStringVector();

    }

    @TruffleBoundary
    @Specialization
    protected RStringVector cacheClass(String cl, RStringVector ext) {
        getRContext().putS4Extends(cl, ext.materialize());
        return ext;
    }

    @TruffleBoundary
    @Specialization
    protected RNull uncacheClass(String klass, RNull rNull) {
        getRContext().removeS4Extends(klass);
        return rNull;
    }
}
