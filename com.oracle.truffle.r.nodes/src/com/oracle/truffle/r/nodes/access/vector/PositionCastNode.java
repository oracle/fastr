/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.access.vector;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.nodes.binary.BoxPrimitiveNode;
import com.oracle.truffle.r.nodes.unary.CastIntegerNode;
import com.oracle.truffle.r.nodes.unary.CastIntegerNodeGen;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDouble;
import com.oracle.truffle.r.runtime.data.REmpty;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RInteger;
import com.oracle.truffle.r.runtime.data.RLogical;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RString;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.data.RTypedValue;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractRawVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

abstract class PositionCastNode extends Node {

    private final ElementAccessMode mode;
    private final boolean replace;

    PositionCastNode(ElementAccessMode mode, boolean replace) {
        this.mode = mode;
        this.replace = replace;
    }

    public static PositionCastNode create(ElementAccessMode mode, boolean replace) {
        return PositionCastNodeGen.create(mode, replace);
    }

    public abstract RTypedValue execute(Object position);

    // TODO these boxing specializations can be removed as we enabled boxing of every value.
    @Specialization
    protected RAbstractVector doInteger(int position) {
        return RInteger.valueOf(position);
    }

    @Specialization
    protected RAbstractVector doString(String position) {
        return RString.valueOf(position);
    }

    @Specialization
    protected RAbstractVector doString(RAbstractStringVector position) {
        // directly supported
        return position;
    }

    @Specialization
    protected RAbstractVector doInteger(RAbstractIntVector position) {
        // directly supported
        return position;
    }

    @Specialization
    protected RAbstractVector doDouble(double position,
                    @Cached("create()") NACheck check) {
        if (mode.isSubscript()) {
            check.enable(position);
            return RInteger.valueOf(check.convertDoubleToInt(position));
        } else {
            return RDouble.valueOf(position);
        }
    }

    @Specialization
    protected RAbstractVector doDouble(RAbstractDoubleVector position,
                    @Cached("createIntegerCast()") CastIntegerNode cast,
                    @Cached("create()") BoxPrimitiveNode box) {
        if (mode.isSubscript()) {
            // double gets casted to integer for subscript
            return (RAbstractVector) box.execute(cast.execute(position));
        } else {
            // because we need to perform a special bounds check with doubles
            // we cannot yet convert the double array to int for subsets
            return (RAbstractVector) box.execute(position);
        }
    }

    protected CastIntegerNode createIntegerCast() {
        return CastIntegerNodeGen.create(true, false, false);
    }

    @Specialization
    protected RAbstractVector doLogical(byte position) {
        // directly supported
        return RLogical.valueOf(position);
    }

    @Specialization
    protected RAbstractVector doLogical(RAbstractLogicalVector position) {
        // directly supported
        return position;
    }

    @Specialization
    protected RMissing doSymbol(RSymbol position) {
        if (position.getName().length() == 0) {
            return doMissing(RMissing.instance);
        } else {
            throw RError.error(this, RError.Message.INVALID_SUBSCRIPT_TYPE, "symbol");
        }
    }

    @Specialization
    protected RMissing doMissing(@SuppressWarnings("unused") RMissing position) {
        if (mode.isSubscript()) {
            if (replace) {
                throw RError.error(this, RError.Message.MISSING_SUBSCRIPT);
            } else {
                throw RError.error(this, RError.Message.INVALID_SUBSCRIPT_TYPE, "symbol");
            }
        } else {
            return RMissing.instance;
        }
    }

    @Specialization
    protected RMissing doEmpty(@SuppressWarnings("unused") REmpty position) {
        return doMissing(null);
    }

    @Specialization
    protected RAbstractVector doNull(@SuppressWarnings("unused") RNull position) {
        // NULL expands to integer(0).
        return RDataFactory.createEmptyIntVector();
    }

    @Specialization(guards = "getInvalidType(position) != null")
    protected RAbstractVector doInvalidType(Object position) {
        throw RError.error(this, RError.Message.INVALID_SUBSCRIPT_TYPE, getInvalidType(position).getName());
    }

    protected static RType getInvalidType(Object positionValue) {
        if (positionValue instanceof RAbstractRawVector) {
            return RType.Raw;
        } else if (positionValue instanceof RAbstractListVector) {
            return RType.List;
        } else if (positionValue instanceof RFunction) {
            return RType.Closure;
        } else if (positionValue instanceof REnvironment) {
            return RType.Environment;
        } else if (positionValue instanceof RAbstractComplexVector) {
            return RType.Complex;
        }
        return null;
    }
}
