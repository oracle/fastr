/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995, 1996, 1997  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1995-2014, The R Core Team
 * Copyright (c) 2002-2008, The R Foundation
 * Copyright (c) 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.numericValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;
import static com.oracle.truffle.r.runtime.RError.Message.INVALID_LOGICAL;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import java.text.Collator;
import java.text.ParseException;
import java.text.RuleBasedCollator;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RPrecedenceBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.base.OrderNodeGen.CmpNodeGen;
import com.oracle.truffle.r.nodes.builtin.base.OrderNodeGen.IsAtomicNANodeGen;
import com.oracle.truffle.r.nodes.builtin.base.OrderNodeGen.OrderVector1NodeGen;
import com.oracle.truffle.r.nodes.builtin.base.SortFunctions.RadixSort;
import com.oracle.truffle.r.nodes.unary.CastToVectorNode;
import com.oracle.truffle.r.nodes.unary.CastToVectorNodeGen;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.closures.RClosures;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

@RBuiltin(name = "order", kind = INTERNAL, parameterNames = {"na.last", "decreasing", "..."}, behavior = PURE)
public abstract class Order extends RPrecedenceBuiltinNode {

    @Child private OrderVector1Node orderVector1Node;
    @Child private CastToVectorNode castVector;
    @Child private CastToVectorNode castVector2;
    @Child private CmpNode cmpNode;
    @Child private IsAtomicNA isNANode;

    private final BranchProfile error = BranchProfile.create();
    private final ConditionProfile notRemoveNAs = ConditionProfile.createBinaryProfile();

    /**
     * For use by {@link RadixSort}.
     */
    public abstract Object execute(byte naLast, boolean decreasing, RArgsValuesAndNames args);

    private static final int[] SINCS = {1073790977, 268460033, 67121153, 16783361, 4197377, 1050113, 262913, 65921, 16577, 4193, 1073, 281, 77, 23, 8, 1, 0};

    private RIntVector executeOrderVector1(RAbstractVector v, byte naLast, boolean dec) {
        return executeOrderVector1(v, naLast, dec, false);
    }

    private RIntVector executeOrderVector1(RAbstractVector v, byte naLast, boolean dec, boolean needsStringCollation) {
        int n = v.getLength();
        reportWork(n);

        RIntVector indxVec = RDataFactory.createIntVector(createIndexes(v, n, naLast), RDataFactory.COMPLETE_VECTOR);
        initOrderVector1(needsStringCollation).execute(indxVec, v, naLast, dec, null);
        int[] indx = indxVec.getDataWithoutCopying();
        for (int i = 0; i < indx.length; i++) {
            indx[i] = indx[i] + 1;
        }

        return RDataFactory.createIntVector(indx, RDataFactory.COMPLETE_VECTOR);
    }

    private int[] createIndexes(RAbstractVector v, int len, byte naLast) {
        if (notRemoveNAs.profile(!RRuntime.isNA(naLast) || v.isComplete())) {
            int[] result = new int[v.getLength()];
            for (int i = 0; i < result.length; i++) {
                result[i] = i;
            }
            return result;
        }

        // if naLast is NA, we should remove indexes of NA values
        if (isNANode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            isNANode = insert(IsAtomicNA.create());
        }

        int naCount = 0;
        for (int i = 0; i < len; i++) {
            if (isNANode.execute(v, i)) {
                naCount++;
            }
        }

        int[] result = new int[len - naCount];
        for (int i = 0, resultIdx = 0; i < len; i++) {
            if (!isNANode.execute(v, i)) {
                result[resultIdx++] = i;
            }
        }
        return result;
    }

