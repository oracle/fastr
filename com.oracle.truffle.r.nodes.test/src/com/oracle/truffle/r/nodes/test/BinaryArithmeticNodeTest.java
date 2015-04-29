/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.nodes.test.TestUtilities.*;
import static com.oracle.truffle.r.runtime.data.RDataFactory.*;
import static com.oracle.truffle.r.runtime.ops.BinaryArithmetic.*;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.*;
import static org.junit.Assume.*;

import java.util.*;

import org.junit.*;
import org.junit.experimental.theories.*;
import org.junit.runner.*;

import com.oracle.truffle.r.nodes.binary.*;
import com.oracle.truffle.r.nodes.test.TestUtilities.NodeHandle;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.RAttributes.RAttribute;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.ops.*;

/**
 * This test verifies white box assumptions for the arithmetic node. Please note that this node
 * should NOT verify correctness. This is done by the integration test suite.
 */
@RunWith(Theories.class)
public class BinaryArithmeticNodeTest extends ArithmeticTest {

    @DataPoints public static final BinaryArithmeticFactory[] BINARY = ALL;

    @Theory
    public void testScalarUnboxing(BinaryArithmeticFactory factory, RScalarVector a, RAbstractVector b) {
        // unboxing cannot work if length is 1
        assumeThat(b.getLength(), is(1));

        // if the right side is shareable these should be prioritized
        assumeThat(b, is(not(instanceOf(RShareable.class))));

        assumeArithmeticCompatible(factory, a, b);
        Object result = executeArithmetic(factory, a, b);
        Assert.assertTrue(isPrimitive(result));
    }

    @Theory
    public void testVectorResult(BinaryArithmeticFactory factory, RAbstractVector a, RAbstractVector b) {
        assumeThat(a, is(not(instanceOf(RScalarVector.class))));
        assumeThat(b, is(not(instanceOf(RScalarVector.class))));
        assumeArithmeticCompatible(factory, a, b);

        Object result = executeArithmetic(factory, a, b);
        Assert.assertFalse(isPrimitive(result));
        assertLengthAndType(factory, a, b, (RAbstractVector) result);

        assumeThat(b, is(not(instanceOf(RScalarVector.class))));
        result = executeArithmetic(factory, b, a);
        assertLengthAndType(factory, a, b, (RAbstractVector) result);
    }

