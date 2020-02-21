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
package com.oracle.truffle.r.runtime.data;

import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

@ExportLibrary(value = AbstractContainerLibrary.class, receiverType = RAbstractVector.class)
public class DefaultRContainerDataLibrary {
    @ExportMessage
    @ImportStatic(DSLConfig.class)
    static class GetLength {
        @Specialization(limit = "getGenericVectorAccessCacheSize()")
        static int getLength(RAbstractVector receiver, @CachedLibrary("receiver.getData()") VectorDataLibrary lib) {
            return lib.getLength(receiver.getData());
        }
    }

    @ExportMessage
    @ImportStatic({DSLConfig.class})
    static class IsComplete {
        @Specialization(limit = "getGenericVectorAccessCacheSize()")
        static boolean isComplete(RAbstractVector receiver, @CachedLibrary("receiver.getData()") VectorDataLibrary lib) {
            return lib.isComplete(receiver.getData());
        }
    }

    // XXX TODO this is temporary
    // To retain the semantics of the original materialize,
    // for sequences and such we return new vector
    @ExportMessage
    @ImportStatic({DSLConfig.class})
    static class Materialize {

        @Specialization(guards = "isRIntVector(vector)", limit = "getGenericVectorAccessCacheSize()")
        static RAbstractVector materializeInt(RAbstractVector vector, @CachedLibrary("vector.getData()") VectorDataLibrary library) {
            if (library.isWriteable(vector.getData())) {
                return vector;
            }
            return new RIntVector(library.materialize(vector.getData()), library.getLength(vector.getData()));
        }

        @Specialization(guards = "isRDoubleVector(vector)", limit = "getGenericVectorAccessCacheSize()")
        static RAbstractVector materializeDouble(RAbstractVector vector, @CachedLibrary("vector.getData()") VectorDataLibrary library) {
            if (library.isWriteable(vector.getData())) {
                return vector;
            }
            return new RDoubleVector(library.materialize(vector.getData()), library.getLength(vector.getData()));
        }

        @Specialization(guards = "!isRIntVector(vector) || !isRDoubleVector(vector)")
        static RAbstractVector materialize(RAbstractVector vector) {
            return vector.materialize();
        }
    }

    // XXX TODO is this temporary?
    // To retain the semantics of the original materialize,
    // for sequences and such we return new vector
    @ExportMessage
    @ImportStatic({DSLConfig.class})
    static class Copy {

        @Specialization(guards = "isRIntVector(vector)", limit = "getGenericVectorAccessCacheSize()")
        static RAbstractVector copyInt(RAbstractVector vector, @CachedLibrary("vector.getData()") VectorDataLibrary library) {
            return ((RIntVector) vector).copy(library);
        }

        @Specialization(guards = "isRDoubleVector(vector)", limit = "getGenericVectorAccessCacheSize()")
        static RAbstractVector copyDouble(RAbstractVector vector, @CachedLibrary("vector.getData()") VectorDataLibrary library) {
            return ((RDoubleVector) vector).copy(library);
        }

        @Specialization(guards = "!isRIntVector(vector) || !isRDoubleVector(vector)")
        static RAbstractVector copy(RAbstractVector vector) {
            return vector.copy();
        }

    }

    // XXX using a different types in msg specializations breaks DSL
    protected static boolean isRIntVector(RAbstractVector v) {
        return v instanceof RIntVector;
    }

    protected static boolean isRDoubleVector(RAbstractVector v) {
        return v instanceof RDoubleVector;
    }

}
