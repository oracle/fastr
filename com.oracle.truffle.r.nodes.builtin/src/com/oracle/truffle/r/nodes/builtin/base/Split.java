/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import java.util.Arrays;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetNamesAttributeNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.base.SplitNodeGen.GetSplitNamesNodeGen;
import com.oracle.truffle.r.nodes.helpers.RFactorNodes;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess.SequentialIterator;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

/**
 * The {@code split} internal. Internal version of 'split' is invoked from 'split.default' function
 * implemented in R, which makes sure that the second argument is always a R factor.
 *
 * TODO Can we find a way to efficiently write the specializations as generics? The code is
 * identical except for the argument type.
 *
 * TODO: GNU R preserves the corresponding values of names attribute. There are (ignored) tests for
 * this in TestBuiltin_split.
 */
@RBuiltin(name = "split", kind = INTERNAL, parameterNames = {"x", "f"}, behavior = PURE)
public abstract class Split extends RBuiltinNode.Arg2 {

    @Child private RFactorNodes.GetLevels getLevelNode = new RFactorNodes.GetLevels();
    @Child private GetSplitNames getSplitNames = GetSplitNamesNodeGen.create();

    private static final int INITIAL_SIZE = 5;
    private static final int SCALE_FACTOR = 2;

    static {
        Casts.noCasts(Split.class);
    }