    @Theory
    public void testSharing(BinaryArithmeticFactory factory, RAbstractVector a, RAbstractVector b) {
        assumeArithmeticCompatible(factory, a, b);

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

    private static void assertLengthAndType(BinaryArithmeticFactory factory, RAbstractVector a, RAbstractVector b, RAbstractVector resultVector) {
        int expectedLength = Math.max(a.getLength(), b.getLength());
        if (a.getLength() == 0 || b.getLength() == 0) {
            expectedLength = 0;
        }
        assertThat(resultVector.getLength(), is(equalTo(expectedLength)));
        RType resultType = getResultType(factory, a, b);
        assertThat(resultVector.getRType(), is(equalTo(resultType)));
    }

    private static RType getResultType(BinaryArithmeticFactory factory, RAbstractVector a, RAbstractVector b) {
        RType resultType = getArgumentType(a, b);
        if (!factory.create().isSupportsIntResult() && resultType == RType.Integer) {
            resultType = RType.Double;
        }
        return resultType;
    }

    @Theory
    public void testEmptyArrays(BinaryArithmeticFactory factory, RAbstractVector vector) {
        testEmptyArray(factory, vector, createEmptyLogicalVector());
        testEmptyArray(factory, vector, createEmptyIntVector());
        testEmptyArray(factory, vector, createEmptyDoubleVector());
        testEmptyArray(factory, vector, createEmptyComplexVector());

    }

    @Theory
    public void testRNullConstantResult(BinaryArithmeticFactory factory, RAbstractVector vector) {
        RAbstractVector result = vector.getRType() == RType.Complex ? createEmptyComplexVector() : createEmptyDoubleVector();

        assertThat(executeArithmetic(factory, vector, RNull.instance), is(result));
        assertThat(executeArithmetic(factory, RNull.instance, vector), is(result));
    }

    @Theory
    public void testBothNull(BinaryArithmeticFactory factory) {
        assertThat(executeArithmetic(factory, RNull.instance, RNull.instance), is(createEmptyDoubleVector()));
    }

    @Theory
    public void testCompleteness(BinaryArithmeticFactory factory, RAbstractVector a, RAbstractVector b) {
        assumeArithmeticCompatible(factory, a, b);

        // disable division they might produce NA values by division with 0
        assumeFalse(factory == BinaryArithmetic.DIV);
        assumeFalse(factory == BinaryArithmetic.INTEGER_DIV);
        assumeFalse(factory == BinaryArithmetic.MOD);

        Object result = executeArithmetic(factory, a, b);

        boolean resultComplete = isPrimitive(result) ? true : ((RAbstractVector) result).isComplete();

        if (a.getLength() == 0 || b.getLength() == 0) {
            Assert.assertTrue(resultComplete);
        } else {
            boolean expectedComplete = a.isComplete() && b.isComplete();
            Assert.assertEquals(expectedComplete, resultComplete);
        }
    }

    @Theory
    public void testCopyAttributes(BinaryArithmeticFactory factory, RAbstractVector aOrig, RAbstractVector bOrig) {
        assumeArithmeticCompatible(factory, aOrig, bOrig);

        // we have to e careful not to change mutable vectors
        RAbstractVector a = aOrig.copy();
        RAbstractVector b = bOrig.copy();
        if (a instanceof RShareable) {
            ((RShareable) a).markNonTemporary();
        }
        if (b instanceof RShareable) {
            ((RShareable) b).markNonTemporary();
        }

        RVector aMaterialized = a.copy().materialize();
        RVector bMaterialized = b.copy().materialize();

        aMaterialized.setAttr("a", "a");
        bMaterialized.setAttr("b", "b");

        if (a.getLength() == 0 || b.getLength() == 0) {
            assertAttributes(executeArithmetic(factory, aMaterialized.copy(), bMaterialized.copy()));
            assertAttributes(executeArithmetic(factory, a, bMaterialized.copy()));
            assertAttributes(executeArithmetic(factory, aMaterialized.copy(), b));
        } else if (a.getLength() == b.getLength()) {
            assertAttributes(executeArithmetic(factory, aMaterialized.copy(), bMaterialized.copy()), "a", "b");
            assertAttributes(executeArithmetic(factory, a, bMaterialized.copy()), "b");
            assertAttributes(executeArithmetic(factory, aMaterialized.copy(), b), "a");
        } else if (a.getLength() > b.getLength()) {
            assertAttributes(executeArithmetic(factory, aMaterialized.copy(), bMaterialized.copy()), "a");
            assertAttributes(executeArithmetic(factory, a, bMaterialized.copy()));
            assertAttributes(executeArithmetic(factory, aMaterialized.copy(), b), "a");
        } else {
            assert a.getLength() < b.getLength();
            assertAttributes(executeArithmetic(factory, aMaterialized.copy(), bMaterialized.copy()), "b");
            assertAttributes(executeArithmetic(factory, a, bMaterialized.copy()), "b");
            assertAttributes(executeArithmetic(factory, aMaterialized.copy(), b));
        }
    }

    @Test
    public void testSequenceFolding() {
        assertFold(true, createIntSequence(1, 3, 10), createIntVectorFromScalar(5), ADD, SUBTRACT, MULTIPLY, INTEGER_DIV);
        assertFold(true, createIntVectorFromScalar(5), createIntSequence(1, 3, 10), ADD, MULTIPLY);
        assertFold(true, createIntSequence(1, 3, 10), createIntSequence(2, 5, 10), ADD, SUBTRACT);
        assertFold(false, createIntVectorFromScalar(5), createIntSequence(1, 3, 10), SUBTRACT, INTEGER_DIV, MOD);
        assertFold(false, createIntSequence(1, 3, 10), createIntSequence(2, 5, 5), ADD, SUBTRACT, MULTIPLY, INTEGER_DIV);

        assertFold(true, createDoubleSequence(1, 3, 10), createDoubleVectorFromScalar(5), ADD, SUBTRACT, MULTIPLY, INTEGER_DIV);
        assertFold(true, createDoubleVectorFromScalar(5), createDoubleSequence(1, 3, 10), ADD, MULTIPLY);
        assertFold(true, createDoubleSequence(1, 3, 10), createDoubleSequence(2, 5, 10), ADD, SUBTRACT);
        assertFold(false, createDoubleVectorFromScalar(5), createDoubleSequence(1, 3, 10), SUBTRACT, INTEGER_DIV, MOD);
        assertFold(false, createDoubleSequence(1, 3, 10), createDoubleSequence(2, 5, 5), ADD, SUBTRACT, MULTIPLY, INTEGER_DIV);
    }

    private static void assertAttributes(Object value, String... keys) {
        if (!(value instanceof RAbstractVector)) {
            Assert.assertEquals(0, keys.length);
            return;
        }

        RAbstractVector vector = (RAbstractVector) value;
        Set<String> expectedAttributes = new HashSet<>(Arrays.asList(keys));

        RAttributes attributes = vector.getAttributes();
        if (attributes == null) {
            Assert.assertEquals(0, keys.length);
            return;
        }
        Set<Object> foundAttributes = new HashSet<>();
        for (RAttribute attribute : attributes) {
            foundAttributes.add(attribute.getName());
            foundAttributes.add(attribute.getValue());
        }
        Assert.assertEquals(expectedAttributes, foundAttributes);
    }

    private static void assumeArithmeticCompatible(BinaryArithmeticFactory factory, RAbstractVector a, RAbstractVector b) {
        RType argumentType = getArgumentType(a, b);
        assumeTrue(argumentType.isNumeric());

        // TODO complex mod, div, min, max not yet implemented
        assumeFalse(factory == DIV && (argumentType == RType.Complex));
        assumeFalse(factory == INTEGER_DIV && (argumentType == RType.Complex));
        assumeFalse(factory == MOD && (argumentType == RType.Complex));
        assumeFalse(factory == MAX && (argumentType == RType.Complex));
        assumeFalse(factory == MIN && (argumentType == RType.Complex));
    }

    private void testEmptyArray(BinaryArithmeticFactory factory, RAbstractVector vector, RAbstractVector empty) {
        assertThat(executeArithmetic(factory, vector, empty), is(getResultType(factory, vector, empty).getEmpty()));
        assertThat(executeArithmetic(factory, empty, vector), is(getResultType(factory, empty, vector).getEmpty()));
        assertThat(executeArithmetic(factory, empty, empty), is(getResultType(factory, empty, empty).getEmpty()));
    }

    private static RType getArgumentType(RAbstractVector a, RAbstractVector b) {
        return RType.maxPrecedence(RType.Integer, RType.maxPrecedence(a.getRType(), b.getRType()));
    }

    private static boolean isPrimitive(Object result) {
        return result instanceof Integer || result instanceof Double || result instanceof Byte || result instanceof RComplex;
    }

    private void assertFold(boolean expectedFold, RAbstractVector left, RAbstractVector right, BinaryArithmeticFactory... arithmetics) {
        for (int i = 0; i < arithmetics.length; i++) {
            BinaryArithmeticFactory factory = arithmetics[i];
            Object result = executeArithmetic(factory, left, right);
            if (expectedFold) {
                assertThat("expected fold " + left + " <op> " + right, result instanceof RSequence);
            } else {
                assertThat("expected not fold" + left + " <op> " + right, !(result instanceof RSequence));
            }
        }
    }

    private NodeHandle<BinaryArithmeticNode> handle;
    private BinaryArithmeticFactory currentFactory;

    @Before
    public void setUp() {
        handle = null;
    }

    @After
    public void tearDown() {
        handle = null;
    }

    private Object executeArithmetic(BinaryArithmeticFactory factory, Object left, Object right) {
        if (handle == null || this.currentFactory != factory) {
            handle = create(factory);
            this.currentFactory = factory;
        }
        return handle.call(left, right);
    }

    private static NodeHandle<BinaryArithmeticNode> create(BinaryArithmeticFactory factory) {
        return createHandle(BinaryArithmeticNode.create(factory, null), //
                        (node, args) -> node.execute(args[0], args[1]));
    }

}
