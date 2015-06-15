/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995, 1996, 1997  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1995-2014, The R Core Team
 * Copyright (c) 2002-2008, The R Foundation
 * Copyright (c) 2015, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.builtin.base.OrderNodeGen.CmpNodeGen;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.closures.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin(name = "order", kind = INTERNAL, parameterNames = {"na.last", "decreasing", "..."})
public abstract class Order extends RPrecedenceBuiltinNode {

    public abstract RIntVector executeRIntVector(VirtualFrame frame, byte naLast, byte dec, RArgsValuesAndNames args);

    @Child private CastToVectorNode castVector;
    @Child private CmpNode cmpNode;

    private final BranchProfile error = BranchProfile.create();
    private final ConditionProfile decProfile = ConditionProfile.createBinaryProfile();

    private static final int[] SINCS = {1073790977, 268460033, 67121153, 16783361, 4197377, 1050113, 262913, 65921, 16577, 4193, 1073, 281, 77, 23, 8, 1, 0};

    private RAbstractVector castVector(Object value) {
        if (castVector == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castVector = insert(CastToVectorNodeGen.create(false));
        }
        return (RAbstractVector) castVector.execute(value);
    }

    private int cmp(VirtualFrame frame, Object v, int i, int j, byte naLast) {
        if (cmpNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            cmpNode = insert(CmpNodeGen.create(null, null, null, null));
        }
        return cmpNode.executeInt(frame, v, i, j, naLast);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "noVec(args)")
    Object orderEmpty(VirtualFrame frame, RAbstractLogicalVector naLastVec, RAbstractLogicalVector decVec, RArgsValuesAndNames args) {
        return RNull.instance;
    }

    @Specialization(guards = {"oneVec(args)", "isFirstIntegerPrecedence(args)"})
    Object orderInt(RAbstractLogicalVector naLastVec, RAbstractLogicalVector decVec, RArgsValuesAndNames args) {
        Object[] vectors = args.getArguments();
        RAbstractIntVector v = (RAbstractIntVector) castVector(vectors[0]);
        int n = v.getLength();

        boolean naLast = true;
        boolean dec = true;

        if (naLastVec.getLength() == 0 || naLastVec.getDataAt(0) == RRuntime.LOGICAL_FALSE) {
            naLast = false;
        }
        if (decVec.getLength() == 0 || decVec.getDataAt(0) == RRuntime.LOGICAL_FALSE) {
            dec = false;
        }

        int[] indx = new int[n];
        for (int i = 0; i < indx.length; i++) {
            indx[i] = i;
        }
        orderVector1(indx, v, naLast, dec);
        for (int i = 0; i < indx.length; i++) {
            indx[i] = indx[i] + 1;
        }

        return RDataFactory.createIntVector(indx, RDataFactory.COMPLETE_VECTOR);
    }

    @Specialization(guards = {"oneVec(args)", "isFirstDoublePrecedence(args)"})
    Object orderDouble(RAbstractLogicalVector naLastVec, RAbstractLogicalVector decVec, RArgsValuesAndNames args) {
        Object[] vectors = args.getArguments();
        RAbstractDoubleVector v = (RAbstractDoubleVector) castVector(vectors[0]);
        int n = v.getLength();

        boolean naLast = true;
        boolean dec = true;

        if (naLastVec.getLength() == 0 || naLastVec.getDataAt(0) == RRuntime.LOGICAL_FALSE) {
            naLast = false;
        }
        if (decVec.getLength() == 0 || decVec.getDataAt(0) == RRuntime.LOGICAL_FALSE) {
            dec = false;
        }

        int[] indx = new int[n];
        for (int i = 0; i < indx.length; i++) {
            indx[i] = i;
        }
        orderVector1(indx, v, naLast, dec);
        for (int i = 0; i < indx.length; i++) {
            indx[i] = indx[i] + 1;
        }

        return RDataFactory.createIntVector(indx, RDataFactory.COMPLETE_VECTOR);
    }

