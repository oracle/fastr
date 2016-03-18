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
package com.oracle.truffle.r.nodes.unary;

import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.NodeFields;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.NullProfile;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.data.RAttributeProfiles;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;

@NodeFields({@NodeField(name = "preserveNames", type = boolean.class), @NodeField(name = "dimensionsPreservation", type = boolean.class), @NodeField(name = "attrPreservation", type = boolean.class)})
public abstract class CastBaseNode extends CastNode {

    private final BranchProfile listCoercionErrorBranch = BranchProfile.create();
    private final ConditionProfile hasDimNamesProfile = ConditionProfile.createBinaryProfile();
    private final NullProfile hasDimensionsProfile = NullProfile.create();
    private final NullProfile hasNamesProfile = NullProfile.create();
    private final RAttributeProfiles attrProfiles = RAttributeProfiles.create();

    protected abstract boolean isPreserveNames();

    protected abstract boolean isDimensionsPreservation();

    protected abstract boolean isAttrPreservation();

    protected RError throwCannotCoerceListError(String type) {
        listCoercionErrorBranch.enter();
        throw RError.error(this, RError.Message.LIST_COERCION, type);
    }

    protected int[] getPreservedDimensions(RAbstractContainer operand) {
        if (isDimensionsPreservation()) {
            return hasDimensionsProfile.profile(operand.getDimensions());
        } else {
            return null;
        }
    }

    protected RStringVector getPreservedNames(RAbstractContainer operand) {
        if (isPreserveNames()) {
            return hasNamesProfile.profile(operand.getNames(attrProfiles));
        } else {
            return null;
        }
    }

    protected void preserveDimensionNames(RAbstractContainer operand, RVector ret) {
        if (isDimensionsPreservation()) {
            RList dimNames = operand.getDimNames(attrProfiles);
            if (hasDimNamesProfile.profile(dimNames != null)) {
                ret.setDimNames((RList) dimNames.copy());
            }
        }
    }
}
