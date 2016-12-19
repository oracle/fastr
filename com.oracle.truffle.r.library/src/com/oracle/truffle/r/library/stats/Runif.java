/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.library.stats;

import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.library.stats.RandGenerationFunctions.RandFunction2_Double;
import com.oracle.truffle.r.library.stats.RandGenerationFunctions.RandomNumberProvider;
import com.oracle.truffle.r.runtime.RRuntime;

public final class Runif extends RandFunction2_Double {
    private final BranchProfile errorProfile = BranchProfile.create();
    private final ConditionProfile minEqualsMaxProfile = ConditionProfile.createBinaryProfile();
    private final ValueProfile minValueProfile = ValueProfile.createEqualityProfile();
    private final ValueProfile maxValueProfile = ValueProfile.createEqualityProfile();

    @Override
    public double execute(double minIn, double maxIn, RandomNumberProvider rand) {
        double min = minValueProfile.profile(minIn);
        double max = maxValueProfile.profile(maxIn);
        if (!RRuntime.isFinite(min) || !RRuntime.isFinite(max) || max < min) {
            errorProfile.enter();
            return RMath.mlError();
        }
        if (minEqualsMaxProfile.profile(min == max)) {
            return min;
        }
        return min + rand.unifRand() * (max - min);
    }
}
