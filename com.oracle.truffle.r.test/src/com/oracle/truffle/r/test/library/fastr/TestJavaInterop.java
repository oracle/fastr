/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
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
import java.util.Iterator;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.oracle.truffle.r.nodes.builtin.base.printer.DoubleVectorPrinter;
import com.oracle.truffle.r.nodes.builtin.fastr.FastRInterop;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.test.TestBase;
import com.oracle.truffle.r.test.library.fastr.TestJavaInterop.TestClass.TestPOJO;
import java.lang.reflect.Constructor;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyArray;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.junit.Ignore;

public class TestJavaInterop extends TestBase {

    private static final String TEST_CLASS = TestClass.class.getName();
    private static final String CREATE_TRUFFLE_OBJECT = "to <- .fastr.interop.new(java.type('" + TEST_CLASS + "'));";
    private static final String CREATE_TEST_ARRAYS = "ta <- new('" + TestArraysClass.class.getName() + "');";

    @Before
    public void testInit() {
        FastRInterop.testingMode();
    }

    @Test
    public void testToByte() {
        assertEvalFastR("v <- .fastr.interop.asByte(1L); v;", "1");
        assertEvalFastR("v <- .fastr.interop.asByte(1.1); v;", "1");
        assertEvalFastR("v <- .fastr.interop.asByte(as.raw(1)); v;", "1");
        assertEvalFastR("v <- .fastr.interop.asByte(1.1); class(v);", "'" + RType.RInteropByte.getName() + "'");
        assertEvalFastR("v <- .fastr.interop.asByte(1.1); typeof(v);", "'" + RType.RInteropByte.getName() + "'");
        assertEvalFastR("v <- .fastr.interop.asByte(" + Byte.MAX_VALUE + "); v;", "" + Byte.MAX_VALUE);
        assertEvalFastR("v <- .fastr.interop.asByte(" + Byte.MIN_VALUE + "); v;", "" + Byte.MIN_VALUE);
        assertEvalFastR("v <- .fastr.interop.asByte(" + Integer.MAX_VALUE + "); v;", "" + new Integer(Integer.MAX_VALUE).byteValue());
        assertEvalFastR("v <- .fastr.interop.asByte(" + Integer.MIN_VALUE + "); v;", "" + new Integer(Integer.MIN_VALUE).byteValue());
        assertEvalFastR("v <- .fastr.interop.asByte(" + Double.MAX_VALUE + "); v;", "" + new Double(Double.MAX_VALUE).byteValue());
        assertEvalFastR("v <- .fastr.interop.asByte(" + Double.MIN_VALUE + "); v;", "" + new Double(Double.MIN_VALUE).byteValue());
    }

    @Test
    public void testToFloat() {
        assertEvalFastR("v <- .fastr.interop.asFloat(1L); v;", "1");
        assertEvalFastR("v <- .fastr.interop.asFloat(1.1); v;", "1.1");
        assertEvalFastR("v <- .fastr.interop.asFloat(as.raw(1)); v;", "1");
        assertEvalFastR("v <- .fastr.interop.asFloat(1.1); class(v);", "'" + RType.RInteropFloat.getName() + "'");
        assertEvalFastR("v <- .fastr.interop.asFloat(1.1); typeof(v);", "'" + RType.RInteropFloat.getName() + "'");
        assertEvalFastR("v <- .fastr.interop.asFloat(1L); class(v);", "'" + RType.RInteropFloat.getName() + "'");
        assertEvalFastR("v <- .fastr.interop.asFloat(1L); typeof(v);", "'" + RType.RInteropFloat.getName() + "'");
        assertEvalFastR("v <- .fastr.interop.asFloat(" + Float.MAX_VALUE + "); v;", "" + Float.MAX_VALUE);
        assertEvalFastR("v <- .fastr.interop.asFloat(" + Float.MIN_VALUE + "); v;", "" + (double) Float.MIN_VALUE);
        assertEvalFastR("v <- .fastr.interop.asFloat(" + Double.MAX_VALUE + "); v;", "Inf");
        assertEvalFastR("v <- .fastr.interop.asFloat(" + Double.MIN_VALUE + "); v;", "" + new Double(Double.MIN_VALUE).floatValue());
    }

    @Test
    public void testToLong() {
        assertEvalFastR("v <- .fastr.interop.asLong(1L); v;", "1");
        assertEvalFastR("v <- .fastr.interop.asLong(1.1); v;", "1");
        assertEvalFastR("v <- .fastr.interop.asLong(as.raw(1)); v;", "1");
        assertEvalFastR("v <- .fastr.interop.asLong(1.1); class(v);", "'" + RType.RInteropLong.getName() + "'");
        assertEvalFastR("v <- .fastr.interop.asLong(1.1); typeof(v);", "'" + RType.RInteropLong.getName() + "'");
        assertEvalFastR("v <- .fastr.interop.asLong(1L); class(v);", "'" + RType.RInteropLong.getName() + "'");
        assertEvalFastR("v <- .fastr.interop.asLong(1L); typeof(v);", "'" + RType.RInteropLong.getName() + "'");
        assertEvalFastR("v <- .fastr.interop.asLong(" + Integer.MAX_VALUE + "); v;", "" + Integer.MAX_VALUE);
        assertEvalFastR("v <- .fastr.interop.asLong(" + Integer.MIN_VALUE + "); v;", "" + Integer.MIN_VALUE);
        assertEvalFastR("v <- .fastr.interop.asLong(" + Double.MAX_VALUE + "); v;", "" + new Double(Double.MAX_VALUE).longValue());
        assertEvalFastR("v <- .fastr.interop.asLong(" + Double.MIN_VALUE + "); v;", "" + new Double(Double.MIN_VALUE).longValue());
    }

    @Test
    public void testToShort() {
        assertEvalFastR("v <- .fastr.interop.asShort(1L); v;", "1");
        assertEvalFastR("v <- .fastr.interop.asShort(1.1); v;", "1");
        assertEvalFastR("v <- .fastr.interop.asShort(as.raw(1)); v;", "1");
        assertEvalFastR("v <- .fastr.interop.asShort(1.1); class(v);", "'" + RType.RInteropShort.getName() + "'");
        assertEvalFastR("v <- .fastr.interop.asShort(1.1); typeof(v);", "'" + RType.RInteropShort.getName() + "'");
        assertEvalFastR("v <- .fastr.interop.asShort(1L); class(v);", "'" + RType.RInteropShort.getName() + "'");
        assertEvalFastR("v <- .fastr.interop.asShort(1L); typeof(v);", "'" + RType.RInteropShort.getName() + "'");
        assertEvalFastR("v <- .fastr.interop.asShort(" + Short.MAX_VALUE + "); v;", "" + Short.MAX_VALUE);
        assertEvalFastR("v <- .fastr.interop.asShort(" + Short.MIN_VALUE + "); v;", "" + Short.MIN_VALUE);
        assertEvalFastR("v <- .fastr.interop.asShort(" + Integer.MAX_VALUE + "); v;", "" + new Integer(Integer.MAX_VALUE).shortValue());
        assertEvalFastR("v <- .fastr.interop.asShort(" + Integer.MIN_VALUE + "); v;", "" + new Integer(Integer.MIN_VALUE).shortValue());
        assertEvalFastR("v <- .fastr.interop.asShort(" + Double.MAX_VALUE + "); v;", "" + new Double(Double.MAX_VALUE).shortValue());
        assertEvalFastR("v <- .fastr.interop.asShort(" + Double.MIN_VALUE + "); v;", "" + new Double(Double.MIN_VALUE).shortValue());
    }

    @Test
    public void testToChar() {
        assertEvalFastR("v <- .fastr.interop.asChar(97L); v;", "'a'");
        assertEvalFastR("v <- .fastr.interop.asChar(97.1); v;", "'a'");
        assertEvalFastR("v <- .fastr.interop.asChar(97.1, 1); v;", errorIn(".fastr.interop.asChar(97.1, 1)", "pos argument not allowed with a numeric value"));
        assertEvalFastR("v <- .fastr.interop.asChar(97L, 1); v;", errorIn(".fastr.interop.asChar(97L, 1)", "pos argument not allowed with a numeric value"));
        assertEvalFastR("v <- .fastr.interop.asChar('abc', 1); v;", "'b'");
        assertEvalFastR("v <- .fastr.interop.asChar('abc', 1.1); v;", "'b'");
        assertEvalFastR("v <- .fastr.interop.asChar(97.1); class(v);", "'" + RType.RInteropChar.getName() + "'");
        assertEvalFastR("v <- .fastr.interop.asChar(97.1); typeof(v);", "'" + RType.RInteropChar.getName() + "'");
        assertEvalFastR("v <- .fastr.interop.asChar(97L); class(v);", "'" + RType.RInteropChar.getName() + "'");
        assertEvalFastR("v <- .fastr.interop.asChar(97L); typeof(v);", "'" + RType.RInteropChar.getName() + "'");
        assertEvalFastR("v <- .fastr.interop.asChar('a'); v;", "'a'");
    }

