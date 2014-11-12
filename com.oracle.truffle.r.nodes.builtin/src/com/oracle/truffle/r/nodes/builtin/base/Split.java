/*
 * Copyright (c) 2014, 2014, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import java.util.*;

import com.oracle.truffle.api.CompilerDirectives.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin(name = "split", kind = INTERNAL, parameterNames = {"x", "f"})
public abstract class Split extends RBuiltinNode {

    private static final int INITIAL_SIZE = 5;
    private static final int SCALE_FACTOR = 2;

    @Specialization
    protected RList split(RAbstractIntVector x, RFactor f) {
        HashMap<Object, Integer> levelMapping = getLevelMapping(f);
        final int nLevels = levelMapping.size();

        // initialise result arrays
        int[][] collectResults = new int[nLevels][];
        int[] collectResultSize = new int[nLevels];
        for (int i = 0; i < collectResults.length; ++i) {
            collectResults[i] = new int[INITIAL_SIZE];
        }

        // perform split
        for (int i = 0, fi = 0; i < x.getLength(); ++i, fi = Utils.incMod(fi, f.getLength())) {
            int resultIndex = levelMapping.get(f.getDataAtAsObject(fi));
            int[] collect = collectResults[resultIndex];
            if (collect.length == collectResultSize[resultIndex]) {
                collectResults[resultIndex] = Arrays.copyOf(collect, collect.length * SCALE_FACTOR);
                collect = collectResults[resultIndex];
            }
            collect[collectResultSize[resultIndex]++] = x.getDataAt(i);
        }

        // assemble result vectors and level names
        String[] names = new String[nLevels];
        Object[] results = new Object[nLevels];
        for (HashMap.Entry<Object, Integer> e : levelMapping.entrySet()) {
            Object level = e.getKey();
            int index = e.getValue();
            names[index] = RRuntime.toString(level);
            results[index] = RDataFactory.createIntVector(Arrays.copyOfRange(collectResults[index], 0, collectResultSize[index]), RDataFactory.COMPLETE_VECTOR);
        }

        return RDataFactory.createList(results, RDataFactory.createStringVector(names, RDataFactory.COMPLETE_VECTOR));
    }

    @Specialization
    protected RList split(RAbstractDoubleVector x, RFactor f) {
        HashMap<Object, Integer> levelMapping = getLevelMapping(f);
        final int nLevels = levelMapping.size();

        // initialise result arrays
        double[][] collectResults = new double[nLevels][];
        int[] collectResultSize = new int[nLevels];
        for (int i = 0; i < collectResults.length; ++i) {
            collectResults[i] = new double[INITIAL_SIZE];
        }

        // perform split
        for (int i = 0, fi = 0; i < x.getLength(); ++i, fi = Utils.incMod(fi, f.getLength())) {
            int resultIndex = levelMapping.get(f.getDataAtAsObject(fi));
            double[] collect = collectResults[resultIndex];
            if (collect.length == collectResultSize[resultIndex]) {
                collectResults[resultIndex] = Arrays.copyOf(collect, collect.length * SCALE_FACTOR);
                collect = collectResults[resultIndex];
            }
            collect[collectResultSize[resultIndex]++] = x.getDataAt(i);
        }

        // assemble result vectors and level names
        String[] names = new String[nLevels];
        Object[] results = new Object[nLevels];
        for (HashMap.Entry<Object, Integer> e : levelMapping.entrySet()) {
            Object level = e.getKey();
            int index = e.getValue();
            names[index] = RRuntime.toString(level);
            results[index] = RDataFactory.createDoubleVector(Arrays.copyOfRange(collectResults[index], 0, collectResultSize[index]), RDataFactory.COMPLETE_VECTOR);
        }

        return RDataFactory.createList(results, RDataFactory.createStringVector(names, RDataFactory.COMPLETE_VECTOR));
    }

    /**
     * Obtain a mapping from factor levels to integers, in the order of appearance of the levels in
     * the factor.
     */
    @TruffleBoundary
    private static HashMap<Object, Integer> getLevelMapping(RFactor f) {
        HashMap<Object, Integer> map = new HashMap<>();
        int lastIndex = 0;
        for (int i = 0; i < f.getLength(); ++i) {
            Object level = f.getDataAtAsObject(i);
            if (!map.containsKey(level)) {
                map.put(level, lastIndex++);
            }
        }
        return map;
    }

}
