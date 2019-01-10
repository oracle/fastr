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

import org.junit.Assert;
import org.junit.Test;

import com.oracle.truffle.r.runtime.Collections.ArrayListInt;
import com.oracle.truffle.r.test.TestBase;

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
}
