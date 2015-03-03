/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates
 *
 * All rights reserved.
 */

package com.oracle.truffle.r.nodes.builtin.base;

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

        @Child protected CastTypeNode castTypeNode;
        @Child protected TypeofNode typeof;

        protected Object basicBit(VirtualFrame frame, RAbstractVector a, RAbstractVector b, String op) {
            RAbstractIntVector aVec = (RAbstractIntVector) castTypeNode.execute(frame, a, RType.Integer);
            RAbstractIntVector bVec = (RAbstractIntVector) castTypeNode.execute(frame, b, RType.Integer);
            int aLen = aVec.getLength();
            int bLen = bVec.getLength();
            int ansSize = (aLen != 0 && bLen != 0) ? max(aLen, bLen) : 0;
            int[] ans = new int[ansSize];
            boolean completeVector = true;

            for (int i = 0; i < ansSize; i++) {
                int aVal = aVec.getDataAt(i % aLen);
                int bVal = bVec.getDataAt(i % bLen);
                if (bVal > 31) {
                    completeVector = false;
                }
                if ((aVal == RRuntime.INT_NA || bVal == RRuntime.INT_NA)) {
                    ans[i] = RRuntime.INT_NA;
                } else {
                    switch (op) {
                        case "&":
                            ans[i] = aVal & bVal;
                            break;
                        case "|":
                            ans[i] = aVal | bVal;
                            break;
                        case "^":
                            ans[i] = aVal ^ bVal;
                            break;
                        case ">>":
                            ans[i] = bVal > 31 ? RRuntime.INT_NA : aVal >> bVal;
                            break;
                        case "<<":
                            ans[i] = bVal > 31 ? RRuntime.INT_NA : aVal << bVal;
                            break;
                    }
                }
            }

            if (completeVector) {
                return RDataFactory.createIntVector(ans, RDataFactory.COMPLETE_VECTOR);
            }
            return RDataFactory.createIntVector(ans, RDataFactory.INCOMPLETE_VECTOR);
        }

        protected Object bitNot(VirtualFrame frame, RAbstractVector a) {
            RAbstractIntVector aVec = (RAbstractIntVector) castTypeNode.execute(frame, a, RType.Integer);
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

        protected int max(int x, int y) {
            if (x >= y) {
                return x;
            }
            return y;
        }

        protected void checkBasicBit(VirtualFrame frame, RAbstractVector a, RAbstractVector b, String op) {
            initChildren();
            hasSameTypes(frame, a, b);
            hasSupportedType(frame, a, op);
        }

        protected void checkShiftOrNot(VirtualFrame frame, RAbstractVector a, String op) {
            initChildren();
            hasSupportedType(frame, a, op);
        }

        protected void hasSameTypes(VirtualFrame frame, RAbstractVector a, RAbstractVector b) {
            if (!typeof.execute(frame, a).getName().equals(typeof.execute(frame, b).getName())) {
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.SAME_TYPE, "a", "b");
            }
        }

        protected void hasSupportedType(VirtualFrame frame, RAbstractVector a, String op) {
            if (!(a instanceof RAbstractIntVector) && !(a instanceof RAbstractDoubleVector)) {
                String type = typeof.execute(frame, a).getName();
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.UNIMPLEMENTED_TYPE_IN_FUNCTION, type, op);
            }
        }

        @SuppressWarnings("unused")
        protected boolean shiftByCharacter(VirtualFrame frame, RAbstractVector a, RAbstractVector n) {
            initChildren();
            if (typeof.execute(frame, n) == RType.Character) {
                return true;
            }
            return false;
        }

        protected void initChildren() {
            if (typeof == null) {
                typeof = insert(TypeofNodeGen.create(null));
                castTypeNode = insert(CastTypeNodeGen.create(null, null));
            }
        }

    }

    @RBuiltin(name = "bitwiseAnd", kind = RBuiltinKind.INTERNAL, parameterNames = {"a", "b"})
    public abstract static class BitwiseAnd extends BasicBitwise {

        @Specialization
        protected Object bitwAnd(VirtualFrame frame, RAbstractVector a, RAbstractVector b) {
            controlVisibility();
            checkBasicBit(frame, a, b, "bitwAnd");
            return basicBit(frame, a, b, "&");
        }

    }

    @RBuiltin(name = "bitwiseOr", kind = RBuiltinKind.INTERNAL, parameterNames = {"a", "b"})
    public abstract static class BitwiseOr extends BasicBitwise {

        @Specialization
        protected Object bitwOr(VirtualFrame frame, RAbstractVector a, RAbstractVector b) {
            controlVisibility();
            checkBasicBit(frame, a, b, "bitwOr");
            return basicBit(frame, a, b, "|");
        }

    }

    @RBuiltin(name = "bitwiseXor", kind = RBuiltinKind.INTERNAL, parameterNames = {"a", "b"})
    public abstract static class BitwiseXor extends BasicBitwise {

        @Specialization
        protected Object bitwXor(VirtualFrame frame, RAbstractVector a, RAbstractVector b) {
            controlVisibility();
            checkBasicBit(frame, a, b, "bitwXor");
            return basicBit(frame, a, b, "^");
        }

    }

    @RBuiltin(name = "bitwiseShiftR", kind = RBuiltinKind.INTERNAL, parameterNames = {"a", "n"})
    public abstract static class BitwiseShiftR extends BasicBitwise {

        @Specialization(guards = {"!shiftByCharacter"})
        protected Object bitwShiftR(VirtualFrame frame, RAbstractVector a, RAbstractVector n) {
            controlVisibility();
            checkShiftOrNot(frame, a, "bitShiftR");
            return basicBit(frame, a, n, ">>");
        }

        @Specialization(guards = {"shiftByCharacter"})
        @SuppressWarnings("unused")
        protected Object bitwShiftRChar(VirtualFrame frame, RAbstractVector a, RAbstractVector n) {
            controlVisibility();
            checkShiftOrNot(frame, a, "bitShiftR");
            return makeNA(a.getLength());
        }

    }

    @RBuiltin(name = "bitwiseShiftL", kind = RBuiltinKind.INTERNAL, parameterNames = {"a", "n"})
    public abstract static class BitwiseShiftL extends BasicBitwise {

        @Specialization(guards = {"!shiftByCharacter"})
        protected Object bitwShiftR(VirtualFrame frame, RAbstractVector a, RAbstractVector n) {
            controlVisibility();
            checkShiftOrNot(frame, a, "bitShiftL");
            return basicBit(frame, a, n, "<<");
        }

        @Specialization(guards = {"shiftByCharacter"})
        @SuppressWarnings("unused")
        protected Object bitwShiftRChar(VirtualFrame frame, RAbstractVector a, RAbstractVector n) {
            controlVisibility();
            checkShiftOrNot(frame, a, "bitShiftL");
            return makeNA(a.getLength());
        }

    }

    @RBuiltin(name = "bitwiseNot", kind = RBuiltinKind.INTERNAL, parameterNames = {"a"})
    public abstract static class BitwiseNot extends BasicBitwise {

        @Specialization
        protected Object bitwNot(VirtualFrame frame, RAbstractVector a) {
            controlVisibility();
            checkShiftOrNot(frame, a, "bitNot");
            return bitNot(frame, a);
        }

    }

}
