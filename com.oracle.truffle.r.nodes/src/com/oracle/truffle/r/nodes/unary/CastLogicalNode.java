/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Arrays;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.r.nodes.helpers.InheritsCheckNode;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.ErrorContext;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.model.RAbstractAtomicVector;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess.SequentialIterator;
import com.oracle.truffle.r.runtime.interop.ConvertForeignObjectNode;

@ImportStatic({RRuntime.class, DSLConfig.class})
public abstract class CastLogicalNode extends CastLogicalBaseNode {

    @Child private CastLogicalNode recursiveCastLogical;
    @Child private InheritsCheckNode inheritsFactorCheck;

    protected CastLogicalNode(boolean preserveNames, boolean preserveDimensions, boolean preserveAttributes) {
        super(preserveNames, preserveDimensions, preserveAttributes);
    }

    protected CastLogicalNode(boolean preserveNames, boolean preserveDimensions, boolean preserveAttributes, boolean forRFFI) {
        super(preserveNames, preserveDimensions, preserveAttributes, forRFFI);
    }

    protected CastLogicalNode(boolean preserveNames, boolean preserveDimensions, boolean preserveAttributes, boolean forRFFI, boolean useClosure, ErrorContext warningContext) {
        super(preserveNames, preserveDimensions, preserveAttributes, forRFFI, useClosure, warningContext);
    }

    protected Object castLogicalRecursive(Object o) {
        if (recursiveCastLogical == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            recursiveCastLogical = insert(CastLogicalNodeGen.create(preserveNames(), preserveDimensions(), preserveRegAttributes()));
        }
        return recursiveCastLogical.execute(o);
    }

    @Specialization
    protected RNull doNull(@SuppressWarnings("unused") RNull operand) {
        return RNull.instance;
    }

    private RLogicalVector vectorCopy(RAbstractVector operand, byte[] bdata, boolean isComplete) {
        RLogicalVector ret = factory().createLogicalVector(bdata, isComplete, getPreservedDimensions(operand), getPreservedNames(operand), getPreservedDimNames(operand));
        if (preserveRegAttributes()) {
            ret.copyRegAttributesFrom(operand);
        }
        return ret;
    }

    private RLogicalVector createResultVector(RAbstractVector operand, VectorAccess uAccess) {
        byte[] bdata = new byte[operand.getLength()];
        try (SequentialIterator sIter = uAccess.access(operand, warningContext())) {
            while (uAccess.next(sIter)) {
                bdata[sIter.getIndex()] = uAccess.getLogical(sIter);
            }
        }
        return vectorCopy(operand, bdata, uAccess.na.neverSeenNAOrNaN());
    }

    @Specialization
    protected RLogicalVector doLogicalVector(RLogicalVector operand) {
        return operand;
    }

    @Specialization(guards = {"uAccess.supports(operand)", "useVectorAccess(operand)"}, limit = "getGenericVectorAccessCacheSize()")
    protected RLogicalVector doAbstractVector(RAbstractAtomicVector operand,
                    @Cached("operand.access()") VectorAccess uAccess) {
        return createResultVector(operand, uAccess);
    }

    @Specialization(replaces = "doAbstractVector", guards = {"useVectorAccess(operand)"})
    protected RLogicalVector doAbstractVectorGeneric(RAbstractAtomicVector operand) {
        return createResultVector(operand, operand.slowPathAccess());
    }

    @Specialization(guards = "isFactor(factor)")
    protected RLogicalVector asLogical(RIntVector factor) {
        byte[] data = new byte[factor.getLength()];
        Arrays.fill(data, RRuntime.LOGICAL_NA);
        return factory().createLogicalVector(data, RDataFactory.INCOMPLETE_VECTOR);
    }

    @Specialization(guards = "uAccess.supports(list)", limit = "getVectorAccessCacheSize()")
    protected RLogicalVector doList(RAbstractListVector list,
                    @Cached("list.access()") VectorAccess uAccess) {
        int length = list.getLength();
        byte[] result = new byte[length];
        boolean seenNA = false;
        try (SequentialIterator sIter = uAccess.access(list, warningContext())) {
            while (uAccess.next(sIter)) {
                int i = sIter.getIndex();
                Object entry = uAccess.getListElement(sIter);
                if (entry instanceof RList) {
                    result[i] = RRuntime.LOGICAL_NA;
                    seenNA = true;
                } else {
                    Object castEntry = castLogicalRecursive(entry);
                    if (castEntry instanceof Byte) {
                        byte value = (Byte) castEntry;
                        result[i] = value;
                        seenNA = seenNA || RRuntime.isNA(value);
                    } else if (castEntry instanceof RLogicalVector) {
                        RLogicalVector logicalVector = (RLogicalVector) castEntry;
                        if (logicalVector.getLength() == 1) {
                            byte value = logicalVector.getDataAt(0);
                            result[i] = value;
                            seenNA = seenNA || RRuntime.isNA(value);
                        } else if (logicalVector.getLength() == 0) {
                            result[i] = RRuntime.LOGICAL_NA;
                            seenNA = true;
                        } else {
                            throw throwCannotCoerceListError("logical");
                        }
                    } else {
                        throw throwCannotCoerceListError("logical");
                    }
                }
            }
        }
        RLogicalVector ret = factory().createLogicalVector(result, !seenNA, getPreservedDimensions(list), getPreservedNames(list), null);
        if (preserveRegAttributes()) {
            ret.copyRegAttributesFrom(list);
        }
        return ret;
    }

    @Specialization(replaces = "doList")
    protected RLogicalVector doListGeneric(RAbstractListVector list) {
        return doList(list, list.slowPathAccess());
    }

    @Specialization(guards = "!pairList.isLanguage()")
    protected RLogicalVector doPairList(RPairList pairList) {
        return (RLogicalVector) castLogicalRecursive(pairList.toRList());
    }

    @Specialization
    protected RMissing doMissing(RMissing missing) {
        return missing;
    }

    @Specialization(guards = "isForeignObject(obj)")
    protected RLogicalVector doForeignObject(TruffleObject obj,
                    @Cached("create()") ConvertForeignObjectNode convertForeign) {
        Object o = convertForeign.convert(obj);
        if (!RRuntime.isForeignObject(o)) {
            if (o instanceof RLogicalVector) {
                return (RLogicalVector) o;
            }
            o = castLogicalRecursive(o);
            if (o instanceof RLogicalVector) {
                return (RLogicalVector) o;
            }
        }
        throw error(RError.Message.CANNOT_COERCE_EXTERNAL_OBJECT_TO_VECTOR, "vector");
    }

    public static CastLogicalNode create() {
        return CastLogicalNodeGen.create(true, true, true);
    }

    public static CastLogicalNode createForRFFI(boolean preserveNames, boolean preserveDimensions, boolean preserveAttributes) {
        return CastLogicalNodeGen.create(preserveNames, preserveDimensions, preserveAttributes, true);
    }

    public static CastLogicalNode createNonPreserving() {
        return CastLogicalNodeGen.create(false, false, false);
    }

    protected boolean isFactor(RIntVector o) {
        if (inheritsFactorCheck == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            inheritsFactorCheck = insert(InheritsCheckNode.create(RRuntime.CLASS_FACTOR));
        }
        return inheritsFactorCheck.execute(o);
    }

    protected boolean useVectorAccess(RAbstractAtomicVector x) {
        if (x instanceof RIntVector && isFactor((RIntVector) x)) {
            return false;
        }
        return !(x instanceof RLogicalVector);
    }
}
