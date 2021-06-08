/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary;

@GenerateUncached
public abstract class StringEltNode extends FFIUpCallNode.Arg2 {
    public static StringEltNode create() {
        return StringEltNodeGen.create();
    }

    @Specialization(limit = "1")
    Object doStringVector(RStringVector stringVector, long index,
                    @CachedLibrary(limit = "getTypedVectorDataLibraryCacheSize()") VectorDataLibrary genericDataLib,
                    @CachedLibrary("stringVector.getData()") VectorDataLibrary stringVecDataLib) {
        Object newCharSXPData = stringVecDataLib.materializeCharSXPStorage(stringVector.getData());
        stringVector.setData(newCharSXPData);
        if (index > Integer.MAX_VALUE) {
            CompilerDirectives.transferToInterpreter();
            throw RError.error(RError.NO_CALLER, RError.Message.LONG_VECTORS_NOT_SUPPORTED);
        }
        return genericDataLib.getCharSXPAt(stringVector.getData(), (int) index);
    }
}
