/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.test.runtime;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import com.oracle.truffle.r.runtime.RSerialize;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.test.TestBase;

public class TestRSerialize extends TestBase {

    // Buffer enlargement tests

    @Test
    public void testDeserializeLongString() {
        char[] chars = new char[1000000];
        Arrays.fill(chars, 'x');
        String longString = new String(chars);
        RStringVector longStringVec = RDataFactory.createStringVector(longString);
        byte[] serialized = RSerialize.serialize(longStringVec, RSerialize.XDR, RSerialize.DEFAULT_VERSION, null);
        Object unserialized = RSerialize.unserialize(RDataFactory.createRawVector(serialized));

        Assert.assertTrue(unserialized instanceof RStringVector);
        Assert.assertEquals(1, ((RStringVector) unserialized).getLength());
        Assert.assertEquals(longString, ((RStringVector) unserialized).getDataAt(0));
    }

    @Test
    public void testDeserializeShortLongStrings() {
        char[] chars = new char[1000000];
        Arrays.fill(chars, 'x');
        String longString = new String(chars);
        RStringVector longStringVec = RDataFactory.createStringVector(new String[]{"abc", longString}, true);
        byte[] serialized = RSerialize.serialize(longStringVec, RSerialize.XDR, RSerialize.DEFAULT_VERSION, null);
        Object unserialized = RSerialize.unserialize(RDataFactory.createRawVector(serialized));

        Assert.assertTrue(unserialized instanceof RStringVector);
        Assert.assertEquals(2, ((RStringVector) unserialized).getLength());
        Assert.assertEquals("abc", ((RStringVector) unserialized).getDataAt(0));
        Assert.assertEquals(longString, ((RStringVector) unserialized).getDataAt(1));
    }

    @Test
    public void testDeserializeLongShortStrings() {
        char[] chars = new char[1000000];
        Arrays.fill(chars, 'x');
        String longString = new String(chars);
        RStringVector longStringVec = RDataFactory.createStringVector(new String[]{longString, "abc"}, true);
        byte[] serialized = RSerialize.serialize(longStringVec, RSerialize.XDR, RSerialize.DEFAULT_VERSION, null);
        Object unserialized = RSerialize.unserialize(RDataFactory.createRawVector(serialized));

        Assert.assertTrue(unserialized instanceof RStringVector);
        Assert.assertEquals(2, ((RStringVector) unserialized).getLength());
        Assert.assertEquals(longString, ((RStringVector) unserialized).getDataAt(0));
        Assert.assertEquals("abc", ((RStringVector) unserialized).getDataAt(1));
    }

    @Test
    public void testDeserializeShortLongShortStrings() {
        char[] chars = new char[1000000];
        Arrays.fill(chars, 'x');
        String longString = new String(chars);
        RStringVector longStringVec = RDataFactory.createStringVector(new String[]{"abc", longString, "abc"}, true);
        byte[] serialized = RSerialize.serialize(longStringVec, RSerialize.XDR, RSerialize.DEFAULT_VERSION, null);
        Object unserialized = RSerialize.unserialize(RDataFactory.createRawVector(serialized));

        Assert.assertTrue(unserialized instanceof RStringVector);
        Assert.assertEquals(3, ((RStringVector) unserialized).getLength());
        Assert.assertEquals("abc", ((RStringVector) unserialized).getDataAt(0));
        Assert.assertEquals(longString, ((RStringVector) unserialized).getDataAt(1));
        Assert.assertEquals("abc", ((RStringVector) unserialized).getDataAt(2));
    }

    @Test
    public void testDeserializeShortLongShortLongStrings() {
        char[] chars = new char[1000000];
        Arrays.fill(chars, 'x');
        String longString = new String(chars);
        RStringVector longStringVec = RDataFactory.createStringVector(new String[]{"abc", longString, "abc", longString}, true);
        byte[] serialized = RSerialize.serialize(longStringVec, RSerialize.XDR, RSerialize.DEFAULT_VERSION, null);
        Object unserialized = RSerialize.unserialize(RDataFactory.createRawVector(serialized));

        Assert.assertTrue(unserialized instanceof RStringVector);
        Assert.assertEquals(4, ((RStringVector) unserialized).getLength());
        Assert.assertEquals("abc", ((RStringVector) unserialized).getDataAt(0));
        Assert.assertEquals(longString, ((RStringVector) unserialized).getDataAt(1));
        Assert.assertEquals("abc", ((RStringVector) unserialized).getDataAt(2));
        Assert.assertEquals(longString, ((RStringVector) unserialized).getDataAt(3));
    }
}
