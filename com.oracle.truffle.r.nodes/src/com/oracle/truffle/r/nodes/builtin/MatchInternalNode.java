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
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.unary.CastStringNode;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.CharSXPWrapper;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntSeqVectorData;
import com.oracle.truffle.r.runtime.data.RComplexVector;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.RRawVector;
import com.oracle.truffle.r.runtime.data.RStringSeqVectorData;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.RandomAccessIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.SeqIterator;
import com.oracle.truffle.r.runtime.data.model.RAbstractAtomicVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
import com.oracle.truffle.r.runtime.ops.na.NAProfile;

abstract class AbstractMatchNode extends RBaseNode {
    protected static final int TABLE_SIZE_FACTOR = 10;

    public abstract Object execute(RAbstractVector x, RAbstractVector table, int noMatch);

    protected final ConditionProfile bigTableProfile = ConditionProfile.createBinaryProfile();

}

@ImportStatic(DSLConfig.class)
public abstract class MatchInternalNode extends AbstractMatchNode {

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

    @Specialization(guards = "table.isSequence()", limit = "getTypedVectorDataLibraryCacheSize()")
    @CompilerDirectives.TruffleBoundary
    protected RIntVector matchInSequence(RIntVector x, RIntVector table, int nomatch,
                    @CachedLibrary("x.getData()") VectorDataLibrary xDataLib) {

        Object xData = x.getData();
        int[] result = initResult(xDataLib.getLength(xData), nomatch);
        boolean matchAll = true;

        RIntSeqVectorData seq = (RIntSeqVectorData) table.getData();
        SeqIterator it = xDataLib.iterator(xData);
        while (xDataLib.nextLoopCondition(xData, it)) {
            int xx = xDataLib.getNextInt(xData, it);
            int index = seq.getIndexFor(xx);
            if (index != -1) {
                result[it.getIndex()] = index + 1;
            } else {
                matchAll = false;
            }
        }
        return RDataFactory.createIntVector(result, setCompleteState(matchAll, nomatch));
    }

    @Specialization(guards = {"xDataLib.getLength(x.getData()) == 1", "!isSequence(table)"}, limit = "getTypedVectorDataLibraryCacheSize()")
    @CompilerDirectives.TruffleBoundary
    protected int matchSizeOne(RIntVector x, RIntVector table, int nomatch,
                    @CachedLibrary("x.getData()") VectorDataLibrary xDataLib,
                    @CachedLibrary("table.getData()") VectorDataLibrary tableDataLib,
                    @Cached("create()") NAProfile naProfile,
                    @Cached("create()") BranchProfile foundProfile,
                    @Cached("create()") BranchProfile notFoundProfile) {
        Object tableData = table.getData();
        SeqIterator it = tableDataLib.iterator(tableData);
        int element = xDataLib.getIntAt(x.getData(), 0);
        if (naProfile.isNA(element)) {
            while (tableDataLib.nextLoopCondition(tableData, it)) {
                if (tableDataLib.isNextNA(tableData, it)) {
                    foundProfile.enter();
                    return it.getIndex() + 1;
                }
            }
        } else {
            while (tableDataLib.nextLoopCondition(tableData, it)) {
                if (element == tableDataLib.getNextInt(tableData, it)) {
                    foundProfile.enter();
                    return it.getIndex() + 1;
                }
            }
        }
        notFoundProfile.enter();
        return nomatch;
    }

    @Specialization(guards = {"xDataLib.getLength(x.getData()) != 1", "!isSequence(table)"}, limit = "getTypedVectorDataLibraryCacheSize()")
    @CompilerDirectives.TruffleBoundary
    protected RIntVector match(RIntVector x, RIntVector table, int nomatch,
                    @SuppressWarnings("unused") @CachedLibrary("x.getData()") VectorDataLibrary xDataLib,
                    @Cached() MatchAsIntVectorNode match) {
        return match.execute(x, table, nomatch);
    }

    @Specialization(limit = "getTypedVectorDataLibraryCacheSize()")
    @CompilerDirectives.TruffleBoundary
    protected RIntVector match(RDoubleVector x, RIntVector table, int nomatch,
                    @CachedLibrary("x.getData()") VectorDataLibrary xDataLib,
                    @CachedLibrary("table.getData()") VectorDataLibrary tableDataLib) {
        Object xData = x.getData();
        Object tableData = table.getData();
        int xLength = xDataLib.getLength(xData);
        int tableLength = tableDataLib.getLength(tableData);
        int[] result = initResult(xLength, nomatch);
        boolean matchAll = true;
        RandomAccessIterator rit = tableDataLib.randomAccessIterator(tableData);
        NonRecursiveHashMapDouble hashTable;
        if (bigTableProfile.profile(tableLength > (xLength * TABLE_SIZE_FACTOR))) {
            hashTable = new NonRecursiveHashMapDouble(xLength);
            NonRecursiveHashSetDouble hashSet = new NonRecursiveHashSetDouble(xLength);
            SeqIterator it = xDataLib.iterator(xData);
            while (xDataLib.nextLoopCondition(xData, it)) {
                hashSet.add(xDataLib.getNextDouble(xData, it));
            }
            for (int i = tableLength - 1; i >= 0; i--) {
                double val = tableDataLib.getDouble(tableData, rit, i);
                if (hashSet.contains(val)) {
                    hashTable.put(val, i);
                }
            }
        } else {
            hashTable = new NonRecursiveHashMapDouble(tableLength);
            for (int i = tableLength - 1; i >= 0; i--) {
                hashTable.put(RRuntime.int2double(tableDataLib.getInt(tableData, rit, i)), i);
            }
        }
        SeqIterator it = xDataLib.iterator(xData);
        while (xDataLib.nextLoopCondition(xData, it)) {
            double xx = xDataLib.getNextDouble(xData, it);
            int index = hashTable.get(xx);
            if (index != -1) {
                result[it.getIndex()] = index + 1;
            } else {
                matchAll = false;
            }
        }
        return RDataFactory.createIntVector(result, setCompleteState(matchAll, nomatch));
    }

