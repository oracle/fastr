/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.unary;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.interop.ForeignArray2R;
import com.oracle.truffle.r.runtime.interop.ForeignArray2RNodeGen;

@ImportStatic(RRuntime.class)
public abstract class CastSymbolNode extends CastBaseNode {

    @Child private ToStringNode toString = ToStringNodeGen.create();
    @Child private CastSymbolNode recursiveCastSymbol;

    protected CastSymbolNode(boolean preserveNames, boolean preserveDimensions, boolean preserveAttributes) {
        this(preserveNames, preserveDimensions, preserveAttributes, false);
    }

    protected CastSymbolNode(boolean preserveNames, boolean preserveDimensions, boolean preserveAttributes, boolean forRFFI) {
        super(preserveNames, preserveDimensions, preserveAttributes, forRFFI);
    }

    @Override
    protected final RType getTargetType() {
        return RType.Symbol;
    }

    public abstract Object executeSymbol(Object o);

    private String toString(Object value) {
        return toString.executeString(value, true, ToStringNode.DEFAULT_SEPARATOR);
    }

    @Specialization
    protected RSymbol doNull(@SuppressWarnings("unused") RNull value) {
        throw error(RError.Message.INVALID_TYPE_LENGTH, "symbol", 0);
    }

    @Specialization
    protected RSymbol doSymbol(RSymbol value) {
        return value;
    }

    @Specialization
    protected RSymbol doInteger(int value) {
        return asSymbol(toString(value));
    }

    @Specialization
    protected RSymbol doDouble(double value) {
        return asSymbol(toString(value));
    }

    @Specialization
    protected RSymbol doLogical(byte value) {
        return asSymbol(toString(value));
    }

    @Specialization
    @TruffleBoundary
    protected RSymbol doString(String value) {
        if (value.isEmpty()) {
            CompilerDirectives.transferToInterpreter();
            throw error(RError.Message.ZERO_LENGTH_VARIABLE);
        }
        return RDataFactory.createSymbolInterned(value);
    }

    @Specialization(guards = "value.getLength() > 0")
    protected RSymbol doStringVector(RStringVector value) {
        // Only element 0 interpreted
        return doString(value.getDataAt(0));
    }

    @Specialization(guards = "value.getLength() > 0")
    protected RSymbol doIntegerVector(RIntVector value) {
        return doInteger(value.getDataAt(0));
    }

    @Specialization(guards = "value.getLength() > 0")
    protected RSymbol doDoubleVector(RDoubleVector value) {
        return doDouble(value.getDataAt(0));
    }

    @Specialization(guards = "value.getLength() > 0")
    protected RSymbol doLogicalVector(RLogicalVector value) {
        return doLogical(value.getDataAt(0));
    }

    @Specialization(guards = "vector.getLength() == 0")
    @TruffleBoundary
    protected RSymbol doEmptyVector(RAbstractVector vector) {
        if (vector instanceof RList) {
            throw error(RError.Message.INVALID_TYPE_LENGTH, "symbol", 0);
        } else {
            throw error(Message.INVALID_DATA_OF_TYPE_TOO_SHORT, vector.getRType().getName(), 0);
        }
    }

    @TruffleBoundary
    private static RSymbol asSymbol(String s) {
        return RDataFactory.createSymbolInterned(s);
    }

    @Specialization(guards = "isForeignObject(obj)")
    protected RSymbol doForeignObject(TruffleObject obj,
                    @Cached("createForeignArray2RNode()") ForeignArray2R foreignArray2R) {
        Object o = foreignArray2R.execute(obj, true);
        if (!RRuntime.isForeignObject(o)) {
            return (RSymbol) castSymbolRecursive(o);
        }
        throw error(RError.Message.CANNOT_COERCE_EXTERNAL_OBJECT_TO_VECTOR, "vector");
    }

    @Override
    protected Object doOtherRFFI(Object mappedValue) {
        if (mappedValue instanceof RList) {
            // to be compatible with GnuR
            throw RError.error(RError.NO_CALLER, Message.INVALID_TYPE_LENGTH, "symbol", ((RList) mappedValue).getLength());
        }
        return super.doOtherRFFI(mappedValue);
    }

    public static CastSymbolNode createForRFFI(boolean preserveNames, boolean preserveDimensions, boolean preserveAttributes) {
        return CastSymbolNodeGen.create(preserveNames, preserveDimensions, preserveAttributes, true);
    }

    public static CastSymbolNode createNonPreserving() {
        return CastSymbolNodeGen.create(false, false, false);
    }

    protected ForeignArray2R createForeignArray2RNode() {
        return ForeignArray2RNodeGen.create();
    }

    private Object castSymbolRecursive(Object o) {
        if (recursiveCastSymbol == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            recursiveCastSymbol = insert(CastSymbolNodeGen.create(preserveNames(), preserveDimensions(), preserveAttributes()));
        }
        return recursiveCastSymbol.executeSymbol(o);
    }
}
