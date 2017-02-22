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
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.not;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.nullValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.numericValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.size;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.squareMatrix;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;
import static com.oracle.truffle.r.nodes.casts.TypeExpr.atom;
import static com.oracle.truffle.r.nodes.casts.MarkLookup.mark;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.oracle.truffle.r.nodes.builtin.CastBuilderTest.DummyBuiltin;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep;
import com.oracle.truffle.r.nodes.builtin.casts.fluent.PreinitialPhaseBuilder;
import com.oracle.truffle.r.nodes.casts.MarkLookup;
import com.oracle.truffle.r.nodes.casts.ResultTypesAnalyser;
import com.oracle.truffle.r.nodes.casts.TypeExpr;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RComplexVector;
import com.oracle.truffle.r.runtime.data.RDouble;
import com.oracle.truffle.r.runtime.data.RDoubleSequence;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RIntSequence;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RInteger;
import com.oracle.truffle.r.runtime.data.RLogical;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RRaw;
import com.oracle.truffle.r.runtime.data.RRawVector;
import com.oracle.truffle.r.runtime.data.RString;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

public class ResultTypesAnalyserTest {

    private CastBuilder cb;
    private PreinitialPhaseBuilder arg;

    @Before
    public void setUp() {
        MarkLookup.clear();
        cb = new CastBuilder(DummyBuiltin.class.getAnnotation(RBuiltin.class));
        arg = cb.arg("x");
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testAsDoubleVector() {
        arg.asDoubleVector();
        assertTypes(RNull.class, RMissing.class, double.class, RDoubleSequence.class, RDoubleVector.class);
    }

    @Test
    public void testAsIntegerVector() {
        arg.asIntegerVector();
        assertTypes(RNull.class, RMissing.class, int.class, RIntSequence.class, RIntVector.class);
    }

    @Test
    public void testAsLogicalVector() {
        arg.asLogicalVector();
        assertTypes(RNull.class, RMissing.class, byte.class, RLogicalVector.class,
                        RArgsValuesAndNames.class);
    }

    @Test
    public void testAsStringVector() {
        arg.asStringVector();
        assertTypes(RNull.class, RMissing.class, String.class, RStringVector.class);
    }

    @Test
    public void testAsRawVector() {
        arg.asRawVector();
        assertTypes(RNull.class, RMissing.class, RRaw.class, RRawVector.class);
    }

    @Test
    public void testAsComplexVector() {
        arg.asComplexVector();
        assertTypes(RNull.class, RMissing.class, RComplex.class, RComplexVector.class);
    }

    @Test
    public void testAsVectorPreserveNonVector() {
        arg.asVector(true);
        assertTypes(RFunction.class, RNull.class, RMissing.class, RAbstractVector.class);
    }

    @Test
    public void testAsVectorVectorNoPreserveNonVector() {
        arg.asVector(false);
        assertTypes(RAbstractVector.class, RFunction.class);
    }

    @Test
    public void testAsAttributableVector() {
        arg.asAttributable(false, false, false);
        assertTypes(RNull.class, RMissing.class, RAttributable.class);
    }

    @Test
    public void testBoxPrimitive() {
        arg.boxPrimitive();
        TypeExpr expected = TypeExpr.union(RNull.class, RMissing.class, RInteger.class, RLogical.class,
                        RDouble.class, RString.class);
        expected = expected.or(expected.not());
        assertTypes(expected);
    }

    @Test
    public void testFindFirst() {
        arg.asStringVector().findFirst();
        assertTypes(String.class);
    }

    @Test
    public void testFindFirstOrNull() {
        arg.asStringVector().findFirstOrNull();
        assertTypes(RNull.class, String.class);
    }

    @Test
    public void testFindFirstAfterGenericVector() {
        arg.asVector().findFirst();
        assertTypes(atom(RFunction.class).or(atom(RNull.class).or(atom(RMissing.class)).not()));
    }

    @Test
    public void testMapToValue() {
        arg.map(mark(constant(1), "m"));
        assertTypes(atom(Integer.class).lower(m("m")));
    }

    @Test
    public void testMapByteToBoolean() {
        arg.mustBe(atomicLogicalValue()).map(toBoolean());
        assertTypes(Boolean.class);
    }

    @Test
    public void testMapDoubleToInt() {
        arg.mustBe(instanceOf(Double.class)).map(doubleToInt());
        assertTypes(Integer.class);
    }

    @Test
    public void testMapToCharAt() {
        arg.mustBe(instanceOf(String.class)).map(charAt0('A'));
        assertTypes(Integer.class);
    }

    @Test
    public void testTypeFilter() {
        arg.mustBe(instanceOf(RString.class));
        assertTypes(RString.class);
    }

    @Test
    public void testTypeFilterInSeries() {
        arg.mustBe(instanceOf(RAbstractVector.class)).mustBe(instanceOf(RString.class));
        assertTypes(RString.class);
    }

    @Test
    public void testRTypeFilter() {
        arg.mustBe(integerValue());
        assertTypes(Integer.class, RAbstractIntVector.class);
    }

    @Test
    public void testCompareScalarValueFilter() {
        arg.mustBe(atomicIntegerValue()).mustBe(mark(eq(1), "x"));
        assertWildTypes("x", Integer.class);
    }

    @Test
    public void testCompareNAValueFilter() {
        arg.mustBe(atomicIntegerValue()).mustBe(mark(intNA(), "x"));
        assertWildTypes("x", Integer.class);
    }

    @Test
    public void testCompareStringLengthFilter() {
        arg.mustBe(instanceOf(String.class)).mustBe(mark(length(1), "x"));
        assertWildTypes("x", String.class);
    }

    @Test
    public void testCompareVectorSizeFilter() {
        arg.mustBe(instanceOf(RStringVector.class)).mustBe(mark(size(1), "x"));
        assertWildTypes("x", RStringVector.class);
    }

    @Test
    public void testCompareElementAtFilter() {
        arg.mustBe(instanceOf(RStringVector.class)).mustBe(mark(elementAt(1, "abc"), "x"));
        assertWildTypes("x", RStringVector.class);
    }

    @Test
    public void testCompareDimFilter() {
        arg.mustBe(instanceOf(RStringVector.class)).mustBe(mark(dimEq(1, 2), "x"));
        assertWildTypes("x", RStringVector.class);
    }

    @Test
    public void testAndFilter1() {
        arg.mustBe(instanceOf(RStringVector.class).and(mark(elementAt(1, "abc"), "x")));
        assertWildTypes("x", RStringVector.class);
    }

    @Test
    public void testAndFilter2() {
        arg.mustBe(instanceOf(RStringVector.class).and(mark(elementAt(1, "abc"), "x1")).and(mark(size(10), "x2")));
        assertTypes(atom(RStringVector.class).lower(m("x1")).and(atom(RStringVector.class).lower(m("x2"))));
    }

    @Test
    public void testAndAsNegationOfOrFilter() {
        // !(!A || !B) = A && B
        arg.mustBe(instanceOf(RAbstractStringVector.class).not().or(instanceOf(RAbstractIntVector.class).not()).not());
        assertTypes(atom(RAbstractStringVector.class).and(atom(RAbstractIntVector.class)));
    }

    @Test
    public void testAndAsNegationOfOrFilter2() {
        // !(!A || !B) = A && B
        arg.mustBe(instanceOf(RStringVector.class).not().or(instanceOf(RIntVector.class).not()).not());
        // A and B are mutually exclusive, thus their conjunction is empty
        assertTypes(TypeExpr.NOTHING);
    }

    @Test
    public void testOrFilter1() {
        arg.mustBe(instanceOf(RStringVector.class).or(nullValue()));
        assertTypes(RStringVector.class, RNull.class);
    }

    @Test
    public void testOrFilter2() {
        arg.mustBe(instanceOf(RStringVector.class).or(instanceOf(RIntVector.class).not()));
        assertTypes(atom(RStringVector.class).or(atom(RIntVector.class).not()));
    }

    @Test
    public void testNotOrFilter() {
        arg.mustBe(instanceOf(String.class).or(instanceOf(Integer.class)).not());
        assertTypes(atom(String.class).not().and(atom(Integer.class).not()));
    }

    @Test
    public void testOrAsNegationOfAndFilter() {
        // !(!A && !B) = A || B
        arg.mustBe(instanceOf(String.class).not().and(instanceOf(Integer.class).not()).not());
        assertTypes(atom(String.class).or(atom(Integer.class)));
    }

    @Test
    public void testNotFilter1() {
        arg.mustBe(instanceOf(RStringVector.class).not());
        assertTypes(atom(RStringVector.class).not());
    }

    @Test
    public void testNotFilter2() {
        // !(x instanceof RStringVector && "abc".equals(x[1]))
        arg.mustBe(instanceOf(RStringVector.class).and(mark(elementAt(1, "abc"), "x")).not());
        assertTypes(atom(RStringVector.class).lower(m("x")).not());
    }

    @Test
    public void testNotFilter3() {
        // !(x instanceof RStringVector) && (x instanceof RAbstractStringVector)
        arg.mustBe(instanceOf(RStringVector.class).not()).mustBe(instanceOf(RAbstractStringVector.class));
        assertTypes(atom(RAbstractStringVector.class).and(atom(RStringVector.class).not()));
    }

    @Test
    public void testNotFilter4() {
        arg.mustBe(instanceOf(RAbstractStringVector.class)).mustBe(mark(elementAt(1, "abc"), "x").not());
        assertTypes(atom(RAbstractStringVector.class).and(atom(RAbstractStringVector.class).lower(m("x")).not()));
    }

    @Test
    public void testMatrixFilter() {
        arg.mustBe(instanceOf(RStringVector.class).and(mark(matrix(), "m")));
        assertTypes(atom(RStringVector.class).lower(m("m")));
    }

    @Test
    public void testDoubleFilter() {
        arg.mustBe(instanceOf(Double.class).and(mark(isFractional(), "x")));
        assertTypes(atom(Double.class).lower(m("x")));
    }

    @Test
    public void testNullFilter() {
        arg.mustBe(nullValue().or(stringValue()));
        assertTypes(TypeExpr.union(RNull.class, RAbstractStringVector.class, String.class));
    }

    @Test
    public void testMissingFilter() {
        arg.mustBe(missingValue().or(stringValue()));
        assertTypes(TypeExpr.union(RMissing.class, RAbstractStringVector.class, String.class));
    }

    @Test
    public void testNonNA() {
        arg.mustBe(atomicIntegerValue()).notNA(RError.Message.GENERIC, "abc");
        // the type representation of the wildcard result type is the nonNA step
        PipelineStep<?, ?> notNAStep = cb.getPipelineBuilders()[0].getFirstStep().getNext();
        assertTypes(atom(Integer.class).lower(notNAStep));
    }

    @Test
    public void testCoerceToAttributable() {
        arg.asAttributable(false, false, false);
        assertTypes(TypeExpr.union(RAttributable.class, RNull.class, RMissing.class));
    }

    @Test
    public void testMapIf() {
        arg.mapIf(nullValue(), mark(constant(1), "m"));
        assertTypes(atom(Integer.class).lower(m("m")).or(atom(RNull.class).not()));
    }

    @Test
    public void testMapIf2() {
        arg.mapIf(nullValue(), mark(constant(1), "m1"), mark(constant("abc"), "m2"));
        assertTypes(atom(String.class).lower(m("m2")).or(atom(Integer.class).lower(m("m1"))));
    }

    @Test
    public void testMapIf3() {
        PipelineStep<?, ?> m1 = chain(map(mark(constant(1), "m1"))).end();
        PipelineStep<?, ?> m2 = chain(map(mark(constant("abc"), "m2"))).end();
        arg.mapIf(nullValue(), m1, m2);
        assertTypes(atom(String.class).lower(m("m2")).or(atom(Integer.class).lower(m("m1"))));
    }

    @Test
    public void testMapIf4() {
        PipelineStep<?, ?> m1 = chain(map(mark(constant(1), "m1"))).end();
        PipelineStep<?, ?> m2 = chain(mapIf(instanceOf(Double.class), chain(map(mark(constant("abc"), "m2"))).end())).end();
        arg.mapIf(nullValue(), m1, m2);
        assertTypes(atom(String.class).lower(m("m2")).or(atom(Integer.class).lower(m("m1")).or(atom(RNull.class).not().and(atom(Double.class).not()))));
    }

    @Test
    public void testMapIf5() {
        PipelineStep<?, ?> m1 = chain(map(mark(constant(1), "m1"))).end();
        PipelineStep<?, ?> m2 = chain(mapIf(instanceOf(Double.class), chain(map(mark(constant("abc"), "m2"))).end())).end();
        arg.mapIf(nullValue(), m1, m2).mapIf(instanceOf(String.class).and(mark(length(10), "m4")), mark(constant((byte) 0), "m3"));
        //@formatter:off
        assertTypes(
                        atom(Byte.class).lower(m("m3")).
                            or(atom(Integer.class).lower(m("m1"))).
                            or(atom(String.class).lower(m("m2")).and(atom(String.class).lower(m("m4")).not())).
                            or(atom(Double.class).not().and(atom(RNull.class).not()).and(atom(String.class).lower(m("m4")).not())));
        //@formatter:on
    }

    @Test
    public void testReturnIf() {
        arg.mapIf(nullValue(), mark(constant(1), "m1"), mark(constant("abc"), "m2"));
        assertTypes(atom(String.class).lower(m("m2")).or(atom(Integer.class).lower(m("m1"))));
    }

    @Test
    public void testAllowMissing() {
        arg.allowMissing().mustBe(stringValue());
        assertTypes(RMissing.class, String.class, RAbstractStringVector.class);
    }

    @Test
    public void testTwoWildcardTypes() {
        arg.mustBe((instanceOf(String.class).and(mark(length(10), "l10").or(mark(length(20), "l20")))));
        assertTypes(atom(String.class).lower(m("l10")).or(atom(String.class).lower(m("l20"))));
    }

    private static Function<RAbstractDoubleVector, Object> getDimVal(int dim) {
        return vec -> vec.getDimensions()[dim];
    }

    @Test
    public void testAnalyseRealPipeline() {
        arg.mustBe(numericValue()).asVector().mustBe(matrix(), RError.ROOTNODE, RError.Message.MUST_BE_NUMERIC_MATRIX, "a").mustBe(not(dimEq(0, 0)), RError.ROOTNODE,
                        RError.Message.GENERIC, "'a' is 0-diml").mustBe(squareMatrix(), RError.ROOTNODE, RError.Message.MUST_BE_SQUARE_MATRIX_SPEC, "a", getDimVal(0), getDimVal(1));
        assertTypes(TypeExpr.union(RAbstractDoubleVector.class, RAbstractIntVector.class, RAbstractLogicalVector.class), true);
    }

    // utilities

    private void assertWildTypes(String mark, Class<?>... expectedTypes) {
        TypeExpr expected = TypeExpr.union(expectedTypes).lower(m(mark));
        assertTypes(expected);
    }

    private void assertTypes(Class<?>... expectedTypes) {
        TypeExpr expected = TypeExpr.union(expectedTypes);
        assertTypes(expected);
    }

    private void assertTypes(TypeExpr expectedType) {
        assertTypes(expectedType, false);
    }

    private void assertTypes(TypeExpr expectedType, boolean removeWildcards) {
        PipelineStep<?, ?> firstStep = cb.getPipelineBuilders()[0].getFirstStep();
        TypeExpr actualType = ResultTypesAnalyser.analyse(firstStep);
        Set<Type> actualNorm = removeWildcards ? actualType.normalize().removeWildcards().toNormalizedConjunctionSet() : actualType.toNormalizedConjunctionSet();
        Set<Type> expectedNorm = expectedType.toNormalizedConjunctionSet();
        Assert.assertEquals(expectedNorm, actualNorm);
    }

    /**
     * Look up the filter by its mark.
     *
     * @param mark the mark
     * @return the filter
     */
    public Object m(String mark) {
        Map<String, Object> result = MarkLookup.lookup(cb.getPipelineBuilders()[0].getFirstStep(), mark);
        Assert.assertNotNull(result.get(mark));
        return result.get(mark);
    }

}