    @Specialization(limit = "4", guards = {"xAccess.supports(x)", "fAccess.supports(f)"})
    protected RList split(RAbstractVector x, RAbstractIntVector f,
                    @Cached("x.access()") VectorAccess xAccess,
                    @Cached("f.access()") VectorAccess fAccess) {
        try (SequentialIterator xIter = xAccess.access(x); SequentialIterator fIter = fAccess.access(f)) {
            RStringVector names = getLevelNode.execute(f);
            int nLevels = getNLevels(names);
            int[] collectResultSize = new int[nLevels];
            Object[] results = new Object[nLevels];

            switch (xAccess.getType()) {
                case Character: {
                    // Initialize result arrays
                    String[][] collectResults = new String[nLevels][INITIAL_SIZE];

                    // perform split
                    while (xAccess.next(xIter)) {
                        fAccess.nextWithWrap(fIter);
                        int resultIndex = fAccess.getInt(fIter) - 1; // a factor is a 1-based int
                                                                     // vector
                        String[] collect = collectResults[resultIndex];
                        if (collect.length == collectResultSize[resultIndex]) {
                            collectResults[resultIndex] = collect = Arrays.copyOf(collect, collect.length * SCALE_FACTOR);
                        }
                        collect[collectResultSize[resultIndex]++] = xAccess.getString(xIter);
                    }

                    RStringVector[] resultNames = getSplitNames.getNames(x, fAccess, fIter, nLevels, collectResultSize);
                    for (int i = 0; i < nLevels; i++) {
                        results[i] = RDataFactory.createStringVector(Arrays.copyOfRange(collectResults[i], 0, collectResultSize[i]), x.isComplete(),
                                        (resultNames != null) ? resultNames[i] : null);
                    }
                    break;
                }
                case Complex: {
                    // Initialize result arrays
                    double[][] collectResults = new double[nLevels][INITIAL_SIZE * 2];

                    // perform split
                    while (xAccess.next(xIter)) {
                        fAccess.nextWithWrap(fIter);
                        int resultIndex = fAccess.getInt(fIter) - 1; // a factor is a 1-based int
                                                                     // vector
                        double[] collect = collectResults[resultIndex];
                        if (collect.length == collectResultSize[resultIndex] * 2) {
                            collectResults[resultIndex] = collect = Arrays.copyOf(collect, collect.length * SCALE_FACTOR);
                        }
                        collect[collectResultSize[resultIndex] * 2] = xAccess.getComplexR(xIter);
                        collect[collectResultSize[resultIndex] * 2 + 1] = xAccess.getComplexI(xIter);
                        collectResultSize[resultIndex]++;
                    }

                    RStringVector[] resultNames = getSplitNames.getNames(x, fAccess, fIter, nLevels, collectResultSize);
                    for (int i = 0; i < nLevels; i++) {
                        results[i] = RDataFactory.createComplexVector(Arrays.copyOfRange(collectResults[i], 0, collectResultSize[i] * 2), x.isComplete(),
                                        (resultNames != null) ? resultNames[i] : null);
                    }
                    break;
                }
                case Double: {
                    // Initialize result arrays
                    double[][] collectResults = new double[nLevels][INITIAL_SIZE];

                    // perform split
                    while (xAccess.next(xIter)) {
                        fAccess.nextWithWrap(fIter);
                        int resultIndex = fAccess.getInt(fIter) - 1; // a factor is a 1-based int
                                                                     // vector
                        double[] collect = collectResults[resultIndex];
                        if (collect.length == collectResultSize[resultIndex]) {
                            collectResults[resultIndex] = collect = Arrays.copyOf(collect, collect.length * SCALE_FACTOR);
                        }
                        collect[collectResultSize[resultIndex]++] = xAccess.getDouble(xIter);
                    }

                    RStringVector[] resultNames = getSplitNames.getNames(x, fAccess, fIter, nLevels, collectResultSize);
                    for (int i = 0; i < nLevels; i++) {
                        results[i] = RDataFactory.createDoubleVector(Arrays.copyOfRange(collectResults[i], 0, collectResultSize[i]), x.isComplete(),
                                        (resultNames != null) ? resultNames[i] : null);
                    }
                    break;
                }
                case Integer: {
                    // Initialize result arrays
                    int[][] collectResults = new int[nLevels][INITIAL_SIZE];

                    // perform split
                    while (xAccess.next(xIter)) {
                        fAccess.nextWithWrap(fIter);
                        int resultIndex = fAccess.getInt(fIter) - 1; // a factor is a 1-based int
                                                                     // vector
                        int[] collect = collectResults[resultIndex];
                        if (collect.length == collectResultSize[resultIndex]) {
                            collectResults[resultIndex] = collect = Arrays.copyOf(collect, collect.length * SCALE_FACTOR);
                        }
                        collect[collectResultSize[resultIndex]++] = xAccess.getInt(xIter);
                    }

                    RStringVector[] resultNames = getSplitNames.getNames(x, fAccess, fIter, nLevels, collectResultSize);
                    for (int i = 0; i < nLevels; i++) {
                        results[i] = RDataFactory.createIntVector(Arrays.copyOfRange(collectResults[i], 0, collectResultSize[i]), x.isComplete(),
                                        (resultNames != null) ? resultNames[i] : null);
                    }
                    break;
                }
                case List: {
                    // Initialize result arrays
                    Object[][] collectResults = new Object[nLevels][INITIAL_SIZE];

                    // perform split
                    while (xAccess.next(xIter)) {
                        fAccess.nextWithWrap(fIter);
                        int resultIndex = fAccess.getInt(fIter) - 1; // a factor is a 1-based int
                                                                     // vector
                        Object[] collect = collectResults[resultIndex];
                        if (collect.length == collectResultSize[resultIndex]) {
                            collectResults[resultIndex] = collect = Arrays.copyOf(collect, collect.length * SCALE_FACTOR);
                        }
                        collect[collectResultSize[resultIndex]++] = xAccess.getListElement(xIter);
                    }

                    RStringVector[] resultNames = getSplitNames.getNames(x, fAccess, fIter, nLevels, collectResultSize);
                    for (int i = 0; i < nLevels; i++) {
                        results[i] = RDataFactory.createList(Arrays.copyOfRange(collectResults[i], 0, collectResultSize[i]),
                                        (resultNames != null) ? resultNames[i] : null);
                    }
                    break;
                }
                case Logical: {
                    // Initialize result arrays
                    byte[][] collectResults = new byte[nLevels][INITIAL_SIZE];

                    // perform split
                    while (xAccess.next(xIter)) {
                        fAccess.nextWithWrap(fIter);
                        int resultIndex = fAccess.getInt(fIter) - 1; // a factor is a 1-based int
                                                                     // vector
                        byte[] collect = collectResults[resultIndex];
                        if (collect.length == collectResultSize[resultIndex]) {
                            collectResults[resultIndex] = collect = Arrays.copyOf(collect, collect.length * SCALE_FACTOR);
                        }
                        collect[collectResultSize[resultIndex]++] = xAccess.getLogical(xIter);
                    }

                    RStringVector[] resultNames = getSplitNames.getNames(x, fAccess, fIter, nLevels, collectResultSize);
                    for (int i = 0; i < nLevels; i++) {
                        results[i] = RDataFactory.createLogicalVector(Arrays.copyOfRange(collectResults[i], 0, collectResultSize[i]), x.isComplete(),
                                        (resultNames != null) ? resultNames[i] : null);
                    }
                    break;
                }
                case Raw: {
                    // Initialize result arrays
                    byte[][] collectResults = new byte[nLevels][INITIAL_SIZE];

                    // perform split
                    while (xAccess.next(xIter)) {
                        fAccess.nextWithWrap(fIter);
                        int resultIndex = fAccess.getInt(fIter) - 1; // a factor is a 1-based int
                                                                     // vector
                        byte[] collect = collectResults[resultIndex];
                        if (collect.length == collectResultSize[resultIndex]) {
                            collectResults[resultIndex] = collect = Arrays.copyOf(collect, collect.length * SCALE_FACTOR);
                        }
                        collect[collectResultSize[resultIndex]++] = xAccess.getRaw(xIter);
                    }

                    RStringVector[] resultNames = getSplitNames.getNames(x, fAccess, fIter, nLevels, collectResultSize);
                    for (int i = 0; i < nLevels; i++) {
                        results[i] = RDataFactory.createRawVector(Arrays.copyOfRange(collectResults[i], 0, collectResultSize[i]),
                                        (resultNames != null) ? resultNames[i] : null);
                    }
                    break;
                }
                default:
                    throw error(Message.UNIMPLEMENTED_TYPE_IN_FUNCTION, xAccess.getType().getName(), "split");
            }
            return RDataFactory.createList(results, names);
        }
    }

