/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */

package com.oracle.truffle.r.nodes.builtin.base;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.binary.CastTypeNode;
import com.oracle.truffle.r.nodes.binary.CastTypeNodeGen;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.unary.TypeofNode;
import com.oracle.truffle.r.nodes.unary.TypeofNodeGen;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.builtins.RBuiltinKind;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.nodes.DuplicationHelper;

public class DuplicatedFunctions {

    protected abstract static class Adapter extends RBuiltinNode {
        @Child protected CastTypeNode castTypeNode;
        @Child protected TypeofNode typeof;

        protected boolean isIncomparable(byte incomparables) {
            return incomparables == RRuntime.LOGICAL_TRUE;
        }

        protected boolean empty(RAbstractContainer x) {
            return x.getLength() == 0;
        }

        protected void initChildren() {
            if (castTypeNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                castTypeNode = insert(CastTypeNodeGen.create(null, null));
                typeof = insert(TypeofNodeGen.create());
            }
        }
    }

    @RBuiltin(name = "duplicated", kind = RBuiltinKind.INTERNAL, parameterNames = {"x", "imcomparables", "fromLast", "nmax"})
    public abstract static class Duplicated extends Adapter {

        @Override
        protected void createCasts(CastBuilder casts) {
            casts.toLogical(2).toInteger(3);
        }

        @TruffleBoundary
        protected static RLogicalVector analyzeAndCreateResult(RAbstractContainer x, RAbstractContainer incomparables, byte fromLast) {
            DuplicationHelper ds = DuplicationHelper.analyze(x, incomparables, false, RRuntime.fromLogical(fromLast));
            return RDataFactory.createLogicalVector(ds.getDupVec(), RDataFactory.COMPLETE_VECTOR);
        }

        @SuppressWarnings("unused")
        @Specialization
        protected RLogicalVector duplicated(RNull x, Object incomparables, byte fromLast, int nmax) {
            return RDataFactory.createEmptyLogicalVector();
        }

        @Specialization(guards = {"!isIncomparable(incomparables)", "!empty(x)"})
        protected RLogicalVector duplicatedFalseIncomparables(RAbstractVector x, @SuppressWarnings("unused") byte incomparables, byte fromLast, @SuppressWarnings("unused") int nmax) {
            return analyzeAndCreateResult(x, null, fromLast);
        }

        @Specialization(guards = {"isIncomparable(incomparables)", "!empty(x)"})
        protected RLogicalVector duplicatedTrueIncomparables(RAbstractVector x, byte incomparables, byte fromLast, @SuppressWarnings("unused") int nmax) {
            initChildren();
            RType xType = typeof.execute(x);
            RAbstractVector vector = (RAbstractVector) (castTypeNode.execute(incomparables, xType));
            return analyzeAndCreateResult(x, vector, fromLast);
        }

        @Specialization(guards = {"!empty(x)"})
        protected RLogicalVector duplicated(RAbstractContainer x, RAbstractContainer incomparables, byte fromLast, @SuppressWarnings("unused") int nmax) {
            initChildren();
            RType xType = typeof.execute(x);
            return analyzeAndCreateResult(x, (RAbstractContainer) (castTypeNode.execute(incomparables, xType)), fromLast);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "empty(x)")
        protected RLogicalVector duplicatedEmpty(RAbstractContainer x, RAbstractContainer incomparables, byte fromLast, int nmax) {
            return RDataFactory.createLogicalVector(0);
        }
    }

    @RBuiltin(name = "anyDuplicated", kind = RBuiltinKind.INTERNAL, parameterNames = {"x", "imcomparables", "fromLast"})
    public abstract static class AnyDuplicated extends Adapter {

        @Override
        protected void createCasts(CastBuilder casts) {
            casts.toLogical(2);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isIncomparable(incomparables)", "!empty(x)"})
        protected int anyDuplicatedFalseIncomparables(RAbstractVector x, byte incomparables, byte fromLast) {
            return DuplicationHelper.analyze(x, null, true, RRuntime.fromLogical(fromLast)).getIndex();
        }

        @Specialization(guards = {"isIncomparable(incomparables)", "!empty(x)"})
        protected int anyDuplicatedTrueIncomparables(RAbstractVector x, byte incomparables, byte fromLast) {
            initChildren();
            RType xType = typeof.execute(x);
            RAbstractVector vector = (RAbstractVector) (castTypeNode.execute(incomparables, xType));
            return DuplicationHelper.analyze(x, vector, true, RRuntime.fromLogical(fromLast)).getIndex();
        }

        @Specialization(guards = {"!empty(x)"})
        protected int anyDuplicated(RAbstractContainer x, RAbstractContainer incomparables, byte fromLast) {
            initChildren();
            RType xType = typeof.execute(x);
            return DuplicationHelper.analyze(x, (RAbstractContainer) (castTypeNode.execute(incomparables, xType)), true, RRuntime.fromLogical(fromLast)).getIndex();
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "empty(x)")
        protected int anyDuplicatedEmpty(RAbstractContainer x, RAbstractContainer incomparables, byte fromLast) {
            return 0;
        }

        @SuppressWarnings("unused")
        @Specialization
        protected int anyDuplicatedNull(RNull x, RAbstractContainer incomparables, byte fromLast) {
            return 0;
        }
    }
}