    @Specialization(guards = {"oneVec(args)", "isFirstLogicalPrecedence(args)"})
    Object orderLogical(RAbstractLogicalVector naLastVec, RAbstractLogicalVector decVec, RArgsValuesAndNames args) {
        Object[] vectors = args.getArguments();
        vectors[0] = RClosures.createLogicalToIntVector((RAbstractLogicalVector) castVector(vectors[0]));
        return orderInt(naLastVec, decVec, args);
    }

    @Specialization(guards = {"oneVec(args)", "isFirstStringPrecedence(args)"})
    Object orderString(RAbstractLogicalVector naLastVec, RAbstractLogicalVector decVec, RArgsValuesAndNames args) {
        Object[] vectors = args.getArguments();
        RAbstractStringVector v = (RAbstractStringVector) castVector(vectors[0]);
        int n = v.getLength();

        boolean naLast = true;
        boolean dec = true;

        if (naLastVec.getLength() == 0 || naLastVec.getDataAt(0) == RRuntime.LOGICAL_FALSE) {
            naLast = false;
        }
        if (decVec.getLength() == 0 || decVec.getDataAt(0) == RRuntime.LOGICAL_FALSE) {
            dec = false;
        }

        int[] indx = new int[n];
        for (int i = 0; i < indx.length; i++) {
            indx[i] = i;
        }
        orderVector1(indx, v, naLast, dec);
        for (int i = 0; i < indx.length; i++) {
            indx[i] = indx[i] + 1;
        }

        return RDataFactory.createIntVector(indx, RDataFactory.COMPLETE_VECTOR);
    }

    @Specialization(guards = {"oneVec(args)", "isFirstComplexPrecedence( args)"})
    Object orderComplex(RAbstractLogicalVector naLastVec, RAbstractLogicalVector decVec, RArgsValuesAndNames args) {
        Object[] vectors = args.getArguments();
        RAbstractComplexVector v = (RAbstractComplexVector) castVector(vectors[0]);
        int n = v.getLength();

        boolean naLast = true;
        boolean dec = true;

        if (naLastVec.getLength() == 0 || naLastVec.getDataAt(0) == RRuntime.LOGICAL_FALSE) {
            naLast = false;
        }
        if (decVec.getLength() == 0 || decVec.getDataAt(0) == RRuntime.LOGICAL_FALSE) {
            dec = false;
        }

        int[] indx = new int[n];
        for (int i = 0; i < indx.length; i++) {
            indx[i] = i;
        }
        orderVector1(indx, v, naLast, dec);
        for (int i = 0; i < indx.length; i++) {
            indx[i] = indx[i] + 1;
        }

        return RDataFactory.createIntVector(indx, RDataFactory.COMPLETE_VECTOR);
    }

    private int preprocessVectors(RArgsValuesAndNames args) {
        Object[] vectors = args.getArguments();
        RAbstractVector v = castVector(vectors[0]);
        int n = v.getLength();
        vectors[0] = v;
        for (int i = 1; i < vectors.length; i++) {
            v = castVector(vectors[i]);
            if (n != v.getLength()) {
                error.enter();
                throw RError.error(RError.Message.ARGUMENT_LENGTHS_DIFFER);
            }
            vectors[i] = v;
        }
        return n;
    }

    @Specialization(guards = {"!oneVec(args)", "!noVec(args)"})
    Object orderMulti(VirtualFrame frame, RAbstractLogicalVector naLastVec, RAbstractLogicalVector decVec, RArgsValuesAndNames args) {
        int n = preprocessVectors(args);

        byte naLast = RRuntime.LOGICAL_TRUE;
        boolean dec = true;

        if (naLastVec.getLength() == 0 || naLastVec.getDataAt(0) == RRuntime.LOGICAL_FALSE) {
            naLast = RRuntime.LOGICAL_FALSE;
        }
        if (decVec.getLength() == 0 || decVec.getDataAt(0) == RRuntime.LOGICAL_FALSE) {
            dec = false;
        }

        int[] indx = new int[n];
        for (int i = 0; i < indx.length; i++) {
            indx[i] = i;
        }
        orderVector(frame, indx, args, naLast, dec);
        for (int i = 0; i < indx.length; i++) {
            indx[i] = indx[i] + 1;
        }

        return RDataFactory.createIntVector(indx, RDataFactory.COMPLETE_VECTOR);
    }

