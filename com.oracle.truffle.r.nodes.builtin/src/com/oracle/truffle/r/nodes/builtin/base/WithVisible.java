/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.runtime.builtins.RBehavior.COMPLEX;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RStringVector;

@RBuiltin(name = "withVisible", kind = PRIMITIVE, parameterNames = "x", behavior = COMPLEX)
public abstract class WithVisible extends RBuiltinNode {
    private static final RStringVector LISTNAMES = RDataFactory.createStringVector(new String[]{"value", "visible"}, RDataFactory.COMPLETE_VECTOR);

    @Specialization(guards = "!isRMissing(x)")
    protected RList withVisible(Object x) {
        // (LS) temporarily disabled to enable parallel benchmarks
        // if (FastROptions.IgnoreVisibility.getBooleanValue()) {
        // RError.warning(this, RError.Message.GENERIC, "using withVisible with IgnoreVisibility");
        // }

        Object[] data = new Object[]{x, RRuntime.asLogical(RContext.getInstance().isVisible())};
        // Visibility is changed by the evaluation (else this code would not work),
        // so we have to force it back on.
        return RDataFactory.createList(data, LISTNAMES);
    }

    @Specialization
    protected RList withVisible(@SuppressWarnings("unused") RMissing x) {
        throw RError.error(this, Message.ARGUMENT_MISSING, "x");
    }
}
