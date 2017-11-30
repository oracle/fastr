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
package com.oracle.truffle.r.nodes.test;

import static com.oracle.truffle.r.nodes.test.TestUtilities.copy;
import static com.oracle.truffle.r.nodes.test.TestUtilities.createHandle;
import static com.oracle.truffle.r.runtime.data.RDataFactory.createEmptyComplexVector;
import static com.oracle.truffle.r.runtime.data.RDataFactory.createEmptyDoubleVector;
import static com.oracle.truffle.r.runtime.data.RDataFactory.createEmptyIntVector;
import static com.oracle.truffle.r.runtime.data.RDataFactory.createEmptyLogicalVector;
import static com.oracle.truffle.r.runtime.data.RDataFactory.createEmptyRawVector;
import static com.oracle.truffle.r.runtime.data.RDataFactory.createEmptyStringVector;
import static com.oracle.truffle.r.runtime.data.RDataFactory.createRaw;
import static com.oracle.truffle.r.runtime.data.RDataFactory.createRawVector;
import static com.oracle.truffle.r.runtime.data.RDataFactory.createStringVector;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeThat;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.internal.AssumptionViolatedException;
import org.junit.runner.RunWith;

import com.oracle.truffle.r.nodes.binary.BinaryBooleanNode;
import com.oracle.truffle.r.nodes.test.TestUtilities.NodeHandle;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RRaw;
import com.oracle.truffle.r.runtime.data.RScalarVector;
import com.oracle.truffle.r.runtime.data.RShareable;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.ops.BinaryCompare;
import com.oracle.truffle.r.runtime.ops.BinaryLogic;
import com.oracle.truffle.r.runtime.ops.BooleanOperationFactory;

/**
 * This test verifies white box assumptions for the arithmetic node. Please note that this node
 * should NOT verify correctness. This is done by the integration test suite.
 */
@RunWith(Theories.class)
public class BinaryBooleanNodeTest extends BinaryVectorTest {
    @Test
    public void dummy() {
        // to make sure this file is recognized as a test
    }

    @DataPoint public static final RAbstractVector EMPTY_STRING = createEmptyStringVector();
    @DataPoint public static final RAbstractVector EMPTY_RAW = createEmptyRawVector();

    @DataPoint public static final RAbstractVector RAW = createRaw((byte) 0xA);
    @DataPoint public static final RAbstractVector RAW2 = createRawVector(new byte[]{0xF, 0x3, 0xF});

    @DataPoint public static final RAbstractVector STRING1 = createStringVector("42");
    @DataPoint public static final RAbstractVector STRING2 = createStringVector(new String[]{"43", "42", "41", "40"}, true);
    @DataPoint public static final RAbstractVector STRING_NOT_COMPLETE = createStringVector(new String[]{"43", RRuntime.STRING_NA, "41", "40"}, false);

    @DataPoints public static final BooleanOperationFactory[] LOGIC = BinaryLogic.ALL;
    @DataPoints public static final BooleanOperationFactory[] COMPARE = BinaryCompare.ALL;

    @Theory
    public void testScalarUnboxing(BooleanOperationFactory factory, RScalarVector aOrig, RAbstractVector bOrig) {
        execInContext(() -> {
            RAbstractVector a = copy(aOrig);
            RAbstractVector b = copy(bOrig);
            // unboxing cannot work if length is 1
            assumeThat(b.getLength(), is(1));

            // if the right side is shareable these should be prioritized
            assumeThat(b, is(not(instanceOf(RShareable.class))));

            assumeArithmeticCompatible(factory, a, b);
            Object result = executeArithmetic(factory, a, b);
            Assert.assertTrue(isPrimitive(result));
            return null;
        });
    }

    @Theory
    public void testVectorResult(BooleanOperationFactory factory, RAbstractVector aOrig, RAbstractVector bOrig) {
        execInContext(() -> {
            RAbstractVector a = copy(aOrig);
            RAbstractVector b = copy(bOrig);
            assumeThat(a, is(not(instanceOf(RScalarVector.class))));
            assumeThat(b, is(not(instanceOf(RScalarVector.class))));
            assumeArithmeticCompatible(factory, a, b);

            Object result = executeArithmetic(factory, a, b);
            Assert.assertFalse(isPrimitive(result));
            assertLengthAndType(factory, a, b, (RAbstractVector) result);

            assumeThat(b, is(not(instanceOf(RScalarVector.class))));
            result = executeArithmetic(factory, b, a);
            assertLengthAndType(factory, a, b, (RAbstractVector) result);
            return null;
        });
    }

