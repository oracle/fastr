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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin(name = "dimnames", kind = PRIMITIVE, parameterNames = {"x"}, internalDispatch = true)
public abstract class DimNames extends RBuiltinNode {

    private final RAttributeProfiles attrProfiles = RAttributeProfiles.create();
    private final ConditionProfile nullProfile = ConditionProfile.createBinaryProfile();
    private final BranchProfile dataframeProfile = BranchProfile.create();
    private final BranchProfile factorProfile = BranchProfile.create();
    private final BranchProfile otherProfile = BranchProfile.create();

    @Specialization
    protected RNull getDimNames(@SuppressWarnings("unused") RNull operand) {
        controlVisibility();
        return RNull.instance;
    }

    @Specialization
    protected Object getDimNames(RAbstractContainer container) {
        controlVisibility();
        RList names;
        if (container instanceof RDataFrame) {
            dataframeProfile.enter();
            names = ((RDataFrame) container).getVector().getDimNames();
        } else if (container instanceof RFactor) {
            factorProfile.enter();
            names = ((RFactor) container).getVector().getDimNames();
        } else {
            otherProfile.enter();
            names = container.getDimNames(attrProfiles);
        }
        return nullProfile.profile(names == null) ? RNull.instance : names;
    }
}
