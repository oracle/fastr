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
package com.oracle.truffle.r.ffi.impl.nodes;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RTypes;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;

public final class MatchNodes {

    @TypeSystemReference(RTypes.class)
    public abstract static class MatchNode extends FFIUpCallNode.Arg3 {

        @SuppressWarnings("unused")
        @Specialization
        Object match(Object itables, Object ix, int nmatch) {
            throw RInternalError.unimplemented("Rf_match");
        }

        public static MatchNode create() {
            return MatchNodesFactory.MatchNodeGen.create();
        }
    }

    @TypeSystemReference(RTypes.class)
    public abstract static class NonNullStringMatchNode extends FFIUpCallNode.Arg2 {

        @Specialization(guards = {"s.getLength() == 1", "t.getLength() == 1"})
        Object matchSingle(RAbstractStringVector s, RAbstractStringVector t) {
            if (s.getDataAt(0) == RRuntime.STRING_NA || t.getDataAt(0) == RRuntime.STRING_NA) {
                return RRuntime.LOGICAL_FALSE;
            }
            return RRuntime.asLogical(s.getDataAt(0).equals(t.getDataAt(0)));
        }

        @Fallback
        @SuppressWarnings("unused")
        Object match(Object s, Object t) {
            throw RInternalError.unimplemented("Rf_NonNullStringMatch");
        }

        public static NonNullStringMatchNode create() {
            return MatchNodesFactory.NonNullStringMatchNodeGen.create();
        }
    }

}
