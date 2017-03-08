/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.logicalFalse;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.logicalValue;
import static com.oracle.truffle.r.runtime.RError.Message.MATCH_VECTOR_ARGS;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import java.util.Arrays;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.base.MatchNodeGen.MatchInternalNodeGen;
import com.oracle.truffle.r.nodes.helpers.InheritsCheckNode;
import com.oracle.truffle.r.nodes.helpers.RFactorNodes;
import com.oracle.truffle.r.nodes.unary.CastStringNode;
import com.oracle.truffle.r.nodes.unary.CastStringNodeGen;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.closures.RClosures;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractRawVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
import com.oracle.truffle.r.runtime.ops.na.NAProfile;

/*
 * TODO: handle "incomparables" parameter.
 */
@RBuiltin(name = "match", kind = INTERNAL, parameterNames = {"x", "table", "nomatch", "incomparables"}, behavior = PURE)
public abstract class Match extends RBuiltinNode {

    protected abstract Object executeRIntVector(Object x, Object table, Object noMatch, Object incomparables);

    @Child private CastStringNode castString;

    @Child private Match matchRecursive;

    static {
        Casts casts = new Casts(Match.class);
        // TODO initially commented out because of use of scalars, the commented out version
        // converted to new cast pipelines API

        // casts.arg("x").allowNull().mustBe(abstractVectorValue(), SHOW_CALLER,
        // MATCH_VECTOR_ARGS).asVectorPreserveAttrs(true);
        // casts.arg("table").allowNull().mustBe(abstractVectorValue()).asVectorPreserveAttrs(true);
        casts.arg("nomatch").asIntegerVector().findFirst();
        casts.arg("incomparables").defaultError(Message.GENERIC, "usage of 'incomparables' in match not implemented").allowNull().mustBe(logicalValue()).asLogicalVector().findFirst().mustBe(
                        logicalFalse());
    }

    protected static MatchInternalNode createInternal() {
        return MatchInternalNodeGen.create();
    }

    @Specialization
    @SuppressWarnings("unused")
    protected RIntVector match(RNull x, RNull table, int nomatch, Object incomparables) {
        return RDataFactory.createIntVector(0);
    }

    @Specialization
    @SuppressWarnings("unused")
    protected RIntVector match(RNull x, RAbstractVector table, int nomatch, Object incomparables) {
        return RDataFactory.createIntVector(0);
    }

    @Specialization
    @SuppressWarnings("unused")
    protected RIntVector match(RAbstractVector x, RNull table, int nomatch, Object incomparables,
                    @Cached("createBinaryProfile()") ConditionProfile na) {
        int[] data = new int[x.getLength()];
        Arrays.fill(data, nomatch);
        return RDataFactory.createIntVector(data, na.profile(!RRuntime.isNA(nomatch)));
    }

    @Child private InheritsCheckNode factorInheritsCheck = new InheritsCheckNode(RRuntime.CLASS_FACTOR);

    protected boolean isFactor(Object o) {
        return factorInheritsCheck.execute(o);
    }

    @Specialization(guards = {"isFactor(x)", "isFactor(table)"})
    protected Object matchFactor(RAbstractIntVector x, RAbstractIntVector table, int nomatch, @SuppressWarnings("unused") Object incomparables,
                    @Cached("create()") RFactorNodes.GetLevels getLevelsNode,
                    @Cached("createInternal()") MatchInternalNode match) {
        return match.execute(RClosures.createFactorToVector(x, true, getLevelsNode.execute(x)),
                        RClosures.createFactorToVector(table, true, getLevelsNode.execute(table)), nomatch);
    }

    @Specialization(guards = {"isFactor(x)", "!isFactor(table)"})
    protected Object matchFactor(RAbstractIntVector x, RAbstractVector table, int nomatch, @SuppressWarnings("unused") Object incomparables,
                    @Cached("create()") RFactorNodes.GetLevels getLevelsNode,
                    @Cached("createInternal()") MatchInternalNode match) {
        return match.execute(RClosures.createFactorToVector(x, true, getLevelsNode.execute(x)), table, nomatch);
    }

