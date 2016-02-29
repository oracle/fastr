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
package com.oracle.truffle.r.library.fastr;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.instrument.factory.RInstrumentFactory;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RNull;

public class FastRCallCounting {

    public abstract static class CreateCallCounter extends RExternalBuiltinNode.Arg1 {
        @Specialization
        protected RNull createCallCounter(RFunction function) {
            if (!function.isBuiltin()) {
                if (!RInstrumentFactory.getInstance().installCounter(function)) {
                    throw RError.error(this, RError.Message.GENERIC, "failed to apply counter, instrumention disabled?");
                }
            }
            return RNull.instance;
        }

        @SuppressWarnings("unused")
        @Fallback
        protected Object fallback(Object a1) {
            throw RError.error(this, RError.Message.INVALID_ARGUMENT, "func");
        }
    }

    public abstract static class GetCallCounter extends RExternalBuiltinNode.Arg1 {
        @Specialization
        protected Object getCallCount(RFunction function) {
            if (!function.isBuiltin()) {
                int entryCount = RInstrumentFactory.getInstance().getCounter(function);
                if (entryCount < 0) {
                    throw RError.error(this, RError.Message.GENERIC, "no associated counter");
                } else {
                    return entryCount;
                }
            }
            return RNull.instance;
        }

        @SuppressWarnings("unused")
        @Fallback
        protected Object fallback(Object a1) {
            throw RError.error(this, RError.Message.INVALID_ARGUMENT, "func");
        }
    }

}
