/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.oracle.truffle.r.nodes.builtin.casts.Filter.AndFilter;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.CompareFilter;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.CompareFilter.ScalarValue;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.DoubleFilter;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.MissingFilter;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.NotFilter;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.NullFilter;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.OrFilter;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.RTypeFilter;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.TypeFilter;
import com.oracle.truffle.r.nodes.builtin.casts.Mapper.MapByteToBoolean;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.CoercionStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.FilterStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.FindFirstStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.MapIfStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.MapStep;
import com.oracle.truffle.r.nodes.builtin.casts.analysis.ForwardedValuesAnalyser;
import com.oracle.truffle.r.nodes.builtin.casts.analysis.ForwardingAnalysisResult;
import com.oracle.truffle.r.runtime.RType;

public class ForwardedValuesAnalyserTest {
    @Test
    public void testCoercion() {
        PipelineStep<?, ?> firstStep = new CoercionStep<>(RType.Logical, false);

        ForwardedValuesAnalyser fwdAn = new ForwardedValuesAnalyser();
        ForwardingAnalysisResult result = fwdAn.analyse(firstStep);
        assertTrue(result.isLogicalForwarded());
        assertTrue(result.isNullForwarded());
        assertTrue(result.isMissingForwarded());
        assertFalse(result.isIntegerForwarded());
        assertFalse(result.isStringForwarded());
    }

    @Test
    public void testFindFirst() {
        PipelineStep<?, ?> firstStep = new CoercionStep<>(RType.Character, false).setNext(new FindFirstStep<>("hello", String.class, null));

        ForwardedValuesAnalyser fwdAn = new ForwardedValuesAnalyser();
        ForwardingAnalysisResult result = fwdAn.analyse(firstStep);
        assertTrue(result.isStringForwarded());
        assertFalse(result.isLogicalForwarded());
        assertFalse(result.isNullForwarded());
        assertFalse(result.isMissingForwarded());
        assertFalse(result.isIntegerForwarded());
    }

    @Test
    public void testRTypeFilter() {
        PipelineStep<?, ?> firstStep = new FilterStep<>(new RTypeFilter<>(RType.Integer), null, false);

        ForwardedValuesAnalyser fwdAn = new ForwardedValuesAnalyser();
        ForwardingAnalysisResult result = fwdAn.analyse(firstStep);
        assertTrue(result.isIntegerForwarded());
        assertFalse(result.isStringForwarded());
        assertFalse(result.isNullForwarded());
        assertFalse(result.isMissingForwarded());
    }

    @Test
    public void testTypeFilter() {
        PipelineStep<?, ?> firstStep = new FilterStep<>(new TypeFilter<>(Integer.class), null, false);

        ForwardedValuesAnalyser fwdAn = new ForwardedValuesAnalyser();
        ForwardingAnalysisResult result = fwdAn.analyse(firstStep);
        assertTrue(result.isIntegerForwarded());
        assertFalse(result.isStringForwarded());
        assertFalse(result.isNullForwarded());
        assertFalse(result.isMissingForwarded());
    }

    @Test
    public void testTypeFilterWithExtraCondition() {
        PipelineStep<?, ?> firstStep = new FilterStep<>(new TypeFilter<>(Integer.class, x -> x > 1), null, false);

        ForwardedValuesAnalyser fwdAn = new ForwardedValuesAnalyser();
        ForwardingAnalysisResult result = fwdAn.analyse(firstStep);
        assertTrue(result.integerForwarded.isUnknown());
        assertFalse(result.isStringForwarded());
        assertFalse(result.isNullForwarded());
        assertFalse(result.isMissingForwarded());
    }

    @Test
    public void testOrFilter1() {
        PipelineStep<?, ?> firstStep = new FilterStep<>(new OrFilter<>(new RTypeFilter<>(RType.Integer), new RTypeFilter<>(RType.Double)), null, false);

        ForwardedValuesAnalyser fwdAn = new ForwardedValuesAnalyser();
        ForwardingAnalysisResult result = fwdAn.analyse(firstStep);
        assertTrue(result.isIntegerForwarded());
        assertTrue(result.isDoubleForwarded());
        assertFalse(result.isStringForwarded());
        assertFalse(result.isNullForwarded());
        assertFalse(result.isMissingForwarded());
    }