    private boolean greaterSub(VirtualFrame frame, int i, int j, RArgsValuesAndNames args, byte naLast, boolean dec) {
        Object[] vectors = args.getArguments();
        int c = -1;
        for (int k = 0; k < args.getLength(); k++) {
            RAbstractVector v = (RAbstractVector) vectors[k];
            c = cmp(frame, v, i, j, naLast);
            if (dec) {
                c = -c;
            }
            if (c > 0) {
                return true;
            }
            if (c < 0) {
                return false;
            }
        }
        return (c == 0 && i < j) ? false : true;
    }

    private void orderVector(VirtualFrame frame, int[] indx, RArgsValuesAndNames args, byte naLast, boolean dec) {
        if (indx.length > 1) {

            int t = 0;
            for (; SINCS[t] > indx.length; t++) {
            }
            for (int h = SINCS[t]; t < 16; h = SINCS[++t]) {
                for (int i = h; i < indx.length; i++) {
                    int itmp = indx[i];
                    int j = i;
                    while (j >= h && greaterSub(frame, indx[j - h], itmp, args, naLast, dec)) {
                        indx[j] = indx[j - h];
                        j -= h;
                    }
                    indx[j] = itmp;
                }
            }
        }
    }

    private void orderVector1(int[] indx, RAbstractIntVector dv, boolean naLast, boolean dec) {
        int lo = 0;
        int hi = indx.length - 1;
        int numNa = 0;
        if (!dv.isComplete()) {
            boolean[] isNa = new boolean[indx.length];
            for (int i = 0; i < isNa.length; i++) {
                if (RRuntime.isNA(dv.getDataAt(i))) {
                    isNa[i] = true;
                    numNa++;
                }
            }

            if (numNa > 0) {
                if (!naLast) {
                    for (int i = 0; i < isNa.length; i++) {
                        isNa[i] = !isNa[i];
                    }
                }
                sortNA(indx, isNa, lo, hi);
                if (naLast) {
                    hi -= numNa;
                } else {
                    lo += numNa;
                }
            }
        }

        sort(indx, dv, lo, hi, dec);
    }

    private void orderVector1(int[] indx, RAbstractDoubleVector dv, boolean naLast, boolean dec) {
        int lo = 0;
        int hi = indx.length - 1;
        int numNa = 0;
        boolean[] isNa = new boolean[indx.length];
        for (int i = 0; i < isNa.length; i++) {
            if (RRuntime.isNAorNaN(dv.getDataAt(i))) {
                isNa[i] = true;
                numNa++;
            }
        }

        if (numNa > 0) {
            if (!naLast) {
                for (int i = 0; i < isNa.length; i++) {
                    isNa[i] = !isNa[i];
                }
            }
            sortNA(indx, isNa, lo, hi);
            if (naLast) {
                hi -= numNa;
            } else {
                lo += numNa;
            }
        }

        sort(indx, dv, lo, hi, dec);
    }

    private void orderVector1(int[] indx, RAbstractStringVector dv, boolean naLast, boolean dec) {
        int lo = 0;
        int hi = indx.length - 1;
        int numNa = 0;
        if (!dv.isComplete()) {
            boolean[] isNa = new boolean[indx.length];
            for (int i = 0; i < isNa.length; i++) {
                if (RRuntime.isNA(dv.getDataAt(i))) {
                    isNa[i] = true;
                    numNa++;
                }
            }

            if (numNa > 0) {
                if (!naLast) {
                    for (int i = 0; i < isNa.length; i++) {
                        isNa[i] = !isNa[i];
                    }
                }
                sortNA(indx, isNa, lo, hi);
                if (naLast) {
                    hi -= numNa;
                } else {
                    lo += numNa;
                }
            }
        }

        sort(indx, dv, lo, hi, dec);
    }