    @Specialization(limit = "getTypedVectorDataLibraryCacheSize()")
    @CompilerDirectives.TruffleBoundary
    protected RIntVector match(RIntVector x, RDoubleVector table, int nomatch,
                    @CachedLibrary("x.getData()") VectorDataLibrary xDataLib,
                    @CachedLibrary("table.getData()") VectorDataLibrary tableDataLib) {
        Object xData = x.getData();
        Object tableData = table.getData();
        int xLength = xDataLib.getLength(xData);
        int tableLength = tableDataLib.getLength(tableData);
        int[] result = initResult(xLength, nomatch);
        boolean matchAll = true;
        RandomAccessIterator rit = tableDataLib.randomAccessIterator(tableData);
        NonRecursiveHashMapInt hashTable;
        if (bigTableProfile.profile(tableLength > (xLength * TABLE_SIZE_FACTOR))) {
            hashTable = new NonRecursiveHashMapInt(xLength);
            NonRecursiveHashSetInt hashSet = new NonRecursiveHashSetInt(xLength);
            SeqIterator it = xDataLib.iterator(xData);
            while (xDataLib.nextLoopCondition(xData, it)) {
                hashSet.add(xDataLib.getNextInt(xData, it));
            }
            for (int i = tableLength - 1; i >= 0; i--) {
                double val = tableDataLib.getDouble(tableData, rit, i);
                if (RRuntime.isNA(val) && hashSet.contains(RRuntime.INT_NA)) {
                    hashTable.put(RRuntime.INT_NA, i);
                } else if (val == (int) val && hashSet.contains((int) val)) {
                    hashTable.put((int) val, i);
                }
            }
        } else {
            hashTable = new NonRecursiveHashMapInt(tableLength);
            for (int i = tableLength - 1; i >= 0; i--) {
                double xx = tableDataLib.getDouble(tableData, rit, i);
                if (RRuntime.isNA(xx)) {
                    hashTable.put(RRuntime.INT_NA, i);
                } else if (xx == (int) xx && !RRuntime.isNA((int) xx)) {
                    hashTable.put((int) xx, i);
                }
            }
        }
        SeqIterator it = xDataLib.iterator(xData);
        while (xDataLib.nextLoopCondition(xData, it)) {
            int xx = xDataLib.getNextInt(xData, it);
            int index = hashTable.get(xx);
            if (index != -1) {
                result[it.getIndex()] = index + 1;
            } else {
                matchAll = false;
            }
        }
        return RDataFactory.createIntVector(result, setCompleteState(matchAll, nomatch));
    }

    @Specialization(guards = "xDataLib.getLength(x.getData()) == 1", limit = "getTypedVectorDataLibraryCacheSize()")
    @CompilerDirectives.TruffleBoundary
    protected int matchSizeOne(RDoubleVector x, RDoubleVector table, int nomatch,
                    @Cached("create()") NAProfile naProfile,
                    @CachedLibrary("x.getData()") VectorDataLibrary xDataLib,
                    @CachedLibrary("table.getData()") VectorDataLibrary tableDataLib,
                    @Cached("create()") BranchProfile foundProfile,
                    @Cached("create()") BranchProfile notFoundProfile) {
        double element = xDataLib.getDoubleAt(x.getData(), 0);
        Object tableData = table.getData();
        if (naProfile.isNA(element)) {
            SeqIterator it = tableDataLib.iterator(tableData);
            while (tableDataLib.nextLoopCondition(tableData, it)) {
                if (tableDataLib.isNextNA(tableData, it)) {
                    foundProfile.enter();
                    return it.getIndex() + 1;
                }
            }
        } else {
            SeqIterator it = tableDataLib.iterator(tableData);
            while (tableDataLib.nextLoopCondition(tableData, it)) {
                if (element == tableDataLib.getNextDouble(tableData, it)) {
                    foundProfile.enter();
                    return it.getIndex() + 1;
                }
            }
        }
        notFoundProfile.enter();
        return nomatch;
    }

    @Specialization(guards = "xDataLib.getLength(x.getData()) != 1", limit = "getTypedVectorDataLibraryCacheSize()")
    @CompilerDirectives.TruffleBoundary
    protected RIntVector match(RDoubleVector x, RDoubleVector table, int nomatch,
                    @SuppressWarnings("unused") @CachedLibrary("x.getData()") VectorDataLibrary xDataLib,
                    @Cached() MatchAsDoubleVectorNode match) {
        return match.execute(x, table, nomatch);
    }