    @Test
    public void testOrFilter2() {
        PipelineStep<?, ?> firstStep = new FilterStep<>(new OrFilter<>(new TypeFilter<>(Integer.class, x -> x > 1), new RTypeFilter<>(RType.Double)), null, false);

        ForwardedValuesAnalyser fwdAn = new ForwardedValuesAnalyser();
        ForwardingAnalysisResult result = fwdAn.analyse(firstStep);
        assertTrue(result.integerForwarded.isUnknown());
        assertTrue(result.isDoubleForwarded());
        assertFalse(result.isStringForwarded());
        assertFalse(result.isNullForwarded());
        assertFalse(result.isMissingForwarded());
    }

    @Test
    public void testAndFilter1() {
        PipelineStep<?, ?> firstStep = new FilterStep<>(new AndFilter<>(new RTypeFilter<>(RType.Integer), new RTypeFilter<>(RType.Double)), null, false);

        ForwardedValuesAnalyser fwdAn = new ForwardedValuesAnalyser();
        ForwardingAnalysisResult result = fwdAn.analyse(firstStep);
        assertFalse(result.isIntegerForwarded());
        assertFalse(result.isDoubleForwarded());
        assertFalse(result.isStringForwarded());
        assertFalse(result.isNullForwarded());
        assertFalse(result.isMissingForwarded());
    }

    @Test
    public void testNot() {
        PipelineStep<?, ?> firstStep = new FilterStep<>(new AndFilter<>(new NotFilter<>(new RTypeFilter<>(RType.Integer)), new NotFilter<>(new RTypeFilter<>(RType.Double))), null, false);

        ForwardedValuesAnalyser fwdAn = new ForwardedValuesAnalyser();
        ForwardingAnalysisResult result = fwdAn.analyse(firstStep);
        assertTrue(result.isStringForwarded());
        assertTrue(result.isComplexForwarded());
        assertTrue(result.isNullForwarded());
        assertTrue(result.isMissingForwarded());
        assertFalse(result.isIntegerForwarded());
        assertFalse(result.isDoubleForwarded());
    }

    @Test
    public void testAndFilter2() {
        PipelineStep<?, ?> firstStep = new FilterStep<>(new AndFilter<>(new TypeFilter<>(Integer.class, x -> x > 1), new TypeFilter<>(Integer.class, x -> x % 2 == 0)), null, false);

        ForwardedValuesAnalyser fwdAn = new ForwardedValuesAnalyser();
        ForwardingAnalysisResult result = fwdAn.analyse(firstStep);
        assertTrue(result.integerForwarded.isUnknown());
        assertFalse(result.isDoubleForwarded());
        assertFalse(result.isStringForwarded());
        assertFalse(result.isNullForwarded());
        assertFalse(result.isMissingForwarded());
    }

    @Test
    public void testIntegerCoercionFollowedByFilterWithExtraCondition() {
        PipelineStep<?, ?> firstStep = new CoercionStep<>(RType.Integer, false).setNext(new FindFirstStep<>(1, Integer.class, null)).setNext(
                        new FilterStep<>(new TypeFilter<>(Integer.class, x -> x > 1), null, false));

        ForwardedValuesAnalyser fwdAn = new ForwardedValuesAnalyser();
        ForwardingAnalysisResult result = fwdAn.analyse(firstStep);
        assertTrue(result.integerForwarded.isUnknown());
        assertFalse(result.isDoubleForwarded());
        assertFalse(result.isStringForwarded());
        assertFalse(result.isNullForwarded());
        assertFalse(result.isMissingForwarded());
    }

