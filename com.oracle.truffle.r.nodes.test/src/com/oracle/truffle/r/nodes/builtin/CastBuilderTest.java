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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.StringWriter;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.oracle.truffle.r.nodes.builtin.CastBuilder.FinalPhaseBuilder;
import com.oracle.truffle.r.nodes.builtin.CastBuilder.InitialPhaseBuilder;
import com.oracle.truffle.r.nodes.builtin.TypePredicates.ReflectivePredicate;
import com.oracle.truffle.r.nodes.test.TestUtilities;
import com.oracle.truffle.r.nodes.test.TestUtilities.NodeHandle;
import com.oracle.truffle.r.nodes.unary.CastNode;
import com.oracle.truffle.r.runtime.MessagePredicate;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;

public class CastBuilderTest {

    private CastBuilder cb;
    private StringWriter out;

    @Before
    public void setUp() {
        cb = new CastBuilder(null);
        out = new StringWriter();
        cb.output(out);
    }

    @After
    public void tearDown() {
        cb = null;
        out = null;
    }

    @Test
    public void testRequire() {
        cb.arg(0).require((n, x) -> x != RNull.instance, (n, x) -> null, (n, x) -> {
            throw new IllegalArgumentException("E");
        });

        assertEquals("A", cast("A"));
        try {
            cast(RNull.instance);
        } catch (IllegalArgumentException e) {
            assertEquals("E", e.getMessage());
        }
    }

    @Test
    public void testError() {
        cb.arg(0).error(x -> x instanceof String, RError.Message.DLL_LOAD_ERROR, CastBuilder.ARG, "123");

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
        cb.arg(0).error(MessagePredicate.SEED_MUST_BE_INT);

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
        cb.arg(0).warning(x -> x instanceof String, RError.Message.DLL_LOAD_ERROR, CastBuilder.ARG, "123");

        assertEquals("A", cast("A"));
        assertEquals(Boolean.FALSE, cast(Boolean.FALSE));
        assertEquals("unable to load shared object 'false'\n  123", out.toString());
    }

    @Test
    public void testWarningWithAttachedPredicate() {
        cb.arg(0).warning(MessagePredicate.SEED_MUST_BE_INT);

        RAbstractIntVector v = RDataFactory.createIntVectorFromScalar(1);
        assertEquals(v, cast(v));
        assertEquals(Boolean.FALSE, cast(Boolean.FALSE));
        assertEquals(RError.Message.SEED_NOT_VALID_INT.message, out.toString());
    }

    @Test
    public void testDefaultError() {
        cb.arg(0, "arg0").error(x -> false);

        try {
            cast(Boolean.FALSE);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals(String.format(RError.Message.INVALID_ARGUMENT.message, "arg0"), e.getMessage());
        }
    }

