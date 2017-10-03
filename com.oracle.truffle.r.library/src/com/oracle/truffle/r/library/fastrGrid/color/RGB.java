/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1997-2014, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.fastrGrid.color;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.r.library.fastrGrid.color.RGBNodeGen.DoubleRGBNodeGen;
import com.oracle.truffle.r.library.fastrGrid.color.RGBNodeGen.IntegerRGBNodeGen;
import com.oracle.truffle.r.library.fastrGrid.device.GridColor;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef;
import com.oracle.truffle.r.nodes.unary.CastDoubleNode;
import com.oracle.truffle.r.nodes.unary.CastIntegerNode;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RTypes;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

public abstract class RGB extends RExternalBuiltinNode.Arg6 {

    static {
        Casts casts = new Casts(RGB.class);
        casts.arg(4).asDoubleVector().findFirst();
        casts.arg(5).mapNull(Predef.emptyStringVector()).asStringVector();
    }

    public static RGB create() {
        return RGBNodeGen.create();
    }

    protected static boolean isIntRange(double mcv) {
        return mcv == 255;
    }

    @Specialization(guards = "isIntRange(mcv)")
    @TruffleBoundary
    Object doInteger(Object r, Object g, Object b, Object a, @SuppressWarnings("unused") double mcv, RAbstractStringVector names,
                    @Cached("create()") CastIntegerNode castR,
                    @Cached("create()") CastIntegerNode castG,
                    @Cached("create()") CastIntegerNode castB,
                    @Cached("create()") CastIntegerNode castA,
                    @Cached("createInteger()") IntegerRGB inner) {
        return inner.execute(castR.doCast(r), castG.doCast(g), castB.doCast(b), a == RNull.instance ? RNull.instance : castA.doCast(a), names);
    }

    @Specialization(guards = "!isIntRange(mcv)")
    @TruffleBoundary
    Object doDouble(Object r, Object g, Object b, Object a, double mcv, RAbstractStringVector names,
                    @Cached("create()") CastDoubleNode castR,
                    @Cached("create()") CastDoubleNode castG,
                    @Cached("create()") CastDoubleNode castB,
                    @Cached("create()") CastDoubleNode castA,
                    @Cached("createDouble()") DoubleRGB inner) {
        if (!Double.isFinite(mcv)) {
            throw error(Message.GENERIC, "invalid value of 'maxColorValue'");
        }
        return inner.execute(castR.doCast(r), castG.doCast(g), castB.doCast(b), a == RNull.instance ? RNull.instance : castA.doCast(a), mcv, names);
    }

    protected static IntegerRGB createInteger() {
        return IntegerRGBNodeGen.create();
    }

    protected static DoubleRGB createDouble() {
        return DoubleRGBNodeGen.create();
    }

    @TypeSystemReference(RTypes.class)
    abstract static class RGBBase extends RBaseNode {

        protected static int addColor(boolean alpha, int[] result, int start, GridColor color) {
            int pos = start;
            result[pos++] = color.getRed();
            result[pos++] = color.getGreen();
            result[pos++] = color.getBlue();
            if (alpha) {
                result[pos++] = color.getAlpha();
            }
            return pos;
        }

        private static final char[] HexDigits = "0123456789ABCDEF".toCharArray();

        protected static String rgb2rgb(int r, int g, int b) {
            char[] colBuf = new char[7];
            colBuf[0] = '#';
            colBuf[1] = HexDigits[(r >> 4) & 15];
            colBuf[2] = HexDigits[r & 15];
            colBuf[3] = HexDigits[(g >> 4) & 15];
            colBuf[4] = HexDigits[g & 15];
            colBuf[5] = HexDigits[(b >> 4) & 15];
            colBuf[6] = HexDigits[b & 15];
            return new String(colBuf);
        }

        protected static String rgba2rgb(int r, int g, int b, int a) {
            char[] colBuf = new char[9];
            colBuf[0] = '#';
            colBuf[1] = HexDigits[(r >> 4) & 15];
            colBuf[2] = HexDigits[r & 15];
            colBuf[3] = HexDigits[(g >> 4) & 15];
            colBuf[4] = HexDigits[g & 15];
            colBuf[5] = HexDigits[(b >> 4) & 15];
            colBuf[6] = HexDigits[b & 15];
            colBuf[7] = HexDigits[(a >> 4) & 15];
            colBuf[8] = HexDigits[a & 15];
            return new String(colBuf);
        }

        protected int scaleColor(double x) {
            if (RRuntime.isNA(x)) {
                error(Message.GENERIC, "color intensity NA, not in [0,1]");
            }
            if (!Double.isFinite(x) || x < 0.0 || x > 1.0) {
                error(Message.GENERIC, "color intensity " + x + ", not in [0,1]");
            }
            return (int) (255 * x + 0.5);
        }

        protected int checkColor(int x) {
            if (RRuntime.isNA(x)) {
                error(Message.GENERIC, "color intensity NA, not in 0:255");
            }
            if (x < 0 || x > 255) {
                error(Message.GENERIC, "color intensity " + x + ", not in 0:255");
            }
            return x;
        }

        protected int scaleAlpha(double x) {
            if (RRuntime.isNA(x)) {
                error(Message.GENERIC, "alpha level NA, not in [0,1]");
            }
            if (!Double.isFinite(x) || x < 0.0 || x > 1.0) {
                error(Message.GENERIC, "alpha level " + x + ", not in [0,1]");
            }
            return (int) (255 * x + 0.5);
        }