    @Test
    public void testCompareScalarValueFilter() {
        PipelineStep<?, ?> firstStep = new FilterStep<>(new CompareFilter<>(CompareFilter.EQ, new CompareFilter.ScalarValue(1, RType.Integer)), null, false);

        ForwardedValuesAnalyser fwdAn = new ForwardedValuesAnalyser();
        ForwardingAnalysisResult result = fwdAn.analyse(firstStep);
        assertTrue(result.integerForwarded.isUnknown());
        assertFalse(result.isDoubleForwarded());
        assertFalse(result.isStringForwarded());
        assertFalse(result.isNullForwarded());
        assertFalse(result.isMissingForwarded());
    }

    @Test
    public void testCompareNAValueFilter() {
        PipelineStep<?, ?> firstStep = new FilterStep<>(new CompareFilter<>(CompareFilter.EQ, new CompareFilter.NATest(RType.Integer)), null, false);

        ForwardedValuesAnalyser fwdAn = new ForwardedValuesAnalyser();
        ForwardingAnalysisResult result = fwdAn.analyse(firstStep);
        assertTrue(result.integerForwarded.isUnknown());
        assertFalse(result.isDoubleForwarded());
        assertFalse(result.isStringForwarded());
        assertFalse(result.isNullForwarded());
        assertFalse(result.isMissingForwarded());
    }

    @Test
    public void testCompareStringLengthFilter() {
        PipelineStep<?, ?> firstStep = new FilterStep<>(new CompareFilter<>(CompareFilter.EQ, new CompareFilter.StringLength(1)), null, false);

        ForwardedValuesAnalyser fwdAn = new ForwardedValuesAnalyser();
        ForwardingAnalysisResult result = fwdAn.analyse(firstStep);
        assertTrue(result.stringForwarded.isUnknown());
        assertFalse(result.isDoubleForwarded());
        assertFalse(result.isIntegerForwarded());
        assertFalse(result.isNullForwarded());
        assertFalse(result.isMissingForwarded());
    }

    @Test
    public void testCompareEmptyVectorFilter() {
        PipelineStep<?, ?> firstStep = new FilterStep<>(new CompareFilter<>(CompareFilter.EQ, new CompareFilter.VectorSize(0)), null, false);

        ForwardedValuesAnalyser fwdAn = new ForwardedValuesAnalyser();
        ForwardingAnalysisResult result = fwdAn.analyse(firstStep);
        assertTrue(result.isNullForwarded());
        assertFalse(result.isMissingForwarded());
        assertFalse(result.isIntegerForwarded());
        assertFalse(result.isDoubleForwarded());
        assertFalse(result.isIntegerForwarded());
    }

    @Test
    public void testCompareOneElementVectorFilter() {
        PipelineStep<?, ?> firstStep = new FilterStep<>(new CompareFilter<>(CompareFilter.EQ, new CompareFilter.VectorSize(1)), null, false);

        ForwardedValuesAnalyser fwdAn = new ForwardedValuesAnalyser();
        ForwardingAnalysisResult result = fwdAn.analyse(firstStep);
        assertTrue(result.isIntegerForwarded());
        assertTrue(result.isDoubleForwarded());
        assertTrue(result.isIntegerForwarded());
        assertFalse(result.isNullForwarded());
        assertFalse(result.isMissingForwarded());
    }

    @Test
    public void testCompareMoreElementsVectorFilter() {
        PipelineStep<?, ?> firstStep = new FilterStep<>(new CompareFilter<>(CompareFilter.GE, new CompareFilter.VectorSize(2)), null, false);

        ForwardedValuesAnalyser fwdAn = new ForwardedValuesAnalyser();
        ForwardingAnalysisResult result = fwdAn.analyse(firstStep);
        assertFalse(result.isIntegerForwarded());
        assertFalse(result.isDoubleForwarded());
        assertFalse(result.isIntegerForwarded());
        assertFalse(result.isNullForwarded());
        assertFalse(result.isMissingForwarded());
    }

