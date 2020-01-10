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
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.ErrorContext;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RForeignBooleanWrapper;
import com.oracle.truffle.r.runtime.data.RForeignStringWrapper;
import com.oracle.truffle.r.runtime.data.RForeignVectorWrapper;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.closures.RClosures;
import com.oracle.truffle.r.runtime.data.model.RAbstractAtomicVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess.SequentialIterator;
import com.oracle.truffle.r.runtime.interop.ConvertForeignObjectNode;
import com.oracle.truffle.r.runtime.ops.na.NAProfile;

@ImportStatic({RRuntime.class, DSLConfig.class})
public abstract class CastDoubleNode extends CastDoubleBaseNode {

    protected CastDoubleNode(boolean preserveNames, boolean preserveDimensions, boolean preserveAttributes, boolean forRFFI, boolean withReuse, ErrorContext warningContext) {
        super(preserveNames, preserveDimensions, preserveAttributes, forRFFI, withReuse, warningContext);
    }

    protected CastDoubleNode(boolean preserveNames, boolean preserveDimensions, boolean preserveAttributes, boolean forRFFI, boolean withReuse) {
        super(preserveNames, preserveDimensions, preserveAttributes, forRFFI, withReuse);
    }

    protected CastDoubleNode(boolean preserveNames, boolean preserveDimensions, boolean preserveAttributes) {
        super(preserveNames, preserveDimensions, preserveAttributes, false, false);
    }

    @Child private CastDoubleNode recursiveCastDouble;

    private Object castDoubleRecursive(Object o) {
        if (recursiveCastDouble == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            recursiveCastDouble = insert(CastDoubleNodeGen.create(preserveNames(), preserveDimensions(), preserveRegAttributes(), false, reuseNonShared()));
        }
        return recursiveCastDouble.executeDouble(o);
    }

    private RDoubleVector vectorCopy(RAbstractContainer operand, double[] data, boolean isComplete) {
        RDoubleVector ret = factory().createDoubleVector(data, isComplete, getPreservedDimensions(operand), getPreservedNames(operand), getPreservedDimNames(operand));
        if (preserveRegAttributes()) {
            ret.copyRegAttributesFrom(operand);
        }
        return ret;
    }

    private RDoubleVector createResultVector(RAbstractAtomicVector operand, VectorAccess uAccess) {
        double[] idata = new double[operand.getLength()];
        try (VectorAccess.SequentialIterator sIter = uAccess.access(operand, warningContext())) {
            while (uAccess.next(sIter)) {
                idata[sIter.getIndex()] = uAccess.getDouble(sIter);
            }
        }
        return vectorCopy(operand, idata, uAccess.na.neverSeenNAOrNaN());
    }

    @Specialization(guards = {"uAccess.supports(x)", "noClosure(x)", "!isForeignIntVector(x)"}, limit = "getGenericVectorAccessCacheSize()")
    protected RAbstractDoubleVector doAbstractVector(RAbstractAtomicVector x,
                    @Cached("createClassProfile()") ValueProfile operandTypeProfile,
                    @Cached("x.access()") VectorAccess uAccess) {
        RAbstractAtomicVector operand = operandTypeProfile.profile(x);
        return createResultVector(operand, uAccess);
    }

    @Specialization(replaces = "doAbstractVector", guards = {"noClosure(x)", "!isForeignIntVector(x)"})
    protected RAbstractDoubleVector doAbstractVectorGeneric(RAbstractAtomicVector x,
                    @Cached("createClassProfile()") ValueProfile operandTypeProfile) {
        return doAbstractVector(x, operandTypeProfile, x.slowPathAccess());
    }

    @Specialization(guards = {"useClosure(x)", "!isForeignIntVector(x)"})
    public RAbstractDoubleVector doAbstractVectorClosure(RAbstractAtomicVector x,
                    @Cached("createClassProfile()") ValueProfile operandTypeProfile,
                    @Cached("create()") NAProfile naProfile) {
        RAbstractAtomicVector operand = operandTypeProfile.profile(x);
        return (RAbstractDoubleVector) castWithReuse(RType.Double, operand, naProfile.getConditionProfile());
    }

    @Specialization
    protected RAbstractDoubleVector doDoubleVector(RAbstractDoubleVector operand) {
        return operand;
    }

