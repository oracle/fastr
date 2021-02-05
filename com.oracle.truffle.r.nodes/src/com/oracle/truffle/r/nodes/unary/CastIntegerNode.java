/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.ErrorContext;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RDoubleSeqVectorData;
import com.oracle.truffle.r.runtime.data.RDoubleVector;

import com.oracle.truffle.r.runtime.data.RForeignVectorWrapper;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.closures.RClosures;
import com.oracle.truffle.r.runtime.data.model.RAbstractAtomicVector;
import com.oracle.truffle.r.runtime.data.RComplexVector;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess.SequentialIterator;
import com.oracle.truffle.r.runtime.interop.ConvertForeignObjectNode;
import com.oracle.truffle.r.runtime.ops.na.NAProfile;

@ImportStatic({RRuntime.class, DSLConfig.class})
public abstract class CastIntegerNode extends CastIntegerBaseNode {

    protected CastIntegerNode(boolean preserveNames, boolean preserveDimensions, boolean preserveAttributes, boolean forRFFI, boolean useClosure, ErrorContext warningContext) {
        super(preserveNames, preserveDimensions, preserveAttributes, forRFFI, useClosure, warningContext);
    }

    protected CastIntegerNode(boolean preserveNames, boolean preserveDimensions, boolean preserveAttributes, boolean forRFFI, boolean useClosure) {
        super(preserveNames, preserveDimensions, preserveAttributes, forRFFI, useClosure, null);
    }

    protected CastIntegerNode(boolean preserveNames, boolean preserveDimensions, boolean preserveAttributes) {
        super(preserveNames, preserveDimensions, preserveAttributes, false, false, null);
    }

    public abstract Object executeInt(int o);

    public abstract Object executeInt(double o);

    public abstract Object executeInt(byte o);

    public abstract Object executeInt(Object o);

    @Specialization
    protected RIntVector doIntVector(RIntVector operand) {
        return operand;
    }

    @Specialization(guards = "isDoubleSequence(operand)")
    protected RIntVector doDoubleSequence(RDoubleVector operand) {
        // start and stride cannot be NA so no point checking
        RDoubleSeqVectorData seq = operand.getSequence();
        return factory().createIntSequence(RRuntime.double2intNoCheck(seq.getStart()), RRuntime.double2intNoCheck(seq.getStride()), seq.getLength());
    }

    private RIntVector vectorCopy(RAbstractVector operand, int[] idata, boolean isComplete) {
        RIntVector ret = factory().createIntVector(idata, isComplete, getPreservedDimensions(operand), getPreservedNames(operand), getPreservedDimNames(operand));
        if (preserveRegAttributes()) {
            ret.copyRegAttributesFrom(operand);
        }
        return ret;
    }

    private RIntVector createResultVector(RAbstractVector operand, VectorAccess uAccess) {
        int[] idata = new int[operand.getLength()];
        SequentialIterator sIter = uAccess.access(operand, getWarningContext());
        while (uAccess.next(sIter)) {
            idata[sIter.getIndex()] = uAccess.getInt(sIter);
        }
        return vectorCopy(operand, idata, uAccess.na.neverSeenNAOrNaN());
    }

    @Specialization(guards = {"uAccess.supports(operand)", "noClosure(operand)", "!isForeignVector(operand)", "!isDoubleSequence(operand)"}, limit = "getGenericVectorAccessCacheSize()")
    protected RIntVector doAbstractVector(RAbstractAtomicVector operand,
                    @Cached("operand.access()") VectorAccess uAccess) {
        return createResultVector(operand, uAccess);
    }

    @Specialization(replaces = "doAbstractVector", guards = {"noClosure(operand)", "!isForeignVector(operand)", "!isDoubleSequence(operand)"})
    protected RIntVector doAbstractVectorGeneric(RAbstractAtomicVector operand) {
        return doAbstractVector(operand, operand.slowPathAccess());
    }

    @Specialization(guards = {"useClosure(x)", "!isForeignVector(x)"})
    public RIntVector doAbstractVectorClosure(RAbstractAtomicVector x,
                    @Cached("createClassProfile()") ValueProfile operandTypeProfile,
                    @Cached("create()") NAProfile naProfile) {
        RAbstractAtomicVector operand = operandTypeProfile.profile(x);
        return (RIntVector) castWithReuse(RType.Integer, operand, naProfile.getConditionProfile());
    }

