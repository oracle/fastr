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
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.RDeparse;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.ErrorContext;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RForeignBooleanWrapper;
import com.oracle.truffle.r.runtime.data.RForeignDoubleWrapper;
import com.oracle.truffle.r.runtime.data.RForeignVectorWrapper;
import com.oracle.truffle.r.runtime.data.RIntSeqVectorData;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RStringSequence;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.data.closures.RClosures;
import com.oracle.truffle.r.runtime.data.model.RAbstractAtomicVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;
import com.oracle.truffle.r.runtime.interop.ConvertForeignObjectNode;

@ImportStatic({RRuntime.class, DSLConfig.class})
public abstract class CastStringNode extends CastStringBaseNode {

    @Child private CastStringNode recursiveCastString;

    protected CastStringNode(boolean preserveNames, boolean preserveDimensions, boolean preserveAttributes) {
        this(preserveNames, preserveDimensions, preserveAttributes, false);
    }

    protected CastStringNode(boolean preserveNames, boolean preserveDimensions, boolean preserveAttributes, boolean forRFFI) {
        super(preserveNames, preserveDimensions, preserveAttributes, forRFFI);
    }

    protected CastStringNode(boolean preserveNames, boolean preserveDimensions, boolean preserveAttributes, boolean forRFFI, boolean useClosure, ErrorContext warningContext) {
        super(preserveNames, preserveDimensions, preserveAttributes, forRFFI, useClosure, warningContext);
    }

    public abstract Object executeString(int o);

    public abstract Object executeString(double o);

    public abstract Object executeString(byte o);

    public abstract Object executeString(Object o);

    private RStringVector vectorCopy(RAbstractContainer operand, String[] data) {
        RStringVector ret = factory().createStringVector(data, operand.isComplete(), getPreservedDimensions(operand), getPreservedNames(operand), getPreservedDimNames(operand));
        if (preserveRegAttributes()) {
            ret.copyRegAttributesFrom(operand);
        }
        return ret;
    }

    @Specialization
    protected RAbstractStringVector doStringVector(RAbstractStringVector vector) {
        return vector;
    }

    @Specialization(guards = "vector.isSequence()")
    protected RStringSequence doIntSequence(RIntVector vector) {
        RIntSeqVectorData seq = vector.getSequence();
        return factory().createStringSequence("", "", seq.getStart(), seq.getStride(), vector.getLength());
    }

    @Specialization(guards = {"uAccess.supports(operandIn)", "handleAsAtomic(operandIn)"}, limit = "getGenericVectorAccessCacheSize()")
    protected RStringVector doAbstractAtomicVector(RAbstractAtomicVector operandIn,
                    @Cached("createClassProfile()") ValueProfile operandProfile,
                    @Cached("operandIn.access()") VectorAccess uAccess) {
        RAbstractContainer operand = operandProfile.profile(operandIn);
        String[] sdata = new String[operand.getLength()];
        // conversions to character will not introduce new NAs,
        // but lets pass the warning context anyway
        try (VectorAccess.SequentialIterator sIter = uAccess.access(operand, warningContext())) {
            while (uAccess.next(sIter)) {
                int i = sIter.getIndex();
                sdata[i] = uAccess.getString(sIter);
            }
        }
        return vectorCopy(operand, sdata);
    }

    @Specialization(replaces = "doAbstractAtomicVector", guards = "handleAsAtomic(operandIn)")
    protected RStringVector doAbstractAtomicVectorGeneric(RAbstractAtomicVector operandIn,
                    @Cached("createClassProfile()") ValueProfile operandProfile) {
        return doAbstractAtomicVector(operandIn, operandProfile, operandIn.slowPathAccess());
    }

