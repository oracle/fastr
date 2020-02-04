/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.RBaseObject;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDouble;
import com.oracle.truffle.r.runtime.data.REmpty;
import com.oracle.truffle.r.runtime.data.RLogical;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RString;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

/**
 * Converts any position value to type supported by {@link PositionCheckNode}, which is string
 * vector, logical vector, integer vector or missing. There is an exception for doubles and subset
 * operation: they are not as the conversion must involve bounds check (only for subset), which
 * cannot be done here.
 *
 * Note, a position is an R object, i.e. in {@code x[c(1,3), c(4.5, 5)]} position is vector
 * {@code c(1,3)} and vector {@code c(4.5,5)} is another position. {@link CachedExtractVectorNode}
 * gets an array of such positions -- one per each dimension of the target vector and passes that to
 * the {@link PositionsCheckNode}, which passes it here.
 */
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

    public abstract RBaseObject execute(Object position);

    // We use boxing specializations to avoid using the type system in this specific case
    @Specialization
    protected RAbstractVector doInteger(int position) {
        return RDataFactory.createIntVectorFromScalar(position);
    }

    // We handle long indexes as ExtractVectorNode, which uses this node, can be used from the
    // interop message handler for READ and there the index can be a long.
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
    protected RAbstractVector doInteger(RIntVector position) {
        // directly supported
        return position;
    }

    @Specialization
    protected RAbstractVector doDouble(double position,
                    @Cached("create()") NACheck check) {
        if (mode.isSubscript()) {
            check.enable(position);
            return RDataFactory.createIntVectorFromScalar(check.convertDoubleToInt(position));
        } else {
            return RDouble.valueOf(position);
        }
    }

    @Specialization
    protected RAbstractVector doDouble(RAbstractDoubleVector position,
                    @Cached("createIntegerCast()") CastIntegerNode cast,
                    @Cached("create()") BoxPrimitiveNode box) {
        if (mode.isSubscript()) {
            // double gets cast to integer for subscript
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
        if (mode.isSubscript() && replace) {
            throw error(RError.Message.MISSING_SUBSCRIPT);
        } else {
            // Note: behavior in the case of 'isSubscript() && !replace' is handled elsewhere as it
            // depends on the type of the target vector
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
        return positionValue instanceof RBaseObject ? ((RBaseObject) positionValue).getRType() : null;
    }
}