    private void orderVector1(int[] indx, RAbstractComplexVector dv, boolean naLast, boolean dec) {
        int lo = 0;
        int hi = indx.length - 1;
        int numNa = 0;
        if (!dv.isComplete()) {
            boolean[] isNa = new boolean[indx.length];
            for (int i = 0; i < isNa.length; i++) {
                if (RRuntime.isNA(dv.getDataAt(i))) {
                    isNa[i] = true;
                    numNa++;
                }
            }

            if (numNa > 0) {
                if (!naLast) {
                    for (int i = 0; i < isNa.length; i++) {
                        isNa[i] = !isNa[i];
                    }
                }
                sortNA(indx, isNa, lo, hi);
                if (naLast) {
                    hi -= numNa;
                } else {
                    lo += numNa;
                }
            }
        }

        sort(indx, dv, lo, hi, dec);
    }

    private static void sortNA(int[] indx, boolean[] isNa, int lo, int hi) {
        int t = 0;
        for (; SINCS[t] > indx.length; t++) {
        }
        for (int h = SINCS[t]; t < 16; h = SINCS[++t]) {
            for (int i = lo + h; i <= hi; i++) {
                int itmp = indx[i];
                int j = i;
                while (j >= lo + h) {
                    int a = indx[j - h];
                    int b = itmp;
                    if (!((isNa[a] && !isNa[b]) || (isNa[a] == isNa[b] && a > b))) {
                        break;
                    }
                    indx[j] = indx[j - h];
                    j -= h;
                }
                indx[j] = itmp;
            }
        }

    }

    private void sort(int[] indx, RAbstractDoubleVector dv, int lo, int hi, boolean dec) {
        int t = 0;
        for (; SINCS[t] > hi - lo + 1; t++) {
        }
        for (int h = SINCS[t]; t < 16; h = SINCS[++t]) {
            for (int i = lo + h; i <= hi; i++) {
                int itmp = indx[i];
                int j = i;
                while (j >= lo + h) {
                    int a = indx[j - h];
                    int b = itmp;
                    if (decProfile.profile(dec)) {
                        if (!((dv.getDataAt(a)) < dv.getDataAt(b) || (dv.getDataAt(a) == dv.getDataAt(b) && a > b))) {
                            break;
                        }
                    } else {
                        if (!((dv.getDataAt(a)) > dv.getDataAt(b) || (dv.getDataAt(a) == dv.getDataAt(b) && a > b))) {
                            break;
                        }

                    }
                    indx[j] = indx[j - h];
                    j -= h;
                }
                indx[j] = itmp;
            }
        }
    }

    private void sort(int[] indx, RAbstractIntVector dv, int lo, int hi, boolean dec) {
        int t = 0;
        for (; SINCS[t] > hi - lo + 1; t++) {
        }
        for (int h = SINCS[t]; t < 16; h = SINCS[++t]) {
            for (int i = lo + h; i <= hi; i++) {
                int itmp = indx[i];
                int j = i;
                while (j >= lo + h) {
                    int a = indx[j - h];
                    int b = itmp;
                    if (decProfile.profile(dec)) {
                        if (!((dv.getDataAt(a)) < dv.getDataAt(b) || (dv.getDataAt(a) == dv.getDataAt(b) && a > b))) {
                            break;
                        }
                    } else {
                        if (!((dv.getDataAt(a)) > dv.getDataAt(b) || (dv.getDataAt(a) == dv.getDataAt(b) && a > b))) {
                            break;
                        }

                    }
                    indx[j] = indx[j - h];
                    j -= h;
                }
                indx[j] = itmp;
            }
        }
    }