    @Specialization(guards = {"!isFactor(x)", "isFactor(table)"})
    protected Object matchFactor(RAbstractVector x, RAbstractIntVector table, int nomatch, @SuppressWarnings("unused") Object incomparables,
                    @Cached("create()") RFactorNodes.GetLevels getLevelsNode,
                    @Cached("createInternal()") MatchInternalNode match) {
        return match.execute(x, RClosures.createFactorToVector(table, true, getLevelsNode.execute(table)), nomatch);
    }

    @Specialization(guards = {"!isRAbstractIntVector(table) || !isFactor(table)"})
    protected Object matchList(RAbstractListVector x, RAbstractVector table, int nomatchObj, @SuppressWarnings("unused") Object incomparables,
                    @Cached("create()") CastStringNode cast,
                    @Cached("createInternal()") MatchInternalNode match) {
        return match.execute((RAbstractVector) cast.execute(x), table, nomatchObj);
    }

    @Specialization(guards = {"!isRAbstractListVector(x)", "!isRAbstractIntVector(x) || !isFactor(x)", "!isRAbstractIntVector(table) || !isFactor(table)"})
    protected Object match(RAbstractVector x, RAbstractVector table, int noMatch, @SuppressWarnings("unused") Object incomparables,
                    @Cached("createInternal()") MatchInternalNode match) {
        return match.execute(x, table, noMatch);
    }

    @Fallback
    @SuppressWarnings("unused")
    protected RIntVector match(Object x, Object table, Object nomatch, Object incomparables) {
        throw error(MATCH_VECTOR_ARGS);
    }

    protected abstract static class MatchInternalNode extends RBaseNode {

        private static final int TABLE_SIZE_FACTOR = 10;

        protected abstract Object execute(RAbstractVector x, RAbstractVector table, int noMatch);

        @Child private CastStringNode castString;

        @Child private MatchInternalNode matchRecursive;

        private final ConditionProfile bigTableProfile = ConditionProfile.createBinaryProfile();

        private RAbstractStringVector castString(RAbstractVector operand) {
            if (castString == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                castString = insert(CastStringNodeGen.create(false, false, false));
            }
            return (RAbstractStringVector) castString.execute(operand);
        }

        @Specialization
        protected RIntVector match(RAbstractIntVector x, RAbstractIntVector table, int nomatch) {
            int[] result = initResult(x.getLength(), nomatch);
            boolean matchAll = true;
            NonRecursiveHashMapInt hashTable;
            if (bigTableProfile.profile(table.getLength() > (x.getLength() * TABLE_SIZE_FACTOR))) {
                hashTable = new NonRecursiveHashMapInt(x.getLength());
                NonRecursiveHashSetInt hashSet = new NonRecursiveHashSetInt(x.getLength());
                for (int i = 0; i < result.length; i++) {
                    hashSet.add(x.getDataAt(i));
                }
                for (int i = table.getLength() - 1; i >= 0; i--) {
                    int val = table.getDataAt(i);
                    if (hashSet.contains(val)) {
                        hashTable.put(val, i);
                    }
                }
            } else {
                hashTable = new NonRecursiveHashMapInt(table.getLength());
                for (int i = table.getLength() - 1; i >= 0; i--) {
                    hashTable.put(table.getDataAt(i), i);
                }
            }
            for (int i = 0; i < result.length; i++) {
                int xx = x.getDataAt(i);
                int index = hashTable.get(xx);
                if (index != -1) {
                    result[i] = index + 1;
                } else {
                    matchAll = false;
                }
            }
            return RDataFactory.createIntVector(result, setCompleteState(matchAll, nomatch));
        }

