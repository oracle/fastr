/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.ErrorContext;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RComplexVector;
import com.oracle.truffle.r.runtime.data.RForeignBooleanWrapper;
import com.oracle.truffle.r.runtime.data.RForeignDoubleWrapper;
import com.oracle.truffle.r.runtime.data.RForeignStringWrapper;
import com.oracle.truffle.r.runtime.data.RForeignVectorWrapper;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RRaw;
import com.oracle.truffle.r.runtime.data.closures.RClosures;
import com.oracle.truffle.r.runtime.data.model.RAbstractAtomicVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess.SequentialIterator;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

public abstract class CastComplexNode extends CastBaseNode {

    public abstract Object executeComplex(int o);

    public abstract Object executeComplex(double o);

    public abstract Object executeComplex(byte o);

    public abstract Object executeComplex(Object o);

    protected CastComplexNode(boolean preserveNames, boolean preserveDimensions, boolean preserveAttributes) {
        super(preserveNames, preserveDimensions, preserveAttributes);
    }

    protected CastComplexNode(boolean preserveNames, boolean preserveDimensions, boolean preserveAttributes, boolean forRFFI) {
        super(preserveNames, preserveDimensions, preserveAttributes, forRFFI);
    }

    protected CastComplexNode(boolean preserveNames, boolean preserveDimensions, boolean preserveAttributes, boolean forRFFI, ErrorContext warningContext) {
        super(preserveNames, preserveDimensions, preserveAttributes, forRFFI, false, warningContext);
    }

    @Child private CastComplexNode recursiveCastComplex;

    private Object castComplexRecursive(Object o) {
        if (recursiveCastComplex == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            recursiveCastComplex = insert(CastComplexNodeGen.create(preserveNames(), preserveDimensions(), preserveRegAttributes()));
        }
        return recursiveCastComplex.executeComplex(o);
    }

    @Override
    protected final RType getTargetType() {
        return RType.Complex;
    }

    @Specialization
    protected RNull doNull(@SuppressWarnings("unused") RNull operand) {
        return RNull.instance;
    }

    @Specialization
    protected RMissing doMissing(@SuppressWarnings("unused") RMissing operand) {
        return RMissing.instance;
    }

    @Specialization
    protected RComplex doInt(int operand,
                    @Cached("create()") NACheck naCheck) {
        naCheck.enable(operand);
        return naCheck.convertIntToComplex(operand);
    }

    @Specialization
    protected RComplex doDouble(double operand,
                    @Cached("create()") NACheck naCheck) {
        naCheck.enable(operand);
        return naCheck.convertDoubleToComplex(operand);
    }

    @Specialization
    protected RComplex doLogical(byte operand,
                    @Cached("create()") NACheck naCheck) {
        naCheck.enable(operand);
        return naCheck.convertLogicalToComplex(operand);
    }

    @Specialization
    protected RComplex doComplex(RComplex operand) {
        return operand;
    }

    @Specialization
    protected RComplex doRaw(RRaw operand) {
        return factory().createComplex(operand.getValue(), 0);
    }

    @Specialization
    protected RComplex doCharacter(String operand,
                    @Cached("createBinaryProfile()") ConditionProfile emptyStringProfile,
                    @Cached("create()") NACheck naCheck) {
        naCheck.enable(operand);
        if (naCheck.check(operand) || emptyStringProfile.profile(operand.isEmpty())) {
            return RComplex.createNA();
        }
        RComplex result = RRuntime.string2complexNoCheck(operand);
        if (RRuntime.isNA(result) && !operand.equals(RRuntime.STRING_NaN)) {
            warning(warningContext(), RError.Message.NA_INTRODUCED_COERCION);
        }
        return result;
    }

    private RComplexVector createResultVector(RAbstractVector operand, VectorAccess uAccess) {
        double[] ddata = new double[operand.getLength() << 1];
        try (SequentialIterator sIter = uAccess.access(operand, warningContext())) {
            while (uAccess.next(sIter)) {
                RComplex complexValue = uAccess.getComplex(sIter);
                int index = sIter.getIndex() << 1;
                ddata[index] = complexValue.getRealPart();
                ddata[index + 1] = complexValue.getImaginaryPart();
            }
        }
        RComplexVector ret = factory().createComplexVector(ddata, !uAccess.na.isEnabled(), getPreservedDimensions(operand), getPreservedNames(operand), getPreservedDimNames(operand));
        if (preserveRegAttributes()) {
            ret.copyRegAttributesFrom(operand);
        }
        return ret;
    }

    @Specialization(guards = {"uAccess.supports(operand)", "isAbstractAtomicVector(operand)"}, limit = "getGenericVectorAccessSize()")
    protected RComplexVector doAbstractVector(RAbstractAtomicVector operand,
                    @Cached("operand.access()") VectorAccess uAccess) {
        return createResultVector(operand, uAccess);
    }

