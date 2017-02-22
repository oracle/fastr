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
package com.oracle.truffle.r.nodes.builtin;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.atomicIntegerValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.atomicLogicalValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.chain;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.charAt0;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.constant;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.dimEq;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.doubleToInt;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.elementAt;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.eq;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.instanceOf;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.intNA;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.integerValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.isFractional;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.length;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.map;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.mapIf;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.matrix;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.missingValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.nullValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.size;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.hamcrest.CustomMatcher;
import org.hamcrest.Matcher;
import org.hamcrest.core.IsNot;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.oracle.truffle.r.nodes.builtin.CastBuilderTest.DummyBuiltin;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep;
import com.oracle.truffle.r.nodes.builtin.casts.fluent.PreinitialPhaseBuilder;
import com.oracle.truffle.r.nodes.casts.SamplesCollector;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RString;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

public class SampleCollectorTest {

    private CastBuilder cb;
    private PreinitialPhaseBuilder arg;

    @Before
    public void setUp() {
        cb = new CastBuilder(DummyBuiltin.class.getAnnotation(RBuiltin.class));
        arg = cb.arg("x");
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testAsDoubleVector() {
        arg.asDoubleVector();
        assertSamples(0.0, Double.NaN, RRuntime.DOUBLE_NA, vector(RType.Double, 0.0), vector(RType.Double, RRuntime.DOUBLE_NA), RNull.instance, RMissing.instance);
    }

    @Test
    public void testAsIntegerVector() {
        arg.asIntegerVector();
        assertSamples(0, RRuntime.INT_NA, vector(RType.Integer, 0), vector(RType.Integer, RRuntime.INT_NA), RNull.instance, RMissing.instance);
    }

    @Test
    public void testAsLogicalVector() {
        arg.asLogicalVector();
        assertAsLogicalVectorSamples();
    }

    private void assertAsLogicalVectorSamples() {
        assertSamples(RRuntime.LOGICAL_FALSE, RRuntime.LOGICAL_NA, RRuntime.LOGICAL_TRUE, vector(RType.Logical, RRuntime.LOGICAL_FALSE, RRuntime.LOGICAL_NA, RRuntime.LOGICAL_TRUE), RNull.instance,
                        RMissing.instance);
    }

    @Test
    public void testAsStringVector() {
        arg.asStringVector();
        assertAsStringVectorSamples();
    }

    private void assertAsStringVectorSamples() {
        assertSamples("", RRuntime.STRING_NA, vector(RType.Character, ""), vector(RType.Character), vector(RType.Character, RRuntime.STRING_NA), RNull.instance,
                        RMissing.instance);
    }

    @Test
    public void testAsRawVector() {
        arg.asRawVector();
        assertSamples((byte) 0, vector(RType.Raw, RDataFactory.createRaw((byte) 0)), RNull.instance, RMissing.instance);
    }

    @Test
    public void testAsComplexVector() {
        arg.asComplexVector();
        RComplex na = RComplex.createNA();
        RComplex z = RDataFactory.createComplex(0, 0);
        assertSamples(z, na, vector(RType.Complex, z), vector(RType.Complex, na), RNull.instance, RMissing.instance);
    }

    @Test
    public void testAsVectorPreserveNonVector() {
        arg.asVector(true);
        assertSamples();
    }

    @Test
    public void testAsVectorVectorNoPreserveNonVector() {
        arg.asVector(false);
        assertSamples();
    }

    @Test
    public void testAsAttributableVector() {
        arg.asAttributable(false, false, false);
        assertSamples();
    }

    @Test
    public void testBoxPrimitive() {
        arg.boxPrimitive();
        assertSamples();
    }

    @Test
    public void testFindFirst() {
        arg.asStringVector().findFirst();
        assertAsStringVectorSamples();
    }

    @Test
    public void testFindFirstOrNull() {
        arg.asStringVector().findFirstOrNull();
        assertAsStringVectorSamples();
    }

    @Test
    public void testFindFirstAfterGenericVector() {
        arg.asVector().findFirst();
        assertSamples();
    }

    @Test
    public void testMapToValue() {
        arg.map(constant(1));
        assertSamples(RNull.instance, RMissing.instance);
    }

    @Test
    public void testMapByteToBoolean() {
        arg.mustBe(atomicLogicalValue()).map(toBoolean());
        assertSamples(RRuntime.LOGICAL_FALSE, RRuntime.LOGICAL_NA, RRuntime.LOGICAL_TRUE, RNull.instance, RMissing.instance);
    }

    @Test
    public void testMapDoubleToInt() {
        arg.mustBe(instanceOf(Double.class)).map(doubleToInt());
        assertSamples(0.0, RRuntime.DOUBLE_NA, RNull.instance, RMissing.instance);
    }

    @Test
    public void testMapToCharAt() {
        arg.mustBe(instanceOf(String.class)).map(charAt0('A'));
        assertSamples("A", RRuntime.STRING_NA, RNull.instance, RMissing.instance);
    }

    @Test
    public void testTypeFilter() {
        arg.mustBe(instanceOf(String.class));
        assertSamples("", RRuntime.STRING_NA, RNull.instance, RMissing.instance);
    }

    @Test
    public void testTypeFilterInSeries() {
        arg.mustBe(instanceOf(RAbstractVector.class)).mustBe(instanceOf(RString.class));
        assertSamples(vector(RType.Character, ""), vector(RType.Character, RRuntime.STRING_NA), RNull.instance, RMissing.instance);
    }

    @Test
    public void testRTypeFilter() {
        arg.mustBe(integerValue());
        assertSamples(0, RRuntime.INT_NA, vector(RType.Integer, 0), vector(RType.Integer, RRuntime.INT_NA), RNull.instance, RMissing.instance);
    }

    @Test
    public void testCompareScalarValueFilter() {
        arg.mustBe(atomicIntegerValue()).mustBe(eq(100));
        assertSamples(100, RNull.instance, RMissing.instance);
    }

    @Test
    public void testCompareNAValueFilter() {
        arg.mustBe(atomicIntegerValue()).mustBe(intNA());
        assertSamples(RRuntime.INT_NA, RNull.instance, RMissing.instance);
    }

    @Test
    public void testCompareStringLengthFilter() {
        arg.mustBe(instanceOf(String.class)).mustBe(length(5));
        assertSamples(stringOfLength(5), RNull.instance, RMissing.instance);
    }

    @Test
    public void testCompareVectorSizeFilter() {
        arg.mustBe(instanceOf(RStringVector.class)).mustBe(size(10));
        assertSamples(vectorOfSize(RType.Character, 10), RNull.instance, RMissing.instance);
    }

    @Test
    public void testCompareElementAtFilter() {
        arg.mustBe(instanceOf(RStringVector.class)).mustBe(elementAt(3, "abc"));
        assertSamples(vectorOfSize(RType.Character, 4), vector(RType.Character, "abc"), RNull.instance, RMissing.instance);
    }

    @Test
    public void testCompareDimFilter() {
        arg.mustBe(instanceOf(RStringVector.class)).mustBe(dimEq(1, 2));
        assertSamples(RNull.instance, RMissing.instance);
    }

    @Test
    public void testAndFilter1() {
        arg.mustBe(instanceOf(RStringVector.class).and(elementAt(3, "xyz")));
        assertSamples(vectorOfSize(RType.Character, 4), vector(RType.Character, "xyz"), RNull.instance, RMissing.instance);
    }

    @Test
    public void testAndFilter2() {
        arg.mustBe(instanceOf(RStringVector.class).and(elementAt(3, "xyz")).and(size(10)));
        assertSamples(vectorOfSize(RType.Character, 10), vector(RType.Character, "xyz"), RNull.instance, RMissing.instance);
    }

    @Test
    public void testAndAsNegationOfOrFilter() {
        // !(!A || !B) = A && B
        arg.mustBe(instanceOf(RAbstractStringVector.class).not().or(instanceOf(RAbstractIntVector.class).not()).not());
        assertSamples(vector(RType.Character, ""), vector(RType.Character, nonEmptyString()), vector(RType.Character, RRuntime.STRING_NA), vector(RType.Integer, 0),
                        vector(RType.Integer, RRuntime.INT_NA), RNull.instance, RMissing.instance);
    }

    @Test
    public void testAndAsNegationOfOrFilter2() {
        // !(!A || !B) = A && B
        arg.mustBe(instanceOf(RStringVector.class).not().or(instanceOf(RIntVector.class).not()).not());
        assertSamples(vector(RType.Character, ""), vector(RType.Character, nonEmptyString()), vector(RType.Character, RRuntime.STRING_NA), vector(RType.Integer, 0),
                        vector(RType.Integer, RRuntime.INT_NA), RNull.instance, RMissing.instance);
    }

    @Test
    public void testOrFilter1() {
        arg.mustBe(instanceOf(RStringVector.class).or(nullValue()));
        assertSamples(vector(RType.Character, ""), vector(RType.Character, nonEmptyString()), vector(RType.Character, RRuntime.STRING_NA), RNull.instance, RMissing.instance);
    }

    @Test
    public void testOrFilter2() {
        arg.mustBe(instanceOf(RStringVector.class).or(instanceOf(RIntVector.class).not()));
        assertSamples(vector(RType.Character, ""), vector(RType.Character, nonEmptyString()), vector(RType.Character, RRuntime.STRING_NA), vector(RType.Integer, 0),
                        vector(RType.Integer, RRuntime.INT_NA), RNull.instance, RMissing.instance);
    }

    @Test
    public void testNotOrFilter() {
        arg.mustBe(instanceOf(RStringVector.class).or(instanceOf(RIntVector.class)).not());
        assertSamples(vector(RType.Character, ""), vector(RType.Character, nonEmptyString()), vector(RType.Character, RRuntime.STRING_NA), vector(RType.Integer, 0),
                        vector(RType.Integer, RRuntime.INT_NA), RNull.instance, RMissing.instance);
    }

    @Test
    public void testOrAsNegationOfAndFilter() {
        // !(!A && !B) = A || B
        arg.mustBe(instanceOf(RAbstractStringVector.class).not().and(instanceOf(RAbstractIntVector.class).not()).not());
        assertSamples(vector(RType.Character, ""), vector(RType.Character, nonEmptyString()), vector(RType.Character, RRuntime.STRING_NA), vector(RType.Integer, 0),
                        vector(RType.Integer, RRuntime.INT_NA), RNull.instance, RMissing.instance);
    }

    @Test
    public void testNotFilter1() {
        arg.mustBe(instanceOf(RStringVector.class).not());
        assertSamples(vector(RType.Character, ""), vector(RType.Character, nonEmptyString()), vector(RType.Character, RRuntime.STRING_NA), RNull.instance, RMissing.instance);
    }

    @Test
    public void testNotFilter2() {
        // !(x instanceof RStringVector && "abc".equals(x[1]))
        arg.mustBe(instanceOf(RStringVector.class).and(elementAt(3, "xyz")).not());
        assertSamples(vectorOfSize(RType.Character, 4), vector(RType.Character, "xyz"), RNull.instance, RMissing.instance);
    }

    @Test
    public void testNotFilter3() {
        // !(x instanceof RStringVector) && (x instanceof RAbstractStringVector)
        arg.mustBe(instanceOf(RStringVector.class).not()).mustBe(instanceOf(RAbstractStringVector.class));
        assertSamples(vector(RType.Character, ""), vector(RType.Character, nonEmptyString()), vector(RType.Character, RRuntime.STRING_NA), RNull.instance, RMissing.instance);
    }

    @Test
    public void testNotFilter4() {
        arg.mustBe(instanceOf(RAbstractStringVector.class)).mustBe(elementAt(3, "xyz").not());
        assertSamples(vectorOfSize(RType.Character, 4), vector(RType.Character, "xyz"), RNull.instance, RMissing.instance);
    }

    @Test
    public void testMatrixFilter() {
        arg.mustBe(instanceOf(RStringVector.class).and(matrix()));
        assertSamples(vector(RType.Character, ""), vector(RType.Character, nonEmptyString()), vector(RType.Character, RRuntime.STRING_NA), RNull.instance, RMissing.instance);
    }

    @Test
    public void testDoubleFilter() {
        arg.mustBe(instanceOf(Double.class).and(isFractional()));
        assertSamples(0.0, Double.NaN, RRuntime.DOUBLE_NA, RNull.instance, RMissing.instance);
    }

    @Test
    public void testNullFilter() {
        arg.mustBe(nullValue().or(stringValue()));
        assertSamples("", RRuntime.STRING_NA, vector(RType.Character, ""), vector(RType.Character, RRuntime.STRING_NA), RNull.instance, RMissing.instance);
    }

    @Test
    public void testMissingFilter() {
        arg.mustBe(missingValue().or(stringValue()));
        assertAsStringVectorSamples();
    }

    @Test
    public void testNonNA() {
        arg.mustBe(atomicIntegerValue()).notNA(RError.Message.GENERIC, "abc");
        assertSamples(RRuntime.STRING_NA);
    }

    @Test
    public void testMapIf() {
        arg.mapIf(nullValue(), constant(1));
        assertSamples(RNull.instance, RMissing.instance);
    }

    @Test
    public void testMapIf2() {
        arg.mapIf(nullValue(), constant(1), constant("abc"));
        assertSamples(RNull.instance, RMissing.instance);
    }

    @Test
    public void testMapIf3() {
        PipelineStep<?, ?> m1 = chain(map(constant(1))).end();
        PipelineStep<?, ?> m2 = chain(map(constant("xyz"))).end();
        arg.mapIf(stringValue(), m1, m2);
        assertAsStringVectorSamples();
    }

    @Test
    public void testMapIf4() {
        PipelineStep<?, ?> m1 = chain(map(constant(1))).end();
        PipelineStep<?, ?> m2 = chain(mapIf(instanceOf(Double.class), chain(map(constant("abc"))).end())).end();
        arg.mapIf(nullValue(), m1, m2);
        assertSamples(0.0, Double.NaN, RRuntime.DOUBLE_NA, RNull.instance, RMissing.instance);
    }

    @Test
    public void testMapIf5() {
        PipelineStep<?, ?> m1 = chain(map(constant(1))).end();
        PipelineStep<?, ?> m2 = chain(mapIf(instanceOf(Double.class), chain(map(constant("abc"))).end())).end();
        arg.mapIf(nullValue(), m1, m2).mapIf(instanceOf(String.class).and(length(10)), constant((byte) 0));
        assertSamples("", RRuntime.STRING_NA, 0.0, Double.NaN, RRuntime.DOUBLE_NA, RNull.instance, RMissing.instance);
    }

    @Test
    public void testReturnIf() {
        arg.mapIf(nullValue(), constant(1), constant("abc"));
        assertSamples(RNull.instance);
    }

    @Test
    public void testTwoWildcardTypes() {
        arg.mustBe((instanceOf(String.class).and(length(10).or(length(20)))));
        assertSamples(stringOfLength(10), stringOfLength(20));
    }

    // utilities

    @SuppressWarnings("unchecked")
    void assertSamples(Object... expected) {
        Set<Object> samples = collectSamples();
        Assert.assertFalse(samples.isEmpty());
        for (Object es : expected) {
            if (es instanceof Matcher) {
                Assert.assertThat(samples, (Matcher<Set<Object>>) es);
            } else {
                Assert.assertTrue("Samples must contain " + es, samples.contains(es));
            }
        }
    }

    Set<Object> collectSamples() {
        return SamplesCollector.collect(cb.getPipelineBuilders()[0].getFirstStep());
    }

    static VectorMatcher vector(RType elemType, Object... expectedElements) {
        return new VectorMatcher(elemType, expectedElements);
    }

    static Matcher<?> nonEmptyString() {
        return new IsNot<>(new StringMatcher(0));
    }

    static Matcher<?> stringOfLength(int expectedLenght) {
        return new StringMatcher(expectedLenght);
    }

    static Matcher<?> vectorOfSize(RType type, int expectedSize) {
        return new VectorSizeMatcher(type, expectedSize);
    }

    static final class StringMatcher extends CustomMatcher<Set<Object>> {

        final int expectedLength;

        private StringMatcher(int expectedLength) {
            super("String of length " + expectedLength + " expected");
            this.expectedLength = expectedLength;
        }

        @Override
        public boolean matches(Object item) {
            @SuppressWarnings("unchecked")
            Set<Object> samples = (Set<Object>) item;
            return samples.stream().filter(s -> s instanceof String && s.toString().length() == expectedLength).findAny().isPresent();
        }

    }

    static final class VectorSizeMatcher extends CustomMatcher<Set<Object>> {

        final int expectedSize;
        final RType type;

        private VectorSizeMatcher(RType type, int expectedSize) {
            super("Vector of size " + expectedSize + " expected");
            this.type = type;
            this.expectedSize = expectedSize;
        }

        @Override
        public boolean matches(Object item) {
            @SuppressWarnings("unchecked")
            Set<Object> samples = (Set<Object>) item;
            return samples.stream().filter(s -> s instanceof RAbstractVector && ((RAbstractVector) s).getRType() == type && ((RAbstractVector) s).getLength() == expectedSize).findAny().isPresent();
        }

    }

    static final class VectorMatcher extends CustomMatcher<Set<Object>> {
        final RType elemType;
        final Object[] expectedElements;

        VectorMatcher(RType elemType, Object... expectedElements) {
            super("Expected " + elemType + " vector: " + Arrays.asList(expectedElements));
            this.elemType = elemType;
            this.expectedElements = expectedElements;
        }

        @Override
        public boolean matches(Object item) {
            @SuppressWarnings("unchecked")
            Set<Object> samples = (Set<Object>) item;
            return samples.stream().filter(s -> s instanceof RAbstractVector && elemType.equals(((RAbstractVector) s).getRType()) && contains((RAbstractVector) s)).findAny().isPresent();
        }

        boolean contains(RAbstractVector v) {
            return Arrays.stream(expectedElements).allMatch(expected -> contains(v, expected));
        }

        static boolean contains(RAbstractVector v, Object expected) {
            if (expected instanceof Matcher) {
                Set<Object> vecAsSet = new HashSet<>();
                for (int i = 0; i < v.getLength(); i++) {
                    vecAsSet.add(v.getDataAtAsObject(i));
                }
                return ((Matcher<?>) expected).matches(vecAsSet);
            } else {
                for (int i = 0; i < v.getLength(); i++) {
                    if (expected.equals(v.getDataAtAsObject(i))) {
                        return true;
                    }
                }
            }
            return false;
        }

    }
}