    @Specialization(guards = "uAccess.supports(list)", limit = "getVectorAccessCacheSize()")
    protected RIntVector doList(RAbstractListVector list,
                    @Cached("list.access()") VectorAccess uAccess) {
        int length = list.getLength();
        int[] result = new int[length];
        boolean seenNA = false;
        SequentialIterator sIter = uAccess.access(list, getWarningContext());
        while (uAccess.next(sIter)) {
            int i = sIter.getIndex();
            Object entry = uAccess.getListElement(sIter);
            if (entry instanceof RList) {
                result[i] = RRuntime.INT_NA;
                seenNA = true;
            } else {
                Object castEntry = castIntegerRecursive(entry);
                if (castEntry instanceof Integer) {
                    int value = (Integer) castEntry;
                    result[i] = value;
                    seenNA = seenNA || RRuntime.isNA(value);
                } else if (castEntry instanceof RIntVector) {
                    RIntVector intVector = (RIntVector) castEntry;
                    if (intVector.getLength() == 1) {
                        int value = intVector.getDataAt(0);
                        result[i] = value;
                        seenNA = seenNA || RRuntime.isNA(value);
                    } else if (intVector.getLength() == 0) {
                        result[i] = RRuntime.INT_NA;
                        seenNA = true;
                    } else {
                        throw throwCannotCoerceListError("integer");
                    }
                } else {
                    throw throwCannotCoerceListError("integer");
                }
            }
        }
        RIntVector ret = factory().createIntVector(result, !seenNA, getPreservedDimensions(list), getPreservedNames(list), null);
        if (preserveRegAttributes()) {
            ret.copyRegAttributesFrom(list);
        }
        return ret;
    }

    @Specialization(replaces = "doList")
    protected RIntVector doListGeneric(RAbstractListVector list) {
        return doList(list, list.slowPathAccess());
    }

    @Specialization(guards = "!pairList.isLanguage()")
    protected RIntVector doPairList(RPairList pairList) {
        return (RIntVector) castIntegerRecursive(pairList.toRList());
    }

    @Specialization(guards = "isForeignObject(obj)")
    protected RIntVector doForeignObject(TruffleObject obj,
                    @Cached("create()") ConvertForeignObjectNode convertForeign) {
        Object o = convertForeign.convert(obj);
        if (!RRuntime.isForeignObject(o)) {
            if (o instanceof RIntVector) {
                return (RIntVector) o;
            }
            o = castIntegerRecursive(o);
            if (o instanceof RIntVector) {
                return (RIntVector) o;
            }
        }
        throw error(RError.Message.CANNOT_COERCE_EXTERNAL_OBJECT_TO_VECTOR, "vector");
    }

    // TODO Should be type-variable and moved to CastNode
    @Specialization(guards = {"args.getLength() == 1", "isIntVector(args.getArgument(0))"})
    protected RIntVector doRArgsValuesAndNames(RArgsValuesAndNames args) {
        return (RIntVector) args.getArgument(0);
    }

    protected boolean isForeignWrapper(Object value) {
        return value instanceof RForeignVectorWrapper;
    }

    public boolean isForeignVector(RAbstractVector operand) {
        return RRuntime.hasVectorData(operand) && operand.isForeignWrapper();
    }

    @Specialization(guards = "operand.isForeignWrapper()")
    protected RIntVector doForeignWrapper(RLogicalVector operand) {
        return RClosures.createToIntVector(operand, true);
    }

    @Specialization(guards = "operand.isForeignWrapper()")
    protected RIntVector doForeignWrapper(RDoubleVector operand) {
        // Note: is it suboptimal, but OK if the foreign wrapper gets handled in other
        // specialization
        return RClosures.createToIntVector(operand, true);
    }

    @Specialization(guards = "operand.isForeignWrapper()")
    protected RIntVector doForeignWrapper(RStringVector operand) {
        return RClosures.createToIntVector(operand, true);
    }

    protected static boolean isIntVector(Object arg) {
        return arg instanceof RIntVector;
    }

    public static CastIntegerNode create() {
        return CastIntegerNodeGen.create(true, true, true, false, false);
    }

    public static CastIntegerNode createWithReuse() {
        return CastIntegerNodeGen.create(true, true, true, false, true);
    }

    public static CastIntegerNode createForRFFI(boolean preserveNames, boolean preserveDimensions, boolean preserveAttributes) {
        return CastIntegerNodeGen.create(preserveNames, preserveDimensions, preserveAttributes, true, false);
    }

    public static CastIntegerNode createNonPreserving() {
        return CastIntegerNodeGen.create(false, false, false, false, false);
    }

    public static CastIntegerNode createNonPreserving(ErrorContext warningContext) {
        return CastIntegerNodeGen.create(false, false, false, false, false, warningContext);
    }

    protected boolean useClosure(RAbstractAtomicVector x) {
        return useClosure() && !isForeignWrapper(x) && !(x instanceof RIntVector) && !(x instanceof RStringVector || x instanceof RComplexVector);
    }

    protected boolean noClosure(RAbstractAtomicVector x) {
        return !isForeignWrapper(x) && !isForeignVector(x) && !(x instanceof RIntVector) && (!useClosure() || x instanceof RStringVector || x instanceof RComplexVector);
    }

    protected boolean isDoubleSequence(RAbstractAtomicVector x) {
        return x instanceof RDoubleVector && x.isSequence();
    }
}
