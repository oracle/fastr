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

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.binary.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

public class BitwiseFunctions {

    public abstract static class BasicBitwise extends RBuiltinNode {

        @Child protected CastTypeNode castTypeNodeA;
        @Child protected CastTypeNode castTypeNodeB;
        @Child protected TypeofNode typeof;

        protected enum Operation {
            AND,
            OR,
            XOR,
            SHIFTR,
            SHIFTL
        }

        protected Object basicBit(VirtualFrame frame, RAbstractVector a, RAbstractVector b, Operation op) {
            RAbstractIntVector aVec = (RAbstractIntVector) castTypeNodeA.execute(frame, a, RType.Integer);
            RAbstractIntVector bVec = (RAbstractIntVector) castTypeNodeB.execute(frame, b, RType.Integer);
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
                                ans[i] = aVal >> bVal;
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
            RAbstractIntVector aVec = (RAbstractIntVector) castTypeNodeA.execute(frame, a, RType.Integer);
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

        protected void checkBasicBit(RAbstractVector a, RAbstractVector b, String op) {
            initChildren();
            hasSameTypes(a, b);
            hasSupportedType(a, op);
        }

        protected void checkShiftOrNot(RAbstractVector a, String op) {
            initChildren();
            hasSupportedType(a, op);
        }

        protected void hasSameTypes(RAbstractVector a, RAbstractVector b) {
            RType aType = typeof.execute(a);
            RType bType = typeof.execute(b);
            boolean aCorrectType = (aType == RType.Integer || aType == RType.Double) ? true : false;
            boolean bCorrectType = (bType == RType.Integer || bType == RType.Double) ? true : false;
            if ((aCorrectType && bCorrectType) || aType == bType) {
                return;
            } else {
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.SAME_TYPE, "a", "b");
            }
        }

        protected void hasSupportedType(RAbstractVector a, String op) {
            if (!(a instanceof RAbstractIntVector) && !(a instanceof RAbstractDoubleVector)) {
                String type = typeof.execute(a).getName();
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.UNIMPLEMENTED_TYPE_IN_FUNCTION, type, op);
            }
        }

        protected boolean shiftByCharacter(RAbstractVector n) {
            initChildren();
            return typeof.execute(n) == RType.Character;
        }

        protected void initChildren() {
            if (typeof == null) {
                typeof = insert(TypeofNodeGen.create(null));
                CompilerDirectives.transferToInterpreterAndInvalidate();
                castTypeNodeA = insert(CastTypeNodeGen.create(null, null));
                castTypeNodeB = insert(CastTypeNodeGen.create(null, null));
            }
        }

    }

    @RBuiltin(name = "bitwiseAnd", kind = RBuiltinKind.INTERNAL, parameterNames = {"a", "b"})
    public abstract static class BitwiseAnd extends BasicBitwise {

        @Specialization
        protected Object bitwAnd(VirtualFrame frame, RAbstractVector a, RAbstractVector b) {
            controlVisibility();
            checkBasicBit(a, b, "bitwAnd");
            return basicBit(frame, a, b, Operation.AND);
        }

    }

    @RBuiltin(name = "bitwiseOr", kind = RBuiltinKind.INTERNAL, parameterNames = {"a", "b"})
    public abstract static class BitwiseOr extends BasicBitwise {

        @Specialization
        protected Object bitwOr(VirtualFrame frame, RAbstractVector a, RAbstractVector b) {
            controlVisibility();
            checkBasicBit(a, b, "bitwOr");
            return basicBit(frame, a, b, Operation.OR);
        }

    }

    @RBuiltin(name = "bitwiseXor", kind = RBuiltinKind.INTERNAL, parameterNames = {"a", "b"})
    public abstract static class BitwiseXor extends BasicBitwise {

        @Specialization
        protected Object bitwXor(VirtualFrame frame, RAbstractVector a, RAbstractVector b) {
            controlVisibility();
            checkBasicBit(a, b, "bitwXor");
            return basicBit(frame, a, b, Operation.XOR);
        }

    }

    @RBuiltin(name = "bitwiseShiftR", kind = RBuiltinKind.INTERNAL, parameterNames = {"a", "n"})
    public abstract static class BitwiseShiftR extends BasicBitwise {

        @Specialization(guards = {"!shiftByCharacter(n)"})
        protected Object bitwShiftR(VirtualFrame frame, RAbstractVector a, RAbstractVector n) {
            controlVisibility();
            checkShiftOrNot(a, "bitShiftR");
            return basicBit(frame, a, n, Operation.SHIFTR);
        }

        @Specialization(guards = {"shiftByCharacter(n)"})
        @SuppressWarnings("unused")
        protected Object bitwShiftRChar(VirtualFrame frame, RAbstractVector a, RAbstractVector n) {
            controlVisibility();
            checkShiftOrNot(a, "bitShiftR");
            return makeNA(a.getLength());
        }

    }

    @RBuiltin(name = "bitwiseShiftL", kind = RBuiltinKind.INTERNAL, parameterNames = {"a", "n"})
    public abstract static class BitwiseShiftL extends BasicBitwise {

        @Specialization(guards = {"!shiftByCharacter(n)"})
        protected Object bitwShiftR(VirtualFrame frame, RAbstractVector a, RAbstractVector n) {
            controlVisibility();
            checkShiftOrNot(a, "bitShiftL");
            return basicBit(frame, a, n, Operation.SHIFTL);
        }

        @Specialization(guards = {"shiftByCharacter(n)"})
        @SuppressWarnings("unused")
        protected Object bitwShiftRChar(VirtualFrame frame, RAbstractVector a, RAbstractVector n) {
            controlVisibility();
            checkShiftOrNot(a, "bitShiftL");
            return makeNA(a.getLength());
        }

    }

    @RBuiltin(name = "bitwiseNot", kind = RBuiltinKind.INTERNAL, parameterNames = {"a"})
    public abstract static class BitwiseNot extends BasicBitwise {

        @Specialization
        protected Object bitwNot(VirtualFrame frame, RAbstractVector a) {
            controlVisibility();
            checkShiftOrNot(a, "bitNot");
            return bitNot(frame, a);
        }

    }

}