        @Specialization
        protected RIntVector match(RAbstractDoubleVector x, RAbstractIntVector table, int nomatch) {
            int[] result = initResult(x.getLength(), nomatch);
            boolean matchAll = true;
            NonRecursiveHashMapDouble hashTable;
            if (bigTableProfile.profile(table.getLength() > (x.getLength() * TABLE_SIZE_FACTOR))) {
                hashTable = new NonRecursiveHashMapDouble(x.getLength());
                NonRecursiveHashSetDouble hashSet = new NonRecursiveHashSetDouble(x.getLength());
                for (int i = 0; i < result.length; i++) {
                    hashSet.add(x.getDataAt(i));
                }
                for (int i = table.getLength() - 1; i >= 0; i--) {
                    int val = table.getDataAt(i);
                    if (hashSet.contains(RRuntime.int2double(val))) {
                        hashTable.put(RRuntime.int2double(val), i);
                    }
                }
            } else {
                hashTable = new NonRecursiveHashMapDouble(table.getLength());
                for (int i = table.getLength() - 1; i >= 0; i--) {
                    hashTable.put(RRuntime.int2double(table.getDataAt(i)), i);
                }
            }
            for (int i = 0; i < result.length; i++) {
                double xx = x.getDataAt(i);
                int index = hashTable.get(xx);
                if (index != -1) {
                    result[i] = index + 1;
                } else {
                    matchAll = false;
                }
            }
            return RDataFactory.createIntVector(result, setCompleteState(matchAll, nomatch));
        }

        @Specialization
        protected RIntVector match(RAbstractIntVector x, RAbstractDoubleVector table, int nomatch) {
            int[] result = initResult(x.getLength(), nomatch);
            boolean matchAll = true;
            NonRecursiveHashMapInt hashTable;
            if (bigTableProfile.profile(table.getLength() > (x.getLength() * TABLE_SIZE_FACTOR))) {
                hashTable = new NonRecursiveHashMapInt(x.getLength());
                NonRecursiveHashSetInt hashSet = new NonRecursiveHashSetInt(x.getLength());
                for (int i = 0; i < result.length; i++) {
                    hashSet.add(x.getDataAt(i));
                }
                for (int i = table.getLength() - 1; i >= 0; i--) {
                    double val = table.getDataAt(i);
                    if (RRuntime.isNA(val) && hashSet.contains(RRuntime.INT_NA)) {
                        hashTable.put(RRuntime.INT_NA, i);
                    } else if (val == (int) val && hashSet.contains((int) val)) {
                        hashTable.put((int) val, i);
                    }
                }
            } else {
                hashTable = new NonRecursiveHashMapInt(table.getLength());
                for (int i = table.getLength() - 1; i >= 0; i--) {
                    double xx = table.getDataAt(i);
                    if (RRuntime.isNA(xx)) {
                        hashTable.put(RRuntime.INT_NA, i);
                    } else if (xx == (int) xx) {
                        hashTable.put((int) xx, i);
                    }
                }
            }
            for (int i = 0; i < result.length; i++) {
                int xx = x.getDataAt(i);
                int index = hashTable.get(xx);
                if (index != -1) {
                    result[i] = index + 1;
                } else {
                    matchAll = false;
                }
            }
            return RDataFactory.createIntVector(result, setCompleteState(matchAll, nomatch));
        }

        @Specialization
        protected RIntVector match(RAbstractDoubleVector x, RAbstractDoubleVector table, int nomatch) {
            int[] result = initResult(x.getLength(), nomatch);
            boolean matchAll = true;
            NonRecursiveHashMapDouble hashTable;
            if (bigTableProfile.profile(table.getLength() > (x.getLength() * TABLE_SIZE_FACTOR))) {
                hashTable = new NonRecursiveHashMapDouble(x.getLength());
                NonRecursiveHashSetDouble hashSet = new NonRecursiveHashSetDouble(x.getLength());
                for (int i = 0; i < result.length; i++) {
                    hashSet.add(x.getDataAt(i));
                }
                for (int i = table.getLength() - 1; i >= 0; i--) {
                    double val = table.getDataAt(i);
                    if (hashSet.contains(val)) {
                        hashTable.put(val, i);
                    }
                }
            } else {
                hashTable = new NonRecursiveHashMapDouble(table.getLength());
                for (int i = table.getLength() - 1; i >= 0; i--) {
                    hashTable.put(table.getDataAt(i), i);
                }
            }
            for (int i = 0; i < result.length; i++) {
                double xx = x.getDataAt(i);
                int index = hashTable.get(xx);
                if (index != -1) {
                    result[i] = index + 1;
                } else {
                    matchAll = false;
                }
            }
            return RDataFactory.createIntVector(result, setCompleteState(matchAll, nomatch));
        }

