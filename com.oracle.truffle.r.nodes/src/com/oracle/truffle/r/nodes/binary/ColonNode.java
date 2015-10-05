/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.binary;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.RASTUtils;
import com.oracle.truffle.r.nodes.binary.ColonNodeGen.ColonCastNodeGen;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.RDeparse.State;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.gnur.*;
import com.oracle.truffle.r.runtime.nodes.*;

@NodeChildren({@NodeChild("left"), @NodeChild("right")})
public abstract class ColonNode extends RNode implements RSyntaxNode, VisibilityController {

    private final BranchProfile naCheckErrorProfile = BranchProfile.create();

    public abstract RNode getLeft();

    public abstract RNode getRight();

    @CreateCast({"left", "right"})
    protected RNode createCast(RNode child) {
        ColonCastNode ccn = ColonCastNodeGen.create(child);
        ccn.assignSourceSection(child.asRSyntaxNode().getSourceSection());
        return ccn;
    }

    private void naCheck(boolean na) {
        if (na) {
            naCheckErrorProfile.enter();
            throw RError.error(this, RError.Message.NA_OR_NAN);
        }
    }

    @Specialization(guards = "left <= right")
    protected RIntSequence colonAscending(int left, int right) {
        controlVisibility();
        naCheck(RRuntime.isNA(left) || RRuntime.isNA(right));
        return RDataFactory.createAscendingRange(left, right);
    }

    @Specialization(guards = "left > right")
    protected RIntSequence colonDescending(int left, int right) {
        controlVisibility();
        naCheck(RRuntime.isNA(left) || RRuntime.isNA(right));
        return RDataFactory.createDescendingRange(left, right);
    }

    @Specialization(guards = "asDouble(left) <= right")
    protected RIntSequence colonAscending(int left, double right) {
        controlVisibility();
        naCheck(RRuntime.isNA(left) || RRuntime.isNAorNaN(right));
        return RDataFactory.createAscendingRange(left, (int) right);
    }

    @Specialization(guards = "asDouble(left) > right")
    protected RIntSequence colonDescending(int left, double right) {
        controlVisibility();
        naCheck(RRuntime.isNA(left) || RRuntime.isNAorNaN(right));
        return RDataFactory.createDescendingRange(left, (int) right);
    }

    @Specialization(guards = "left <= asDouble(right)")
    protected RDoubleSequence colonAscending(double left, int right) {
        controlVisibility();
        naCheck(RRuntime.isNAorNaN(left) || RRuntime.isNA(right));
        return RDataFactory.createAscendingRange(left, right);
    }

    @Specialization(guards = "left > asDouble(right)")
    protected RDoubleSequence colonDescending(double left, int right) {
        controlVisibility();
        naCheck(RRuntime.isNAorNaN(left) || RRuntime.isNA(right));
        return RDataFactory.createDescendingRange(left, right);
    }

    @Specialization(guards = "left <= right")
    protected RDoubleSequence colonAscending(double left, double right) {
        controlVisibility();
        naCheck(RRuntime.isNAorNaN(left) || RRuntime.isNAorNaN(right));
        return RDataFactory.createAscendingRange(left, right);
    }

    @Specialization(guards = "left > right")
    protected RDoubleSequence colonDescending(double left, double right) {
        controlVisibility();
        naCheck(RRuntime.isNAorNaN(left) || RRuntime.isNAorNaN(right));
        return RDataFactory.createDescendingRange(left, right);
    }

    public static ColonNode create(SourceSection src, RNode left, RNode right) {
        ColonNode cn = ColonNodeGen.create(left, right);
        cn.assignSourceSection(src);
        return cn;
    }

    @Override
    public void deparseImpl(RDeparse.State state) {
        state.startNodeDeparse(this);
        getLeft().deparse(state);
        state.append(':');
        getRight().deparse(state);
        state.endNodeDeparse(this);
    }

    @Override
    public void serializeImpl(RSerialize.State state) {
        state.setAsBuiltin(":");
        state.openPairList(SEXPTYPE.LISTSXP);
        state.serializeNodeSetCar(getLeft());
        state.openPairList(SEXPTYPE.LISTSXP);
        state.serializeNodeSetCar(getRight());
        state.linkPairList(2);
        state.setCdr(state.closePairList());
    }

