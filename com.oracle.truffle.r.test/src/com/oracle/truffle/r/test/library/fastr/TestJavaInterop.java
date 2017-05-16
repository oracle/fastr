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

import com.oracle.truffle.r.runtime.RType;
import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class TestJavaInterop extends TestBase {

    private static final String TEST_CLASS = TestClass.class.getName();

    @Test
    public void testToByte() {
        assertEvalFastR("v <- .fastr.interop.toByte(1L); v;", "1");
        assertEvalFastR("v <- .fastr.interop.toByte(1.1); v;", "1");
        assertEvalFastR("v <- .fastr.interop.toByte(as.raw(1)); v;", "1");
        assertEvalFastR("v <- .fastr.interop.toByte(1.1); class(v);", "'" + RType.RInteropByte.getName() + "'");
        assertEvalFastR("v <- .fastr.interop.toByte(1.1); typeof(v);", "'" + RType.RInteropByte.getName() + "'");
        assertEvalFastR("v <- .fastr.interop.toByte(" + Byte.MAX_VALUE + "); v;", "" + Byte.MAX_VALUE);
        assertEvalFastR("v <- .fastr.interop.toByte(" + Byte.MIN_VALUE + "); v;", "" + Byte.MIN_VALUE);
        assertEvalFastR("v <- .fastr.interop.toByte(" + Integer.MAX_VALUE + "); v;", "" + new Integer(Integer.MAX_VALUE).byteValue());
        assertEvalFastR("v <- .fastr.interop.toByte(" + Integer.MIN_VALUE + "); v;", "" + new Integer(Integer.MIN_VALUE).byteValue());
        assertEvalFastR("v <- .fastr.interop.toByte(" + Double.MAX_VALUE + "); v;", "" + new Double(Double.MAX_VALUE).byteValue());
        assertEvalFastR("v <- .fastr.interop.toByte(" + Double.MIN_VALUE + "); v;", "" + new Double(Double.MIN_VALUE).byteValue());
    }

    @Test
    public void testToFloat() {
        assertEvalFastR("v <- .fastr.interop.toFloat(1L); v;", "1");
        assertEvalFastR("v <- .fastr.interop.toFloat(1.1); v;", "1.1");
        assertEvalFastR("v <- .fastr.interop.toFloat(as.raw(1)); v;", "1");
        assertEvalFastR("v <- .fastr.interop.toFloat(1.1); class(v);", "'" + RType.RInteropFloat.getName() + "'");
        assertEvalFastR("v <- .fastr.interop.toFloat(1.1); typeof(v);", "'" + RType.RInteropFloat.getName() + "'");
        assertEvalFastR("v <- .fastr.interop.toFloat(1L); class(v);", "'" + RType.RInteropFloat.getName() + "'");
        assertEvalFastR("v <- .fastr.interop.toFloat(1L); typeof(v);", "'" + RType.RInteropFloat.getName() + "'");
        assertEvalFastR("v <- .fastr.interop.toFloat(" + Float.MAX_VALUE + "); v;", "" + Float.MAX_VALUE);
        assertEvalFastR("v <- .fastr.interop.toFloat(" + Float.MIN_VALUE + "); v;", "" + (double) Float.MIN_VALUE);
        assertEvalFastR("v <- .fastr.interop.toFloat(" + Double.MAX_VALUE + "); v;", "Inf");
        assertEvalFastR("v <- .fastr.interop.toFloat(" + Double.MIN_VALUE + "); v;", "" + new Double(Double.MIN_VALUE).floatValue());
    }

    @Test
    public void testToLong() {
        assertEvalFastR("v <- .fastr.interop.toLong(1L); v;", "1");
        assertEvalFastR("v <- .fastr.interop.toLong(1.1); v;", "1");
        assertEvalFastR("v <- .fastr.interop.toLong(as.raw(1)); v;", "1");
        assertEvalFastR("v <- .fastr.interop.toLong(1.1); class(v);", "'" + RType.RInteropLong.getName() + "'");
        assertEvalFastR("v <- .fastr.interop.toLong(1.1); typeof(v);", "'" + RType.RInteropLong.getName() + "'");
        assertEvalFastR("v <- .fastr.interop.toLong(1L); class(v);", "'" + RType.RInteropLong.getName() + "'");
        assertEvalFastR("v <- .fastr.interop.toLong(1L); typeof(v);", "'" + RType.RInteropLong.getName() + "'");
        assertEvalFastR("v <- .fastr.interop.toLong(" + Integer.MAX_VALUE + "); v;", "" + Integer.MAX_VALUE);
        assertEvalFastR("v <- .fastr.interop.toLong(" + Integer.MIN_VALUE + "); v;", "" + Integer.MIN_VALUE);
        assertEvalFastR("v <- .fastr.interop.toLong(" + Double.MAX_VALUE + "); v;", "" + new Double(Double.MAX_VALUE).longValue());
        assertEvalFastR("v <- .fastr.interop.toLong(" + Double.MIN_VALUE + "); v;", "" + new Double(Double.MIN_VALUE).longValue());
    }

    @Test
    public void testToShort() {
        assertEvalFastR("v <- .fastr.interop.toShort(1L); v;", "1");
        assertEvalFastR("v <- .fastr.interop.toShort(1.1); v;", "1");
        assertEvalFastR("v <- .fastr.interop.toShort(as.raw(1)); v;", "1");
        assertEvalFastR("v <- .fastr.interop.toShort(1.1); class(v);", "'" + RType.RInteropShort.getName() + "'");
        assertEvalFastR("v <- .fastr.interop.toShort(1.1); typeof(v);", "'" + RType.RInteropShort.getName() + "'");
        assertEvalFastR("v <- .fastr.interop.toShort(1L); class(v);", "'" + RType.RInteropShort.getName() + "'");
        assertEvalFastR("v <- .fastr.interop.toShort(1L); typeof(v);", "'" + RType.RInteropShort.getName() + "'");
        assertEvalFastR("v <- .fastr.interop.toShort(" + Short.MAX_VALUE + "); v;", "" + Short.MAX_VALUE);
        assertEvalFastR("v <- .fastr.interop.toShort(" + Short.MIN_VALUE + "); v;", "" + Short.MIN_VALUE);
        assertEvalFastR("v <- .fastr.interop.toShort(" + Integer.MAX_VALUE + "); v;", "" + new Integer(Integer.MAX_VALUE).shortValue());
        assertEvalFastR("v <- .fastr.interop.toShort(" + Integer.MIN_VALUE + "); v;", "" + new Integer(Integer.MIN_VALUE).shortValue());
        assertEvalFastR("v <- .fastr.interop.toShort(" + Double.MAX_VALUE + "); v;", "" + new Double(Double.MAX_VALUE).shortValue());
        assertEvalFastR("v <- .fastr.interop.toShort(" + Double.MIN_VALUE + "); v;", "" + new Double(Double.MIN_VALUE).shortValue());
    }

    @Test
    public void testToChar() {
        assertEvalFastR("v <- .fastr.interop.toChar(97L); v;", "'a'");
        assertEvalFastR("v <- .fastr.interop.toChar(97.1); v;", "'a'");
        assertEvalFastR("v <- .fastr.interop.toChar(97.1, 1); v;", "cat('Error in .fastr.interop.toChar(97.1, 1) : ', '\n', ' pos argument not allowed with a numeric value', '\n')");
        assertEvalFastR("v <- .fastr.interop.toChar(97L, 1); v;", "cat('Error in .fastr.interop.toChar(97L, 1) : ','\n',' pos argument not allowed with a numeric value', '\n')");
        assertEvalFastR("v <- .fastr.interop.toChar('abc', 1); v;", "'b'");
        assertEvalFastR("v <- .fastr.interop.toChar('abc', 1.1); v;", "'b'");
        assertEvalFastR("v <- .fastr.interop.toChar(97.1); class(v);", "'" + RType.RInteropChar.getName() + "'");
        assertEvalFastR("v <- .fastr.interop.toChar(97.1); typeof(v);", "'" + RType.RInteropChar.getName() + "'");
        assertEvalFastR("v <- .fastr.interop.toChar(97L); class(v);", "'" + RType.RInteropChar.getName() + "'");
        assertEvalFastR("v <- .fastr.interop.toChar(97L); typeof(v);", "'" + RType.RInteropChar.getName() + "'");
        assertEvalFastR("v <- .fastr.interop.toChar('a'); v;", "'a'");
    }

    @Test
    public void testToArray() {
        assertEvalFastR("a <- .fastr.java.toArray(1L); a;", getRValue(new int[]{1}));
        assertEvalFastR("a <- .fastr.java.toArray(c(1L, 2L)); a;", getRValue(new int[]{1, 2}));
        assertEvalFastR("a <- .fastr.java.toArray(1L,,T); a;", getRValue(new int[]{1}));
        assertEvalFastR("a <- .fastr.java.toArray(c(1L, 2L),,T); a;", getRValue(new int[]{1, 2}));

        assertEvalFastR("a <- .fastr.java.toArray(1.1); a;", getRValue(new double[]{1.1}));
        assertEvalFastR("a <- .fastr.java.toArray(c(1.1, 1.2)); a;", getRValue(new double[]{1.1, 1.2}));
        assertEvalFastR("a <- .fastr.java.toArray(1.1,,T); a;", getRValue(new double[]{1.1}));
        assertEvalFastR("a <- .fastr.java.toArray(c(1.1, 1.2),,T); a;", getRValue(new double[]{1.1, 1.2}));

        assertEvalFastR("a <- .fastr.java.toArray(T); a;", getRValue(new boolean[]{true}));
        assertEvalFastR("a <- .fastr.java.toArray(c(T, F)); a;", getRValue(new boolean[]{true, false}));
        assertEvalFastR("a <- .fastr.java.toArray(T,,T); a;", getRValue(new boolean[]{true}));
        assertEvalFastR("a <- .fastr.java.toArray(c(T, F),,T); a;", getRValue(new boolean[]{true, false}));

        assertEvalFastR("a <- .fastr.java.toArray('a'); a;", getRValue(new String[]{"a"}));
        assertEvalFastR("a <- .fastr.java.toArray(c('a', 'b')); a;", getRValue(new String[]{"a", "b"}));
        assertEvalFastR("a <- .fastr.java.toArray('a',,T); a;", getRValue(new String[]{"a"}));
        assertEvalFastR("a <- .fastr.java.toArray(c('a', 'b'),,T); a;", getRValue(new String[]{"a", "b"}));

        assertEvalFastR("a <- .fastr.java.toArray(.fastr.interop.toShort(1)); a;", getRValue(new short[]{1}));
        assertEvalFastR("a <- .fastr.java.toArray(c(.fastr.interop.toShort(1), .fastr.interop.toShort(2))); a;", getRValue(new short[]{1, 2}));
        assertEvalFastR("a <- .fastr.java.toArray(.fastr.interop.toShort(1),,T); a;", getRValue(new short[]{1}));
        assertEvalFastR("a <- .fastr.java.toArray(c(.fastr.interop.toShort(1), .fastr.interop.toShort(2)),,T); a;", getRValue(new short[]{1, 2}));

        assertEvalFastR("a <- .fastr.java.toArray(.fastr.interop.toShort(1), 'java.lang.Short'); a;", getRValue(new short[]{1}));
        assertEvalFastR("a <- .fastr.java.toArray(c(.fastr.interop.toShort(1), .fastr.interop.toShort(2)), 'java.lang.Short'); a;", getRValue(new short[]{1, 2}));
        assertEvalFastR("a <- .fastr.java.toArray(.fastr.interop.toShort(1), 'java.lang.Short', T); a;", getRValue(new short[]{1}));
        assertEvalFastR("a <- .fastr.java.toArray(c(.fastr.interop.toShort(1), .fastr.interop.toShort(2)), 'java.lang.Short', T); a;", getRValue(new short[]{1, 2}));

        assertEvalFastR("a <- .fastr.java.toArray(c(.fastr.interop.toShort(1), .fastr.interop.toShort(2)), 'int'); a;", getRValue(new int[]{1, 2}));
        assertEvalFastR("a <- .fastr.java.toArray(c(1.123, 2.123), 'double'); a;", getRValue(new double[]{1.123, 2.123}));
        assertEvalFastR("a <- .fastr.java.toArray(.fastr.interop.toShort(1), 'double'); a;", getRValue(new double[]{1}));

        assertEvalFastR("a <- .fastr.java.toArray(1L); .fastr.java.toArray(a);", getRValue(new int[]{1}));
        assertEvalFastR("a <- .fastr.java.toArray(1L); .fastr.java.toArray(a,,T);", getRValue(new int[]{1}));

        assertEvalFastR("a <- .fastr.java.toArray(.fastr.interop.toShort(1)); .fastr.java.toArray(a);", getRValue(new short[]{1}));

        assertEvalFastR("tc <- .fastr.java.class('" + TEST_CLASS + "'); to <- .fastr.interop.new(tc); a <- .fastr.java.toArray(to); .fastr.java.isArray(a)", "TRUE");
        assertEvalFastR("tc <- .fastr.java.class('" + TEST_CLASS + "'); to <- .fastr.interop.new(tc); a <- .fastr.java.toArray(c(to, to)); .fastr.java.isArray(a)", "TRUE");
        
        assertEvalFastR(Ignored.Unimplemented, "a <- .fastr.java.toArray(1L,,F); a;", getRValue(new int[]{1}));
    }

    @Test
    public void testFromArray() {
        testFromArray("fieldStaticBooleanArray", "logical");
        testFromArray("fieldStaticByteArray", "integer");
        testFromArray("fieldStaticCharArray", "character");
        testFromArray("fieldStaticDoubleArray", "double");
        testFromArray("fieldStaticFloatArray", "double");
        testFromArray("fieldStaticIntArray", "integer");
        testFromArray("fieldStaticLongArray", "double");
        testFromArray("fieldStaticShortArray", "integer");
        testFromArray("fieldStaticStringArray", "character");

        assertEvalFastR("t <- .fastr.interop.new(.fastr.java.class('" + TEST_CLASS + "')); v <- .fastr.java.fromArray(t$objectArray); is.list(v)", "TRUE");
        testFromArray("objectIntArray", "integer");
        testFromArray("objectDoubleArray", "double");
        assertEvalFastR("t <- .fastr.interop.new(.fastr.java.class('" + TEST_CLASS + "')); v <- .fastr.java.fromArray(t$mixedTypesArray); is.list(v)", "TRUE");

        assertEvalFastR("t <- .fastr.interop.new(.fastr.java.class('" + TEST_CLASS + "')); v <- .fastr.java.fromArray(t$hasNullIntArray); is.list(v)", "TRUE");
        assertEvalFastR("t <- .fastr.interop.new(.fastr.java.class('" + TEST_CLASS + "')); v <- .fastr.java.fromArray(t$hasNullIntArray); v[1]", "list(1)");
        assertEvalFastR("t <- .fastr.interop.new(.fastr.java.class('" + TEST_CLASS + "')); v <- .fastr.java.fromArray(t$hasNullIntArray); v[2]", "list(NULL)");
        assertEvalFastR("t <- .fastr.interop.new(.fastr.java.class('" + TEST_CLASS + "')); v <- .fastr.java.fromArray(t$hasNullIntArray); v[3]", "list(3)");
    }

    public void testFromArray(String field, String type) {
        assertEvalFastR("t <- .fastr.interop.new(.fastr.java.class('" + TEST_CLASS + "')); v <- .fastr.java.fromArray(t$" + field + "); is.vector(v)", "TRUE");
        assertEvalFastR("t <- .fastr.interop.new(.fastr.java.class('" + TEST_CLASS + "')); v <- .fastr.java.fromArray(t$" + field + "); typeof(v)", getRValue(type));
    }

    @Test
    public void testNew() {
        assertEvalFastR("tc <- .fastr.java.class('" + Boolean.class.getName() + "'); t <- .fastr.interop.new(tc, TRUE); t", "TRUE");
        assertEvalFastR("tc <- .fastr.java.class('" + Byte.class.getName() + "'); t <- .fastr.interop.new(tc, .fastr.interop.toByte(1)); t", "1");
        assertEvalFastR("tc <- .fastr.java.class('" + Character.class.getName() + "'); t <- .fastr.interop.new(tc, .fastr.interop.toChar(97)); t", "'a'");
        assertEvalFastR("tc <- .fastr.java.class('" + Double.class.getName() + "'); t <- .fastr.interop.new(tc, 1.1); t", "1.1");
        assertEvalFastR("tc <- .fastr.java.class('" + Float.class.getName() + "'); t <- .fastr.interop.new(tc, .fastr.interop.toFloat(1.1)); t", "1.1");
        assertEvalFastR("tc <- .fastr.java.class('" + Integer.class.getName() + "'); t <- .fastr.interop.new(tc, 1L); t", "1");
        assertEvalFastR("tc <- .fastr.java.class('" + Long.class.getName() + "'); t <- .fastr.interop.new(tc, .fastr.interop.toLong(1)); t", "1");
        assertEvalFastR("tc <- .fastr.java.class('" + Short.class.getName() + "'); t <- .fastr.interop.new(tc, .fastr.interop.toShort(1)); t", "1");
        assertEvalFastR("tc <- .fastr.java.class('" + String.class.getName() + "'); t <- .fastr.interop.new(tc, 'abc'); t", "'abc'");
        assertEvalFastR("tc <- .fastr.java.class('" + TestNullClass.class.getName() + "'); t <- .fastr.interop.new(tc, NULL); class(t)", "'" + RType.TruffleObject.getName() + "'");
    }

    @Test
    public void testCombineInteropTypes() {
        assertEvalFastR("class(c(.fastr.interop.toByte(123)))", "'interopt.byte'");
        assertEvalFastR("class(c(.fastr.interop.toByte(123), .fastr.interop.toByte(234)))", "'list'");
        assertEvalFastR("class(c(.fastr.interop.toByte(123), 1))", "'list'");
        assertEvalFastR("class(c(1, .fastr.interop.toByte(123)))", "'list'");

        assertEvalFastR("tc <- .fastr.java.class('" + TEST_CLASS + "'); t <- .fastr.interop.new(tc); class(c(t))", "'truffle.object'");
        assertEvalFastR("tc <- .fastr.java.class('" + TEST_CLASS + "'); t <- .fastr.interop.new(tc); t1 <- .fastr.interop.new(tc); class(c(t, t1))", "'list'");
        assertEvalFastR("tc <- .fastr.java.class('" + TEST_CLASS + "'); t <- .fastr.interop.new(tc); class(c(1, t))", "'list'");
        assertEvalFastR("tc <- .fastr.java.class('" + TEST_CLASS + "'); t <- .fastr.interop.new(tc); class(c(t, 1))", "'list'");
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
    public void testMethods() throws IllegalAccessException, ClassNotFoundException, IllegalArgumentException, InvocationTargetException {
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
        assertEvalFastR("tc <- .fastr.java.class('" + TEST_CLASS + "'); t <- .fastr.interop.new(tc); t$" + member, getRValue(value));
    }

    @Test
    public void testAllTypes() {
        getValueForAllTypesMethod("allTypesMethod");
        getValueForAllTypesMethod("allTypesStaticMethod");
    }

    @Test
    public void testNonPrimitiveParameter() {
        assertEvalFastR("tc <- .fastr.java.class('" + TEST_CLASS + "'); t <- .fastr.interop.new(tc); t$equals(t)", "TRUE");
    }
    
    @Test
    public void testClassAsParameter() {
        // needs to be implemented in truffle
        assertEvalFastR(Ignored.Unimplemented, "tc <- .fastr.java.class('" + TEST_CLASS + "'); t <- .fastr.interop.new(tc); t$classAsArg(tc)", getRValue(TEST_CLASS));
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
        assertEvalFastR("tc <- .fastr.java.class('" + TEST_CLASS + "'); t <- .fastr.interop.new(tc); t$" + method + "(" + getRValuesAsString(bo, bt, c, sh, i, l, d, f, s) + ")",
                        getRValue("" + bo + bt + c + sh + i + l + d + f + s));
    }

    @Test
    public void testNullParameter() {
        assertEvalFastR(Ignored.Unimplemented, "tc <- .fastr.java.class('" + TEST_CLASS + "'); t <- .fastr.interop.new(tc); t$isNull('string')", "java.lang.String");
        assertEvalFastR(Ignored.Unimplemented, "tc <- .fastr.java.class('" + TEST_CLASS + "'); t <- .fastr.interop.new(tc); t$isNull(1)", "java.lang.Long");
    }

    @Test
    public void testOverloaded() {
        assertEvalFastR(Ignored.Unimplemented, "tc <- .fastr.java.class('" + TEST_CLASS + "'); t <- .fastr.interop.new(tc); t$isOverloaded(TRUE)", "boolean");
        assertEvalFastR(Ignored.Unimplemented, "tc <- .fastr.java.class('" + TEST_CLASS + "'); t <- .fastr.interop.new(tc); t$isOverloaded('string')", String.class.getName());
        // TODO add remaining isOverloaded(...) calls once this is fixed
    }

    @Test
    public void testArrayAccess() {
        assertEvalFastR("tc <- .fastr.java.class('" + TEST_CLASS + "'); t <- .fastr.interop.new(tc); a <- t$fieldIntArray; a[1]", "1");
        assertEvalFastR("tc <- .fastr.java.class('" + TEST_CLASS + "'); t <- .fastr.interop.new(tc); a <- t$fieldIntArray; a[[1]]", "1");
        assertEvalFastR("tc <- .fastr.java.class('" + TEST_CLASS + "'); t <- .fastr.interop.new(tc); a <- t$int2DimArray; a[1]", getRValue(new int[]{1, 2, 3}));
        assertEvalFastR("tc <- .fastr.java.class('" + TEST_CLASS + "'); t <- .fastr.interop.new(tc); a <- t$int2DimArray; a[[1]]", getRValue(new int[]{1, 2, 3}));
        assertEvalFastR("tc <- .fastr.java.class('" + TEST_CLASS + "'); t <- .fastr.interop.new(tc); a <- t$int2DimArray; a[1,2]", "2");
        assertEvalFastR("tc <- .fastr.java.class('" + TEST_CLASS + "'); t <- .fastr.interop.new(tc); a <- t$int2DimArray; a[[1,2]]", "2");

        assertEvalFastR(Ignored.Unimplemented, "tc <- .fastr.java.class('" + TEST_CLASS + "'); t <- .fastr.interop.new(tc); a <- t$fieldIntArray; a[1]<-123; a[1]", "123");
        assertEvalFastR(Ignored.Unimplemented, "tc <- .fastr.java.class('" + TEST_CLASS + "'); t <- .fastr.interop.new(tc); a <- t$fieldIntArray; a[[1]]<-123; a[[1]]", "123");
        assertEvalFastR(Ignored.Unimplemented, "tc <- .fastr.java.class('" + TEST_CLASS + "'); t <- .fastr.interop.new(tc); a <- t$fieldIntArray; a[1,2]<-1234; a[1,2]", "1234");
        assertEvalFastR(Ignored.Unimplemented, "tc <- .fastr.java.class('" + TEST_CLASS + "'); t <- .fastr.interop.new(tc); a <- t$fieldIntArray; a[[1,2]]<-1234; a[[1,2]]", "1234");
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
            sb.append("cat('[1] ");
            int lenght = Array.getLength(value);
            for (int i = 0; i < lenght; i++) {
                if (lenght > 1 && value.getClass().getComponentType() == Boolean.TYPE && (boolean) Array.get(value, i)) {
                    // what the heck?
                    sb.append(" ");
                }
                sb.append(getRValue(Array.get(value, i)));
                if (i < lenght - 1) {
                    sb.append(" ");
                }
            }
            sb.append("\\nattr(,\"is.truffle.object\")\\n[1] TRUE\\n')");
            return sb.toString();
        }
        return value.toString();
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

    public static class TestNullClass {
        public TestNullClass(Object o) {
            assert o == null;
        }
    }

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

        public boolean fieldBoolean = fieldStaticBoolean;
        public byte fieldByte = fieldStaticByte;
        public char fieldChar = fieldStaticChar;
        public short fieldShort = fieldStaticShort;
        public int fieldInteger = fieldStaticInteger;
        public long fieldLong = fieldStaticLong;
        public double fieldDouble = fieldStaticDouble;
        public float fieldFloat = fieldStaticFloat;

        public Boolean fieldBooleanObject = fieldBoolean;
        public Byte fieldByteObject = fieldByte;
        public Character fieldCharObject = fieldChar;
        public Short fieldShortObject = fieldShort;
        public Integer fieldIntegerObject = fieldInteger;
        public Long fieldLongObject = fieldLong;
        public Double fieldDoubleObject = fieldDouble;
        public Float fieldFloatObject = fieldFloat;
        public String fieldStringObject = fieldStaticStringObject;

        public static Double fieldStaticNaNObject = Double.NaN;
        public static double fieldStaticNaN = Double.NaN;

        public static boolean[] fieldStaticBooleanArray = new boolean[]{true, false, true};
        public static byte[] fieldStaticByteArray = new byte[]{1, 2, 3};
        public static char[] fieldStaticCharArray = new char[]{'a', 'b', 'c'};
        public static double[] fieldStaticDoubleArray = new double[]{1.1, 2.1, 3.1};
        public static float[] fieldStaticFloatArray = new float[]{1.1f, 2.1f, 3.1f};
        public static int[] fieldStaticIntArray = new int[]{1, 2, 3};
        public static long[] fieldStaticLongArray = new long[]{1, 2, 3};
        public static short[] fieldStaticShortArray = new short[]{1, 2, 3};
        public static String[] fieldStaticStringArray = new String[]{"a", "b", "c"};

        public boolean[] fieldBooleanArray = fieldStaticBooleanArray;
        public byte[] fieldByteArray = fieldStaticByteArray;
        public char[] fieldCharArray = fieldStaticCharArray;
        public double[] fieldDoubleArray = fieldStaticDoubleArray;
        public float[] fieldFloatArray = fieldStaticFloatArray;
        public int[] fieldIntArray = fieldStaticIntArray;
        public long[] fieldLongArray = fieldStaticLongArray;
        public short[] fieldShortArray = fieldStaticShortArray;
        public String[] fieldStringArray = fieldStaticStringArray;

        public int[][] int2DimArray = new int[][]{new int[]{1, 2, 3}, new int[]{4, 5, 5}};
        public Object[] objectArray = new Object[]{new Object(), new Object(), new Object()};
        public Object[] objectIntArray = new Object[]{1, 2, 3};
        public Object[] objectDoubleArray = new Object[]{1.1, 2.1, 3.1};
        public Object[] mixedTypesArray = new Object[]{1, 2.1, 'a'};
        public Integer[] hasNullIntArray = new Integer[]{1, null, 3};

        public static Object fieldStaticNullObject = null;
        public Object fieldNullObject = null;

        public TestClass() {
            this(true, Byte.MAX_VALUE, 'a', Double.MAX_VALUE, Float.MAX_VALUE, Integer.MAX_VALUE, Long.MAX_VALUE, Short.MAX_VALUE, "a string");
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
            return fieldStaticIntArray;
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
            return fieldIntArray;
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
    }

    public static class TestArrayClass {
        public static TestArrayClass[] testArray = new TestArrayClass[]{new TestArrayClass(), new TestArrayClass(), new TestArrayClass()};

    }

}
