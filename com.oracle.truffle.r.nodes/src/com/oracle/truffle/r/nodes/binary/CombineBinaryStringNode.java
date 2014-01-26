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
package com.oracle.truffle.r.nodes.binary;

import java.util.*;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.runtime.data.*;

@SuppressWarnings("unused")
public abstract class CombineBinaryStringNode extends CombineBinaryNode {

    @Specialization
    public RNull combine(RNull left, RNull right) {
        return RNull.instance;
    }

    @Specialization(order = 1)
    public String combine(RNull left, String right) {
        return right;
    }

    @Specialization(order = 2)
    public String combine(String left, RNull right) {
        return left;
    }

    @Specialization(order = 3)
    public RStringVector combine(RStringVector left, RNull right) {
        return left;
    }

    @Specialization(order = 4)
    public RStringVector combine(RNull left, RStringVector right) {
        return right;
    }

    @Specialization(order = 5)
    public RStringVector combine(String left, String right) {
        return RDataFactory.createStringVector(new String[]{left, right}, RDataFactory.INCOMPLETE_VECTOR);
    }

    @Specialization(order = 6)
    public RStringVector combine(RStringVector left, String right) {
        int dataLength = left.getLength();
        String[] result = new String[dataLength + 1];
        for (int i = 0; i < dataLength; ++i) {
            result[i] = left.getDataAt(i);
        }
        result[dataLength] = right;
        return RDataFactory.createStringVector(result, RDataFactory.INCOMPLETE_VECTOR, combineNames(left, false));
    }

    @Specialization(order = 7)
    public RStringVector combine(String left, RStringVector right) {
        int dataLength = right.getLength();
        String[] result = new String[dataLength + 1];
        for (int i = 0; i < dataLength; ++i) {
            result[i + 1] = right.getDataAt(i);
        }
        result[0] = left;
        return RDataFactory.createStringVector(result, RDataFactory.INCOMPLETE_VECTOR, combineNames(right, true));
    }

    @Specialization(order = 8)
    public RStringVector combine(RStringVector left, RStringVector right) {
        return (RStringVector) genericCombine(left, right);
    }

}
