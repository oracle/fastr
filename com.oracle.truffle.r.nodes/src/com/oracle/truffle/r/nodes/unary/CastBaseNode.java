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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetDimAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetDimNamesAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetNamesAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.SetDimNamesAttributeNode;
import com.oracle.truffle.r.runtime.NullProfile;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RTypedValue;
import com.oracle.truffle.r.runtime.data.RVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.env.REnvironment;

public abstract class CastBaseNode extends CastNode {

    private final BranchProfile listCoercionErrorBranch = BranchProfile.create();
    private final ConditionProfile hasDimNamesProfile = ConditionProfile.createBinaryProfile();
    private final NullProfile hasDimensionsProfile = NullProfile.create();
    private final NullProfile hasNamesProfile = NullProfile.create();
    @Child private GetNamesAttributeNode getNamesNode = GetNamesAttributeNode.create();
    @Child private GetDimAttributeNode getDimNode;
    @Child private SetDimNamesAttributeNode setDimNamesNode;
    @Child private GetDimNamesAttributeNode getDimNamesNode;

    private final boolean preserveNames;
    private final boolean preserveDimensions;
    private final boolean preserveAttributes;

    protected CastBaseNode(boolean preserveNames, boolean preserveDimensions, boolean preserveAttributes) {
        this.preserveNames = preserveNames;
        this.preserveDimensions = preserveDimensions;
        this.preserveAttributes = preserveAttributes;
        if (preserveDimensions) {
            getDimNamesNode = GetDimNamesAttributeNode.create();
        }
    }

    public boolean preserveNames() {
        return preserveNames;
    }

    public final boolean preserveDimensions() {
        return preserveDimensions;
    }

    public boolean preserveAttributes() {
        return preserveAttributes;
    }

    protected abstract RType getTargetType();

    protected RError throwCannotCoerceListError(String type) {
        listCoercionErrorBranch.enter();
        throw RError.error(this, RError.Message.LIST_COERCION, type);
    }

    protected int[] getPreservedDimensions(RAbstractContainer operand) {
        if (preserveDimensions()) {
            if (getDimNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getDimNode = insert(GetDimAttributeNode.create());
            }
            return hasDimensionsProfile.profile(getDimNode.getDimensions(operand));
        } else {
            return null;
        }
    }

    protected RStringVector getPreservedNames(RAbstractContainer operand) {
        if (preserveNames()) {
            return hasNamesProfile.profile(getNamesNode.getNames(operand));
        } else {
            return null;
        }
    }

    protected void preserveDimensionNames(RAbstractContainer operand, RVector<?> ret) {
        if (preserveDimensions()) {
            RList dimNames = getDimNamesNode.getDimNames(operand);
            if (hasDimNamesProfile.profile(dimNames != null)) {
                if (setDimNamesNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    setDimNamesNode = insert(SetDimNamesAttributeNode.create());
                }
                setDimNamesNode.setDimNames(ret, (RList) dimNames.copy());
            }
        }
    }

    @Fallback
    @TruffleBoundary
    protected Object doOther(Object value) {
        Object mappedValue = RRuntime.asAbstractVector(value);
        if (mappedValue instanceof REnvironment) {
            throw RError.error(RError.SHOW_CALLER, RError.Message.ENVIRONMENTS_COERCE);
        } else if (mappedValue instanceof RTypedValue) {
            throw RError.error(RError.SHOW_CALLER, RError.Message.CANNOT_COERCE, ((RTypedValue) mappedValue).getRType().getName(), getTargetType().getName());
        } else if (mappedValue instanceof TruffleObject) {
            throw RError.error(RError.SHOW_CALLER, RError.Message.CANNOT_COERCE, "truffleobject", getTargetType().getName());
        } else {
            throw RInternalError.shouldNotReachHere("unexpected value of type " + (mappedValue == null ? "null" : mappedValue.getClass()));
        }
    }
}
