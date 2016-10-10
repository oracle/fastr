/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.runtime.RError.Message.MATCH_VECTOR_ARGS;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import java.util.Arrays;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.helpers.InheritsCheckNode;
import com.oracle.truffle.r.nodes.unary.CastStringNode;
import com.oracle.truffle.r.nodes.unary.CastStringNodeGen;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RAttributeProfiles;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RFunction;
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
import com.oracle.truffle.r.runtime.ops.na.NACheck;
import com.oracle.truffle.r.runtime.ops.na.NAProfile;

@RBuiltin(name = "match", kind = INTERNAL, parameterNames = {"x", "table", "nomatch", "incomparables"}, behavior = PURE)
public abstract class Match extends RBuiltinNode {

    private static final int TABLE_SIZE_FACTOR = 10;

    protected abstract Object executeRIntVector(Object x, Object table, Object noMatch, Object incomparables);

    @Child private CastStringNode castString;

    @Child private Match matchRecursive;

    private final NACheck naCheck = NACheck.create();
    private final ConditionProfile bigTableProfile = ConditionProfile.createBinaryProfile();
    private final RAttributeProfiles attrProfiles = RAttributeProfiles.create();

    @Override
    protected void createCasts(CastBuilder casts) {
        // TODO initially commented out because of use of scalars, the commented out version
        // converted to new cast pipelines API

        // casts.arg("x").allowNull().mustBe(abstractVectorValue(), SHOW_CALLER,
        // MATCH_VECTOR_ARGS).asVectorPreserveAttrs(true);
        // casts.arg("table").allowNull().mustBe(abstractVectorValue()).asVectorPreserveAttrs(true);
        casts.arg("nomatch").asIntegerVector().findFirst();
        // casts.arg("incomparables").allowNull().mustBe(abstractVectorValue()).asVectorPreserveAttrs(true);
    }

    private RAbstractStringVector castString(RAbstractVector operand) {
        if (castString == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castString = insert(CastStringNodeGen.create(false, false, false));
        }
        return (RAbstractStringVector) castString.execute(operand);
    }

    private Object matchRecursive(Object x, Object table, Object noMatch, Object incomparables) {
        if (matchRecursive == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            matchRecursive = insert(MatchNodeGen.create(null));
        }
        return matchRecursive.executeRIntVector(x, table, noMatch, incomparables);
    }

    // FIXME deal incomparables parameter

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
    protected RIntVector match(RAbstractVector x, RNull table, int nomatch, Object incomparables) {
        int[] data = new int[x.getLength()];
        Arrays.fill(data, nomatch);
        naCheck.enable(nomatch);
        naCheck.check(nomatch);
        return RDataFactory.createIntVector(data, naCheck.neverSeenNA());
    }

    @Specialization(guards = {"isFactor(x)", "isFactor(table)"})
    protected Object matchFactor(RAbstractIntVector x, RAbstractIntVector table, int nomatch, Object incomparables) {
        naCheck.enable(x);
        naCheck.enable(table);
        return matchRecursive(RClosures.createFactorToVector(x, true, attrProfiles), RClosures.createFactorToVector(table, true, attrProfiles), nomatch, incomparables);
    }

    @Specialization(guards = {"isFactor(x)", "!isFactor(table)"})
    protected Object matchFactor(RAbstractIntVector x, RAbstractVector table, int nomatch, Object incomparables) {
        naCheck.enable(x);
        return matchRecursive(RClosures.createFactorToVector(x, true, attrProfiles), table, nomatch, incomparables);
    }

    @Specialization(guards = {"!isFactor(x)", "isFactor(table)"})
    protected Object matchFactor(RAbstractVector x, RAbstractIntVector table, int nomatch, Object incomparables) {
        naCheck.enable(table);
        return matchRecursive(x, RClosures.createFactorToVector(table, true, attrProfiles), nomatch, incomparables);
    }

    @Specialization
    protected Object matchList(RAbstractListVector x, Object table, Object nomatchObj, Object incomparables, //
                    @Cached("create()") CastStringNode cast) {
        return matchRecursive(cast.execute(x), table, nomatchObj, incomparables);
    }

