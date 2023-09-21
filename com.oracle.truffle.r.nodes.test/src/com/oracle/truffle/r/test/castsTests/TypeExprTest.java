/*
 * Copyright (c) 2013, 2023, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.test.castsTests;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.test.casts.CastUtils;
import com.oracle.truffle.r.test.casts.Not;
import com.oracle.truffle.r.test.casts.TypeExpr;
import com.oracle.truffle.r.test.casts.UpperBoundsConjunction;

public class TypeExprTest {

    @Test
    public void testNormalize() {
        Assert.assertEquals(toSet(String.class), TypeExpr.union(String.class).toNormalizedConjunctionSet());
        Assert.assertEquals(toSet(String.class, Integer.class), TypeExpr.union(String.class, Integer.class).toNormalizedConjunctionSet());
        Assert.assertEquals(toSet(String.class), TypeExpr.atom(String.class).and(TypeExpr.atom(RNull.class).not()).toNormalizedConjunctionSet());
        Assert.assertEquals(toSet(UpperBoundsConjunction.create(Not.negateType(String.class), Not.negateType(Integer.class))),
                        TypeExpr.atom(String.class).not().and(TypeExpr.atom(Integer.class).not()).toNormalizedConjunctionSet());
        Assert.assertEquals(toSet(), TypeExpr.atom(String.class).not().and(TypeExpr.atom(String.class)).toNormalizedConjunctionSet());
        Assert.assertEquals(toSet(UpperBoundsConjunction.create(Runnable.class, RStringVector.class)),
                        TypeExpr.atom(Runnable.class).and(TypeExpr.atom(RStringVector.class)).toNormalizedConjunctionSet());
        Assert.assertEquals(toSet(RStringVector.class), TypeExpr.atom(UpperBoundsConjunction.fromType(RStringVector.class)).toNormalizedConjunctionSet());
        Assert.assertEquals(toSet(UpperBoundsConjunction.create(RStringVector.class, Runnable.class)),
                        TypeExpr.atom(UpperBoundsConjunction.fromType(Runnable.class)).and(TypeExpr.atom(UpperBoundsConjunction.fromType(RStringVector.class))).toNormalizedConjunctionSet());
    }

    @Test
    public void testContains() {
        TypeExpr te = TypeExpr.atom(String.class).or(TypeExpr.atom(RNull.class).not());
        Assert.assertTrue(te.contains(String.class));
        Assert.assertTrue(te.contains(Not.negateType(RNull.class)));
        Assert.assertFalse(te.contains(RNull.class));
    }

    @Test
    public void testNot() {
        TypeExpr from = TypeExpr.atom(String.class).or(TypeExpr.atom(RNull.class).not());
        Assert.assertFalse(CastUtils.Casts.existsConvertibleActualType(from, RNull.class, false));
        Assert.assertTrue(CastUtils.Casts.existsConvertibleActualType(from, String.class, false));
        Assert.assertTrue(CastUtils.Casts.existsConvertibleActualType(from, Integer.class, false));
    }

    @Test
    public void testLower() {
        TypeExpr te = TypeExpr.atom(String.class);
        Assert.assertEquals(toSet(UpperBoundsConjunction.fromType(String.class).asWildcard("a")), te.lower("a").toNormalizedConjunctionSet());
        te = TypeExpr.atom(String.class).or(TypeExpr.atom(RNull.class));
        Assert.assertEquals(toSet(UpperBoundsConjunction.fromType(String.class).asWildcard("a"), UpperBoundsConjunction.fromType(RNull.class).asWildcard("a")),
                        te.lower("a").toNormalizedConjunctionSet());
        te = TypeExpr.atom(Runnable.class).and(TypeExpr.atom(RStringVector.class));
        Set<Type> exp = toSet(UpperBoundsConjunction.create(Runnable.class, RStringVector.class).asWildcard("a"));
        Assert.assertEquals(exp, te.lower("a").toNormalizedConjunctionSet());

        te = TypeExpr.atom(RIntVector.class).and(TypeExpr.atom(RDoubleVector.class).lower());
        Assert.assertTrue(te.isNothing());
    }

    private static Set<Type> toSet(Type... classes) {
        return new HashSet<>(Arrays.asList(classes));
    }
}