        @Specialization
        protected RIntVector match(RAbstractIntVector x, RAbstractLogicalVector table, int nomatch) {
            int[] result = initResult(x.getLength(), nomatch);
            boolean matchAll = true;
            int[] values = {RRuntime.LOGICAL_TRUE, RRuntime.LOGICAL_FALSE, RRuntime.LOGICAL_NA};
            int[] indexes = new int[values.length];
            for (int i = 0; i < values.length; i++) {
                byte value = (byte) values[i];
                for (int j = 0; j < table.getLength(); j++) {
                    if (table.getDataAt(j) == value) {
                        indexes[i] = j + 1;
                        break;
                    }
                }
                values[i] = RRuntime.logical2int(value);
            }
            for (int i = 0; i < result.length; i++) {
                int xx = x.getDataAt(i);
                boolean match = false;
                for (int j = 0; j < values.length; j++) {
                    if (xx == values[j] && indexes[j] != 0) {
                        result[i] = indexes[j];
                        match = true;
                        break;
                    }
                }
                matchAll &= match;
            }
            return RDataFactory.createIntVector(result, setCompleteState(matchAll, nomatch));
        }

        @Specialization(guards = "x.getLength() == 1")
        protected int matchSizeOne(RAbstractStringVector x, RAbstractStringVector table, int nomatch,
                        @Cached("create()") NAProfile naProfile,
                        @Cached("create()") BranchProfile foundProfile,
                        @Cached("create()") BranchProfile notFoundProfile) {
            String element = x.getDataAt(0);
            int length = table.getLength();
            if (naProfile.isNA(element)) {
                for (int i = 0; i < length; i++) {
                    if (RRuntime.isNA(table.getDataAt(i))) {
                        foundProfile.enter();
                        return i + 1;
                    }
                }
            } else {
                for (int i = 0; i < length; i++) {
                    if (element.equals(table.getDataAt(i))) {
                        foundProfile.enter();
                        return i + 1;
                    }
                }
            }
            notFoundProfile.enter();
            return nomatch;
        }

        @Specialization
        protected RIntVector match(RAbstractStringVector x, RAbstractStringVector table, int nomatch) {
            int[] result = initResult(x.getLength(), nomatch);
            boolean matchAll = true;
            NonRecursiveHashMapCharacter hashTable;
            if (bigTableProfile.profile(table.getLength() > (x.getLength() * TABLE_SIZE_FACTOR))) {
                hashTable = new NonRecursiveHashMapCharacter(x.getLength());
                NonRecursiveHashSetCharacter hashSet = new NonRecursiveHashSetCharacter(x.getLength());
                for (int i = 0; i < result.length; i++) {
                    hashSet.add(x.getDataAt(i));
                }
                for (int i = table.getLength() - 1; i >= 0; i--) {
                    String val = table.getDataAt(i);
                    if (hashSet.contains(val)) {
                        hashTable.put(val, i);
                    }
                }
            } else {
                hashTable = new NonRecursiveHashMapCharacter(table.getLength());
                for (int i = table.getLength() - 1; i >= 0; i--) {
                    hashTable.put(table.getDataAt(i), i);
                }
            }
            for (int i = 0; i < result.length; i++) {
                String xx = x.getDataAt(i);
                int index = hashTable.get(xx);
                if (index != -1) {
                    result[i] = index + 1;
                } else {
                    matchAll = false;
                }
            }
            return RDataFactory.createIntVector(result, setCompleteState(matchAll, nomatch));
        }

        @Specialization
        protected RIntVector match(RAbstractLogicalVector x, RAbstractStringVector table, int nomatch,
                        @Cached("createBinaryProfile()") ConditionProfile isNAProfile) {
            return match((RAbstractStringVector) x.castSafe(RType.Character, isNAProfile), table, nomatch);
        }

        @Specialization
        protected RIntVector match(RAbstractRawVector x, RAbstractIntVector table, int nomatch,
                        @Cached("createBinaryProfile()") ConditionProfile isNAProfile) {
            return match((RAbstractStringVector) x.castSafe(RType.Character, isNAProfile), (RAbstractStringVector) table.castSafe(RType.Character, isNAProfile), nomatch);
        }

