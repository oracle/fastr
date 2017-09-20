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
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractRawVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

/**
 * Contains nodes that encapsulate {@link GetDataStore} and one of the nodes from {@link GetDataAt}
 * as these are often used together. These are convenience wrappers to be used e.g. for a @Cached
 * parameter.
 */
public abstract class VectorReadAccess extends VectorAccessAdapter {

    public abstract Object getDataAtAsObject(RAbstractVector vector, Object store, int index);

    @NodeInfo(cost = NodeCost.NONE)
    public static final class Double extends VectorReadAccess {
        @Child private GetDataAt.Double getDataAtNode = GetDataAt.Double.create();

        public double getDataAt(RAbstractDoubleVector vec, Object store, int index) {
            return getDataAtNode.get(vec, store, index);
        }

        @Override
        public Object getDataAtAsObject(RAbstractVector vector, Object store, int index) {
            return getDataAt((RAbstractDoubleVector) vector, store, index);
        }

        public static Double create() {
            return new Double();
        }
    }

    @NodeInfo(cost = NodeCost.NONE)
    public static final class Int extends VectorReadAccess {
        @Child private GetDataAt.Int getDataAtNode = GetDataAt.Int.create();

        public int getDataAt(RAbstractIntVector vec, Object store, int index) {
            return getDataAtNode.get(vec, store, index);
        }

        @Override
        public Object getDataAtAsObject(RAbstractVector vector, Object store, int index) {
            return getDataAt((RAbstractIntVector) vector, store, index);
        }

        public static Int create() {
            return new Int();
        }
    }

    @NodeInfo(cost = NodeCost.NONE)
    public static final class Logical extends VectorReadAccess {
        @Child private GetDataAt.Logical getDataAtNode = GetDataAt.Logical.create();

        public byte getDataAt(RAbstractLogicalVector vec, Object store, int index) {
            return getDataAtNode.get(vec, store, index);
        }

        @Override
        public Object getDataAtAsObject(RAbstractVector vector, Object store, int index) {
            return getDataAt((RAbstractLogicalVector) vector, store, index);
        }

        public static Logical create() {
            return new Logical();
        }
    }

    @NodeInfo(cost = NodeCost.NONE)
    public static final class Complex extends VectorReadAccess {
        @Child private GetDataAt.Complex getDataAtNode = GetDataAt.Complex.create();

        public RComplex getDataAt(RAbstractComplexVector vec, Object store, int index) {
            return getDataAtNode.get(vec, store, index);
        }

        @Override
        public Object getDataAtAsObject(RAbstractVector vector, Object store, int index) {
            return getDataAt((RAbstractComplexVector) vector, store, index);
        }

        public static Complex create() {
            return new Complex();
        }
    }

    @NodeInfo(cost = NodeCost.NONE)
    public static final class String extends VectorReadAccess {
        @Child private GetDataAt.String getDataAtNode = GetDataAt.String.create();

        public java.lang.String getDataAt(RAbstractStringVector vec, Object store, int index) {
            return getDataAtNode.get(vec, store, index);
        }

        @Override
        public Object getDataAtAsObject(RAbstractVector vector, Object store, int index) {
            return getDataAt((RAbstractStringVector) vector, store, index);
        }

        public static String create() {
            return new String();
        }
    }

    @NodeInfo(cost = NodeCost.NONE)
    public static final class Raw extends VectorReadAccess {
        @Child private GetDataAt.Raw getDataAtNode = GetDataAt.Raw.create();

        public byte getDataAt(RAbstractRawVector vec, Object store, int index) {
            return getDataAtNode.get(vec, store, index);
        }

        @Override
        public Object getDataAtAsObject(RAbstractVector vector, Object store, int index) {
            return getDataAt((RAbstractRawVector) vector, store, index);
        }

        public static Raw create() {
            return new Raw();
        }
    }
}