    @Specialization(replaces = "split")
    protected RList splitGeneric(RAbstractVector x, RAbstractIntVector f) {
        return split(x, f, x.slowPathAccess(), f.slowPathAccess());
    }

    protected abstract static class GetSplitNames extends RBaseNode {

        private final ConditionProfile namesProfile = ConditionProfile.createBinaryProfile();
        @Child private GetNamesAttributeNode getNamesNode = GetNamesAttributeNode.create();

        private RStringVector[] getNames(RAbstractVector x, VectorAccess fAccess, SequentialIterator fIter, int nLevels, int[] collectResultSize) {
            RStringVector xNames = getNamesNode.getNames(x);
            if (namesProfile.profile(xNames != null)) {
                String[][] namesArr = new String[nLevels][];
                int[] resultNamesIdxs = new int[nLevels];
                for (int i = 0; i < nLevels; i++) {
                    namesArr[i] = new String[collectResultSize[i]];
                }
                execute(fAccess, fIter, xNames, namesArr, resultNamesIdxs);
                RStringVector[] resultNames = new RStringVector[nLevels];
                for (int i = 0; i < nLevels; i++) {
                    resultNames[i] = RDataFactory.createStringVector(namesArr[i], xNames.isComplete());
                }
                return resultNames;
            }
            return null;
        }

        protected abstract void execute(VectorAccess fAccess, SequentialIterator fIter, RStringVector names, String[][] namesArr, int[] resultNamesIdxs);

        @Specialization(guards = "namesAccess.supports(names)")
        protected void fillNames(VectorAccess fAccess, SequentialIterator fIter, RStringVector names, String[][] namesArr, int[] resultNamesIdxs,
                        @Cached("names.access()") VectorAccess namesAccess) {
            try (SequentialIterator namesIter = namesAccess.access(names)) {
                while (namesAccess.next(namesIter)) {
                    fAccess.nextWithWrap(fIter);
                    int resultIndex = fAccess.getInt(fIter) - 1; // a factor is a 1-based int
                                                                 // vector
                    namesArr[resultIndex][resultNamesIdxs[resultIndex]++] = namesAccess.getString(namesIter);
                }
            }
        }

        @Specialization(replaces = "fillNames")
        protected void fillNamesGeneric(VectorAccess fAccess, SequentialIterator fIter, RStringVector names, String[][] namesArr, int[] resultNamesIdxs) {
            fillNames(fAccess, fIter, names, namesArr, resultNamesIdxs, names.slowPathAccess());
        }
    }

    private static int getNLevels(RStringVector levels) {
        return levels != null ? levels.getLength() : 0;
    }
}
