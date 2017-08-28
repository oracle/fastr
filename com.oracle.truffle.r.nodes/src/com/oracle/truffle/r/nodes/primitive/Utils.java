/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.primitive;

import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.data.nodes.SetDataAt;
import com.oracle.truffle.r.runtime.data.nodes.VectorIterator;

abstract class Utils {
    private Utils() {
    }

    static SetDataAt createSetDataAtNode(RType type) {
        switch (type) {
            case Raw:
                return SetDataAt.Raw.create();
            case Logical:
                return SetDataAt.Logical.create();
            case Integer:
                return SetDataAt.Int.create();
            case Double:
                return SetDataAt.Double.create();
            case Complex:
                return SetDataAt.Complex.create();
            case Character:
                return SetDataAt.String.create();
            default:
                throw RInternalError.shouldNotReachHere("BinaryMapNode unexpected result type " + type);
        }
    }

    static final class VecStore<T extends RAbstractVector> {
        public final T vector;
        public final Object store;

        VecStore(T vector, Object store) {
            this.vector = vector;
            this.store = store;
        }
    }

    public static VectorIterator.Generic createIterator() {
        return VectorIterator.Generic.create();
    }

    public static VectorIterator.Generic createIteratorWrapAround() {
        return VectorIterator.Generic.createWrapAround();
    }
}
