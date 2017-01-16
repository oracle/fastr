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
package com.oracle.truffle.r.test.runtime.data;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.Iterator;

import org.junit.Test;

import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPairList;

public class RPairListTests {
    @Test
    public void testIterator() {
        Iterator<RPairList> iterator = RDataFactory.createPairList(1, RDataFactory.createPairList(2, RNull.instance)).iterator();
        assertEquals(true, iterator.hasNext());
        assertEquals(1, iterator.next().car());
        assertEquals(true, iterator.hasNext());
        assertEquals(2, iterator.next().car());
        assertEquals(false, iterator.hasNext());
    }

    @Test
    public void testToList() {
        RPairList pairList = RDataFactory.createPairList(1, RDataFactory.createPairList(2, RNull.instance, "name2"), "name1");
        RList result = pairList.toRList();
        assertArrayEquals(new String[]{"name1", "name2"}, result.getNames().getDataWithoutCopying());
        assertArrayEquals(new Object[]{1, 2}, result.getDataWithoutCopying());
    }
}