        @Specialization
        protected RIntVector match(RAbstractIntVector x, RAbstractStringVector table, int nomatch,
                        @Cached("createBinaryProfile()") ConditionProfile isNAProfile) {
            return match((RAbstractStringVector) x.castSafe(RType.Character, isNAProfile), table, nomatch);
        }

        @Specialization
        protected RIntVector match(RAbstractDoubleVector x, RAbstractStringVector table, int nomatch,
                        @Cached("createBinaryProfile()") ConditionProfile isNAProfile) {
            return match((RAbstractStringVector) x.castSafe(RType.Character, isNAProfile), table, nomatch);
        }

        @Specialization
        protected RIntVector match(RAbstractLogicalVector x, RAbstractLogicalVector table, int nomatch) {
            int[] result = initResult(x.getLength(), nomatch);
            boolean matchAll = true;
            byte[] values = {RRuntime.LOGICAL_TRUE, RRuntime.LOGICAL_FALSE, RRuntime.LOGICAL_NA};
            int[] indexes = new int[values.length];
            for (int i = 0; i < values.length; i++) {
                byte value = values[i];
                for (int j = 0; j < table.getLength(); j++) {
                    if (table.getDataAt(j) == value) {
                        indexes[i] = j + 1;
                        break;
                    }
                }
            }
            for (int i = 0; i < result.length; i++) {
                byte xx = x.getDataAt(i);
                boolean match = false;
                for (int j = 0; j < values.length; j++) {
                    if (xx == values[j] && indexes[j] != 0) {
                        result[i] = indexes[j];
                        match = true;
                        break;
                    }
                }
                matchAll &= match;
            }
            return RDataFactory.createIntVector(result, setCompleteState(matchAll, nomatch));
        }

        @Specialization(guards = "!isRAbstractStringVector(table)")
        protected RIntVector match(RAbstractStringVector x, RAbstractVector table, int nomatch) {
            int[] result = initResult(x.getLength(), nomatch);
            boolean matchAll = true;
            RAbstractStringVector stringTable = castString(table);
            NonRecursiveHashMapCharacter hashTable = new NonRecursiveHashMapCharacter(table.getLength());
            for (int i = table.getLength() - 1; i >= 0; i--) {
                hashTable.put(stringTable.getDataAt(i), i);
            }
            for (int i = 0; i < result.length; i++) {
                String xx = x.getDataAt(i);
                int index = hashTable.get(xx);
                if (index != -1) {
                    result[i] = index + 1;
                } else {
                    matchAll = false;
                }
            }
            return RDataFactory.createIntVector(result, setCompleteState(matchAll, nomatch));
        }

        @Specialization
        protected RIntVector match(RAbstractComplexVector x, RAbstractComplexVector table, int nomatch) {
            int[] result = initResult(x.getLength(), nomatch);
            boolean matchAll = true;
            NonRecursiveHashMapComplex hashTable;
            if (bigTableProfile.profile(table.getLength() > (x.getLength() * TABLE_SIZE_FACTOR))) {
                hashTable = new NonRecursiveHashMapComplex(x.getLength());
                NonRecursiveHashSetComplex hashSet = new NonRecursiveHashSetComplex(x.getLength());
                for (int i = 0; i < result.length; i++) {
                    hashSet.add(x.getDataAt(i));
                }
                for (int i = table.getLength() - 1; i >= 0; i--) {
                    RComplex val = table.getDataAt(i);
                    if (hashSet.contains(val)) {
                        hashTable.put(val, i);
                    }
                }
            } else {
                hashTable = new NonRecursiveHashMapComplex(table.getLength());
                for (int i = table.getLength() - 1; i >= 0; i--) {
                    hashTable.put(table.getDataAt(i), i);
                }
            }
            for (int i = 0; i < result.length; i++) {
                RComplex xx = x.getDataAt(i);
                int index = hashTable.get(xx);
                if (index != -1) {
                    result[i] = index + 1;
                } else {
                    matchAll = false;
                }
            }
            return RDataFactory.createIntVector(result, setCompleteState(matchAll, nomatch));
        }

        private static int[] initResult(int length, int nomatch) {
            int[] result = new int[length];
            Arrays.fill(result, nomatch);
            return result;
        }

