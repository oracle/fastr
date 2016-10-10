/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.asBoolean;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.asInteger;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.asIntegerVector;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.asLogicalVector;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.asStringVector;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.atomicIntegerValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.atomicLogicalValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.chain;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.complexValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.dimGt;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.doubleNA;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.doubleToInt;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.elementAt;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.emptyStringVector;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.findFirst;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.gt;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.gt0;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.gte;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.instanceOf;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.integerValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.isFractional;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.logicalTrue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.logicalValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.lte;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.map;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.mustBe;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.not;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.notEmpty;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.notNA;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.nullConstant;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.numericValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.shouldBe;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.singleElement;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.squareMatrix;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;
import static com.oracle.truffle.r.runtime.RError.SHOW_CALLER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.function.Function;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineConfig;
import com.oracle.truffle.r.nodes.builtin.casts.fluent.InitialPhaseBuilder;
import com.oracle.truffle.r.nodes.builtin.casts.fluent.PreinitialPhaseBuilder;
import com.oracle.truffle.r.nodes.casts.CastNodeSampler;
import com.oracle.truffle.r.nodes.casts.FilterSamplerFactory;
import com.oracle.truffle.r.nodes.casts.MapperSamplerFactory;
import com.oracle.truffle.r.nodes.casts.Samples;
import com.oracle.truffle.r.nodes.test.TestUtilities;
import com.oracle.truffle.r.nodes.test.TestUtilities.NodeHandle;
import com.oracle.truffle.r.nodes.unary.CastNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBehavior;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.builtins.RBuiltinKind;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

/**
 * Tests the cast pipelines and also that the samples generation process matches the intended
 * semantics.
 */
public class CastBuilderTest {

    /**
     * The sampler at the moment does not generate any samples for pipelines that do not have any
     * filter.
     */
    private static final boolean NO_FILTER_EXPECT_EMPTY_SAMPLES = true;

    private static final boolean TEST_SAMPLING = false;

    private CastBuilder cb;
    private PreinitialPhaseBuilder<Object> arg;

    static {
        if (TEST_SAMPLING) {
            PipelineConfig.setFilterFactory(FilterSamplerFactory.INSTANCE);
            PipelineConfig.setMapperFactory(MapperSamplerFactory.INSTANCE);
        }
        CastNode.testingMode();
    }

    @Before
    public void setUp() {
        cb = new CastBuilder(DummyBuiltin.class.getAnnotation(RBuiltin.class));
        arg = cb.arg("x");
    }

    @After
    public void tearDown() {
        cb = null;
    }

    @Test
    public void testError() {
        arg.mustBe(instanceOf(String.class), RError.Message.DLL_LOAD_ERROR, Function.identity(), "123");
        assertCastPreserves("A");
        assertCastFail(Boolean.FALSE, "unable to load shared object 'false'\n  123");
        testPipeline();
    }

    @Test
    public void testErrorWithAttachedPredicate() {
        arg.mustBe(integerValue(), Message.SEED_NOT_VALID_INT);
        assertCastPreserves(RDataFactory.createIntVectorFromScalar(1));
        assertCastFail(Boolean.FALSE, RError.Message.SEED_NOT_VALID_INT.message);
        testPipeline();
    }

    @Test
    public void testWarning() {
        arg.shouldBe(instanceOf(String.class), RError.Message.DLL_LOAD_ERROR, Function.identity(), "123");
        assertCastPreserves("A");
        assertCastWarning(Boolean.FALSE, cast(Boolean.FALSE), "unable to load shared object 'false'\n  123");
        testPipeline(NO_FILTER_EXPECT_EMPTY_SAMPLES);
    }

    @Test
    public void testWarningWithAttachedPredicate() {
        arg.shouldBe(integerValue(), Message.SEED_NOT_VALID_INT);
        assertCastPreserves(RDataFactory.createIntVectorFromScalar(1));
        assertCastWarning(Boolean.FALSE, cast(Boolean.FALSE), RError.Message.SEED_NOT_VALID_INT.message);
        testPipeline(NO_FILTER_EXPECT_EMPTY_SAMPLES);
    }

    @Test
    public void testDefaultError() {
        arg.mustBe(atomicLogicalValue().and(logicalTrue()));
        assertCastFail(RRuntime.LOGICAL_FALSE, String.format(RError.Message.INVALID_ARGUMENT.message, "x"));
        testPipeline(true);
    }

