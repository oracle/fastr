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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.r.runtime.RInternalError;

public final class VectorDataLibraryUtils {
    // Iterators and exceptions for the use of RXXXVectorDataLibrary-s

    public static RInternalError notWriteableError(Class<?> dataClass, String method) {
        CompilerDirectives.transferToInterpreter();
        throw RInternalError.shouldNotReachHere(String.format("RVectorData class '%s' is not writeable, it must be materialized before writing. Method: '%s'", dataClass.getSimpleName(), method));
    }

    public abstract static class Iterator {
        private final Object store;
        private final int length;

        protected Iterator(Object store, int length) {
            this.store = store;
            this.length = length;
        }

        // Note: intentionally package private
        final Object getStore() {
            return store;
        }

        public final int getLength() {
            return length;
        }
    }

    public static final class SeqIterator extends Iterator {
        private int index;

        protected SeqIterator(Object store, int length) {
            super(store, length);
            index = -1;
        }

        public boolean next() {
            return ++index < getLength();
        }

        public void nextWithWrap() {
            // TODO
        }

        public int getIndex() {
            return index;
        }
    }

    public static final class RandomAccessIterator extends Iterator {
        protected RandomAccessIterator(Object store, int length) {
            super(store, length);
        }
    }

}
