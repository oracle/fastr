/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.r.ffi.impl.nodes.MatchNodesFactory.NonNullStringMatchNodeGen;
import com.oracle.truffle.r.nodes.builtin.Match5Node;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.CharSXPWrapper;
import com.oracle.truffle.r.runtime.data.RTypes;
import com.oracle.truffle.r.runtime.data.RStringVector;

public final class MatchNodes {

    @TypeSystemReference(RTypes.class)
    public abstract static class Match5UpCallNode extends FFIUpCallNode.Arg5 {
        @Specialization
        Object match(Object x, Object table, int noMatch, Object incomparables, @SuppressWarnings("unused") Object env,
                        @Cached Match5Node match5Node) {
            return match5Node.execute(x, table, noMatch, incomparables);
        }

        public static Match5UpCallNode create() {
            return MatchNodesFactory.Match5UpCallNodeGen.create();
        }
    }

    @TypeSystemReference(RTypes.class)
    @GenerateUncached
    public abstract static class NonNullStringMatchNode extends FFIUpCallNode.Arg2 {

        @Specialization(guards = {"s.getLength() == 1", "t.getLength() == 1"})
        Object matchSingle(RStringVector s, RStringVector t) {
            if (s.getDataAt(0) == RRuntime.STRING_NA || t.getDataAt(0) == RRuntime.STRING_NA) {
                return RRuntime.LOGICAL_FALSE;
            }
            return RRuntime.asLogical(s.getDataAt(0).equals(t.getDataAt(0)));
        }

        @Specialization
        Object match(CharSXPWrapper s, CharSXPWrapper t) {
            if (s.getContents() == RRuntime.STRING_NA || t.getContents() == RRuntime.STRING_NA) {
                return RRuntime.LOGICAL_FALSE;
            }
            return RRuntime.asLogical(s.getContents().equals(t.getContents()));
        }

        @Fallback
        @SuppressWarnings("unused")
        Object match(Object s, Object t) {
            throw RInternalError.unimplemented("Rf_NonNullStringMatch");
        }

        public static NonNullStringMatchNode create() {
            return NonNullStringMatchNodeGen.create();
        }

        public static NonNullStringMatchNode getUncached() {
            return NonNullStringMatchNodeGen.getUncached();
        }
    }

}