    @Specialization(limit = "getTypedVectorDataLibraryCacheSize()")
    @CompilerDirectives.TruffleBoundary
    protected RIntVector match(RIntVector x, RLogicalVector table, int nomatch,
                    @CachedLibrary("x.getData()") VectorDataLibrary xDataLib,
                    @CachedLibrary("table.getData()") VectorDataLibrary tableDataLib) {
        Object xData = x.getData();
        Object tableData = table.getData();
        int[] result = initResult(xDataLib.getLength(xData), nomatch);
        boolean matchAll = true;
        int[] values = {RRuntime.LOGICAL_TRUE, RRuntime.LOGICAL_FALSE, RRuntime.LOGICAL_NA};
        int[] indexes = new int[values.length];
        for (int i = 0; i < values.length; i++) {
            byte value = (byte) values[i];
            SeqIterator it = tableDataLib.iterator(tableData);
            while (tableDataLib.nextLoopCondition(tableData, it)) {
                if (tableDataLib.getNextLogical(tableData, it) == value) {
                    indexes[i] = it.getIndex() + 1;
                    break;
                }
            }
            values[i] = RRuntime.logical2int(value);
        }
        SeqIterator it = xDataLib.iterator(xData);
        while (xDataLib.nextLoopCondition(xData, it)) {
            int xx = xDataLib.getNextInt(xData, it);
            boolean match = false;
            for (int j = 0; j < values.length; j++) {
                if (xx == values[j] && indexes[j] != 0) {
                    result[it.getIndex()] = indexes[j];
                    match = true;
                    break;
                }
            }
            matchAll &= match;
        }
        return RDataFactory.createIntVector(result, setCompleteState(matchAll, nomatch));
    }

    @Specialization(limit = "getTypedVectorDataLibraryCacheSize()")
    @CompilerDirectives.TruffleBoundary
    protected RIntVector match(RDoubleVector x, RLogicalVector table, int nomatch,
                    @CachedLibrary("x.getData()") VectorDataLibrary xDataLib,
                    @CachedLibrary("table.getData()") VectorDataLibrary tableDataLib) {
        Object xData = x.getData();
        Object tableData = table.getData();
        int[] result = initResult(xDataLib.getLength(xData), nomatch);
        boolean matchAll = true;
        int[] values = {RRuntime.LOGICAL_TRUE, RRuntime.LOGICAL_FALSE, RRuntime.LOGICAL_NA};
        int[] indexes = new int[values.length];
        for (int i = 0; i < values.length; i++) {
            byte value = (byte) values[i];
            SeqIterator it = tableDataLib.iterator(tableData);
            while (tableDataLib.nextLoopCondition(tableData, it)) {
                if (tableDataLib.getNextLogical(tableData, it) == value) {
                    indexes[i] = it.getIndex() + 1;
                    break;
                }
            }
            values[i] = RRuntime.logical2int(value);
        }
        SeqIterator it = xDataLib.iterator(xData);
        while (xDataLib.nextLoopCondition(xData, it)) {
            boolean isNA = xDataLib.isNextNA(xData, it);
            double xx = xDataLib.getNextDouble(xData, it);
            boolean match = false;
            for (int j = 0; j < values.length; j++) {
                if ((isNA && RRuntime.isNA(values[j]) || xx == values[j] && !RRuntime.isNA(values[j])) && indexes[j] != 0) {
                    result[it.getIndex()] = indexes[j];
                    match = true;
                    break;
                }
            }
            matchAll &= match;
        }
        return RDataFactory.createIntVector(result, setCompleteState(matchAll, nomatch));
    }

    @Specialization(guards = {"xDataLib.getLength(x.getData()) == 1", "isCharSXP(x)", "isCharSXP(table)"}, limit = "getTypedVectorDataLibraryCacheSize()")
    @CompilerDirectives.TruffleBoundary
    protected int matchSizeOne(RList x, RList table, int nomatch,
                    @CachedLibrary("x.getData()") VectorDataLibrary xDataLib,
                    @CachedLibrary("table.getData()") VectorDataLibrary tableDataLib,
                    @Cached("create()") NAProfile naProfile,
                    @Cached("create()") BranchProfile foundProfile,
                    @Cached("create()") BranchProfile notFoundProfile) {
        Object xData = x.getData();
        Object tableData = table.getData();

        Object data = xDataLib.getElementAt(xData, 0);
        Object td;

        assert data instanceof CharSXPWrapper;
        String element = ((CharSXPWrapper) data).getContents();
        if (naProfile.isNA(element)) {
            SeqIterator it = tableDataLib.iterator(tableData);
            while (tableDataLib.next(tableData, it)) {
                td = tableDataLib.getNextElement(tableData, it);
                assert td instanceof CharSXPWrapper;
                if (RRuntime.isNA(((CharSXPWrapper) td).getContents())) {
                    foundProfile.enter();
                    return it.getIndex() + 1;
                }
            }
        } else {
            SeqIterator it = tableDataLib.iterator(tableData);
            while (tableDataLib.next(tableData, it)) {
                td = tableDataLib.getNextElement(tableData, it);
                assert td instanceof CharSXPWrapper;
                if (element.equals(((CharSXPWrapper) td).getContents())) {
                    foundProfile.enter();
                    return it.getIndex() + 1;
                }
            }
        }
        notFoundProfile.enter();
        return nomatch;
    }