    @Specialization
    protected RIntVector match(RAbstractIntVector x, RAbstractIntVector table, int nomatch, @SuppressWarnings("unused") Object incomparables) {
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
    protected RIntVector match(RAbstractDoubleVector x, RAbstractIntVector table, int nomatch, @SuppressWarnings("unused") Object incomparables) {
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
    protected RIntVector match(RAbstractIntVector x, RAbstractDoubleVector table, int nomatch, @SuppressWarnings("unused") Object incomparables) {
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
    protected RIntVector match(RAbstractDoubleVector x, RAbstractDoubleVector table, int nomatch, @SuppressWarnings("unused") Object incomparables) {
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
    protected RIntVector match(RAbstractIntVector x, RAbstractLogicalVector table, int nomatch, @SuppressWarnings("unused") Object incomparables) {
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
    protected int matchSizeOne(RAbstractStringVector x, RAbstractStringVector table, int nomatch, @SuppressWarnings("unused") Object incomparables, //
                    @Cached("create()") NAProfile naProfile, //
                    @Cached("create()") BranchProfile foundProfile, //
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
    protected RIntVector match(RAbstractStringVector x, RAbstractStringVector table, int nomatch, @SuppressWarnings("unused") Object incomparables) {
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
    protected RIntVector match(RAbstractLogicalVector x, RAbstractStringVector table, int nomatch, Object incomparables) {
        naCheck.enable(x);
        return match(RClosures.createLogicalToStringVector(x), table, nomatch, incomparables);
    }

    @Specialization
    protected RIntVector match(RAbstractRawVector x, RAbstractIntVector table, int nomatch, Object incomparables) {
        naCheck.enable(x);
        return match(RClosures.createRawToStringVector(x), RClosures.createIntToStringVector(table), nomatch, incomparables);
    }

    @Specialization
    protected RIntVector match(RAbstractIntVector x, RAbstractStringVector table, int nomatch, Object incomparables) {
        naCheck.enable(x);
        return match(RClosures.createIntToStringVector(x), table, nomatch, incomparables);
    }

    @Specialization
    protected RIntVector match(RAbstractDoubleVector x, RAbstractStringVector table, int nomatch, Object incomparables) {
        naCheck.enable(x);
        return match(RClosures.createDoubleToStringVector(x), table, nomatch, incomparables);
    }

    @Specialization
    protected RIntVector match(RAbstractLogicalVector x, RAbstractLogicalVector table, RAbstractIntVector nomatchVec, @SuppressWarnings("unused") Object incomparables) {
        int nomatch = nomatchVec.getLength() == 0 ? RRuntime.INT_NA : nomatchVec.getDataAt(0);
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

    @Specialization(guards = "!isStringVectorTable(table)")
    protected RIntVector match(RAbstractStringVector x, RAbstractVector table, RAbstractIntVector nomatchVec, @SuppressWarnings("unused") Object incomparables) {
        int nomatch = nomatchVec.getLength() == 0 ? RRuntime.INT_NA : nomatchVec.getDataAt(0);
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
    protected RIntVector match(RAbstractComplexVector x, RAbstractComplexVector table, RAbstractIntVector nomatchVec, @SuppressWarnings("unused") Object incomparables) {
        int nomatch = nomatchVec.getLength() == 0 ? RRuntime.INT_NA : nomatchVec.getDataAt(0);
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

    @Specialization
    @SuppressWarnings("unused")
    protected RIntVector match(RFunction x, Object table, int nomatch, Object incomparables) {
        throw RError.error(this, MATCH_VECTOR_ARGS);
    }

    @Specialization
    @SuppressWarnings("unused")
    protected RIntVector match(Object x, RFunction table, int nomatch, Object incomparables) {
        throw RError.error(this, MATCH_VECTOR_ARGS);
    }

    protected boolean isStringVectorTable(RAbstractVector table) {
        return table.getElementClass() == String.class;
    }

    @Child private InheritsCheckNode factorInheritsCheck = new InheritsCheckNode(RRuntime.CLASS_FACTOR);

    protected boolean isFactor(Object o) {
        return factorInheritsCheck.execute(o);
    }

    private static int[] initResult(int length, int nomatch) {
        int[] result = new int[length];
        Arrays.fill(result, nomatch);
        return result;
    }

    /**
     * Set the "complete" status. If {@code nomatch} is not NA (uncommon), then the result vector is
     * always COMPLETE, otherwise it is INCOMPLETE unless everything matched.
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
        private NonRecursiveHashMapInt map;

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
        private NonRecursiveHashMapDouble map;

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
        private NonRecursiveHashMapCharacter map;

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
        private NonRecursiveHashMapComplex map;

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
