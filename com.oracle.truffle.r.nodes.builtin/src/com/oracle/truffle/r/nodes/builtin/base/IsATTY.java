/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.conn.RConnection;
import com.oracle.truffle.r.runtime.conn.StdConnections.StdConnection;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;

@RBuiltin(name = "isatty", kind = INTERNAL, parameterNames = {"con"}, behavior = PURE)
public abstract class IsATTY extends RBuiltinNode {

    @Specialization
    @TruffleBoundary
    protected byte isATTYNonConnection(RAbstractIntVector con) {
        if (con.getLength() == 1) {
            RStringVector clazz = con.getClassHierarchy();
            for (int i = 0; i < clazz.getLength(); i++) {
                if ("connection".equals(clazz.getDataAt(i))) {
                    RConnection connection = RContext.getInstance().stateRConnection.getConnection(con.getDataAt(0), false);
                    if (connection != null) {
                        return RRuntime.asLogical(connection instanceof StdConnection);
                    } else {
                        return RRuntime.LOGICAL_FALSE;
                    }
                }
            }
        }
        return RRuntime.LOGICAL_FALSE;
    }

    @Fallback
    @TruffleBoundary
    protected byte isATTYNonConnection(@SuppressWarnings("unused") Object con) {
        return RRuntime.LOGICAL_FALSE;
    }
}