        /**
         * Set the "complete" status. If {@code nomatch} is not NA (uncommon), then the result
         * vector is always COMPLETE, otherwise it is INCOMPLETE unless everything matched.
         */
        private static boolean setCompleteState(boolean matchAll, int nomatch) {
            return nomatch != RRuntime.INT_NA || matchAll ? RDataFactory.COMPLETE_VECTOR : RDataFactory.INCOMPLETE_VECTOR;
        }

        // simple implementations of non-recursive hash-maps to enable compilation
        // TODO: consider replacing with a more efficient library implementation

        private abstract static class NonRecursiveHashMap {

            protected final int[] values;
            protected int naValue;

            protected NonRecursiveHashMap(int entryCount) {
                int capacity = Math.max(entryCount * 3 / 2, 1);
                values = new int[Integer.highestOneBit(capacity) << 1];
            }

            protected int index(int hash) {
                // Multiply by -127
                return ((hash << 1) - (hash << 8)) & (values.length - 1);
            }
        }

        private static final class NonRecursiveHashMapCharacter extends NonRecursiveHashMap {

            private final String[] keys;

            NonRecursiveHashMapCharacter(int approxCapacity) {
                super(approxCapacity);
                keys = new String[values.length];
            }

            public boolean put(String key, int value) {
                assert value >= 0;
                if (RRuntime.isNA(key)) {
                    boolean ret = naValue == 0;
                    naValue = value + 1;
                    return ret;
                } else {
                    int ind = index(key.hashCode());
                    while (true) {
                        if (values[ind] == 0) {
                            keys[ind] = key;
                            values[ind] = value + 1;
                            return false;
                        } else if (key.equals(keys[ind])) {
                            values[ind] = value + 1;
                            return true;
                        } else {
                            ind++;
                            if (ind == values.length) {
                                ind = 0;
                            }
                        }
                    }
                }
            }

            public int get(String key) {
                if (RRuntime.isNA(key)) {
                    return naValue - 1;
                } else {
                    int ind = index(key.hashCode());
                    int firstInd = ind;
                    while (true) {
                        if (key.equals(keys[ind])) {
                            return values[ind] - 1;
                        } else {
                            ind++;
                            if (ind == values.length) {
                                ind = 0;
                            }
                            if (ind == firstInd || values[ind] == 0) {
                                return -1;
                            }
                        }
                    }
                }
            }
        }

        private static final class NonRecursiveHashMapComplex extends NonRecursiveHashMap {

            private final RComplex[] keys;

            NonRecursiveHashMapComplex(int approxCapacity) {
                super(approxCapacity);
                keys = new RComplex[values.length];
            }

            public boolean put(RComplex key, int value) {
                assert value >= 0;
                if (RRuntime.isNA(key)) {
                    boolean ret = naValue == 0;
                    naValue = value + 1;
                    return ret;
                } else {
                    int ind = index(key.hashCode());
                    while (true) {
                        if (values[ind] == 0) {
                            keys[ind] = key;
                            values[ind] = value + 1;
                            return false;
                        } else if (key.equals(keys[ind])) {
                            values[ind] = value + 1;
                            return true;
                        } else {
                            ind++;
                            if (ind == values.length) {
                                ind = 0;
                            }
                        }
                    }
                }
            }

            public int get(RComplex key) {
                if (RRuntime.isNA(key)) {
                    return naValue - 1;
                } else {
                    int ind = index(key.hashCode());
                    int firstInd = ind;
                    while (true) {
                        if (key.equals(keys[ind])) {
                            return values[ind] - 1;
                        } else {
                            ind++;
                            if (ind == values.length) {
                                ind = 0;
                            }
                            if (ind == firstInd || values[ind] == 0) {
                                return -1;
                            }
                        }
                    }
                }
            }
        }

        private static final class NonRecursiveHashMapDouble extends NonRecursiveHashMap {

            private final double[] keys;
            private int nanValue;

            NonRecursiveHashMapDouble(int approxCapacity) {
                super(approxCapacity);
                keys = new double[values.length];
                Arrays.fill(keys, RRuntime.DOUBLE_NA);
            }

