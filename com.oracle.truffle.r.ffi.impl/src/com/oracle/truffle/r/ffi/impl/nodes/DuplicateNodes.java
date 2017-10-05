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

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.ffi.impl.nodes.DuplicateNodesFactory.DuplicateNodeGen;
import com.oracle.truffle.r.runtime.data.RExternalPtr;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RSequence;
import com.oracle.truffle.r.runtime.data.RShareable;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.env.REnvironment;

public final class DuplicateNodes {

    public abstract static class DuplicateNode extends FFIUpCallNode.Arg2 {

        @Specialization
        public Object duplicateShareable(RShareable x, int deep) {
            assert !isReusableForDuplicate(x);
            return deep == 1 ? x.deepCopy() : x.copy();
        }

        @Specialization
        public Object duplicateSequence(RSequence x, @SuppressWarnings("unused") int deep) {
            return x.materialize();
        }

        @Specialization
        public Object duplicateExternalPtr(RExternalPtr x, @SuppressWarnings("unused") int deep) {
            return x.copy();
        }

        @Specialization(guards = "isReusableForDuplicate(val)")
        public Object returnReusable(Object val, @SuppressWarnings("unused") int deep) {
            return val;
        }

        protected static boolean isReusableForDuplicate(Object o) {
            return o == RNull.instance || o instanceof REnvironment || o instanceof RSymbol;
        }

        public static DuplicateNode create() {
            return DuplicateNodeGen.create();
        }
    }

}
