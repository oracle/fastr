/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2015, Purdue University
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */

package com.oracle.truffle.r.nodes.builtin.base;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.LoopConditionProfile;
import com.oracle.truffle.r.nodes.binary.CastTypeNode;
import com.oracle.truffle.r.nodes.binary.CastTypeNodeGen;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.unary.TypeofNode;
import com.oracle.truffle.r.nodes.unary.TypeofNodeGen;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.RBuiltinKind;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

public class BitwiseFunctions {

    public abstract static class BasicBitwise extends RBuiltinNode {

        private final BranchProfile errorProfile = BranchProfile.create();
        private final NACheck naCheckA = NACheck.create();
        private final NACheck naCheckB = NACheck.create();
        private final LoopConditionProfile loopProfile = LoopConditionProfile.createCountingProfile();

        @Child private CastTypeNode castTypeA = CastTypeNodeGen.create(null, null);
        @Child private CastTypeNode castTypeB = CastTypeNodeGen.create(null, null);
        @Child private TypeofNode typeofA = TypeofNodeGen.create();
        @Child private TypeofNode typeofB = TypeofNodeGen.create();

        protected enum Operation {
            AND("bitwAnd"),
            OR("bitwOr"),
            XOR("bitwXor"),
            NOT("bitNot"),
            SHIFTR("bitShiftR"),
            SHIFTL("bitShiftL");

            private final String name;

            Operation(String name) {
                this.name = name;
            }
        }

        protected Object basicBit(RAbstractVector a, RAbstractVector b, Operation op) {
            checkBasicBit(a, b, op);
            RAbstractIntVector aVec = (RAbstractIntVector) castTypeA.execute(a, RType.Integer);
            RAbstractIntVector bVec = (RAbstractIntVector) castTypeB.execute(b, RType.Integer);
            naCheckA.enable(aVec);
            naCheckB.enable(bVec);
            int aLen = aVec.getLength();
            int bLen = bVec.getLength();
            int ansSize = (aLen != 0 && bLen != 0) ? Math.max(aLen, bLen) : 0;
            int[] ans = new int[ansSize];
            boolean completeVector = true;
            loopProfile.profileCounted(ansSize);
            for (int i = 0; loopProfile.inject(i < ansSize); i++) {
                int aVal = aVec.getDataAt(i % aLen);
                int bVal = bVec.getDataAt(i % bLen);
                if (naCheckA.check(aVal) || naCheckB.check(bVal)) {
                    ans[i] = RRuntime.INT_NA;
                    completeVector = false;
                } else {
                    int v;
                    switch (op) {
                        case AND:
                            v = aVal & bVal;
                            break;
                        case OR:
                            v = aVal | bVal;
                            break;
                        case XOR:
                            v = aVal ^ bVal;
                            break;
                        case SHIFTR:
                            if (bVal > 31) {
                                v = RRuntime.INT_NA;
                            } else {
                                v = aVal >>> bVal;
                            }
                            break;
                        case SHIFTL:
                            if (bVal > 31) {
                                v = RRuntime.INT_NA;
                            } else {
                                v = aVal << bVal;
                            }
                            break;
                        default:
                            throw RInternalError.shouldNotReachHere();
                    }
                    ans[i] = v;
                    if (v == RRuntime.INT_NA) {
                        completeVector = false;
                    }
                }
            }

            return RDataFactory.createIntVector(ans, completeVector);
        }

        protected Object bitNot(RAbstractVector a) {
            RAbstractIntVector aVec = (RAbstractIntVector) castTypeA.execute(a, RType.Integer);
            int[] ans = new int[aVec.getLength()];
            for (int i = 0; i < aVec.getLength(); i++) {
                ans[i] = ~aVec.getDataAt(i);
            }
            return RDataFactory.createIntVector(ans, RDataFactory.COMPLETE_VECTOR);
        }

        protected Object makeNA(int length) {
            int[] na = new int[length];
            for (int i = 0; i < length; i++) {
                na[i] = RRuntime.INT_NA;
            }
            return RDataFactory.createIntVector(na, RDataFactory.INCOMPLETE_VECTOR);
        }

        protected void checkBasicBit(RAbstractVector a, RAbstractVector b, Operation op) {
            hasSameTypes(a, b);
            hasSupportedType(a, op);
        }