    @Specialization(guards = {"xDataLib.getLength(x.getData()) != 1", "isCharSXP(x)", "isCharSXP(table)"}, limit = "getTypedVectorDataLibraryCacheSize()")
    @CompilerDirectives.TruffleBoundary
    protected RIntVector match(RList x, RList table, int nomatch,
                    @CachedLibrary("x.getData()") VectorDataLibrary xDataLib,
                    @CachedLibrary("table.getData()") VectorDataLibrary tableDataLib) {
        Object xData = x.getData();
        Object tableData = table.getData();
        int xLength = xDataLib.getLength(xData);
        int tableLength = tableDataLib.getLength(tableData);
        int[] result = initResult(xLength, nomatch);
        Object element;
        boolean matchAll = true;
        RandomAccessIterator rit = tableDataLib.randomAccessIterator(tableData);
        NonRecursiveHashMapCharacter hashTable;
        if (bigTableProfile.profile(tableLength > (xLength * TABLE_SIZE_FACTOR))) {
            hashTable = new NonRecursiveHashMapCharacter(xLength);
            NonRecursiveHashSetCharacter hashSet = new NonRecursiveHashSetCharacter(xLength);
            SeqIterator it = xDataLib.iterator(xData);
            while (xDataLib.next(xData, it)) {
                element = xDataLib.getNextElement(xData, it);
                assert element instanceof CharSXPWrapper;
                hashSet.add(((CharSXPWrapper) element).getContents());
            }
            for (int i = tableLength - 1; i >= 0; i--) {
                element = tableDataLib.getElement(tableData, rit, i);
                assert element instanceof CharSXPWrapper;
                String val = ((CharSXPWrapper) element).getContents();
                if (hashSet.contains(val)) {
                    hashTable.put(val, i);
                }
            }
        } else {
            hashTable = new NonRecursiveHashMapCharacter(tableLength);
            for (int i = tableLength - 1; i >= 0; i--) {
                element = tableDataLib.getElement(tableData, rit, i);
                assert element instanceof CharSXPWrapper;
                hashTable.put(((CharSXPWrapper) element).getContents(), i);
            }
        }
        SeqIterator it = xDataLib.iterator(xData);
        while (xDataLib.next(xData, it)) {
            element = xDataLib.getNextElement(xData, it);
            assert element instanceof CharSXPWrapper;
            String xx = ((CharSXPWrapper) element).getContents();
            int index = hashTable.get(xx);
            if (index != -1) {
                result[it.getIndex()] = index + 1;
            } else {
                matchAll = false;
            }
        }
        return RDataFactory.createIntVector(result, setCompleteState(matchAll, nomatch));
    }

    @Specialization()
    protected RIntVector match(RDoubleVector x, RComplexVector table, int nomatch,
                    @Cached() MatchAsComplexVectorNode match) {
        return match.execute(x, table, nomatch);
    }

    @Specialization
    protected RIntVector match(RLogicalVector x, RIntVector table, int nomatch,
                    @Cached() MatchAsIntVectorNode match) {
        return match.execute(x, table, nomatch);
    }

    @Specialization()
    protected RIntVector match(RLogicalVector x, RDoubleVector table, int nomatch,
                    @Cached() MatchAsDoubleVectorNode match) {
        return match.execute(x, table, nomatch);
    }

    @Specialization()
    protected RIntVector match(RLogicalVector x, RComplexVector table, int nomatch,
                    @Cached() MatchAsComplexVectorNode match) {
        return match.execute(x, table, nomatch);
    }

    @Specialization()
    protected RIntVector match(RIntVector x, RComplexVector table, int nomatch,
                    @Cached() MatchAsComplexVectorNode match) {
        return match.execute(x, table, nomatch);
    }

    @Specialization(guards = "xDataLib.getLength(x.getData()) == 1", limit = "getTypedVectorDataLibraryCacheSize()")
    @CompilerDirectives.TruffleBoundary
    protected int matchSizeOne(RRawVector x, RRawVector table, int nomatch,
                    @CachedLibrary("x.getData()") VectorDataLibrary xDataLib,
                    @CachedLibrary("table.getData()") VectorDataLibrary tableDataLib,
                    @Cached("create()") BranchProfile foundProfile,
                    @Cached("create()") BranchProfile notFoundProfile) {
        byte element = xDataLib.getRawAt(x.getData(), 0);
        Object tableData = table.getData();
        SeqIterator it = tableDataLib.iterator(tableData);
        while (tableDataLib.next(tableData, it)) {
            if (element == tableDataLib.getNextRaw(tableData, it)) {
                foundProfile.enter();
                return it.getIndex() + 1;
            }
        }
        notFoundProfile.enter();
        return nomatch;
    }

    @Specialization(guards = "xDataLib.getLength(x.getData()) != 1", limit = "getTypedVectorDataLibraryCacheSize()")
    protected RIntVector match(RRawVector x, RRawVector table, int nomatch,
                    @CachedLibrary("x.getData()") VectorDataLibrary xDataLib,
                    @CachedLibrary("table.getData()") VectorDataLibrary tableDataLib) {
        Object xData = x.getData();
        Object tableData = table.getData();
        int xLength = xDataLib.getLength(xData);
        int tableLength = tableDataLib.getLength(tableData);
        int[] result = initResult(xLength, nomatch);
        boolean matchAll = true;
        RandomAccessIterator rit = tableDataLib.randomAccessIterator(tableData);
        NonRecursiveHashMapRaw hashTable;
        if (bigTableProfile.profile(tableLength > (xLength * TABLE_SIZE_FACTOR))) {
            hashTable = new NonRecursiveHashMapRaw();
            NonRecursiveHashSetRaw hashSet = new NonRecursiveHashSetRaw();

            SeqIterator it = xDataLib.iterator(xData);
            while (xDataLib.next(xData, it)) {
                hashSet.add(xDataLib.getNextRaw(xData, it));
            }
            for (int i = tableLength - 1; i >= 0; i--) {
                byte val = tableDataLib.getRaw(tableData, rit, i);
                if (hashSet.contains(val)) {
                    hashTable.put(val, i);
                }
            }
        } else {
            hashTable = new NonRecursiveHashMapRaw();
            for (int i = tableLength - 1; i >= 0; i--) {
                hashTable.put(tableDataLib.getRaw(tableData, rit, i), i);
            }
        }
        SeqIterator it = xDataLib.iterator(xData);
        while (xDataLib.next(xData, it)) {
            byte xx = xDataLib.getNextRaw(xData, it);
            int index = hashTable.get(xx);
            if (index != -1) {
                result[it.getIndex()] = index + 1;
            } else {
                matchAll = false;
            }
        }
        return RDataFactory.createIntVector(result, setCompleteState(matchAll, nomatch));
    }

