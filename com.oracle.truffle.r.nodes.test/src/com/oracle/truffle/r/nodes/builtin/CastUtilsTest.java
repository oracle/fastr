/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.oracle.truffle.r.nodes.casts.CastUtils;
import com.oracle.truffle.r.nodes.casts.Not;
import com.oracle.truffle.r.nodes.casts.TypeExpr;
import com.oracle.truffle.r.runtime.data.RDouble;
import com.oracle.truffle.r.runtime.data.RIntSequence;
import com.oracle.truffle.r.runtime.data.RInteger;
import com.oracle.truffle.r.runtime.data.RSequence;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

public class CastUtilsTest {

    @Test
    public void testArgumentPowerSet() {
        List<TypeExpr> argTypeSets = new LinkedList<>();
        argTypeSets.add(TypeExpr.union(Integer.class, RInteger.class));
        argTypeSets.add(TypeExpr.union(Double.class, RDouble.class));

        Set<List<Type>> powerSet = CastUtils.argumentProductSet(argTypeSets);
        Assert.assertEquals(4, powerSet.size());
        Assert.assertTrue(powerSet.contains(Arrays.asList(Integer.class, Double.class)));
        Assert.assertTrue(powerSet.contains(Arrays.asList(RInteger.class, Double.class)));
        Assert.assertTrue(powerSet.contains(Arrays.asList(Integer.class, RDouble.class)));
        Assert.assertTrue(powerSet.contains(Arrays.asList(RInteger.class, RDouble.class)));
    }

    @Test
    public void testIsFullyConvertible() {
        Assert.assertEquals(CastUtils.Cast.Coverage.full, CastUtils.Casts.isConvertible(Integer.class, int.class, false));
        Assert.assertEquals(CastUtils.Cast.Coverage.full, CastUtils.Casts.isConvertible(int.class, Integer.class, false));
        Assert.assertEquals(CastUtils.Cast.Coverage.full, CastUtils.Casts.isConvertible(String.class, Object.class, false));
        Assert.assertEquals(CastUtils.Cast.Coverage.full, CastUtils.Casts.isConvertible(RAbstractIntVector.class, RAbstractVector.class, false));
        Assert.assertEquals(CastUtils.Cast.Coverage.full, CastUtils.Casts.isConvertible(Object.class, Object.class, false));
        Assert.assertEquals(CastUtils.Cast.Coverage.full, CastUtils.Casts.isConvertible(RAbstractVector.class, RAbstractVector.class, false));

        Assert.assertEquals(CastUtils.Cast.Coverage.full, CastUtils.Casts.isConvertible(Not.negateType(Integer.class), Not.negateType(int.class), false));
        Assert.assertEquals(CastUtils.Cast.Coverage.full, CastUtils.Casts.isConvertible(Not.negateType(RSequence.class), Not.negateType(RIntSequence.class), false));
        Assert.assertEquals(CastUtils.Cast.Coverage.full, CastUtils.Casts.isConvertible(Not.negateType(String.class), Object.class, false));
        Assert.assertEquals(CastUtils.Cast.Coverage.full, CastUtils.Casts.isConvertible(RIntSequence.class, Not.negateType(String.class), false));
        // final class -> interface
        Assert.assertEquals(CastUtils.Cast.Coverage.full, CastUtils.Casts.isConvertible(RIntSequence.class, Not.negateType(Serializable.class), false));
        // interface -> final class
        Assert.assertEquals(CastUtils.Cast.Coverage.full, CastUtils.Casts.isConvertible(Serializable.class, Not.negateType(RIntSequence.class), false));
    }

