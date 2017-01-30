/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.base.fastpaths;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RIntSequence;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.nodes.RFastPathNode;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

public abstract class IsElementFastPath extends RFastPathNode {

    @Specialization(guards = "el.getLength() == 1")
    protected Byte iselementOne(RAbstractStringVector el, RAbstractStringVector set,
                    @Cached("create()") BranchProfile trueProfile,
                    @Cached("create()") BranchProfile falseProfile) {
        String element = el.getDataAt(0);
        int length = set.getLength();
        for (int i = 0; i < length; i++) {
            if (element.equals(set.getDataAt(i))) {
                trueProfile.enter();
                return RRuntime.LOGICAL_TRUE;
            }
        }
        falseProfile.enter();
        return RRuntime.LOGICAL_FALSE;
    }

    @Specialization
    protected Byte iselementOne(double el, double set) {
        return RRuntime.asLogical(el == set);
    }

    @Specialization(guards = "el.getLength() == 1")
    protected Byte iselementOne(RAbstractDoubleVector el, RAbstractDoubleVector set,
                    @Cached("create()") BranchProfile trueProfile,
                    @Cached("create()") BranchProfile falseProfile) {
        double element = el.getDataAt(0);
        int length = set.getLength();
        for (int i = 0; i < length; i++) {
            if (element == set.getDataAt(i)) {
                trueProfile.enter();
                return RRuntime.LOGICAL_TRUE;
            }
        }
        falseProfile.enter();
        return RRuntime.LOGICAL_FALSE;
    }

    @Specialization(guards = "el.getLength() == 1")
    protected Byte isElementOneSequence(RAbstractDoubleVector el, RIntSequence set,
                    @Cached("createBinaryProfile()") ConditionProfile profile) {
        double element = el.getDataAt(0);
        return RRuntime.asLogical(profile.profile(element >= set.getStart() && element <= set.getEnd()));
    }

    @Specialization(replaces = "isElementOneSequence", guards = "el.getLength() == 1")
    protected Byte iselementOne(RAbstractDoubleVector el, RAbstractIntVector set,
                    @Cached("create()") NACheck na,
                    @Cached("create()") BranchProfile trueProfile,
                    @Cached("create()") BranchProfile falseProfile) {
        double element = el.getDataAt(0);
        int length = set.getLength();
        na.enable(set);
        for (int i = 0; i < length; i++) {
            int data = set.getDataAt(i);
            if (na.check(data)) {
                if (RRuntime.isNA(element)) {
                    trueProfile.enter();
                    return RRuntime.LOGICAL_TRUE;
                }
            } else if (element == data) {
                trueProfile.enter();
                return RRuntime.LOGICAL_TRUE;
            }
        }
        falseProfile.enter();
        return RRuntime.LOGICAL_FALSE;
    }

    @Fallback
    @SuppressWarnings("unused")
    protected Object fallback(Object el, Object set) {
        return null;
    }
}