    @Test
    public void testIsInteger() {
        InitialPhaseBuilder<RAbstractIntVector> pipeline = cb.arg(0).isInteger(RError.Message.SEED_NOT_VALID_INT);
        Set<Class<?>> contextTypes = pipeline.state().contextTypes();
        assertEquals(1, contextTypes.size());
        assertEquals(true, contextTypes.contains(RAbstractIntVector.class));

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
    public void testIsNumeric() {
        cb.arg(0).isNumeric(RError.Message.SEED_NOT_VALID_INT);

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
        cb.arg(0).asInteger();

        assertEquals(1, cast(1));
        assertEquals(0, cast(RRuntime.LOGICAL_FALSE));
        assertEquals(1, cast(1.3d));
        RComplex c = RDataFactory.createComplex(1, 0);
        assertEquals(1, cast(c));
    }

    @Test
    public void testAsDouble() {
        cb.arg(0).asDouble();

        assertEquals(1.2, cast(1.2));
    }

    @Test
    public void testAsLogical() {
        cb.arg(0).asLogical();

        assertEquals(RRuntime.LOGICAL_TRUE, cast(1.2));
    }

    @Test
    public void testAsString() {
        cb.arg(0).asString();

        assertEquals("1.2", cast(1.2));
    }

    @Test
    public void testEmptyError() {
        cb.arg(0).asInteger().emptyError();

        try {
            cast(RDataFactory.createIntVector(0));
        } catch (IllegalArgumentException e) {
            assertEquals(RError.Message.LENGTH_ZERO.message, e.getMessage());
        }
    }

    @Test
    public void testEmptyErrorWithCustomMessage() {
        cb.arg(0).asInteger().emptyError(RError.Message.SEED_NOT_VALID_INT);

        try {
            cast(RDataFactory.createIntVector(0));
        } catch (IllegalArgumentException e) {
            assertEquals(RError.Message.SEED_NOT_VALID_INT.message, e.getMessage());
        }
    }

    @Test
    public void testSizeWarning() {
        cb.arg(0).asInteger().sizeWarning();

        cast(RDataFactory.createIntVector(new int[]{1, 2}, true));
        assertEquals(RError.Message.LENGTH_GT_1.message, out.toString());
    }

    @Test
    public void testSizeWarningWithCustomMessage() {
        cb.arg(0).asInteger().sizeWarning(RError.Message.SEED_NOT_VALID_INT);

        cast(RDataFactory.createIntVector(new int[]{1, 2}, true));
        assertEquals(RError.Message.SEED_NOT_VALID_INT.message, out.toString());
    }

    @Test
    public void testFindFirst() {
        cb.arg(0).asInteger().findFirst();

        assertEquals(1, cast(1));
        assertEquals(1, cast(RDataFactory.createIntVector(new int[]{1, 2}, true)));
        assertEquals(1, cast("1"));
        assertEquals(RNull.instance, cast(RDataFactory.createIntVector(0)));
    }

    @Test
    public void testIsPresent() {
        cb.arg(0).asInteger().findFirst().isPresent((n, x) -> 2 * x, n -> -1);

        assertEquals(2, cast(1));
        assertEquals(2, cast(RDataFactory.createIntVector(new int[]{1, 2}, true)));
        assertEquals(2, cast("1"));
        assertEquals(-1, cast(RDataFactory.createIntVector(0)));
    }

    @Test
    public void testOrElse() {
        cb.arg(0).asInteger().findFirst().orElse(-1);

        assertEquals(1, cast(1));
        assertEquals(1, cast(RDataFactory.createIntVector(new int[]{1, 2}, true)));
        assertEquals(1, cast("1"));
        assertEquals(-1, cast(RDataFactory.createIntVector(0)));
    }

    @Test
    public void testOrElseThrow() {
        cb.arg(0).asInteger().findFirst().orElseThrow(RError.Message.SEED_NOT_VALID_INT);

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
        cb.arg(0).asLogical().findFirst().noNA().orElse(RRuntime.LOGICAL_TRUE);
        assertEquals(RRuntime.LOGICAL_TRUE, cast(RRuntime.LOGICAL_NA));
    }

    @Test
    public void testNoIntegerNA() {
        cb.arg(0).asInteger().findFirst().noNA().orElse(1);
        assertEquals(1, cast(RRuntime.INT_NA));
    }

    @Test
    public void testNoDoubleNA() {
        cb.arg(0).asDouble().findFirst().noNA().orElse(1.1);
        assertEquals(1.1, cast(RRuntime.DOUBLE_NA));
    }

    @Test
    public void testNoStringNA() {
        cb.arg(0).asString().findFirst().noNA().orElse("A");
        assertEquals("A", cast(RRuntime.DOUBLE_NA));
    }

    @Test
    public void testSample0() {
        cb.arg(0).asInteger().sizeWarning().findFirst().orElse(0);

        assertEquals(1, cast(RDataFactory.createIntVector(new int[]{1, 2}, true)));
        assertEquals(RError.Message.LENGTH_GT_1.message, out.toString());
    }

    @Test
    public void testSample1() {
        cb.arg(0).asInteger().emptyError();

        try {
            cast(RDataFactory.createIntVector(0));
        } catch (IllegalArgumentException e) {
            assertEquals(RError.Message.LENGTH_ZERO.message, e.getMessage());
        }
    }

    @Test
    public void testSample2() {
        cb.arg(0).asInteger().sizeWarning(RError.Message.INVALID_USE, 1).emptyError(RError.Message.ARGUMENT_EMPTY, 1);

        cast(RDataFactory.createIntVector(new int[]{1, 2}, true));
        assertEquals(String.format(RError.Message.INVALID_USE.message, 1), out.toString());

        try {
            cast(RDataFactory.createIntVector(0));
        } catch (IllegalArgumentException e) {
            assertEquals(String.format(RError.Message.ARGUMENT_EMPTY.message, 1), e.getMessage());
        }
    }

    @Test
    public void testSample3() {
        cb.arg(0).asLogical().findFirstBoolean().orElse(false);

        assertEquals(Boolean.TRUE, cast(RDataFactory.createLogicalVector(new byte[]{RRuntime.LOGICAL_TRUE, RRuntime.LOGICAL_FALSE}, true)));
        assertEquals(Boolean.FALSE, cast(RDataFactory.createLogicalVector(0)));
    }

    @Test
    public void testSample4() {
        // the predicate is attached to the error message
        cb.arg(0).error(MessagePredicate.SEED_MUST_BE_INT).asInteger();

        cast(RDataFactory.createIntVector(new int[]{1, 2}, true));

        try {
            cast("x");
        } catch (IllegalArgumentException e) {
            assertEquals(RError.Message.SEED_NOT_VALID_INT.message, e.getMessage());
        }
    }

    @Test
    public void testSample5() {
        FinalPhaseBuilder<Object> pipeline = cb.arg(0).defaultError(RError.Message.INVALID_ARGUMENT, "fill").
                        is(IS_NUMERIC.or(IS_LOGICAL)).
                        asVector().
                        sizeError().
                        findFirst().
                        orElseThrow().
                        warning(MessagePredicate.FILL_SHOULD_BE_POSITIVE);
        // mapIf(IS_SCALAR_LOGICAL, x -> RRuntime.fromLogical(x));

        Set<Class<?>> contextTypes = pipeline.state().contextTypes();
        assertEquals(3, contextTypes.size());
        assertEquals(true, contextTypes.contains(RAbstractIntVector.class));
        assertEquals(true, contextTypes.contains(RAbstractDoubleVector.class));
        assertEquals(true, contextTypes.contains(RAbstractComplexVector.class));
        assertEquals(true, contextTypes.contains(RAbstractLogicalVector.class));

        assertEquals(true, cast(RRuntime.LOGICAL_TRUE));
        assertEquals(10, cast(10));
        try {
            cast("xyz");
        } catch (IllegalArgumentException e) {
            assertEquals(String.format(RError.Message.INVALID_ARGUMENT.message, "fill"), e.getMessage());
        }
        try {
            cast(RDataFactory.createIntVector(new int[]{1, 2}, true));
        } catch (IllegalArgumentException e) {
            assertEquals(String.format(RError.Message.INVALID_ARGUMENT.message, "fill"), e.getMessage());
        }
        try {
            cast(RDataFactory.createIntVector(0));
        } catch (IllegalArgumentException e) {
            assertEquals(String.format(RError.Message.INVALID_ARGUMENT.message, "fill"), e.getMessage());
        }
        cast(-10); // warning
        assertEquals(RError.Message.NON_POSITIVE_FILL.message, out.toString());
    }

    @Test
    public void testSample6() {
        cb.arg(0).map(x -> x == RNull.instance ? RDataFactory.createStringVector(0) : x).
                        isString(RError.Message.INVALID_ARGUMENT, "labels").
                        asString();

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

    private Object cast(Object arg) {
        CastNode argCastNode = cb.getCasts()[0];
        NodeHandle<CastNode> argCastNodeHandle = TestUtilities.createHandle(argCastNode, (node, args) -> node.execute(args[0]));
        return argCastNodeHandle.call(arg);
    }
}
