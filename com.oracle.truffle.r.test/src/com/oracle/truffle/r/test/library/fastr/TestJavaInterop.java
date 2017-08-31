/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.test.library.fastr;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.oracle.truffle.r.nodes.builtin.base.printer.DoubleVectorPrinter;
import com.oracle.truffle.r.nodes.builtin.fastr.FastRInterop;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.test.TestBase;

public class TestJavaInterop extends TestBase {

    private static final String TEST_CLASS = TestClass.class.getName();
    private static final String CREATE_TRUFFLE_OBJECT = "to <- new.external(new.java.class('" + TEST_CLASS + "'));";

    @Before
    public void testInit() {
        FastRInterop.testingMode();
    }

    @Test
    public void testToByte() {
        assertEvalFastR("v <- as.external.byte(1L); v;", "1");
        assertEvalFastR("v <- as.external.byte(1.1); v;", "1");
        assertEvalFastR("v <- as.external.byte(as.raw(1)); v;", "1");
        assertEvalFastR("v <- as.external.byte(1.1); class(v);", "'" + RType.RInteropByte.getName() + "'");
        assertEvalFastR("v <- as.external.byte(1.1); typeof(v);", "'" + RType.RInteropByte.getName() + "'");
        assertEvalFastR("v <- as.external.byte(" + Byte.MAX_VALUE + "); v;", "" + Byte.MAX_VALUE);
        assertEvalFastR("v <- as.external.byte(" + Byte.MIN_VALUE + "); v;", "" + Byte.MIN_VALUE);
        assertEvalFastR("v <- as.external.byte(" + Integer.MAX_VALUE + "); v;", "" + new Integer(Integer.MAX_VALUE).byteValue());
        assertEvalFastR("v <- as.external.byte(" + Integer.MIN_VALUE + "); v;", "" + new Integer(Integer.MIN_VALUE).byteValue());
        assertEvalFastR("v <- as.external.byte(" + Double.MAX_VALUE + "); v;", "" + new Double(Double.MAX_VALUE).byteValue());
        assertEvalFastR("v <- as.external.byte(" + Double.MIN_VALUE + "); v;", "" + new Double(Double.MIN_VALUE).byteValue());
    }

    @Test
    public void testToFloat() {
        assertEvalFastR("v <- as.external.float(1L); v;", "1");
        assertEvalFastR("v <- as.external.float(1.1); v;", "1.1");
        assertEvalFastR("v <- as.external.float(as.raw(1)); v;", "1");
        assertEvalFastR("v <- as.external.float(1.1); class(v);", "'" + RType.RInteropFloat.getName() + "'");
        assertEvalFastR("v <- as.external.float(1.1); typeof(v);", "'" + RType.RInteropFloat.getName() + "'");
        assertEvalFastR("v <- as.external.float(1L); class(v);", "'" + RType.RInteropFloat.getName() + "'");
        assertEvalFastR("v <- as.external.float(1L); typeof(v);", "'" + RType.RInteropFloat.getName() + "'");
        assertEvalFastR("v <- as.external.float(" + Float.MAX_VALUE + "); v;", "" + Float.MAX_VALUE);
        assertEvalFastR("v <- as.external.float(" + Float.MIN_VALUE + "); v;", "" + (double) Float.MIN_VALUE);
        assertEvalFastR("v <- as.external.float(" + Double.MAX_VALUE + "); v;", "Inf");
        assertEvalFastR("v <- as.external.float(" + Double.MIN_VALUE + "); v;", "" + new Double(Double.MIN_VALUE).floatValue());
    }

    @Test
    public void testToLong() {
        assertEvalFastR("v <- as.external.long(1L); v;", "1");
        assertEvalFastR("v <- as.external.long(1.1); v;", "1");
        assertEvalFastR("v <- as.external.long(as.raw(1)); v;", "1");
        assertEvalFastR("v <- as.external.long(1.1); class(v);", "'" + RType.RInteropLong.getName() + "'");
        assertEvalFastR("v <- as.external.long(1.1); typeof(v);", "'" + RType.RInteropLong.getName() + "'");
        assertEvalFastR("v <- as.external.long(1L); class(v);", "'" + RType.RInteropLong.getName() + "'");
        assertEvalFastR("v <- as.external.long(1L); typeof(v);", "'" + RType.RInteropLong.getName() + "'");
        assertEvalFastR("v <- as.external.long(" + Integer.MAX_VALUE + "); v;", "" + Integer.MAX_VALUE);
        assertEvalFastR("v <- as.external.long(" + Integer.MIN_VALUE + "); v;", "" + Integer.MIN_VALUE);
        assertEvalFastR("v <- as.external.long(" + Double.MAX_VALUE + "); v;", "" + new Double(Double.MAX_VALUE).longValue());
        assertEvalFastR("v <- as.external.long(" + Double.MIN_VALUE + "); v;", "" + new Double(Double.MIN_VALUE).longValue());
    }

    @Test
    public void testToShort() {
        assertEvalFastR("v <- as.external.short(1L); v;", "1");
        assertEvalFastR("v <- as.external.short(1.1); v;", "1");
        assertEvalFastR("v <- as.external.short(as.raw(1)); v;", "1");
        assertEvalFastR("v <- as.external.short(1.1); class(v);", "'" + RType.RInteropShort.getName() + "'");
        assertEvalFastR("v <- as.external.short(1.1); typeof(v);", "'" + RType.RInteropShort.getName() + "'");
        assertEvalFastR("v <- as.external.short(1L); class(v);", "'" + RType.RInteropShort.getName() + "'");
        assertEvalFastR("v <- as.external.short(1L); typeof(v);", "'" + RType.RInteropShort.getName() + "'");
        assertEvalFastR("v <- as.external.short(" + Short.MAX_VALUE + "); v;", "" + Short.MAX_VALUE);
        assertEvalFastR("v <- as.external.short(" + Short.MIN_VALUE + "); v;", "" + Short.MIN_VALUE);
        assertEvalFastR("v <- as.external.short(" + Integer.MAX_VALUE + "); v;", "" + new Integer(Integer.MAX_VALUE).shortValue());
        assertEvalFastR("v <- as.external.short(" + Integer.MIN_VALUE + "); v;", "" + new Integer(Integer.MIN_VALUE).shortValue());
        assertEvalFastR("v <- as.external.short(" + Double.MAX_VALUE + "); v;", "" + new Double(Double.MAX_VALUE).shortValue());
        assertEvalFastR("v <- as.external.short(" + Double.MIN_VALUE + "); v;", "" + new Double(Double.MIN_VALUE).shortValue());
    }

    @Test
    public void testToChar() {
        assertEvalFastR("v <- as.external.char(97L); v;", "'a'");
        assertEvalFastR("v <- as.external.char(97.1); v;", "'a'");
        assertEvalFastR("v <- as.external.char(97.1, 1); v;", errorIn("as.external.char(97.1, 1)", "pos argument not allowed with a numeric value"));
        assertEvalFastR("v <- as.external.char(97L, 1); v;", errorIn("as.external.char(97L, 1)", "pos argument not allowed with a numeric value"));
        assertEvalFastR("v <- as.external.char('abc', 1); v;", "'b'");
        assertEvalFastR("v <- as.external.char('abc', 1.1); v;", "'b'");
        assertEvalFastR("v <- as.external.char(97.1); class(v);", "'" + RType.RInteropChar.getName() + "'");
        assertEvalFastR("v <- as.external.char(97.1); typeof(v);", "'" + RType.RInteropChar.getName() + "'");
        assertEvalFastR("v <- as.external.char(97L); class(v);", "'" + RType.RInteropChar.getName() + "'");
        assertEvalFastR("v <- as.external.char(97L); typeof(v);", "'" + RType.RInteropChar.getName() + "'");
        assertEvalFastR("v <- as.external.char('a'); v;", "'a'");
    }

    @Test
    public void testToArray() {
        assertEvalFastR("a <- as.java.array(1L); a;", getRValue(new int[]{1}));
        assertEvalFastR("a <- as.java.array(1L); java.class(a);", "'[I'");
        assertEvalFastR("a <- as.java.array(c(1L, 2L)); a;", getRValue(new int[]{1, 2}));
        assertEvalFastR("a <- as.java.array(1L,,T); a;", getRValue(new int[]{1}));
        assertEvalFastR("a <- as.java.array(c(1L, 2L),,T); a;", getRValue(new int[]{1, 2}));
        assertEvalFastR("a <- as.java.array(c(1L, 2L),'double',T); a;", getRValue(new double[]{1, 2}));

        assertEvalFastR("a <- as.java.array(1.1); a;", getRValue(new double[]{1.1}));
        assertEvalFastR("a <- as.java.array(1.1); java.class(a);", "'[D'");
        assertEvalFastR("a <- as.java.array(c(1.1, 1.2)); a;", getRValue(new double[]{1.1, 1.2}));
        assertEvalFastR("a <- as.java.array(1.1,,T); a;", getRValue(new double[]{1.1}));
        assertEvalFastR("a <- as.java.array(c(1.1, 1.2),,T); a;", getRValue(new double[]{1.1, 1.2}));
        assertEvalFastR("a <- as.java.array(c(1.1, 1.2),'double',T); a;", getRValue(new double[]{1.1, 1.2}));

        assertEvalFastR("a <- as.java.array(T); a;", getRValue(new boolean[]{true}));
        assertEvalFastR("a <- as.java.array(T); java.class(a);", "'[Z'");
        assertEvalFastR("a <- as.java.array(c(T, F)); a;", getRValue(new boolean[]{true, false}));
        assertEvalFastR("a <- as.java.array(T,,T); a;", getRValue(new boolean[]{true}));
        assertEvalFastR("a <- as.java.array(c(T, F),,T); a;", getRValue(new boolean[]{true, false}));
        assertEvalFastR("a <- as.java.array(c(T, F),'boolean',T); a;", getRValue(new boolean[]{true, false}));

        assertEvalFastR("a <- as.java.array('a'); a;", getRValue(new String[]{"a"}));
        assertEvalFastR("a <- as.java.array('a'); java.class(a);", "'[Ljava.lang.String;'");
        assertEvalFastR("a <- as.java.array(c('a', 'b')); a;", getRValue(new String[]{"a", "b"}));
        assertEvalFastR("a <- as.java.array('a',,T); a;", getRValue(new String[]{"a"}));
        assertEvalFastR("a <- as.java.array(c('a', 'b'),,T); a;", getRValue(new String[]{"a", "b"}));
        assertEvalFastR("a <- as.java.array(c('a', 'b'),'java.lang.String',T); a;", getRValue(new String[]{"a", "b"}));

        assertEvalFastR("a <- as.java.array(as.raw(1)); a", getRValue(new byte[]{1}));
        assertEvalFastR("a <- as.java.array(as.raw(1)); java.class(a);", "'[B'");
        assertEvalFastR("a <- as.java.array(as.raw(1)); length(a);", "1");
        assertEvalFastR("a <- as.java.array(as.raw(c(1, 2, 3))); length(a);", "3");
        assertEvalFastR("a <- as.java.array(as.raw(c(1, 2, 3))); java.class(a);", "'[B'");
        assertEvalFastR("a <- as.java.array(as.raw(c(1, 2, 3)), 'int'); java.class(a);", "'[I'");

        assertEvalFastR("a <- as.java.array(as.external.short(1)); a;", getRValue(new short[]{1}));
        assertEvalFastR("a <- as.java.array(as.external.short(1)); java.class(a);", "'[S'");
        assertEvalFastR("a <- as.java.array(c(as.external.short(1), as.external.short(2))); a;", getRValue(new short[]{1, 2}));
        assertEvalFastR("a <- as.java.array(as.external.short(1),,T); a;", getRValue(new short[]{1}));
        assertEvalFastR("a <- as.java.array(c(as.external.short(1), as.external.short(2)),,T); a;", getRValue(new short[]{1, 2}));
        assertEvalFastR("a <- as.java.array(c(as.external.short(1), as.external.short(2)),'int',T); a;", getRValue(new int[]{1, 2}));

        assertEvalFastR("a <- as.java.array(as.external.short(1), 'java.lang.Short'); a;", getRValue(new short[]{1}));
        assertEvalFastR("a <- as.java.array(c(as.external.short(1), as.external.short(2)), 'java.lang.Short'); a;", getRValue(new short[]{1, 2}));
        assertEvalFastR("a <- as.java.array(as.external.short(1), 'java.lang.Short', T); a;", getRValue(new short[]{1}));
        assertEvalFastR("a <- as.java.array(c(as.external.short(1), as.external.short(2)), 'java.lang.Short', T); a;", getRValue(new short[]{1, 2}));

        assertEvalFastR("a <- as.java.array(c(as.external.short(1), as.external.short(2)), 'int'); a;", getRValue(new int[]{1, 2}));
        assertEvalFastR("a <- as.java.array(c(1.123, 2.123), 'double'); a;", getRValue(new double[]{1.123, 2.123}));
        assertEvalFastR("a <- as.java.array(as.external.short(1), 'double'); a;", getRValue(new double[]{1}));

        assertEvalFastR("a <- as.java.array(1L); as.java.array(a);", getRValue(new int[]{1}));
        assertEvalFastR("a <- as.java.array(1L); as.java.array(a,,T);", getRValue(new int[]{1}));

        assertEvalFastR("a <- as.java.array(as.external.short(1)); as.java.array(a);", getRValue(new short[]{1}));

        assertEvalFastR("tc <- new.java.class('" + TEST_CLASS + "'); to <- new.external(tc); a <- as.java.array(to); is.external.array(a)", "TRUE");
        assertEvalFastR("tc <- new.java.class('" + TEST_CLASS + "'); to <- new.external(tc); a <- as.java.array(c(to, to)); is.external.array(a)", "TRUE");
        assertEvalFastR("tc <- new.java.class('" + TEST_CLASS + "'); to <- new.external(tc); a <- as.java.array(c(to, to)); length(a)", "2");
        assertEvalFastR("tc <- new.java.class('" + TEST_CLASS + "'); to <- new.external(tc); a <- as.java.array(c(to, to)); java.class(a);",
                        "[Lcom.oracle.truffle.r.test.library.fastr.TestJavaInterop$TestClass;");

        assertEvalFastR("a <- as.java.array(1L,,F); a;", getRValue(new int[]{1}));
    }

