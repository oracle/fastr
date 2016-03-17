/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.data.closures;

import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

/*
 * This closure is meant to be used only for implementation of the binary operators.
 */
public class RFactorToDoubleVectorClosure extends RToDoubleVectorClosure implements RAbstractDoubleVector {

    private final RAbstractDoubleVector levels;
    private final boolean withNames;

    public RFactorToDoubleVectorClosure(RFactor factor, RAbstractDoubleVector levels, boolean withNames) {
        super(factor.getVector());
        assert levels != null;
        this.levels = levels;
        this.withNames = withNames;
    }

    @Override
    public final RAbstractVector castSafe(RType type, ConditionProfile isNAProfile) {
        switch (type) {
            case Double:
                return this;
            case Character:
                return new RDoubleToStringVectorClosure(this);
            case Complex:
                return new RDoubleToComplexVectorClosure(this);
            default:
                return null;
        }
    }

    public double getDataAt(int index) {
        int val = ((RIntVector) vector).getDataAt(index);
        if (!vector.isComplete() && RRuntime.isNA(val)) {
            return RRuntime.DOUBLE_NA;
        } else {
            return levels.getDataAt(val - 1);
        }
    }

    @Override
    public RStringVector getNames(RAttributeProfiles attrProfiles) {
        return withNames ? super.getNames(attrProfiles) : null;
    }

}