    @Specialization(replaces = "doAbstractVector", guards = "isAbstractAtomicVector(operand)")
    protected RComplexVector doAbstractVectorGeneric(RAbstractAtomicVector operand) {
        return doAbstractVector(operand, operand.slowPathAccess());
    }

    @Specialization
    protected RAbstractComplexVector doComplexVector(RAbstractComplexVector vector) {
        return vector;
    }

    @Specialization(guards = "uAccess.supports(list)", limit = "getVectorAccessCacheSize()")
    protected RComplexVector doList(RAbstractListVector list,
                    @Cached("list.access()") VectorAccess uAccess) {
        int length = list.getLength();
        double[] result = new double[length * 2];
        boolean seenNA = false;
        try (SequentialIterator sIter = uAccess.access(list, warningContext())) {
            while (uAccess.next(sIter)) {
                int i = sIter.getIndex() << 1;
                Object entry = uAccess.getListElement(sIter);
                if (entry instanceof RList) {
                    result[i] = RRuntime.DOUBLE_NA;
                    result[i + 1] = RRuntime.DOUBLE_NA;
                    seenNA = true;
                } else {
                    Object castEntry = castComplexRecursive(entry);
                    if (castEntry instanceof RComplex) {
                        RComplex value = (RComplex) castEntry;
                        result[i] = value.getRealPart();
                        result[i + 1] = value.getImaginaryPart();
                        seenNA = seenNA || RRuntime.isNA(value);
                    } else if (castEntry instanceof RComplexVector) {
                        RComplexVector complexVector = (RComplexVector) castEntry;
                        if (complexVector.getLength() == 1) {
                            RComplex value = complexVector.getDataAt(0);
                            result[i] = value.getRealPart();
                            result[i + 1] = value.getImaginaryPart();
                            seenNA = seenNA || RRuntime.isNA(value);
                        } else if (complexVector.getLength() == 0) {
                            result[i] = RRuntime.DOUBLE_NA;
                            result[i + 1] = RRuntime.DOUBLE_NA;
                            seenNA = true;
                        } else {
                            throw throwCannotCoerceListError("complex");
                        }
                    } else {
                        throw throwCannotCoerceListError("complex");
                    }
                }
            }
        }
        RComplexVector ret = factory().createComplexVector(result, !seenNA, getPreservedDimensions(list), getPreservedNames(list), null);
        if (preserveRegAttributes()) {
            ret.copyRegAttributesFrom(list);
        }
        return ret;
    }

    @Specialization(replaces = "doList")
    protected RComplexVector doListGeneric(RAbstractListVector list) {
        return doList(list, list.slowPathAccess());
    }

    @Specialization(guards = "!pairList.isLanguage()")
    protected RComplexVector doPairList(RPairList pairList) {
        return (RComplexVector) castComplexRecursive(pairList.toRList());
    }

    protected boolean isAbstractAtomicVector(RAbstractAtomicVector value) {
        return !(value instanceof RForeignVectorWrapper) && !(value instanceof RAbstractComplexVector);
    }

    @Specialization
    protected RAbstractComplexVector doForeignWrapper(RForeignBooleanWrapper operand) {
        return RClosures.createToComplexVector(operand, true);
    }

    @Specialization(guards = "operand.isForeignWrapper()")
    protected RAbstractComplexVector doForeignWrapper(RIntVector operand) {
        // Note: is it suboptimal, but OK if the foreign wrapper gets handled in other
        // specialization
        return RClosures.createToComplexVector(operand, true);
    }

    @Specialization
    protected RAbstractComplexVector doForeignWrapper(RForeignDoubleWrapper operand) {
        return RClosures.createToComplexVector(operand, true);
    }

    @Specialization
    protected RAbstractComplexVector doForeignWrapper(RForeignStringWrapper operand) {
        return RClosures.createToComplexVector(operand, true);
    }

    public static CastComplexNode create() {
        return CastComplexNodeGen.create(true, true, true);
    }

    public static CastComplexNode createForRFFI(boolean preserveNames, boolean preserveDimensions, boolean preserveAttributes) {
        return CastComplexNodeGen.create(preserveNames, preserveDimensions, preserveAttributes, true);
    }

    public static CastComplexNode createNonPreserving() {
        return CastComplexNodeGen.create(false, false, false);
    }

    protected int getVectorAccessCacheSize() {
        return DSLConfig.getVectorAccessCacheSize();
    }

    protected int getGenericVectorAccessSize() {
        return DSLConfig.getVectorAccessCacheSize() * 6;
    }
}