        protected void checkShiftOrNot(RAbstractVector a, Operation op) {
            hasSupportedType(a, op);
        }

        protected void hasSameTypes(RAbstractVector a, RAbstractVector b) {
            RType aType = typeofA.execute(a);
            RType bType = typeofB.execute(b);
            boolean aCorrectType = (aType == RType.Integer || aType == RType.Double) ? true : false;
            boolean bCorrectType = (bType == RType.Integer || bType == RType.Double) ? true : false;
            if ((aCorrectType && bCorrectType) || aType == bType) {
                return;
            } else {
                errorProfile.enter();
                throw RError.error(this, RError.Message.SAME_TYPE, "a", "b");
            }
        }

        protected void hasSupportedType(RAbstractVector a, Operation op) {
            if (!(a instanceof RAbstractIntVector) && !(a instanceof RAbstractDoubleVector)) {
                errorProfile.enter();
                String type = typeofA.execute(a).getName();
                throw RError.error(this, RError.Message.UNIMPLEMENTED_TYPE_IN_FUNCTION, type, op.name);
            }
        }

        protected boolean shiftByCharacter(RAbstractVector n) {
            return typeofB.execute(n) == RType.Character;
        }
    }

    @RBuiltin(name = "bitwiseAnd", kind = RBuiltinKind.INTERNAL, parameterNames = {"a", "b"})
    public abstract static class BitwiseAnd extends BasicBitwise {

        @Specialization
        protected Object bitwAnd(RAbstractVector a, RAbstractVector b) {
            controlVisibility();
            return basicBit(a, b, Operation.AND);
        }
    }

    @RBuiltin(name = "bitwiseOr", kind = RBuiltinKind.INTERNAL, parameterNames = {"a", "b"})
    public abstract static class BitwiseOr extends BasicBitwise {

        @Specialization
        protected Object bitwOr(RAbstractVector a, RAbstractVector b) {
            controlVisibility();
            return basicBit(a, b, Operation.OR);
        }
    }

    @RBuiltin(name = "bitwiseXor", kind = RBuiltinKind.INTERNAL, parameterNames = {"a", "b"})
    public abstract static class BitwiseXor extends BasicBitwise {

        @Specialization
        protected Object bitwXor(RAbstractVector a, RAbstractVector b) {
            controlVisibility();
            return basicBit(a, b, Operation.XOR);
        }
    }

    @RBuiltin(name = "bitwiseShiftR", kind = RBuiltinKind.INTERNAL, parameterNames = {"a", "n"})
    public abstract static class BitwiseShiftR extends BasicBitwise {

        @Specialization(guards = {"!shiftByCharacter(n)"})
        protected Object bitwShiftR(RAbstractVector a, RAbstractVector n) {
            controlVisibility();
            return basicBit(a, n, Operation.SHIFTR);
        }

        @Specialization(guards = {"shiftByCharacter(n)"})
        @SuppressWarnings("unused")
        protected Object bitwShiftRChar(RAbstractVector a, RAbstractVector n) {
            controlVisibility();
            checkShiftOrNot(a, Operation.SHIFTR);
            return makeNA(a.getLength());
        }
    }

    @RBuiltin(name = "bitwiseShiftL", kind = RBuiltinKind.INTERNAL, parameterNames = {"a", "n"})
    public abstract static class BitwiseShiftL extends BasicBitwise {

        @Specialization(guards = {"!shiftByCharacter(n)"})
        protected Object bitwShiftR(RAbstractVector a, RAbstractVector n) {
            controlVisibility();
            return basicBit(a, n, Operation.SHIFTL);
        }

        @Specialization(guards = {"shiftByCharacter(n)"})
        @SuppressWarnings("unused")
        protected Object bitwShiftRChar(RAbstractVector a, RAbstractVector n) {
            controlVisibility();
            checkShiftOrNot(a, Operation.SHIFTL);
            return makeNA(a.getLength());
        }
    }

    @RBuiltin(name = "bitwiseNot", kind = RBuiltinKind.INTERNAL, parameterNames = {"a"})
    public abstract static class BitwiseNot extends BasicBitwise {

        @Specialization
        protected Object bitwNot(RAbstractVector a) {
            controlVisibility();
            checkShiftOrNot(a, Operation.NOT);
            return bitNot(a);
        }
    }
}
