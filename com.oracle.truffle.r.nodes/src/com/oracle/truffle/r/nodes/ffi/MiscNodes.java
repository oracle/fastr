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
package com.oracle.truffle.r.nodes.ffi;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RTypesFlatLayout;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.ffi.CharSXPWrapper;
import com.oracle.truffle.r.runtime.gnur.SEXPTYPE;

public final class MiscNodes {

    @TypeSystemReference(RTypesFlatLayout.class)
    public abstract static class LENGTHNode extends FFIUpCallNode.Arg1 {

        @Specialization
        protected int length(@SuppressWarnings("unused") RNull obj) {
            return 0;
        }

        @Specialization
        protected int length(@SuppressWarnings("unused") int x) {
            return 1;
        }

        @Specialization
        protected int length(@SuppressWarnings("unused") double x) {
            return 1;
        }

        @Specialization
        protected int length(@SuppressWarnings("unused") byte x) {
            return 1;
        }

        @Specialization
        protected int length(@SuppressWarnings("unused") String x) {
            return 1;
        }

        @Specialization
        protected int length(CharSXPWrapper obj) {
            return obj.getContents().length();
        }

        @Specialization
        protected int length(RAbstractContainer obj) {
            // Should this use RLengthNode?
            return obj.getLength();
        }

        @Fallback
        protected int length(Object obj) {
            CompilerDirectives.transferToInterpreter();
            throw RError.error(RError.SHOW_CALLER2, RError.Message.LENGTH_MISAPPLIED, SEXPTYPE.gnuRTypeForObject(obj).name());
        }
    }
}