        protected int checkAlpha(int x) {
            if (RRuntime.isNA(x)) {
                error(Message.GENERIC, "alpha level NA, not in 0:255");
            }
            if (x < 0 || x > 255) {
                error(Message.GENERIC, "alpha level " + x + ", not in 0:255");
            }
            return x;
        }
    }

    abstract static class IntegerRGB extends RGBBase {

        public abstract RStringVector execute(Object r, Object g, Object b, Object a, RAbstractStringVector names);

        @Specialization
        @TruffleBoundary
        protected RStringVector doAlpha(RAbstractIntVector r, RAbstractIntVector g, RAbstractIntVector b, RAbstractIntVector a, RAbstractStringVector names) {
            int lengthR = r.getLength();
            int lengthG = g.getLength();
            int lengthB = b.getLength();
            int lengthA = a.getLength();
            if (lengthR == 0 || lengthG == 0 || lengthB == 0 || lengthA == 0) {
                return RDataFactory.createEmptyStringVector();
            }
            int length = Math.max(Math.max(lengthR, lengthG), Math.max(lengthB, lengthA));
            if (names.getLength() != 0 && names.getLength() != length) {
                throw error(Message.GENERIC, "invalid 'names' vector");
            }
            String[] result = new String[length];
            for (int i = 0; i < length; i++) {
                int rValue = r.getDataAt(i % lengthR);
                int gValue = g.getDataAt(i % lengthG);
                int bValue = b.getDataAt(i % lengthB);
                int aValue = a.getDataAt(i % lengthA);
                result[i] = rgba2rgb(checkColor(rValue), checkColor(gValue), checkColor(bValue), checkAlpha(aValue));
            }
            return RDataFactory.createStringVector(result, true, names.getLength() == 0 ? null : names.materialize());
        }

        @Specialization
        @TruffleBoundary
        protected RStringVector doNonAlpha(RAbstractIntVector r, RAbstractIntVector g, RAbstractIntVector b, @SuppressWarnings("unused") RNull a, RAbstractStringVector names) {
            int lengthR = r.getLength();
            int lengthG = g.getLength();
            int lengthB = b.getLength();
            if (lengthR == 0 || lengthG == 0 || lengthB == 0) {
                return RDataFactory.createEmptyStringVector();
            }
            int length = Math.max(Math.max(lengthR, lengthG), lengthB);
            if (names.getLength() != 0 && names.getLength() != length) {
                throw error(Message.GENERIC, "invalid 'names' vector");
            }
            String[] result = new String[length];
            for (int i = 0; i < length; i++) {
                int rValue = r.getDataAt(i % lengthR);
                int gValue = g.getDataAt(i % lengthG);
                int bValue = b.getDataAt(i % lengthB);
                result[i] = rgb2rgb(checkColor(rValue), checkColor(gValue), checkColor(bValue));
            }
            return RDataFactory.createStringVector(result, true, names.getLength() == 0 ? null : names.materialize());
        }
    }

    abstract static class DoubleRGB extends RGBBase {

        public abstract RStringVector execute(Object r, Object g, Object b, Object a, double mcv, RAbstractStringVector names);

        @Specialization
        @TruffleBoundary
        protected RStringVector doAlpha(RAbstractDoubleVector r, RAbstractDoubleVector g, RAbstractDoubleVector b, RAbstractDoubleVector a, double mcv, RAbstractStringVector names) {
            int lengthR = r.getLength();
            int lengthG = g.getLength();
            int lengthB = b.getLength();
            int lengthA = a.getLength();
            if (lengthR == 0 || lengthG == 0 || lengthB == 0 || lengthA == 0) {
                return RDataFactory.createEmptyStringVector();
            }
            int length = Math.max(Math.max(lengthR, lengthG), Math.max(lengthB, lengthA));
            if (names.getLength() != 0 && names.getLength() != length) {
                throw error(Message.GENERIC, "invalid 'names' vector");
            }
            String[] result = new String[length];
            for (int i = 0; i < length; i++) {
                double rValue = r.getDataAt(i % lengthR);
                double gValue = g.getDataAt(i % lengthG);
                double bValue = b.getDataAt(i % lengthB);
                double aValue = a.getDataAt(i % lengthA);
                result[i] = rgba2rgb(scaleColor(rValue / mcv), scaleColor(gValue / mcv), scaleColor(bValue / mcv), scaleAlpha(aValue / mcv));
            }
            return RDataFactory.createStringVector(result, true, names.getLength() == 0 ? null : names.materialize());
        }

        @Specialization
        @TruffleBoundary
        protected RStringVector doNonAlpha(RAbstractDoubleVector r, RAbstractDoubleVector g, RAbstractDoubleVector b, @SuppressWarnings("unused") RNull a, double mcv, RAbstractStringVector names) {
            int lengthR = r.getLength();
            int lengthG = g.getLength();
            int lengthB = b.getLength();
            if (lengthR == 0 || lengthG == 0 || lengthB == 0) {
                return RDataFactory.createEmptyStringVector();
            }
            int length = Math.max(Math.max(lengthR, lengthG), lengthB);
            if (names.getLength() != 0 && names.getLength() != length) {
                throw error(Message.GENERIC, "invalid 'names' vector");
            }
            String[] result = new String[length];
            for (int i = 0; i < length; i++) {
                double rValue = r.getDataAt(i % lengthR);
                double gValue = g.getDataAt(i % lengthG);
                double bValue = b.getDataAt(i % lengthB);
                result[i] = rgb2rgb(scaleColor(rValue / mcv), scaleColor(gValue / mcv), scaleColor(bValue / mcv));
            }
            return RDataFactory.createStringVector(result, true, names.getLength() == 0 ? null : names.materialize());
        }
    }
}
