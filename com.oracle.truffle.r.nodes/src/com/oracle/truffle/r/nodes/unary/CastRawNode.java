/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.unary;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.ErrorContext;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RRaw;
import com.oracle.truffle.r.runtime.data.RRawVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractAtomicVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractRawVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess.SequentialIterator;
import com.oracle.truffle.r.runtime.ops.na.NAProfile;

@ImportStatic({DSLConfig.class})
public abstract class CastRawNode extends CastBaseNode {

    protected CastRawNode(boolean preserveNames, boolean preserveDimensions, boolean preserveAttributes) {
        this(preserveNames, preserveDimensions, preserveAttributes, false);
    }

    protected CastRawNode(boolean preserveNames, boolean preserveDimensions, boolean preserveAttributes, boolean forRFFI) {
        super(preserveNames, preserveDimensions, preserveAttributes, forRFFI);
    }

    protected CastRawNode(boolean preserveNames, boolean preserveDimensions, boolean preserveAttributes, boolean forRFFI, boolean useClosures, ErrorContext warningContext) {
        super(preserveNames, preserveDimensions, preserveAttributes, forRFFI, useClosures, warningContext);
    }

    @Child private CastRawNode recursiveCastRaw;

    @Override
    protected final RType getTargetType() {
        return RType.Raw;
    }

    protected Object castRawRecursive(Object o) {
        if (recursiveCastRaw == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            recursiveCastRaw = insert(CastRawNodeGen.create(preserveNames(), preserveDimensions(), preserveRegAttributes()));
        }
        return recursiveCastRaw.executeRaw(o);
    }

    public abstract Object executeRaw(int o);

    public abstract Object executeRaw(double o);

    public abstract Object executeRaw(byte o);

    public abstract Object executeRaw(Object o);

    @Specialization
    protected RNull doNull(@SuppressWarnings("unused") RNull operand) {
        return RNull.instance;
    }

    @Specialization
    protected RMissing doMissing(@SuppressWarnings("unused") RMissing operand) {
        return RMissing.instance;
    }

    private RRaw checkOutOfRange(int operand, int intResult) {
        if (intResult != operand) {
            warning(warningContext(), RError.Message.OUT_OF_RANGE);
            return factory().createRaw((byte) 0);
        }
        return factory().createRaw((byte) intResult);
    }

    @Specialization
    protected RRaw doInt(int operand) {
        int intResult = RRuntime.int2rawIntValue(operand);
        return checkOutOfRange(operand, intResult);
    }

    @Specialization
    protected RRaw doDouble(double operand,
                    @Cached("create()") NAProfile naProfile) {
        int intResult;
        if (naProfile.isNA(operand)) {
            warning(warningContext(), RError.Message.OUT_OF_RANGE);
            intResult = 0;
        } else {
            if (operand > Integer.MAX_VALUE || operand <= Integer.MIN_VALUE) {
                intResult = 0;
                warning(warningContext(), RError.Message.NA_INTRODUCED_COERCION_INT);
            } else {
                intResult = RRuntime.double2rawIntValue(operand);
            }
        }
        return checkOutOfRange((int) operand, intResult);
    }

    @Specialization
    protected RRaw doComplex(RComplex operand,
                    @Cached("create()") NAProfile naProfile) {
        int intResult;
        if (naProfile.isNA(operand)) {
            warning(warningContext(), RError.Message.OUT_OF_RANGE);
            intResult = 0;
        } else {
            double realPart = operand.getRealPart();
            if (realPart > Integer.MAX_VALUE || realPart <= Integer.MIN_VALUE) {
                intResult = 0;
                warning(warningContext(), RError.Message.NA_INTRODUCED_COERCION_INT);
            } else {
                intResult = RRuntime.complex2rawIntValue(operand);
                if (operand.getImaginaryPart() != 0) {
                    warning(warningContext(), RError.Message.IMAGINARY_PARTS_DISCARDED_IN_COERCION);
                }
            }
        }
        return checkOutOfRange((int) operand.getRealPart(), intResult);
    }

    @Specialization
    protected RRaw doRaw(RRaw operand) {
        return operand;
    }

    @Specialization
    protected RRaw doLogical(byte operand) {
        // need to convert to int so that NA-related warning is caught
        int intVal = RRuntime.logical2int(operand);
        return doInt(intVal);
    }