    @Theory
    public void testSharing(BooleanOperationFactory factory, RAbstractVector aOrig, RAbstractVector bOrig) {
        execInContext(() -> {
            RAbstractVector a = copy(aOrig);
            RAbstractVector b = copy(bOrig);
            assumeArithmeticCompatible(factory, aOrig, bOrig);

            // not part of this test, see #testEmptyArrays
            assumeThat(a.getLength(), is(not(0)));
            assumeThat(b.getLength(), is(not(0)));

            // sharing does not work if a is a scalar vector
            assumeThat(a, is(not(instanceOf(RScalarVector.class))));

            RType resultType = getResultType(factory, a, b);
            int maxLength = Integer.max(a.getLength(), b.getLength());
            RAbstractVector sharedResult = null;
            if (a.getLength() == maxLength && isShareable(a, resultType)) {
                sharedResult = a;
            }
            if (sharedResult == null && b.getLength() == maxLength && isShareable(b, resultType)) {
                sharedResult = b;
            }

            Object result = executeArithmetic(factory, a, b);

            if (sharedResult == null) {
                Assert.assertNotSame(a, result);
                Assert.assertNotSame(b, result);
            } else {
                Assert.assertSame(sharedResult, result);
            }
            return null;
        });
    }

    private static boolean isShareable(RAbstractVector a, RType resultType) {
        if (a.getRType() != resultType) {
            // needs cast -> not shareable
            return false;
        }

        if (a instanceof RShareable) {
            if (((RShareable) a).isTemporary()) {
                return true;
            }
        }
        return false;
    }

    private static void assertLengthAndType(BooleanOperationFactory factory, RAbstractVector a, RAbstractVector b, RAbstractVector resultVector) {
        int expectedLength = Math.max(a.getLength(), b.getLength());
        if (a.getLength() == 0 || b.getLength() == 0) {
            expectedLength = 0;
        }
        assertThat(resultVector.getLength(), is(equalTo(expectedLength)));
        RType resultType = getResultType(factory, a, b);
        assertThat(resultVector.getRType(), is(equalTo(resultType)));
    }

    private static RType getResultType(BooleanOperationFactory factory, RAbstractVector a, RAbstractVector b) {
        RType resultType;
        if ((factory == BinaryLogic.AND || factory == BinaryLogic.OR) && getArgumentType(a, b) == RType.Raw) {
            resultType = RType.Raw;
        } else {
            resultType = RType.Logical;
        }
        return resultType;
    }

    private static RType getArgumentType(RAbstractVector a, RAbstractVector b) {
        return RType.maxPrecedence(a.getRType(), b.getRType());
    }

    @Theory
    public void testEmptyArrays(BooleanOperationFactory factory, RAbstractVector originalVector) {
        execInContext(() -> {
            RAbstractVector vector = copy(originalVector);
            assumeArithmeticCompatible(factory, vector, createEmptyLogicalVector());
            assumeArithmeticCompatible(factory, vector, createEmptyIntVector());
            assumeArithmeticCompatible(factory, vector, createEmptyDoubleVector());
            assumeArithmeticCompatible(factory, vector, createEmptyComplexVector());

            testEmptyArray(factory, vector, createEmptyLogicalVector());
            testEmptyArray(factory, vector, createEmptyIntVector());
            testEmptyArray(factory, vector, createEmptyDoubleVector());
            testEmptyArray(factory, vector, createEmptyComplexVector());
            return null;
        });
    }