    @Specialization(guards = {"uAccess.supports(x)", "handleAsNonAtomic(x)"}, limit = "getGenericVectorAccessCacheSize()")
    protected RStringVector doNonAtomic(RAbstractContainer x,
                    @Cached("createClassProfile()") ValueProfile operandProfile,
                    @Cached("createBinaryProfile()") ConditionProfile isLanguageProfile,
                    @Cached("x.access()") VectorAccess uAccess) {
        RAbstractContainer operand = operandProfile.profile(x);
        String[] sdata = new String[operand.getLength()];
        try (VectorAccess.SequentialIterator sIter = uAccess.access(operand, warningContext())) {
            while (uAccess.next(sIter)) {
                int i = sIter.getIndex();
                Object o = uAccess.getListElement(sIter);
                if (isLanguageProfile.profile((o instanceof RPairList && ((RPairList) o).isLanguage()))) {
                    sdata[i] = RDeparse.deparse(o);
                } else {
                    sdata[i] = toString(o);
                }
            }
        }
        return vectorCopy(operand, sdata);
    }

    @Specialization(replaces = "doNonAtomic", guards = "handleAsNonAtomic(list)")
    protected RStringVector doNonAtomicGeneric(RAbstractContainer list,
                    @Cached("createClassProfile()") ValueProfile operandProfile,
                    @Cached("createBinaryProfile()") ConditionProfile isLanguageProfile) {
        return doNonAtomic(list, operandProfile, isLanguageProfile, list.slowPathAccess());
    }

    @Specialization(guards = "isForeignObject(obj)")
    protected RAbstractStringVector doForeignObject(TruffleObject obj,
                    @Cached("create()") ConvertForeignObjectNode convertForeign) {
        Object o = convertForeign.convert(obj);
        if (!RRuntime.isForeignObject(o)) {
            if (o instanceof RAbstractStringVector) {
                return (RAbstractStringVector) o;
            }
            o = castStringRecursive(o);
            if (o instanceof RAbstractStringVector) {
                return (RAbstractStringVector) o;
            }
        }
        throw error(RError.Message.CANNOT_COERCE_EXTERNAL_OBJECT_TO_VECTOR, "vector");
    }

    @Specialization
    protected String doRSymbol(RSymbol s) {
        return s.getName();
    }

    @Specialization
    protected RAbstractStringVector doForeignWrapper(RForeignBooleanWrapper operand) {
        return RClosures.createToStringVector(operand, true);
    }

    @Specialization(guards = "operand.isForeignWrapper()")
    protected RAbstractStringVector doForeignWrapper(RIntVector operand) {
        // Note: is it suboptimal, but OK if the foreign wrapper gets handled in other specialization
        return RClosures.createToStringVector(operand, true);
    }

    @Specialization
    protected RAbstractStringVector doForeignWrapper(RForeignDoubleWrapper operand) {
        return RClosures.createToStringVector(operand, true);
    }

    protected boolean isForeignWrapper(Object value) {
        return value instanceof RForeignVectorWrapper;
    }

    protected boolean isIntSequence(RAbstractContainer c) {
        return c instanceof RIntVector && ((RIntVector) c).isSequence();
    }

    protected boolean handleAsAtomic(RAbstractAtomicVector x) {
        return !isForeignWrapper(x) && !(isIntSequence(x) || x instanceof RAbstractStringVector);
    }

    protected boolean handleAsNonAtomic(RAbstractContainer x) {
        return !isForeignWrapper(x) && !(x instanceof RAbstractAtomicVector);
    }

    public static CastStringNode create() {
        return CastStringNodeGen.create(true, true, true);
    }

    public static CastStringNode createForRFFI(boolean preserveNames, boolean preserveDimensions, boolean preserveAttributes) {
        return CastStringNodeGen.create(preserveNames, preserveDimensions, preserveAttributes, true);
    }

    public static CastStringNode createNonPreserving() {
        return CastStringNodeGen.create(false, false, false);
    }

    private Object castStringRecursive(Object o) {
        if (recursiveCastString == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            recursiveCastString = insert(CastStringNodeGen.create(preserveNames(), preserveDimensions(), preserveRegAttributes()));
        }
        return recursiveCastString.executeString(o);
    }
}
