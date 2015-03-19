/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

/**
 * Conversion and manipulation of objects of type "raw".
 */
public class RawFunctions {

    @RBuiltin(name = "charToRaw", kind = RBuiltinKind.INTERNAL, parameterNames = "x")
    public abstract static class CharToRaw extends RBuiltinNode {
        @Specialization
        protected RRawVector charToRaw(RAbstractStringVector x) {
            if (x.getLength() > 1) {
                RError.warning(getEncapsulatingSourceSection(), RError.Message.ARG_SHOULD_BE_CHARACTER_VECTOR_LENGTH_ONE);
            }
            String s = x.getDataAt(0);
            byte[] data = new byte[s.length()];
            for (int i = 0; i < data.length; i++) {
                data[i] = (byte) s.charAt(i);
            }
            return RDataFactory.createRawVector(data);
        }

        @Fallback
        protected Object charToRaw(@SuppressWarnings("unused") Object x) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.ARG_MUST_BE_CHARACTER_VECTOR_LENGTH_ONE);
        }

    }

    // TODO the rest of the functions

}
