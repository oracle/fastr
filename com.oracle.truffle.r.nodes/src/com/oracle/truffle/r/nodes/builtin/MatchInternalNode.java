/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.nodes.builtin;

import java.util.Arrays;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.unary.CastStringNode;
import com.oracle.truffle.r.nodes.unary.CastStringNodeGen;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.CharSXPWrapper;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntSeqVectorData;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RStringSeqVectorData;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RComplexVector;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.RRawVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
import com.oracle.truffle.r.runtime.ops.na.NAProfile;

public abstract class MatchInternalNode extends RBaseNode {

    private static final int TABLE_SIZE_FACTOR = 10;

    public abstract Object execute(RAbstractVector x, RAbstractVector table, int noMatch);

    @Node.Child private CastStringNode castString;

    private final ConditionProfile bigTableProfile = ConditionProfile.createBinaryProfile();

    private RAbstractStringVector castString(RAbstractVector operand) {
        if (castString == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castString = insert(CastStringNodeGen.create(false, false, false));
        }
        return (RAbstractStringVector) RRuntime.asAbstractVector(castString.doCast(operand));
    }

    protected boolean isSequence(RAbstractVector vec) {
        return vec.isSequence();
    }

    protected boolean isCharSXP(RAbstractListVector list) {
        for (int i = 0; i < list.getLength(); i++) {
            if (!(RType.getRType(list.getDataAt(i)).equals(RType.Char))) {
                return false;
            }
        }
        return true;
    }

    @Specialization(guards = "table.isSequence()")
    @CompilerDirectives.TruffleBoundary
    protected RIntVector matchInSequence(RIntVector x, RIntVector table, int nomatch) {
        int[] result = initResult(x.getLength(), nomatch);
        boolean matchAll = true;

        RIntSeqVectorData seq = (RIntSeqVectorData) table.getData();
        for (int i = 0; i < result.length; i++) {
            int xx = x.getDataAt(i);
            int index = seq.getIndexFor(xx);
            if (index != -1) {
                result[i] = index + 1;
            } else {
                matchAll = false;
            }
        }
        return RDataFactory.createIntVector(result, setCompleteState(matchAll, nomatch));
    }