    @Test
    public void testIsPartiallyConvertible() {
        Assert.assertEquals(CastUtils.Cast.Coverage.partial, CastUtils.Casts.isConvertible(Object.class, String.class, false));
        Assert.assertEquals(CastUtils.Cast.Coverage.partial, CastUtils.Casts.isConvertible(RAbstractVector.class, RAbstractIntVector.class, false));

        Assert.assertEquals(CastUtils.Cast.Coverage.partial, CastUtils.Casts.isConvertible(Not.negateType(RAbstractIntVector.class), Not.negateType(RAbstractVector.class), false));

        Assert.assertEquals(CastUtils.Cast.Coverage.partial, CastUtils.Casts.isConvertible(Not.negateType(RIntSequence.class), String.class, false));
        Assert.assertEquals(CastUtils.Cast.Coverage.partial, CastUtils.Casts.isConvertible(Not.negateType(RIntSequence.class), Serializable.class, false));
        Assert.assertEquals(CastUtils.Cast.Coverage.partial, CastUtils.Casts.isConvertible(Not.negateType(Serializable.class), RIntSequence.class, false));

    }

    @Test
    public void testIsPotentiallyConvertibleNo() {
        // interface -> interface
        Assert.assertEquals(CastUtils.Cast.Coverage.potential, CastUtils.Casts.isConvertible(RAbstractVector.class, Serializable.class, false));
        // abstract class -> interface
        Assert.assertEquals(CastUtils.Cast.Coverage.potential, CastUtils.Casts.isConvertible(RSequence.class, Serializable.class, false));
        // interface -> abstract class
        Assert.assertEquals(CastUtils.Cast.Coverage.potential, CastUtils.Casts.isConvertible(Serializable.class, RSequence.class, false));

        // implicit conversions are by default "potential"
        Assert.assertEquals(CastUtils.Cast.Coverage.potential, CastUtils.Casts.isConvertible(String.class, RAbstractStringVector.class, true));
    }

    @Test
    public void testIsNotConvertible() {
        Assert.assertEquals(CastUtils.Cast.Coverage.none, CastUtils.Casts.isConvertible(RIntSequence.class, String.class, false));
        // final class -> interface
        Assert.assertEquals(CastUtils.Cast.Coverage.none, CastUtils.Casts.isConvertible(RIntSequence.class, Serializable.class, false));
        // interface -> final class
        Assert.assertEquals(CastUtils.Cast.Coverage.none, CastUtils.Casts.isConvertible(Serializable.class, RIntSequence.class, false));
        // Nothing -> Not<String>
        Assert.assertEquals(CastUtils.Cast.Coverage.none, CastUtils.Casts.isConvertible(Not.negateType(Object.class), Not.negateType(String.class), false));
        // Nothing -> Nothing
        Assert.assertEquals(CastUtils.Cast.Coverage.none, CastUtils.Casts.isConvertible(Not.negateType(Object.class), Not.negateType(Object.class), false));
        // class -> not(class)
        Assert.assertEquals(CastUtils.Cast.Coverage.none, CastUtils.Casts.isConvertible(Object.class, Not.negateType(Object.class), false));
        Assert.assertEquals(CastUtils.Cast.Coverage.none, CastUtils.Casts.isConvertible(Not.negateType(Object.class), Object.class, false));
        Assert.assertEquals(CastUtils.Cast.Coverage.none, CastUtils.Casts.isConvertible(String.class, Not.negateType(String.class), false));
        Assert.assertEquals(CastUtils.Cast.Coverage.none, CastUtils.Casts.isConvertible(Not.negateType(String.class), String.class, false));
        Assert.assertEquals(CastUtils.Cast.Coverage.none, CastUtils.Casts.isConvertible(Not.negateType(String.class), Not.negateType(Object.class), false));
        Assert.assertEquals(CastUtils.Cast.Coverage.none, CastUtils.Casts.isConvertible(Not.negateType(Object.class), Not.negateType(String.class), false));
        Assert.assertEquals(CastUtils.Cast.Coverage.none, CastUtils.Casts.isConvertible(Not.negateType(RAbstractIntVector.class), RAbstractIntVector.class, false));
    }
}