    @Test
    public void testCompareMoreElementsVectorFilter2() {
        PipelineStep<?, ?> firstStep = new FilterStep<>(new CompareFilter<>(CompareFilter.LE, new CompareFilter.VectorSize(2)), null, false);

        ForwardedValuesAnalyser fwdAn = new ForwardedValuesAnalyser();
        ForwardingAnalysisResult result = fwdAn.analyse(firstStep);
        assertTrue(result.isIntegerForwarded());
        assertTrue(result.isDoubleForwarded());
        assertTrue(result.isIntegerForwarded());
        assertTrue(result.isNullForwarded());
        assertFalse(result.isMissingForwarded());
    }

    @Test
    public void testCompareElementAt0Filter() {
        PipelineStep<?, ?> firstStep = new FilterStep<>(new CompareFilter<>(CompareFilter.EQ, new CompareFilter.ElementAt(0, 1, RType.Integer)), null, false);

        ForwardedValuesAnalyser fwdAn = new ForwardedValuesAnalyser();
        ForwardingAnalysisResult result = fwdAn.analyse(firstStep);
        assertTrue(result.integerForwarded.isUnknown());
        assertFalse(result.isDoubleForwarded());
        assertFalse(result.isIntegerForwarded());
        assertFalse(result.isNullForwarded());
        assertFalse(result.isMissingForwarded());
    }

    @Test
    public void testCompareElementAt1Filter() {
        PipelineStep<?, ?> firstStep = new FilterStep<>(new CompareFilter<>(CompareFilter.EQ, new CompareFilter.ElementAt(1, 1, RType.Integer)), null, false);

        ForwardedValuesAnalyser fwdAn = new ForwardedValuesAnalyser();
        ForwardingAnalysisResult result = fwdAn.analyse(firstStep);
        assertFalse(result.isIntegerForwarded());
        assertFalse(result.isDoubleForwarded());
        assertFalse(result.isIntegerForwarded());
        assertFalse(result.isNullForwarded());
        assertFalse(result.isMissingForwarded());
    }

    @Test
    public void testCompareDimFilter() {
        PipelineStep<?, ?> firstStep = new FilterStep<>(new CompareFilter<>(CompareFilter.EQ, new CompareFilter.Dim(0, 1)), null, false);

        ForwardedValuesAnalyser fwdAn = new ForwardedValuesAnalyser();
        ForwardingAnalysisResult result = fwdAn.analyse(firstStep);
        assertFalse(result.isIntegerForwarded());
        assertFalse(result.isDoubleForwarded());
        assertFalse(result.isIntegerForwarded());
        assertFalse(result.isNullForwarded());
        assertFalse(result.isMissingForwarded());
    }

    @Test
    public void testDoubleFilter() {
        PipelineStep<?, ?> firstStep = new FilterStep<>(DoubleFilter.IS_FINITE, null, false);

        ForwardedValuesAnalyser fwdAn = new ForwardedValuesAnalyser();
        ForwardingAnalysisResult result = fwdAn.analyse(firstStep);
        assertTrue(result.doubleForwarded.isUnknown());
        assertFalse(result.isIntegerForwarded());
        assertFalse(result.isStringForwarded());
        assertFalse(result.isNullForwarded());
        assertFalse(result.isMissingForwarded());
    }

    @Test
    public void testNotNullFilter() {
        PipelineStep<?, ?> firstStep = new FilterStep<>(new NotFilter<>(NullFilter.INSTANCE), null, false);

        ForwardedValuesAnalyser fwdAn = new ForwardedValuesAnalyser();
        ForwardingAnalysisResult result = fwdAn.analyse(firstStep);
        assertTrue(result.isIntegerForwarded());
        assertTrue(result.isDoubleForwarded());
        assertTrue(result.isStringForwarded());
        assertTrue(result.isMissingForwarded());
        assertFalse(result.isNullForwarded());
    }