    private OrderVector1Node initOrderVector1(boolean needsStringCollation) {
        if (orderVector1Node == null || needsStringCollation && !orderVector1Node.needsStringCollation) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            orderVector1Node = insert(OrderVector1NodeGen.create(needsStringCollation));
        }
        return orderVector1Node;
    }

    private RAbstractVector castVector(Object value) {
        if (castVector == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castVector = insert(CastToVectorNodeGen.create(false));
        }
        return (RAbstractVector) castVector.execute(value);
    }

    private RAbstractVector castVector2(Object value) {
        if (castVector2 == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castVector2 = insert(CastToVectorNodeGen.create(false));
        }
        return (RAbstractVector) castVector2.execute(value);
    }

    @Override
    protected void createCasts(CastBuilder casts) {
        casts.arg("na.last").mustBe(numericValue(), INVALID_LOGICAL, "na.last").asLogicalVector().findFirst();
        casts.arg("decreasing").mustBe(numericValue(), INVALID_LOGICAL, "decreasing").asLogicalVector().findFirst().map(toBoolean());
    }

    private int cmp(Object v, int i, int j, boolean naLast) {
        if (cmpNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            cmpNode = insert(CmpNodeGen.create());
        }
        return cmpNode.executeInt(v, i, j, naLast);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "noVec(args)")
    Object orderEmpty(byte naLastVec, boolean decVec, RArgsValuesAndNames args) {
        return RNull.instance;
    }

    @Specialization(guards = {"oneVec(args)", "isFirstIntegerPrecedence(args)"})
    Object orderInt(byte naLast, boolean decreasing, RArgsValuesAndNames args) {
        Object[] vectors = args.getArguments();
        RAbstractIntVector v = (RAbstractIntVector) castVector(vectors[0]);
        return executeOrderVector1(v, naLast, decreasing);
    }

    @Specialization(guards = {"oneVec(args)", "isFirstDoublePrecedence(args)"})
    Object orderDouble(byte naLast, boolean decreasing, RArgsValuesAndNames args) {
        Object[] vectors = args.getArguments();
        RAbstractDoubleVector v = (RAbstractDoubleVector) castVector(vectors[0]);
        return executeOrderVector1(v, naLast, decreasing);
    }

    @Specialization(guards = {"oneVec(args)", "isFirstLogicalPrecedence(args)"})
    Object orderLogical(byte naLast, boolean decreasing, RArgsValuesAndNames args) {
        Object[] vectors = args.getArguments();
        RAbstractIntVector v = RClosures.createLogicalToIntVector((RAbstractLogicalVector) castVector(vectors[0]));
        return executeOrderVector1(v, naLast, decreasing);
    }

    @Specialization(guards = {"oneVec(args)", "isFirstStringPrecedence(args)"})
    Object orderString(byte naLast, boolean decreasing, RArgsValuesAndNames args,
                    @Cached("create()") BranchProfile collationProfile) {
        Object[] vectors = args.getArguments();
        RAbstractStringVector v = (RAbstractStringVector) castVector(vectors[0]);
        int n = v.getLength();
        boolean needsCollation = false;
        outer: for (int i = 0; i < n; i++) {
            String str = v.getDataAt(i);
            for (int i2 = 0; i2 < str.length(); i2++) {
                char c = str.charAt(i2);
                if (c > 127) {
                    collationProfile.enter();
                    needsCollation = true;
                    break outer;
                }
            }
        }

        return executeOrderVector1(v, naLast, decreasing, needsCollation);
    }

    @Specialization(guards = {"oneVec(args)", "isFirstComplexPrecedence( args)"})
    Object orderComplex(byte naLast, boolean decreasing, RArgsValuesAndNames args) {
        Object[] vectors = args.getArguments();
        RAbstractComplexVector v = (RAbstractComplexVector) castVector(vectors[0]);
        return executeOrderVector1(v, naLast, decreasing);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"oneVec(args)", "isFirstListPrecedence( args)"})
    Object orderList(byte naLast, boolean decreasing, RArgsValuesAndNames args) {
        /*
         * Lists are not actually supported by GnuR but there is a corner case of a length < 2 list
         * that produces a result in GnuR and there is a unit test for that (when called via
         * .Internal)
         */
        RList list = (RList) args.getArgument(0);
        switch (list.getLength()) {
            case 0:
                return RDataFactory.createIntVector(0);
            case 1:
                return RDataFactory.createIntVectorFromScalar(1);
            default:
                throw RError.error(RError.NO_CALLER, RError.Message.UNIMPLEMENTED_TYPE_IN_FUNCTION, "list", "orderVector1");
        }
    }

    private int preprocessVectors(RArgsValuesAndNames args, ValueProfile lengthProfile) {
        Object[] vectors = args.getArguments();
        RAbstractVector v = castVector(vectors[0]);
        int n = v.getLength();
        reportWork(n);
        vectors[0] = v;
        int length = lengthProfile.profile(vectors.length);
        for (int i = 1; i < length; i++) {
            v = castVector2(vectors[i]);
            if (n != v.getLength()) {
                error.enter();
                throw RError.error(this, RError.Message.ARGUMENT_LENGTHS_DIFFER);
            }
            vectors[i] = v;
        }
        return n;
    }

    @Specialization(guards = {"!oneVec(args)", "!noVec(args)"})
    Object orderMulti(byte naLast, boolean decreasing, RArgsValuesAndNames args, //
                    @Cached("createEqualityProfile()") ValueProfile lengthProfile) {
        int n = preprocessVectors(args, lengthProfile);

        int[] indx = new int[n];
        for (int i = 0; i < indx.length; i++) {
            indx[i] = i;
        }
        orderVector(indx, args.getArguments(), RRuntime.fromLogical(naLast), decreasing);
        for (int i = 0; i < indx.length; i++) {
            indx[i] = indx[i] + 1;
        }

        return RDataFactory.createIntVector(indx, RDataFactory.COMPLETE_VECTOR);
    }

    private boolean greaterSub(int i, int j, Object[] vectors, boolean naLast, boolean dec) {
        int c = -1;
        for (int k = 0; k < vectors.length; k++) {
            RAbstractVector v = (RAbstractVector) vectors[k];
            c = cmp(v, i, j, naLast);
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

    private void orderVector(int[] indx, Object[] vectors, boolean naLast, boolean dec) {
        if (indx.length > 1) {

            int t = 0;
            for (; SINCS[t] > indx.length; t++) {
            }
            for (int h = SINCS[t]; t < 16; h = SINCS[++t]) {
                for (int i = h; i < indx.length; i++) {
                    int itmp = indx[i];
                    int j = i;
                    while (j >= h && greaterSub(indx[j - h], itmp, vectors, naLast, dec)) {
                        indx[j] = indx[j - h];
                        j -= h;
                    }
                    indx[j] = itmp;
                }
            }
        }
    }

    /**
     * Also used by {@link Rank}, where the "rho" parameter is not null. TODO handle S4 objects
     * (which involves rho)
     */
    abstract static class OrderVector1Node extends RBaseNode {
        private final ConditionProfile decProfile = ConditionProfile.createBinaryProfile();

        private final boolean needsStringCollation;

        protected OrderVector1Node(boolean needsStringCollation) {
            this.needsStringCollation = needsStringCollation;
        }

        public abstract Object execute(Object v, Object dv, byte naLast, boolean dec, Object rho);

        @Specialization
        protected Object orderVector1(RIntVector indxVec, RAbstractIntVector dv, byte naLast, boolean decreasing, Object rho) {
            if (indxVec.getLength() < 2) {
                return indxVec;
            }
            int[] indx = indxVec.getDataWithoutCopying();
            int lo = 0;
            int hi = indx.length - 1;
            if (rho == null) {
                int numNa = 0;
                if (!dv.isComplete() && !RRuntime.isNA(naLast)) {
                    boolean[] isNa = new boolean[indx.length];
                    for (int i = 0; i < isNa.length; i++) {
                        if (RRuntime.isNA(dv.getDataAt(i))) {
                            isNa[i] = true;
                            numNa++;
                        }
                    }

                    if (numNa > 0) {
                        if (!RRuntime.fromLogical(naLast)) {
                            for (int i = 0; i < isNa.length; i++) {
                                isNa[i] = !isNa[i];
                            }
                        }
                        sortNA(indx, isNa, lo, hi);
                        if (RRuntime.fromLogical(naLast)) {
                            hi -= numNa;
                        } else {
                            lo += numNa;
                        }
                    }
                }
            }

            sort(indx, dv, lo, hi, decreasing);
            return indxVec;
        }

        @Specialization
        protected Object orderVector1(RIntVector indxVec, RAbstractDoubleVector dv, byte naLast, boolean decreasing, Object rho) {
            if (indxVec.getLength() < 2) {
                return indxVec;
            }
            int[] indx = indxVec.getDataWithoutCopying();
            int lo = 0;
            int hi = indx.length - 1;
            if (rho == null && !RRuntime.isNA(naLast)) {
                int numNa = 0;
                boolean[] isNa = new boolean[indx.length];
                for (int i = 0; i < isNa.length; i++) {
                    if (RRuntime.isNAorNaN(dv.getDataAt(i))) {
                        isNa[i] = true;
                        numNa++;
                    }
                }

                if (numNa > 0) {
                    if (!RRuntime.fromLogical(naLast)) {
                        for (int i = 0; i < isNa.length; i++) {
                            isNa[i] = !isNa[i];
                        }
                    }
                    sortNA(indx, isNa, lo, hi);
                    if (RRuntime.fromLogical(naLast)) {
                        hi -= numNa;
                    } else {
                        lo += numNa;
                    }
                }
            }

            sort(indx, dv, lo, hi, decreasing);
            return indxVec;
        }

        @Specialization
        protected Object orderVector1(RIntVector indxVec, RAbstractStringVector dv, byte naLast, boolean decreasing, Object rho) {
            if (indxVec.getLength() < 2) {
                return indxVec;
            }
            int[] indx = indxVec.getDataWithoutCopying();
            int lo = 0;
            int hi = indx.length - 1;
            if (rho == null) {
                int numNa = 0;
                if (!dv.isComplete() && !RRuntime.isNA(naLast)) {
                    boolean[] isNa = new boolean[indx.length];
                    for (int i = 0; i < isNa.length; i++) {
                        if (RRuntime.isNA(dv.getDataAt(i))) {
                            isNa[i] = true;
                            numNa++;
                        }
                    }

                    if (numNa > 0) {
                        if (!RRuntime.fromLogical(naLast)) {
                            for (int i = 0; i < isNa.length; i++) {
                                isNa[i] = !isNa[i];
                            }
                        }
                        sortNA(indx, isNa, lo, hi);
                        if (RRuntime.fromLogical(naLast)) {
                            hi -= numNa;
                        } else {
                            lo += numNa;
                        }
                    }
                }
            }

            sort(indx, dv, lo, hi, decreasing);
            return indxVec;
        }

        @Specialization
        protected Object orderVector1(RIntVector indxVec, RAbstractComplexVector dv, byte naLast, boolean decreasing, Object rho) {
            if (indxVec.getLength() < 2) {
                return indxVec;
            }
            int[] indx = indxVec.getDataWithoutCopying();
            int lo = 0;
            int hi = indx.length - 1;
            if (rho == null) {
                int numNa = 0;
                if (!dv.isComplete() && !RRuntime.isNA(naLast)) {
                    boolean[] isNa = new boolean[indx.length];
                    for (int i = 0; i < isNa.length; i++) {
                        if (RRuntime.isNA(dv.getDataAt(i))) {
                            isNa[i] = true;
                            numNa++;
                        }
                    }

                    if (numNa > 0) {
                        if (!RRuntime.fromLogical(naLast)) {
                            for (int i = 0; i < isNa.length; i++) {
                                isNa[i] = !isNa[i];
                            }
                        }
                        sortNA(indx, isNa, lo, hi);
                        if (RRuntime.fromLogical(naLast)) {
                            hi -= numNa;
                        } else {
                            lo += numNa;
                        }
                    }
                }
            }

            sort(indx, dv, lo, hi, decreasing);
            return indxVec;
        }

        @SuppressWarnings("unused")
        @Specialization
        protected Object orderVector1(RIntVector indxVec, RList dv, byte naLast, boolean decreasing, Object rho) {
            /* Only needed to satisfy .Internal(rank) in unit test */
            return indxVec;
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
            Collator collator = createCollator();
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
                        int c = compareString(collator, dv.getDataAt(a), dv.getDataAt(b));
                        if (decProfile.profile(dec)) {
                            if (!(c < 0 || (c == 0 && a > b))) {
                                break;
                            }
                        } else {
                            if (!(c > 0 || (c == 0 && a > b))) {
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

        private int compareString(Collator collator, String dataAt, String dataAt2) {
            if (needsStringCollation) {
                return compare(collator, dataAt, dataAt2);
            } else {
                return dataAt.compareToIgnoreCase(dataAt2);
            }
        }

        @TruffleBoundary
        private static int compare(Collator collator, String dataAt, String dataAt2) {
            return collator.compare(dataAt, dataAt2);
        }

        @TruffleBoundary
        private Collator createCollator() {
            if (!needsStringCollation) {
                return null;
            }
            // add rule for space before '_'
            Collator collator = Collator.getInstance();
            String rules = ((RuleBasedCollator) collator).getRules();
            try {
                return new RuleBasedCollator(rules.replaceAll("<'\u005f'", "<' '<'\u005f'"));
            } catch (ParseException e) {
                throw RInternalError.shouldNotReachHere(e);
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

    protected boolean isFirstListPrecedence(RArgsValuesAndNames args) {
        return isListPrecedence(args.getArgument(0));
    }

    protected boolean noVec(RArgsValuesAndNames args) {
        return args.isEmpty();
    }

    protected boolean oneVec(RArgsValuesAndNames args) {
        return args.getLength() == 1;
    }

    abstract static class IsAtomicNA extends Node {
        public abstract boolean execute(RAbstractVector vec, int idx);

        public static IsAtomicNA create() {
            return IsAtomicNANodeGen.create();
        }

        @Specialization
        protected boolean doInt(RAbstractIntVector v, int idx) {
            return RRuntime.isNA(v.getDataAt(idx));
        }

        @Specialization
        protected boolean doDouble(RAbstractDoubleVector v, int idx) {
            return RRuntime.isNA(v.getDataAt(idx));
        }

        @Specialization
        protected boolean doString(RAbstractStringVector v, int idx) {
            return RRuntime.isNA(v.getDataAt(idx));
        }

        @Specialization
        protected boolean doComplex(RAbstractComplexVector v, int idx) {
            return RRuntime.isNA(v.getDataAt(idx));
        }
    }

    /**
     * Also used by {@link Rank}. *
     */
    abstract static class CmpNode extends RBaseNode {

        public abstract int executeInt(Object v, int i, int j, boolean naLast);

        @Specialization
        protected int lcmp(RAbstractLogicalVector v, int i, int j, boolean naLast) {
            byte x = v.getDataAt(i);
            byte y = v.getDataAt(j);
            boolean nax = RRuntime.isNA(x);
            boolean nay = RRuntime.isNA(y);
            if (nax && nay) {
                return 0;
            }
            if (nax) {
                return naLast ? 1 : -1;
            }
            if (nay) {
                return naLast ? -1 : 1;
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
        protected int icmp(RAbstractIntVector v, int i, int j, boolean naLast) {
            int x = v.getDataAt(i);
            int y = v.getDataAt(j);
            boolean nax = RRuntime.isNA(x);
            boolean nay = RRuntime.isNA(y);
            if (nax && nay) {
                return 0;
            }
            if (nax) {
                return naLast ? 1 : -1;
            }
            if (nay) {
                return naLast ? -1 : 1;
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
        protected int rcmp(RAbstractDoubleVector v, int i, int j, boolean naLast) {
            double x = v.getDataAt(i);
            double y = v.getDataAt(j);
            boolean nax = RRuntime.isNAorNaN(x);
            boolean nay = RRuntime.isNAorNaN(y);
            if (nax && nay) {
                return 0;
            }
            if (nax) {
                return naLast ? 1 : -1;
            }
            if (nay) {
                return naLast ? -1 : 1;
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
        protected int scmp(RAbstractStringVector v, int i, int j, boolean naLast) {
            String x = v.getDataAt(i);
            String y = v.getDataAt(j);
            boolean nax = RRuntime.isNA(x);
            boolean nay = RRuntime.isNA(y);
            if (nax && nay) {
                return 0;
            }
            if (nax) {
                return naLast ? 1 : -1;
            }
            if (nay) {
                return naLast ? -1 : 1;
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
        protected int ccmp(RAbstractComplexVector v, int i, int j, boolean naLast) {
            RComplex x = v.getDataAt(i);
            RComplex y = v.getDataAt(j);
            // compare real parts
            boolean nax = RRuntime.isNA(x.getRealPart());
            boolean nay = RRuntime.isNA(y.getRealPart());
            if (nax && nay) {
                return 0;
            }
            if (nax) {
                return naLast ? 1 : -1;
            }
            if (nay) {
                return naLast ? -1 : 1;
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
                return naLast ? 1 : -1;
            }
            if (nay) {
                return naLast ? -1 : 1;
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