    @Test
    public void testToArray() {
        assertEvalFastR("a <- .fastr.interop.asJavaArray(1L); a;", getRValue(new int[]{1}));
        assertEvalFastR("a <- .fastr.interop.asJavaArray(1L); a$getClass()$getName();", "'[I'");
        assertEvalFastR("a <- .fastr.interop.asJavaArray(c(1L, 2L)); a;", getRValue(new int[]{1, 2}));
        assertEvalFastR("a <- .fastr.interop.asJavaArray(1L,,T); a;", getRValue(new int[]{1}));
        assertEvalFastR("a <- .fastr.interop.asJavaArray(c(1L, 2L),,T); a;", getRValue(new int[]{1, 2}));
        assertEvalFastR("a <- .fastr.interop.asJavaArray(c(1L, 2L),'double',T); a;", getRValue(new double[]{1, 2}));

        assertEvalFastR("a <- .fastr.interop.asJavaArray(1.1); a;", getRValue(new double[]{1.1}));
        assertEvalFastR("a <- .fastr.interop.asJavaArray(1.1); a$getClass()$getName();", "'[D'");
        assertEvalFastR("a <- .fastr.interop.asJavaArray(c(1.1, 1.2)); a;", getRValue(new double[]{1.1, 1.2}));
        assertEvalFastR("a <- .fastr.interop.asJavaArray(1.1,,T); a;", getRValue(new double[]{1.1}));
        assertEvalFastR("a <- .fastr.interop.asJavaArray(c(1.1, 1.2),,T); a;", getRValue(new double[]{1.1, 1.2}));
        assertEvalFastR("a <- .fastr.interop.asJavaArray(c(1.1, 1.2),'double',T); a;", getRValue(new double[]{1.1, 1.2}));

        assertEvalFastR("a <- .fastr.interop.asJavaArray(T); a;", getRValue(new boolean[]{true}));
        assertEvalFastR("a <- .fastr.interop.asJavaArray(T); a$getClass()$getName();", "'[Z'");
        assertEvalFastR("a <- .fastr.interop.asJavaArray(c(T, F)); a;", getRValue(new boolean[]{true, false}));
        assertEvalFastR("a <- .fastr.interop.asJavaArray(T,,T); a;", getRValue(new boolean[]{true}));
        assertEvalFastR("a <- .fastr.interop.asJavaArray(c(T, F),,T); a;", getRValue(new boolean[]{true, false}));
        assertEvalFastR("a <- .fastr.interop.asJavaArray(c(T, F),'boolean',T); a;", getRValue(new boolean[]{true, false}));

        assertEvalFastR("a <- .fastr.interop.asJavaArray('a'); a;", getRValue(new String[]{"a"}));
        assertEvalFastR("a <- .fastr.interop.asJavaArray('a'); a$getClass()$getName();", "'[Ljava.lang.String;'");
        assertEvalFastR("a <- .fastr.interop.asJavaArray(c('a', 'b')); a;", getRValue(new String[]{"a", "b"}));
        assertEvalFastR("a <- .fastr.interop.asJavaArray('a',,T); a;", getRValue(new String[]{"a"}));
        assertEvalFastR("a <- .fastr.interop.asJavaArray(c('a', 'b'),,T); a;", getRValue(new String[]{"a", "b"}));
        assertEvalFastR("a <- .fastr.interop.asJavaArray(c('a', 'b'),'java.lang.String',T); a;", getRValue(new String[]{"a", "b"}));

        assertEvalFastR("a <- .fastr.interop.asJavaArray(as.raw(1)); a", getRValue(new byte[]{1}));
        assertEvalFastR("a <- .fastr.interop.asJavaArray(as.raw(1)); a$getClass()$getName();", "'[B'");
        assertEvalFastR("a <- .fastr.interop.asJavaArray(as.raw(1)); length(a);", "1");
        assertEvalFastR("a <- .fastr.interop.asJavaArray(as.raw(c(1, 2, 3))); length(a);", "3");
        assertEvalFastR("a <- .fastr.interop.asJavaArray(as.raw(c(1, 2, 3))); a$getClass()$getName();", "'[B'");
        assertEvalFastR("a <- .fastr.interop.asJavaArray(as.raw(c(1, 2, 3)), 'int'); a$getClass()$getName();", "'[I'");

        assertEvalFastR("a <- .fastr.interop.asJavaArray(.fastr.interop.asShort(1)); a;", getRValue(new short[]{1}));
        assertEvalFastR("a <- .fastr.interop.asJavaArray(.fastr.interop.asShort(1)); a$getClass()$getName();", "'[S'");
        assertEvalFastR("a <- .fastr.interop.asJavaArray(c(.fastr.interop.asShort(1), .fastr.interop.asShort(2))); a;", getRValue(new short[]{1, 2}));
        assertEvalFastR("a <- .fastr.interop.asJavaArray(.fastr.interop.asShort(1),,T); a;", getRValue(new short[]{1}));
        assertEvalFastR("a <- .fastr.interop.asJavaArray(c(.fastr.interop.asShort(1), .fastr.interop.asShort(2)),,T); a;", getRValue(new short[]{1, 2}));
        assertEvalFastR("a <- .fastr.interop.asJavaArray(c(.fastr.interop.asShort(1), .fastr.interop.asShort(2)),'int',T); a;", getRValue(new int[]{1, 2}));

        assertEvalFastR("a <- .fastr.interop.asJavaArray(.fastr.interop.asShort(1), 'java.lang.Short'); a;", getRValue(new short[]{1}));
        assertEvalFastR("a <- .fastr.interop.asJavaArray(c(.fastr.interop.asShort(1), .fastr.interop.asShort(2)), 'java.lang.Short'); a;", getRValue(new short[]{1, 2}));
        assertEvalFastR("a <- .fastr.interop.asJavaArray(.fastr.interop.asShort(1), 'java.lang.Short', T); a;", getRValue(new short[]{1}));
        assertEvalFastR("a <- .fastr.interop.asJavaArray(c(.fastr.interop.asShort(1), .fastr.interop.asShort(2)), 'java.lang.Short', T); a;", getRValue(new short[]{1, 2}));

        assertEvalFastR("a <- .fastr.interop.asJavaArray(c(.fastr.interop.asShort(1), .fastr.interop.asShort(2)), 'int'); a;", getRValue(new int[]{1, 2}));
        assertEvalFastR("a <- .fastr.interop.asJavaArray(c(1.123, 2.123), 'double'); a;", getRValue(new double[]{1.123, 2.123}));
        assertEvalFastR("a <- .fastr.interop.asJavaArray(.fastr.interop.asShort(1), 'double'); a;", getRValue(new double[]{1}));

        assertEvalFastR("a <- .fastr.interop.asJavaArray(1L); .fastr.interop.asJavaArray(a);", getRValue(new int[]{1}));
        assertEvalFastR("a <- .fastr.interop.asJavaArray(1L); .fastr.interop.asJavaArray(a,,T);", getRValue(new int[]{1}));

        assertEvalFastR("a <- .fastr.interop.asJavaArray(.fastr.interop.asShort(1)); .fastr.interop.asJavaArray(a);", getRValue(new short[]{1}));

        assertEvalFastR("tc <- java.type('" + TEST_CLASS + "'); to <- .fastr.interop.new(tc); a <- .fastr.interop.asJavaArray(to); is.polyglot.value(a) && length(a) > 0", "TRUE");
        assertEvalFastR("tc <- java.type('" + TEST_CLASS + "'); to <- .fastr.interop.new(tc); a <- .fastr.interop.asJavaArray(c(to, to)); is.polyglot.value(a) && length(a) > 0", "TRUE");
        assertEvalFastR("tc <- java.type('" + TEST_CLASS + "'); to <- .fastr.interop.new(tc); a <- .fastr.interop.asJavaArray(c(to, to)); length(a)", "2");
        assertEvalFastR("tc <- java.type('" + TEST_CLASS + "'); to <- .fastr.interop.new(tc); a <- .fastr.interop.asJavaArray(c(to, to)); a$getClass()$getName();",
                        "[Lcom.oracle.truffle.r.test.library.fastr.TestJavaInterop$TestClass;");

        assertEvalFastR("a <- .fastr.interop.asJavaArray(1L,,F); a;", getRValue(new int[]{1}));
    }

