/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2015, Purdue University
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates
 *
 * All rights reserved.
 */

package com.oracle.truffle.r.nodes.builtin.base;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.binary.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

public class BitwiseFunctions {

    public abstract static class BasicBitwise extends RBuiltinNode {

        private final BranchProfile errorProfile = BranchProfile.create();

        @Child protected CastTypeNode castTypeA = CastTypeNodeGen.create(null, null);
        @Child protected CastTypeNode castTypeB = CastTypeNodeGen.create(null, null);
        @Child protected TypeofNode typeofA = TypeofNodeGen.create(null);
        @Child protected TypeofNode typeofB = TypeofNodeGen.create(null);

        protected enum Operation {
            AND("bitwAnd"),
            OR("bitwOr"),
            XOR("bitwXor"),
            NOT("bitNot"),
            SHIFTR("bitShiftR"),
            SHIFTL("bitShiftL");

            private final String name;

            private Operation(String name) {
                this.name = name;
            }
        }

        protected Object basicBit(VirtualFrame frame, RAbstractVector a, RAbstractVector b, Operation op) {
            checkBasicBit(a, b, op);
            RAbstractIntVector aVec = (RAbstractIntVector) castTypeA.execute(frame, a, RType.Integer);
            RAbstractIntVector bVec = (RAbstractIntVector) castTypeB.execute(frame, b, RType.Integer);
            int aLen = aVec.getLength();
            int bLen = bVec.getLength();
            int ansSize = (aLen != 0 && bLen != 0) ? Math.max(aLen, bLen) : 0;
            int[] ans = new int[ansSize];
            boolean completeVector = true;

            for (int i = 0; i < ansSize; i++) {
                int aVal = aVec.getDataAt(i % aLen);
                int bVal = bVec.getDataAt(i % bLen);
                if ((aVal == RRuntime.INT_NA || bVal == RRuntime.INT_NA)) {
                    ans[i] = RRuntime.INT_NA;
                    completeVector = false;
                } else {
                    switch (op) {
                        case AND:
                            ans[i] = aVal & bVal;
                            break;
                        case OR:
                            ans[i] = aVal | bVal;
                            break;
                        case XOR:
                            ans[i] = aVal ^ bVal;
                            break;
                        case SHIFTR:
                            if (bVal > 31) {
                                ans[i] = RRuntime.INT_NA;
                                completeVector = false;
                            } else {
                                ans[i] = aVal >>> bVal;
                            }
                            break;
                        case SHIFTL:
                            if (bVal > 31) {
                                ans[i] = RRuntime.INT_NA;
                                completeVector = false;
                            } else {
                                ans[i] = aVal << bVal;
                            }
                            break;
                    }
                }
            }

            return RDataFactory.createIntVector(ans, completeVector);
        }

        protected Object bitNot(VirtualFrame frame, RAbstractVector a) {
            RAbstractIntVector aVec = (RAbstractIntVector) castTypeA.execute(frame, a, RType.Integer);
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
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.SAME_TYPE, "a", "b");
            }
        }

        protected void hasSupportedType(RAbstractVector a, Operation op) {
            if (!(a instanceof RAbstractIntVector) && !(a instanceof RAbstractDoubleVector)) {
                errorProfile.enter();
                String type = typeofA.execute(a).getName();
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.UNIMPLEMENTED_TYPE_IN_FUNCTION, type, op.name);
            }
        }

        protected boolean shiftByCharacter(RAbstractVector n) {
            return typeofB.execute(n) == RType.Character;
        }

    }

    @RBuiltin(name = "bitwiseAnd", kind = RBuiltinKind.INTERNAL, parameterNames = {"a", "b"})
    public abstract static class BitwiseAnd extends BasicBitwise {

        @Specialization
        protected Object bitwAnd(VirtualFrame frame, RAbstractVector a, RAbstractVector b) {
            controlVisibility();
            return basicBit(frame, a, b, Operation.AND);
        }

    }

    @RBuiltin(name = "bitwiseOr", kind = RBuiltinKind.INTERNAL, parameterNames = {"a", "b"})
    public abstract static class BitwiseOr extends BasicBitwise {

        @Specialization
        protected Object bitwOr(VirtualFrame frame, RAbstractVector a, RAbstractVector b) {
            controlVisibility();
            return basicBit(frame, a, b, Operation.OR);
        }

    }

    @RBuiltin(name = "bitwiseXor", kind = RBuiltinKind.INTERNAL, parameterNames = {"a", "b"})
    public abstract static class BitwiseXor extends BasicBitwise {

        @Specialization
        protected Object bitwXor(VirtualFrame frame, RAbstractVector a, RAbstractVector b) {
            controlVisibility();
            return basicBit(frame, a, b, Operation.XOR);
        }

    }

    @RBuiltin(name = "bitwiseShiftR", kind = RBuiltinKind.INTERNAL, parameterNames = {"a", "n"})
    public abstract static class BitwiseShiftR extends BasicBitwise {

        @Specialization(guards = {"!shiftByCharacter(n)"})
        protected Object bitwShiftR(VirtualFrame frame, RAbstractVector a, RAbstractVector n) {
            controlVisibility();
            return basicBit(frame, a, n, Operation.SHIFTR);
        }

        @Specialization(guards = {"shiftByCharacter(n)"})
        @SuppressWarnings("unused")
        protected Object bitwShiftRChar(VirtualFrame frame, RAbstractVector a, RAbstractVector n) {
            controlVisibility();
            checkShiftOrNot(a, Operation.SHIFTR);
            return makeNA(a.getLength());
        }

    }

    @RBuiltin(name = "bitwiseShiftL", kind = RBuiltinKind.INTERNAL, parameterNames = {"a", "n"})
    public abstract static class BitwiseShiftL extends BasicBitwise {

        @Specialization(guards = {"!shiftByCharacter(n)"})
        protected Object bitwShiftR(VirtualFrame frame, RAbstractVector a, RAbstractVector n) {
            controlVisibility();
            return basicBit(frame, a, n, Operation.SHIFTL);
        }

        @Specialization(guards = {"shiftByCharacter(n)"})
        @SuppressWarnings("unused")
        protected Object bitwShiftRChar(VirtualFrame frame, RAbstractVector a, RAbstractVector n) {
            controlVisibility();
            checkShiftOrNot(a, Operation.SHIFTL);
            return makeNA(a.getLength());
        }

    }

    @RBuiltin(name = "bitwiseNot", kind = RBuiltinKind.INTERNAL, parameterNames = {"a"})
    public abstract static class BitwiseNot extends BasicBitwise {

        @Specialization
        protected Object bitwNot(VirtualFrame frame, RAbstractVector a) {
            controlVisibility();
            checkShiftOrNot(a, Operation.NOT);
            return bitNot(frame, a);
        }

    }

}
