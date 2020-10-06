/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.logicalFalse;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.logicalValue;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.Match5Node;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;

/*
 * TODO: handle "incomparables" parameter.
 */
@RBuiltin(name = "match", kind = INTERNAL, parameterNames = {"x", "table", "nomatch", "incomparables"}, behavior = PURE)
public abstract class Match extends RBuiltinNode.Arg4 {

    protected abstract Object executeRIntVector(Object x, Object table, Object noMatch, Object incomparables);

    static {
        Casts casts = new Casts(Match.class);
        casts.arg("nomatch").asIntegerVector().findFirst();
        casts.arg("incomparables").defaultError(Message.GENERIC, "usage of 'incomparables' in match not implemented").allowNull().mustBe(logicalValue()).asLogicalVector().findFirst().mustBe(
                        logicalFalse());
    }

    @Specialization
    Object doIt(Object x, Object table, int nomatch, Object incomparables,
                    @Cached Match5Node match5Node) {
        return match5Node.execute(x, table, nomatch, incomparables);
    }
}
