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
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

public abstract class CombineBinaryListNode extends CombineBinaryNode {

    @Specialization
    protected RList combine(RList left, double right, //
                    @Cached("createCountingProfile()") LoopConditionProfile profile) {
        return extend(left, right, profile);
    }

    @Specialization
    protected RList combine(RList left, RAbstractVector right, //
                    @Cached("createCountingProfile()") LoopConditionProfile profileLeft, //
                    @Cached("createCountingProfile()") LoopConditionProfile profileRight) {
        Object[] data = left.getDataWithoutCopying();
        Object[] result = new Object[data.length + right.getLength()];
        profileLeft.profileCounted(data.length);
        for (int i = 0; profileLeft.inject(i < data.length); i++) {
            result[i] = data[i];
        }
        profileRight.profileCounted(right.getLength());
        for (int i = 0; profileRight.inject(i < right.getLength()); i++) {
            result[i + data.length] = right.getDataAtAsObject(i);
        }
        return RDataFactory.createList(result, combineNames(left, right, profileLeft, profileRight));
    }

    private RList extend(RList list, Object x, LoopConditionProfile profile) {
        final int ll = list.getLength();
        Object[] result = new Object[ll + 1];
        System.arraycopy(list.getDataWithoutCopying(), 0, result, 0, ll);
        result[ll] = x;
        return RDataFactory.createList(result, combineNames(list, false, profile));
    }
}