    @Specialization
    protected Object match(RRawVector x, RIntVector table, int nomatch,
                    @Cached() MatchAsStringVectorNode match) {
        return match.execute(x, table, nomatch);
    }

    @Specialization
    protected Object match(RRawVector x, RDoubleVector table, int nomatch,
                    @Cached() MatchAsStringVectorNode match) {
        return match.execute(x, table, nomatch);
    }

    @Specialization(guards = {"isRComplexVector(table) || isRLogicalVector(table)"}, limit = "getTypedVectorDataLibraryCacheSize()")
    protected RIntVector match(RRawVector x, @SuppressWarnings("unused") RAbstractAtomicVector table, @SuppressWarnings("unused") int nomatch,
                    @CachedLibrary("x.getData()") VectorDataLibrary xDataLib) {
        return RDataFactory.createIntVector(xDataLib.getLength(x.getData()), true);
    }

    @Specialization
    protected Object match(RIntVector x, RRawVector table, int nomatch,
                    @Cached() MatchAsStringVectorNode match) {
        return match.execute(x, table, nomatch);
    }

    @Specialization(guards = {"isRComplexVector(x) || isRLogicalVector(x)"}, limit = "getTypedVectorDataLibraryCacheSize()")
    protected RIntVector match(RAbstractAtomicVector x, @SuppressWarnings("unused") RRawVector table, @SuppressWarnings("unused") int nomatch,
                    @CachedLibrary("x.getData()") VectorDataLibrary xDataLib) {
        return RDataFactory.createIntVector(xDataLib.getLength(x.getData()), true);
    }

    @Specialization()
    protected RIntVector match(RComplexVector x, RLogicalVector table, int nomatch,
                    @Cached() MatchAsComplexVectorNode match) {
        return match.execute(x, table, nomatch);
    }

    @Specialization()
    protected RIntVector match(RComplexVector x, RIntVector table, int nomatch,
                    @Cached() MatchAsComplexVectorNode match) {
        return match.execute(x, table, nomatch);
    }

    @Specialization()
    protected RIntVector match(RComplexVector x, RDoubleVector table, int nomatch,
                    @Cached() MatchAsComplexVectorNode match) {
        return match.execute(x, table, nomatch);
    }

    @Specialization
    protected Object match(RDoubleVector x, RRawVector table, int nomatch,
                    @Cached() MatchAsStringVectorNode match) {
        return match.execute(x, table, nomatch);
    }