    @Specialization(guards = {"x.getLength() == 1", "!isSequence(table)"})
    @CompilerDirectives.TruffleBoundary
    protected int matchSizeOne(RIntVector x, RIntVector table, int nomatch,
                    @Cached("create()") NAProfile naProfile,
                    @Cached("create()") BranchProfile foundProfile,
                    @Cached("create()") BranchProfile notFoundProfile) {
        int element = x.getDataAt(0);
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
                if (element == table.getDataAt(i)) {
                    foundProfile.enter();
                    return i + 1;
                }
            }
        }
        notFoundProfile.enter();
        return nomatch;
    }

    @Specialization(guards = {"x.getLength() != 1", "!isSequence(table)"})
    @CompilerDirectives.TruffleBoundary
    protected RIntVector match(RIntVector x, RIntVector table, int nomatch) {
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
    @CompilerDirectives.TruffleBoundary
    protected RIntVector match(RDoubleVector x, RIntVector table, int nomatch) {
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
    @CompilerDirectives.TruffleBoundary
    protected RIntVector match(RIntVector x, RDoubleVector table, int nomatch) {
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

    @Specialization(guards = "x.getLength() == 1")
    @CompilerDirectives.TruffleBoundary
    protected int matchSizeOne(RDoubleVector x, RDoubleVector table, int nomatch,
                    @Cached("create()") NAProfile naProfile,
                    @Cached("create()") BranchProfile foundProfile,
                    @Cached("create()") BranchProfile notFoundProfile) {
        double element = x.getDataAt(0);
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
                if (element == table.getDataAt(i)) {
                    foundProfile.enter();
                    return i + 1;
                }
            }
        }
        notFoundProfile.enter();
        return nomatch;
    }

    @Specialization(guards = "x.getLength() != 1")
    @CompilerDirectives.TruffleBoundary
    protected RIntVector match(RDoubleVector x, RDoubleVector table, int nomatch) {
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
    @CompilerDirectives.TruffleBoundary
    protected RIntVector match(RIntVector x, RLogicalVector table, int nomatch) {
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

    @Specialization
    @CompilerDirectives.TruffleBoundary
    protected RIntVector match(RDoubleVector x, RLogicalVector table, int nomatch) {
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
            double xx = x.getDataAt(i);
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

    @Specialization(guards = "isSequence(table)")
    @CompilerDirectives.TruffleBoundary
    protected RIntVector matchInSequence(RAbstractStringVector x, RStringVector table, int nomatch) {
        int[] result = initResult(x.getLength(), nomatch);
        boolean matchAll = true;

        RStringSeqVectorData seq = table.getSequence();
        for (int i = 0; i < result.length; i++) {
            String xx = x.getDataAt(i);
            int index = seq.getIndexFor(xx);
            if (index != -1) {
                result[i] = index + 1;
            } else {
                matchAll = false;
            }
        }
        return RDataFactory.createIntVector(result, setCompleteState(matchAll, nomatch));
    }

    @Specialization(guards = {"x.getLength() == 1", "!isSequence(table)"})
    @CompilerDirectives.TruffleBoundary
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

    @Specialization(guards = {"x.getLength() != 1", "!isSequence(table)"})
    @CompilerDirectives.TruffleBoundary
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

    @Specialization(guards = {"x.getLength() == 1", "isCharSXP(x)", "isCharSXP(table)"})
    @CompilerDirectives.TruffleBoundary
    protected int matchSizeOne(RList x, RList table, int nomatch,
                    @Cached("create()") NAProfile naProfile,
                    @Cached("create()") BranchProfile foundProfile,
                    @Cached("create()") BranchProfile notFoundProfile) {
        Object data = x.getDataAt(0);
        Object tableData;

        assert data instanceof CharSXPWrapper;
        String element = ((CharSXPWrapper) data).getContents();
        int length = table.getLength();
        if (naProfile.isNA(element)) {
            for (int i = 0; i < length; i++) {
                tableData = table.getDataAt(i);
                assert tableData instanceof CharSXPWrapper;
                if (RRuntime.isNA(((CharSXPWrapper) tableData).getContents())) {
                    foundProfile.enter();
                    return i + 1;
                }
            }
        } else {
            for (int i = 0; i < length; i++) {
                tableData = table.getDataAt(i);
                assert tableData instanceof CharSXPWrapper;
                if (element.equals(((CharSXPWrapper) tableData).getContents())) {
                    foundProfile.enter();
                    return i + 1;
                }
            }
        }
        notFoundProfile.enter();
        return nomatch;
    }

    @Specialization(guards = {"x.getLength() != 1", "isCharSXP(x)", "isCharSXP(table)"})
    @CompilerDirectives.TruffleBoundary
    protected RIntVector match(RList x, RList table, int nomatch) {
        int[] result = initResult(x.getLength(), nomatch);
        Object element;
        boolean matchAll = true;
        NonRecursiveHashMapCharacter hashTable;
        if (bigTableProfile.profile(table.getLength() > (x.getLength() * TABLE_SIZE_FACTOR))) {
            hashTable = new NonRecursiveHashMapCharacter(x.getLength());
            NonRecursiveHashSetCharacter hashSet = new NonRecursiveHashSetCharacter(x.getLength());
            for (int i = 0; i < result.length; i++) {
                element = x.getDataAt(i);
                assert element instanceof CharSXPWrapper;
                hashSet.add(((CharSXPWrapper) element).getContents());
            }
            for (int i = table.getLength() - 1; i >= 0; i--) {
                element = table.getDataAt(i);
                assert element instanceof CharSXPWrapper;
                String val = ((CharSXPWrapper) element).getContents();
                if (hashSet.contains(val)) {
                    hashTable.put(val, i);
                }
            }
        } else {
            hashTable = new NonRecursiveHashMapCharacter(table.getLength());
            for (int i = table.getLength() - 1; i >= 0; i--) {
                element = table.getDataAt(i);
                assert element instanceof CharSXPWrapper;
                hashTable.put(((CharSXPWrapper) element).getContents(), i);
            }
        }
        for (int i = 0; i < result.length; i++) {
            element = x.getDataAt(i);
            assert element instanceof CharSXPWrapper;
            String xx = ((CharSXPWrapper) element).getContents();
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
    protected RIntVector match(RDoubleVector x, RComplexVector table, int nomatch,
                    @Cached("createBinaryProfile()") ConditionProfile isNAProfile) {
        return match((RComplexVector) x.castSafe(RType.Complex, isNAProfile), table, nomatch);
    }

    @Specialization
    protected RIntVector match(RLogicalVector x, RIntVector table, int nomatch,
                    @Cached("createBinaryProfile()") ConditionProfile isNAProfile) {
        return match((RIntVector) x.castSafe(RType.Integer, isNAProfile), table, nomatch);
    }

    @Specialization
    protected RIntVector match(RLogicalVector x, RDoubleVector table, int nomatch,
                    @Cached("createBinaryProfile()") ConditionProfile isNAProfile) {
        return match((RDoubleVector) x.castSafe(RType.Double, isNAProfile), table, nomatch);
    }

    @Specialization
    protected RIntVector match(RLogicalVector x, RComplexVector table, int nomatch,
                    @Cached("createBinaryProfile()") ConditionProfile isNAProfile) {
        return match((RComplexVector) x.castSafe(RType.Complex, isNAProfile), table, nomatch);
    }

    @Specialization
    protected RIntVector match(RLogicalVector x, RAbstractStringVector table, int nomatch,
                    @Cached("createBinaryProfile()") ConditionProfile isNAProfile) {
        return match((RAbstractStringVector) x.castSafe(RType.Character, isNAProfile), table, nomatch);
    }

    @Specialization
    protected RIntVector match(RIntVector x, RComplexVector table, int nomatch,
                    @Cached("createBinaryProfile()") ConditionProfile isNAProfile) {
        return match((RComplexVector) x.castSafe(RType.Complex, isNAProfile), table, nomatch);
    }

    @Specialization(guards = "x.getLength() == 1")
    @CompilerDirectives.TruffleBoundary
    protected int matchSizeOne(RRawVector x, RRawVector table, int nomatch,
                    @Cached("create()") BranchProfile foundProfile,
                    @Cached("create()") BranchProfile notFoundProfile) {
        byte element = x.getRawDataAt(0);
        int length = table.getLength();
        for (int i = 0; i < length; i++) {
            if (element == table.getRawDataAt(i)) {
                foundProfile.enter();
                return i + 1;
            }
        }
        notFoundProfile.enter();
        return nomatch;
    }

    @Specialization(guards = "x.getLength() != 1")
    protected RIntVector match(RRawVector x, RRawVector table, int nomatch) {
        int[] result = initResult(x.getLength(), nomatch);
        boolean matchAll = true;
        NonRecursiveHashMapRaw hashTable;
        if (bigTableProfile.profile(table.getLength() > (x.getLength() * TABLE_SIZE_FACTOR))) {
            hashTable = new NonRecursiveHashMapRaw();
            NonRecursiveHashSetRaw hashSet = new NonRecursiveHashSetRaw();

            for (int i = 0; i < result.length; i++) {
                hashSet.add(x.getRawDataAt(i));
            }
            for (int i = table.getLength() - 1; i >= 0; i--) {
                byte val = table.getRawDataAt(i);
                if (hashSet.contains(val)) {
                    hashTable.put(val, i);
                }
            }
        } else {
            hashTable = new NonRecursiveHashMapRaw();
            for (int i = table.getLength() - 1; i >= 0; i--) {
                hashTable.put(table.getRawDataAt(i), i);
            }
        }
        for (int i = 0; i < result.length; i++) {
            byte xx = x.getRawDataAt(i);
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
    protected RIntVector match(RRawVector x, RIntVector table, int nomatch,
                    @Cached("createBinaryProfile()") ConditionProfile isNAProfile) {
        return match((RAbstractStringVector) x.castSafe(RType.Character, isNAProfile), (RAbstractStringVector) table.castSafe(RType.Character, isNAProfile), nomatch);
    }

    @Specialization
    protected RIntVector match(RRawVector x, RDoubleVector table, int nomatch,
                    @Cached("createBinaryProfile()") ConditionProfile isNAProfile) {
        return match((RAbstractStringVector) x.castSafe(RType.Character, isNAProfile), (RAbstractStringVector) table.castSafe(RType.Character, isNAProfile), nomatch);
    }

    @Specialization
    protected RIntVector match(RRawVector x, RAbstractStringVector table, int nomatch,
                    @Cached("createBinaryProfile()") ConditionProfile isNAProfile) {
        return match((RAbstractStringVector) x.castSafe(RType.Character, isNAProfile), table, nomatch);
    }

    @Specialization
    protected RIntVector match(RRawVector x, @SuppressWarnings("unused") RLogicalVector table, @SuppressWarnings("unused") int nomatch) {
        return RDataFactory.createIntVector(x.getLength(), true);
    }

    @Specialization
    protected RIntVector match(RRawVector x, @SuppressWarnings("unused") RComplexVector table, @SuppressWarnings("unused") int nomatch) {
        return RDataFactory.createIntVector(x.getLength(), true);
    }

    @Specialization
    protected RIntVector match(RIntVector x, RAbstractStringVector table, int nomatch,
                    @Cached("createBinaryProfile()") ConditionProfile isNAProfile) {
        return match((RAbstractStringVector) x.castSafe(RType.Character, isNAProfile), table, nomatch);
    }

    @Specialization
    protected RIntVector match(RDoubleVector x, RAbstractStringVector table, int nomatch,
                    @Cached("createBinaryProfile()") ConditionProfile isNAProfile) {
        return match((RAbstractStringVector) x.castSafe(RType.Character, isNAProfile), table, nomatch);
    }

    @Specialization
    protected RIntVector match(RIntVector x, RRawVector table, int nomatch,
                    @Cached("createBinaryProfile()") ConditionProfile isNAProfile) {
        return match((RAbstractStringVector) x.castSafe(RType.Character, isNAProfile), (RAbstractStringVector) table.castSafe(RType.Character, isNAProfile), nomatch);
    }

    @Specialization
    protected RIntVector match(RLogicalVector x, @SuppressWarnings("unused") RRawVector table, @SuppressWarnings("unused") int nomatch) {
        return RDataFactory.createIntVector(x.getLength(), true);
    }

    @Specialization
    protected RIntVector match(RComplexVector x, @SuppressWarnings("unused") RRawVector table, @SuppressWarnings("unused") int nomatch) {
        return RDataFactory.createIntVector(x.getLength(), true);
    }

    @Specialization
    protected RIntVector match(RComplexVector x, RLogicalVector table, int nomatch,
                    @Cached("createBinaryProfile()") ConditionProfile isNAProfile) {
        return match(x, (RComplexVector) table.castSafe(RType.Complex, isNAProfile), nomatch);
    }

    @Specialization
    protected RIntVector match(RComplexVector x, RIntVector table, int nomatch,
                    @Cached("createBinaryProfile()") ConditionProfile isNAProfile) {
        return match(x, (RComplexVector) table.castSafe(RType.Complex, isNAProfile), nomatch);
    }

    @Specialization
    protected RIntVector match(RComplexVector x, RDoubleVector table, int nomatch,
                    @Cached("createBinaryProfile()") ConditionProfile isNAProfile) {
        return match(x, (RComplexVector) table.castSafe(RType.Complex, isNAProfile), nomatch);
    }

    @Specialization
    protected RIntVector match(RComplexVector x, RAbstractStringVector table, int nomatch,
                    @Cached("createBinaryProfile()") ConditionProfile isNAProfile) {
        return match((RAbstractStringVector) x.castSafe(RType.Character, isNAProfile), table, nomatch);
    }

    @Specialization
    protected RIntVector match(RDoubleVector x, RRawVector table, int nomatch,
                    @Cached("createBinaryProfile()") ConditionProfile isNAProfile) {
        return match((RAbstractStringVector) x.castSafe(RType.Character, isNAProfile),
                        (RAbstractStringVector) table.castSafe(RType.Character, isNAProfile), nomatch);
    }

    @Specialization
    @CompilerDirectives.TruffleBoundary
    protected RIntVector match(RLogicalVector x, RLogicalVector table, int nomatch) {
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
    @CompilerDirectives.TruffleBoundary
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

    @Specialization(guards = "x.getLength() == 1")
    @CompilerDirectives.TruffleBoundary
    protected int matchSizeOne(RComplexVector x, RComplexVector table, int nomatch,
                    @Cached("create()") NAProfile naProfile,
                    @Cached("create()") BranchProfile foundProfile,
                    @Cached("create()") BranchProfile notFoundProfile) {
        RComplex element = x.getDataAt(0);
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
    @CompilerDirectives.TruffleBoundary
    protected RIntVector match(RComplexVector x, RComplexVector table, int nomatch) {
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
            values = new int[Integer.highestOneBit(capacity) << 2];
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
        private final int m;
        private final int k;

        NonRecursiveHashMapComplex(int entryCount) {
            super(entryCount);
            keys = new RComplex[values.length];

            int m1 = 2;
            int k1 = 1;
            int n = 2 * entryCount;
            while (m1 < n) {
                m1 *= 2;
                k1++;
            }
            m = m1;
            k = k1;
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
                        ind = (ind + 1) % m;
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
                        ind = (ind + 1) % m;
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

        private static final int u = new Long(3141592653L).intValue();

        @Override
        protected int index(int key) {
            return u * key >>> (32 - k);
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

    private static final class NonRecursiveHashMapRaw {

        private final int[] keys;
        protected final int[] values;

        NonRecursiveHashMapRaw() {
            values = new int[256];
            keys = new int[256];
            Arrays.fill(keys, -1);
        }

        public boolean put(byte key, int value) {
            assert value >= 0;
            int ind = key & 0xFF;
            while (true) {
                if (values[ind] == 0) {
                    keys[ind] = key;
                    values[ind] = value + 1;
                    return false;
                } else if (key == keys[ind]) {
                    values[ind] = value + 1;
                    return true;
                } else {
                    assert false;
                }
            }
        }

        public int get(byte key) {
            int ind = key & 0xFF;
            while (true) {
                if (key == keys[ind]) {
                    return values[ind] - 1;
                } else {
                    return -1;
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
            return map.get(value) == 1;
        }
    }

    private static class NonRecursiveHashSetRaw {
        private final NonRecursiveHashMapRaw map;

        NonRecursiveHashSetRaw() {
            map = new NonRecursiveHashMapRaw();
        }

        public boolean add(byte value) {
            return map.put(value, 1);
        }

        public boolean contains(byte value) {
            return map.get(value) == 1;
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
            return map.get(value) == 1;
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
            return map.get(value) == 1;
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
            return map.get(value) == 1;
        }
    }
}
