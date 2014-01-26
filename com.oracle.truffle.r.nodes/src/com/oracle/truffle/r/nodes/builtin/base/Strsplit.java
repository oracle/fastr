/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.base;

import java.util.*;

import com.oracle.truffle.api.CompilerDirectives.SlowPath;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.ops.na.*;

@RBuiltin("strsplit")
public abstract class Strsplit extends RBuiltinNode {

    protected final NACheck na = NACheck.create();

    private static final Object[] PARAMETER_NAMES = new Object[]{"x", "split", "fixed", "perl", "useBytes"};

    @Override
    public Object[] getParameterNames() {
        return PARAMETER_NAMES;
    }

    @Override
    public RNode[] getParameterValues() {
        return new RNode[]{null, null, ConstantNode.create(false), ConstantNode.create(false), ConstantNode.create(false)};
    }

    @SuppressWarnings("unused")
    @Specialization
    public RList split(RAbstractStringVector x, String split, byte fixed, byte perl, byte useBytes) {
        List<RStringVector> result = new ArrayList<>(x.getLength());
        na.enable(x);
        for (int i = 0; i < x.getLength(); ++i) {
            result.add(splitIntl(x.getDataAt(i), split, na));
        }
        return RDataFactory.createList(result.toArray(new Object[0]));
    }

    @SuppressWarnings("unused")
    @Specialization
    public RList split(RAbstractStringVector x, RAbstractStringVector split, byte fixed, byte perl, byte useBytes) {
        List<RStringVector> result = new ArrayList<>(x.getLength());
        na.enable(x);
        for (int i = 0; i < x.getLength(); ++i) {
            result.add(splitIntl(x.getDataAt(i), getSplit(split, i), na));
        }
        return RDataFactory.createList(result.toArray(new Object[0]));
    }

    private static String getSplit(RAbstractStringVector split, int i) {
        return split.getDataAt(i % split.getLength());
    }

    @SlowPath
    private static RStringVector splitIntl(String input, String separator, NACheck check) {
        String[] result = input.split(separator);
        if (separator.equals("")) {
            result = Arrays.copyOfRange(result, 1, result.length);
        }
        return RDataFactory.createStringVector(result, check.neverSeenNA());
    }
}
