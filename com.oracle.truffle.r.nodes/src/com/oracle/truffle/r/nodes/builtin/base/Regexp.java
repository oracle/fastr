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
import java.util.regex.*;

import com.oracle.truffle.api.CompilerDirectives.SlowPath;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin(value = "regexpr")
public abstract class Regexp extends RBuiltinNode {

    @Specialization
    public Object regexp(String pattern, RAbstractStringVector vector) {
        controlVisibility();
        int[] result = new int[vector.getLength()];
        for (int i = 0; i < vector.getLength(); i++) {
            result[i] = findIndex(pattern, vector.getDataAt(i)).get(0);
        }
        return RDataFactory.createIntVector(result, RDataFactory.COMPLETE_VECTOR);
    }

    protected static List<Integer> findIndex(String pattern, String text) {
        Matcher m = getPatternMatcher(pattern, text);
        List<Integer> list = new ArrayList<>();
        while (m.find()) {
            // R starts counting at index 1
            list.add(m.start() + 1);
        }
        if (list.size() > 0) {
            return list;
        }
        list.add(-1);
        return list;
    }

    @SlowPath
    public static Matcher getPatternMatcher(String pattern, String text) {
        return Pattern.compile(pattern).matcher(text);
    }
}
