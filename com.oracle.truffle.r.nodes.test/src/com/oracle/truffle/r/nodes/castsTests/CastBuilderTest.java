/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.castsTests;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.abstractVectorValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.anyValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.asBoolean;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.asInteger;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.asIntegerVector;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.asLogicalVector;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.asStringVector;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.asVector;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.atomicIntegerValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.atomicLogicalValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.chain;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.complexValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.constant;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.dimGt;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.doubleNA;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.doubleToInt;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.doubleValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.elementAt;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.emptyStringVector;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.findFirst;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.gt;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.gt0;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.gte;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.instanceOf;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.intNA;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.integerValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.isFractional;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.logicalTrue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.logicalValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.lte;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.map;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.missingValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.mustBe;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.mustNotBeNA;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.not;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.notEmpty;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.nullConstant;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.nullValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.numericValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.rawValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.shouldBe;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.singleElement;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.squareMatrix;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.function.Function;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef;
import com.oracle.truffle.r.nodes.builtin.casts.fluent.InitialPhaseBuilder;
import com.oracle.truffle.r.nodes.builtin.casts.fluent.PreinitialPhaseBuilder;
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
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RIntSequence;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.env.REnvironment.NewEnv;
import com.oracle.truffle.r.test.generate.FastRSession;

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
    private CastNode castNode;
    private PreinitialPhaseBuilder arg;

    private static FastRSession fastRSession;

    static {
        fastRSession = FastRSession.create();
        CastNode.testingMode();
    }

    @Before
    public void setUp() {
        cb = new CastBuilder(DummyBuiltin.class.getAnnotation(RBuiltin.class));
        arg = cb.arg("x");
        castNode = null;
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

        // TODO: it fails (NPE) due to the uninitialized RContext.runtimeASTAccess field
        // assertEquals("1.2", cast(1.2));
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
        arg.defaultWarning(RError.Message.LENGTH_GT_1).asIntegerVector().shouldBe(singleElement());
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
        arg.defaultError(RError.Message.SEED_NOT_VALID_INT).asIntegerVector().findFirst();
        assertCastFail(RNull.instance, Message.SEED_NOT_VALID_INT.message);
    }

    @Test
    public void testFindFirstWithoutDefaultValue() {
        arg.asIntegerVector().findFirst();

        assertCastFail(RNull.instance, "invalid 'x' argument");
        assertCastFail(RMissing.instance, "invalid 'x' argument");
        assertCastFail(RDataFactory.createIntVector(0), "invalid 'x' argument");
        assertCastFail(RDataFactory.createList(), "invalid 'x' argument");
        assertEquals(1, cast(1));
        assertEquals(1, cast(RDataFactory.createIntVector(new int[]{1, 2}, true)));
        assertEquals(1, cast("1"));
        testPipeline(NO_FILTER_EXPECT_EMPTY_SAMPLES);
    }

    @Test
    public void testNoLogicalNA() {
        arg.asLogicalVector().findFirst().replaceNA(RRuntime.LOGICAL_TRUE);

        assertCastPreserves(RRuntime.LOGICAL_FALSE);
        assertEquals(RRuntime.LOGICAL_TRUE, cast(RRuntime.LOGICAL_NA));
        testPipeline();
    }

    @Test
    public void testNoIntegerNA() {
        arg.asIntegerVector().findFirst().replaceNA(1);

        assertCastPreserves(42);
        assertEquals(1, cast(RRuntime.INT_NA));
        testPipeline();
    }

    @Test
    public void testNoDoubleNA() {
        arg.asDoubleVector().findFirst().replaceNA(1.1);

        assertCastPreserves(3.142);
        assertEquals(1.1, cast(RRuntime.DOUBLE_NA));
        testPipeline();
    }

    @Test
    public void testNoStringNA() {
        arg.asStringVector().findFirst().replaceNA("A");

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
        assertEquals(Boolean.TRUE, cast(RRuntime.LOGICAL_TRUE));
        assertEquals(Boolean.FALSE, cast(RRuntime.LOGICAL_FALSE));
        assertEquals(Boolean.TRUE, cast(RRuntime.LOGICAL_NA));
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
                  with(mustNotBeNA()).
                  with(map(toBoolean())).
                  with(mustBe(instanceOf(Boolean.class))).
                  with(shouldBe(instanceOf(Object.class))).
                  end());

        assertCastPreserves(RDataFactory.createList());
        assertEquals(true, cast(1));
        assertCastFail(RRuntime.INT_NA);
    }

    @Test
    public void testChain2() {
        arg.
        mapIf(instanceOf(RAbstractListVector.class),
           chain(asVector()).
              with(findFirst().objectElement()).
              end()).mustBe(instanceOf(RList.class).or(instanceOf(REnvironment.class)));

        RList l = RDataFactory.createList();
        assertEquals(l, cast(RDataFactory.createList(new Object[]{l})));
        NewEnv env = RDataFactory.createNewEnv("aaa");
        assertEquals(env, cast(RDataFactory.createList(new Object[]{env})));
        assertCastPreserves(env);
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
        arg.mustNotBeMissing(RError.Message.GENERIC, argMsg).mustBe(stringValue(), RError.Message.GENERIC, argMsg);

        assertCastPreserves("abc");
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

        assertCastFail(1.0);

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
    public void testSample22() {
        arg.mapIf(nullValue().or(missingValue()), emptyStringVector()).mustBe(stringValue());
        arg.mapNull(emptyStringVector()).mustBe(stringValue());
        Object res = cast(RNull.instance);
        assertTrue(res instanceof RAbstractStringVector);
        assertEquals(0, ((RAbstractStringVector) res).getLength());
        res = cast(RMissing.instance);
        assertTrue(res instanceof RAbstractStringVector);
        assertEquals(0, ((RAbstractStringVector) res).getLength());
        res = cast("abc");
        assertEquals("abc", res);
    }

    @Test
    public void testSample23() {
        //@formatter:off
        arg.defaultError(RError.Message.INVALID_UNNAMED_ARGUMENTS).
            mustBe(abstractVectorValue()).
            asIntegerVector().
            findFirst(RRuntime.INT_NA).
            mustBe(intNA().not().and(gte(0)));
        //@formatter:on
        assertEquals(1, cast(1));
        assertEquals(1, cast(1));
        assertEquals(1, cast("1"));
        assertCastFail(RError.Message.INVALID_UNNAMED_ARGUMENTS.message, RRuntime.INT_NA, -1, RNull.instance);
    }

    @Test
    public void testSampleNonNASequence() {
        arg.mustNotBeNA(RError.Message.GENERIC, "Error");
        RIntSequence seq = RDataFactory.createIntSequence(1, 1, 1);
        Object res = cast(seq);
        Assert.assertSame(seq, res);
    }

    @Test
    public void testSampleNAVector() {
        arg.replaceNA("REPLACEMENT");
        RDoubleVector vec = RDataFactory.createDoubleVector(new double[]{0, 1, RRuntime.DOUBLE_NA, 3}, false);
        Object res = cast(vec);
        Assert.assertEquals("REPLACEMENT", res);
    }

    @Test
    public void testPreserveNonVectorFlag() {
        arg.asVector(true);
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
        arg.defaultError(Message.SEED_NOT_VALID_INT).mustNotBeNull();
        assertCastFail(RNull.instance, Message.SEED_NOT_VALID_INT.message);
    }

    // RNull/RMissing tests. See com.oracle.truffle.r.nodes.builtin.casts.Filter$ResultForArg.

    @Test
    public void testMustBeNull() {
        arg.mustBe(nullValue());
        cast(RNull.instance);
    }

    @Test
    public void testMustNotBeNull() {
        arg.mustBe(nullValue().not());
        try {
            cast(RNull.instance);
            fail();
        } catch (Exception e) {
        }
    }

    @Test
    public void testMustNotBeNullAndNotDouble() {
        arg.mustBe(nullValue().not().and(doubleValue().not()));
        Assert.assertEquals("A", cast("A"));
        try {
            cast(RNull.instance);
            fail();
        } catch (Exception e) {
        }
        try {
            cast(1.23);
            fail();
        } catch (Exception e) {
        }
    }

    @Test
    public void testMapIfNull() {
        arg.mapIf(nullValue(), constant("A"), constant(1));
        Assert.assertEquals("A", cast(RNull.instance));
        Assert.assertEquals(1, cast("X"));
    }

    @Test
    public void testBlockingNull1() {
        // Here, the result for NULL in the 'singleElement()' filter is UNDEFINED, i.e. NULL does
        // not pass through.
        arg.asStringVector().mustBe(singleElement());
        try {
            cast(RNull.instance);
            fail();
        } catch (Exception e) {
        }
    }

    @Test
    public void testBlockingNull2() {
        // Here, the result for NULL in the 'instanceOf(RIntSequence.class)' filter is FALSE,
        // i.e. NULL does not pass through.
        arg.mustBe(instanceOf(RIntSequence.class));
        try {
            cast(RNull.instance);
            fail();
        } catch (Exception e) {
        }
    }

    @Test
    public void testPassingNull1() {
        // Here, the result for NULL in the 'stringValue().not()' filter is TRUE,
        // i.e. NULL passes through.
        arg.mustBe(stringValue().not());
        Assert.assertEquals(RNull.instance, cast(RNull.instance));
    }

    @Test
    public void testBlockingNull3() {
        // Here, the result for NULL in the 'instanceOf(RIntSequence.class).not()' filter is TRUE,
        // while in the 'integerValue()' filter the result is FALSE. The result of the
        // conjunction of the two filters by the 'and' operator is FALSE, i.e. NULL does not
        // pass through.
        arg.mustBe(instanceOf(RIntSequence.class).not().and(integerValue()));
        try {
            cast(RNull.instance);
            fail();
        } catch (Exception e) {
        }
    }

    @Test
    public void testBlockingNull4() {
        // Here, the result for NULL in the 'instanceOf(RIntSequence.class)' filter is FALSE,
        // while in the 'singleElement()' filter the result is UNDEFINED. The result of the
        // conjunction of the two filters by the 'and' operator is UNDEFINED, i.e. NULL does not
        // pass through.
        arg.asIntegerVector().mustBe(instanceOf(RIntSequence.class).and(singleElement()));
        try {
            cast(RNull.instance);
            fail();
        } catch (Exception e) {
        }
    }

    @Test
    public void testPassingNull2() {
        // Here, the result for NULL in the 'instanceOf(RIntSequence.class).not()' filter is TRUE,
        // while in the 'stringValue()' filter the result is FALSE. The result of the
        // disjunction of the two filters by the 'or' operator is TRUE, i.e. NULL passes through.
        arg.mustBe(instanceOf(RIntSequence.class).not().or(stringValue()));
        Assert.assertEquals(RNull.instance, cast(RNull.instance));
    }

    @Test
    public void testBlockingNullAllowingMissing() {
        arg.mustNotBeNull().mustBe(stringValue().not());
        Assert.assertEquals(1, cast(1));
        try {
            cast(RNull.instance);
            fail();
        } catch (Exception e) {
        }
        Assert.assertEquals(RMissing.instance, cast(RMissing.instance));
    }

    @Test
    public void testMapIfPassingNull() {
        // The condition 'stringValue()' returns FALSE for NULL, i.e. NULL passes through unmapped.
        arg.mapIf(stringValue(), constant(1));
        Assert.assertEquals(RNull.instance, cast(RNull.instance));
        Assert.assertEquals(1, cast("abc"));
    }

    @Test
    public void testMapIfMappingNull() {
        // The condition 'stringValue()' returns TRUE for NULL, i.e. NULL is mapped to 1.
        arg.mapIf(stringValue().not(), constant(1));
        Assert.assertEquals(1, cast(RNull.instance));
        Assert.assertEquals("abc", cast("abc"));
    }

    @Test
    public void testComplexFilterWithForwardedNull() {
        arg.mustBe(nullValue().or(numericValue()).or(stringValue()).or(complexValue())).mapIf(numericValue().or(complexValue()), asIntegerVector());
        Assert.assertEquals(RNull.instance, cast(RNull.instance));
        Assert.assertEquals("abc", cast("abc"));
    }

    @Test
    public void testFindFirstOrNull() {
        arg.mustBe(nullValue().or(integerValue())).asIntegerVector().findFirstOrNull();
        Assert.assertEquals(RNull.instance, cast(RNull.instance));
        Assert.assertEquals(1, cast(1));
    }

    @Test
    public void testReturnIf() {
        arg.returnIf(nullValue(), constant(1.1)).mustBe(logicalValue()).asLogicalVector().findFirst().map(toBoolean());
        Assert.assertEquals(1.1, cast(RNull.instance));
        Assert.assertEquals(true, cast(RRuntime.LOGICAL_TRUE));
    }

    @Test
    public void testNotNullAndNotMissing() {
        arg.mustBe(nullValue().not().and(missingValue().not()));
        try {
            cast(RNull.instance);
            fail();
        } catch (Exception e) {
        }
        try {
            cast(RMissing.instance);
            fail();
        } catch (Exception e) {
        }
        Assert.assertEquals("abc", cast("abc"));
    }

    @Test
    public void testWarningInElseBranchOfMapIf() {
        arg.mapIf(numericValue(),
                        chain(asIntegerVector()).with(findFirst().integerElement()).end(),
                        chain(asIntegerVector()).with(Predef.shouldBe(anyValue().not(), RError.Message.NA_INTRODUCED_COERCION)).end());

        Assert.assertEquals(1, cast(1));
        Assert.assertEquals(1, cast("1"));
        Assert.assertEquals(RError.Message.NA_INTRODUCED_COERCION.message, CastNode.getLastWarning());
    }

    @Test
    public void testWarningInTrueBranchOfMapIf() {
        arg.allowNull().mapIf(stringValue(), chain(asStringVector()).with(shouldBe(anyValue().not(),
                        RError.Message.NA_INTRODUCED_COERCION)).end(),
                        asIntegerVector());
        Assert.assertEquals("1", cast("1"));
        Assert.assertEquals(RError.Message.NA_INTRODUCED_COERCION.message, CastNode.getLastWarning());
    }

    private static final RFunction DUMMY_FUNCTION = RDataFactory.createFunction(RFunction.NO_NAME, RFunction.NO_NAME, null, null, null);

    @Test
    public void testReturnIfFunction() {
        arg.allowNull().returnIf(instanceOf(RFunction.class)).asVector(false);
        RFunction f = DUMMY_FUNCTION;
        Object o = cast(f);
        assertEquals(f, o);
        testPipeline();
    }

    @Test
    public void testIntOrRawPassing() {
        arg.mustNotBeNull().mustBe(integerValue().or(rawValue()));
        assertCastPreserves(1);
        assertCastPreserves(RDataFactory.createIntVector(new int[]{1, 2}, true));
        assertCastPreserves(RDataFactory.createRaw((byte) 2));
        assertCastPreserves(RDataFactory.createRawVector(new byte[]{1, 2}));
        assertCastFail(RNull.instance);
        assertCastFail("abc");
    }

    /**
     * Casts given object using the configured pipeline in {@link #arg}.
     */
    private Object cast(Object a) {
        CastNode.clearLastWarning();
        NodeHandle<CastNode> argCastNodeHandle = TestUtilities.createHandle(getCastNode(), (node, args) -> node.doCast(args[0]), fastRSession.getContext().getLanguage());
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

    private void assertCastFail(String expectedMessage, Object... values) {
        for (Object value : values) {
            assertCastFail(value, expectedMessage);
        }
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
     * This tests the pipeline sampling process: all positive samples are successful and all
     * negative cause an error.
     */
    private static void testPipeline() {
        testPipeline(false);
    }

    private static void testPipeline(@SuppressWarnings("unused") boolean emptyPositiveSamplesAllowed) {
        if (!TEST_SAMPLING) {
            return;
        }

        // TODO:
    }

    private CastNode getCastNode() {
        if (castNode == null) {
            castNode = cb.getCasts()[0];
        }
        return castNode;
    }

    @SuppressWarnings("unused")
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
    static class DummyBuiltin {
    }
}
