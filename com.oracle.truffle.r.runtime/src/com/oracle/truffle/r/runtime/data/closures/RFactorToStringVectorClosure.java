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

import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

/*
 * This closure is meant to be used only for implementation of the binary operators.
 */
public class RFactorToStringVectorClosure extends RToStringVectorClosure implements RAbstractStringVector {

    private final RAbstractStringVector levels;
    private final boolean withNames;

    public RFactorToStringVectorClosure(RFactor factor, RAbstractStringVector levels, boolean withNames) {
        super(factor.getVector());
        this.levels = levels;
        this.withNames = withNames;
        if (this.levels == null) {
            RError.warning(RError.SHOW_CALLER2, RError.Message.IS_NA_TO_NON_VECTOR, "NULL");
        }
    }

    @Override
    public final RAbstractVector castSafe(RType type) {
        switch (type) {
            case Character:
                return this;
            default:
                return null;
        }
    }

    public String getDataAt(int index) {
        if (levels == null || levels.getLength() == 0) {
            return RRuntime.STRING_NA;
        } else {
            int val = ((RIntVector) vector).getDataAt(index);
            if (!vector.isComplete() && RRuntime.isNA(val)) {
                return RRuntime.STRING_NA;
            } else {
                String l = levels.getDataAt(val - 1);
                if (!levels.isComplete() && RRuntime.isNA(l)) {
                    return "NA"; // for comparison
                } else {
                    return l;
                }
            }
        }
    }

    @Override
    public RStringVector getNames(RAttributeProfiles attrProfiles) {
        return withNames ? super.getNames(attrProfiles) : null;
    }
}