    @Theory
    public void testRNullConstantResult(BooleanOperationFactory factory, RAbstractVector originalVector) {
        execInContext(() -> {
            RAbstractVector vector = copy(originalVector);
            assumeFalse(isLogicOp(factory));
            assumeFalse(vector.getRType() == RType.Raw);
            assertThat(executeArithmetic(factory, vector, RNull.instance), isEmptyVectorOf(RType.Logical));
            assertThat(executeArithmetic(factory, RNull.instance, vector), isEmptyVectorOf(RType.Logical));
            return null;
        });
    }

    @Theory
    public void testBothNull(BooleanOperationFactory factory) {
        execInContext(() -> {
            assumeFalse(isLogicOp(factory));
            assertThat(executeArithmetic(factory, RNull.instance, RNull.instance), isEmptyVectorOf(RType.Logical));
            return null;
        });
    }

    @Theory
    public void testCompleteness(BooleanOperationFactory factory, RAbstractVector aOrig, RAbstractVector bOrig) {
        execInContext(() -> {
            RAbstractVector a = copy(aOrig);
            RAbstractVector b = copy(bOrig);
            assumeArithmeticCompatible(factory, a, b);

            Object result = executeArithmetic(factory, a, b);

            boolean resultComplete = isPrimitive(result) ? true : ((RAbstractVector) result).isComplete();

            if (a.getLength() == 0 || b.getLength() == 0) {
                Assert.assertTrue(resultComplete);
            } else {
                boolean expectedComplete = a.isComplete() && b.isComplete();
                Assert.assertEquals(expectedComplete, resultComplete);
            }
            return null;
        });
    }

    @Theory
    public void testGeneric(BooleanOperationFactory factory) {
        execInContext(() -> {
            // this should trigger the generic case
            for (RAbstractVector vector : ALL_VECTORS) {
                try {
                    assumeArithmeticCompatible(factory, vector, vector);
                } catch (AssumptionViolatedException e) {
                    continue;
                }
                executeArithmetic(factory, copy(vector), copy(vector));
            }
            return null;
        });
    }

    private static void assumeArithmeticCompatible(BooleanOperationFactory factory, RAbstractVector a, RAbstractVector b) {
        RType argumentType = getArgumentType(a, b);

        assumeFalse(isLogicOp(factory) && getResultType(factory, a, b) == RType.Raw && (argumentType != RType.Raw));
        assumeFalse(!isLogicOp(factory) && (argumentType == RType.Complex));
        assumeFalse(isLogicOp(factory) && (argumentType != RType.Raw && (a.getRType() == RType.Raw || b.getRType() == RType.Raw)));
        assumeFalse(!isLogicOp(factory) && (a.getRType() == RType.Raw || b.getRType() == RType.Raw));
        assumeFalse(isLogicOp(factory) && (a.getRType() == RType.Character || b.getRType() == RType.Character));

    }

    private static boolean isLogicOp(BooleanOperationFactory factory) {
        return factory == BinaryLogic.AND || factory == BinaryLogic.OR;
    }

    private void testEmptyArray(BooleanOperationFactory factory, RAbstractVector vector, RAbstractVector empty) {
        assertThat(executeArithmetic(factory, vector, empty), isEmptyVectorOf(getResultType(factory, vector, empty)));
        assertThat(executeArithmetic(factory, empty, vector), isEmptyVectorOf(getResultType(factory, empty, vector)));
        assertThat(executeArithmetic(factory, empty, empty), isEmptyVectorOf(getResultType(factory, empty, empty)));
    }

    private static boolean isPrimitive(Object result) {
        return result instanceof Integer || result instanceof Double || result instanceof Byte || result instanceof RComplex || result instanceof RRaw;
    }

    private NodeHandle<BinaryBooleanNode> handle;
    private BooleanOperationFactory currentFactory;

    @Before
    public void setUp() {
        handle = null;
    }

    @After
    public void tearDown() {
        handle = null;
    }

    private Object executeArithmetic(BooleanOperationFactory factory, Object left, Object right) {
        if (handle == null || this.currentFactory != factory) {
            handle = create(factory);
            this.currentFactory = factory;
        }
        return handle.call(left, right);
    }

    private static NodeHandle<BinaryBooleanNode> create(BooleanOperationFactory factory) {
        return createHandle(BinaryBooleanNode.create(factory),
                        (node, args) -> node.execute(null, args[0], args[1]));
    }
}