    @Specialization
    protected RRaw doString(String operand,
                    @Cached("create()") NAProfile naProfile,
                    @Cached("createBinaryProfile()") ConditionProfile emptyStringProfile) {
        int intValue;
        if (naProfile.isNA(operand) || emptyStringProfile.profile(operand.isEmpty())) {
            intValue = 0;
        } else {
            intValue = RRuntime.string2intNoCheck(operand);
            if (RRuntime.isNA(intValue)) {
                try {
                    Double.parseDouble(operand);
                    warning(warningContext(), RError.Message.NA_INTRODUCED_COERCION_INT);
                } catch (NumberFormatException e) {
                    warning(warningContext(), RError.Message.NA_INTRODUCED_COERCION);
                }
            }
        }
        int intRawValue = RRuntime.int2rawIntValue(intValue);
        if (intRawValue != intValue) {
            warning(warningContext(), RError.Message.OUT_OF_RANGE);
            return RRaw.valueOf((byte) 0);
        }
        return RRaw.valueOf((byte) intRawValue);
    }

    private RRawVector vectorCopy(RAbstractVector operand, byte[] bdata) {
        RRawVector ret = factory().createRawVector(bdata, getPreservedDimensions(operand), getPreservedNames(operand), getPreservedDimNames(operand));
        if (preserveRegAttributes()) {
            ret.copyRegAttributesFrom(operand);
        }
        return ret;
    }

    private RRawVector createResultVector(RAbstractVector operand, VectorAccess uAccess) {
        byte[] bdata = new byte[operand.getLength()];
        try (SequentialIterator sIter = uAccess.access(operand, warningContext())) {
            while (uAccess.next(sIter)) {
                bdata[sIter.getIndex()] = uAccess.getRaw(sIter);
            }
        }
        return vectorCopy(operand, bdata);
    }

    @Specialization(guards = {"uAccess.supports(operand)", "!isRawVector(operand)"}, limit = "getGenericVectorAccessCacheSize()")
    protected RRawVector doAbstractVector(RAbstractAtomicVector operand,
                    @Cached("operand.access()") VectorAccess uAccess) {
        return createResultVector(operand, uAccess);
    }

    @Specialization(replaces = "doAbstractVector", guards = {"!isRawVector(operand)"})
    protected RRawVector doAbstractVectorGeneric(RAbstractAtomicVector operand) {
        return createResultVector(operand, operand.slowPathAccess());
    }

    @Specialization
    protected RAbstractRawVector doRawVector(RAbstractRawVector operand) {
        return operand;
    }

    @Specialization(guards = {"uAccess.supports(value)"}, limit = "getVectorAccessCacheSize()")
    protected RRawVector doList(RAbstractListVector value,
                    @Cached("value.access()") VectorAccess uAccess) {
        byte[] bdata = new byte[value.getLength()];
        try (SequentialIterator sIter = uAccess.access(value, warningContext())) {
            while (uAccess.next(sIter)) {
                int i = sIter.getIndex();
                Object entry = uAccess.getListElement(sIter);
                if (entry instanceof RList) {
                    bdata[i] = 0;
                } else {
                    Object castEntry = castRawRecursive(entry);
                    if (castEntry instanceof RRaw) {
                        bdata[i] = ((RRaw) castRawRecursive(castEntry)).getValue();
                    } else if (castEntry instanceof RAbstractRawVector) {
                        RAbstractRawVector rawVector = (RAbstractRawVector) castEntry;
                        if (rawVector.getLength() == 1) {
                            bdata[i] = rawVector.getRawDataAt(0);
                        } else if (rawVector.getLength() == 0) {
                            bdata[i] = 0;
                        } else {
                            throw throwCannotCoerceListError("object");
                        }
                    } else {
                        throw throwCannotCoerceListError("object");
                    }
                }
            }
        }
        RRawVector result = factory().createRawVector(bdata, getPreservedDimensions(value), getPreservedNames(value), null);
        if (preserveRegAttributes()) {
            result.copyRegAttributesFrom(value);
        }
        return result;
    }

    @Specialization(replaces = "doList")
    protected RRawVector doListGenric(RAbstractListVector value) {
        return doList(value, value.slowPathAccess());
    }

    @Specialization(guards = "!pairList.isLanguage()")
    protected RRawVector doPairList(RPairList pairList) {
        return (RRawVector) castRawRecursive(pairList.toRList());
    }

    public static CastRawNode createForRFFI(boolean preserveNames, boolean preserveDimensions, boolean preserveAttributes) {
        return CastRawNodeGen.create(preserveNames, preserveDimensions, preserveAttributes, true);
    }

    public static CastRawNode createNonPreserving() {
        return CastRawNodeGen.create(false, false, false);
    }

    protected boolean isRawVector(RAbstractAtomicVector x) {
        return x instanceof RAbstractRawVector;
    }

}
