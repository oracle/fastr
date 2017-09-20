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
package com.oracle.truffle.r.runtime.data.nodes;

import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RComplexVector;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.RRawVector;
import com.oracle.truffle.r.runtime.data.RStringVector;

/**
 * Contains nodes that encapsulate {@link GetDataStore} and one of the nodes from {@link SetDataAt}
 * as these are often used together. These are convenience wrappers to be used e.g. for a @Cached
 * parameter.
 */
public abstract class VectorWriteAccess {
    @NodeInfo(cost = NodeCost.NONE)
    public static final class Double extends VectorAccessAdapter {
        @Child private SetDataAt.Double setDataAtNode = SetDataAt.Double.create();

        public void setDataAt(RDoubleVector vector, Object store, int index, double value) {
            setDataAtNode.setDataAt(vector, store, index, value);
        }

        public static Double create() {
            return new Double();
        }
    }

    @NodeInfo(cost = NodeCost.NONE)
    public static final class Int extends VectorAccessAdapter {
        @Child private SetDataAt.Int setDataAtNode = SetDataAt.Int.create();

        public void setDataAt(RIntVector vector, Object store, int index, int value) {
            setDataAtNode.setDataAt(vector, store, index, value);
        }

        public static Int create() {
            return new Int();
        }
    }

    @NodeInfo(cost = NodeCost.NONE)
    public static final class Logical extends VectorAccessAdapter {
        @Child private SetDataAt.Logical setDataAtNode = SetDataAt.Logical.create();

        public void setDataAt(RLogicalVector vector, Object store, int index, byte value) {
            setDataAtNode.setDataAt(vector, store, index, value);
        }

        public static Logical create() {
            return new Logical();
        }
    }

    @NodeInfo(cost = NodeCost.NONE)
    public static final class Raw extends VectorAccessAdapter {
        @Child private SetDataAt.Raw setDataAtNode = SetDataAt.Raw.create();

        public void setDataAt(RRawVector vector, Object store, int index, byte value) {
            setDataAtNode.setDataAt(vector, store, index, value);
        }

        public static Raw create() {
            return new Raw();
        }
    }

    @NodeInfo(cost = NodeCost.NONE)
    public static final class String extends VectorAccessAdapter {
        @Child private SetDataAt.String setDataAtNode = SetDataAt.String.create();

        public void setDataAt(RStringVector vector, Object store, int index, java.lang.String value) {
            setDataAtNode.setDataAt(vector, store, index, value);
        }

        public static String create() {
            return new String();
        }
    }

    @NodeInfo(cost = NodeCost.NONE)
    public static final class Complex extends VectorAccessAdapter {
        @Child private SetDataAt.Complex setDataAtNode = SetDataAt.Complex.create();

        public void setDataAt(RComplexVector vector, Object store, int index, RComplex value) {
            setDataAtNode.setDataAt(vector, store, index, value);
        }

        public static Complex create() {
            return new Complex();
        }
    }
}
