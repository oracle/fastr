/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetDimAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetDimNamesAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetNamesAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.SetDimNamesAttributeNode;
import com.oracle.truffle.r.runtime.NullProfile;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RSharingAttributeStorage;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RTypedValue;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.env.REnvironment;

public abstract class CastBaseNode extends CastNode {

    private final BranchProfile listCoercionErrorBranch = BranchProfile.create();
    private final ConditionProfile hasDimNamesProfile = ConditionProfile.createBinaryProfile();
    private final NullProfile hasDimensionsProfile = NullProfile.create();
    private final NullProfile hasNamesProfile = NullProfile.create();
    private final ValueProfile reuseClassProfile;

    @Child private GetNamesAttributeNode getNamesNode = GetNamesAttributeNode.create();
    @Child private GetDimAttributeNode getDimNode;
    @Child private SetDimNamesAttributeNode setDimNamesNode;
    @Child private GetDimNamesAttributeNode getDimNamesNode;

    private final boolean preserveNames;
    private final boolean preserveDimensions;
    private final boolean preserveAttributes;

    /** {@code true} if a cast should try to reuse a non-shared vector. */
    private final boolean reuseNonShared;

    /**
     * GnuR provides several, sometimes incompatible, ways to coerce given value to given type. This
     * flag tells the cast node that it should behave in a way compatible with functions exposed by
     * the native interface.
     */
    private final boolean forRFFI;

    protected CastBaseNode(boolean preserveNames, boolean preserveDimensions, boolean preserveAttributes) {
        this(preserveNames, preserveDimensions, preserveAttributes, false);
    }

    protected CastBaseNode(boolean preserveNames, boolean preserveDimensions, boolean preserveAttributes, boolean forRFFI) {
        this(preserveNames, preserveDimensions, preserveAttributes, forRFFI, false);
    }

    protected CastBaseNode(boolean preserveNames, boolean preserveDimensions, boolean preserveAttributes, boolean forRFFI, boolean reuseNonShared) {
        this.preserveNames = preserveNames;
        this.preserveDimensions = preserveDimensions;
        this.preserveAttributes = preserveAttributes;
        this.forRFFI = forRFFI;
        if (preserveDimensions) {
            getDimNamesNode = GetDimNamesAttributeNode.create();
        }
        this.reuseNonShared = reuseNonShared;
        reuseClassProfile = reuseNonShared ? ValueProfile.createClassProfile() : null;
    }

    public final boolean preserveNames() {
        return preserveNames;
    }

    public final boolean preserveDimensions() {
        return preserveDimensions;
    }

    public final boolean preserveRegAttributes() {
        return preserveAttributes;
    }

    public final boolean preserveAttributes() {
        return preserveAttributes || preserveNames || preserveDimensions;
    }

    public final boolean reuseNonShared() {
        return reuseNonShared;
    }

    protected abstract RType getTargetType();

    protected RError throwCannotCoerceListError(String type) {
        listCoercionErrorBranch.enter();
        throw error(RError.Message.LIST_COERCION, type);
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

    protected void preserveDimensionNames(RAbstractContainer operand, RAbstractContainer ret) {
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
        return forRFFI ? doOtherRFFI(mappedValue) : doOtherDefault(mappedValue);
    }

    protected Object doOtherDefault(Object mappedValue) {
        if (mappedValue instanceof REnvironment) {
            throw error(RError.Message.ENVIRONMENTS_COERCE);
        } else if (mappedValue instanceof RTypedValue) {
            throw error(RError.Message.CANNOT_COERCE, ((RTypedValue) mappedValue).getRType().getName(), getTargetType().getName());
        } else if (mappedValue instanceof TruffleObject) {
            throw error(RError.Message.CANNOT_COERCE, "truffleobject", getTargetType().getName());
        } else {
            throw RInternalError.shouldNotReachHere("unexpected value of type " + (mappedValue == null ? "null" : mappedValue.getClass()));
        }
    }

    protected Object doOtherRFFI(Object mappedValue) {
        if (mappedValue instanceof RTypedValue) {
            warning(Message.CANNOT_COERCE_RFFI, ((RTypedValue) mappedValue).getRType().getName(), getTargetType().getName());
        } else if (mappedValue instanceof TruffleObject) {
            throw error(RError.Message.CANNOT_COERCE, "truffleobject", getTargetType().getName());
        }
        return RNull.instance;
    }

    protected boolean isReusable(RAbstractVector v) {
        return reuseNonShared;
    }

    protected RAbstractVector castWithReuse(RType targetType, RAbstractVector v, ConditionProfile naProfile) {
        assert isReusable(v);
        return reuseClassProfile.profile(v.castSafe(targetType, naProfile, preserveAttributes()));
    }
}
