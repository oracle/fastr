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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.function.Function;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef;
import com.oracle.truffle.r.nodes.builtin.casts.fluent.InitialPhaseBuilder;
import com.oracle.truffle.r.nodes.builtin.casts.fluent.PipelineConfigBuilder;
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
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;

public class CastBuilderTest {

    private CastBuilder cb;
    private PreinitialPhaseBuilder<Object> arg;

    static {
        PipelineConfigBuilder.setFilterFactory(FilterSamplerFactory.INSTANCE);
        PipelineConfigBuilder.setMapperFactory(MapperSamplerFactory.INSTANCE);
    }

    @Before
    public void setUp() {
        cb = new CastBuilder(null);
        arg = cb.arg(0, "x");
    }

    @After
    public void tearDown() {
        cb = null;
    }

    @Test
    public void testError() {
        arg.mustBe(instanceOf(String.class),
                        RError.Message.DLL_LOAD_ERROR, Function.identity(), "123");
        testPipeline();

        assertEquals("A", cast("A"));
        try {
            cast(Boolean.FALSE);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("unable to load shared object 'false'\n  123", e.getMessage());
        }

    }

    @Test
    public void testErrorWithAttachedPredicate() {
        arg.mustBe(integerValue(), Message.SEED_NOT_VALID_INT);
        testPipeline();

        RAbstractIntVector v = RDataFactory.createIntVectorFromScalar(1);
        assertEquals(v, cast(v));
        try {
            cast(Boolean.FALSE);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals(RError.Message.SEED_NOT_VALID_INT.message, e.getMessage());
        }
    }

    @Test
    public void testWarning() {
        arg.shouldBe(instanceOf(String.class), RError.Message.DLL_LOAD_ERROR, Function.identity(), "123");
        testPipeline();

        assertEquals("A", cast("A"));
        assertEquals(Boolean.FALSE, cast(Boolean.FALSE));
        // assertEquals("unable to load shared object 'false'\n 123", out.toString());
    }

    @Test
    public void testWarningWithAttachedPredicate() {
        arg.shouldBe(integerValue(), Message.SEED_NOT_VALID_INT);
        testPipeline();

        RAbstractIntVector v = RDataFactory.createIntVectorFromScalar(1);
        assertEquals(v, cast(v));
        assertEquals(Boolean.FALSE, cast(Boolean.FALSE));
        // assertEquals(RError.Message.SEED_NOT_VALID_INT.message, out.toString());
    }

    @Test
    public void testDefaultError() {
        arg.mustBe(atomicLogicalValue().and(logicalTrue()));
        testPipeline(false);

        try {
            cast(RRuntime.LOGICAL_FALSE);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals(String.format(RError.Message.INVALID_ARGUMENT.message, "arg0"), e.getMessage());
        }
    }

    @Test
    public void testIsInteger() {
        arg.mustBe(integerValue(), RError.Message.SEED_NOT_VALID_INT);
        testPipeline();

        RAbstractIntVector v = RDataFactory.createIntVectorFromScalar(1);
        assertEquals(v, cast(v));
        assertEquals(1, cast(1));
        try {
            cast("x");
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals(RError.Message.SEED_NOT_VALID_INT.message, e.getMessage());
        }
    }

    @Test
    public void testIsNumericOrComplex() {
        arg.mustBe(numericValue().or(complexValue()), RError.Message.SEED_NOT_VALID_INT);
        testPipeline();

        assertEquals(1, cast(1));
        assertEquals(RRuntime.LOGICAL_FALSE, cast(RRuntime.LOGICAL_FALSE));
        assertEquals(1.3d, cast(1.3d));
        RComplex c = RDataFactory.createComplex(1, 2);
        assertEquals(c, cast(c));

        Object v = RDataFactory.createIntVectorFromScalar(1);
        assertEquals(v, cast(v));
        v = RDataFactory.createLogicalVectorFromScalar(RRuntime.LOGICAL_FALSE);
        assertEquals(v, cast(v));
        v = RDataFactory.createDoubleVectorFromScalar(1.2);
        assertEquals(v, cast(v));
        v = RDataFactory.createComplexVectorFromScalar(c);
        assertEquals(v, cast(v));

        try {
            cast("x");
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals(RError.Message.SEED_NOT_VALID_INT.message, e.getMessage());
        }
    }