    @Specialization(limit = "getTypedVectorDataLibraryCacheSize()")
    @CompilerDirectives.TruffleBoundary
    protected RIntVector match(RLogicalVector x, RLogicalVector table, int nomatch,
                    @CachedLibrary("x.getData()") VectorDataLibrary xDataLib,
                    @CachedLibrary("table.getData()") VectorDataLibrary tableDataLib) {
        Object xData = x.getData();
        Object tableData = table.getData();

        int[] result = initResult(xDataLib.getLength(xData), nomatch);
        boolean matchAll = true;
        byte[] values = {RRuntime.LOGICAL_TRUE, RRuntime.LOGICAL_FALSE, RRuntime.LOGICAL_NA};
        int[] indexes = new int[values.length];
        for (int i = 0; i < values.length; i++) {
            byte value = values[i];
            SeqIterator it = tableDataLib.iterator(tableData);
            while (tableDataLib.nextLoopCondition(tableData, it)) {
                if (tableDataLib.getNextLogical(tableData, it) == value) {
                    indexes[i] = it.getIndex() + 1;
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

    @Specialization(guards = "xDataLib.getLength(x.getData()) == 1", limit = "getTypedVectorDataLibraryCacheSize()")
    @CompilerDirectives.TruffleBoundary
    protected int matchSizeOne(RComplexVector x, RComplexVector table, int nomatch,
                    @CachedLibrary("x.getData()") VectorDataLibrary xDataLib,
                    @CachedLibrary("table.getData()") VectorDataLibrary tableDataLib,
                    @Cached("create()") NAProfile naProfile,
                    @Cached("create()") BranchProfile foundProfile,
                    @Cached("create()") BranchProfile notFoundProfile) {
        Object xData = x.getData();
        Object tableData = table.getData();

        RComplex element = xDataLib.getComplexAt(xData, 0);
        if (naProfile.isNA(element)) {
            SeqIterator it = tableDataLib.iterator(tableData);
            while (tableDataLib.nextLoopCondition(tableData, it)) {
                if (tableDataLib.isNextNA(tableData, it)) {
                    foundProfile.enter();
                    return it.getIndex() + 1;
                }
            }
        } else {
            SeqIterator it = tableDataLib.iterator(tableData);
            while (tableDataLib.nextLoopCondition(tableData, it)) {
                if (element.equals(tableDataLib.getNextComplex(tableData, it))) {
                    foundProfile.enter();
                    return it.getIndex() + 1;
                }
            }
        }
        notFoundProfile.enter();
        return nomatch;
    }

    @Specialization
    @CompilerDirectives.TruffleBoundary
    protected RIntVector match(RComplexVector x, RComplexVector table, int nomatch,
                    @Cached() MatchAsComplexVectorNode match) {
        return match.execute(x, table, nomatch);
    }

    @Specialization(guards = "isSequence(table)", limit = "getTypedVectorDataLibraryCacheSize()")
    @CompilerDirectives.TruffleBoundary
    protected RIntVector matchInSequence(RStringVector x, RStringVector table, int nomatch,
                    @CachedLibrary("x.getData()") VectorDataLibrary xDataLib) {
        Object xData = x.getData();
        int[] result = initResult(xDataLib.getLength(xData), nomatch);
        boolean matchAll = true;

        RStringSeqVectorData seq = table.getSequence();
        SeqIterator it = xDataLib.iterator(xData);
        while (xDataLib.nextLoopCondition(xData, it)) {
            String xx = xDataLib.getNextString(xData, it);
            int index = seq.getIndexFor(xx);
            if (index != -1) {
                result[it.getIndex()] = index + 1;
            } else {
                matchAll = false;
            }
        }
        return RDataFactory.createIntVector(result, setCompleteState(matchAll, nomatch));
    }

    @Specialization(guards = {"xDataLib.getLength(x.getData()) == 1", "!isSequence(table)"}, limit = "getTypedVectorDataLibraryCacheSize()")
    @CompilerDirectives.TruffleBoundary
    protected int matchSizeOne(RStringVector x, RStringVector table, int nomatch,
                    @CachedLibrary("x.getData()") VectorDataLibrary xDataLib,
                    @CachedLibrary("table.getData()") VectorDataLibrary tableDataLib,
                    @Cached("create()") NAProfile naProfile,
                    @Cached("create()") BranchProfile foundProfile,
                    @Cached("create()") BranchProfile notFoundProfile) {
        String element = xDataLib.getStringAt(x.getData(), 0);
        Object tableData = table.getData();
        SeqIterator it = tableDataLib.iterator(tableData);
        if (naProfile.isNA(element)) {
            while (tableDataLib.nextLoopCondition(tableData, it)) {
                if (tableDataLib.isNextNA(tableData, it)) {
                    foundProfile.enter();
                    return it.getIndex() + 1;
                }
            }
        } else {
            while (tableDataLib.nextLoopCondition(tableData, it)) {
                if (element.equals(tableDataLib.getNextString(tableData, it))) {
                    foundProfile.enter();
                    return it.getIndex() + 1;
                }
            }
        }
        notFoundProfile.enter();
        return nomatch;
    }

    @Specialization(guards = {"xDataLib.getLength(x.getData()) != 1", "!isSequence(table)"}, limit = "getTypedVectorDataLibraryCacheSize()")
    @CompilerDirectives.TruffleBoundary
    protected Object match(RStringVector x, RStringVector table, int nomatch,
                    @SuppressWarnings("unused") @CachedLibrary("x.getData()") VectorDataLibrary xDataLib,
                    @Cached() MatchAsStringVectorNode match) {
        return match.execute(x, table, nomatch);
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

    @Specialization
    protected RIntVector match(RLogicalVector x, RStringVector table, int nomatch,
                    @Cached() MatchAsStringVectorNode match) {
        return match.execute(x, table, nomatch);
    }

    @Specialization
    protected RIntVector match(RRawVector x, RStringVector table, int nomatch,
                    @Cached() MatchAsStringVectorNode match) {
        return match.execute(x, table, nomatch);
    }

    @Specialization
    protected RIntVector match(RIntVector x, RStringVector table, int nomatch,
                    @Cached() MatchAsStringVectorNode match) {
        return match.execute(x, table, nomatch);
    }

    @Specialization
    protected RIntVector match(RDoubleVector x, RStringVector table, int nomatch,
                    @Cached() MatchAsStringVectorNode match) {
        return match.execute(x, table, nomatch);
    }

    @Specialization
    protected RIntVector match(RComplexVector x, RStringVector table, int nomatch,
                    @Cached() MatchAsStringVectorNode match) {
        return match.execute(x, table, nomatch);
    }

    @Specialization(guards = {"!isRStringVector(table)", "isRAbstractAtomicVector(table)"}, limit = "getTypedVectorDataLibraryCacheSize()")
    @CompilerDirectives.TruffleBoundary
    protected RIntVector match(RStringVector x, RAbstractVector table, int nomatch,
                    @CachedLibrary("x.getData()") VectorDataLibrary xDataLib,
                    @CachedLibrary("table.getData()") VectorDataLibrary tableDataLib) {
        return matchString(x, table, nomatch, xDataLib, tableDataLib);
    }

    @Specialization(guards = {"!isRStringVector(table)", "!isRAbstractAtomicVector(table)"}, limit = "getTypedVectorDataLibraryCacheSize()")
    @CompilerDirectives.TruffleBoundary
    protected RIntVector match(RStringVector x, @SuppressWarnings("unused") RAbstractVector table, int nomatch,
                    @Cached() CastStringNode castString,
                    @CachedLibrary("x.getData()") VectorDataLibrary xDataLib,
                    @CachedLibrary(limit = "getTypedVectorDataLibraryCacheSize()") VectorDataLibrary tableCoercionResultDataLib) {
        return matchString(x, (RStringVector) RRuntime.asAbstractVector(castString.doCast(table)), nomatch, xDataLib, tableCoercionResultDataLib);
    }

    protected RStringVector catsString(RAbstractVector v, CastStringNode castString) {
        return (RStringVector) RRuntime.asAbstractVector(castString.doCast(v));
    }

    protected RIntVector matchString(RAbstractVector x, RAbstractVector table, int nomatch, VectorDataLibrary xDataLib, VectorDataLibrary tableDataLib) {
        Object xData = x.getData();
        Object tableData = table.getData();
        int xLength = xDataLib.getLength(xData);
        int tableLength = tableDataLib.getLength(tableData);
        int[] result = initResult(xLength, nomatch);
        boolean matchAll = true;
        NonRecursiveHashMapCharacter hashTable = new NonRecursiveHashMapCharacter(tableLength);
        RandomAccessIterator rit = tableDataLib.randomAccessIterator(tableData);
        for (int i = tableLength - 1; i >= 0; i--) {
            hashTable.put(tableDataLib.getString(tableData, rit, i), i);
        }
        SeqIterator it = xDataLib.iterator(xData);
        while (xDataLib.nextLoopCondition(xData, it)) {
            String xx = xDataLib.getNextString(xData, it);
            int index = hashTable.get(xx);
            if (index != -1) {
                result[it.getIndex()] = index + 1;
            } else {
                matchAll = false;
            }
        }
        return RDataFactory.createIntVector(result, setCompleteState(matchAll, nomatch));
    }

    private abstract static class MatchAsNode extends AbstractMatchNode {
        protected abstract RIntVector execute(RAbstractAtomicVector x, RAbstractAtomicVector table, int nomatch);
    }

    @ImportStatic(DSLConfig.class)
    protected abstract static class MatchAsIntVectorNode extends MatchAsNode {
        @Specialization(limit = "getTypedVectorDataLibraryCacheSize()")
        protected RIntVector matchInt(RAbstractAtomicVector x, RAbstractAtomicVector table, int nomatch,
                        @CachedLibrary("x.getData()") VectorDataLibrary xDataLib,
                        @CachedLibrary("table.getData()") VectorDataLibrary tableDataLib) {
            Object xData = x.getData();
            Object tableData = table.getData();
            int xLength = xDataLib.getLength(xData);
            int tableLength = tableDataLib.getLength(tableData);
            int[] result = initResult(xLength, nomatch);
            boolean matchAll = true;

            RandomAccessIterator rit = tableDataLib.randomAccessIterator(tableData);
            NonRecursiveHashMapInt hashTable;
            if (bigTableProfile.profile(tableLength > (xLength * TABLE_SIZE_FACTOR))) {
                hashTable = new NonRecursiveHashMapInt(xLength);
                NonRecursiveHashSetInt hashSet = new NonRecursiveHashSetInt(xLength);
                SeqIterator it = xDataLib.iterator(xData);
                while (xDataLib.nextLoopCondition(xData, it)) {
                    hashSet.add(xDataLib.getNextInt(xData, it));
                }
                for (int i = tableLength - 1; i >= 0; i--) {
                    int val = tableDataLib.getInt(tableData, rit, i);
                    if (hashSet.contains(val)) {
                        hashTable.put(val, i);
                    }
                }
            } else {
                hashTable = new NonRecursiveHashMapInt(tableLength);
                for (int i = tableLength - 1; i >= 0; i--) {
                    hashTable.put(tableDataLib.getInt(tableData, rit, i), i);
                }
            }
            SeqIterator it = xDataLib.iterator(xData);
            while (xDataLib.nextLoopCondition(xData, it)) {
                int xx = xDataLib.getNextInt(xData, it);
                int index = hashTable.get(xx);
                if (index != -1) {
                    result[it.getIndex()] = index + 1;
                } else {
                    matchAll = false;
                }
            }
            return RDataFactory.createIntVector(result, setCompleteState(matchAll, nomatch));
        }
    }

    @ImportStatic(DSLConfig.class)
    protected abstract static class MatchAsDoubleVectorNode extends MatchAsNode {
        @Specialization(limit = "getTypedVectorDataLibraryCacheSize()")
        protected RIntVector matchDouble(RAbstractAtomicVector x, RAbstractAtomicVector table, int nomatch,
                        @CachedLibrary("x.getData()") VectorDataLibrary xDataLib,
                        @CachedLibrary("table.getData()") VectorDataLibrary tableDataLib) {
            Object xData = x.getData();
            Object tableData = table.getData();
            int xLength = xDataLib.getLength(xData);
            int tableLength = tableDataLib.getLength(tableData);
            int[] result = initResult(xLength, nomatch);
            boolean matchAll = true;
            RandomAccessIterator rit = tableDataLib.randomAccessIterator(tableData);
            NonRecursiveHashMapDouble hashTable;
            if (bigTableProfile.profile(tableLength > (xLength * TABLE_SIZE_FACTOR))) {
                hashTable = new NonRecursiveHashMapDouble(xLength);
                NonRecursiveHashSetDouble hashSet = new NonRecursiveHashSetDouble(xLength);
                SeqIterator it = xDataLib.iterator(xData);
                while (xDataLib.nextLoopCondition(xData, it)) {
                    hashSet.add(xDataLib.getNextDouble(xData, it));
                }
                for (int i = tableLength - 1; i >= 0; i--) {
                    double val = tableDataLib.getDouble(tableData, rit, i);
                    if (hashSet.contains(val)) {
                        hashTable.put(val, i);
                    }
                }
            } else {
                hashTable = new NonRecursiveHashMapDouble(tableLength);
                for (int i = tableLength - 1; i >= 0; i--) {
                    hashTable.put(tableDataLib.getDouble(tableData, rit, i), i);
                }
            }
            SeqIterator it = xDataLib.iterator(xData);
            while (xDataLib.nextLoopCondition(xData, it)) {
                double xx = xDataLib.getNextDouble(xData, it);
                int index = hashTable.get(xx);
                if (index != -1) {
                    result[it.getIndex()] = index + 1;
                } else {
                    matchAll = false;
                }
            }
            return RDataFactory.createIntVector(result, setCompleteState(matchAll, nomatch));
        }
    }

    @ImportStatic(DSLConfig.class)
    protected abstract static class MatchAsComplexVectorNode extends MatchAsNode {
        @Specialization(limit = "getTypedVectorDataLibraryCacheSize()")
        protected RIntVector matchComplex(RAbstractAtomicVector x, RAbstractAtomicVector table, int nomatch,
                        @CachedLibrary("x.getData()") VectorDataLibrary xDataLib,
                        @CachedLibrary("table.getData()") VectorDataLibrary tableDataLib) {
            Object xData = x.getData();
            Object tableData = table.getData();
            int xLength = xDataLib.getLength(xData);
            int tableLength = tableDataLib.getLength(tableData);

            int[] result = initResult(xLength, nomatch);
            boolean matchAll = true;
            RandomAccessIterator rit = tableDataLib.randomAccessIterator(tableData);
            NonRecursiveHashMapComplex hashTable;
            if (bigTableProfile.profile(tableLength > (xLength * TABLE_SIZE_FACTOR))) {
                hashTable = new NonRecursiveHashMapComplex(xLength);
                NonRecursiveHashSetComplex hashSet = new NonRecursiveHashSetComplex(xLength);
                SeqIterator it = xDataLib.iterator(xData);
                while (xDataLib.nextLoopCondition(xData, it)) {
                    hashSet.add(xDataLib.getNextComplex(xData, it));
                }
                for (int i = tableLength - 1; i >= 0; i--) {
                    RComplex val = tableDataLib.getComplex(tableData, rit, i);
                    if (hashSet.contains(val)) {
                        hashTable.put(val, i);
                    }
                }
            } else {
                hashTable = new NonRecursiveHashMapComplex(tableLength);
                for (int i = tableLength - 1; i >= 0; i--) {
                    hashTable.put(tableDataLib.getComplex(tableData, rit, i), i);
                }
            }
            SeqIterator it = xDataLib.iterator(xData);
            while (xDataLib.nextLoopCondition(xData, it)) {
                RComplex xx = xDataLib.getNextComplex(xData, it);
                int index = hashTable.get(xx);
                if (index != -1) {
                    result[it.getIndex()] = index + 1;
                } else {
                    matchAll = false;
                }
            }
            return RDataFactory.createIntVector(result, setCompleteState(matchAll, nomatch));
        }
    }

    @ImportStatic(DSLConfig.class)
    protected abstract static class MatchAsStringVectorNode extends MatchAsNode {
        @Specialization(limit = "getTypedVectorDataLibraryCacheSize()")
        protected RIntVector match(RAbstractAtomicVector x, RAbstractAtomicVector table, int nomatch,
                        @CachedLibrary("x.getData()") VectorDataLibrary xDataLib,
                        @CachedLibrary("table.getData()") VectorDataLibrary tableDataLib) {
            Object xData = x.getData();
            Object tableData = table.getData();
            int xLength = xDataLib.getLength(xData);
            int tableLength = tableDataLib.getLength(tableData);
            int[] result = initResult(xLength, nomatch);
            boolean matchAll = true;
            NonRecursiveHashMapCharacter hashTable;
            RandomAccessIterator rit = tableDataLib.randomAccessIterator(tableData);
            if (bigTableProfile.profile(tableLength > (xLength * TABLE_SIZE_FACTOR))) {
                hashTable = new NonRecursiveHashMapCharacter(xLength);
                NonRecursiveHashSetCharacter hashSet = new NonRecursiveHashSetCharacter(xLength);
                SeqIterator it = xDataLib.iterator(xData);
                while (xDataLib.nextLoopCondition(xData, it)) {
                    hashSet.add(xDataLib.getNextString(xData, it));
                }
                for (int i = tableLength - 1; i >= 0; i--) {
                    String val = tableDataLib.getString(tableData, rit, i);
                    if (hashSet.contains(val)) {
                        hashTable.put(val, i);
                    }
                }
            } else {
                hashTable = new NonRecursiveHashMapCharacter(tableLength);
                for (int i = tableLength - 1; i >= 0; i--) {
                    hashTable.put(tableDataLib.getString(tableData, rit, i), i);
                }
            }
            SeqIterator it = xDataLib.iterator(xData);
            while (xDataLib.nextLoopCondition(xData, it)) {
                String xx = xDataLib.getNextString(xData, it);
                int index = hashTable.get(xx);
                if (index != -1) {
                    result[it.getIndex()] = index + 1;
                } else {
                    matchAll = false;
                }
            }
            return RDataFactory.createIntVector(result, setCompleteState(matchAll, nomatch));
        }
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
