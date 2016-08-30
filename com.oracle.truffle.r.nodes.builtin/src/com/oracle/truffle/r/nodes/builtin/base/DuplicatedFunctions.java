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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.abstractVectorValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.emptyList;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.binary.CastTypeNode;
import com.oracle.truffle.r.nodes.binary.CastTypeNodeGen;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.unary.TypeofNode;
import com.oracle.truffle.r.nodes.unary.TypeofNodeGen;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.RTypedValue;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.nodes.DuplicationHelper;

public class DuplicatedFunctions {

    protected abstract static class Adapter extends RBuiltinNode {
        @Child protected CastTypeNode castTypeNode;
        @Child protected TypeofNode typeof;

        private final ConditionProfile incomparable = ConditionProfile.createBinaryProfile();

        protected void casts(CastBuilder casts) {
            // these are similar to those in DuplicatedFunctions.java
            casts.arg("x").mapNull(emptyList()).mustBe(abstractVectorValue(), RError.SHOW_CALLER,
                            RError.Message.APPLIES_TO_VECTORS,
                            "duplicated()").asVector();
            // not much more can be done for incomparables as it is either a vector of incomparable
            // values or a (single) logical value
            casts.arg("incomparables").asVector(true);
            casts.arg("fromLast").asLogicalVector().findFirst(RRuntime.LOGICAL_FALSE);
        }

        protected boolean isIncomparable(RAbstractVector incomparables) {
            if (incomparable.profile(incomparables.getLength() == 1 && incomparables instanceof RLogicalVector && ((RAbstractLogicalVector) incomparables).getDataAt(0) == RRuntime.LOGICAL_FALSE)) {
                return false;
            } else {
                return true;
            }
        }

        protected boolean notAbstractVector(Object o) {
            return !(o instanceof RAbstractVector);
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

    @RBuiltin(name = "duplicated", kind = INTERNAL, parameterNames = {"x", "incomparables", "fromLast", "nmax"}, behavior = PURE)
    public abstract static class Duplicated extends Adapter {

        @Override
        protected void createCasts(CastBuilder casts) {
            casts(casts);
            // currently not supported and not tested, but NA is a correct value (the same for empty
            // vectors) whereas 0 is not (throws an error)
            casts.arg("nmax").asIntegerVector().findFirst(RRuntime.INT_NA);
        }

        @TruffleBoundary
        protected static RLogicalVector analyzeAndCreateResult(RAbstractVector x, RAbstractVector incomparables, byte fromLast) {
            DuplicationHelper ds = DuplicationHelper.analyze(x, incomparables, false, RRuntime.fromLogical(fromLast));
            return RDataFactory.createLogicalVector(ds.getDupVec(), RDataFactory.COMPLETE_VECTOR);
        }

        @Specialization(guards = {"!isIncomparable(incomparables)", "!empty(x)"})
        protected RLogicalVector duplicatedFalseIncomparables(RAbstractVector x, @SuppressWarnings("unused") RAbstractVector incomparables, byte fromLast, @SuppressWarnings("unused") int nmax) {
            return analyzeAndCreateResult(x, null, fromLast);
        }

        @Specialization(guards = {"isIncomparable(incomparables)", "!empty(x)"})
        protected RLogicalVector duplicatedTrueIncomparables(RAbstractVector x, RAbstractVector incomparables, byte fromLast, @SuppressWarnings("unused") int nmax) {
            initChildren();
            RType xType = typeof.execute(x);
            RAbstractVector vector = (RAbstractVector) (castTypeNode.execute(incomparables, xType));
            return analyzeAndCreateResult(x, vector, fromLast);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"notAbstractVector(incomparables)", "!empty(x)"})
        protected RLogicalVector duplicatedTrueIncomparables(RAbstractVector x, Object incomparables, byte fromLast, int nmax) {
            initChildren();
            RType xType = typeof.execute(x);
            // TODO: this is not quite correct, as passing expression generated some obscure error
            // message, but is it worth fixing
            throw RError.error(RError.SHOW_CALLER, RError.Message.CANNOT_COERCE, ((RTypedValue) incomparables).getRType().getName(), xType.getName());
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "empty(x)")
        protected RLogicalVector duplicatedEmpty(RAbstractVector x, Object incomparables, byte fromLast, int nmax) {
            return RDataFactory.createEmptyLogicalVector();
        }
    }

    @RBuiltin(name = "anyDuplicated", kind = INTERNAL, parameterNames = {"x", "incomparables", "fromLast"}, behavior = PURE)
    public abstract static class AnyDuplicated extends Adapter {

        @Override
        protected void createCasts(CastBuilder casts) {
            casts(casts);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isIncomparable(incomparables)", "!empty(x)"})
        protected int anyDuplicatedFalseIncomparables(RAbstractVector x, RAbstractVector incomparables, byte fromLast) {
            return DuplicationHelper.analyze(x, null, true, RRuntime.fromLogical(fromLast)).getIndex();
        }

        @Specialization(guards = {"isIncomparable(incomparables)", "!empty(x)"})
        protected int anyDuplicatedTrueIncomparables(RAbstractVector x, RAbstractVector incomparables, byte fromLast) {
            initChildren();
            RType xType = typeof.execute(x);
            return DuplicationHelper.analyze(x, (RAbstractVector) (castTypeNode.execute(incomparables, xType)), true, RRuntime.fromLogical(fromLast)).getIndex();
        }

        @Specialization(guards = {"notAbstractVector(incomparables)", "!empty(x)"})
        protected int anyDuplicatedTrueIncomparables(RAbstractVector x, Object incomparables, @SuppressWarnings("unused") byte fromLast) {
            initChildren();
            RType xType = typeof.execute(x);
            // TODO: this is not quite correct, as passing expression generated some obscure error
            // message, but is it worth fixing
            throw RError.error(RError.SHOW_CALLER, RError.Message.CANNOT_COERCE, ((RTypedValue) incomparables).getRType().getName(), xType.getName());
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "empty(x)")
        protected int anyDuplicatedEmpty(RAbstractVector x, Object incomparables, byte fromLast) {
            return 0;
        }

    }
}