    private void sort(int[] indx, RAbstractStringVector dv, int lo, int hi, boolean dec) {
        int t = 0;
        for (; SINCS[t] > hi - lo + 1; t++) {
        }
        for (int h = SINCS[t]; t < 16; h = SINCS[++t]) {
            for (int i = lo + h; i <= hi; i++) {
                int itmp = indx[i];
                int j = i;
                while (j >= lo + h) {
                    int a = indx[j - h];
                    int b = itmp;
                    if (decProfile.profile(dec)) {
                        if (!(dv.getDataAt(a).compareTo(dv.getDataAt(b)) < 0 || (dv.getDataAt(a).compareTo(dv.getDataAt(b)) == 0 && a > b))) {
                            break;
                        }
                    } else {
                        if (!(dv.getDataAt(a).compareTo(dv.getDataAt(b)) > 0 || (dv.getDataAt(a).compareTo(dv.getDataAt(b)) == 0 && a > b))) {
                            break;
                        }
                    }
                    indx[j] = indx[j - h];
                    j -= h;
                }
                indx[j] = itmp;
            }
        }
    }

    private static boolean lt(RComplex a, RComplex b) {
        if (a.getRealPart() == b.getRealPart()) {
            return a.getImaginaryPart() < b.getImaginaryPart();
        } else {
            return a.getRealPart() < b.getRealPart();
        }
    }

    private static boolean gt(RComplex a, RComplex b) {
        if (a.getRealPart() == b.getRealPart()) {
            return a.getImaginaryPart() > b.getImaginaryPart();
        } else {
            return a.getRealPart() > b.getRealPart();
        }
    }

    private static boolean eq(RComplex a, RComplex b) {
        return a.getRealPart() == b.getRealPart() && a.getImaginaryPart() == b.getImaginaryPart();
    }

    private void sort(int[] indx, RAbstractComplexVector dv, int lo, int hi, boolean dec) {
        int t = 0;
        for (; SINCS[t] > hi - lo + 1; t++) {
        }
        for (int h = SINCS[t]; t < 16; h = SINCS[++t]) {
            for (int i = lo + h; i <= hi; i++) {
                int itmp = indx[i];
                int j = i;
                while (j >= lo + h) {
                    int a = indx[j - h];
                    int b = itmp;
                    if (decProfile.profile(dec)) {
                        if (!(lt(dv.getDataAt(a), dv.getDataAt(b)) || (eq(dv.getDataAt(a), dv.getDataAt(b)) && a > b))) {
                            break;
                        }
                    } else {
                        if (!(gt(dv.getDataAt(a), dv.getDataAt(b)) || (eq(dv.getDataAt(a), dv.getDataAt(b)) && a > b))) {
                            break;
                        }

                    }
                    indx[j] = indx[j - h];
                    j -= h;
                }
                indx[j] = itmp;
            }
        }
    }

    protected boolean isFirstIntegerPrecedence(RArgsValuesAndNames args) {
        return isIntegerPrecedence(args.getArgument(0));
    }

    protected boolean isFirstDoublePrecedence(RArgsValuesAndNames args) {
        return isDoublePrecedence(args.getArgument(0));
    }

    protected boolean isFirstLogicalPrecedence(RArgsValuesAndNames args) {
        return isLogicalPrecedence(args.getArgument(0));
    }

    protected boolean isFirstStringPrecedence(RArgsValuesAndNames args) {
        return isStringPrecedence(args.getArgument(0));
    }

    protected boolean isFirstComplexPrecedence(RArgsValuesAndNames args) {
        return isComplexPrecedence(args.getArgument(0));
    }

    protected boolean noVec(RArgsValuesAndNames args) {
        return args.isEmpty();
    }

    protected boolean oneVec(RArgsValuesAndNames args) {
        return args.getLength() == 1;
    }

    @NodeChildren({@NodeChild("v"), @NodeChild("i"), @NodeChild("j"), @NodeChild("naLast")})
    protected abstract static class CmpNode extends RNode {

        public abstract int executeInt(VirtualFrame frame, Object v, int i, int j, byte naLast);

