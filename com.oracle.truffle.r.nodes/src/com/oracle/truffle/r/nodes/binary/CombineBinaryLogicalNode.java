/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.ops.na.*;

@SuppressWarnings("unused")
public abstract class CombineBinaryLogicalNode extends CombineBinaryNode {

    private final NACheck check = NACheck.create();

    @Specialization
    protected RNull combine(RNull left, RNull right) {
        return RNull.instance;
    }

    @Specialization
    protected byte combine(RNull left, byte right) {
        return right;
    }

    @Specialization
    protected byte combine(byte left, RNull right) {
        return left;
    }

    @Specialization
    protected RLogicalVector combine(RLogicalVector left, RNull right) {
        return left;
    }

    @Specialization
    protected RLogicalVector combine(RNull left, RLogicalVector right) {
        return right;
    }

    @Specialization
    protected RLogicalVector combine(byte left, byte right) {
        return RDataFactory.createLogicalVector(new byte[]{left, right}, !RRuntime.isNA(left) && !RRuntime.isNA(right));
    }

    @Specialization
    protected RLogicalVector combine(RLogicalVector left, byte right) {
        check.enable(left);
        check.enable(right);
        int dataLength = left.getLength();
        byte[] result = new byte[dataLength + 1];
        for (int i = 0; i < dataLength; i++) {
            byte value = left.getDataAt(i);
            check.check(value);
            result[i] = value;
        }
        result[dataLength] = right;
        check.check(right);
        return RDataFactory.createLogicalVector(result, check.neverSeenNA(), combineNames(left, false));
    }

    @Specialization
    protected RLogicalVector combine(byte left, RLogicalVector right) {
        check.enable(right);
        check.enable(left);
        int dataLength = right.getLength();
        byte[] result = new byte[dataLength + 1];
        for (int i = 0; i < dataLength; i++) {
            byte value = right.getDataAt(i);
            check.check(value);
            result[i + 1] = value;
        }
        result[0] = left;
        check.check(left);
        return RDataFactory.createLogicalVector(result, check.neverSeenNA(), combineNames(right, true));
    }

    @Specialization
    protected RLogicalVector combine(RLogicalVector left, RLogicalVector right) {
        return (RLogicalVector) genericCombine(left, right);
    }
}
