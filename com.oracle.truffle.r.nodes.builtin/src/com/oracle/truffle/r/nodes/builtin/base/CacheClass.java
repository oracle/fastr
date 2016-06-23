/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995, 1996, 1997  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1995-2014, The R Core Team
 * Copyright (c) 2002-2008, The R Foundation
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.runtime.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;

@RBuiltin(name = ".cache_class", kind = PRIMITIVE, parameterNames = {"class", "extends"})
public abstract class CacheClass extends RBuiltinNode {

    @Override
    protected void createCasts(CastBuilder casts) {
        casts.arg("class").defaultError(RError.Message.GENERIC, "invalid class argument to internal .class_cache").mustBe(stringValue()).asStringVector().findFirst();
    }

    @TruffleBoundary
    @Specialization
    protected RAbstractStringVector getClass(String cl, RAbstractStringVector ext) {
        RContext.getInstance().putS4Extends(cl, ext.materialize());
        return null;
    }
}