    @Test
    public void testIsInteger() {
        arg.mustBe(integerValue(), RError.Message.SEED_NOT_VALID_INT);
        assertCastPreserves(1, RDataFactory.createIntVectorFromScalar(1));
        assertCastFail("x", RError.Message.SEED_NOT_VALID_INT.message);
        testPipeline();
    }

    @Test
    public void testIsNumericOrComplex() {
        arg.mustBe(numericValue().or(complexValue()), RError.Message.SEED_NOT_VALID_INT);

        assertCastPreserves(1, RRuntime.LOGICAL_FALSE, 1.3d, RDataFactory.createComplex(1, 2), RDataFactory.createIntVectorFromScalar(1));
        assertCastPreserves(RDataFactory.createLogicalVectorFromScalar(RRuntime.LOGICAL_FALSE), RDataFactory.createDoubleVectorFromScalar(1.2));
        assertCastPreserves(RDataFactory.createComplexVectorFromScalar(RDataFactory.createComplex(1, 2)));

        assertCastFail("x", RError.Message.SEED_NOT_VALID_INT.message);
        testPipeline();
    }

    @Test
    public void testAsInteger() {
        arg.asIntegerVector();

        assertCastPreserves(1, -2, RRuntime.INT_NA);
        assertEquals(0, cast(RRuntime.LOGICAL_FALSE));
        assertEquals(1, cast(1.3d));
        assertEquals(42, cast("42"));
        assertEquals(1, cast(RDataFactory.createComplex(1, 0)));
        assertEquals(RNull.instance, cast(RNull.instance));
        assertEquals(RMissing.instance, cast(RMissing.instance));
        testPipeline(NO_FILTER_EXPECT_EMPTY_SAMPLES);
    }

    @Test
    public void testAsDouble() {
        arg.asDoubleVector();

        assertCastPreserves(1.2, RRuntime.DOUBLE_NA, Double.NaN, Double.POSITIVE_INFINITY);
        assertEquals(1.0, cast(RRuntime.LOGICAL_TRUE));
        assertEquals(1.0, cast("1"));
        assertEquals(RRuntime.DOUBLE_NA, cast(RRuntime.INT_NA));
        assertEquals(RNull.instance, cast(RNull.instance));
        assertEquals(RMissing.instance, cast(RMissing.instance));
        testPipeline();
    }

    @Test
    public void testAsLogical() {
        arg.asLogicalVector();

        assertCastPreserves(RRuntime.LOGICAL_FALSE, RRuntime.LOGICAL_NA);
        assertEquals(RRuntime.LOGICAL_TRUE, cast(1.2));
        assertEquals(RNull.instance, cast(RNull.instance));
        assertEquals(RRuntime.LOGICAL_NA, cast(RRuntime.INT_NA));
        assertEquals(RMissing.instance, cast(RMissing.instance));
        testPipeline(NO_FILTER_EXPECT_EMPTY_SAMPLES);
    }

    @Test
    public void testAsString() {
        arg.asStringVector();

        assertEquals("1.2", cast(1.2));
        assertEquals("TRUE", cast(RRuntime.LOGICAL_TRUE));
        assertEquals("FALSE", cast(RRuntime.LOGICAL_FALSE));
        assertEquals("NA", cast(RRuntime.LOGICAL_NA));
        assertEquals("NA", cast(RRuntime.INT_NA));
        assertEquals(RNull.instance, cast(RNull.instance));
        assertEquals(RMissing.instance, cast(RMissing.instance));
        testPipeline(NO_FILTER_EXPECT_EMPTY_SAMPLES);
    }

    @Test
    public void testAsVector() {
        // Note: unlike asXYZVector, asVector always converts to a vector, even scalar values
        arg.asVector();
        assertCastPreserves(RNull.instance, RMissing.instance);
        testAsVectorBase();
    }

    @Test
    public void testAsVectorWithPreserveNonVectorEqualToFalse() {
        // preserveNonVector is true by default
        arg.asVector(/* preserveNonVector: */false);

        Object result = cast(RNull.instance);
        assertTrue(result instanceof RList);
        assertTrue(((RList) result).getLength() == 0);

        result = cast(RMissing.instance);
        assertTrue(result instanceof RList);
        assertTrue(((RList) result).getLength() == 0);

        testAsVectorBase();
    }