    @Test
    public void testAsInteger() {
        arg.asIntegerVector();
        testPipeline();

        assertEquals(1, cast(1));
        assertEquals(0, cast(RRuntime.LOGICAL_FALSE));
        assertEquals(1, cast(1.3d));
        RComplex c = RDataFactory.createComplex(1, 0);
        assertEquals(1, cast(c));
    }

    @Test
    public void testAsDouble() {
        arg.asDoubleVector();
        testPipeline();

        assertEquals(1.2, cast(1.2));
    }

    @Test
    public void testAsLogical() {
        arg.asLogicalVector();
        testPipeline();

        assertEquals(RRuntime.LOGICAL_TRUE, cast(1.2));
    }

    @Test
    public void testAsString() {
        arg.asStringVector();
        testPipeline();

        assertEquals("1.2", cast(1.2));
    }

    @Test
    public void testEmptyError() {
        arg.asIntegerVector().mustBe(notEmpty());
        testPipeline();

        try {
            cast(RDataFactory.createIntVector(0));
        } catch (IllegalArgumentException e) {
            assertEquals(String.format(RError.Message.INVALID_ARGUMENT.message, "x"), e.getMessage());
        }
    }

    @Test
    public void testEmptyErrorWithCustomMessage() {
        arg.asIntegerVector().mustBe(notEmpty(), RError.Message.SEED_NOT_VALID_INT);
        testPipeline();

        try {
            cast(RDataFactory.createIntVector(0));
        } catch (IllegalArgumentException e) {
            assertEquals(RError.Message.SEED_NOT_VALID_INT.message, e.getMessage());
        }
    }

    // TODO: is it OK to leave this out?
    // @Test
    // public void testSizeWarning() {
    // arg.defaultWarning(RError.Message.LENGTH_GT_1).asIntegerVector().shouldBe(singleElement());
    // testPipeline();
    //
    // cast(RDataFactory.createIntVector(new int[]{1, 2}, true));
    // // assertEquals(RError.Message.LENGTH_GT_1.message, out.toString());
    // }

    @Test
    public void testSizeWarningWithCustomMessage() {
        arg.asIntegerVector().shouldBe(singleElement(), RError.Message.SEED_NOT_VALID_INT);
        testPipeline();

        cast(RDataFactory.createIntVector(new int[]{1, 2}, true));
        // assertEquals(RError.Message.SEED_NOT_VALID_INT.message, out.toString());
    }

    @Test
    public void testFindFirst() {
        arg.asIntegerVector().findFirst(0);
        testPipeline();

        assertEquals(0, cast(RNull.instance));
        assertEquals(1, cast(1));
        assertEquals(1, cast(RDataFactory.createIntVector(new int[]{1, 2}, true)));
        assertEquals(1, cast("1"));
        assertEquals(0, cast(RDataFactory.createIntVector(0)));
    }

    @Test
    public void testGenericVector1() {
        arg.asVector(true);
        testPipeline();

        assertEquals(RNull.instance, cast(RNull.instance));
    }

    @Test
    public void testFindFirstWithDefaultValue() {
        arg.asIntegerVector().findFirst(-1);
        testPipeline();

        assertEquals(1, cast(1));
        assertEquals(1, cast(RDataFactory.createIntVector(new int[]{1, 2}, true)));
        assertEquals(1, cast("1"));
        assertEquals(-1, cast(RDataFactory.createIntVector(0)));
    }

    @Test
    public void testFindFirstThrow() {
        arg.asIntegerVector().mustBe(notEmpty(), RError.Message.SEED_NOT_VALID_INT).findFirst();
        testPipeline();

        assertEquals(1, cast(1));
        assertEquals(1, cast(RDataFactory.createIntVector(new int[]{1, 2}, true)));
        assertEquals(1, cast("1"));
        try {
            cast(RDataFactory.createIntVector(0));
        } catch (IllegalArgumentException e) {
            assertEquals(RError.Message.SEED_NOT_VALID_INT.message, e.getMessage());
        }
    }