    @Test
    public void testArrayAsParameter() {
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "ja <- .fastr.interop.asJavaArray(c(1L, 2L, 3L), 'int'); to$isIntArray(ja)", "'" + (new int[1]).getClass().getName() + "'");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "ja <- .fastr.interop.asJavaArray(c(1L, 2L, 3L), 'java.lang.Integer'); to$isIntegerArray(ja)", "'" + (new Integer[1]).getClass().getName() + "'");
    }

    @Test
    public void testNewArray() {
        testNewArray("java.lang.Boolean");
        testNewArray("java.lang.Byte");
        testNewArray("java.lang.Character");
        testNewArray("java.lang.Double");
        testNewArray("java.lang.Float");
        testNewArray("java.lang.Integer");
        testNewArray("java.lang.Long");
        testNewArray("java.lang.Short");
        testNewArray("java.lang.String");

        testNewArray("boolean");
        testNewArray("byte");
        testNewArray("char");
        testNewArray("double");
        testNewArray("float");
        testNewArray("int");
        testNewArray("long");
        testNewArray("short");

        testNewArray("com.oracle.truffle.r.test.library.fastr.TestJavaInterop$TestClass");
    }

    public void testNewArray(String className) {
        assertEvalFastR("a <- new(java.type('" + className + "[]'), 10L); is.polyglot.value(a) && length(a) > 0", "TRUE");
        assertEvalFastR("a <- new(java.type('" + className + "[]'), 10L); length(a)", "10");
        assertEvalFastR("a <- new(java.type('" + className + "[]'), 10L); a$getClass()$getName();", toArrayClassName(className, 1));

        assertEvalFastR("a <- new(java.type('" + className + "[][]'), c(2L, 3L)); is.polyglot.value(a) && length(a) > 0", "TRUE");
        assertEvalFastR("a <- new(java.type('" + className + "[][]'), c(2L, 3L)); length(a);", "2L");
        assertEvalFastR("a <- new(java.type('" + className + "[][]'), c(2L, 3L)); length(a[1]);", "3L");
        assertEvalFastR("a <- new(java.type('" + className + "[][]'), c(2L, 3L)); a$getClass()$getName();", toArrayClassName(className, 2));
    }

    @Test
    public void testGetClassAndClassName() {
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "to$getClass()$getName()", "'com.oracle.truffle.r.test.library.fastr.TestJavaInterop$TestClass'");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "to$class$getName()", "'com.oracle.truffle.r.test.library.fastr.TestJavaInterop$TestClass'");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "to$class$getClass()$getName()", "'java.lang.Class'");
    }

    @Test
    public void testInteroptNew() {
        assertEvalFastR("tc <- java.type('" + Boolean.class.getName() + "'); t <- .fastr.interop.new(tc, TRUE); t", "TRUE");
        assertEvalFastR("tc <- java.type('java/lang/Boolean'); t <- new(tc, TRUE); t", "TRUE");
        assertEvalFastR("tc <- java.type('" + Byte.class.getName() + "'); t <- .fastr.interop.new(tc, .fastr.interop.asByte(1)); t", "1");
        assertEvalFastR("tc <- java.type('" + Character.class.getName() + "'); t <- .fastr.interop.new(tc, .fastr.interop.asChar(97)); t", "'a'");
        assertEvalFastR("tc <- java.type('" + Double.class.getName() + "'); t <- .fastr.interop.new(tc, 1.1); t", "1.1");
        assertEvalFastR("tc <- java.type('" + Float.class.getName() + "'); t <- .fastr.interop.new(tc, .fastr.interop.asFloat(1.1)); t", "1.1");
        assertEvalFastR("tc <- java.type('" + Integer.class.getName() + "'); t <- .fastr.interop.new(tc, 1L); t", "1");
        assertEvalFastR("tc <- java.type('" + Long.class.getName() + "'); t <- .fastr.interop.new(tc, .fastr.interop.asLong(1)); t", "1");
        assertEvalFastR("tc <- java.type('" + Short.class.getName() + "'); t <- .fastr.interop.new(tc, .fastr.interop.asShort(1)); t", "1");
        assertEvalFastR("tc <- java.type('" + String.class.getName() + "'); t <- .fastr.interop.new(tc, 'abc'); t", "'abc'");
        assertEvalFastR("tc <- java.type('" + TestNullClass.class.getName() + "'); t <- .fastr.interop.new(tc, NULL); class(t)", "'" + RType.TruffleObject.getName() + "'");
    }

    @Test
    public void testNew() {
        assertEvalFastR("tc <- java.type('" + Boolean.class.getName() + "'); to <- new(tc, TRUE); to", "TRUE");
        assertEvalFastR("tc <- java.type('" + TEST_CLASS + "'); to <- new(tc); to$fieldInteger", getRValue(Integer.MAX_VALUE));

        assertEvalFastR("to <- new('" + Boolean.class.getName() + "', TRUE); to", "TRUE");
        assertEvalFastR("to <- new('java/lang/Boolean', TRUE); to", "TRUE");
        assertEvalFastR("to <- new('" + TEST_CLASS + "'); to$fieldStaticInteger",
                        getRValue(Integer.MAX_VALUE));

        assertEvalFastR("to <- new('" + TEST_CLASS + "'); new(to)", errorIn(".fastr.interop.new(Class, ...)", "error during Java object instantiation"));

        assertEvalFastR("to <- new('__bogus_class_name__');", errorIn("getClass(Class, where = topenv(parent.frame()))", "“__bogus_class_name__” is not a defined class"));

        assertEvalFastR(Context.NoJavaInterop, "new('integer'); ", "cat('integer(0)'");
        assertEvalFastR(Context.NoJavaInterop, "new('" + Boolean.class.getName() + "');",
                        errorIn("getClass(Class, where = topenv(parent.frame()))", "“" + Boolean.class.getName() + "” is not a defined class"));
        assertEvalFastR(Context.NoJavaInterop, "new('__bogus_class_name__');", errorIn("getClass(Class, where = topenv(parent.frame()))", "“__bogus_class_name__” is not a defined class"));
    }

    @Test
    public void testCombineInteropTypes() {
        assertEvalFastR("class(c(.fastr.interop.asByte(123)))", "'interopt.byte'");
        assertEvalFastR("class(c(.fastr.interop.asByte(123), .fastr.interop.asByte(234)))", "'list'");
        assertEvalFastR("class(c(.fastr.interop.asByte(123), 1))", "'list'");
        assertEvalFastR("class(c(1, .fastr.interop.asByte(123)))", "'list'");
    }

    @Test
    public void testCombineForeignObjects() throws IllegalArgumentException {

        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " class(c(to))", "'list'");
        assertEvalFastR("tc <- java.type('" + TEST_CLASS + "'); t <- .fastr.interop.new(tc); t1 <- .fastr.interop.new(tc); class(c(t, t1))", "'list'");
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
            if (name.startsWith("field") && !name.toLowerCase().contains("static")) {
                testForValue(name, f.get(t));
            }
        }
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " to$mixedTypesArray",
                        "cat('[polyglot value]\n[[1]]\n[1] 1\n\n[[2]]\n[1] 2.1\n\n[[3]]\n[1] \"a\"\n\n[[4]]\n[1] TRUE\n\n[[5]]\nNULL\n\n', sep='')");
    }

    @Test
    public void testMethods() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        TestClass t = new TestClass();
        Method[] methods = t.getClass().getDeclaredMethods();
        for (Method m : methods) {
            if (m.getParameterCount() == 0) {
                String name = m.getName();
                if (name.startsWith("method") && !name.toLowerCase().contains("static")) {
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
        assertEvalFastR(Ignored.ImplementationError, CREATE_TRUFFLE_OBJECT + " to$classAsArg(java.type(" + TEST_CLASS + "))", getRValue(TEST_CLASS));
    }

    private void getValueForAllTypesMethod(String method) {
        boolean bo = true;
        byte bt = Byte.MAX_VALUE;
        char c = 'a';
        short sh = Short.MAX_VALUE;
        int i = Integer.MAX_VALUE;
        long l = Integer.MAX_VALUE;
        double d = Double.MAX_VALUE;
        float f = 2f;
        String s = "testString";
        StringBuilder sb = new StringBuilder();
        sb.append(method);
        sb.append("(");
        sb.append(getRValue(bo));
        sb.append(", .fastr.interop.asByte(");
        sb.append(getRValue(bt));
        sb.append("), .fastr.interop.asChar(");
        sb.append(getRValue(c));
        sb.append("), .fastr.interop.asShort(");
        sb.append(getRValue(sh));
        sb.append("), ");
        sb.append(getRValue(i));
        sb.append("L, ");
        sb.append(" .fastr.interop.asLong(");
        sb.append(getRValue(l));
        sb.append("), ");
        sb.append(getRValue(d));
        sb.append(", .fastr.interop.asFloat(");
        sb.append(getRValue(f));
        sb.append("), ");
        sb.append(getRValue(s));
        sb.append(")");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " to$" + sb.toString(), getRValue("" + bo + bt + c + sh + i + l + d + f + s));
    }

    @Test
    public void testNullParameters() {
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " to$methodAcceptsOnlyNull(NULL)", "");

        assertEvalFastR(Ignored.Unimplemented, CREATE_TRUFFLE_OBJECT + " to$isNull('string')", "java.lang.String");
        assertEvalFastR(Ignored.Unimplemented, CREATE_TRUFFLE_OBJECT + " to$isNull(1)", "java.lang.Long");
    }

    @Test
    @Ignore("FIXME: overloads resolution is not working")
    public void testOverloaded() {
        String className = TestOverload.class.getName();
        String createClass = "toc <- java.type('" + className + "'); ";

        assertEvalFastR(createClass + " toc$isOverloaded(TRUE)", "'boolean'");
        assertEvalFastR(createClass + " toc$isOverloaded(.fastr.interop.asByte(1))", "'byte'");
        assertEvalFastR(createClass + " toc$isOverloaded(.fastr.interop.asChar('a'))", "'char'");
        assertEvalFastR(createClass + " toc$isOverloaded(1)", "'double'");
        assertEvalFastR(createClass + " toc$isOverloaded(.fastr.interop.asFloat(1))", "'float'");
        assertEvalFastR(createClass + " toc$isOverloaded(1L)", "'int'");
        assertEvalFastR(createClass + " toc$isOverloaded(.fastr.interop.asLong(1))", "'long'");
        assertEvalFastR(createClass + " toc$isOverloaded(.fastr.interop.asShort(1))", "'short'");
        assertEvalFastR(createClass + " toc$isOverloaded('string')", getRValue(String.class.getName()));

        assertEvalFastR("new('" + className + "', TRUE)$type", "'boolean'");
        assertEvalFastR("new('" + className + "', .fastr.interop.asByte(1))$type", "'byte'");
        assertEvalFastR("new('" + className + "', .fastr.interop.asChar('a'))$type", "'char'");
        assertEvalFastR("new('" + className + "', 1)$type", "'double'");
        assertEvalFastR("new('" + className + "', .fastr.interop.asFloat(1))$type", "'float'");
        assertEvalFastR("new('" + className + "', 1L)$type", "'int'");
        assertEvalFastR("new('" + className + "', .fastr.interop.asLong(1))$type", "'long'");
        assertEvalFastR("new('" + className + "', .fastr.interop.asShort(1))$type", "'short'");
        assertEvalFastR("new('" + className + "', 'string')$type", getRValue(String.class.getName()));
    }

    @Test
    public void testArrayReadWrite() {
        assertEvalFastR("a <- .fastr.interop.asJavaArray(c(1,2,3)); a[1]", "1");
        assertEvalFastR("a <- .fastr.interop.asJavaArray(c(1,2,3)); a[[1]]", "1");

        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " to$fieldIntegerArray[1];", "1");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " to$fieldIntegerArray[[1]];", "1");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " to$fieldIntegerArray[TRUE];", getRValue(new TestClass().fieldIntegerArray));

        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " to$int2DimArray[1]", getRValue(new int[]{1, 2, 3}));
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " to$int2DimArray[[1]]", getRValue(new int[]{1, 2, 3}));
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " to$int2DimArray[1,2]", "2");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " to$int2DimArray[[1,2]]", "2");

        assertEvalFastR("a <- .fastr.interop.asJavaArray(c(1,2,3)); a[1] <- 123; a[1]", "123");
        assertEvalFastR("a <- .fastr.interop.asJavaArray(c(1,2,3)); a[[1]] <- 123; a[[1]]", "123");

        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " to$fieldIntegerArray[1] <- 123L; to$fieldIntegerArray[1]", "123");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " to$fieldIntegerArray[[1]] <- 1234L; to$fieldIntegerArray[[1]]", "1234");

        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " to$fieldStringArray[1] <- NULL; to$fieldStringArray[1]", "NULL");

        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " to$int2DimArray[1,2] <- 1234L; to$int2DimArray[1,2]", "1234");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " to$int2DimArray[[1,2]] <- 12345L; to$int2DimArray[[1,2]]", "12345");

        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " to['fieldStringObject'] <- 'test'; to['fieldStringObject']", "'test'");

        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " to$fieldIntegerArray[TRUE] <- 0; to$fieldIntegerArray", "cat('[polyglot value]\n[1] 0 0 0\n')");
        assertEvalFastR(Ignored.ImplementationError, CREATE_TRUFFLE_OBJECT + " to$fieldIntegerArray[FALSE]", "integer(0)");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " t1 <- to[TRUE]; identical(to, t1)", "TRUE");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " to[FALSE]", "list()");

        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " to['x']", errorIn("to[\"x\"]", "invalid index/identifier during foreign access: x"));
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " to[1]", errorIn("to[1]", "invalid index/identifier during foreign access: 0.0"));
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " to$fieldIntegerArray[c(1, 5)]", errorIn("to$fieldIntegerArray[c(1, 5)]", "invalid index/identifier during foreign access: 4.0"));
    }

    @Test
    public void testReadByPositionsVector() {
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " to$fieldStringArray[c(1, 3)]", "c('a', 'c')");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " to$fieldStringArray[c(c(1,2), c(3, 2))]", "c('a', 'b', 'c', 'b')");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " to$hasNullIntArray[c(2, 3)]", "c(NA_integer_, 3L)");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " to$fieldStringArray[c(TRUE, FALSE)]", "c('a', 'c')");

        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " t1 <- to[c(TRUE, FALSE)]; identical(to, t1)", "TRUE");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " to[c(FALSE, TRUE)]", "list()");

        assertEvalFastR(Ignored.ImplementationError, CREATE_TEST_ARRAYS + " ta$integerArray2[c(1,2), c(1,2)]", "just wrong");

        TestClass tc = new TestClass();
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " to[c('fieldStringObject', 'fieldChar')]", "c('" + tc.fieldStringObject + "', '" + tc.fieldChar + "')");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " to[c('fieldStringObject', 'fieldInteger')]", "list('" + tc.fieldStringObject + "', " + tc.fieldInteger + ")");
    }

    @Test
    public void testWriteByPositionsVector() {
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " to$fieldStringArray[c(1, 3)] <- 'x'; to$fieldStringArray", "cat('[polyglot value]\n[1] \"x\" \"b\" \"x\"\n')");
        assertEvalFastR("ja <- new(java.type('int[]'), 6); for(i in 1:6) ja[i] <- i; ja[c(c(1, 3), c(5, 6))] <- 0; ja", "cat('[polyglot value]\n[1] 0 2 0 4 0 0\n')");

        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " to$fieldStringArray[c(1, 3)] <- c('x', 'x', 'x'); to$fieldStringArray",
                        errorIn("to$fieldStringArray[c(1, 3)] <- c(\"x\", \"x\", \"x\")", "number of items to replace is not a multiple of replacement length"));

        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " to[c('fieldInteger', 'fieldDouble')] <- c(111L, 222); to['fieldInteger'] ", "111");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " to[c('fieldInteger', 'fieldDouble')] <- c(111L, 222); to['fieldDouble'] ", "222");

        assertEvalFastR(Ignored.ImplementationError, CREATE_TEST_ARRAYS + " ta$integerArray2[c(1,2), c(1,2)] <- 1", "just wrong");

        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " to$fieldStringArray[c(1, 3)] <- c('x', 'y'); to$fieldStringArray", "cat('[polyglot value]\n[1] \"x\" \"b\" \"y\"\n')");
        assertEvalFastR("ja <- new(java.type('int[]'), 6); for(i in 1:6) ja[i] <- i; ja[c(1, 3, 5, 6)] <- c(8, 9); ja", "cat('[polyglot value]\n[1] 8 2 9 4 8 9\n')");

        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " to$fieldStringArray[c(TRUE, FALSE)] <- 'x'; to$fieldStringArray", "cat('[polyglot value]\n[1] \"x\" \"b\" \"x\"\n')");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " to$fieldStringArray[c(TRUE, FALSE)] <- c('x', 'y'); to$fieldStringArray", "cat('[polyglot value]\n[1] \"x\" \"b\" \"y\"\n" +
                        errorIn("In to$fieldStringArray[c(TRUE, FALSE)] <- c(\"x\", \"y\")", "number of items to replace is not a multiple of replacement length", true, true) + "')");
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
    public void testNamesForForeignObject() {
        assertEvalFastR("tc <- java.type('" + TestNamesClassNoMembers.class.getName() + "'); t <- .fastr.interop.new(tc); names(t)", "'class'");
        assertEvalFastR("tc <- java.type('" + TestNamesClassNoPublicMembers.class.getName() + "'); t <- .fastr.interop.new(tc); names(t)", "'class'");
        assertEvalFastR("tc <- java.type('" + TestNamesClass.class.getName() + "'); c('staticField', 'staticMethod') %in% names(tc)", "c(TRUE, TRUE)");
        assertEvalFastR("tc <- java.type('" + TestNamesClass.class.getName() + "'); names(tc$staticField)", "NULL");
        assertEvalFastR("tc <- java.type('" + TestNamesClass.class.getName() + "'); names(tc$staticMethod)", "NULL");
        assertEvalFastR("tc <- java.type('" + TestNamesClass.class.getName() + "'); t <- .fastr.interop.new(tc); sort(names(t))",
                        "c('class', 'field', 'method', 'staticField', 'staticMethod', 'superField', 'superMethod')");

        assertEvalFastR("names(java.type('int[]'))", "'class'");
        assertEvalFastR("names(new(java.type('int[]'), 3))", "NULL");
    }

    @Test
    public void testReturnsNull() {
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "is.null(to$methodReturnsNull())", "TRUE");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "is.null(to$fieldNullObject)", "TRUE");
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

        assertPassingForeighObjectToFunction("is.nan", errorIn("is.nan(to)", "default method not implemented for type 'polyglot.value'"));
        assertPassingForeighObjectToFunction("is.finite", errorIn("is.finite(to)", "default method not implemented for type 'polyglot.value'"));
        assertPassingForeighObjectToFunction("is.infinite", errorIn("is.infinite(to)", "default method not implemented for type 'polyglot.value'"));

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
    public void testCRBind() {
        testCRBind("cbind");
        testCRBind("rbind");
    }

    public void testCRBind(String fun) {
        assertEvalFastR(CREATE_TEST_ARRAYS + " v <- ta$integerArray; " + fun + "(v)", " v <- c(1, 2, 3); " + fun + "(v)");
        assertEvalFastR(CREATE_TEST_ARRAYS + " v <- ta$integerArray; " + fun + "(v, 1)", " v <- c(1, 2, 3); " + fun + "(v, 1)");
        assertEvalFastR(Output.IgnoreWhitespace, CREATE_TEST_ARRAYS + " v1 <- ta$integerArray; v2 <- ta$stringArray; " + fun + "(v1, v2)",
                        "v1 <- c(1, 2,  3); v2 <- c('a', 'b', 'c'); " + fun + "(v1, v2)");
        assertEvalFastR(CREATE_TEST_ARRAYS + " v1 <- ta$integerArray2; v2 <- ta$stringArray; " + fun + "(v1, v2)",
                        "v1 <- c(1, 1, 2, 2, 3, 3); dim(v1) <- c(2,3); v2 <- c('a', 'b', 'c'); " + fun + "(v1, v2)");
        assertEvalFastR(CREATE_TEST_ARRAYS + " v1 <- ta$integerArray2; v2 <- ta$stringArray2; " + fun + "(v1, v2)",
                        "v1 <- c(1, 1, 2, 2, 3, 3); dim(v1) <- c(2,3); v2 <- c('a', 'a', 'b', 'b', 'c', 'c'); dim(v2) <- c(2,3); " + fun + "(v1, v2)");
    }

    @Test
    public void testMatrix() {
        assertEvalFastR("ja <- new(java.type('int[]'), 6); for(i in 1:6) ja[i] <- i; matrix(ja, c(3, 2))", "matrix(1:6, c(3, 2))");
        assertEvalFastR(Ignored.ImplementationError, "ja <- new(java.type('int[]'), 6); for(i in 1:6) ja[i] <- i; .fastr.inspect(matrix(ja, c(3, 2)))",
                        "'com.oracle.truffle.r.runtime.data.RIntVector'");
        assertEvalFastR(CREATE_TEST_ARRAYS + "matrix(ta$integerArray2, c(3, 2))", "v <- c(1, 1, 2, 2, 3, 3); dim(v) <- c(2, 3); matrix(v, c(3, 2))");
        assertEvalFastR(CREATE_TEST_ARRAYS + "matrix(ta$integerArray3, c(3, 2, 2))", "v <- c(1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3); dim(v) <- c(2, 2, 3); matrix(v, c(3, 2, 2))");
    }

    @Test
    public void testArray() {
        assertEvalFastR("ja <- new(java.type('int[]'), 6); for(i in 1:6) ja[i] <- i; array(ja, c(3, 2))", "matrix(1:6, c(3, 2))");
        assertEvalFastR(Ignored.ImplementationError, "ja <- new(java.type('int[]'), 6); for(i in 1:6) ja[i] <- i; .fastr.inspect(array(ja, c(3, 2)))",
                        "'com.oracle.truffle.r.runtime.data.RIntVector'");
        assertEvalFastR(CREATE_TEST_ARRAYS + "array(ta$integerArray2, c(3, 2))", "v <- c(1, 1, 2, 2, 3, 3); dim(v) <- c(2, 3); array(v, c(3, 2))");
        assertEvalFastR(CREATE_TEST_ARRAYS + "array(ta$integerArray3, c(3, 2, 2))", "v <- c(1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3); dim(v) <- c(2, 2, 3); array(v, c(3, 2, 2))");
    }

    @Test
    public void testAttributes() {
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " attributes(to)", "NULL");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " attr(to, 'a')<-'a'", errorIn("attr(to, \"a\") <- \"a\"", "polyglot value cannot be attributed"));
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " attr(to, which = 'a')", errorIn("attr(to, which = \"a\")", "polyglot value cannot be attributed"));
    }

    @Test
    public void testIdentical() {
        assertEvalFastR("b1 <- .fastr.interop.asByte(1); identical(b1, b1)", "TRUE");
        assertEvalFastR("b1 <- .fastr.interop.asByte(1); b2 <- .fastr.interop.asByte(1); identical(b1, b2)", "FALSE");
        assertEvalFastR("b1 <- .fastr.interop.asByte(1); s1 <- .fastr.interop.asShort(1); identical(b1, s1)", "FALSE");
    }

    @Test
    public void testAsVectorFromArray() throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
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
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " as.vector(to$mixedTypesArray)", "list(" + getTestFieldValuesAsResult(new TestClass(), "mixedTypesArray") + ")");

        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " v <- as.vector(to$hasNullIntArray); typeof(v)", "'integer'");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " v <- as.vector(to$hasNullIntArray); v[1]", "1");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " v <- as.vector(to$hasNullIntArray); v[2]", "NA");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " v <- as.vector(to$hasNullIntArray); v[3]", "3");

        assertEvalFastR("as.vector(new(java.type('java.lang.Integer[]'), 0L), 'character');", "character()");

        assertEvalFastR(CREATE_TEST_ARRAYS + " as.vector(ta$booleanArray2)", "c(T, T, F, F, T, T)");
        assertEvalFastR(CREATE_TEST_ARRAYS + " as.vector(ta$booleanArray3)", "c(T, T, T, T, F, F, F, F, T, T, T, T)");

        assertEvalFastR(CREATE_TEST_ARRAYS + " as.vector(ta$booleanArray2, 'list')", "list(T, T, F, F, T, T)");
        assertEvalFastR(CREATE_TEST_ARRAYS + " as.vector(ta$booleanArray3, 'list')", "list(T, T, T, T, F, F, F, F, T, T, T, T)");

        assertEvalFastR(CREATE_TEST_ARRAYS + " as.vector(ta$mixedObjectArray)", "list(1, 'a', '1')");
        assertEvalFastR(CREATE_TEST_ARRAYS + " as.vector(ta$mixedObjectArray2)", "l <- list('a', 1, 'b', 2, 'c', 3); dim(l) <- c(2, 3); l");
        // TODO is this expected behaviour?
        // the closesest would be as.list(matrix(c(1, NA, 2, NA, 3, NA), c(2,3)))
        assertEvalFastR(CREATE_TEST_ARRAYS + " as.vector(ta$mixedObjectArray2)", "l <- list('a', 1, 'b', 2, 'c', 3); dim(l) <- c(2, 3); l");

        // TODO add tests for as.vector(ta$objectArray/2/3),
        // TODO add tests for as.list(ta$objectArray/2/3),
        // TODO add tests for .fastr.interop.asVector(ta$objectArray/2/3),
        // assertEvalFastR(CREATE_TEST_ARRAYS + " as.vector(ta$objectArray)", "polyglotvalues");

        assertEvalFastR(Ignored.ImplementationError, "as.vector(new(java.type('java.lang.Integer[]'), 1))", "integer()");

        assertEvalFastR("talc <- new('" + TestAsListClassMixed.class.getName() + "')" + "; as.vector(talc)",
                        errorIn("as.vector(talc)", "no method for coercing this polyglot value to a vector"));
    }

    public void testAsVectorFromArray(String field, String type) {
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " v <- as.vector(to$" + field + "); is.vector(v)", "TRUE");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + " v <- as.vector(to$" + field + "); typeof(v)", getRValue(type));
    }

    @Test
    public void testAsList() throws IllegalArgumentException {
        // foreign atomic arrays
        assertEvalFastR(CREATE_TEST_ARRAYS + " as.list(ta$stringArray)", "list('a', 'b', 'c')");
        assertEvalFastR(CREATE_TEST_ARRAYS + " as.list(ta$stringArray2)", "list('a', 'a', 'b', 'b', 'c', 'c')");
        assertEvalFastR(CREATE_TEST_ARRAYS + " as.list(ta$stringArray3)", "list('a', 'a', 'a', 'a', 'b', 'b', 'b', 'b', 'c', 'c', 'c', 'c')");

        assertEvalFastR(CREATE_TEST_ARRAYS + " as.list(ta$booleanArray)", "list(T, F, T)");
        assertEvalFastR(CREATE_TEST_ARRAYS + " as.list(ta$booleanArray2)", "list(T, T, F, F, T, T)");
        assertEvalFastR(CREATE_TEST_ARRAYS + " as.list(ta$booleanArray3)", "list(T, T, T, T, F, F, F, F, T, T, T, T)");

        // foreign heterogenous arrays
        assertEvalFastR(CREATE_TEST_ARRAYS + " as.list(ta$mixedObjectArray)", "list(1, 'a', '1'))");
        // TODO is this expected behaviour?
        assertEvalFastR(CREATE_TEST_ARRAYS + " as.list(ta$mixedObjectArray2)", "l <- list('a', 1, 'b', 2, 'c', 3); dim(l) <- c (2, 3); l");

        // foreign object with members
        assertEvalFastR("talc <- new('" + TestAsListClass.class.getName() + "')" + "; as.list(talc)", "list(b=c(T, F, T), i=c(1, 2, 3))");

        String result = "b2 <- c(T, T, F, F, T, T); i2 <- c(1, 1, 2, 2, 3, 3); list(b=b2, i=i2)";
        assertEvalFastR("talc <- new('" + TestAsListClass2.class.getName() + "')" + "; as.list(talc)", result);

        result = "b3 <- c(T, T, T, T, F, F, F, F, T, T, T, T); " + "i3 <- c(1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3); list(b=b3, i=i3)";
        assertEvalFastR("talc <- new('" + TestAsListClass3.class.getName() + "')" + "; as.list(talc)", result);

        result = "b2 <- c(T, T, F, F, T, T); i <- c(1,2,3); oa <- list(T, 'a', F, 'b', T, 'c'); dim(oa)  <- c(2, 3); list(b=b2, i=i, oa=oa, o=NULL)";
        assertEvalFastR("talc <- new('" + TestAsListClassMixed.class.getName() + "')" + "; as.list(talc)", result);

        // TODO test for as.vector too
        assertEvalFastR(Ignored.ImplementationError, "ta <- new('" + TestMultiDimArraysClass.class.getName() + "'); as.list(ta$integerArray2x3x4x5)", "c(2, 3, 4, 5)");
    }

    @Test
    public void testFastrInteropAsVector() throws IllegalArgumentException {
        assertEvalFastR(".fastr.interop.asVector(1)", "NULL");

        // TODO currently can't tell what type the array is if no elemenet or only null
        assertEvalFastR(Ignored.ImplementationError, "ja <- new(java.type('java.lang.String[]'), 0L); .fastr.interop.asVector(ja)", "character()");
        assertEvalFastR(Ignored.ImplementationError, "ja <- new(java.type('java.lang.String[]'), 1L); .fastr.interop.asVector(ja)", "character()");

        assertEvalFastR(CREATE_TEST_ARRAYS + " .fastr.interop.asVector(ta$shortArray, recursive=TRUE, dropDimensions=TRUE)", "c(1, 2, 3)");
        assertEvalFastR(CREATE_TEST_ARRAYS + " .fastr.interop.asVector(ta$shortArray2, recursive=TRUE, dropDimensions=TRUE)", "c(1, 1, 2, 2, 3, 3)");
        assertEvalFastR(CREATE_TEST_ARRAYS + " .fastr.interop.asVector(ta$shortArray3, recursive=TRUE, dropDimensions=TRUE)", "c(1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3)");

        assertEvalFastR(CREATE_TEST_ARRAYS + " .fastr.interop.asVector(ta$shortArray, recursive=TRUE, dropDimensions=FALSE)", "c(1, 2, 3)");
        assertEvalFastR(CREATE_TEST_ARRAYS + " .fastr.interop.asVector(ta$shortArray2, recursive=TRUE, dropDimensions=FALSE)", "v <- c(1, 1, 2, 2, 3, 3)); dim(v) <- c(2, 3); v");
        assertEvalFastR(CREATE_TEST_ARRAYS + " .fastr.interop.asVector(ta$shortArray3, recursive=TRUE, dropDimensions=FALSE)", "v <- c(1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3)); dim(v) <- c(2, 2, 3); v");

        assertEvalFastR(CREATE_TEST_ARRAYS + " .fastr.interop.asVector(ta$shortArray, recursive=FALSE)", "c(1, 2, 3)");
        assertEvalFastR(CREATE_TEST_ARRAYS + " .fastr.interop.asVector(ta$shortArray2, recursive=FALSE)", "cat('[[1]]\n[polyglot value]\n\n[[2]]\n[polyglot value]\n\n')");
        assertEvalFastR(CREATE_TEST_ARRAYS + " .fastr.interop.asVector(ta$shortArray3, recursive=FALSE)", "cat('[[1]]\n[polyglot value]\n\n[[2]]\n[polyglot value]\n\n')");

        assertEvalFastR(CREATE_TEST_ARRAYS + " .fastr.interop.asVector(ta$mixedObjectArray)", "list(1, 'a', '1'))");
        assertEvalFastR(CREATE_TEST_ARRAYS + " .fastr.interop.asVector(ta$mixedObjectArray2, T)", "l <- list('a', 1, 'b', 2, 'c', 3); dim(l) <- c(2, 3); l");

        String result = "b2 <- c(T, T, F, F, T, T); i2 <- c(1, 1, 2, 2, 3, 3); dim(b2) <- c(2, 3); dim(i2) <- c(2, 3); list(b=b2, i=i2)";
        assertEvalFastR("talc <- new('" + TestAsListClass2.class.getName() + "')" + "; .fastr.interop.asVector(talc, T)", result);

        result = "b3 <- c(T, T, T, T, F, F, F, F, T, T, T, T); " + "i3 <- c(1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3); dim(b3) <- c(2, 2, 3); dim(i3) <- c(2, 2, 3); list(b=b3, i=i3)";
        assertEvalFastR("talc <- new('" + TestAsListClass3.class.getName() + "')" + "; .fastr.interop.asVector(talc, T)", result);

        result = "b2 <- c(T, T, F, F, T, T); dim(b2) <- c(2, 3); i <- c(1,2,3); oa <- list(T, 'a', F, 'b', T, 'c'); dim(oa) <- c(2, 3); list(b=b2, i=i, oa=oa, o=NULL)";
        assertEvalFastR("talc <- new('" + TestAsListClassMixed.class.getName() + "')" + "; .fastr.interop.asVector(talc, T)", result);
    }

    @Test
    public void testIsMatrix() {
        assertEvalFastR(CREATE_TEST_ARRAYS + " is.matrix(ta$booleanArray)", "FALSE");
        assertEvalFastR(CREATE_TEST_ARRAYS + " is.matrix(ta$booleanArray2)", "TRUE");
        assertEvalFastR(CREATE_TEST_ARRAYS + " is.matrix(ta$booleanArray3)", "FALSE");
    }

    @Test
    public void testIsArray() {
        assertEvalFastR(CREATE_TEST_ARRAYS + " is.array(ta$booleanArray)", "FALSE");
        assertEvalFastR(CREATE_TEST_ARRAYS + " is.array(ta$booleanArray2)", "TRUE");
        assertEvalFastR(CREATE_TEST_ARRAYS + " is.array(ta$booleanArray3)", "TRUE");
    }

    @Test
    public void testAsMatrix() throws IllegalArgumentException {
        assertEvalFastR(CREATE_TEST_ARRAYS + " as.matrix(ta$stringArray)", "as.matrix(c('a', 'b', 'c'))");
        assertEvalFastR(CREATE_TEST_ARRAYS + " as.matrix(ta$stringArray2)", "a2 <- c('a', 'a', 'b', 'b', 'c', 'c'); dim(a2) <- c(2, 3); as.matrix(a2)");
        assertEvalFastR(CREATE_TEST_ARRAYS + " as.matrix(ta$stringArray3)", "a3 <- c('a', 'a', 'a', 'a', 'b', 'b', 'b', 'b', 'c', 'c', 'c', 'c'); dim(a3) <- c(2, 2, 3); as.matrix(a3)");
        assertEvalFastR(CREATE_TEST_ARRAYS + " as.matrix(ta$mixedObjectArray)", "as.matrix(list(1, 'a', '1'))");
        assertEvalFastR(CREATE_TEST_ARRAYS + " as.matrix(ta$mixedObjectArray2)", "l <- list('a', 1, 'b', 2, 'c', 3); dim(l) <- c(2, 3); as.matrix(l)");

        assertEvalFastR("talc <- new('" + TestAsListClass.class.getName() + "')" + "; as.matrix(talc)", "as.matrix(list(b=c(T, F, T), i=c(1L, 2L, 3L)))");
    }

    @Test
    public void testAsArray() throws IllegalArgumentException {
        assertEvalFastR(CREATE_TEST_ARRAYS + " as.array(ta$stringArray)", "as.array(c('a', 'b', 'c'))");
        assertEvalFastR(CREATE_TEST_ARRAYS + " as.array(ta$stringArray2)", "a2 <- c('a', 'a', 'b', 'b', 'c', 'c'); dim(a2) <- c(2, 3); as.array(a2)");
        assertEvalFastR(CREATE_TEST_ARRAYS + " as.array(ta$stringArray3)", "a3 <- c('a', 'a', 'a', 'a', 'b', 'b', 'b', 'b', 'c', 'c', 'c', 'c'); dim(a3) <- c(2, 2, 3); as.array(a3)");
        assertEvalFastR(CREATE_TEST_ARRAYS + " as.array(ta$mixedObjectArray)", "as.array(list(1, 'a', '1'))");
    }

    @Test
    public void testAsDataFrame() throws IllegalArgumentException {
        // foreign arrays
        assertEvalFastR(CREATE_TEST_ARRAYS + " a1 <- ta$stringArray; as.data.frame(a1)", "a1 <- c('a', 'b', 'c'); as.data.frame(a1)");

        String result = "a2 <- 'a', 'a', 'b', 'b', 'c', 'c'; dim(a2) <- c(2,3); as.data.frame(a2)";
        assertEvalFastR(CREATE_TEST_ARRAYS + " a2 <- ta$stringArray2; as.data.frame(a2)", result);

        result = "a3 <- 'a', 'a', 'a', 'a', 'b', 'b', 'b', 'b', 'c', 'c', 'c', 'c'; dim(a3) <- c(2, 2, 3); as.data.frame(a3)";
        assertEvalFastR(CREATE_TEST_ARRAYS + " a3 <- ta$stringArray3; as.data.frame(a3)", result);

        assertEvalFastR(CREATE_TEST_ARRAYS + " ma <- ta$mixedObjectArray; as.data.frame(ma)", "ma <- list(1L, 'a', '1'); as.data.frame(ma)");

        // foreign object with members
        assertEvalFastR("talc <- new('" + TestAsListClass.class.getName() + "')" + "; as.data.frame(talc)", "as.data.frame(list(b=c(T, F, T), i=c(1, 2, 3)))");

        result = "b2 <- c(T, T, F, F, T, T); i2 <- c(1, 1, 2, 2, 3, 3); dim(b2) <- c(2, 3); dim(i2) <- c(2, 3); as.data.frame(list(b=b2, i=i2))";
        assertEvalFastR("talc <- new('" + TestAsListClass2.class.getName() + "')" + "; as.data.frame(talc)", result);

        result = "b3 <- c(T, T, T, T, F, F, F, F, T, T, T, T); " + "i3 <- c(1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3); dim(b3) <- c(2, 2, 3); dim(i3) <- c(2, 2, 3); as.data.frame(list(b=b3, i=i3))";
        assertEvalFastR("talc <- new('" + TestAsListClass3.class.getName() + "')" + "; as.data.frame(talc)", result);

        result = "b2 <- c(T, T, F, F, T, T); i <- c(1,2,3); dim(b2) <- c(2, 3); as.data.frame(list(b=b2, i=i, o=NULL))";
        assertEvalFastR(Output.IgnoreErrorContext, "talc <- new('" + TestAsListClassMixed.class.getName() + "')" + "; as.data.frame(talc)", result);

        // proxies
        result = "as.data.frame(list(x=c(1, 2, 3), y=(T, T, T)))";
        assertEvalFastR("tpa <- new('" + TestProxyArray.class.getName() + "')" + "; as.data.frame(c(1,2,3))", result);

        result = "as.data.frame(list(x=c(1, 2, 3), y=(T, T, T)))";
        assertEvalFastR("tdfpo <- new('" + TestDFProxyObject.class.getName() + "')" + "; as.data.frame(tdfpo)", result);
    }

    @Test
    public void testUnlist() throws IllegalArgumentException {
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
    }

    private void testUnlistByType(String fieldType, String result, String clazz) {
        testForeingObjectUnlist(fieldType + "Array", result, clazz);
        if (!fieldType.equals("string")) {
            testForeingObjectUnlist(fieldType + "ObjectArray", result, clazz);
        }
    }

    private void testForeingObjectUnlist(String fieldPrefix, String result, String clazz) {
        String field = fieldPrefix;
        assertEvalFastR(CREATE_TEST_ARRAYS + " unlist(ta$" + field + ")", "c(" + result + ")");
        assertEvalFastR(CREATE_TEST_ARRAYS + " class(unlist(ta$" + field + "))", "'" + clazz + "'");
        assertEvalFastR(CREATE_TEST_ARRAYS + " unlist(ta$" + field + ", recursive=FALSE)", "c(" + result + ")");

        // TODO + "3"
        field = fieldPrefix + "2";
        String resultVector = "c(" + result + ", " + result + ")";
        // TODO: missing tests for iterable which does not behave like truffle array;
        // String expected = fieldPrefix.contains("Iterable") ? resultVector : "matrix(" +
        // resultVector + ", nrow=2, ncol=3, byrow=T)";
        String expected = "matrix(" + resultVector + ", nrow=2, ncol=3, byrow=T)";

        assertEvalFastR(CREATE_TEST_ARRAYS + " unlist(ta$" + field + ")", expected);
        expected = "'matrix'";
        assertEvalFastR(CREATE_TEST_ARRAYS + " class(unlist(ta$" + field + "))", expected);
        assertEvalFastR(CREATE_TEST_ARRAYS + " unlist(ta$" + field + ", recursive=FALSE)", "c(" + result + ", " + result + ")");
    }

    private static String getTestFieldValuesAsResult(Object testClass, String name) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        Field f = testClass.getClass().getDeclaredField(name);
        Object value = f.get(testClass);
        if (value instanceof List) {
            List<?> l = (List<?>) value;
            value = l.toArray(new Object[l.size()]);
        }
        StringBuilder sb = new StringBuilder();
        int length = Array.getLength(value);
        for (int i = 0; i < length; i++) {
            Object object = Array.get(value, i);
            if (object == null) {
                sb.append("NULL");
            } else if (object instanceof Boolean) {
                sb.append(Boolean.toString((boolean) object).toUpperCase());
            } else if (object instanceof Float) {
                sb.append((double) ((float) object));
            } else if (object instanceof String || object instanceof Character) {
                sb.append("'").append(object).append("'");
            } else {
                sb.append(object);
            }
            if (i < length - 1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    @Test
    public void testDim() {
        assertEvalFastR(CREATE_TEST_ARRAYS + " dim(ta$integerArray)", "NULL");
        assertEvalFastR(CREATE_TEST_ARRAYS + " dim(ta$integerArray2)", "c(2, 3)");
        assertEvalFastR(CREATE_TEST_ARRAYS + " dim(ta$integerArray3)", "c(2, 2, 3)");
        assertEvalFastR(CREATE_TEST_ARRAYS + " dim(ta$integerArray3)", "c(2, 2, 3)");

        assertEvalFastR(CREATE_TEST_ARRAYS + " dim(ta)", "NULL");

        assertEvalFastR("ta <- new('" + TestMultiDimArraysClass.class.getName() + "'); dim(ta$integerArray3NotSquare)", "NULL");
        assertEvalFastR("ta <- new('" + TestMultiDimArraysClass.class.getName() + "'); dim(ta$integerArray2NotSquare)", "NULL");
        assertEvalFastR("ta <- new('" + TestMultiDimArraysClass.class.getName() + "'); dim(ta$integerArray2x3x4x5)", "c(2, 3, 4, 5)");
    }

    @Test
    public void testMultiDimArrays() throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        TestMultiDimArraysClass tac = new TestMultiDimArraysClass();
        testMultiDimArrayElement(tac, "string");
        testMultiDimArrayElement(tac, "boolean");
        testMultiDimArrayElement(tac, "byte");
        testMultiDimArrayElement(tac, "char");
        testMultiDimArrayElement(tac, "double");
        testMultiDimArrayElement(tac, "float");
        testMultiDimArrayElement(tac, "integer");
        testMultiDimArrayElement(tac, "long");
        testMultiDimArrayElement(tac, "short");

        testMultiDimArrayElement("integerArray2x3x4x5", "2,3,4,5", new ArrayList<>(),
                        tac.integerArray2x3x4x5);

        assertEvalFastR("ta <- new('" + TestMultiDimArraysClass.class.getName() + "'); as.vector(ta$integerArray2NotSquare)", "c(1:7)");
    }

    private void testMultiDimArrayElement(TestMultiDimArraysClass tac, String fieldPrefix) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        String fieldName = fieldPrefix + "Array2";
        Object array = tac.getClass().getDeclaredField(fieldName).get(tac);
        testMultiDimArrayElement(fieldName, "2, 3", new ArrayList<>(), array);

        fieldName = fieldPrefix + "Array3";
        array = tac.getClass().getDeclaredField(fieldName).get(tac);
        testMultiDimArrayElement(fieldName, "2, 2, 3", new ArrayList<>(), array);
    }

    private void testMultiDimArrayElement(String fieldName, String dims, final List<Integer> coor, Object obj) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        if (obj.getClass().isArray()) {
            for (int i = 0; i < Array.getLength(obj); i++) {
                List<Integer> c = new ArrayList<>(coor);
                c.add(i);
                testMultiDimArrayElement(fieldName, dims, c, Array.get(obj, i));
            }
        } else {
            // as.vector(ta$integerArray3)[1,2,3] == ta$integerArray3[1,2,3]
            String vectorCor = getRLikeCoordinates(coor);
            assertEvalFastR("ta <- new('" + TestMultiDimArraysClass.class.getName() + "'); v <- as.vector(ta$" + fieldName + "); dim(v) <- c(" + dims + "); v" + vectorCor + " == ta$" + fieldName +
                            vectorCor, "TRUE");

            // as.vector(ta$integerArray3)[1,2,3] == ta$integerArray3[1][2][3]
            String javaCor = getJavaLikeCoordinates(coor);
            assertEvalFastR("ta <- new('" + TestMultiDimArraysClass.class.getName() + "'); v <- as.vector(ta$" + fieldName + "); dim(v) <- c(" + dims + "); v" + vectorCor + " == ta$" + fieldName +
                            javaCor, "TRUE");
        }
    }

    private static String getRLikeCoordinates(final List<Integer> dims) {
        StringBuilder cor = new StringBuilder();
        cor.append("[");
        for (int i = 0; i < dims.size(); i++) {
            cor.append(dims.get(i) + 1);
            if (i < dims.size() - 1) {
                cor.append(", ");
            }
        }
        cor.append("]");
        return cor.toString();
    }

    private static String getJavaLikeCoordinates(final List<Integer> dims) {
        StringBuilder cor = new StringBuilder();
        for (int i = 0; i < dims.size(); i++) {
            cor.append("[");
            cor.append(dims.get(i) + 1);
            cor.append("]");
        }
        return cor.toString();
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
                } else if ((asXXX.equals("as.raw")) && (name.contains("String") || name.contains("Char"))) {
                    assertEvalFastR(Output.IgnoreWarningMessage, expr, getAsXXX(f.get(t), asXXX));
                } else if (asXXX.equals("as.expression") && (name.contains("Long") || name.contains("Double"))) {
                    assertEvalFastR(Ignored.ImplementationError, expr, getAsXXX(f.get(t), asXXX));
                } else if (asXXX.equals("as.raw") && (name.contains("Long") || name.contains("Double") || name.contains("NaN"))) {
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
            assertEvalFastR(Output.IgnoreWarningMessage, CREATE_TRUFFLE_OBJECT + asXXX + "(to$fieldStringBooleanArray);", toRVector(t.fieldStringBooleanArray, asXXX));
        }

        if (asXXX.equals("as.expression")) {
            assertEvalFastR(Output.IgnoreErrorContext, CREATE_TRUFFLE_OBJECT + asXXX + "(to);", errorIn(asXXX + "(to)", "no method for coercing this polyglot value to a vector"));
        } else if (asXXX.equals("as.raw") || asXXX.equals("as.complex")) {
            assertEvalFastR(CREATE_TRUFFLE_OBJECT + asXXX + "(to);", errorIn(asXXX + "(to)", "cannot coerce type 'polyglot.value' to vector of type '" + type + "'"));
        } else {
            assertEvalFastR(CREATE_TRUFFLE_OBJECT + asXXX + "(to);", errorIn(asXXX + "(to)", "no method for coercing this polyglot value to a vector"));
        }
    }

    @Test
    public void testNoCopyOncast() throws IllegalArgumentException, NoSuchFieldException, IllegalAccessException {
        testNoCopyOncast("integer", "RToIntVectorClosure", new String[]{"fieldBooleanArray", "fieldDoubleArray", "fieldStringArray"});
        testNoCopyOncast("double", "RToDoubleVectorClosure", new String[]{"fieldBooleanArray", "fieldIntegerArray", "fieldStringArray"});
        testNoCopyOncast("complex", "RToComplexVectorClosure", new String[]{"fieldBooleanArray", "fieldIntegerArray", "fieldDoubleArray", "fieldStringArray"});
        testNoCopyOncast("character", "RToStringVectorClosure", new String[]{"fieldBooleanArray", "fieldIntegerArray", "fieldDoubleArray"});
    }

    public void testNoCopyOncast(String type, String closure, String... fields) throws IllegalArgumentException, NoSuchFieldException, IllegalAccessException {
        for (String field : fields) {
            if (!"character".equals(type) && field.contains("String")) {
                assertEvalFastR(Output.IgnoreWarningMessage, CREATE_TRUFFLE_OBJECT + "as." + type + "(as.vector(to$" + field + "));", "as." + type + "(" + toRVector(new TestClass(), field) + ")");
            } else {
                assertEvalFastR(CREATE_TRUFFLE_OBJECT + "as." + type + "(as.vector(to$" + field + "));", "as." + type + "(" + toRVector(new TestClass(), field) + ")");
            }
            assertEvalFastR(CREATE_TRUFFLE_OBJECT + ".fastr.inspect(as." + type + "(as.vector(to$" + field + ")));", "cat('com.oracle.truffle.r.runtime.data.closures." + closure + "\n')");
            assertEvalFastR(CREATE_TRUFFLE_OBJECT +
                            ".fastr.inspect(as.vector(as.vector(to$" + field + "), '" + type + "'));", "cat('com.oracle.truffle.r.runtime.data.closures." + closure + "\n')");
            assertEvalFastR(CREATE_TRUFFLE_OBJECT +
                            ".fastr.inspect(as.vector(as." + type + "(to$" + field + "), '" + type + "'));", "cat('com.oracle.truffle.r.runtime.data.closures." + closure + "\n')");

        }
    }

    @Test
    public void testIf() throws IllegalArgumentException {
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "if(to$fieldBoolean) print('OK')", "if(T) print('OK')");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "if(to$fieldInteger) print('OK')", "if(1) print('OK')");

        assertEvalFastR(Output.IgnoreWarningContext, CREATE_TRUFFLE_OBJECT + "if(to$fieldBooleanArray) print('OK')", "if(c(T, F)) print('OK')");
        assertEvalFastR(Output.IgnoreWarningContext, CREATE_TRUFFLE_OBJECT + "if(to$fieldIntegerArray) print('OK')", "if(c(T, F)) print('OK')");
        assertEvalFastR(Output.IgnoreWarningContext, Output.IgnoreErrorContext, CREATE_TRUFFLE_OBJECT + "if(to$fieldStringArray) print('OK')", "if(c('a', 'b')) print('OK')");

        assertEvalFastR(Output.IgnoreWarningContext, CREATE_TRUFFLE_OBJECT + "if(to$fieldStringBooleanArray) print('OK')", "if(c('TRUE', 'TRUE', 'FALSE')) print('OK')");

        assertEvalFastR(Output.IgnoreErrorContext, CREATE_TRUFFLE_OBJECT + "if(to) print('OK')", errorIn("if (T) print('OK')", " argument is not interpretable as logical"));
    }

    @Test
    public void testWhile() throws IllegalArgumentException {
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "while(to$fieldBoolean) {print('OK'); break;}", "while(T) {print('OK'); break;}");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "while(to$fieldInteger) {print('OK'); break;}", "while(1) {print('OK'); break;}");

        assertEvalFastR(Output.IgnoreWarningContext, CREATE_TRUFFLE_OBJECT + "while(to$fieldBooleanArray) {print('OK'); break;}", "while(c(T, F)) {print('OK'); break;}");
        assertEvalFastR(Output.IgnoreWarningContext, CREATE_TRUFFLE_OBJECT + "while(to$fieldIntegerArray) {print('OK'); break;}", "while(c(T, F)) {print('OK'); break;}");
        assertEvalFastR(Output.IgnoreWarningContext, Output.IgnoreErrorContext, CREATE_TRUFFLE_OBJECT + "if(to$fieldStringArray) {print('OK'); break;}", "while(c('a', 'b')) {print('OK'); break;}");

        assertEvalFastR(Output.IgnoreWarningContext, CREATE_TRUFFLE_OBJECT + "while(to$fieldStringBooleanArray) {print('OK'); break;}", "while(c('TRUE', 'TRUE', 'FALSE')) {print('OK'); break;}");

        assertEvalFastR(Output.IgnoreErrorContext, CREATE_TRUFFLE_OBJECT + "while(to) print('OK')", errorIn("if (T) print('OK')", " argument is not interpretable as logical"));
    }

    @Test
    public void testFor() {
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "for(i in to$fieldIntegerArray) print(i)", "for(i in c(1,2,3)) print(i)");
    }

    @Test
    public void testElseIf() throws IllegalArgumentException {
        assertEvalFastR(CREATE_TEST_ARRAYS + " ifelse(ta)", errorIn("as.logical(test)", "no method for coercing this polyglot value to a vector"));
        assertEvalFastR(CREATE_TEST_ARRAYS + " ifelse(ta$booleanArray, 1, 2)", "c(1,2,1)");
        assertEvalFastR(CREATE_TEST_ARRAYS + " ifelse(ta$integerArray, 1, 2)", "c(1,1,1)");
        assertEvalFastR(CREATE_TEST_ARRAYS + " ifelse(ta$stringArray, 1, 2)", "c(NA, NA, NA)");
    }

    @Test
    public void testArraysWithNullConversion() throws IllegalArgumentException {
        assertEvalFastR(CREATE_TEST_ARRAYS + "as.vector(ta$booleanObjectArrayWithNull)", "c(T, NA, T)");
        assertEvalFastR(CREATE_TEST_ARRAYS + "as.vector(ta$byteObjectArrayWithNull)", "c(1, NA, 3)");
        assertEvalFastR(CREATE_TEST_ARRAYS + "as.vector(ta$charObjectArrayWithNull)", "c('a', NA, 'c')");
        assertEvalFastR(CREATE_TEST_ARRAYS + "as.vector(ta$doubleObjectArrayWithNull)", "c(1.1, NA, 1.3)");
        assertEvalFastR(CREATE_TEST_ARRAYS + "as.vector(ta$floatObjectArrayWithNull)", "c(1.1, NA, 1.3)");
        assertEvalFastR(CREATE_TEST_ARRAYS + "as.vector(ta$integerObjectArrayWithNull)", "c(1L, NA, 3L)");
        assertEvalFastR(CREATE_TEST_ARRAYS + "as.vector(ta$longObjectArrayWithNull)", "c(1, NA, 3)");
        assertEvalFastR(CREATE_TEST_ARRAYS + "as.vector(ta$shortObjectArrayWithNull)", "c(1L, NA, 3L)");
        assertEvalFastR(CREATE_TEST_ARRAYS + "as.vector(ta$stringArrayWithNull)", "c('a', NA, 'c')");
    }

    @Test
    public void testForeignVectorArithmeticOp() throws NoSuchFieldException,
                    IllegalAccessException {
        TestJavaInterop.this.testForeignVectorArithmeticOp("fieldBooleanArray", false, "integer(0)");
        TestJavaInterop.this.testForeignVectorArithmeticOp("fieldByteArray", false, "integer(0)");
        TestJavaInterop.this.testForeignVectorArithmeticOp("fieldDoubleArray", false, "numeric(0)");
        TestJavaInterop.this.testForeignVectorArithmeticOp("fieldFloatArray", false, "numeric(0)");
        TestJavaInterop.this.testForeignVectorArithmeticOp("fieldIntegerArray", false, "integer(0)");
        TestJavaInterop.this.testForeignVectorArithmeticOp("fieldLongArray", false, "numeric(0)");
        TestJavaInterop.this.testForeignVectorArithmeticOp("fieldShortArray", false, "integer(0)");

        TestJavaInterop.this.testForeignVectorArithmeticOp("fieldCharArray", true, null);
        TestJavaInterop.this.testForeignVectorArithmeticOp("fieldStringArray", true, null);

        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "to + 1", errorIn("to + 1", "non-numeric argument to binary operator"));
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "1 + to", errorIn("1 + to", "non-numeric argument to binary operator"));
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "to + to", errorIn("to + to", "non-numeric argument to binary operator"));
    }

    private void testForeignVectorArithmeticOp(String vec, boolean fail, String expectedOKForNull) throws NoSuchFieldException, IllegalAccessException {
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

        expectedKO = errorIn("to$" + vec + " + NULL", "non-numeric argument to binary operator");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "to$" + vec + " + NULL", fail ? expectedKO : expectedOKForNull);

        expectedKO = errorIn("NULL + to$" + vec, "non-numeric argument to binary operator");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "NULL + to$" + vec, fail ? expectedKO : expectedOKForNull);
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

        testForeignVectorBooleanOp("fieldStringArray", true);

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

        testForeignVectorScalarBooleanOp(operator, "fieldCharArray", true);
        testForeignVectorScalarBooleanOp(operator, "fieldStringArray", true);

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

        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "abs(to$fieldCharArray)", errorIn("abs(to$fieldCharArray)", "non-numeric argument to mathematical function"));
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "abs(to$fieldStringArray)", errorIn("abs(to$fieldStringArray)", "non-numeric argument to mathematical function"));

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

        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "range(to$fieldCharArray)", "c('a', 'c')");
        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "range(to$fieldStringArray)", "c('a', 'c')");

        assertEvalFastR(CREATE_TRUFFLE_OBJECT + "range(to)", errorIn("range(to)", "invalid 'type' (polyglot.value) of argument"));
    }

    private static final String CREATE_EXCEPTIONS_TO = "to <- new('" + TestExceptionsClass.class.getName() + "');";

    @Test
    public void testException() {
        assertEvalFastR("to <- new('" + TestExceptionsClass.class.getName() + "', 'java.io.IOException');",
                        errorIn(".fastr.interop.new(Class, ...)", "Foreign function failed: java.io.IOException"));
        assertEvalFastR("to <- new('" + TestExceptionsClass.class.getName() + "', 'java.io.IOException', 'msg');",
                        errorIn(".fastr.interop.new(Class, ...)", "Foreign function failed: java.io.IOException: msg"));
        assertEvalFastR("to <- new('" + TestExceptionsClass.class.getName() + "', 'java.lang.RuntimeException');",
                        errorIn(".fastr.interop.new(Class, ...)", "Foreign function failed: java.lang.RuntimeException"));
        assertEvalFastR("to <- new('" + TestExceptionsClass.class.getName() + "', 'java.lang.RuntimeException', 'msg');",
                        errorIn(".fastr.interop.new(Class, ...)", "Foreign function failed: java.lang.RuntimeException: msg"));

        assertEvalFastR(CREATE_EXCEPTIONS_TO + "to$exception('java.io.IOException')", errorIn("to$exception(\"java.io.IOException\")", "Foreign function failed: java.io.IOException"));
        assertEvalFastR(CREATE_EXCEPTIONS_TO + "to$exception('java.io.IOException', 'msg')",
                        errorIn("to$exception(\"java.io.IOException\", \"msg\")", "Foreign function failed: java.io.IOException: msg"));
        assertEvalFastR(CREATE_EXCEPTIONS_TO + "to$exception('java.lang.RuntimeException')",
                        errorIn("to$exception(\"java.lang.RuntimeException\")", "Foreign function failed: java.lang.RuntimeException"));
        assertEvalFastR(CREATE_EXCEPTIONS_TO + "to$exception('java.lang.RuntimeException', 'msg')",
                        errorIn("to$exception(\"java.lang.RuntimeException\", \"msg\")", "Foreign function failed: java.lang.RuntimeException: msg"));
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
            sb.append("cat('[polyglot value]\\n[1] ");
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
        if (value instanceof TestPOJO) {
            StringBuilder sb = new StringBuilder();
            sb.append("[polyglot value]\n$data\n[1] \"").append(((TestPOJO) value).data).append("\"\n\n$toString\n[polyglot value]\n\n$getData\n[polyglot value]\n\n");
            return String.format("cat('%s')", sb.toString());
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

    private static List<?> list(Object o) {
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

    private static String errorIn(String left, String right) {
        return errorIn(left, right, false, false);
    }

    private static String errorIn(String left, String right, boolean warning, boolean onlyText) {
        String errorIn;
        if (warning) {
            errorIn = "Warning message:\n";
        } else {
            errorIn = "Error in ";
        }
        String delim = " :";

        StringBuilder sb = new StringBuilder();
        if (!onlyText) {
            sb.append("cat('");
        }
        sb.append(errorIn);
        sb.append(left.replaceAll("\\'", "\\\\\\'"));
        sb.append(delim);
        if (errorIn.length() + left.length() + delim.length() + 1 + right.length() >= 74) {
            sb.append("', '\n', '");
        }
        sb.append(' ');
        sb.append(right.replaceAll("\\'", "\\\\\\'"));
        sb.append("', '\n");
        if (!onlyText) {
            sb.append("')");
        }
        return sb.toString();
    }

    public static class TestNamesClass extends TestNamesClassSuper {
        public Object field;
        public static Object staticField;

        public void method() {
        }

        public static void staticMethod() {
        }
    }

    public static class TestNamesClassSuper {
        public Object superField;

        public void superMethod() {

        }
    }

    public static class TestNamesClassNoMembers {

    }

    public static class TestNamesClassNoPublicMembers {
        int i;
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
        public TestPOJO pojo = new TestPOJO("POJO for field");

        public static class Element {
            public final String data;

            public Element(String data) {
                this.data = data;
            }
        }

        public Element[] arrayObject = new Element[]{new Element("a"), new Element("b"), new Element("c"), null};

        public static class TestPOJO {
            public final String data;

            public TestPOJO(String data) {
                this.data = data;
            }

            public String getData() {
                return data;
            }

            @Override
            public String toString() {
                return String.format("TetsPOJO[data='%s']", data);
            }
        }

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

        public TestPOJO returnsPOJO() {
            return new TestPOJO("POJO for method");
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

        public void methodVoid() {
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
        public int[][] integerArray3x4 = {{1, 2, 3, 4}, {5, 6, 7, 8}, {9, 10, 11, 12}};
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
        public Boolean[] booleanObjectArrayWithNull = {true, null, true};

        public Byte[] byteObjectArray = {1, 2, 3};
        public Byte[][] byteObjectArray2 = {{1, 2, 3}, {1, 2, 3}};
        public Byte[][][] byteObjectArray3 = {{{1, 2, 3}, {1, 2, 3}}, {{1, 2, 3}, {1, 2, 3}}};
        public Byte[] byteObjectArrayWithNull = {1, null, 3};

        public Character[] charObjectArray = {'a', 'b', 'c'};
        public Character[][] charObjectArray2 = {{'a', 'b', 'c'}, {'a', 'b', 'c'}};
        public Character[][][] charObjectArray3 = {{{'a', 'b', 'c'}, {'a', 'b', 'c'}}, {{'a', 'b', 'c'}, {'a', 'b', 'c'}}};
        public Character[] charObjectArrayWithNull = {'a', null, 'c'};

        public Double[] doubleObjectArray = {1.1, 1.2, 1.3};
        public Double[][] doubleObjectArray2 = {{1.1, 1.2, 1.3}, {1.1, 1.2, 1.3}};
        public Double[][][] doubleObjectArray3 = {{{1.1, 1.2, 1.3}, {1.1, 1.2, 1.3}}, {{1.1, 1.2, 1.3}, {1.1, 1.2, 1.3}}};
        public Double[] doubleObjectArrayWithNull = {1.1, null, 1.3};

        public Float[] floatObjectArray = {1.1f, 1.2f, 1.3f};
        public Float[][] floatObjectArray2 = {{1.1f, 1.2f, 1.3f}, {1.1f, 1.2f, 1.3f}};
        public Float[][][] floatObjectArray3 = {{{1.1f, 1.2f, 1.3f}, {1.1f, 1.2f, 1.3f}}, {{1.1f, 1.2f, 1.3f}, {1.1f, 1.2f, 1.3f}}};
        public Float[] floatObjectArrayWithNull = {1.1f, null, 1.3f};

        public Integer[] integerObjectArray = {1, 2, 3};
        public Integer[][] integerObjectArray2 = {{1, 2, 3}, {1, 2, 3}};
        public Integer[][][] integerObjectArray3 = {{{1, 2, 3}, {1, 2, 3}}, {{1, 2, 3}, {1, 2, 3}}};
        public Integer[] integerObjectArrayWithNull = {1, null, 3};

        public Long[] longObjectArray = {1L, 2L, 3L};
        public Long[][] longObjectArray2 = {{1L, 2L, 3L}, {1L, 2L, 3L}};
        public Long[][][] longObjectArray3 = {{{1L, 2L, 3L}, {1L, 2L, 3L}}, {{1L, 2L, 3L}, {1L, 2L, 3L}}};
        public Long[] longObjectArrayWithNull = {1L, null, 3L};

        public Short[] shortObjectArray = {1, 2, 3};
        public Short[][] shortObjectArray2 = {{1, 2, 3}, {1, 2, 3}};
        public Short[][][] shortObjectArray3 = {{{1, 2, 3}, {1, 2, 3}}, {{1, 2, 3}, {1, 2, 3}}};
        public Short[] shortObjectArrayWithNull = {1, null, 3};

        public String[] stringArray = {"a", "b", "c"};
        public String[][] stringArray2 = {{"a", "b", "c"}, {"a", "b", "c"}};
        public String[][][] stringArray3 = {{{"a", "b", "c"}, {"a", "b", "c"}}, {{"a", "b", "c"}, {"a", "b", "c"}}};
        public String[] stringArrayWithNull = {"a", null, "c"};

        public Object[] onlyIntegerObjectArray = {1, 2, 3};
        public Object[] onlyLongObjectArray = {1L, 2L, 3L};
        public Object[] numericObjectArray = {1, 1L, 1.1};
        public Object[] mixedObjectArray = {1, "a", "1"};
        public Object[][] mixedObjectArray2 = {{"a", "b", "c"}, {1, 2, 3}};

        public Object[] objectArray = {new Object(), new Object(), new Object()};
        public Object[][] objectArray2 = {{new Object(), new Object(), new Object()}, {new Object(), new Object(), new Object()}};
        public Object[][][] objectArray3 = {{{new Object(), new Object(), new Object()}, {new Object(), new Object(), new Object()}},
                        {{new Object(), new Object(), new Object()}, {new Object(), new Object(), new Object()}}};

        public int[] getIntArray() {
            return integerArray;
        }

        public Integer[] getIntegerArray() {
            return integerObjectArray;
        }

        public String[] getStringArray() {
            return stringArray;
        }
    }

    public static class TestMultiDimArraysClass {
        public boolean[][] booleanArray2 = {{true, false, true}, {false, false, true}};
        public boolean[][][] booleanArray3 = {{{true, false, true}, {false, false, true}}, {{true, true, false}, {true, true, true}}};

        public byte[][] byteArray2 = {{1, 2, 3}, {4, 5, 6}};
        public byte[][][] byteArray3 = {{{1, 2, 3}, {4, 5, 6}}, {{7, 8, 9}, {10, 11, 12}}};

        public char[][] charArray2 = {{'a', 'b', 'c'}, {'d', 'e', 'f'}};
        public char[][][] charArray3 = {{{'a', 'b', 'c'}, {'d', 'e', 'f'}}, {{'g', 'h', 'i'}, {'j', 'k', 'l'}}};

        public double[][] doubleArray2 = {{1.1, 1.2, 1.3}, {1.4, 1.5, 1.6}};
        public double[][][] doubleArray3 = {{{1.1, 1.2, 1.3}, {1.4, 1.5, 1.6}}, {{1.7, 1.8, 1.9}, {1.11, 1.12, 1.13}}};

        public float[][] floatArray2 = {{1.1f, 1.2f, 1.3f}, {1.4f, 1.5f, 1.6f}};
        public float[][][] floatArray3 = {{{1.1f, 1.2f, 1.3f}, {1.4f, 1.5f, 1.6f}}, {{1.7f, 1.8f, 1.9f}, {1.11f, 1.12f, 1.13f}}};

        public int[][] integerArray2 = {{1, 2, 3}, {4, 5, 6}};
        public int[][][] integerArray3 = {{{1, 2, 3}, {4, 5, 6}}, {{7, 8, 9}, {10, 11, 12}}};

        public Object[][] integerArray2NotSquare = new Object[][]{{1, 2, 3}, {4, 5, 6, 7}};
        public Object[][] integerArray3NotSquare = new Object[][][]{{{1, 2, 3}, {4, 5, 6, 7}}, {{8, 9, 10}, {11, 12, 13, 14}}};
        public int[][][][] integerArray2x3x4x5 = new int[2][3][4][5];

        public long[][] longArray2 = {{1L, 2L, 3L}, {4L, 5L, 6L}};
        public long[][][] longArray3 = {{{1L, 2L, 3L}, {4L, 5L, 6L}}, {{7L, 8L, 9L}, {10L, 11L, 12L}}};

        public short[][] shortArray2 = {{1, 2, 3}, {4, 5, 6}};
        public short[][][] shortArray3 = {{{1, 2, 3}, {4, 5, 6}}, {{7, 8, 9}, {10, 11, 12}}};

        public String[][] stringArray2 = {{"a", "b", "c"}, {"d", "e", "f"}};
        public String[][][] stringArray3 = {{{"a", "d", "c"}, {"b", "o", "r"}}, {{"i", "n", "g"}, {"h", "i", "j"}}};

        public TestMultiDimArraysClass() {
            int i = 1;
            for (int x = 0; x < 2; x++) {
                for (int y = 0; y < 3; y++) {
                    for (int z = 0; z < 4; z++) {
                        for (int k = 0; k < 5; k++) {
                            integerArray2x3x4x5[x][y][z][k] = i++;
                        }
                    }
                }
            }
        }

    }

    public static class TestExceptionsClass {

        public TestExceptionsClass() {

        }

        public TestExceptionsClass(String className) throws Throwable {
            if (className != null) {
                throwEx(className);
            }
        }

        public TestExceptionsClass(String className, String msg) throws Throwable {
            if (className != null) {
                throwEx(className, msg);
            }
        }

        public static void exception(String className) throws Throwable {
            throwEx(className);
        }

        public static void exception(String className, String msg) throws Throwable {
            throwEx(className, msg);
        }

        private static void throwEx(String className) throws Throwable {
            throwEx(className, null);
        }

        private static void throwEx(String className, String msg) throws Throwable {
            Class<?> clazz = Class.forName(className);
            Object t;
            if (msg == null) {
                t = clazz.newInstance();
            } else {
                Constructor<?> ctor = clazz.getDeclaredConstructor(String.class);
                t = ctor.newInstance(msg);
            }
            assert t instanceof Throwable : "throwable instance expected: " + className;
            throw (Throwable) t;
        }
    }

    public static class TestAsListClass {
        public boolean[] b = {true, false, true};
        public int[] i = {1, 2, 3};

        public static int[] staticFieldsAreIgnoredByAsList;

        public void methodsAreIgnoredByAsList() {
        }

        public static void methodsAreIgnoredByAsList2() {
        }
    }

    public static class TestAsListClass2 {
        public boolean[][] b = {{true, false, true}, {true, false, true}};
        public int[][] i = {{1, 2, 3}, {1, 2, 3}};
    }

    public static class TestAsListClass3 {
        public boolean[][][] b = {{{true, false, true}, {true, false, true}}, {{true, false, true}, {true, false, true}}};
        public int[][][] i = {{{1, 2, 3}, {1, 2, 3}}, {{1, 2, 3}, {1, 2, 3}}};
    }

    public static class TestAsListClassMixed {
        public boolean[][] b = {{true, false, true}, {true, false, true}};
        public int[] i = {1, 2, 3};
        public Object[][] oa = {{true, false, true}, {'a', 'b', 'c'}};
        public Object o = null;
    }

    private static class TestProxyArray implements ProxyArray {
        @Override
        public Object get(long index) {
            return index > 3 ? 2 : 1;
        }

        @Override
        public void set(long index, Value value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long getSize() {
            return 3;
        }
    }

    private static class TestBooleanProxyArray implements ProxyArray {
        @Override
        public Object get(long index) {
            return index <= 3;
        }

        @Override
        public void set(long index, Value value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long getSize() {
            return 3;
        }
    }

    public static class TestDFProxyObject implements ProxyObject {
        private final TestProxyArray x = new TestProxyArray();
        private final TestBooleanProxyArray y = new TestBooleanProxyArray();

        @Override
        public Object getMember(String key) {
            return "x".equals(key) ? x : y;
        }

        @Override
        public Object getMemberKeys() {
            return new String[]{"x", "y"};
        }

        @Override
        public boolean hasMember(String key) {
            return key.equals("x") || key.equals("y");
        }

        @Override
        public void putMember(String key, Value value) {
            throw new UnsupportedOperationException("implement me!");
        }
    }

}
