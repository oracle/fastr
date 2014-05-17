/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.util.regex.*;

import com.oracle.truffle.api.CompilerDirectives.SlowPath;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin(value = "grep")
public abstract class Grep extends RBuiltinNode {

    @Specialization
    public RIntVector grep(String pattern, RAbstractStringVector vector) {
        controlVisibility();
        int[] result = findAllIndexes(pattern, vector);
        if (result == null) {
            return RDataFactory.createEmptyIntVector();
        } else {
            return RDataFactory.createIntVector(result, RDataFactory.COMPLETE_VECTOR);
        }
    }

    @SlowPath
    protected static int[] findAllIndexes(String pattern, RAbstractStringVector vector) {
        int[] tmp = new int[vector.getLength()];
        int numMatches = 0;
        int ind = 0;
        for (int i = 0; i < vector.getLength(); i++) {
            if (findIndex(pattern, vector.getDataAt(i))) {
                numMatches++;
                tmp[ind++] = i + 1;
            }
        }
        if (numMatches == 0) {
            return null;
        } else if (numMatches == vector.getLength()) {
            return tmp;
        } else {
            // trim array to the appropriate size
            int[] result = new int[numMatches];
            for (int i = 0; i < result.length; i++) {
                result[i] = tmp[i];
            }
            return result;
        }
    }

    protected static boolean findIndex(String pattern, String text) {
        Matcher m = Regexp.getPatternMatcher(pattern, text);
        if (m.find()) {
            return true;
        } else {
            return false;
        }
    }

}