    @Test
    public void testNoLogicalNA() {
        arg.asLogicalVector().findFirst().notNA(RRuntime.LOGICAL_TRUE);
        testPipeline();

        assertEquals(RRuntime.LOGICAL_TRUE, cast(RRuntime.LOGICAL_NA));
    }

    @Test
    public void testNoIntegerNA() {
        arg.asIntegerVector().findFirst().notNA(1);
        testPipeline();

        assertEquals(1, cast(RRuntime.INT_NA));
    }

    @Test
    public void testNoDoubleNA() {
        arg.asDoubleVector().findFirst().notNA(1.1);
        testPipeline();

        assertEquals(1.1, cast(RRuntime.DOUBLE_NA));
    }

    @Test
    public void testNoStringNA() {
        arg.asStringVector().findFirst().notNA("A");
        testPipeline();

        assertEquals("A", cast(RRuntime.DOUBLE_NA));
    }

    @Test
    public void testSample0() {
        arg.asIntegerVector().shouldBe(singleElement()).findFirst(0);
        testPipeline();

        assertEquals(1, cast(RDataFactory.createIntVector(new int[]{1, 2}, true)));
        // assertEquals(String.format(RError.Message.INVALID_ARGUMENT.message, "x"),
        // out.toString());
    }

    @Test
    public void testSample1() {
        arg.asIntegerVector().mustBe(notEmpty(), RError.Message.LENGTH_ZERO);
        testPipeline();

        try {
            cast(RDataFactory.createIntVector(0));
        } catch (IllegalArgumentException e) {
            assertEquals(RError.Message.LENGTH_ZERO.message, e.getMessage());
        }
    }

    @Test
    public void testSample2() {
        arg.asIntegerVector().shouldBe(singleElement(), RError.Message.INVALID_USE, 1).mustBe(notEmpty(), RError.Message.ARGUMENT_EMPTY, 1);
        testPipeline();

        cast(RDataFactory.createIntVector(new int[]{1, 2}, true));
        // assertEquals(String.format(RError.Message.INVALID_USE.message, 1), out.toString());

        try {
            cast(RDataFactory.createIntVector(0));
        } catch (IllegalArgumentException e) {
            assertEquals(String.format(RError.Message.ARGUMENT_EMPTY.message, 1), e.getMessage());
        }
    }

    @Test
    public void testSample3() {
        arg.asLogicalVector().findFirst(RRuntime.LOGICAL_FALSE).map(toBoolean());
        testPipeline();

        assertEquals(Boolean.TRUE, cast(RDataFactory.createLogicalVector(new byte[]{RRuntime.LOGICAL_TRUE, RRuntime.LOGICAL_FALSE}, true)));
        assertEquals(Boolean.FALSE, cast(RDataFactory.createLogicalVector(0)));
    }

    @Test
    public void testSample4() {
        // the predicate is attached to the error message
        arg.mustBe(integerValue(), Message.SEED_NOT_VALID_INT).asIntegerVector();
        testPipeline();

        cast(RDataFactory.createIntVector(new int[]{1, 2}, true));

        try {
            cast("x");
        } catch (IllegalArgumentException e) {
            assertEquals(RError.Message.SEED_NOT_VALID_INT.message, e.getMessage());
        }
    }

