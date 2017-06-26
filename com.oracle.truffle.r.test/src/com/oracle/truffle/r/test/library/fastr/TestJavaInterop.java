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
        assertEvalFastR("v <- as.external.char(97.1, 1); v;", "cat('Error in as.external.char(97.1, 1) : ', '\n', ' pos argument not allowed with a numeric value', '\n')");
        assertEvalFastR("v <- as.external.char(97L, 1); v;", "cat('Error in as.external.char(97L, 1) : ','\n',' pos argument not allowed with a numeric value', '\n')");
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

        assertEvalFastR(Ignored.Unimplemented, "a <- as.java.array(1L,,F); a;", getRValue(new int[]{1}));
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

        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "java.class(to$methodReturnsNull())", "cat('Error in java.class(to$methodReturnsNull()) : unsupported type', '\n')");
        assertEvalFastR("java.class(NULL)", "cat('Error in java.class(NULL) : unsupported type', '\n')");
        assertEvalFastR("java.class(1)", "cat('Error in java.class(1) : unsupported type', '\n')");
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

        assertEvalFastR("to <- new('" + TEST_CLASS + "'); new(to)", "cat('Error in new.external(Class, ...) : ', '\n', '  error during Java object instantiation\n', sep='')");

        assertEvalFastR("to <- new('__bogus_class_name__');", "cat('Error in getClass(Class, where = topenv(parent.frame())) : ', '\n', '  “__bogus_class_name__” is not a defined class\n', sep='')");
    }

    @Test
    public void testCombineInteropTypes() {
        assertEvalFastR("class(c(as.external.byte(123)))", "'interopt.byte'");
        assertEvalFastR("class(c(as.external.byte(123), as.external.byte(234)))", "'list'");
        assertEvalFastR("class(c(as.external.byte(123), 1))", "'list'");
        assertEvalFastR("class(c(1, as.external.byte(123)))", "'list'");

        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " class(c(to))", "'truffle.object'");
        assertEvalFastR("tc <- new.java.class('" + TEST_CLASS + "'); t <- new.external(tc); t1 <- new.external(tc); class(c(t, t1))", "'list'");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " class(c(1, t))", "'list'");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " class(c(t, 1))", "'list'");

        TestClass t = new TestClass();
        assertEvalFastR(Ignored.Unimplemented, CREATE_TRUFFLE_OBJECT + " c(to$fieldStringArray)", toRVector(t.fieldStringArray, null));
        assertEvalFastR(Ignored.Unimplemented, CREATE_TRUFFLE_OBJECT + " c(to$listString)", toRVector(t.listString, null));
        assertEvalFastR(Ignored.Unimplemented, CREATE_TRUFFLE_OBJECT + " c(to$fieldStringArray, to$fieldStringArray)", "c('a', 'b', 'c', 'a', 'b', 'c')");
        assertEvalFastR(Ignored.Unimplemented, CREATE_TRUFFLE_OBJECT + " c(to$listString, to$listString)", "c('a', 'b', 'c', 'a', 'b', 'c')");
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
        assertEvalFastR(Ignored.ImplementationError, "cl <- new.java.class('java.util.Collections'); em<-cl$EMPTY_MAP; names(em)", "NULL");
        assertEvalFastR(Ignored.ImplementationError, "tc <- new.java.class('" + TestNamesClassMap.class.getName() + "'); to <- new.external(tc); sort(names(to$m()))", "c('one', 'two')");
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

        assertPassingForeighObjectToFunction("is.nan", "cat('Error in is.nan(to) : ', '\n', '  default method not implemented for type \\'external object\\'\n', sep='')");
        assertPassingForeighObjectToFunction("is.finite", "cat('Error in is.finite(to) : ', '\n', '  default method not implemented for type \\'external object\\'\n', sep='')");
        assertPassingForeighObjectToFunction("is.infinite", "cat('Error in is.infinite(to) : ', '\n', '  default method not implemented for type \\'external object\\'\n', sep='')");

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
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " attr(to, 'a')<-'a'", "cat('Error in attr(to, \"a\") <- \"a\" : external object cannot be attributed\n')");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " attr(to, which = 'a')", "cat('Error in attr(to, which = \"a\") : external object cannot be attributed\n')");
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

        assertEvalFastR(Output.IgnoreErrorContext, CREATE_TRUFFLE_OBJECT + " l<-as.list(to);", "cat('Error in as.list(to) : ', '\n', ' no method for coercing this external object to a list', '\n')");
    }

    @Test
    public void testConvertEmptyList() throws IllegalArgumentException, IllegalAccessException {
        assertEvalFastR(Ignored.ImplementationError, CREATE_TRUFFLE_OBJECT + "as.character(to$listEmpty);", "as.character(list())");
    }

    @Test
    public void testAsXXX() throws IllegalArgumentException, IllegalAccessException {
        testAsXXX("as.character");
        testAsXXX("as.complex");
        testAsXXX("as.double");
        testAsXXX("as.expression");
        testAsXXX("as.integer");
        testAsXXX("as.logical");
        testAsXXX("as.raw");
        testAsXXX("as.symbol");
        testAsXXX("as.vector");
        // TODO more tests
    }

    public void testAsXXX(String asXXX) throws IllegalArgumentException, IllegalAccessException {
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
            assertEvalFastR(Output.IgnoreErrorContext, CREATE_TRUFFLE_OBJECT + asXXX + "(to);",
                            "cat('Error in " + asXXX + "(to) : ', '\n', ' no method for coercing this external object to a vector', '\n')");
        } else {
            assertEvalFastR(CREATE_TRUFFLE_OBJECT + asXXX + "(to);", "cat('Error in " + asXXX + "(to) : ', '\n', ' no method for coercing this external object to a vector', '\n')");
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
        assertEvalFastR(Output.IgnoreErrorContext, CREATE_TRUFFLE_OBJECT + "if(to$listString) print('OK')",
                        "if(c('A', 'B')) print('OK')");

        assertEvalFastR(Output.IgnoreWarningContext, CREATE_TRUFFLE_OBJECT + "if(to$listStringBoolean) print('OK')",
                        "if(" + toRVector(t.listStringBoolean, null) + ") print('OK')");

        assertEvalFastR(Output.IgnoreErrorContext, CREATE_TRUFFLE_OBJECT + "if(to) print('OK')",
                        "cat('Error in if (T) print(\\'OK\\') : ', '\n', '  argument is not interpretable as logical', '\n')");
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

        assertEvalFastR(Output.IgnoreErrorContext, CREATE_TRUFFLE_OBJECT + "while(to) print('OK')",
                        "cat('Error in if (T) print(\\'OK\\') : ', '\n', '  argument is not interpretable as logical', '\n')");
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

    private String toRVector(Object o, String asXXX) {
        if (o.getClass().isArray()) {
            List<Object> l = new ArrayList<>();
            for (int i = 0; i < Array.getLength(o); i++) {
                l.add(Array.get(o, i));
            }
            return toRVector(l, asXXX);
        }
        Assert.fail(o + " should have been an array");
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

    private static String getBooleanPrefix(Object value, int i) {
        if (value.getClass().getComponentType() == Boolean.TYPE && (boolean) Array.get(value, i)) {
            return " ";
        }
        if (i > 0 && value.getClass().getComponentType() == String.class &&
                        (Array.get(value, i).equals("T") || Array.get(value, i).equals("F") || Array.get(value, i).equals("TRUE") || Array.get(value, i).equals("FALSE"))) {
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
            mixedTypesArray = new Object[]{1, 2.1, 'a'};
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

    public static class TestArrayClass {
        public static TestArrayClass[] testArray = new TestArrayClass[]{new TestArrayClass(), new TestArrayClass(), new TestArrayClass()};
    }
}
