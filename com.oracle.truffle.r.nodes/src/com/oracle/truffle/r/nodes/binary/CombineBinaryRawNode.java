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

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.LoopConditionProfile;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RRaw;
import com.oracle.truffle.r.runtime.data.RRawVector;

@SuppressWarnings("unused")
/** Takes only RNull, RRaw or RRawVector as arguments. Use CastRawNode to cast the operands. */
public abstract class CombineBinaryRawNode extends CombineBinaryNode {

    @Specialization
    protected RNull combine(RNull left, RNull right) {
        return RNull.instance;
    }

    @Specialization
    protected RRaw combine(RNull left, RRaw right) {
        return right;
    }

    @Specialization
    protected RRaw combine(RRaw left, RNull right) {
        return left;
    }

    @Specialization
    protected RRawVector combine(RRawVector left, RNull right) {
        return left;
    }

    @Specialization
    protected RRawVector combine(RNull left, RRawVector right) {
        return right;
    }

    @Specialization
    protected RRawVector combine(RRaw left, RRaw right) {
        return RDataFactory.createRawVector(new byte[]{left.getValue(), right.getValue()});
    }

    @Specialization
    protected RRawVector combine(RRawVector left, RRaw right, //
                    @Cached("createCountingProfile()") LoopConditionProfile profile) {
        int dataLength = left.getLength();
        byte[] result = new byte[dataLength + 1];
        profile.profileCounted(dataLength);
        for (int i = 0; profile.inject(i < dataLength); i++) {
            result[i] = left.getDataAt(i).getValue();
        }
        result[dataLength] = right.getValue();
        return RDataFactory.createRawVector(result, combineNames(left, false, profile));
    }

    @Specialization
    protected RRawVector combine(RRaw left, RRawVector right, //
                    @Cached("createCountingProfile()") LoopConditionProfile profile) {
        int dataLength = right.getLength();
        byte[] result = new byte[dataLength + 1];
        profile.profileCounted(dataLength);
        for (int i = 0; profile.inject(i < dataLength); i++) {
            result[i + 1] = right.getDataAt(i).getValue();
        }
        result[0] = left.getValue();
        return RDataFactory.createRawVector(result, combineNames(right, true, profile));
    }

    @Specialization
    protected RRawVector combine(RRawVector left, RRawVector right, //
                    @Cached("createCountingProfile()") LoopConditionProfile profileLeft, //
                    @Cached("createCountingProfile()") LoopConditionProfile profileRight) {
        return (RRawVector) genericCombine(left, right, profileLeft, profileRight);
    }

}