    @Test
    public void testSample5() {
        arg.defaultError(RError.Message.INVALID_ARGUMENT,
                        "fill").mustBe(numericValue().or(logicalValue())).asVector().mustBe(singleElement()).findFirst().shouldBe(
                                        atomicLogicalValue().or(atomicIntegerValue().and(gt(0))), Message.NON_POSITIVE_FILL).mapIf(
                                                        atomicLogicalValue(), toBoolean());

        testPipeline();

        assertEquals(true, cast(RRuntime.LOGICAL_TRUE));
        assertEquals(10, cast(10));
        try {
            cast("xyz");
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals(String.format(RError.Message.INVALID_ARGUMENT.message, "fill"), e.getMessage());
        }
        try {
            cast(RDataFactory.createIntVector(new int[]{1, 2}, true));
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals(String.format(RError.Message.INVALID_ARGUMENT.message, "fill"), e.getMessage());
        }
        try {
            cast(RDataFactory.createIntVector(0));
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals(String.format(RError.Message.INVALID_ARGUMENT.message, "fill"), e.getMessage());
        }
        cast(-10); // warning
        // assertEquals(String.format(Message.NON_POSITIVE_FILL.message, "fill"), out.toString());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testSample6() {
        arg.mapNull(emptyStringVector()).mustBe(stringValue(), RError.Message.INVALID_ARGUMENT, "labels").asStringVector();
        testPipeline();

        assertEquals("abc", cast("abc"));

        Object asv = cast(RNull.instance);
        assertEquals(true, asv instanceof RAbstractStringVector);
        assertEquals(0, ((RAbstractStringVector) asv).getLength());

        RAbstractStringVector sv = RDataFactory.createStringVector(new String[]{"abc", "def"}, true);
        assertEquals(sv, cast(sv));

        sv = RDataFactory.createStringVector(0);
        asv = cast(sv);
        assertEquals(true, asv instanceof RAbstractStringVector);
        assertEquals(0, ((RAbstractStringVector) asv).getLength());

        try {
            cast(123);
        } catch (IllegalArgumentException e) {
            assertEquals(String.format(RError.Message.INVALID_ARGUMENT.message, "labels"), e.getMessage());
        }

        try {
            cast(RNull.instance);
        } catch (IllegalArgumentException e) {
            assertEquals(String.format(RError.Message.INVALID_ARGUMENT.message, "labels"), e.getMessage());
        }
    }

    @Test
    public void testSample7() {
        arg.asIntegerVector().findFirst().mustBe(gte(2).and(lte(5)).not());

        testPipeline(true);

    }

    @Test
    public void testSample8() {
        arg.allowNull().asLogicalVector().findFirst(RRuntime.LOGICAL_TRUE).mustBe(logicalTrue(), RError.Message.NYI, "non-blocking mode not supported").map(toBoolean());
        cast(RNull.instance);
    }

    @Test
    public void testSample9() {
        arg.mapIf(instanceOf(RList.class).not(), nullConstant());

        RList list = RDataFactory.createList();
        assertEquals(list, cast(list));
        assertEquals(RNull.instance, cast("abc"));
    }

    @Test
    public void testSample10() {
        arg.mapIf(instanceOf(RList.class), nullConstant());

        RList list = RDataFactory.createList();
        assertEquals(RNull.instance, cast(list));
        assertEquals("abc", cast("abc"));
    }