    @Test
    public void testNotMissingFilter() {
        PipelineStep<?, ?> firstStep = new FilterStep<>(new NotFilter<>(MissingFilter.INSTANCE), null, false);

        ForwardedValuesAnalyser fwdAn = new ForwardedValuesAnalyser();
        ForwardingAnalysisResult result = fwdAn.analyse(firstStep);
        assertTrue(result.isIntegerForwarded());
        assertTrue(result.isDoubleForwarded());
        assertTrue(result.isStringForwarded());
        assertTrue(result.isNullForwarded());
        assertFalse(result.isMissingForwarded());
    }

    @Test
    public void testNoBranchReturnIf() {
        PipelineStep<?, ?> firstStep = new MapIfStep<>(NullFilter.INSTANCE, null, null, true).setNext(new FilterStep<>(new RTypeFilter<>(RType.Integer), null, false));

        ForwardedValuesAnalyser fwdAn = new ForwardedValuesAnalyser();
        ForwardingAnalysisResult result = fwdAn.analyse(firstStep);
        assertTrue(result.isIntegerForwarded());
        assertTrue(result.isNullForwarded());
        assertFalse(result.isDoubleForwarded());
        assertFalse(result.isStringForwarded());
        assertFalse(result.isMissingForwarded());
    }

    @Test
    public void testReturnIfWithTrueBranch() {
        //@formatter:off
        PipelineStep<?, ?> firstStep = new MapIfStep<>(new RTypeFilter<>(RType.Integer),
                        new FilterStep<>(new CompareFilter<>(CompareFilter.EQ, new ScalarValue(1, RType.Integer)), null, false), null, true).
                        setNext(new FilterStep<>(new RTypeFilter<>(RType.Double), null, false));
        //@formatter:on

        ForwardedValuesAnalyser fwdAn = new ForwardedValuesAnalyser();
        ForwardingAnalysisResult result = fwdAn.analyse(firstStep);
        assertTrue(result.isDoubleForwarded());
        assertTrue(result.integerForwarded.isUnknown());
        assertFalse(result.isNullForwarded());
        assertFalse(result.isStringForwarded());
        assertFalse(result.isMissingForwarded());
    }

    @Test
    public void testReturnIfWithBothBranches() {
        //@formatter:off
        PipelineStep<?, ?> firstStep = new MapIfStep<>(new RTypeFilter<>(RType.Integer), // the condition
                        // true branch
                        new FilterStep<>(new CompareFilter<>(CompareFilter.EQ, new ScalarValue(1, RType.Integer)), null, false),
                        // false branch
                        new FilterStep<>(new RTypeFilter<>(RType.Double), null, false), true);
        //@formatter:on

        ForwardedValuesAnalyser fwdAn = new ForwardedValuesAnalyser();
        ForwardingAnalysisResult result = fwdAn.analyse(firstStep);
        assertTrue(result.isDoubleForwarded());
        assertTrue(result.integerForwarded.isUnknown());
        assertFalse(result.isNullForwarded());
        assertFalse(result.isStringForwarded());
        assertFalse(result.isMissingForwarded());
    }

    @Test
    public void testReturnIfWithTrueBranchChain() {
        //@formatter:off
        PipelineStep<?, ?> findFirstBoolean = new CoercionStep<>(RType.Logical, false).setNext(new FindFirstStep<>(null, Byte.class, null)).setNext(new MapStep<>(new MapByteToBoolean(false)));
        PipelineStep<?, ?> firstStep = new MapIfStep<>(new RTypeFilter<>(RType.Logical), // the condition
                        findFirstBoolean, null, true);
        //@formatter:on

        ForwardedValuesAnalyser fwdAn = new ForwardedValuesAnalyser();
        ForwardingAnalysisResult result = fwdAn.analyse(firstStep);
        // TODO: change it to the positive assertion when the selected mappers (such as
        // MapByteToBoolean) are supported
        assertFalse(result.isLogicalForwarded());
        assertTrue(result.logicalForwarded.mapper instanceof MapByteToBoolean);
        assertTrue(result.isDoubleForwarded());
        assertTrue(result.isIntegerForwarded());
        assertTrue(result.isNullForwarded());
        assertTrue(result.isStringForwarded());
        assertTrue(result.isMissingForwarded());
    }

}
