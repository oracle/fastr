/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.access.vector;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.binary.BoxPrimitiveNode;
import com.oracle.truffle.r.nodes.unary.CastIntegerNode;
import com.oracle.truffle.r.nodes.unary.CastIntegerNodeGen;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDouble;
import com.oracle.truffle.r.runtime.data.REmpty;
import com.oracle.truffle.r.runtime.data.RInteger;
import com.oracle.truffle.r.runtime.data.RLogical;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RString;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.data.RTypedValue;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

abstract class PositionCastNode extends RBaseNode {

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
    protected RAbstractVector doLong(long position, @Cached("create()") NACheck check) {
        return doDouble(position, check);
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
            return (RAbstractVector) box.execute(cast.doCast(position));
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
        if (position == RSymbol.MISSING) {
            return doMissing(RMissing.instance);
        } else {
            throw error(RError.Message.INVALID_SUBSCRIPT_TYPE, "symbol");
        }
    }

    @Specialization
    protected RMissing doMissing(@SuppressWarnings("unused") RMissing position) {
        if (mode.isSubscript()) {
            if (replace) {
                throw error(RError.Message.MISSING_SUBSCRIPT);
            } else {
                throw error(Message.SUBSCRIPT_BOUNDS);
            }
        } else {
            return RMissing.instance;
        }
    }

    @Specialization
    protected RMissing doEmpty(@SuppressWarnings("unused") REmpty position) {
        return doMissing(RMissing.instance);
    }

    @Specialization
    protected RAbstractVector doNull(@SuppressWarnings("unused") RNull position) {
        // NULL expands to integer(0).
        return RDataFactory.createEmptyIntVector();
    }

    @Fallback
    protected RAbstractVector doInvalidType(Object position) {
        RType type = getInvalidType(position);
        String name = type == null ? "unknown" : type.getName();
        throw error(RError.Message.INVALID_SUBSCRIPT_TYPE, name);
    }

    protected static RType getInvalidType(Object positionValue) {
        return positionValue instanceof RTypedValue ? ((RTypedValue) positionValue).getRType() : null;
    }
}