    private void testAsVectorBase() {
        assertVectorEquals(RDataFactory.createIntVectorFromScalar(1), cast(1));
        assertVectorEquals(RDataFactory.createDoubleVectorFromScalar(1), cast(1.0));
        assertCastPreserves(RDataFactory.createStringVector(new String[]{"a", "b"}, true));
        testPipeline(NO_FILTER_EXPECT_EMPTY_SAMPLES);
    }

    @Test
    public void testEmptyError() {
        arg.asIntegerVector().mustBe(notEmpty());
        assertCastPreserves(RDataFactory.createIntVector(new int[]{42, 1}, true));
        assertCastFail(RDataFactory.createIntVector(0), String.format(RError.Message.INVALID_ARGUMENT.message, "x"));
        testPipeline();
        // TODO: error in the sampler
    }

    @Test
    public void testEmptyErrorWithCustomMessage() {
        arg.asIntegerVector().mustBe(notEmpty(), RError.Message.SEED_NOT_VALID_INT);
        assertCastFail(RDataFactory.createIntVector(0), Message.SEED_NOT_VALID_INT.message);
    }

    @Test
    public void testSizeWarning() {
        arg.defaultWarning(SHOW_CALLER, RError.Message.LENGTH_GT_1).asIntegerVector().shouldBe(singleElement());
        RIntVector intVector = RDataFactory.createIntVector(new int[]{1, 2}, true);
        assertCastWarning(intVector, intVector, RError.Message.LENGTH_GT_1.message);
        testPipeline(NO_FILTER_EXPECT_EMPTY_SAMPLES);
    }

    @Test
    public void testFindFirstWithDefaultValue() {
        arg.asIntegerVector().findFirst(42);

        assertEquals(42, cast(RNull.instance));
        assertEquals(42, cast(RMissing.instance));
        assertEquals(1, cast(1));
        assertEquals(1, cast(RDataFactory.createIntVector(new int[]{1, 2}, true)));
        assertEquals(1, cast("1"));
        assertEquals(42, cast(RDataFactory.createIntVector(0)));
        assertEquals(42, cast(RDataFactory.createList()));
        testPipeline(NO_FILTER_EXPECT_EMPTY_SAMPLES);
    }

    @Test
    public void testFindFirstWithDefaultError() {
        // findFirst takes the explicitly specified default error from pipeline
        arg.asIntegerVector().defaultError(SHOW_CALLER, RError.Message.SEED_NOT_VALID_INT).findFirst();
        assertCastFail(RNull.instance, Message.SEED_NOT_VALID_INT.message);
    }

    @Test
    public void testFindFirstWithoutDefaultValue() {
        arg.asIntegerVector().findFirst();

        assertCastFail(RNull.instance, "argument is of length zero");
        assertCastFail(RMissing.instance, "argument is of length zero");
        assertCastFail(RDataFactory.createIntVector(0), "argument is of length zero");
        assertCastFail(RDataFactory.createList(), "argument is of length zero");
        assertEquals(1, cast(1));
        assertEquals(1, cast(RDataFactory.createIntVector(new int[]{1, 2}, true)));
        assertEquals(1, cast("1"));
        testPipeline(NO_FILTER_EXPECT_EMPTY_SAMPLES);
    }

    @Test
    public void testNoLogicalNA() {
        arg.asLogicalVector().findFirst().notNA(RRuntime.LOGICAL_TRUE);

        assertCastPreserves(RRuntime.LOGICAL_FALSE);
        assertEquals(RRuntime.LOGICAL_TRUE, cast(RRuntime.LOGICAL_NA));
        testPipeline();
    }

    @Test
    public void testNoIntegerNA() {
        arg.asIntegerVector().findFirst().notNA(1);

        assertCastPreserves(42);
        assertEquals(1, cast(RRuntime.INT_NA));
        testPipeline();
    }

    @Test
    public void testNoDoubleNA() {
        arg.asDoubleVector().findFirst().notNA(1.1);

        assertCastPreserves(3.142);
        assertEquals(1.1, cast(RRuntime.DOUBLE_NA));
        testPipeline();
    }

    @Test
    public void testNoStringNA() {
        arg.asStringVector().findFirst().notNA("A");

        assertCastPreserves("hello world");
        assertEquals("A", cast(RRuntime.DOUBLE_NA));
        testPipeline();
    }

    @Test
    public void testSingleElementWarning() {
        arg.asIntegerVector().shouldBe(singleElement()).findFirst(0);
        assertEquals(1, cast(RDataFactory.createIntVector(new int[]{1, 2}, true)));
        testPipeline();
    }

