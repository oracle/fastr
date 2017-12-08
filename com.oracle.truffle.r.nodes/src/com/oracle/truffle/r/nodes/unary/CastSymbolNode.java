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
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RRaw;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.data.model.RAbstractAtomicVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess.SequentialIterator;

public abstract class CastSymbolNode extends CastBaseNode {

    @Child private ToStringNode toString = ToStringNodeGen.create();

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

    private String toString(Object value) {
        return toString.executeString(value, ToStringNode.DEFAULT_SEPARATOR);
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
    protected RSymbol doRaw(RRaw value) {
        return asSymbol(toString(value));
    }

    @Specialization
    protected RSymbol doComplex(RComplex value) {
        return asSymbol(toString(value));
    }

    @Specialization
    @TruffleBoundary
    protected RSymbol doString(String value) {
        if (value.isEmpty()) {
            CompilerDirectives.transferToInterpreter();
            throw error(RError.Message.ZERO_LENGTH_VARIABLE);
        }
        return asSymbol(value);
    }

    @Specialization(guards = "access.supports(vector)")
    protected RSymbol doVector(RAbstractAtomicVector vector,
                    @Cached("createBinaryProfile()") ConditionProfile emptyProfile,
                    @Cached("vector.access()") VectorAccess access) {
        SequentialIterator it = access.access(vector);
        if (emptyProfile.profile(!access.next(it))) {
            throw doEmptyVector(vector);
        }
        switch (access.getType()) {
            case Raw:
                return asSymbol(toString(RRaw.valueOf(access.getRaw(it))));
            case Logical:
                return doLogical(access.getLogical(it));
            case Integer:
                return doInteger(access.getInt(it));
            case Double:
                return doDouble(access.getDouble(it));
            case Complex:
                return doComplex(access.getComplex(it));
            case Character:
                return doString(access.getString(it));
            default:
                CompilerDirectives.transferToInterpreter();
                throw RInternalError.shouldNotReachHere("unexpected atomic type " + access.getType());
        }
    }

    @Specialization(replaces = "doVector")
    protected RSymbol doVectorGeneric(RAbstractAtomicVector vector,
                    @Cached("createBinaryProfile()") ConditionProfile emptyProfile) {
        return doVector(vector, emptyProfile, vector.slowPathAccess());
    }

    @TruffleBoundary
    protected RError doEmptyVector(RAbstractVector vector) {
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
}