    @Specialization(guards = "uAccess.supports(list)", limit = "getVectorAccessCacheSize()")
    protected RDoubleVector doList(RAbstractListVector list,
                    @Cached("list.access()") VectorAccess uAccess) {
        int length = list.getLength();
        double[] result = new double[length];
        boolean seenNA = false;
        try (SequentialIterator sIter = uAccess.access(list, warningContext())) {
            while (uAccess.next(sIter)) {
                int i = sIter.getIndex();
                Object entry = uAccess.getListElement(sIter);
                if (entry instanceof RList) {
                    result[i] = RRuntime.DOUBLE_NA;
                    seenNA = true;
                } else {
                    Object castEntry = castDoubleRecursive(entry);
                    if (castEntry instanceof Double) {
                        double value = (Double) castEntry;
                        result[i] = value;
                        seenNA = seenNA || RRuntime.isNA(value);
                    } else if (castEntry instanceof RDoubleVector) {
                        RDoubleVector doubleVector = (RDoubleVector) castEntry;
                        if (doubleVector.getLength() == 1) {
                            double value = doubleVector.getDataAt(0);
                            result[i] = value;
                            seenNA = seenNA || RRuntime.isNA(value);
                        } else if (doubleVector.getLength() == 0) {
                            result[i] = RRuntime.DOUBLE_NA;
                            seenNA = true;
                        } else {
                            throw throwCannotCoerceListError("numeric");
                        }
                    } else {
                        throw throwCannotCoerceListError("numeric");
                    }
                }
            }
        }
        RDoubleVector ret = factory().createDoubleVector(result, !seenNA, getPreservedDimensions(list), getPreservedNames(list), null);
        if (preserveRegAttributes()) {
            ret.copyRegAttributesFrom(list);
        }
        return ret;
    }

    @Specialization(replaces = "doList")
    protected RDoubleVector doListGeneric(RAbstractListVector list) {
        return doList(list, list.slowPathAccess());
    }

    @Specialization(guards = "!pairList.isLanguage()")
    protected RDoubleVector doPairList(RPairList pairList) {
        return (RDoubleVector) castDoubleRecursive(pairList.toRList());
    }

    @Specialization(guards = "isForeignObject(obj)")
    protected RAbstractDoubleVector doForeignObject(TruffleObject obj,
                    @Cached("create()") ConvertForeignObjectNode convertForeign) {
        Object o = convertForeign.convert(obj);
        if (!RRuntime.isForeignObject(o)) {
            if (o instanceof RAbstractDoubleVector) {
                return (RAbstractDoubleVector) o;
            }
            o = castDoubleRecursive(o);
            if (o instanceof RAbstractDoubleVector) {
                return (RAbstractDoubleVector) o;
            }
        }
        throw error(RError.Message.CANNOT_COERCE_EXTERNAL_OBJECT_TO_VECTOR, "vector");
    }

    protected boolean isForeignWrapper(Object value) {
        return value instanceof RForeignVectorWrapper;
    }

    @Specialization
    protected RAbstractDoubleVector doForeignWrapper(RForeignBooleanWrapper operand) {
        return RClosures.createToDoubleVector(operand, true);
    }

    @Specialization(guards = "operand.isForeignWrapper()")
    protected RAbstractDoubleVector doForeignWrapper(RIntVector operand) {
        // Note: is it suboptimal, but OK if the foreign wrapper gets handled in other
        // specialization
        return RClosures.createToDoubleVector(operand, true);
    }

    @Specialization
    protected RAbstractDoubleVector doForeignWrapper(RForeignStringWrapper operand) {
        return RClosures.createToDoubleVector(operand, true);
    }

    public boolean isForeignIntVector(RAbstractAtomicVector operand) {
        return operand instanceof RIntVector && ((RIntVector) operand).isForeignWrapper();
    }

    public static CastDoubleNode create() {
        return CastDoubleNodeGen.create(true, true, true, false, false);
    }

    public static CastDoubleNode createWithReuse() {
        return CastDoubleNodeGen.create(true, true, true, false, true);
    }

    public static CastDoubleNode createForRFFI(boolean preserveNames, boolean preserveDimensions, boolean preserveAttributes) {
        return CastDoubleNodeGen.create(preserveNames, preserveDimensions, preserveAttributes, true, false);
    }

    public static CastDoubleNode createNonPreserving() {
        return CastDoubleNodeGen.create(false, false, false, false, false);
    }

    protected boolean useClosure(RAbstractAtomicVector x) {
        return useClosure() && !isForeignWrapper(x) && !(x instanceof RAbstractDoubleVector) && !(x instanceof RAbstractStringVector || x instanceof RAbstractComplexVector);
    }

    protected boolean noClosure(RAbstractAtomicVector x) {
        return !isForeignWrapper(x) && !(x instanceof RAbstractDoubleVector) && (!useClosure() || x instanceof RAbstractStringVector || x instanceof RAbstractComplexVector);
    }
}
