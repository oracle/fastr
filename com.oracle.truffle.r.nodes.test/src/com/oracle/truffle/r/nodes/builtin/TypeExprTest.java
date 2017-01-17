/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.nodes.casts.Not.negateType;
import static com.oracle.truffle.r.nodes.casts.TypeExpr.atom;
import static com.oracle.truffle.r.nodes.casts.TypeExpr.union;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.oracle.truffle.r.nodes.casts.CastUtils;
import com.oracle.truffle.r.nodes.casts.Not;
import com.oracle.truffle.r.nodes.casts.TypeConjunction;
import com.oracle.truffle.r.nodes.casts.TypeExpr;
import com.oracle.truffle.r.runtime.data.RIntSequence;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RSequence;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;

public class TypeExprTest {

    @Test
    public void testNormalize() {
        Assert.assertEquals(toSet(String.class), union(String.class).normalize());
        Assert.assertEquals(toSet(String.class, Integer.class), union(String.class, Integer.class).normalize());
        Assert.assertEquals(toSet(RIntSequence.class), atom(RIntSequence.class).and(atom(RSequence.class)).normalize());
        Assert.assertEquals(toSet(String.class), atom(String.class).and(atom(RNull.class).not()).normalize());
        Assert.assertEquals(toSet(TypeConjunction.create(negateType(String.class), negateType(Integer.class))),
                        atom(String.class).not().and(atom(Integer.class).not()).normalize());
        Assert.assertEquals(toSet(), atom(String.class).not().and(atom(String.class)).normalize());
        Assert.assertEquals(toSet(TypeConjunction.create(RAbstractIntVector.class, RAbstractStringVector.class)), atom(RAbstractIntVector.class).and(atom(RAbstractStringVector.class)).normalize());
    }

    @Test
    public void testContains() {
        TypeExpr te = atom(String.class).or(atom(RNull.class).not());
        Assert.assertTrue(te.contains(String.class));
        Assert.assertTrue(te.contains(Not.negateType(RNull.class)));
        Assert.assertFalse(te.contains(RNull.class));
    }

    @Test
    public void testNot() {
        TypeExpr from = atom(String.class).or(atom(RNull.class).not());
        Assert.assertFalse(CastUtils.Casts.existsConvertibleActualType(from, RNull.class, false));
        Assert.assertTrue(CastUtils.Casts.existsConvertibleActualType(from, String.class, false));
        Assert.assertTrue(CastUtils.Casts.existsConvertibleActualType(from, Integer.class, false));
    }

    private static Set<Type> toSet(Type... classes) {
        return new HashSet<>(Arrays.asList(classes));
    }
}
