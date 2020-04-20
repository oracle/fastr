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
import com.oracle.truffle.r.runtime.data.RComplexVector;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RRaw;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess.SequentialIterator;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

@ImportStatic(DSLConfig.class)
public abstract class CastIntegerBaseNode extends CastBaseNode {

    protected final NACheck naCheck = NACheck.create();

    @Child private CastIntegerNode recursiveCastInteger;

    protected CastIntegerBaseNode(boolean preserveNames, boolean preserveDimensions, boolean preserveAttributes, boolean forRFFI, boolean useClosure, ErrorContext warningContext) {
        super(preserveNames, preserveDimensions, preserveAttributes, forRFFI, useClosure, warningContext);
    }

    protected CastIntegerBaseNode(boolean preserveNames, boolean preserveDimensions, boolean preserveAttributes, boolean forRFFI, boolean useClosure) {
        super(preserveNames, preserveDimensions, preserveAttributes, forRFFI, useClosure, null);
    }

    protected CastIntegerBaseNode(boolean preserveNames, boolean preserveDimensions, boolean preserveAttributes) {
        super(preserveNames, preserveDimensions, preserveAttributes, false, false, null);
    }

    @Override
    protected final RType getTargetType() {
        return RType.Integer;
    }

    protected Object castIntegerRecursive(Object o) {
        if (recursiveCastInteger == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            recursiveCastInteger = insert(CastIntegerNodeGen.create(preserveNames(), preserveDimensions(), preserveRegAttributes(), false, reuseNonShared()));
        }
        return recursiveCastInteger.executeInt(o);
    }

    @Specialization
    protected RNull doNull(@SuppressWarnings("unused") RNull operand) {
        return RNull.instance;
    }

    @Specialization
    protected RMissing doMissing(RMissing operand) {
        return operand;
    }

    @Specialization
    protected int doInt(int operand) {
        return operand;
    }

    @Specialization
    protected int doDouble(double operand) {
        naCheck.enable(operand);
        if (naCheck.checkNAorNaN(operand)) {
            return RRuntime.INT_NA;
        }
        // either a warning context was preset or
        // we pass this node to get the context if it becomes necessary
        int result = naCheck.convertDoubleToInt(operand);
        naCheck.enable(result);
        if (naCheck.check(result)) {
            RBaseNode ctx = warningContext();
            warning(ctx != null ? ctx : getErrorContext(), RError.Message.NA_INTRODUCED_COERCION_INT);
        }
        return result;
    }

    @Specialization(guards = "uAccess.supports(operand)", limit = "getVectorAccessCacheSize()")
    protected int doComplex(@SuppressWarnings("unused") RComplex operand,
                    @Cached("getVector(operand)") RComplexVector vector,
                    @Cached("vector.access()") VectorAccess uAccess) {
        try (SequentialIterator sIter = uAccess.access(vector, getWarningContext())) {
            uAccess.next(sIter);
            return uAccess.getInt(sIter);
        }
    }

    @Specialization(replaces = "doComplex")
    protected int doComplexGeneric(RComplex operand) {
        RComplexVector vector = getVector(operand);
        return doComplex(operand, vector, vector.slowPathAccess());
    }

    protected RComplexVector getVector(RComplex c) {
        return RDataFactory.createComplexVectorFromScalar(c);
    }

    @Specialization
    protected int doCharacter(String operand,
                    @Cached("createBinaryProfile()") ConditionProfile emptyStringProfile) {
        naCheck.enable(operand);
        if (naCheck.check(operand) || emptyStringProfile.profile(operand.isEmpty())) {
            return RRuntime.INT_NA;
        }
        double d = naCheck.convertStringToDouble(operand);
        naCheck.enable(d);
        if (naCheck.checkNAorNaN(d)) {
            if (naCheck.check(d)) {
                warning(warningContext(), RError.Message.NA_INTRODUCED_COERCION);
            }
            return RRuntime.INT_NA;
        }
        int result = naCheck.convertDoubleToInt(d);
        naCheck.enable(result);
        if (naCheck.check(result)) {
            warning(warningContext(), RError.Message.NA_INTRODUCED_COERCION_INT);
        }
        return result;
    }

    @Specialization
    protected int doBoolean(byte operand) {
        naCheck.enable(operand);
        return naCheck.convertLogicalToInt(operand);
    }

    @Specialization
    protected int doRaw(RRaw operand) {
        return RRuntime.raw2int(operand.getValue());
    }

    protected RBaseNode getWarningContext() {
        RBaseNode ctx = warningContext();
        return ctx != null ? ctx : this;
    }
}