    @Test
    public void testSingleElementWarningAndEmptyError() {
        arg.asIntegerVector().shouldBe(singleElement(), RError.Message.INVALID_USE, "y").mustBe(notEmpty(), RError.Message.ARGUMENT_EMPTY, 42);

        RIntVector vec = RDataFactory.createIntVector(new int[]{1, 2}, true);
        assertCastWarning(vec, vec, String.format(RError.Message.INVALID_USE.message, "y"));
        assertCastFail(RDataFactory.createIntVector(0), String.format(Message.ARGUMENT_EMPTY.message, 42));
        testPipeline();
    }

    @Test
    public void testLogicalToBooleanPipeline() {
        arg.asLogicalVector().findFirst(RRuntime.LOGICAL_FALSE).map(toBoolean());
        assertEquals(Boolean.TRUE, cast(RDataFactory.createLogicalVector(new byte[]{RRuntime.LOGICAL_TRUE, RRuntime.LOGICAL_FALSE}, true)));
        assertEquals(Boolean.FALSE, cast(RDataFactory.createLogicalVector(0)));
        testPipeline(NO_FILTER_EXPECT_EMPTY_SAMPLES);
    }

    @Test
    public void testMustBeIntegerAndAsIntegerVector() {
        arg.mustBe(integerValue(), Message.SEED_NOT_VALID_INT).asIntegerVector();
        assertCastPreserves(RDataFactory.createIntVector(new int[]{1, 2}, true));
        assertCastFail("x", RError.Message.SEED_NOT_VALID_INT.message);
        testPipeline();
    }

    //@formatter:off

    @Test
    public void testComplexPipeline() {
        arg.defaultError(RError.Message.INVALID_ARGUMENT, "fill").
                mustBe(numericValue().or(logicalValue())).
                asVector().mustBe(singleElement()).findFirst().
                shouldBe(atomicLogicalValue().or(atomicIntegerValue().and(gt(0))), Message.NON_POSITIVE_FILL).
                mapIf(atomicLogicalValue(), toBoolean());


        assertCastPreserves(10);
        assertEquals(true, cast(RRuntime.LOGICAL_TRUE));
        assertCastFail("xyz", String.format(RError.Message.INVALID_ARGUMENT.message, "fill"));
        assertCastFail(RDataFactory.createIntVector(new int[]{1, 2}, true), String.format(RError.Message.INVALID_ARGUMENT.message, "fill"));
        assertCastFail(RDataFactory.createIntVector(0), String.format(RError.Message.INVALID_ARGUMENT.message, "fill"));
        assertCastWarning(-10, -10, String.format(Message.NON_POSITIVE_FILL.message, "fill"));
        testPipeline();
    }

    @Test
    public void testMapNull() {
        arg.mapNull(emptyStringVector()).mustBe(stringValue(), RError.Message.INVALID_ARGUMENT, "labels").asStringVector();
        testPipeline();

        assertCastPreserves("abc", RDataFactory.createStringVector(new String[]{"abc", "def"}, true));

        Object asv = cast(RNull.instance);
        assertEquals(true, asv instanceof RAbstractStringVector);
        assertEquals(0, ((RAbstractStringVector) asv).getLength());

        asv = cast(RDataFactory.createStringVector(0));
        assertEquals(true, asv instanceof RAbstractStringVector);
        assertEquals(0, ((RAbstractStringVector) asv).getLength());

        assertCastFail(123, String.format(RError.Message.INVALID_ARGUMENT.message, "labels"));
    }

    @Test
    public void testNumberComparison() {
        arg.asIntegerVector().findFirst().mustBe(gte(2).and(lte(5)).not());
        assertCastPreserves(10, 0);
        assertCastFail(2);
        assertCastFail(5);
        assertCastFail(3);
        // TODO sampler does not work here testPipeline(NO_FILTER_EXPECT_EMPTY_SAMPLES);
    }

    @Test
    public void testMapNonListToNull() {
        arg.mapIf(instanceOf(RList.class).not(), nullConstant());
        assertCastPreserves(RDataFactory.createList());
        assertEquals(RNull.instance, cast("abc"));
    }