        @Specialization
        protected int lcmp(RAbstractLogicalVector v, int i, int j, byte naLast) {
            byte x = v.getDataAt(i);
            byte y = v.getDataAt(j);
            boolean nax = RRuntime.isNA(x);
            boolean nay = RRuntime.isNA(y);
            if (nax && nay) {
                return 0;
            }
            if (nax) {
                return naLast == RRuntime.LOGICAL_TRUE ? 1 : -1;
            }
            if (nay) {
                return naLast == RRuntime.LOGICAL_TRUE ? -1 : 1;
            }
            if (x < y) {
                return -1;
            }
            if (x > y) {
                return 1;
            }
            return 0;
        }

        @Specialization
        protected int icmp(RAbstractIntVector v, int i, int j, byte naLast) {
            int x = v.getDataAt(i);
            int y = v.getDataAt(j);
            boolean nax = RRuntime.isNA(x);
            boolean nay = RRuntime.isNA(y);
            if (nax && nay) {
                return 0;
            }
            if (nax) {
                return naLast == RRuntime.LOGICAL_TRUE ? 1 : -1;
            }
            if (nay) {
                return naLast == RRuntime.LOGICAL_TRUE ? -1 : 1;
            }
            if (x < y) {
                return -1;
            }
            if (x > y) {
                return 1;
            }
            return 0;
        }

        @Specialization
        protected int rcmp(RAbstractDoubleVector v, int i, int j, byte naLast) {
            double x = v.getDataAt(i);
            double y = v.getDataAt(j);
            boolean nax = RRuntime.isNAorNaN(x);
            boolean nay = RRuntime.isNAorNaN(y);
            if (nax && nay) {
                return 0;
            }
            if (nax) {
                return naLast == RRuntime.LOGICAL_TRUE ? 1 : -1;
            }
            if (nay) {
                return naLast == RRuntime.LOGICAL_TRUE ? -1 : 1;
            }
            if (x < y) {
                return -1;
            }
            if (x > y) {
                return 1;
            }
            return 0;
        }

        @Specialization
        protected int scmp(RAbstractStringVector v, int i, int j, byte naLast) {
            String x = v.getDataAt(i);
            String y = v.getDataAt(j);
            boolean nax = RRuntime.isNA(x);
            boolean nay = RRuntime.isNA(y);
            if (nax && nay) {
                return 0;
            }
            if (nax) {
                return naLast == RRuntime.LOGICAL_TRUE ? 1 : -1;
            }
            if (nay) {
                return naLast == RRuntime.LOGICAL_TRUE ? -1 : 1;
            }
            if (x.compareTo(y) < 0) {
                return -1;
            }
            if (x.compareTo(y) > 0) {
                return 1;
            }
            return 0;
        }

        @Specialization
        protected int ccmp(RAbstractComplexVector v, int i, int j, byte naLast) {
            RComplex x = v.getDataAt(i);
            RComplex y = v.getDataAt(j);
            // compare real parts
            boolean nax = RRuntime.isNA(x.getRealPart());
            boolean nay = RRuntime.isNA(y.getRealPart());
            if (nax && nay) {
                return 0;
            }
            if (nax) {
                return naLast == RRuntime.LOGICAL_TRUE ? 1 : -1;
            }
            if (nay) {
                return naLast == RRuntime.LOGICAL_TRUE ? -1 : 1;
            }
            if (x.getRealPart() < y.getRealPart()) {
                return -1;
            }
            if (x.getRealPart() > y.getRealPart()) {
                return 1;
            }

            // compare real parts
            nax = RRuntime.isNA(x.getImaginaryPart());
            nay = RRuntime.isNA(y.getImaginaryPart());
            if (nax && nay) {
                return 0;
            }
            if (nax) {
                return naLast == RRuntime.LOGICAL_TRUE ? 1 : -1;
            }
            if (nay) {
                return naLast == RRuntime.LOGICAL_TRUE ? -1 : 1;
            }
            if (x.getImaginaryPart() < y.getImaginaryPart()) {
                return -1;
            }
            if (x.getImaginaryPart() > y.getImaginaryPart()) {
                return 1;
            }
            return 0; // equal
        }

    }
}