    @Override
    public RSyntaxNode substituteImpl(REnvironment env) {
        return create(null, getLeft().substitute(env).asRNode(), getRight().substitute(env).asRNode());
    }

    public int getRlengthImpl() {
        return 3;
    }

    @Override
    public Object getRelementImpl(int index) {
        switch (index) {
            case 0:
                return RDataFactory.createSymbol(":");
            case 1:
                return RASTUtils.createLanguageElement(getLeft());
            case 2:
                return RASTUtils.createLanguageElement(getRight());
            default:
                throw RInternalError.shouldNotReachHere();
        }
    }

    @Override
    public boolean getRequalsImpl(RSyntaxNode other) {
        throw RInternalError.unimplemented();
    }

    protected static double asDouble(int intValue) {
        return intValue;
    }

    @NodeChild("operand")
    public abstract static class ColonCastNode extends RNode implements RSyntaxNode {

        private final ConditionProfile lengthGreaterOne = ConditionProfile.createBinaryProfile();

        public abstract RNode getOperand();

        @Specialization(guards = "isIntValue(operand)")
        protected int doDoubleToInt(double operand) {
            return (int) operand;
        }

        @Specialization(guards = "!isIntValue(operand)")
        protected double doDouble(double operand) {
            return operand;
        }

        @Specialization
        protected int doSequence(RIntSequence sequence) {
            if (lengthGreaterOne.profile(sequence.getLength() > 1)) {
                RError.warning(this, RError.Message.ONLY_FIRST_USED, sequence.getLength());
            }
            return sequence.getStart();
        }

        @Specialization
        protected int doSequence(RIntVector vector) {
            if (lengthGreaterOne.profile(vector.getLength() > 1)) {
                RError.warning(this, RError.Message.ONLY_FIRST_USED, vector.getLength());
            }
            return vector.getDataAt(0);
        }

        @Specialization(guards = "isFirstIntValue(vector)")
        protected int doDoubleVectorFirstIntValue(RDoubleVector vector) {
            if (lengthGreaterOne.profile(vector.getLength() > 1)) {
                RError.warning(this, RError.Message.ONLY_FIRST_USED, vector.getLength());
            }
            return (int) vector.getDataAt(0);
        }

        @Specialization(guards = "!isFirstIntValue(vector)")
        protected double doDoubleVector(RDoubleVector vector) {
            if (lengthGreaterOne.profile(vector.getLength() > 1)) {
                RError.warning(this, RError.Message.ONLY_FIRST_USED, vector.getLength());
            }
            return vector.getDataAt(0);
        }

        @Specialization
        protected int doInt(int operand) {
            return operand;
        }

        @Specialization
        protected int doBoolean(byte operand) {
            return RRuntime.logical2int(operand);
        }

        @Specialization
        protected int doString(RAbstractStringVector vector) {
            if (lengthGreaterOne.profile(vector.getLength() > 1)) {
                RError.warning(this, RError.Message.ONLY_FIRST_USED, vector.getLength());
            }
            String val = vector.getDataAt(0);
            if (RRuntime.isNA(val)) {
                throw RError.error(this, RError.Message.NA_OR_NAN);
            }
            // TODO it might be a double or complex string
            int result = RRuntime.string2intNoCheck(val);
            if (RRuntime.isNA(result)) {
                RError.warning(this, RError.Message.NA_INTRODUCED_COERCION);
                throw RError.error(this, RError.Message.NA_OR_NAN);
            }
            return result;
        }

        protected static boolean isIntValue(double d) {
            return (((int) d)) == d;
        }

        protected static boolean isFirstIntValue(RDoubleVector d) {
            return (((int) d.getDataAt(0))) == d.getDataAt(0);
        }

        @Override
        public void deparseImpl(State state) {
            state.startNodeDeparse(this);
            getOperand().deparse(state);
            state.endNodeDeparse(this);
        }

        @Override
        public void serializeImpl(RSerialize.State state) {
            getOperand().serialize(state);
        }

        @Override
        public RSyntaxNode substituteImpl(REnvironment env) {
            return getOperand().substitute(env);
        }

        public int getRlengthImpl() {
            return getOperand().getRLength();
        }

        @Override
        public Object getRelementImpl(int index) {
            return getOperand().getRelement(index);
        }

        @Override
        public boolean getRequalsImpl(RSyntaxNode other) {
            return getOperand().getRequals(other);
        }
    }
}
