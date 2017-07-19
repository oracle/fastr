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

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetNamesAttributeNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.helpers.RFactorNodes;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RRawVector;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;

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
    @Child private GetNamesAttributeNode getNames = GetNamesAttributeNode.create();

    @SuppressWarnings("unused") private final ConditionProfile noStringLevels = ConditionProfile.createBinaryProfile();
    private final ConditionProfile namesProfile = ConditionProfile.createBinaryProfile();

    private static final int INITIAL_SIZE = 5;
    private static final int SCALE_FACTOR = 2;

    static {
        Casts.noCasts(Split.class);
    }

    @Specialization
    protected RList split(RAbstractListVector x, RAbstractIntVector f) {
        int[] factor = f.materialize().getDataWithoutCopying();
        RStringVector names = getLevelNode.execute(f);
        final int nLevels = getNLevels(names);

        // initialise result arrays
        Object[][] collectResults = new Object[nLevels][];
        int[] collectResultSize = new int[nLevels];
        for (int i = 0; i < collectResults.length; i++) {
            collectResults[i] = new Object[INITIAL_SIZE];
        }

        // perform split
        for (int i = 0, fi = 0; i < x.getLength(); ++i, fi = Utils.incMod(fi, factor.length)) {
            int resultIndex = factor[fi] - 1; // a factor is a 1-based int vector
            Object[] collect = collectResults[resultIndex];
            if (collect.length == collectResultSize[resultIndex]) {
                collectResults[resultIndex] = Arrays.copyOf(collect, collect.length * SCALE_FACTOR);
                collect = collectResults[resultIndex];
            }
            collect[collectResultSize[resultIndex]++] = x.getDataAt(i);
        }

        // assemble result vectors and level names
        Object[] results = new Object[nLevels];
        for (int i = 0; i < nLevels; i++) {
            results[i] = RDataFactory.createList(Arrays.copyOfRange(collectResults[i], 0, collectResultSize[i]));
        }

        return RDataFactory.createList(results, names);
    }

    @Specialization
    protected RList split(RAbstractIntVector x, RAbstractIntVector f) {
        int[] factor = f.materialize().getDataWithoutCopying();
        RStringVector names = getLevelNode.execute(f);
        final int nLevels = getNLevels(names);

        // initialise result arrays
        int[][] collectResults = new int[nLevels][];
        int[] collectResultSize = new int[nLevels];
        for (int i = 0; i < collectResults.length; i++) {
            collectResults[i] = new int[INITIAL_SIZE];
        }

        // perform split
        for (int i = 0, fi = 0; i < x.getLength(); ++i, fi = Utils.incMod(fi, factor.length)) {
            int resultIndex = factor[fi] - 1; // a factor is a 1-based int vector
            int[] collect = collectResults[resultIndex];
            if (collect.length == collectResultSize[resultIndex]) {
                collectResults[resultIndex] = Arrays.copyOf(collect, collect.length * SCALE_FACTOR);
                collect = collectResults[resultIndex];
            }
            collect[collectResultSize[resultIndex]++] = x.getDataAt(i);
        }

        Object[] results = new Object[nLevels];
        RStringVector xNames = getNames.getNames(x);
        if (namesProfile.profile(xNames != null)) {
            String[][] resultNames = new String[nLevels][];
            int[] resultNamesIdxs = new int[nLevels];
            for (int i = 0; i < nLevels; i++) {
                resultNames[i] = new String[collectResultSize[i]];
            }
            for (int i = 0, fi = 0; i < x.getLength(); ++i, fi = Utils.incMod(fi, factor.length)) {
                int resultIndex = factor[fi] - 1; // a factor is a 1-based int vector
                resultNames[resultIndex][resultNamesIdxs[resultIndex]++] = xNames.getDataAt(i);
            }
            for (int i = 0; i < nLevels; i++) {
                results[i] = RDataFactory.createIntVector(Arrays.copyOfRange(collectResults[i], 0, collectResultSize[i]), x.isComplete(),
                                RDataFactory.createStringVector(resultNames[i], xNames.isComplete()));
            }
        } else {
            // assemble result vectors and level names
            for (int i = 0; i < nLevels; i++) {
                results[i] = RDataFactory.createIntVector(Arrays.copyOfRange(collectResults[i], 0, collectResultSize[i]), x.isComplete());
            }
        }

        return RDataFactory.createList(results, names);
    }

    @Specialization
    protected RList split(RAbstractDoubleVector x, RAbstractIntVector f) {
        int[] factor = f.materialize().getDataWithoutCopying();
        RStringVector names = getLevelNode.execute(f);
        final int nLevels = getNLevels(names);

        // initialise result arrays
        double[][] collectResults = new double[nLevels][];
        int[] collectResultSize = new int[nLevels];
        for (int i = 0; i < collectResults.length; i++) {
            collectResults[i] = new double[INITIAL_SIZE];
        }

        // perform split
        for (int i = 0, fi = 0; i < x.getLength(); ++i, fi = Utils.incMod(fi, factor.length)) {
            int resultIndex = factor[fi] - 1; // a factor is a 1-based int vector
            double[] collect = collectResults[resultIndex];
            if (collect.length == collectResultSize[resultIndex]) {
                collectResults[resultIndex] = Arrays.copyOf(collect, collect.length * SCALE_FACTOR);
                collect = collectResults[resultIndex];
            }
            collect[collectResultSize[resultIndex]++] = x.getDataAt(i);
        }

        // assemble result vectors and level names
        Object[] results = new Object[nLevels];
        for (int i = 0; i < nLevels; i++) {
            results[i] = RDataFactory.createDoubleVector(Arrays.copyOfRange(collectResults[i], 0, collectResultSize[i]), RDataFactory.COMPLETE_VECTOR);
        }

        return RDataFactory.createList(results, names);
    }

    @Specialization
    protected RList split(RAbstractStringVector x, RAbstractIntVector f) {
        int[] factor = f.materialize().getDataWithoutCopying();
        RStringVector names = getLevelNode.execute(f);
        final int nLevels = getNLevels(names);

        // initialise result arrays
        String[][] collectResults = new String[nLevels][];
        int[] collectResultSize = new int[nLevels];
        for (int i = 0; i < collectResults.length; i++) {
            collectResults[i] = new String[INITIAL_SIZE];
        }

        // perform split
        for (int i = 0, fi = 0; i < x.getLength(); ++i, fi = Utils.incMod(fi, factor.length)) {
            int resultIndex = factor[fi] - 1; // a factor is a 1-based int vector
            String[] collect = collectResults[resultIndex];
            if (collect.length == collectResultSize[resultIndex]) {
                collectResults[resultIndex] = Arrays.copyOf(collect, collect.length * SCALE_FACTOR);
                collect = collectResults[resultIndex];
            }
            collect[collectResultSize[resultIndex]++] = x.getDataAt(i);
        }

        // assemble result vectors and level names
        Object[] results = new Object[nLevels];
        for (int i = 0; i < nLevels; i++) {
            results[i] = RDataFactory.createStringVector(Arrays.copyOfRange(collectResults[i], 0, collectResultSize[i]), RDataFactory.COMPLETE_VECTOR);
        }

        return RDataFactory.createList(results, names);
    }

    @Specialization
    protected RList split(RAbstractLogicalVector x, RAbstractIntVector f) {
        int[] factor = f.materialize().getDataWithoutCopying();
        RStringVector names = getLevelNode.execute(f);
        final int nLevels = getNLevels(names);

        // initialise result arrays
        byte[][] collectResults = new byte[nLevels][];
        int[] collectResultSize = new int[nLevels];
        for (int i = 0; i < collectResults.length; i++) {
            collectResults[i] = new byte[INITIAL_SIZE];
        }

        // perform split
        for (int i = 0, fi = 0; i < x.getLength(); ++i, fi = Utils.incMod(fi, factor.length)) {
            int resultIndex = factor[fi] - 1; // a factor is a 1-based int vector
            byte[] collect = collectResults[resultIndex];
            if (collect.length == collectResultSize[resultIndex]) {
                collectResults[resultIndex] = Arrays.copyOf(collect, collect.length * SCALE_FACTOR);
                collect = collectResults[resultIndex];
            }
            collect[collectResultSize[resultIndex]++] = x.getDataAt(i);
        }

        // assemble result vectors and level names
        Object[] results = new Object[nLevels];
        for (int i = 0; i < nLevels; i++) {
            results[i] = RDataFactory.createLogicalVector(Arrays.copyOfRange(collectResults[i], 0, collectResultSize[i]), x.isComplete());
        }

        return RDataFactory.createList(results, names);
    }

    @Specialization
    protected RList split(RRawVector x, RAbstractIntVector f) {
        int[] factor = f.materialize().getDataWithoutCopying();
        RStringVector names = getLevelNode.execute(f);
        final int nLevels = getNLevels(names);

        // initialise result arrays
        byte[][] collectResults = new byte[nLevels][];
        int[] collectResultSize = new int[nLevels];
        for (int i = 0; i < collectResults.length; i++) {
            collectResults[i] = new byte[INITIAL_SIZE];
        }

        // perform split
        for (int i = 0, fi = 0; i < x.getLength(); ++i, fi = Utils.incMod(fi, factor.length)) {
            int resultIndex = factor[fi] - 1; // a factor is a 1-based int vector
            byte[] collect = collectResults[resultIndex];
            if (collect.length == collectResultSize[resultIndex]) {
                collectResults[resultIndex] = Arrays.copyOf(collect, collect.length * SCALE_FACTOR);
                collect = collectResults[resultIndex];
            }
            collect[collectResultSize[resultIndex]++] = x.getDataAt(i).getValue();
        }

        // assemble result vectors and level names
        Object[] results = new Object[nLevels];
        for (int i = 0; i < nLevels; i++) {
            results[i] = RDataFactory.createRawVector(Arrays.copyOfRange(collectResults[i], 0, collectResultSize[i]));
        }

        return RDataFactory.createList(results, names);
    }

    private static int getNLevels(RStringVector levels) {
        return levels != null ? levels.getLength() : 0;
    }
}
