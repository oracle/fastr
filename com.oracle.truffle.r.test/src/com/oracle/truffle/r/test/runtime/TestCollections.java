/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.test.runtime;

import com.oracle.truffle.r.runtime.Collections.ArrayListInt;
import com.oracle.truffle.r.runtime.Collections.ArrayListObj;
import com.oracle.truffle.r.runtime.ffi.AfterDownCallProfiles;
import com.oracle.truffle.r.test.TestBase;
import org.junit.Assert;
import org.junit.Test;

public class TestCollections extends TestBase {
    @Test
    public void testArrayListInt() {
        ArrayListInt list = new ArrayListInt(2);
        list.add(1);
        list.add(2);
        list.add(3);
        list.add(4);
        Assert.assertEquals(4, list.size());
        for (int i = 0; i < 4; i++) {
            Assert.assertEquals(i + 1, list.get(i));
        }
        list.add(5);
        Assert.assertEquals(5, list.size());
        for (int i = 0; i < 5; i++) {
            Assert.assertEquals(i + 1, list.get(i));
        }
        int[] arr = list.toArray();
        Assert.assertEquals(5, arr.length);
        for (int i = 0; i < 5; i++) {
            Assert.assertEquals(i + 1, arr[i]);
        }
    }

    @Test
    public void testArrayListObj() {
        ArrayListObj<Integer> list = new ArrayListObj<>(2);
        list.add(1);
        list.add(2);
        list.add(3);
        list.add(4);
        Assert.assertEquals(4, list.size());
        for (int i = 0; i < 4; i++) {
            Assert.assertEquals(Integer.valueOf(i + 1), list.get(i));
        }
        list.add(5);
        Assert.assertEquals(5, list.size());
        for (int i = 0; i < 5; i++) {
            Assert.assertEquals(Integer.valueOf(i + 1), list.get(i));
        }
        Object[] arr = list.toArray();
        Assert.assertEquals(5, arr.length);
        for (int i = 0; i < 5; i++) {
            Assert.assertEquals(i + 1, arr[i]);
        }
    }

    @Test
    public void testArrayListObjSetAndRemove() {
        ArrayListObj<Integer> list = new ArrayListObj<>(2);
        list.add(33);
        list.set(0, 1);
        Assert.assertEquals(Integer.valueOf(1), list.get(0));
        list.add(2);
        list.remove(0);
        Assert.assertEquals(1, list.size());
        Assert.assertEquals(Integer.valueOf(2), list.get(0));
    }

    @Test
    public void testArrayListObjPop() {
        ArrayListObj<Integer> list = new ArrayListObj<>(2);

        list.add(33);
        list.add(44);
        list.add(55);
        list.pop();
        Assert.assertEquals(2, list.size());
        Assert.assertEquals(Integer.valueOf(33), list.get(0));
        Assert.assertEquals(Integer.valueOf(44), list.get(1));

        list.add(66);
        Assert.assertEquals(3, list.size());
        Assert.assertEquals(Integer.valueOf(33), list.get(0));
        Assert.assertEquals(Integer.valueOf(44), list.get(1));
        Assert.assertEquals(Integer.valueOf(66), list.get(2));

        list.pop();
        list.pop();
        Assert.assertEquals(1, list.size());
        Assert.assertEquals(Integer.valueOf(33), list.get(0));
    }

    @Test
    public void testArrayListObjClear() {
        ArrayListObj<Integer> list = new ArrayListObj<>(2);
        list.add(33);
        list.add(44);
        list.add(55);
        list.clear(AfterDownCallProfiles.getUncached());
        Assert.assertEquals(0, list.size());
    }

    @Test(expected = IllegalStateException.class)
    public void testArrayListObjPopEmpty() {
        ArrayListObj<Integer> list = new ArrayListObj<>(2);
        list.add(33);
        list.pop();
        list.pop();
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testArrayListObjIndexOutOfBounds() {
        ArrayListObj<Integer> list = new ArrayListObj<>(2);
        list.add(33);
        list.get(1);
    }
}
