/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import static com.oracle.truffle.r.runtime.RCompression.Type.GZIP;
import com.oracle.truffle.r.runtime.RError;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.BaseRConnection;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.ConnectionClass;
import static com.oracle.truffle.r.runtime.conn.ConnectionSupport.ConnectionClass.GZCon;
import com.oracle.truffle.r.runtime.conn.RConnection;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import java.io.IOException;

@RBuiltin(name = "gzcon", kind = INTERNAL, parameterNames = {"con", "level", "allowNonCompressed", "text"}, behavior = PURE)
public abstract class GZCon extends RBuiltinNode.Arg4 {

    static {
        Casts casts = new Casts(GZCon.class);
        casts.arg("con").defaultError(RError.Message.NOT_CONNECTION, "con").mustNotBeNull().asIntegerVector().findFirst();
        casts.arg("level").asIntegerVector().findFirst();
        casts.arg("allowNonCompressed").asLogicalVector().findFirst().map(toBoolean());
        casts.arg("text").asLogicalVector().findFirst().map(toBoolean());
    }

    @Specialization
    public RAbstractIntVector gzcon(int conIndex, @SuppressWarnings("unused") int level, @SuppressWarnings("unused") boolean allowNonCompressed, @SuppressWarnings("unused") boolean text,
                    @Cached("createBinaryProfile()") ConditionProfile gzConProfile) {
        BaseRConnection base = RConnection.fromIndex(conIndex);
        if (gzConProfile.profile(base.getConnectionClass() == ConnectionClass.GZCon)) {
            RError.warning(this, RError.Message.IS_GZCON);
            return base.asVector();
        }
        try {
            base.setCompressiontype(GZIP);
            base.updateConnectionClass(GZCon);
        } catch (IOException ex) {
            throw error(RError.Message.GENERIC, ex.getMessage());
        }
        return base.asVector();
    }

}
