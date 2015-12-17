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
import com.oracle.truffle.r.runtime.data.RStringVector;

@SuppressWarnings("unused")
public abstract class CombineBinaryStringNode extends CombineBinaryNode {

    @Specialization
    protected RNull combine(RNull left, RNull right) {
        return RNull.instance;
    }

    @Specialization
    protected String combine(RNull left, String right) {
        return right;
    }

    @Specialization
    protected String combine(String left, RNull right) {
        return left;
    }

    @Specialization
    protected RStringVector combine(RStringVector left, RNull right) {
        return left;
    }

    @Specialization
    protected RStringVector combine(RNull left, RStringVector right) {
        return right;
    }

    @Specialization
    protected RStringVector combine(String left, String right) {
        return RDataFactory.createStringVector(new String[]{left, right}, RDataFactory.INCOMPLETE_VECTOR);
    }

    @Specialization
    protected RStringVector combine(RStringVector left, String right, //
                    @Cached("createCountingProfile()") LoopConditionProfile profile) {
        int dataLength = left.getLength();
        String[] result = new String[dataLength + 1];
        profile.profileCounted(dataLength);
        for (int i = 0; profile.inject(i < dataLength); i++) {
            result[i] = left.getDataAt(i);
        }
        result[dataLength] = right;
        return RDataFactory.createStringVector(result, RDataFactory.INCOMPLETE_VECTOR, combineNames(left, false, profile));
    }

    @Specialization
    protected RStringVector combine(String left, RStringVector right, //
                    @Cached("createCountingProfile()") LoopConditionProfile profile) {
        int dataLength = right.getLength();
        String[] result = new String[dataLength + 1];
        profile.profileCounted(dataLength);
        for (int i = 0; profile.inject(i < dataLength); i++) {
            result[i + 1] = right.getDataAt(i);
        }
        result[0] = left;
        return RDataFactory.createStringVector(result, RDataFactory.INCOMPLETE_VECTOR, combineNames(right, true, profile));
    }

    @Specialization
    protected RStringVector combine(RStringVector left, RStringVector right, //
                    @Cached("createCountingProfile()") LoopConditionProfile profileLeft, //
                    @Cached("createCountingProfile()") LoopConditionProfile profileRight) {
        return (RStringVector) genericCombine(left, right, profileLeft, profileRight);
    }

}