    //@formatter:off
    @Test
    public void testSample11() {
        arg.
            mapIf(instanceOf(RList.class).not(),
               chain(asLogicalVector()).
                  with(findFirst().logicalElement()).
                  with(notNA()).
                  with(map(toBoolean())).
                  with(mustBe(instanceOf(Boolean.class))).
                  with(shouldBe(instanceOf(Object.class))).
                  end());

        RList list = RDataFactory.createList();
        assertEquals(list, cast(list));
        assertEquals(true, cast(1));
        try {
            cast(RRuntime.INT_NA);
            fail();
        } catch (IllegalArgumentException e) {
            // ok
        }
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
    public void testSample12() {
        arg.alias(nonListToBoolean());

        RList list = RDataFactory.createList();
        assertEquals(list, cast(list));
        assertEquals(true, cast(1));
    }

    //@formatter:on

    @Test
    public void testSample14() {
        arg.allowNull().mustBe(stringValue()).asStringVector().findFirst();

        assertEquals("abc", cast("abc"));
        assertEquals("abc", cast(RDataFactory.createStringVector(new String[]{"abc", "xyz"}, true)));
        assertEquals(RNull.instance, cast(RNull.instance));
    }

    @Test
    public void testSample15() {
        // arg.asIntegerVector().mustBe(Predef.notEmpty());

        arg.mustBe(instanceOf(RAbstractStringVector.class).and(singleElement()).and(not(elementAt(0, RRuntime.STRING_NA))));

        cast(RDataFactory.createStringVector(new String[]{"abc"}, true));
        try {
            cast(RDataFactory.createStringVector(new String[]{"abc", "xyz"}, true));
        } catch (IllegalArgumentException e) {
            // ok
        }
        try {
            cast(RRuntime.STRING_NA);
        } catch (IllegalArgumentException e) {
            // ok
        }
    }

    private static String argType(Object arg) {
        return arg.getClass().getSimpleName();
    }

    private String argMsg(Object arg) {
        return "'data' must be of a vector type, was " + argType(arg);
    }

    @Test
    public void testSample16() {
        Function<Object, Object> argMsg = this::argMsg;
        arg.allowNull().shouldBe(stringValue(), RError.Message.GENERIC, argMsg);

        cast(RNull.instance);
    }

    @Test
    public void testSample17() {
        arg.asDoubleVector().findFirst().mapIf(doubleNA().not().and(not(isFractional())), doubleToInt());

        Object r = cast(RRuntime.STRING_NA);
        assertTrue(r instanceof Double);
        assertTrue(RRuntime.isNA((double) r));
        assertEquals(42, cast("42"));
        assertEquals(42.2, cast("42.2"));
    }

    @Test
    public void testSample18() {
        arg.asDoubleVector(true, true, true).mustBe(squareMatrix());

        RIntVector vec = RDataFactory.createIntVector(new int[]{0, 1, 2, 3}, true, new int[]{2, 2});
        Object res = cast(vec);
        assertTrue(res instanceof RAbstractDoubleVector);
        RAbstractDoubleVector dvec = (RAbstractDoubleVector) res;
        assertNotNull(dvec.getDimensions());
        assertEquals(2, dvec.getDimensions().length);
        assertEquals(2, dvec.getDimensions()[0]);
        assertEquals(2, dvec.getDimensions()[1]);
    }

    @Test
    public void testSample19() {
        arg.asDoubleVector(true, true, true).mustBe(dimGt(1, 0));

        RIntVector vec = RDataFactory.createIntVector(new int[]{0, 1, 2, 3}, true, new int[]{2, 2});
        cast(vec);
    }

    @Test
    public void testSample20() {
        arg.allowNull().mapIf(Predef.integerValue(), asIntegerVector(), asStringVector(true, false, false));
        RDoubleVector vec = RDataFactory.createDoubleVector(new double[]{0, 1, 2, 3}, true);

        Object res = cast(vec);
        assertTrue(res instanceof RAbstractStringVector);
    }

    @Test
    public void testSample21() {
        arg.mustBe(numericValue()).asVector().mustBe(singleElement()).findFirst().shouldBe(
                        instanceOf(Byte.class).or(instanceOf(Integer.class).and(gt0())),
                        Message.NON_POSITIVE_FILL).mapIf(atomicLogicalValue(), asBoolean(), asInteger());
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

    class RBuiltinRootNode extends RootNode {

        @Child private RBuiltinNode builtinNode;

        RBuiltinRootNode(RBuiltinNode builtinNode) {
            super(TruffleLanguage.class, null, null);
            this.builtinNode = builtinNode;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            return builtinNode.execute(frame);
        }
    }

    private Object cast(Object a) {
        CastNode argCastNode = cb.getCasts()[0];
        NodeHandle<CastNode> argCastNodeHandle = TestUtilities.createHandle(argCastNode, (node, args) -> node.execute(args[0]));
        return argCastNodeHandle.call(a);
    }

    private void testPipeline() {
        testPipeline(true);
    }

    private void testPipeline(@SuppressWarnings("unused") boolean positiveMustNotBeEmpty) {
        CastNodeSampler<CastNode> sampler = CastNodeSampler.createSampler(cb.getCasts()[0]);
        System.out.println(sampler.resultTypes());
        Samples<?> samples = sampler.collectSamples();
        //
        // if (positiveMustNotBeEmpty) {
        // Assert.assertFalse(samples.positiveSamples().isEmpty());
        // }
        //
        // testPipeline(samples);
    }

    @SuppressWarnings("unused")
    private void testPipeline(Samples<?> samples) {

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
}