            public boolean put(double key, int value) {
                assert value >= 0;
                if (RRuntime.isNA(key)) {
                    boolean ret = naValue == 0;
                    naValue = value + 1;
                    return ret;
                } else if (Double.isNaN(key)) {
                    boolean ret = nanValue == 0;
                    nanValue = value + 1;
                    return ret;
                } else {
                    int ind = index(Double.hashCode(key));
                    while (true) {
                        if (values[ind] == 0) {
                            keys[ind] = key;
                            values[ind] = value + 1;
                            return false;
                        } else if (key == keys[ind]) {
                            values[ind] = value + 1;
                            return true;
                        } else {
                            ind++;
                            if (ind == values.length) {
                                ind = 0;
                            }
                        }
                    }
                }
            }

            public int get(double key) {
                if (RRuntime.isNA(key)) {
                    return naValue - 1;
                } else if (Double.isNaN(key)) {
                    return nanValue - 1;
                } else {
                    int ind = index(Double.hashCode(key));
                    int firstInd = ind;
                    while (true) {
                        if (key == keys[ind]) {
                            return values[ind] - 1;
                        } else {
                            ind++;
                            if (ind == values.length) {
                                ind = 0;
                            }
                            if (ind == firstInd || values[ind] == 0) {
                                return -1;
                            }
                        }
                    }
                }
            }
        }

        private static final class NonRecursiveHashMapInt extends NonRecursiveHashMap {

            private final int[] keys;

            NonRecursiveHashMapInt(int approxCapacity) {
                super(approxCapacity);
                keys = new int[values.length];
                Arrays.fill(keys, RRuntime.INT_NA);
            }

            public boolean put(int key, int value) {
                assert value >= 0;
                if (RRuntime.isNA(key)) {
                    boolean ret = naValue == 0;
                    naValue = value + 1;
                    return ret;
                } else {
                    int ind = index(Integer.hashCode(key));
                    while (true) {
                        if (values[ind] == 0) {
                            keys[ind] = key;
                            values[ind] = value + 1;
                            return false;
                        } else if (key == keys[ind]) {
                            values[ind] = value + 1;
                            return true;
                        } else {
                            ind++;
                            if (ind == values.length) {
                                ind = 0;
                            }
                        }
                    }
                }
            }

            public int get(int key) {
                if (RRuntime.isNA(key)) {
                    return naValue - 1;
                } else {
                    int ind = index(Integer.hashCode(key));
                    int firstInd = ind;
                    while (true) {
                        if (key == keys[ind]) {
                            return values[ind] - 1;
                        } else {
                            ind++;
                            if (ind == values.length) {
                                ind = 0;
                            }
                            if (ind == firstInd || values[ind] == 0) {
                                return -1;
                            }
                        }
                    }
                }
            }
        }

        private static class NonRecursiveHashSetInt {
            private final NonRecursiveHashMapInt map;

            NonRecursiveHashSetInt(int approxCapacity) {
                map = new NonRecursiveHashMapInt(approxCapacity);
            }

            public boolean add(int value) {
                return map.put(value, 1);
            }

            public boolean contains(int value) {
                return map.get(value) == 1 ? true : false;
            }
        }

        private static class NonRecursiveHashSetDouble {
            private final NonRecursiveHashMapDouble map;

            NonRecursiveHashSetDouble(int approxCapacity) {
                map = new NonRecursiveHashMapDouble(approxCapacity);
            }

            public boolean add(double value) {
                return map.put(value, 1);
            }

            public boolean contains(double value) {
                return map.get(value) == 1 ? true : false;
            }
        }

        private static class NonRecursiveHashSetCharacter {
            private final NonRecursiveHashMapCharacter map;

            NonRecursiveHashSetCharacter(int approxCapacity) {
                map = new NonRecursiveHashMapCharacter(approxCapacity);
            }

            public boolean add(String value) {
                return map.put(value, 1);
            }

            public boolean contains(String value) {
                return map.get(value) == 1 ? true : false;
            }
        }

        private static class NonRecursiveHashSetComplex {
            private final NonRecursiveHashMapComplex map;

            NonRecursiveHashSetComplex(int approxCapacity) {
                map = new NonRecursiveHashMapComplex(approxCapacity);
            }

            public boolean add(RComplex value) {
                return map.put(value, 1);
            }

            public boolean contains(RComplex value) {
                return map.get(value) == 1 ? true : false;
            }
        }
    }
}
