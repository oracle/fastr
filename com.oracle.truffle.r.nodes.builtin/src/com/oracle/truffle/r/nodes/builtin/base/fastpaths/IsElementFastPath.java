/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.nodes.builtin.base.fastpaths;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntSeqVectorData;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary;
import com.oracle.truffle.r.runtime.nodes.RFastPathNode;

@ImportStatic(DSLConfig.class)
public abstract class IsElementFastPath extends RFastPathNode {

    @Specialization(guards = {"elLib.getLength(el.getData()) == 1"}, limit = "getTypedVectorDataLibraryCacheSize()")
    protected Byte iselementOneCachedString(RStringVector el, RStringVector set,
                    @CachedLibrary("el.getData()") VectorDataLibrary elLib,
                    @CachedLibrary("set.getData()") VectorDataLibrary setLib,
                    @Cached("create()") BranchProfile trueProfile,
                    @Cached("create()") BranchProfile falseProfile) {
        Object elData = el.getData();
        Object setData = set.getData();
        String element = elLib.getStringAt(elData, 0);
        VectorDataLibrary.SeqIterator it = setLib.iterator(setData);
        while (setLib.nextLoopCondition(setData, it)) {
            if (element.equals(setLib.getNextString(setData, it))) {
                trueProfile.enter();
                return RRuntime.LOGICAL_TRUE;
            }
        }
        falseProfile.enter();
        return RRuntime.LOGICAL_FALSE;
    }

    @Specialization
    protected Byte isElementOne(double el, double set) {
        return RRuntime.asLogical(el == set);
    }

    @Specialization(guards = {"elLib.getLength(el.getData()) == 1"}, limit = "getTypedVectorDataLibraryCacheSize()")
    protected Byte iselementOne(RDoubleVector el, RDoubleVector set,
                    @CachedLibrary("el.getData()") VectorDataLibrary elLib,
                    @CachedLibrary("set.getData()") VectorDataLibrary setLib,
                    @Cached("create()") BranchProfile trueProfile,
                    @Cached("create()") BranchProfile falseProfile) {
        Object elData = el.getData();
        Object setData = set.getData();
        double element = elLib.getDoubleAt(elData, 0);
        VectorDataLibrary.SeqIterator it = setLib.iterator(setData);
        while (setLib.nextLoopCondition(setData, it)) {
            if (element == setLib.getNextDouble(setData, it)) {
                trueProfile.enter();
                return RRuntime.LOGICAL_TRUE;
            }
        }
        falseProfile.enter();
        return RRuntime.LOGICAL_FALSE;
    }

    @Specialization(guards = {"set.isSequence()", "elLib.getLength(el.getData()) == 1", "set.getSequence().getStride() >= 0"}, limit = "getTypedVectorDataLibraryCacheSize()")
    protected Byte isElementOneSequence(RDoubleVector el, RIntVector set,
                    @CachedLibrary("el.getData()") VectorDataLibrary elLib,
                    @Cached("createBinaryProfile()") ConditionProfile profile) {
        double element = elLib.getDoubleAt(el.getData(), 0);
        RIntSeqVectorData seq = set.getSequence();
        return RRuntime.asLogical(profile.profile(element >= seq.getStart() && element <= seq.getEnd()));
    }

    @Specialization(replaces = "isElementOneSequence", guards = {"!set.isSequence()", "elLib.getLength(el.getData()) == 1"}, limit = "getTypedVectorDataLibraryCacheSize()")
    protected Byte isElementOne(RDoubleVector el, RIntVector set,
                    @CachedLibrary("el.getData()") VectorDataLibrary elLib,
                    @CachedLibrary("set.getData()") VectorDataLibrary setLib,
                    @Cached("create()") BranchProfile trueProfile,
                    @Cached("create()") BranchProfile falseProfile) {
        Object elData = el.getData();
        Object setData = set.getData();
        double element = elLib.getDoubleAt(elData, 0);

        VectorDataLibrary.SeqIterator it = setLib.iterator(setData);
        while (setLib.nextLoopCondition(setData, it)) {
            if (setLib.isNextNA(setData, it)) {
                if (RRuntime.isNA(element)) {
                    trueProfile.enter();
                    return RRuntime.LOGICAL_TRUE;
                }
            } else if (element == setLib.getNextInt(setData, it)) {
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