    @Test
    public void testArrayAsParameter() {
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "ja <- as.java.array(c(1L, 2L, 3L), 'int'); to$isIntArray(ja)", "'" + (new int[1]).getClass().getName() + "'");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "ja <- as.java.array(c(1L, 2L, 3L), 'java.lang.Integer'); to$isIntegerArray(ja)", "'" + (new Integer[1]).getClass().getName() + "'");
    }

    @Test
    public void testNewArray() {
        testNewArray("java.lang.Boolean", true);
        testNewArray("java.lang.Byte", true);
        testNewArray("java.lang.Character", true);
        testNewArray("java.lang.Double", true);
        testNewArray("java.lang.Float", true);
        testNewArray("java.lang.Integer", true);
        testNewArray("java.lang.Long", true);
        testNewArray("java.lang.Short", true);
        testNewArray("java.lang.String", true);

        testNewArray("boolean", true);
        testNewArray("byte", true);
        testNewArray("char", true);
        testNewArray("double", true);
        testNewArray("float", true);
        testNewArray("int", true);
        testNewArray("long", true);
        testNewArray("short", true);

        testNewArray("com.oracle.truffle.r.test.library.fastr.TestJavaInterop$TestClass", true);

        // test also with double length/dimensions
        testNewArray("java.lang.String", false);
    }

    public void testNewArray(String className, boolean dimInt) {
        String dim = dimInt ? "10L" : "10.9";
        assertEvalFastR("a <- new.java.array('" + className + "', " + dim + "); is.external.array(a);", "TRUE");
        assertEvalFastR("a <- new.java.array('" + className + "', " + dim + "); length(a);", "10");
        assertEvalFastR("a <- new.java.array('" + className + "', " + dim + "); java.class(a);", toArrayClassName(className, 1));

        dim = dimInt ? "c(2L, 3L)" : "c(2.9, 3.9)";
        assertEvalFastR("a <- new.java.array('" + className + "', " + dim + "); is.external.array(a);", "TRUE");
        assertEvalFastR("a <- new.java.array('" + className + "', " + dim + "); length(a);", "2L");
        assertEvalFastR("a <- new.java.array('" + className + "', " + dim + "); length(a[1]);", "3L");
        assertEvalFastR("a <- new.java.array('" + className + "', " + dim + "); java.class(a);", toArrayClassName(className, 2));
    }

    @Test
    public void testGetClass() {
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "java.class(to)", "'com.oracle.truffle.r.test.library.fastr.TestJavaInterop$TestClass'");

        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "java.class(to$methodReturnsNull())", errorIn("java.class(to$methodReturnsNull())", "unsupported type"));
        assertEvalFastR("java.class(NULL)", errorIn("java.class(NULL)", "unsupported type"));
        assertEvalFastR("java.class(1)", errorIn("java.class(1)", "unsupported type"));
    }

    @Test
    public void testAsVectorFromArray() {
        testAsVectorFromArray("fieldStaticBooleanArray", "logical");
        testAsVectorFromArray("fieldStaticByteArray", "integer");
        testAsVectorFromArray("fieldStaticCharArray", "character");
        testAsVectorFromArray("fieldStaticDoubleArray", "double");
        testAsVectorFromArray("fieldStaticFloatArray", "double");
        testAsVectorFromArray("fieldStaticIntegerArray", "integer");
        testAsVectorFromArray("fieldStaticLongArray", "double");
        testAsVectorFromArray("fieldStaticShortArray", "integer");
        testAsVectorFromArray("fieldStaticStringArray", "character");

        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " v <- as.vector(to$objectArray); is.list(v)", "TRUE");
        testAsVectorFromArray("objectIntArray", "integer");
        testAsVectorFromArray("objectDoubleArray", "double");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " v <- as.vector(to$mixedTypesArray); is.list(v)", "TRUE");

        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " v <- as.vector(to$hasNullIntArray); is.list(v)", "TRUE");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " v <- as.vector(to$hasNullIntArray); v[1]", "list(1)");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " v <- as.vector(to$hasNullIntArray); v[2]", "list(NULL)");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " v <- as.vector(to$hasNullIntArray); v[3]", "list(3)");
    }

    @Test
    public void testFromArray() {
        testAsVectorFromArray("fieldStaticBooleanArray", "logical");
        testAsVectorFromArray("fieldStaticByteArray", "integer");
        testAsVectorFromArray("fieldStaticCharArray", "character");
        testAsVectorFromArray("fieldStaticDoubleArray", "double");
        testAsVectorFromArray("fieldStaticFloatArray", "double");
        testAsVectorFromArray("fieldStaticIntegerArray", "integer");
        testAsVectorFromArray("fieldStaticLongArray", "double");
        testAsVectorFromArray("fieldStaticShortArray", "integer");
        testAsVectorFromArray("fieldStaticStringArray", "character");

        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " v <- .fastr.interop.fromArray(to$objectArray); is.list(v)", "TRUE");
        testAsVectorFromArray("objectIntArray", "integer");
        testAsVectorFromArray("objectDoubleArray", "double");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " v <- .fastr.interop.fromArray(to$mixedTypesArray); is.list(v)", "TRUE");

        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " v <- .fastr.interop.fromArray(to$hasNullIntArray); is.list(v)", "TRUE");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " v <- .fastr.interop.fromArray(to$hasNullIntArray); v[1]", "list(1)");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " v <- .fastr.interop.fromArray(to$hasNullIntArray); v[2]", "list(NULL)");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " v <- .fastr.interop.fromArray(to$hasNullIntArray); v[3]", "list(3)");

        assertEvalFastR("ja <- new.java.array('java.lang.String', 0L); .fastr.interop.fromArray(ja)", "list()");
    }

    public void testAsVectorFromArray(String field, String type) {
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " v <- as.vector(to$" + field + "); is.vector(v)", "TRUE");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " v <- as.vector(to$" + field + "); typeof(v)", getRValue(type));
    }

    @Test
    public void testInteroptNew() {
        assertEvalFastR("tc <- new.java.class('" + Boolean.class.getName() + "'); t <- new.external(tc, TRUE); t", "TRUE");
        assertEvalFastR("tc <- new.java.class('java/lang/Boolean'); t <- new(tc, TRUE); t", "TRUE");
        assertEvalFastR("tc <- new.java.class('" + Byte.class.getName() + "'); t <- new.external(tc, as.external.byte(1)); t", "1");
        assertEvalFastR("tc <- new.java.class('" + Character.class.getName() + "'); t <- new.external(tc, as.external.char(97)); t", "'a'");
        assertEvalFastR("tc <- new.java.class('" + Double.class.getName() + "'); t <- new.external(tc, 1.1); t", "1.1");
        assertEvalFastR("tc <- new.java.class('" + Float.class.getName() + "'); t <- new.external(tc, as.external.float(1.1)); t", "1.1");
        assertEvalFastR("tc <- new.java.class('" + Integer.class.getName() + "'); t <- new.external(tc, 1L); t", "1");
        assertEvalFastR("tc <- new.java.class('" + Long.class.getName() + "'); t <- new.external(tc, as.external.long(1)); t", "1");
        assertEvalFastR("tc <- new.java.class('" + Short.class.getName() + "'); t <- new.external(tc, as.external.short(1)); t", "1");
        assertEvalFastR("tc <- new.java.class('" + String.class.getName() + "'); t <- new.external(tc, 'abc'); t", "'abc'");
        assertEvalFastR("tc <- new.java.class('" + TestNullClass.class.getName() + "'); t <- new.external(tc, NULL); class(t)", "'" + RType.TruffleObject.getName() + "'");
    }

    @Test
    public void testNewWithJavaClass() {
        assertEvalFastR("tc <- new.java.class('" + Boolean.class.getName() + "'); to <- new(tc, TRUE); to", "TRUE");
        assertEvalFastR("tc <- new.java.class('" + TEST_CLASS + "'); to <- new(tc); to$fieldInteger", getRValue(Integer.MAX_VALUE));

        assertEvalFastR("to <- new('" + Boolean.class.getName() + "', TRUE); to", "TRUE");
        assertEvalFastR("to <- new('java/lang/Boolean', TRUE); to", "TRUE");
        assertEvalFastR("to <- new('" + TEST_CLASS + "'); to$fieldStaticInteger", getRValue(Integer.MAX_VALUE));

        assertEvalFastR("to <- new('" + TEST_CLASS + "'); new(to)", errorIn("new.external(Class, ...)", "error during Java object instantiation"));

        assertEvalFastR("to <- new('__bogus_class_name__');", errorIn("getClass(Class, where = topenv(parent.frame()))", "“__bogus_class_name__” is not a defined class"));
    }

    @Test
    public void testCombineInteropTypes() {
        assertEvalFastR("class(c(as.external.byte(123)))", "'interopt.byte'");
        assertEvalFastR("class(c(as.external.byte(123), as.external.byte(234)))", "'list'");
        assertEvalFastR("class(c(as.external.byte(123), 1))", "'list'");
        assertEvalFastR("class(c(1, as.external.byte(123)))", "'list'");
    }

    @Test
    public void testCombineForeignObjects() throws IllegalAccessException, IllegalArgumentException {

        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " class(c(to))", "'list'");
        assertEvalFastR("tc <- new.java.class('" + TEST_CLASS + "'); t <- new.external(tc); t1 <- new.external(tc); class(c(t, t1))", "'list'");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " class(c(1, t))", "'list'");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " class(c(t, 1))", "'list'");

        TestClass t = new TestClass();

        testCombineForeignObjects("fieldBooleanArray", t.fieldBooleanArray);
        testCombineForeignObjects("fieldByteArray", t.fieldByteArray);
        testCombineForeignObjects("fieldCharArray", t.fieldCharArray);
        testCombineForeignObjects("fieldDoubleArray", t.fieldDoubleArray);
        testCombineForeignObjects("fieldFloatArray", t.fieldFloatArray);
        testCombineForeignObjects("fieldIntegerArray", t.fieldIntegerArray);
        testCombineForeignObjects("fieldLongArray", t.fieldLongArray);
        testCombineForeignObjects("fieldShortArray", t.fieldShortArray);
        testCombineForeignObjects("fieldStringArray", t.fieldStringArray);

        testCombineForeignObjects("listBoolean", t.listBoolean);
        testCombineForeignObjects("listByte", t.listByte);
        testCombineForeignObjects("listChar", t.listChar);
        testCombineForeignObjects("listDouble", t.listDouble);
        testCombineForeignObjects("listFloat", t.listFloat);
        testCombineForeignObjects("listInteger", t.listInteger);
        testCombineForeignObjects("listLong", t.listLong);
        testCombineForeignObjects("listShort", t.listShort);
        testCombineForeignObjects("listString", t.listString);
        testCombineForeignObjects("listStringInt", t.listStringInt);
        testCombineForeignObjects("listStringBoolean", t.listStringBoolean);
        testCombineForeignObjects("listEmpty", t.listEmpty);
    }

    private void testCombineForeignObjects(String field, Object fieldObject) {
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " c(to$" + field + ")", toRVector(fieldObject, null));
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " c(to$" + field + ", to$" + field + ")", c(fieldObject, fieldObject));
    }

    @Test
    public void testFields() throws IllegalArgumentException, IllegalAccessException {
        TestClass t = new TestClass();
        Field[] fields = t.getClass().getDeclaredFields();
        for (Field f : fields) {
            String name = f.getName();
            if (name.startsWith("field")) {
                testForValue(name, f.get(t));
            }
        }
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " to$mixedTypesArray",
                        "cat('[external object]\n[[1]]\n[1] 1\n\n[[2]]\n[1] 2.1\n\n[[3]]\n[1] \"a\"\n\n[[4]]\n[1] TRUE\n\n[[5]]\nNULL\n\n', sep='')");
    }

    @Test
    public void testMethods() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        TestClass t = new TestClass();
        Method[] methods = t.getClass().getDeclaredMethods();
        for (Method m : methods) {
            if (m.getParameterCount() == 0) {
                String name = m.getName();
                if (name.startsWith("method")) {
                    testForValue(name + "()", m.invoke(t));
                }
            }
        }
    }

    private void testForValue(String member, Object value) {
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " to$" + member, getRValue(value));
    }

    @Test
    public void testAllTypes() {
        getValueForAllTypesMethod("allTypesMethod");
        getValueForAllTypesMethod("allTypesStaticMethod");
    }

    @Test
    public void testNonPrimitiveParameter() {
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " to$equals(to)", "TRUE");
    }

    @Test
    public void testClassAsParameter() {
        // fails in testdownstream
        assertEvalFastR(Ignored.ImplementationError, CREATE_TRUFFLE_OBJECT + " to$classAsArg(new.java.class(" + TEST_CLASS + "))", getRValue(TEST_CLASS));
    }

    private void getValueForAllTypesMethod(String method) {
        boolean bo = true;
        byte bt = Byte.MAX_VALUE;
        char c = 'a';
        short sh = Short.MAX_VALUE;
        int i = Integer.MAX_VALUE;
        long l = Long.MAX_VALUE;
        double d = Double.MAX_VALUE;
        float f = Float.MAX_VALUE;
        String s = "testString";
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " to$" + method + "(" + getRValuesAsString(bo, bt, c, sh, i, l, d, f, s) + ")",
                        getRValue("" + bo + bt + c + sh + i + l + d + f + s));
    }

    @Test
    public void testNullParameters() {
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " to$methodAcceptsOnlyNull(NULL)", "");

        assertEvalFastR(Ignored.Unimplemented, CREATE_TRUFFLE_OBJECT + " to$isNull('string')", "java.lang.String");
        assertEvalFastR(Ignored.Unimplemented, CREATE_TRUFFLE_OBJECT + " to$isNull(1)", "java.lang.Long");
    }

    @Test
    public void testOverloaded() {
        String className = TestOverload.class.getName();
        String createClass = "toc <- new.java.class('" + className + "'); ";

        assertEvalFastR(createClass + " toc$isOverloaded(TRUE)", "'boolean'");
        assertEvalFastR(createClass + " toc$isOverloaded(as.external.byte(1))", "'byte'");
        assertEvalFastR(createClass + " toc$isOverloaded(as.external.char('a'))", "'char'");
        assertEvalFastR(createClass + " toc$isOverloaded(1)", "'double'");
        assertEvalFastR(createClass + " toc$isOverloaded(as.external.float(1))", "'float'");
        assertEvalFastR(createClass + " toc$isOverloaded(1L)", "'int'");
        assertEvalFastR(createClass + " toc$isOverloaded(as.external.long(1))", "'long'");
        assertEvalFastR(createClass + " toc$isOverloaded(as.external.short(1))", "'short'");
        assertEvalFastR(createClass + " toc$isOverloaded('string')", getRValue(String.class.getName()));

        assertEvalFastR("new('" + className + "', TRUE)$type", "'boolean'");
        assertEvalFastR("new('" + className + "', as.external.byte(1))$type", "'byte'");
        assertEvalFastR("new('" + className + "', as.external.char('a'))$type", "'char'");
        assertEvalFastR("new('" + className + "', 1)$type", "'double'");
        assertEvalFastR("new('" + className + "', as.external.float(1))$type", "'float'");
        assertEvalFastR("new('" + className + "', 1L)$type", "'int'");
        assertEvalFastR("new('" + className + "', as.external.long(1))$type", "'long'");
        assertEvalFastR("new('" + className + "', as.external.short(1))$type", "'short'");
        assertEvalFastR("new('" + className + "', 'string')$type", getRValue(String.class.getName()));
    }

    @Test
    public void testArrayReadWrite() {
        assertEvalFastR("a <- as.java.array(c(1,2,3)); a[1]", "1");
        assertEvalFastR("a <- as.java.array(c(1,2,3)); a[[1]]", "1");

        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " to$fieldIntegerArray[1];", "1");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " to$fieldIntegerArray[[1]];", "1");

        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " to$int2DimArray[1]", getRValue(new int[]{1, 2, 3}));
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " to$int2DimArray[[1]]", getRValue(new int[]{1, 2, 3}));
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " to$int2DimArray[1,2]", "2");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " to$int2DimArray[[1,2]]", "2");

        assertEvalFastR("a <- as.java.array(c(1,2,3)); a[1] <- 123; a[1]", "123");
        assertEvalFastR("a <- as.java.array(c(1,2,3)); a[[1]] <- 123; a[[1]]", "123");

        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " to$fieldIntegerArray[1] <- 123L; to$fieldIntegerArray[1]", "123");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " to$fieldIntegerArray[[1]] <- 1234L; to$fieldIntegerArray[[1]]", "1234");

        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " to$fieldStringArray[1] <- NULL; to$fieldStringArray[1]", "NULL");

        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " to$int2DimArray[1,2] <- 1234L; to$int2DimArray[1,2]", "1234");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " to$int2DimArray[[1,2]] <- 12345L; to$int2DimArray[[1,2]]", "12345");
    }

    @Test
    public void testReadByVector() {
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " to$fieldStringArray[c(1, 3)]", "c('a', 'c')");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " to$map[c('one', 'three')]", "c('1', '3')");

        TestClass tc = new TestClass();
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " to[c('fieldStringObject', 'fieldChar')]", "c('" + tc.fieldStringObject + "', '" + tc.fieldChar + "')");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " to[c('fieldStringObject', 'fieldInteger')]", "list('" + tc.fieldStringObject + "', " + tc.fieldInteger + ")");
    }

    @Test
    public void testLengthArray() {
        TestArraysClass ta = new TestArraysClass();
        assertEvalFastR(CREATE_TEST_ARRAYS + " length(ta$objectEmpty)", "" + ta.objectEmpty.length);

        assertEvalFastR(CREATE_TEST_ARRAYS + " length(ta$booleanArray)", "" + ta.booleanArray.length);
        assertEvalFastR(CREATE_TEST_ARRAYS + " length(ta$booleanArray2)", "" + ta.booleanArray2.length);
        assertEvalFastR(CREATE_TEST_ARRAYS + " length(ta$booleanArray3)", "" + ta.booleanArray3.length);
    }

    @Test
    public void testLengthIterable() {
        String thisFQN = this.getClass().getName();
        for (String s : new String[]{"Size", "GetSize", "Length", "GetLength", "NoSizeMethod"}) {
            String clazz = thisFQN + "$TestIterable" + s;
            assertEvalFastR("ti <- new('" + clazz + "', 123); length(ti)", "123");
        }
    }

    public void testMap() {
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " m <- to$map; m['one']", "'1'");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " m <- to$map; m['two']", "'2'");

        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " m <- to$map; m['one']<-'11'; m['one']", "'11'");

        // truffle
        assertEvalFastR(Ignored.Unimplemented, "how to put into map?", "'11'");
    }

    @Test
    public void testNamesForForeignObject() {
        assertEvalFastR("tc <- new.java.class('" + TestNamesClassNoMembers.class.getName() + "'); t <- new.external(tc); names(t)", "NULL");
        assertEvalFastR("tc <- new.java.class('" + TestNamesClassNoPublicMembers.class.getName() + "'); t <- new.external(tc); names(t)", "NULL");
        assertEvalFastR("tc <- new.java.class('" + TestNamesClass.class.getName() + "'); sort(names(tc))", "c('staticField', 'staticMethod')");
        assertEvalFastR("tc <- new.java.class('" + TestNamesClass.class.getName() + "'); names(tc$staticField)", "NULL");
        assertEvalFastR("tc <- new.java.class('" + TestNamesClass.class.getName() + "'); names(tc$staticMethod)", "NULL");
        assertEvalFastR("tc <- new.java.class('" + TestNamesClass.class.getName() + "'); t <- new.external(tc); sort(names(t))", "c('field', 'method', 'staticField', 'staticMethod')");
        // Note: The following two tests fails on Solaris. It seems that the Java interop on
        // Solaris treats the two inner classes SimpleImmutableEntry and SimpleEntry of
        // java.util.AbstractMap as if they were members.
        assertEvalFastR(Ignored.Unstable, "cl <- new.java.class('java.util.Collections'); em<-cl$EMPTY_MAP; names(em)", "NULL");
        assertEvalFastR(Ignored.Unstable, "tc <- new.java.class('" + TestNamesClassMap.class.getName() + "'); to <- new.external(tc); sort(names(to$m()))", "c('one', 'two')");
    }

    @Test
    public void testIsExternalExecutable() {
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "is.external.executable(to)", "FALSE");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "is.external.executable(to$methodBoolean)", "TRUE");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "is.external.executable(to$fieldBoolean)", "FALSE");
    }

    @Test
    public void testIsExternalNull() {
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "is.external.null(to)", "FALSE");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "is.external.null(to$methodReturnsNull)", "FALSE");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "is.external.null(to$methodReturnsNull())", "TRUE");
    }

    @Test
    public void testIsXXXForForeignObject() {
        // missing: is.element, is.empty.model, is.leaf, is.loaded, is.na.data.frame,
        // is.na.numeric_version, is.na.POSIXlt

        assertPassingForeighObjectToFunction("is.array", "FALSE");
        assertPassingForeighObjectToFunction("is.atomic", "FALSE");
        assertPassingForeighObjectToFunction("is.call", "FALSE");
        assertPassingForeighObjectToFunction("is.character", "FALSE");
        assertPassingForeighObjectToFunction("is.complex", "FALSE");
        assertPassingForeighObjectToFunction("is.data.frame", "FALSE");
        assertPassingForeighObjectToFunction("is.double", "FALSE");
        assertPassingForeighObjectToFunction("is.environment", "FALSE");
        assertPassingForeighObjectToFunction("is.expression", "FALSE");
        assertPassingForeighObjectToFunction("is.factor", "FALSE");
        assertPassingForeighObjectToFunction("is.function", "FALSE");
        assertPassingForeighObjectToFunction("is.integer", "FALSE");
        assertPassingForeighObjectToFunction("is.language", "FALSE");
        assertPassingForeighObjectToFunction("is.logical", "FALSE");
        assertPassingForeighObjectToFunction("is.matrix", "FALSE");
        assertPassingForeighObjectToFunction("is.mts", "FALSE");
        assertPassingForeighObjectToFunction("is.na", "FALSE");
        assertPassingForeighObjectToFunction("is.name", "FALSE");
        assertPassingForeighObjectToFunction("is.null", "FALSE");
        assertPassingForeighObjectToFunction("is.numeric", "FALSE");
        assertPassingForeighObjectToFunction("is.numeric.Date", "FALSE");
        assertPassingForeighObjectToFunction("is.numeric.difftime", "FALSE");
        assertPassingForeighObjectToFunction("is.numeric.POSIXt", "FALSE");
        assertPassingForeighObjectToFunction("is.numeric_version", "FALSE");
        assertPassingForeighObjectToFunction("is.object", "FALSE");
        assertPassingForeighObjectToFunction("is.ordered", "FALSE");
        assertPassingForeighObjectToFunction("is.package_version", "FALSE");
        assertPassingForeighObjectToFunction("is.pairlist", "FALSE");
        assertPassingForeighObjectToFunction("is.primitive", "FALSE");
        assertPassingForeighObjectToFunction("is.qr", "FALSE");
        assertPassingForeighObjectToFunction("is.raster", "FALSE");
        assertPassingForeighObjectToFunction("is.raw", "FALSE");
        assertPassingForeighObjectToFunction("is.recursive", "FALSE");
        assertPassingForeighObjectToFunction("is.relistable", "FALSE");
        assertPassingForeighObjectToFunction("is.stepfun", "FALSE");
        assertPassingForeighObjectToFunction("is.symbol", "FALSE");
        assertPassingForeighObjectToFunction("is.table", "FALSE");
        assertPassingForeighObjectToFunction("is.ts", "FALSE");
        assertPassingForeighObjectToFunction("is.tskernel", "FALSE");
        assertPassingForeighObjectToFunction("is.unsorted", "FALSE");
        assertPassingForeighObjectToFunction("is.vector", "FALSE");

        assertPassingForeighObjectToFunction("is.nan", errorIn("is.nan(to)", "default method not implemented for type 'external object'"));
        assertPassingForeighObjectToFunction("is.finite", errorIn("is.finite(to)", "default method not implemented for type 'external object'"));
        assertPassingForeighObjectToFunction("is.infinite", errorIn("is.infinite(to)", "default method not implemented for type 'external object'"));

    }

    private void assertPassingForeighObjectToFunction(String function, String expectedOutput) {
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " " + function + "(to)", expectedOutput);
    }

    @Test
    public void testAddToList() {
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " l <- list(to); is.list(l)", "TRUE");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " l <- list();  l$foreignobject <- to; identical(to, l$foreignobject)", "TRUE");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " l <- list(1); l$foreignobject <- to; identical(to, l$foreignobject)", "TRUE");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " l <- list(1); l$foreignobject <- 1; l$foreignobject <- to; identical(to, l$foreignobject)", "TRUE");
    }

    @Test
    public void testAttributes() {
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " attributes(to)", "NULL");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " attr(to, 'a')<-'a'", errorIn("attr(to, \"a\") <- \"a\"", "external object cannot be attributed"));
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " attr(to, which = 'a')", errorIn("attr(to, which = \"a\")", "external object cannot be attributed"));
    }

    @Test
    public void testIdentical() {
        assertEvalFastR("b1 <- as.external.byte(1); identical(b1, b1)", "TRUE");
        assertEvalFastR("b1 <- as.external.byte(1); b2 <- as.external.byte(1); identical(b1, b2)", "FALSE");
        assertEvalFastR("b1 <- as.external.byte(1); s1 <- as.external.short(1); identical(b1, s1)", "FALSE");

        assertEvalFastR("al <- new.external(new.java.class('java.util.ArrayList')); identical(t, t)", "TRUE");
        assertEvalFastR("ll <- new.external(new.java.class('java.util.LinkedList')); al <- new.external(new.java.class('java.util.ArrayList')); identical(al, ll)", "FALSE");
    }

    @Test
    public void testAsList() {
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " l<-as.list(to$listString); is.list(l)", "TRUE");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " l<-as.list(to$listString); l[[1]]", "'a'");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " l<-as.list(to$listString); l[[2]]", "'b'");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " l<-as.list(to$listString); length(l)", "3");

        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " l<-as.list(to$listObject); is.list(l)", "TRUE");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " l<-as.list(to$listObject); l[[1]]$data", "'a'");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " l<-as.list(to$listObject); l[[2]]$data", "'b'");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " l<-as.list(to$listObject); length(l)", "4");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " l<-as.list(to$listObject); l[[4]]$data", "NULL");

        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " l<-as.list(to$fieldStringArray); is.list(l)", "TRUE");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " l<-as.list(to$fieldStringArray); l[[1]]", "'a'");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " l<-as.list(to$fieldStringArray); l[[2]]", "'b'");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " l<-as.list(to$fieldStringArray); length(l)", "3");

        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " l<-as.list(to$arrayObject); is.list(l)", "TRUE");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " l<-as.list(to$arrayObject); l[[1]]$data", "'a'");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " l<-as.list(to$arrayObject); l[[2]]$data", "'b'");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " l<-as.list(to$arrayObject); length(l)", "4");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " l<-as.list(to$arrayObject); l[[4]]$data", "NULL");

        assertEvalFastR(Output.IgnoreErrorContext, CREATE_TRUFFLE_OBJECT + " l<-as.list(to);", errorIn("as.list(to)", "no method for coercing this external object to a list"));
    }

    private static final String CREATE_TEST_ARRAYS = "ta <- new('" + TestArraysClass.class.getName() + "');";

    @Test
    public void testUnlist() throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {

        assertEvalFastR(CREATE_TEST_ARRAYS + " tal <- unlist(ta); identical(ta, tal)", "TRUE");
        assertEvalFastR(CREATE_TEST_ARRAYS + " l<-list(ta, ta); ul <- unlist(l); identical(l, ul)", "TRUE");

        // arrays
        testUnlistByType("string", "'a', 'b', 'c'", "character");
        testUnlistByType("boolean", "TRUE, FALSE, TRUE", "logical");
        testUnlistByType("byte", "1, 2, 3", "integer");
        testUnlistByType("char", "'a', 'b', 'c'", "character");
        testUnlistByType("double", "1.1, 1.2, 1.3", "numeric");
        testUnlistByType("float", "1.1, 1.2, 1.3", "numeric");
        testUnlistByType("integer", "1, 2, 3", "integer");
        testUnlistByType("long", "1, 2, 3", "numeric");
        testUnlistByType("short", "1, 2, 3", "integer");

        assertEvalFastR(CREATE_TEST_ARRAYS + " unlist(ta$onlyIntegerObjectArray)", "c(1, 2, 3)");
        assertEvalFastR(CREATE_TEST_ARRAYS + " class(unlist(ta$onlyIntegerObjectArray))", "'integer'");

        assertEvalFastR(CREATE_TEST_ARRAYS + " unlist(ta$onlyLongObjectArray)", "c(1, 2, 3)");
        assertEvalFastR(CREATE_TEST_ARRAYS + " class(unlist(ta$onlyLongObjectArray))", "'numeric'");

        assertEvalFastR(CREATE_TEST_ARRAYS + " unlist(ta$mixedObjectArray)", "c('1', 'a', '1')");
        assertEvalFastR(CREATE_TEST_ARRAYS + " class(unlist(ta$mixedObjectArray))", "'character'");

        assertEvalFastR(CREATE_TEST_ARRAYS + " unlist(ta$mixedIntegerList)", "c(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12)");
        assertEvalFastR(CREATE_TEST_ARRAYS + " class(unlist(ta$mixedIntegerList))", "'integer'");

        testForeingObjectInListUnlist("byte", new String[]{"TRUE", "FALSE", "TRUE"}, "integer");
        testForeingObjectInListUnlist("byte", new String[]{"1L", "2L", "3L"}, "integer");
        testForeingObjectInListUnlist("byte", new String[]{"1.1", "2.1", "3.1"}, "numeric");
        testForeingObjectInListUnlist("byte", new String[]{"'a'", "'aa'", "'aaa'"}, "character");

        testForeingObjectInListUnlist("boolean", new String[]{"TRUE", "FALSE", "TRUE"}, "logical");
        testForeingObjectInListUnlist("boolean", new String[]{"1L", "2L", "3L"}, "integer");
        testForeingObjectInListUnlist("boolean", new String[]{"1.1", "1.2", "1.3"}, "numeric");
        testForeingObjectInListUnlist("boolean", new String[]{"'a'", "'aa'", "'aaa'"}, "character");

        testForeingObjectInListUnlist("double", new String[]{"TRUE", "FALSE", "TRUE"}, "numeric");
        testForeingObjectInListUnlist("double", new String[]{"1L", "2L", "3L"}, "numeric");
        testForeingObjectInListUnlist("double", new String[]{"1.1", "2.1", "3.1"}, "numeric");
        testForeingObjectInListUnlist("double", new String[]{"'a'", "'aa'", "'aaa'"}, "character");

        testForeingObjectInListUnlist("float", new String[]{"TRUE", "FALSE", "TRUE"}, "numeric");
        testForeingObjectInListUnlist("float", new String[]{"1L", "2L", "3L"}, "numeric");
        testForeingObjectInListUnlist("float", new String[]{"1.1", "2.1", "3.1"}, "numeric");
        testForeingObjectInListUnlist("float", new String[]{"'a'", "'aa'", "'aaa'"}, "character");

        testForeingObjectInListUnlist("integer", new String[]{"TRUE", "FALSE", "TRUE"}, "integer");
        testForeingObjectInListUnlist("integer", new String[]{"1L", "2L", "3L"}, "integer");
        testForeingObjectInListUnlist("integer", new String[]{"1.1", "1.2", "1.3"}, "numeric");
        testForeingObjectInListUnlist("integer", new String[]{"'a'", "'aa'", "'aaa'"}, "character");

        testForeingObjectInListUnlist("long", new String[]{"TRUE", "FALSE", "TRUE"}, "numeric");
        testForeingObjectInListUnlist("long", new String[]{"1L", "2L", "3L"}, "numeric");
        testForeingObjectInListUnlist("long", new String[]{"1.1", "1.2", "1.3"}, "numeric");
        testForeingObjectInListUnlist("long", new String[]{"'a'", "'aa'", "'aaa'"}, "character");

        testForeingObjectInListUnlist("short", new String[]{"TRUE", "FALSE", "TRUE"}, "integer");
        testForeingObjectInListUnlist("short", new String[]{"1L", "2L", "3L"}, "integer");
        testForeingObjectInListUnlist("short", new String[]{"1.1", "1.2", "1.3"}, "numeric");
        testForeingObjectInListUnlist("short", new String[]{"'a'", "'aa'", "'aaa'"}, "character");

        testForeingObjectInListUnlist("string", new String[]{"TRUE", "FALSE", "TRUE"}, "character");
        testForeingObjectInListUnlist("string", new String[]{"1L", "2L", "3L"}, "character");
        testForeingObjectInListUnlist("string", new String[]{"1.1", "1.2", "1.3"}, "character");
        testForeingObjectInListUnlist("string", new String[]{"'a'", "'aa'", "'aaa'"}, "character");
    }

    private void testUnlistByType(String fieldType, String result, String clazz) {
        testForeingObjectUnlist(fieldType + "Array", result, clazz);
        if (!fieldType.equals("string")) {
            testForeingObjectUnlist(fieldType + "ObjectArray", result, clazz);
        }
        testForeingObjectUnlist(fieldType + "List", result, clazz);
    }

    private void testForeingObjectUnlist(String fieldPrefix, String result, String clazz) {
        String field = fieldPrefix;
        assertEvalFastR(CREATE_TEST_ARRAYS + " unlist(ta$" + field + ")", "c(" + result + ")");
        assertEvalFastR(CREATE_TEST_ARRAYS + " class(unlist(ta$" + field + "))", "'" + clazz + "'");
        assertEvalFastR(CREATE_TEST_ARRAYS + " unlist(ta$" + field + ", recursive=FALSE)", "c(" + result + ")");

        field = fieldPrefix + "2";
        assertEvalFastR(CREATE_TEST_ARRAYS + " unlist(ta$" + field + ")", "c(" + result + ", " + result + ")");
        assertEvalFastR(CREATE_TEST_ARRAYS + " class(unlist(ta$" + field + "))", "'" + clazz + "'");

        assertEvalFastR(CREATE_TEST_ARRAYS + " unlist(ta$" + field + ", recursive=FALSE)", "list(" + result + ", " + result + "))");

        field = fieldPrefix + "3";
        assertEvalFastR(CREATE_TEST_ARRAYS + " unlist(ta$" + field + ")", "c(" + result + ", " + result + ", " + result + ", " + result + "))");
        assertEvalFastR(CREATE_TEST_ARRAYS + " class(unlist(ta$" + field + "))", "'" + clazz + "'");
        assertEvalFastR(CREATE_TEST_ARRAYS + " unlist(ta$" + field + ", recursive=FALSE)",
                        "cat('[[1]]','\n','[external object]','\n\n','[[2]]','\n','[external object]','\n\n','[[3]]','\n','[external object]','\n\n','[[4]]','\n','[external object]','\n\n', sep='')");

    }

    private void testForeingObjectInListUnlist(String field, String[] toMixWith, String clazz) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        StringBuilder sbToVector = new StringBuilder();
        StringBuilder sbVector = new StringBuilder();
        StringBuilder sbList = new StringBuilder();
        int i = 0;
        sbToVector.append("c(");
        for (String s : toMixWith) {
            sbVector.append(s);
            String mwv = toMixWith[i];
            if (mwv.endsWith("L")) {
                mwv = mwv.replace("L", "");
            }
            mwv = mwv.replace("'", "\"");
            sbList.append("'[[").append(++i).append("]]','\n','").append("[1] ").append(mwv).append("','\n\n',");
            sbToVector.append(s);
            if (i < toMixWith.length) {
                sbVector.append(", ");
                sbToVector.append(", ");
            }
        }
        sbToVector.append(")");

        String mixWithVector = sbToVector.toString();
        String resultInVector = sbVector.toString();
        String resultInList = sbList.toString();
        String testFieldArrayResult = getTestFieldValuesAsResult(field + "Array");
        String testFieldListResult = getTestFieldValuesAsResult(field + "List");
        assertEvalFastR(CREATE_TEST_ARRAYS + " unlist(list(" + mixWithVector + ", ta$" + field + "Array, ta$" + field + "List))",
                        "c(" + resultInVector + ", " + testFieldArrayResult + ", " + testFieldListResult + ")");
        assertEvalFastR(CREATE_TEST_ARRAYS + " class(unlist(list(" + mixWithVector + ", ta$" + field + "Array, ta$" + field + "List)))", "'" + clazz + "'");
        assertEvalFastR(CREATE_TEST_ARRAYS + " unlist(list(" + mixWithVector + ", ta$" + field + "Array, ta$" + field + "List), recursive=FALSE)",
                        "c(" + resultInVector + ", " + testFieldArrayResult + ", " + testFieldListResult + ")");

        assertEvalFastR(CREATE_TEST_ARRAYS + " unlist(list(" + mixWithVector + ", ta$" + field + "Array2, ta$" + field + "List2))",
                        "c(" + resultInVector + ", " + testFieldArrayResult + ", " + testFieldArrayResult + ", " + testFieldListResult + ", " + testFieldListResult + ")");
        assertEvalFastR(CREATE_TEST_ARRAYS + " class(unlist(list(" + mixWithVector + ", ta$" + field + "Array2, ta$" + field + "List2)))", "'" + clazz + "'");
        assertEvalFastR(CREATE_TEST_ARRAYS + " unlist(list(" + mixWithVector + ", ta$" + field + "Array2, ta$" + field + "List2), recursive=FALSE)",
                        "cat(" + resultInList +
                                        "'[[4]]','\n','[external object]','\n\n','[[5]]','\n','[external object]','\n\n','[[6]]','\n','[external object]','\n\n','[[7]]','\n','[external object]','\n\n', sep='')");

        assertEvalFastR(CREATE_TEST_ARRAYS + " unlist(list(" + mixWithVector + ", ta$" + field + "Array3, ta$" + field + "List3))",
                        "c(" + resultInVector + ", " + testFieldArrayResult + ", " + testFieldArrayResult + ", " + testFieldArrayResult + ", " + testFieldArrayResult + ", " + testFieldListResult +
                                        ", " + testFieldListResult + ", " + testFieldListResult + ", " + testFieldListResult + ")");
        assertEvalFastR(CREATE_TEST_ARRAYS + " class(unlist(list(" + mixWithVector + ", ta$" + field + "Array3, ta$" + field + "List3)))", "'" + clazz + "'");
        assertEvalFastR(CREATE_TEST_ARRAYS + " unlist(list(" + mixWithVector + ", ta$" + field + "Array3, ta$" + field + "List3), recursive=FALSE)",
                        "cat(" + resultInList +
                                        "'[[4]]','\n','[external object]','\n\n','[[5]]','\n','[external object]','\n\n','[[6]]','\n','[external object]','\n\n','[[7]]','\n','[external object]','\n\n', sep='')");
    }

    private static String getTestFieldValuesAsResult(String name) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        TestArraysClass ta = new TestArraysClass();
        Field f = ta.getClass().getDeclaredField(name);
        Object value = f.get(ta);
        if (value instanceof List) {
            List<?> l = (List<?>) value;
            value = l.toArray(new Object[l.size()]);
        }
        StringBuilder sb = new StringBuilder();
        int length = Array.getLength(value);
        for (int i = 0; i < length; i++) {
            Object object = Array.get(value, i);
            if (object instanceof String || object instanceof Character) {
                sb.append("'");
            }
            if (object instanceof Boolean) {
                sb.append(Boolean.toString((boolean) object).toUpperCase());
            } else if (object instanceof Float) {
                sb.append((double) ((float) object));
            } else {
                sb.append(object);
            }
            if (object instanceof String || object instanceof Character) {
                sb.append("'");
            }
            if (i < length - 1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    @Test
    public void testConvertEmptyList() throws IllegalArgumentException {
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "as.character(to$listEmpty);", "as.character(list())");
    }

    @Test
    public void testAsXXX() throws IllegalArgumentException, IllegalAccessException {
        testAsXXX("as.character", "character");
        testAsXXX("as.complex", "complex");
        testAsXXX("as.double", "double");
        testAsXXX("as.expression", "expression");
        testAsXXX("as.integer", "integer");
        testAsXXX("as.logical", "logical");
        testAsXXX("as.raw", "raw");
        testAsXXX("as.symbol", "symbol");
        testAsXXX("as.vector", null);
        // TODO more tests
    }

    public void testAsXXX(String asXXX, String type) throws IllegalArgumentException, IllegalAccessException {
        TestClass t = new TestClass();

        Field[] fields = t.getClass().getDeclaredFields();
        for (Field f : fields) {
            String name = f.getName();
            String expr = CREATE_TRUFFLE_OBJECT + asXXX + "(to$" + name + ")";
            // test for each primitive wrapper object and string
            if (name.startsWith("fieldStatic") && name.endsWith("Object")) {
                if (asXXX.equals("as.character") && name.contains("Long")) {
                    assertEvalFastR(Ignored.ImplementationError, expr, getAsXXX(f.get(t), asXXX));
                } else if (!(asXXX.equals("as.character") || asXXX.equals("as.expression") || asXXX.equals("as.logical") || asXXX.equals("as.symbol") || asXXX.equals("as.vector")) &&
                                (name.contains("String") || name.contains("Char"))) {
                    assertEvalFastR(Output.IgnoreWarningMessage, expr, getAsXXX(f.get(t), asXXX));
                } else if (asXXX.equals("as.expression") && (name.contains("Long") || name.contains("Double"))) {
                    assertEvalFastR(Ignored.ImplementationError, expr, getAsXXX(f.get(t), asXXX));
                } else if (asXXX.equals("as.raw") && (name.contains("Short") || name.contains("Integer") || name.contains("Long") || name.contains("Double") || name.contains("NaN"))) {
                    assertEvalFastR(Output.IgnoreWarningMessage, expr, getAsXXX(f.get(t), asXXX));
                } else if (asXXX.equals("as.symbol") && (name.contains("Long") || name.contains("Double") || name.contains("Float"))) {
                    assertEvalFastR(Ignored.ImplementationError, expr, getAsXXX(f.get(t), asXXX));
                } else if (asXXX.equals("as.symbol") && (name.contains("Null"))) {
                    assertEvalFastR(Output.IgnoreErrorContext, expr, getAsXXX(f.get(t), asXXX));
                } else {
                    assertEvalFastR(expr, getAsXXX(f.get(t), asXXX));
                }
            }
        }

        // test arrays
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + asXXX + "(to$fieldBooleanArray);", toRVector(t.fieldBooleanArray, asXXX));
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + asXXX + "(to$fieldByteArray);", toRVector(t.fieldByteArray, asXXX));
        if (!(asXXX.equals("as.character") || asXXX.equals("as.expression") || asXXX.equals("as.logical") || asXXX.equals("as.symbol") || asXXX.equals("as.vector"))) {
            assertEvalFastR(Output.IgnoreWarningMessage, CREATE_TRUFFLE_OBJECT + asXXX + "(to$fieldCharArray);", toRVector(t.fieldCharArray, asXXX));
        } else {
            assertEvalFastR(CREATE_TRUFFLE_OBJECT + asXXX + "(to$fieldCharArray);", toRVector(t.fieldCharArray, asXXX));
        }
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + asXXX + "(to$fieldDoubleArray);", toRVector(t.fieldDoubleArray, asXXX));
        if (asXXX.equals("as.symbol")) {
            assertEvalFastR(Ignored.ImplementationError, CREATE_TRUFFLE_OBJECT + asXXX + "(to$fieldFloatArray);", toRVector(t.fieldFloatArray, asXXX));
        } else {
            assertEvalFastR(CREATE_TRUFFLE_OBJECT + asXXX + "(to$fieldFloatArray);", toRVector(t.fieldFloatArray, asXXX));
        }
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + asXXX + "(to$fieldIntegerArray);", toRVector(t.fieldIntegerArray, asXXX));
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + asXXX + "(to$fieldLongArray);", toRVector(t.fieldLongArray, asXXX));
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + asXXX + "(to$fieldShortArray);", toRVector(t.fieldShortArray, asXXX));
        if (!(asXXX.equals("as.character") || asXXX.equals("as.expression") || asXXX.equals("as.logical") || asXXX.equals("as.symbol") || asXXX.equals("as.vector"))) {
            assertEvalFastR(Output.IgnoreWarningMessage, CREATE_TRUFFLE_OBJECT + asXXX + "(to$fieldStringArray);", toRVector(t.fieldStringArray, asXXX));
        } else {
            assertEvalFastR(CREATE_TRUFFLE_OBJECT + asXXX + "(to$fieldStringArray);", toRVector(t.fieldStringArray, asXXX));
        }

        assertEvalFastR(CREATE_TRUFFLE_OBJECT + asXXX + "(to$fieldStringIntArray);", toRVector(t.fieldStringIntArray, asXXX));
        if (!(asXXX.equals("as.complex") || asXXX.equals("as.integer") || asXXX.equals("as.raw") || asXXX.equals("as.double"))) {
            assertEvalFastR(CREATE_TRUFFLE_OBJECT + asXXX + "(to$fieldStringBooleanArray);", toRVector(t.fieldStringBooleanArray, asXXX));
        } else {
            assertEvalFastR(Output.IgnoreWarningContext, CREATE_TRUFFLE_OBJECT + asXXX + "(to$fieldStringBooleanArray);", toRVector(t.fieldStringBooleanArray, asXXX));
        }

        // tests lists
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + asXXX + "(to$listBoolean);", toRVector(t.listBoolean, asXXX));
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + asXXX + "(to$listByte);", toRVector(t.listByte, asXXX));
        if (!(asXXX.equals("as.character") || asXXX.equals("as.expression") || asXXX.equals("as.logical") || asXXX.equals("as.symbol") || asXXX.equals("as.vector"))) {
            assertEvalFastR(Output.IgnoreWarningMessage, CREATE_TRUFFLE_OBJECT + asXXX + "(to$listChar);", toRVector(t.listChar, asXXX));
        } else {
            assertEvalFastR(CREATE_TRUFFLE_OBJECT + asXXX + "(to$listChar);", toRVector(t.listChar, asXXX));
        }
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + asXXX + "(to$listDouble);", toRVector(t.listDouble, asXXX));
        if (asXXX.equals("as.symbol")) {
            assertEvalFastR(Ignored.ImplementationError, CREATE_TRUFFLE_OBJECT + asXXX + "(to$listFloat);", toRVector(t.listFloat, asXXX));
        } else {
            assertEvalFastR(CREATE_TRUFFLE_OBJECT + asXXX + "(to$listFloat);", toRVector(t.listFloat, asXXX));
        }
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + asXXX + "(to$listInteger);", toRVector(t.listInteger, asXXX));
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + asXXX + "(to$listLong);", toRVector(t.listLong, asXXX));
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + asXXX + "(to$listShort);", toRVector(t.listShort, asXXX));
        if (!(asXXX.equals("as.character") || asXXX.equals("as.expression") || asXXX.equals("as.logical") || asXXX.equals("as.symbol") || asXXX.equals("as.vector"))) {
            assertEvalFastR(Output.IgnoreWarningContext, CREATE_TRUFFLE_OBJECT + asXXX + "(to$listString);", toRVector(t.listString, asXXX));
        } else {
            assertEvalFastR(CREATE_TRUFFLE_OBJECT + asXXX + "(to$listString);", toRVector(t.listString, asXXX));
        }

        assertEvalFastR(CREATE_TRUFFLE_OBJECT + asXXX + "(to$listStringInt);", toRVector(t.listStringInt, asXXX));
        if (!(asXXX.equals("as.complex") || asXXX.equals("as.integer") || asXXX.equals("as.raw") || asXXX.equals("as.double"))) {
            assertEvalFastR(CREATE_TRUFFLE_OBJECT + asXXX + "(to$listStringBoolean);", toRVector(t.listStringBoolean, asXXX));
        } else {
            assertEvalFastR(Output.IgnoreWarningContext, CREATE_TRUFFLE_OBJECT + asXXX + "(to$listStringBoolean);", toRVector(t.listStringBoolean, asXXX));
        }

        if (asXXX.equals("as.expression")) {
            assertEvalFastR(Output.IgnoreErrorContext, CREATE_TRUFFLE_OBJECT + asXXX + "(to);", errorIn(asXXX + "(to)", "no method for coercing this external object to a vector"));
        } else if (asXXX.equals("as.raw") || asXXX.equals("as.complex")) {
            assertEvalFastR(CREATE_TRUFFLE_OBJECT + asXXX + "(to);", errorIn(asXXX + "(to)", "cannot coerce type 'truffleobject' to vector of type '" + type + "'"));
        } else {
            assertEvalFastR(CREATE_TRUFFLE_OBJECT + asXXX + "(to);", errorIn(asXXX + "(to)", "no method for coercing this external object to a vector"));
        }
    }

    @Test
    public void testIf() throws IllegalArgumentException {
        TestClass t = new TestClass();

        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "if(to$fieldBoolean) print('OK')", "if(T) print('OK')");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "if(to$fieldInteger) print('OK')", "if(1) print('OK')");

        assertEvalFastR(Output.IgnoreWarningContext, CREATE_TRUFFLE_OBJECT + "if(to$fieldBooleanArray) print('OK')", "if(c(T, F)) print('OK')");
        assertEvalFastR(Output.IgnoreWarningContext, CREATE_TRUFFLE_OBJECT + "if(to$fieldIntegerArray) print('OK')", "if(c(T, F)) print('OK')");
        assertEvalFastR(Output.IgnoreWarningContext, Output.IgnoreErrorContext, CREATE_TRUFFLE_OBJECT + "if(to$fieldStringArray) print('OK')", "if(c('a', 'b')) print('OK')");

        assertEvalFastR(Output.IgnoreWarningContext, CREATE_TRUFFLE_OBJECT + "if(to$fieldStringBooleanArray) print('OK')", "if(c('TRUE', 'TRUE', 'FALSE')) print('OK')");
        assertEvalFastR(Output.IgnoreWarningContext, CREATE_TRUFFLE_OBJECT + "if(to$listBoolean) print('OK')", "if(c(T, F)) print('OK')");
        assertEvalFastR(Output.IgnoreWarningContext, CREATE_TRUFFLE_OBJECT + "if(to$listInteger) print('OK')", "if(c(T, F)) print('OK')");
        assertEvalFastR(Output.IgnoreErrorContext, CREATE_TRUFFLE_OBJECT + "if(to$listString) print('OK')", "if(c('A', 'B')) print('OK')");

        assertEvalFastR(Output.IgnoreWarningContext, CREATE_TRUFFLE_OBJECT + "if(to$listStringBoolean) print('OK')", "if(" + toRVector(t.listStringBoolean, null) + ") print('OK')");

        assertEvalFastR(Output.IgnoreErrorContext, CREATE_TRUFFLE_OBJECT + "if(to) print('OK')", errorIn("if (T) print('OK')", " argument is not interpretable as logical"));
    }

    @Test
    public void testWhile() throws IllegalArgumentException {
        TestClass t = new TestClass();

        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "while(to$fieldBoolean) {print('OK'); break;}", "while(T) {print('OK'); break;}");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "while(to$fieldInteger) {print('OK'); break;}", "while(1) {print('OK'); break;}");

        assertEvalFastR(Output.IgnoreWarningContext, CREATE_TRUFFLE_OBJECT + "while(to$fieldBooleanArray) {print('OK'); break;}", "while(c(T, F)) {print('OK'); break;}");
        assertEvalFastR(Output.IgnoreWarningContext, CREATE_TRUFFLE_OBJECT + "while(to$fieldIntegerArray) {print('OK'); break;}", "while(c(T, F)) {print('OK'); break;}");
        assertEvalFastR(Output.IgnoreWarningContext, Output.IgnoreErrorContext, CREATE_TRUFFLE_OBJECT + "if(to$fieldStringArray) {print('OK'); break;}", "while(c('a', 'b')) {print('OK'); break;}");

        assertEvalFastR(Output.IgnoreWarningContext, CREATE_TRUFFLE_OBJECT + "while(to$fieldStringBooleanArray) {print('OK'); break;}", "while(c('TRUE', 'TRUE', 'FALSE')) {print('OK'); break;}");
        assertEvalFastR(Output.IgnoreWarningContext, CREATE_TRUFFLE_OBJECT + "while(to$listBoolean) {print('OK'); break;}", "while(c(T, F)) {print('OK'); break;}");
        assertEvalFastR(Output.IgnoreWarningContext, CREATE_TRUFFLE_OBJECT + "while(to$listInteger) {print('OK'); break;}", "while(c(T, F)) {print('OK'); break;}");
        assertEvalFastR(Output.IgnoreErrorContext, CREATE_TRUFFLE_OBJECT + "while(to$listString) {print('OK'); break;}",
                        "while(c('A', 'B')) {print('OK'); break;}");

        assertEvalFastR(Output.IgnoreWarningContext, CREATE_TRUFFLE_OBJECT + "while(to$listStringBoolean) {print('OK'); break;}",
                        "while(" + toRVector(t.listStringBoolean, null) + ") {print('OK'); break;}");

        assertEvalFastR(Output.IgnoreErrorContext, CREATE_TRUFFLE_OBJECT + "while(to) print('OK')", errorIn("if (T) print('OK')", " argument is not interpretable as logical"));
    }

    @Test
    public void testForeignVectorArithmeticOp() throws NoSuchFieldException,
                    IllegalAccessException {
        TestJavaInterop.this.testForeignVectorArithmeticOp("fieldBooleanArray", false);
        TestJavaInterop.this.testForeignVectorArithmeticOp("fieldByteArray", false);
        TestJavaInterop.this.testForeignVectorArithmeticOp("fieldDoubleArray", false);
        TestJavaInterop.this.testForeignVectorArithmeticOp("fieldFloatArray", false);
        TestJavaInterop.this.testForeignVectorArithmeticOp("fieldIntegerArray", false);
        TestJavaInterop.this.testForeignVectorArithmeticOp("fieldLongArray", false);
        TestJavaInterop.this.testForeignVectorArithmeticOp("fieldShortArray", false);

        TestJavaInterop.this.testForeignVectorArithmeticOp("listBoolean", false);
        TestJavaInterop.this.testForeignVectorArithmeticOp("listByte", false);
        TestJavaInterop.this.testForeignVectorArithmeticOp("listDouble", false);
        TestJavaInterop.this.testForeignVectorArithmeticOp("listFloat", false);
        TestJavaInterop.this.testForeignVectorArithmeticOp("listInteger", false);
        TestJavaInterop.this.testForeignVectorArithmeticOp("listLong", false);
        TestJavaInterop.this.testForeignVectorArithmeticOp("listShort", false);

        TestJavaInterop.this.testForeignVectorArithmeticOp("fieldCharArray", true);
        TestJavaInterop.this.testForeignVectorArithmeticOp("fieldStringArray", true);
        TestJavaInterop.this.testForeignVectorArithmeticOp("listString", true);
        TestJavaInterop.this.testForeignVectorArithmeticOp("listStringInt", true);
        TestJavaInterop.this.testForeignVectorArithmeticOp("listChar", true);

        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "to + 1", errorIn("to + 1", "non-numeric argument to binary operator"));
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "1 + to", errorIn("1 + to", "non-numeric argument to binary operator"));
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "to + to", errorIn("to + to", "non-numeric argument to binary operator"));
    }

    private void testForeignVectorArithmeticOp(String vec, boolean fail) throws NoSuchFieldException, IllegalAccessException {
        TestClass t = new TestClass();

        String expectedOK;
        String expectedKO;

        expectedOK = toRVector(t, vec) + " + 1";
        expectedKO = errorIn("to$" + vec + " + 1", "non-numeric argument to binary operator");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "to$" + vec + " + 1", fail ? expectedKO : expectedOK);

        expectedOK = toRVector(t, vec) + " + c(1, 2, 3)";
        expectedKO = errorIn("to$" + vec + " + c(1, 2, 3)", "non-numeric argument to binary operator");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "to$" + vec + " + c(1, 2, 3)", fail ? expectedKO : expectedOK);

        expectedOK = "1 + " + toRVector(t, vec);
        expectedKO = errorIn("1 + to$" + vec, "non-numeric argument to binary operator");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "1 + to$" + vec, fail ? expectedKO : expectedOK);

        expectedOK = "c(1, 2, 3) + " + toRVector(t, vec);
        expectedKO = errorIn("c(1, 2, 3) + to$" + vec + "", "non-numeric argument to binary operator");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " c(1, 2, 3) + to$" + vec, fail ? expectedKO : expectedOK);

        expectedOK = toRVector(t, vec) + " + " + toRVector(t, vec);
        expectedKO = errorIn("to$" + vec + " + to$" + vec + "", "non-numeric argument to binary operator");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "to$" + vec + " + to$" + vec, fail ? expectedKO : expectedOK);

        expectedOK = "-" + toRVector(t, vec);
        expectedKO = errorIn("-(to$" + vec + ")", "invalid argument to unary operator");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "-(to$" + vec + ")", fail ? expectedKO : expectedOK);

        expectedOK = "numeric(0)";
        expectedKO = errorIn("to$" + vec + " + NULL", "non-numeric argument to binary operator");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "to$" + vec + " + NULL", fail ? expectedKO : expectedOK);

        expectedOK = "numeric(0)";
        expectedKO = errorIn("NULL + to$" + vec, "non-numeric argument to binary operator");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "NULL + to$" + vec, fail ? expectedKO : expectedOK);
    }

    @Test
    public void testForeignVectorBooleanOp() throws NoSuchFieldException,
                    IllegalAccessException {
        testForeignVectorBooleanOp("fieldBooleanArray", false);
        testForeignVectorBooleanOp("fieldByteArray", false);
        testForeignVectorBooleanOp("fieldDoubleArray", false);
        testForeignVectorBooleanOp("fieldFloatArray", false);
        testForeignVectorBooleanOp("fieldIntegerArray", false);
        testForeignVectorBooleanOp("fieldLongArray", false);
        testForeignVectorBooleanOp("fieldShortArray", false);

        testForeignVectorBooleanOp("listBoolean", false);
        testForeignVectorBooleanOp("listByte", false);
        testForeignVectorBooleanOp("listDouble", false);
        testForeignVectorBooleanOp("listFloat", false);
        testForeignVectorBooleanOp("listInteger", false);
        testForeignVectorBooleanOp("listLong", false);
        testForeignVectorBooleanOp("listShort", false);

        testForeignVectorBooleanOp("fieldCharArray", true);
        testForeignVectorBooleanOp("fieldStringArray", true);
        testForeignVectorBooleanOp("listString", true);
        testForeignVectorBooleanOp("listStringInt", true);
        testForeignVectorBooleanOp("listChar", true);

        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "to & T", errorIn("to & T", "operations are possible only for numeric, logical or complex types"));
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "T & to", errorIn("T & to", "operations are possible only for numeric, logical or complex types"));
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "to & to", errorIn("to & to", "operations are possible only for numeric, logical or complex types"));
    }

    private void testForeignVectorBooleanOp(String vec, boolean fail) throws NoSuchFieldException, IllegalAccessException {
        TestClass t = new TestClass();

        String expectedOK;
        String expectedKO;

        expectedOK = toRVector(t, vec) + " & T";
        expectedKO = errorIn("to$" + vec + " & T", "operations are possible only for numeric, logical or complex types");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "to$" + vec + " & T", fail ? expectedKO : expectedOK);

        expectedOK = toRVector(t, vec) + " & c(T, T, F)";
        expectedKO = errorIn("to$" + vec + " & c(T, T, F)", "operations are possible only for numeric, logical or complex types");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "to$" + vec + " & c(T, T, F)", fail ? expectedKO : expectedOK);

        expectedOK = "T & " + toRVector(t, vec);
        expectedKO = errorIn("T & to$" + vec, "operations are possible only for numeric, logical or complex types");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "T & to$" + vec, fail ? expectedKO : expectedOK);

        expectedOK = "c(T, T, F) & " + toRVector(t, vec);
        expectedKO = errorIn("c(T, T, F) & to$" + vec + "", "operations are possible only for numeric, logical or complex types");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " c(T, T, F) & to$" + vec, fail ? expectedKO : expectedOK);

        expectedOK = toRVector(t, vec) + " & " + toRVector(t, vec);
        expectedKO = errorIn("to$" + vec + " & to$" + vec + "", "operations are possible only for numeric, logical or complex types");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "to$" + vec + " & to$" + vec, fail ? expectedKO : expectedOK);

        expectedOK = "!" + toRVector(t, vec);
        expectedKO = errorIn("!(to$" + vec + ")", "invalid argument type");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "!(to$" + vec + ")", fail ? expectedKO : expectedOK);

        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "to$" + vec + " & NULL", "logical(0)");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "NULL & to$" + vec, "logical(0)");
    }

    @Test
    public void testForeignVectorScalarBooleanOp() throws NoSuchFieldException,
                    IllegalAccessException {
        testForeignVectorScalarBooleanOp("||");
        testForeignVectorScalarBooleanOp("&&");
    }

    private void testForeignVectorScalarBooleanOp(String operator) throws NoSuchFieldException,
                    IllegalAccessException {
        testForeignVectorScalarBooleanOp(operator, "fieldBooleanArray", false);
        testForeignVectorScalarBooleanOp(operator, "fieldByteArray", false);
        testForeignVectorScalarBooleanOp(operator, "fieldDoubleArray", false);
        testForeignVectorScalarBooleanOp(operator, "fieldFloatArray", false);
        testForeignVectorScalarBooleanOp(operator, "fieldIntegerArray", false);
        testForeignVectorScalarBooleanOp(operator, "fieldLongArray", false);
        testForeignVectorScalarBooleanOp(operator, "fieldShortArray", false);

        testForeignVectorScalarBooleanOp(operator, "listBoolean", false);
        testForeignVectorScalarBooleanOp(operator, "listByte", false);
        testForeignVectorScalarBooleanOp(operator, "listDouble", false);
        testForeignVectorScalarBooleanOp(operator, "listFloat", false);
        testForeignVectorScalarBooleanOp(operator, "listInteger", false);
        testForeignVectorScalarBooleanOp(operator, "listLong", false);
        testForeignVectorScalarBooleanOp(operator, "listShort", false);

        testForeignVectorScalarBooleanOp(operator, "fieldCharArray", true);
        testForeignVectorScalarBooleanOp(operator, "fieldStringArray", true);
        testForeignVectorScalarBooleanOp(operator, "listString", true);
        testForeignVectorScalarBooleanOp(operator, "listStringInt", true);
        testForeignVectorScalarBooleanOp(operator, "listChar", true);

        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "to " + operator + " T", errorIn("to " + operator + " T", "invalid 'x' type in 'x " + operator + " y'"));
        String cmd = CREATE_TRUFFLE_OBJECT + "T " + operator + " to";
        if (operator.equals("||")) {
            assertEvalFastR(cmd, "TRUE");
        } else {
            assertEvalFastR(cmd, errorIn("T " + operator + " to", "invalid 'y' type in 'x " + operator + " y'"));
        }
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "to " + operator + " to", errorIn("to " + operator + " to", "invalid 'x' type in 'x " + operator + " y'"));
    }

    private void testForeignVectorScalarBooleanOp(String operator, String vec, boolean fail) throws NoSuchFieldException, IllegalAccessException {
        TestClass t = new TestClass();

        String expectedOK;
        String expectedKO;

        expectedOK = toRVector(t, vec) + operator + " T";
        expectedKO = errorIn("to$" + vec + " " + operator + " T", "invalid 'x' type in 'x " + operator + " y'");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "to$" + vec + " " + operator + " T", fail ? expectedKO : expectedOK);

        expectedOK = toRVector(t, vec) + operator + " c(T, T, F)";
        expectedKO = errorIn("to$" + vec + " " + operator + " c(T, T, F)", "invalid 'x' type in 'x " + operator + " y'");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "to$" + vec + " " + operator + " c(T, T, F)", fail ? expectedKO : expectedOK);

        expectedOK = "T " + operator + " " + toRVector(t, vec);
        expectedKO = errorIn("T " + operator + " to$" + vec + "", "invalid 'y' type in 'x " + operator + " y'");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "T " + operator + " to$" + vec, fail && !operator.equals("||") ? expectedKO : expectedOK);

        expectedOK = "c(T, T, F) " + operator + " " + toRVector(t, vec);
        expectedKO = errorIn("c(T, T, F) " + operator + " to$" + vec + "", "invalid 'y' type in 'x " + operator + " y'");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " c(T, T, F) " + operator + " to$" + vec, fail && !operator.equals("||") ? expectedKO : expectedOK);

        expectedOK = toRVector(t, vec) + " " + operator + " " + toRVector(t, vec);
        expectedKO = errorIn("to$" + vec + " " + operator + " to$" + vec, "invalid 'x' type in 'x " + operator + " y'");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "to$" + vec + " " + operator + " to$" + vec, fail ? expectedKO : expectedOK);

        String cmd = CREATE_TRUFFLE_OBJECT + "to$" + vec + " " + operator + " NULL";
        if (operator.equals("||") && !fail) {
            assertEvalFastR(cmd, "TRUE");
        } else {
            assertEvalFastR(cmd, errorIn("to$" + vec + " " + operator + " NULL", "invalid " + (fail ? "'x'" : "'y'") + " type in 'x " + operator + " y'"));
        }
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "NULL " + operator + " to$" + vec, errorIn("NULL " + operator + " to$" + vec, "invalid 'x' type in 'x " + operator + " y'"));
    }

    @Test
    public void testForeignUnaryArithmeticOp() {

        TestClass t = new TestClass();

        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "abs(to$fieldBooleanArray)", "c(1, 0, 1)");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "abs(to$fieldByteArray)", toRVector(t.fieldByteArray, null));
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "abs(to$fieldDoubleArray)", toRVector(t.fieldDoubleArray, null));
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "abs(to$fieldFloatArray)", toRVector(t.fieldFloatArray, null));
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "abs(to$fieldIntegerArray)", toRVector(t.fieldIntegerArray, null));
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "abs(to$fieldLongArray)", toRVector(t.fieldLongArray, null));
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "abs(to$fieldShortArray)", toRVector(t.fieldShortArray, null));

        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "abs(to$listBoolean)", "c(1, 0, 1)");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "abs(to$listByte)", toRVector(t.listByte, null));
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "abs(to$listDouble)", toRVector(t.listDouble, null));
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "abs(to$listFloat)", toRVector(t.listFloat, null));
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "abs(to$listInteger)", toRVector(t.listInteger, null));
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "abs(to$listLong)", toRVector(t.listLong, null));
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "abs(to$listShort)", toRVector(t.listShort, null));

        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "abs(to$fieldCharArray)", errorIn("abs(to$fieldCharArray)", "non-numeric argument to mathematical function"));
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "abs(to$fieldStringArray)", errorIn("abs(to$fieldStringArray)", "non-numeric argument to mathematical function"));
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "abs(to$listString)", errorIn("abs(to$listString)", "non-numeric argument to mathematical function"));
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "abs(to$listStringInt)", errorIn("abs(to$listStringInt)", "non-numeric argument to mathematical function"));
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "abs(to$listChar)", errorIn("abs(to$listChar)", "non-numeric argument to mathematical function"));

        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "abs(to)", errorIn("abs(to)", "non-numeric argument to mathematical function"));
    }

    @Test
    public void testForeignUnaryArithmeticReduceOp() {
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "range(to$fieldBooleanArray)", "c(0, 1)");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "range(to$fieldByteArray)", "c(1, 3)");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "range(to$fieldDoubleArray)", "c(1.1, 3.1)");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "range(to$fieldFloatArray)", "c(1.1, 3.1)");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "range(to$fieldIntegerArray)", "c(1, 3)");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "range(to$fieldLongArray)", "c(1, 3)");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "range(to$fieldShortArray)", "c(1, 3)");

        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "range(to$listBoolean)", "c(0, 1)");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "range(to$listByte)", "c(1, 3)");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "range(to$listDouble)", "c(1.1, 3.1)");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "range(to$listFloat)", "c(1.1, 3.1)");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "range(to$listInteger)", "c(1, 3)");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "range(to$listLong)", "c(1, 3)");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "range(to$listShort)", "c(1, 3)");

        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "range(to$fieldCharArray)", "c('a', 'c')");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "range(to$fieldStringArray)", "c('a', 'c')");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "range(to$listString)", "c('a', 'c')");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "range(to$listStringInt)", "c('1', '3')");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "range(to$listChar)", "c('a', 'c')");

        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "range(to)", errorIn("min(x, na.rm = na.rm)", "invalid 'type' (list) of argument"));
    }

    private String getRValue(Object value) {
        if (value == null) {
            return "NULL";
        }
        if (value instanceof Boolean) {
            return value.toString().toUpperCase();
        }
        if (value instanceof Double) {
            if (((Double) value) == (((Double) value).intValue())) {
                return Integer.toString(((Double) value).intValue());
            }
        }
        if (value instanceof String) {
            return "\"" + value.toString() + "\"";
        }
        if (value instanceof String) {
            return "\"" + value.toString() + "\"";
        }
        if (value instanceof Character) {
            return "\"" + ((Character) value).toString() + "\"";
        }
        if (value.getClass().isArray()) {
            StringBuilder sb = new StringBuilder();
            sb.append("cat('[external object]\\n[1] ");
            int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                if (length > 1) {
                    // what the heck?
                    sb.append(getBooleanPrefix(value, i));
                }
                sb.append(getRValue(Array.get(value, i)));
                if (i < length - 1) {
                    sb.append(" ");
                }
            }
            sb.append("\\n')");
            return sb.toString();
        }
        return value.toString();
    }

    private String toRVector(TestClass t, String fieldName) throws NoSuchFieldException, IllegalAccessException {
        Field fld = t.getClass().getDeclaredField(fieldName);
        return toRVector(fld.get(t), null);
    }

    private String toRVector(Object o, String asXXX) {
        return toRVector(list(o), asXXX);
    }

    private String toRVector(Object[] l, String asXXX) {
        return toRVector(Arrays.asList(l), asXXX);
    }

    private String c(Object a1, Object a2) {
        List<Object> l = new ArrayList<>();
        List<?> l1 = list(a1);
        List<?> l2 = list(a2);
        l.addAll(l1);
        l.addAll(l2);
        return toRVector(l, null);
    }

    private List<?> list(Object o) {
        if (o.getClass().isArray()) {
            List<Object> l = new ArrayList<>();
            for (int i = 0; i < Array.getLength(o); i++) {
                l.add(Array.get(o, i));
            }
            return l;
        } else if (o instanceof List) {
            return (List<?>) o;
        }
        Assert.fail(o + " should have been an array or list");
        return null;
    }

    private String toRVector(List<?> l, String asXXX) {
        StringBuilder sb = new StringBuilder();
        if (asXXX != null) {
            sb.append(asXXX);
            sb.append("(c(");
        } else {
            sb.append("c(");
        }
        Iterator<?> it = l.iterator();
        while (it.hasNext()) {
            Object o = it.next();
            if (asXXX != null) {
                if (asXXX.equals("as.character") && (o instanceof Double || o instanceof Float)) {
                    o = DoubleVectorPrinter.encodeReal(((Number) o).doubleValue());
                }
                sb.append(getRValue(o));
            } else {
                sb.append(getRValue(o));
            }
            if (it.hasNext()) {
                sb.append(',');
            }
        }
        if (asXXX != null) {
            sb.append("))");
        } else {
            sb.append(")");
        }
        return sb.toString();
    }

    private String getAsXXX(Object o, String asXXX) {
        StringBuilder sb = new StringBuilder();
        sb.append(asXXX);
        sb.append("(");
        Object val = o;
        if (asXXX.equals("as.character") && (o instanceof Double || o instanceof Float)) {
            val = DoubleVectorPrinter.encodeReal(((Number) o).doubleValue());
        }
        sb.append(getRValue(val));
        sb.append(')');
        return sb.toString();
    }

    private String getRValuesAsString(Object... values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            Object v = values[i];
            sb.append(getRValue(v));
            if (i < values.length - 1) {
                sb.append(",");
            }
        }
        return sb.toString();
    }

    private static String getBooleanPrefix(Object array, int i) {
        Object element = Array.get(array, i);
        if (element == null) {
            return "";
        }
        if (array.getClass().getComponentType() == Boolean.TYPE && (boolean) element) {
            return " ";
        }
        if (i > 0 && array.getClass().getComponentType() == String.class &&
                        (element.equals("T") || element.equals("F") || element.equals("TRUE") || element.equals("FALSE"))) {
            return " ";
        }
        return "";
    }

    private static String toArrayClassName(String className, int dims) {
        StringBuilder sb = new StringBuilder();
        sb.append("'");
        for (int i = 0; i < dims; i++) {
            sb.append("[");
        }
        switch (className) {
            case "boolean":
                sb.append("Z");
                break;
            case "byte":
                sb.append("B");
                break;
            case "char":
                sb.append("C");
                break;
            case "double":
                sb.append("D");
                break;
            case "float":
                sb.append("F");
                break;
            case "int":
                sb.append("I");
                break;
            case "long":
                sb.append("J");
                break;
            case "short":
                sb.append("S");
                break;
            default:
                sb.append('L');
                sb.append(className);
                sb.append(';');
        }
        sb.append('\'');
        return sb.toString();
    }

    private String errorIn(String left, String right) {
        String errorIn = "Error in ";
        String delim = " :";

        StringBuilder sb = new StringBuilder();
        sb.append("cat('");
        sb.append(errorIn);
        sb.append(left.replaceAll("\\'", "\\\\\\'"));
        sb.append(delim);
        if (errorIn.length() + left.length() + delim.length() + 1 + right.length() >= 74) {
            sb.append("', '\n', '");
        }
        sb.append(' ');
        sb.append(right.replaceAll("\\'", "\\\\\\'"));
        sb.append("', '\n')");
        return sb.toString();
    }

    public static class TestNamesClass {
        public Object field;
        public static Object staticField;

        public void method() {
        }

        public static void staticMethod() {
        }
    }

    public static class TestNamesClassNoMembers {

    }

    public static class TestNamesClassNoPublicMembers {
        int i;
    }

    public static class TestNamesClassMap {
        public static Map<String, String> m() {
            HashMap<String, String> m = new HashMap<>();
            m.put("one", "1");
            m.put("two", "2");
            return m;
        }
    }

    public static class TestNullClass {
        public TestNullClass(Object o) {
            assert o == null;
        }
    }

    @SuppressWarnings("unused")
    public static class TestOverload {
        public String type;

        public TestOverload(boolean o) {
            type = boolean.class.getName();
        }

        public TestOverload(Boolean o) {
            type = o.getClass().getName();
        }

        public TestOverload(byte o) {
            type = byte.class.getName();
        }

        public TestOverload(Byte o) {
            type = o.getClass().getName();
        }

        public TestOverload(char o) {
            type = char.class.getName();
        }

        public TestOverload(Character o) {
            type = o.getClass().getName();
        }

        public TestOverload(double o) {
            type = double.class.getName();
        }

        public TestOverload(Double o) {
            type = o.getClass().getName();
        }

        public TestOverload(int o) {
            type = int.class.getName();
        }

        public TestOverload(Integer o) {
            type = o.getClass().getName();
        }

        public TestOverload(float o) {
            type = float.class.getName();
        }

        public TestOverload(Float o) {
            type = o.getClass().getName();
        }

        public TestOverload(long o) {
            type = long.class.getName();
        }

        public TestOverload(Long o) {
            type = o.getClass().getName();
        }

        public TestOverload(short o) {
            type = short.class.getName();
        }

        public TestOverload(Short o) {
            type = o.getClass().getName();
        }

        public TestOverload(String o) {
            type = o.getClass().getName();
        }

        public static String isOverloaded(boolean b) {
            return "boolean";
        }

        public static String isOverloaded(Boolean b) {
            return Boolean.class.getName();
        }

        public static String isOverloaded(byte b) {
            return "byte";
        }

        public static String isOverloaded(Byte b) {
            return Byte.class.getName();
        }

        public static String isOverloaded(char c) {
            return "char";
        }

        public static String isOverloaded(Character c) {
            return Character.class.getName();
        }

        public static String isOverloaded(double l) {
            return "double";
        }

        public static String isOverloaded(Double l) {
            return Double.class.getName();
        }

        public static String isOverloaded(Float f) {
            return Float.class.getName();
        }

        public static String isOverloaded(float f) {
            return "float";
        }

        public static String isOverloaded(int c) {
            return "int";
        }

        public static String isOverloaded(Integer c) {
            return Integer.class.getName();
        }

        public static String isOverloaded(long l) {
            return "long";
        }

        public static String isOverloaded(Long l) {
            return Long.class.getName();
        }

        public static String isOverloaded(short c) {
            return "short";
        }

        public static String isOverloaded(Short c) {
            return Short.class.getName();
        }

        public static String isOverloaded(String s) {
            return String.class.getName();
        }
    }

    @SuppressWarnings("unused")
    public static class TestClass {

        public static boolean fieldStaticBoolean = true;
        public static byte fieldStaticByte = Byte.MAX_VALUE;
        public static char fieldStaticChar = 'a';
        public static short fieldStaticShort = Short.MAX_VALUE;
        public static int fieldStaticInteger = Integer.MAX_VALUE;
        public static long fieldStaticLong = Long.MAX_VALUE;
        public static double fieldStaticDouble = Double.MAX_VALUE;
        public static float fieldStaticFloat = Float.MAX_VALUE;

        public static Boolean fieldStaticBooleanObject = fieldStaticBoolean;
        public static Byte fieldStaticByteObject = fieldStaticByte;
        public static Character fieldStaticCharObject = fieldStaticChar;
        public static Short fieldStaticShortObject = fieldStaticShort;
        public static Integer fieldStaticIntegerObject = fieldStaticInteger;
        public static Long fieldStaticLongObject = fieldStaticLong;
        public static Double fieldStaticDoubleObject = fieldStaticDouble;
        public static Float fieldStaticFloatObject = fieldStaticFloat;
        public static String fieldStaticStringObject = "a string";

        public boolean fieldBoolean;
        public byte fieldByte;
        public char fieldChar;
        public short fieldShort;
        public int fieldInteger;
        public long fieldLong;
        public double fieldDouble;
        public float fieldFloat;

        public Boolean fieldBooleanObject;
        public Byte fieldByteObject;
        public Character fieldCharObject;
        public Short fieldShortObject;
        public Integer fieldIntegerObject;
        public Long fieldLongObject;
        public Double fieldDoubleObject;
        public Float fieldFloatObject;
        public String fieldStringObject;

        public static boolean[] fieldStaticBooleanArray;
        public static byte[] fieldStaticByteArray;
        public static char[] fieldStaticCharArray;
        public static double[] fieldStaticDoubleArray;
        public static float[] fieldStaticFloatArray;
        public static int[] fieldStaticIntegerArray;
        public static long[] fieldStaticLongArray;
        public static short[] fieldStaticShortArray;
        public static String[] fieldStaticStringArray;

        public boolean[] fieldBooleanArray = fieldStaticBooleanArray;
        public byte[] fieldByteArray = fieldStaticByteArray;
        public char[] fieldCharArray = fieldStaticCharArray;
        public double[] fieldDoubleArray = fieldStaticDoubleArray;
        public float[] fieldFloatArray = fieldStaticFloatArray;
        public int[] fieldIntegerArray = fieldStaticIntegerArray;
        public long[] fieldLongArray = fieldStaticLongArray;
        public short[] fieldShortArray = fieldStaticShortArray;
        public String[] fieldStringArray = fieldStaticStringArray;
        public String[] fieldStringIntArray = new String[]{"1", "2", "3"};
        public String[] fieldStringBooleanArray = new String[]{"TRUE", "TRUE", "FALSE"};

        public int[][] int2DimArray;
        public Object[] objectArray;
        public Object[] objectIntArray;
        public Object[] objectDoubleArray;
        public Object[] mixedTypesArray;
        public Integer[] hasNullIntArray;

        public static Double fieldStaticNaNObject = Double.NaN;
        public static double fieldStaticNaN = Double.NaN;

        public static Object fieldStaticNullObject = null;
        public Object fieldNullObject = null;

        public List<Boolean> listBoolean = new ArrayList<>(Arrays.asList(true, false, true));
        public List<Byte> listByte = new ArrayList<>(Arrays.asList((byte) 1, (byte) 2, (byte) 3));
        public List<Character> listChar = new ArrayList<>(Arrays.asList('a', 'b', 'c'));
        public List<Double> listDouble = new ArrayList<>(Arrays.asList(1.1, 2.1, 3.1));
        public List<Float> listFloat = new ArrayList<>(Arrays.asList(1.1f, 2.1f, 3.1f));
        public List<Integer> listInteger = new ArrayList<>(Arrays.asList(1, 2, 3));
        public List<Long> listLong = new ArrayList<>(Arrays.asList(1L, 2L, 3L));
        public List<Short> listShort = new ArrayList<>(Arrays.asList((short) 1, (short) 2, (short) 3));
        public List<String> listString = new ArrayList<>(Arrays.asList("a", "b", "c"));
        public List<String> listStringInt = new ArrayList<>(Arrays.asList("1", "2", "3"));
        public List<String> listStringBoolean = new ArrayList<>(Arrays.asList("TRUE", "TRUE", "FALSE"));
        public List<String> listEmpty = new ArrayList<>();

        public static class Element {
            public final String data;

            public Element(String data) {
                this.data = data;
            }
        }

        public List<Element> listObject = new ArrayList<>(Arrays.asList(new Element("a"), new Element("b"), new Element("c"), null));
        public Element[] arrayObject = new Element[]{new Element("a"), new Element("b"), new Element("c"), null};

        public Map<String, String> map;

        public TestClass() {
            this(true, Byte.MAX_VALUE, 'a', Double.MAX_VALUE, 1.1f, Integer.MAX_VALUE, Long.MAX_VALUE, Short.MAX_VALUE, "a string");
        }

        public TestClass(boolean bo, byte bt, char c, double d, float f, int i, long l, short sh, String st) {
            fieldStaticBoolean = bo;
            fieldStaticByte = bt;
            fieldStaticChar = c;
            fieldStaticDouble = d;
            fieldStaticFloat = f;
            fieldStaticInteger = i;
            fieldStaticLong = l;
            fieldStaticShort = sh;
            fieldStaticStringObject = st;

            fieldStaticBooleanObject = fieldStaticBoolean;
            fieldStaticByteObject = fieldStaticByte;
            fieldStaticCharObject = fieldStaticChar;
            fieldStaticShortObject = fieldStaticShort;
            fieldStaticIntegerObject = fieldStaticInteger;
            fieldStaticLongObject = fieldStaticLong;
            fieldStaticDoubleObject = fieldStaticDouble;
            fieldStaticFloatObject = fieldStaticFloat;

            this.fieldBoolean = fieldStaticBoolean;
            this.fieldByte = fieldStaticByte;
            this.fieldChar = fieldStaticChar;
            this.fieldShort = fieldStaticShort;
            this.fieldInteger = fieldStaticInteger;
            this.fieldLong = fieldStaticLong;
            this.fieldDouble = fieldStaticDouble;
            this.fieldFloat = fieldStaticFloat;

            this.fieldBooleanObject = fieldBoolean;
            this.fieldByteObject = fieldByte;
            this.fieldCharObject = fieldChar;
            this.fieldShortObject = fieldShort;
            this.fieldIntegerObject = fieldInteger;
            this.fieldLongObject = fieldLong;
            this.fieldDoubleObject = fieldDouble;
            this.fieldFloatObject = fieldFloat;
            this.fieldStringObject = fieldStaticStringObject;

            fieldStaticBooleanArray = new boolean[]{true, false, true};
            fieldStaticByteArray = new byte[]{1, 2, 3};
            fieldStaticCharArray = new char[]{'a', 'b', 'c'};
            fieldStaticDoubleArray = new double[]{1.1, 2.1, 3.1};
            fieldStaticFloatArray = new float[]{1.1f, 2.1f, 3.1f};
            fieldStaticIntegerArray = new int[]{1, 2, 3};
            fieldStaticLongArray = new long[]{1, 2, 3};
            fieldStaticShortArray = new short[]{1, 2, 3};
            fieldStaticStringArray = new String[]{"a", "b", "c"};

            fieldBooleanArray = fieldStaticBooleanArray;
            fieldByteArray = fieldStaticByteArray;
            fieldCharArray = fieldStaticCharArray;
            fieldDoubleArray = fieldStaticDoubleArray;
            fieldFloatArray = fieldStaticFloatArray;
            fieldIntegerArray = fieldStaticIntegerArray;
            fieldLongArray = fieldStaticLongArray;
            fieldShortArray = fieldStaticShortArray;
            fieldStringArray = fieldStaticStringArray;

            int2DimArray = new int[][]{new int[]{1, 2, 3}, new int[]{4, 5, 5}};
            objectArray = new Object[]{new Object(), new Object(), new Object()};
            objectIntArray = new Object[]{1, 2, 3};
            objectDoubleArray = new Object[]{1.1, 2.1, 3.1};
            mixedTypesArray = new Object[]{1, 2.1, 'a', true, null};
            hasNullIntArray = new Integer[]{1, null, 3};

            map = new HashMap<>();
            map.put("one", "1");
            map.put("two", "2");
            map.put("three", "3");
        }

        public static boolean methodStaticBoolean() {
            return fieldStaticBoolean;
        }

        public static byte methodStaticByte() {
            return fieldStaticByte;
        }

        public static char methodStaticChar() {
            return fieldStaticChar;
        }

        public static short methodStaticShort() {
            return fieldStaticShort;
        }

        public static int methodStaticInteger() {
            return fieldStaticInteger;
        }

        public static long methodStaticLong() {
            return fieldStaticLong;
        }

        public static double methodStaticDouble() {
            return fieldStaticDouble;
        }

        public static float methodStaticFloat() {
            return fieldStaticFloat;
        }

        public static String methodStaticStringObject() {
            return fieldStaticStringObject;
        }

        public static String[] methodStaticStringArray() {
            return fieldStaticStringArray;
        }

        public static int[] methodStaticIntArray() {
            return fieldStaticIntegerArray;
        }

        public boolean methodBoolean() {
            return fieldBoolean;
        }

        public byte methodByte() {
            return fieldByte;
        }

        public char methodChar() {
            return fieldChar;
        }

        public short methodShort() {
            return fieldShort;
        }

        public int methodInteger() {
            return fieldInteger;
        }

        public long methodLong() {
            return fieldLong;
        }

        public double methodDouble() {
            return fieldDouble;
        }

        public float methodFloat() {
            return fieldFloat;
        }

        public String methodStringObject() {
            return fieldStringObject;
        }

        public String[] methodStringArray() {
            return fieldStringArray;
        }

        public int[] methodIntArray() {
            return fieldIntegerArray;
        }

        public static Boolean methodStaticBooleanObject() {
            return fieldStaticBooleanObject;
        }

        public static Byte methodStaticByteObject() {
            return fieldStaticByteObject;
        }

        public static Character methodStaticCharObject() {
            return fieldStaticCharObject;
        }

        public static Short methodStaticShortObject() {
            return fieldStaticShortObject;
        }

        public static Integer methodStaticIntegerObject() {
            return fieldStaticIntegerObject;
        }

        public static Long methodStaticLongObject() {
            return fieldStaticLongObject;
        }

        public static Double methodStaticDoubleObject() {
            return fieldStaticDoubleObject;
        }

        public static Float methodStaticFloatObject() {
            return fieldStaticFloatObject;
        }

        public Boolean methodBooleanObject() {
            return fieldBoolean;
        }

        public Byte methodByteObject() {
            return fieldByteObject;
        }

        public Character methodCharObject() {
            return fieldCharObject;
        }

        public Short methodShortObject() {
            return fieldShortObject;
        }

        public Integer methodIntegerObject() {
            return fieldIntegerObject;
        }

        public Long methodLongObject() {
            return fieldLongObject;
        }

        public Double methodDoubleObject() {
            return fieldDoubleObject;
        }

        public Float methodFloatObject() {
            return fieldFloatObject;
        }

        public static Object methodStaticReturnsNull() {
            return null;
        }

        public Object methodReturnsNull() {
            return null;
        }

        public void methodAcceptsOnlyNull(Object o) {
            Assert.assertNull(o);
        }

        public String classAsArg(Class<?> c) {
            return c.getName();
        }

        public String allTypesMethod(boolean bo, byte bt, char ch, short sh, int in, long lo, double db, float fl, String st) {
            return "" + bo + bt + ch + sh + in + lo + db + fl + st;
        }

        public static Object allTypesStaticMethod(boolean bo, byte bt, char ch, short sh, int in, long lo, double db, float fl, String st) {
            return "" + bo + bt + ch + sh + in + lo + db + fl + st;
        }

        public String isNull(String s) {
            return s == null ? String.class.getName() : null;
        }

        public String isNull(Long l) {
            return l == null ? Long.class.getName() : null;
        }

        public boolean equals(TestClass tc) {
            return tc == this;
        }

        public String isOverloaded(boolean b) {
            return "boolean";
        }

        public String isOverloaded(Boolean b) {
            return Boolean.class.getName();
        }

        public String isOverloaded(byte b) {
            return "byte";
        }

        public String isOverloaded(Byte b) {
            return Byte.class.getName();
        }

        public String isOverloaded(char c) {
            return "char";
        }

        public String isOverloaded(Character c) {
            return Character.class.getName();
        }

        public String isOverloaded(double l) {
            return "double";
        }

        public String isOverloaded(Double l) {
            return Double.class.getName();
        }

        public String isOverloaded(Float f) {
            return Float.class.getName();
        }

        public String isOverloaded(float f) {
            return "float";
        }

        public String isOverloaded(int c) {
            return "int";
        }

        public String isOverloaded(Integer c) {
            return Integer.class.getName();
        }

        public String isOverloaded(long l) {
            return "long";
        }

        public String isOverloaded(Long l) {
            return Long.class.getName();
        }

        public String isOverloaded(short c) {
            return "short";
        }

        public String isOverloaded(Short c) {
            return Short.class.getName();
        }

        public String isOverloaded(String s) {
            return String.class.getName();
        }

        public String isIntArray(int[] a) {
            return a.getClass().getName();
        }

        public String isIntegerArray(Integer[] a) {
            return a.getClass().getName();
        }
    }

    public static class TestArraysClass {
        public Object[] objectEmpty = new Object[0];

        public boolean[] booleanArray = {true, false, true};
        public boolean[][] booleanArray2 = {{true, false, true}, {true, false, true}};
        public boolean[][][] booleanArray3 = {{{true, false, true}, {true, false, true}}, {{true, false, true}, {true, false, true}}};

        public byte[] byteArray = {1, 2, 3};
        public byte[][] byteArray2 = {{1, 2, 3}, {1, 2, 3}};
        public byte[][][] byteArray3 = {{{1, 2, 3}, {1, 2, 3}}, {{1, 2, 3}, {1, 2, 3}}};

        public char[] charArray = {'a', 'b', 'c'};
        public char[][] charArray2 = {{'a', 'b', 'c'}, {'a', 'b', 'c'}};
        public char[][][] charArray3 = {{{'a', 'b', 'c'}, {'a', 'b', 'c'}}, {{'a', 'b', 'c'}, {'a', 'b', 'c'}}};

        public double[] doubleArray = {1.1, 1.2, 1.3};
        public double[][] doubleArray2 = {{1.1, 1.2, 1.3}, {1.1, 1.2, 1.3}};
        public double[][][] doubleArray3 = {{{1.1, 1.2, 1.3}, {1.1, 1.2, 1.3}}, {{1.1, 1.2, 1.3}, {1.1, 1.2, 1.3}}};

        public float[] floatArray = {1.1f, 1.2f, 1.3f};
        public float[][] floatArray2 = {{1.1f, 1.2f, 1.3f}, {1.1f, 1.2f, 1.3f}};
        public float[][][] floatArray3 = {{{1.1f, 1.2f, 1.3f}, {1.1f, 1.2f, 1.3f}}, {{1.1f, 1.2f, 1.3f}, {1.1f, 1.2f, 1.3f}}};

        public int[] integerArray = {1, 2, 3};
        public int[][] integerArray2 = {{1, 2, 3}, {1, 2, 3}};
        public int[][][] integerArray3 = {{{1, 2, 3}, {1, 2, 3}}, {{1, 2, 3}, {1, 2, 3}}};

        public long[] longArray = {1L, 2L, 3L};
        public long[][] longArray2 = {{1L, 2L, 3L}, {1L, 2L, 3L}};
        public long[][][] longArray3 = {{{1L, 2L, 3L}, {1L, 2L, 3L}}, {{1L, 2L, 3L}, {1L, 2L, 3L}}};

        public short[] shortArray = {1, 2, 3};
        public short[][] shortArray2 = {{1, 2, 3}, {1, 2, 3}};
        public short[][][] shortArray3 = {{{1, 2, 3}, {1, 2, 3}}, {{1, 2, 3}, {1, 2, 3}}};

        public Boolean[] booleanObjectArray = {true, false, true};
        public Boolean[][] booleanObjectArray2 = {{true, false, true}, {true, false, true}};
        public Boolean[][][] booleanObjectArray3 = {{{true, false, true}, {true, false, true}}, {{true, false, true}, {true, false, true}}};

        public Byte[] byteObjectArray = {1, 2, 3};
        public Byte[][] byteObjectArray2 = {{1, 2, 3}, {1, 2, 3}};
        public Byte[][][] byteObjectArray3 = {{{1, 2, 3}, {1, 2, 3}}, {{1, 2, 3}, {1, 2, 3}}};

        public Character[] charObjectArray = {'a', 'b', 'c'};
        public Character[][] charObjectArray2 = {{'a', 'b', 'c'}, {'a', 'b', 'c'}};
        public Character[][][] charObjectArray3 = {{{'a', 'b', 'c'}, {'a', 'b', 'c'}}, {{'a', 'b', 'c'}, {'a', 'b', 'c'}}};

        public Double[] doubleObjectArray = {1.1, 1.2, 1.3};
        public Double[][] doubleObjectArray2 = {{1.1, 1.2, 1.3}, {1.1, 1.2, 1.3}};
        public Double[][][] doubleObjectArray3 = {{{1.1, 1.2, 1.3}, {1.1, 1.2, 1.3}}, {{1.1, 1.2, 1.3}, {1.1, 1.2, 1.3}}};

        public Float[] floatObjectArray = {1.1f, 1.2f, 1.3f};
        public Float[][] floatObjectArray2 = {{1.1f, 1.2f, 1.3f}, {1.1f, 1.2f, 1.3f}};
        public Float[][][] floatObjectArray3 = {{{1.1f, 1.2f, 1.3f}, {1.1f, 1.2f, 1.3f}}, {{1.1f, 1.2f, 1.3f}, {1.1f, 1.2f, 1.3f}}};

        public Integer[] integerObjectArray = {1, 2, 3};
        public Integer[][] integerObjectArray2 = {{1, 2, 3}, {1, 2, 3}};
        public Integer[][][] integerObjectArray3 = {{{1, 2, 3}, {1, 2, 3}}, {{1, 2, 3}, {1, 2, 3}}};

        public Long[] longObjectArray = {1L, 2L, 3L};
        public Long[][] longObjectArray2 = {{1L, 2L, 3L}, {1L, 2L, 3L}};
        public Long[][][] longObjectArray3 = {{{1L, 2L, 3L}, {1L, 2L, 3L}}, {{1L, 2L, 3L}, {1L, 2L, 3L}}};

        public Short[] shortObjectArray = {1, 2, 3};
        public Short[][] shortObjectArray2 = {{1, 2, 3}, {1, 2, 3}};
        public Short[][][] shortObjectArray3 = {{{1, 2, 3}, {1, 2, 3}}, {{1, 2, 3}, {1, 2, 3}}};

        public String[] stringArray = {"a", "b", "c"};
        public String[][] stringArray2 = {{"a", "b", "c"}, {"a", "b", "c"}};
        public String[][][] stringArray3 = {{{"a", "b", "c"}, {"a", "b", "c"}}, {{"a", "b", "c"}, {"a", "b", "c"}}};

        public Object[] onlyIntegerObjectArray = {1, 2, 3};
        public Object[] onlyLongObjectArray = {1L, 2L, 3L};
        public Object[] numericObjectArray = {1, 1L, 1.1};
        public Object[] mixedObjectArray = {1, "a", "1"};

        public List<Boolean> booleanList = Arrays.asList(booleanObjectArray);
        public List<?> booleanList2 = Arrays.asList(new List<?>[]{booleanList, booleanList});
        public List<?> booleanList3 = Arrays.asList(new List<?>[]{booleanList2, booleanList2});

        public List<Byte> byteList = Arrays.asList(byteObjectArray);
        public List<?> byteList2 = Arrays.asList(new List<?>[]{byteList, byteList});
        public List<?> byteList3 = Arrays.asList(new List<?>[]{byteList2, byteList2});

        public List<Character> charList = Arrays.asList(charObjectArray);
        public List<?> charList2 = Arrays.asList(new List<?>[]{charList, charList});
        public List<?> charList3 = Arrays.asList(new List<?>[]{charList2, charList2});

        public List<Double> doubleList = Arrays.asList(doubleObjectArray);
        public List<?> doubleList2 = Arrays.asList(new List<?>[]{doubleList, doubleList});
        public List<?> doubleList3 = Arrays.asList(new List<?>[]{doubleList2, doubleList2});

        public List<Float> floatList = Arrays.asList(floatObjectArray);
        public List<?> floatList2 = Arrays.asList(new List<?>[]{floatList, floatList});
        public List<?> floatList3 = Arrays.asList(new List<?>[]{floatList2, floatList2});

        public List<Integer> integerList = Arrays.asList(integerObjectArray);
        public List<?> integerList2 = Arrays.asList(new List<?>[]{integerList, integerList});
        public List<?> integerList3 = Arrays.asList(new List<?>[]{integerList2, integerList2});

        public List<Long> longList = Arrays.asList(longObjectArray);
        public List<?> longList2 = Arrays.asList(new List<?>[]{longList, longList});
        public List<?> longList3 = Arrays.asList(new List<?>[]{longList2, longList2});

        public List<Short> shortList = Arrays.asList(shortObjectArray);
        public List<?> shortList2 = Arrays.asList(new List<?>[]{shortList, shortList});
        public List<?> shortList3 = Arrays.asList(new List<?>[]{shortList2, shortList2});

        public List<String> stringList = Arrays.asList(stringArray);
        public List<?> stringList2 = Arrays.asList(new List<?>[]{Arrays.asList(stringArray), Arrays.asList(stringArray)});
        public List<?> stringList3 = Arrays.asList(new List<?>[]{stringList2, stringList2});

        public List<?> mixedIntegerList = Arrays.asList(new Object[]{new Integer[]{1, 2, 3}, Arrays.asList(new int[]{4, 5, 6}), new int[]{7, 8, 9}, 10, 11, 12});
    }

    public static class TestIterableSize extends AbstractTestIterable {
        public TestIterableSize(int size) {
            super(size);
        }

        public int size() {
            return size;
        }
    }

    public static class TestIterableGetSize extends AbstractTestIterable {
        public TestIterableGetSize(int size) {
            super(size);
        }

        public int getSize() {
            return size;
        }
    }

    public static class TestIterableLength extends AbstractTestIterable {
        public TestIterableLength(int size) {
            super(size);
        }

        public int length() {
            return size;
        }
    }

    public static class TestIterableGetLength extends AbstractTestIterable {
        public TestIterableGetLength(int size) {
            super(size);
        }

        public int getLength() {
            return size;
        }
    }

    public static class TestIterableNoSizeMethod implements Iterable<Integer> {
        private final int size;

        public TestIterableNoSizeMethod(int size) {
            this.size = size;
        }

        private class I implements Iterator<Integer> {
            private int size;

            I() {
                this.size = TestIterableNoSizeMethod.this.size;
            }

            @Override
            public boolean hasNext() {
                return size-- > 0;
            }

            @Override
            public Integer next() {
                throw new UnsupportedOperationException("Should not reach here.");
            }
        }

        @Override
        public Iterator<Integer> iterator() {
            return new I();
        }
    }

    private static class AbstractTestIterable implements Iterable<Object> {
        protected final int size;

        AbstractTestIterable(int size) {
            this.size = size;
        }

        @Override
        public Iterator<Object> iterator() {
            throw new UnsupportedOperationException("Should not reach here.");
        }
    }
}