    @Test
    public void testChain() {
        arg.
            mapIf(instanceOf(RList.class).not(),
               chain(asLogicalVector()).
                  with(findFirst().logicalElement()).
                  with(notNA()).
                  with(map(toBoolean())).
                  with(mustBe(instanceOf(Boolean.class))).
                  with(shouldBe(instanceOf(Object.class))).
                  end());

        assertCastPreserves(RDataFactory.createList());
        assertEquals(true, cast(1));
        assertCastFail(RRuntime.INT_NA);
    }

    public Function<InitialPhaseBuilder<Object>, InitialPhaseBuilder<Object>> nonListToBoolean() {
        return phaseBuilder -> phaseBuilder.
                        mapIf(instanceOf(RList.class).not(),
                           chain(asLogicalVector()).
                              with(findFirst().logicalElement()).
                              with(toBoolean()).
                              end());
    }

    @Test
    public void testAlias() {
        arg.alias(nonListToBoolean());
        assertCastPreserves(RDataFactory.createList());
        assertEquals(true, cast(1));
    }

    //@formatter:on

    @Test
    public void testAllowNullFindFirst() {
        // Note: when null is allowed findFirst does not fail
        arg.allowNull().mustBe(stringValue()).asStringVector().findFirst();

        assertCastPreserves("abc", RNull.instance);
        assertEquals("abc", cast(RDataFactory.createStringVector(new String[]{"abc", "xyz"}, true)));
        assertCastFail(RMissing.instance);
    }

    @Test
    public void notFirstNAElementInStringVector() {
        arg.asStringVector().mustBe(elementAt(0, RRuntime.STRING_NA).not().and(singleElement()));

        assertCastPreserves(RDataFactory.createStringVector(new String[]{"abc"}, true), "abc");
        assertCastFail(RDataFactory.createStringVector(new String[]{"abc", "xyz"}, true));
        assertCastFail(RDataFactory.createStringVector(new String[]{RRuntime.STRING_NA}, false));
    }

    @Test
    public void testMessageArgumentAsLambda() {
        Function<Object, Object> argMsg = name -> "something";
        arg.conf(c -> c.allowNull().mustNotBeMissing(SHOW_CALLER, RError.Message.GENERIC, argMsg)).mustBe(stringValue(), RError.Message.GENERIC, argMsg);

        assertCastPreserves(RNull.instance);
        assertCastFail(RMissing.instance, "something");
        assertCastFail(42, "something");
    }

    @Test
    public void testSample17() {
        // TODO: Filter implementation for sampling fails on assertion
        arg.asDoubleVector().findFirst().mapIf(doubleNA().not().and(not(isFractional())), doubleToInt());

        Object r = cast(RRuntime.STRING_NA);
        assertTrue(r instanceof Double);
        assertTrue(RRuntime.isNA((double) r));
        assertEquals(42, cast("42"));
        assertEquals(42.2, cast("42.2"));
    }

    @Test
    public void testMustBeSquareMatrix() {
        arg.asDoubleVector(true, true, true).mustBe(squareMatrix());

        RIntVector vec = RDataFactory.createIntVector(new int[]{0, 1, 2, 3}, true, new int[]{2, 2});
        Object res = cast(vec);
        assertTrue(res instanceof RAbstractDoubleVector);
        RAbstractDoubleVector dvec = (RAbstractDoubleVector) res;
        assertNotNull(dvec.getDimensions());
        assertEquals(2, dvec.getDimensions().length);
        assertEquals(2, dvec.getDimensions()[0]);
        assertEquals(2, dvec.getDimensions()[1]);

        RIntVector notSquare = RDataFactory.createIntVector(new int[]{0, 1, 2, 3}, true, new int[]{1, 4});
        assertCastFail(notSquare);
    }

    @Test
    public void testDimensionsFilter() {
        arg.asDoubleVector(true, true, true).mustBe(dimGt(1, 0));

        RIntVector vec = RDataFactory.createIntVector(new int[]{0, 1, 2, 3}, true, new int[]{2, 2});
        RDoubleVector doubleVec = RDataFactory.createDoubleVector(new double[]{0, 1, 2, 3}, true, new int[]{2, 2});
        assertVectorEquals(doubleVec, cast(vec));
    }

    @Test
    public void testDimensionsFilterNegative() {
        arg.asDoubleVector(true, true, true).mustBe(dimGt(4, 0));
        assertCastFail(RDataFactory.createIntVector(new int[]{0, 1, 2, 3}, true, new int[]{2, 2}));
    }

    @Test
    public void testMapIf() {
        arg.allowNull().mapIf(Predef.integerValue(), asIntegerVector(), asStringVector(true, false, false));

        RDoubleVector vec = RDataFactory.createDoubleVector(new double[]{0, 1, 2, 3}, true);
        Object res = cast(vec);
        assertTrue(res instanceof RAbstractStringVector);

        assertCastPreserves(RDataFactory.createSharedIntVectorFromScalar(42));
    }

    @Test
    public void testSample21() {
        arg.mustBe(numericValue()).asVector().mustBe(singleElement()).findFirst().shouldBe(
                        instanceOf(Byte.class).or(instanceOf(Integer.class).and(gt0())),
                        Message.NON_POSITIVE_FILL).mapIf(atomicLogicalValue(), asBoolean(), asInteger());
        // TODO: asserts
        testPipeline();
    }

    @Test
    public void testPreserveNonVectorFlag() {
        arg.allowNull().asVector(true);
        assertEquals(RNull.instance, cast(RNull.instance));
    }

    @Test
    public void testNotPreserveNonVectorFlag() {
        arg.asVector(false);

        Object res = cast(RNull.instance);
        Assert.assertTrue(res instanceof RList);
        Assert.assertEquals(0, ((RList) res).getLength());
    }

    @Test
    public void defaultErrorForMustNotBeNull() {
        arg.defaultError(RError.SHOW_CALLER, Message.SEED_NOT_VALID_INT).mustNotBeNull();
        assertCastFail(RNull.instance, Message.SEED_NOT_VALID_INT.message);
    }

    /**
     * Casts given object using the configured pipeline in {@link #arg}.
     */
    private Object cast(Object a) {
        CastNode argCastNode = cb.getCasts()[0];
        NodeHandle<CastNode> argCastNodeHandle = TestUtilities.createHandle(argCastNode, (node, args) -> node.execute(args[0]));
        return argCastNodeHandle.call(a);
    }

    private void assertCastPreserves(Object... values) {
        for (Object value : values) {
            assertCastPreserves(value);
        }
    }

    private void assertCastPreserves(Object value) {
        assertEquals(value, cast(value));
    }

    private static void assertVectorEquals(RAbstractVector expected, Object actualObj) {
        RAbstractVector actual = (RAbstractVector) actualObj;
        assertEquals("vectors differ in size", expected.getLength(), actual.getLength());
        for (int i = 0; i < expected.getLength(); i++) {
            assertEquals("vectors differ at position " + i, expected.getDataAtAsObject(i), actual.getDataAtAsObject(i));
        }
    }

    private void assertCastWarning(Object value, Object expectedValue, String expectedMessage) {
        assertEquals(expectedValue, cast(value));
        assertEquals("Expected warning message", expectedMessage, CastNode.getLastWarning());
    }

    private void assertCastFail(Object value) {
        assertCastFail(value, String.format(RError.Message.INVALID_ARGUMENT.message, "x"));
    }

    private void assertCastFail(Object value, String expectedMessage) {
        try {
            cast(value);
            fail("cast should have failed");
        } catch (IllegalArgumentException e) {
            assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * This tests the pipeline sampling process: all positive samples ase successful and all
     * negative cause an error.
     */
    private void testPipeline() {
        testPipeline(false);
    }

    private void testPipeline(boolean emptyPositiveSamplesAllowed) {
        if (!TEST_SAMPLING) {
            return;
        }

        CastNodeSampler<CastNode> sampler = CastNodeSampler.createSampler(cb.getCasts()[0]);
        Samples<?> samples = sampler.collectSamples();
        if (!emptyPositiveSamplesAllowed) {
            Assert.assertFalse(samples.positiveSamples().isEmpty());
        }
        testPipeline(samples);
    }

    private void testPipeline(Samples<?> samples) {
        if (!TEST_SAMPLING) {
            return;
        }

        for (Object sample : samples.positiveSamples()) {
            try {
                cast(sample);
            } catch (Exception e) {
                e.printStackTrace();
                fail();
            }
        }
        for (Object sample : samples.negativeSamples()) {
            try {
                cast(sample);
                fail();
            } catch (IllegalArgumentException e) {
                // ok
            }
        }
    }

    /**
     * Just so that we do not have to build the annotation instance by hand.
     */
    @RBuiltin(kind = RBuiltinKind.PRIMITIVE, name = "forTestingOnly", parameterNames = {"x"}, behavior = RBehavior.PURE)
    private static class DummyBuiltin {
    }
}
