/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.unary;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetDimAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetDimNamesAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetNamesAttributeNode;
import com.oracle.truffle.r.runtime.NullProfile;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.ErrorContext;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.RBaseObject;
import com.oracle.truffle.r.runtime.data.RDataFactory.VectorFactory;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

public abstract class CastBaseNode extends CastNode {

    private final BranchProfile listCoercionErrorBranch = BranchProfile.create();
    private final NullProfile hasDimensionsProfile = NullProfile.create();
    private final NullProfile hasNamesProfile = NullProfile.create();
    private final NullProfile hasDimNamesProfile = NullProfile.create();
    private final ValueProfile reuseClassProfile;

    @Child private GetNamesAttributeNode getNamesNode = GetNamesAttributeNode.create();
    @Child private GetDimAttributeNode getDimNode;
    @Child private GetDimNamesAttributeNode getDimNamesNode;

    @Child private VectorFactory factory;

    private final boolean preserveNames;
    private final boolean preserveDimensions;
    private final boolean preserveAttributes;

    /** {@code true} if a cast should wrap the input in a closure. */
    private final boolean useClosure;

    /**
     * GnuR provides several, sometimes incompatible, ways to coerce given value to given type. This
     * flag tells the cast node that it should behave in a way compatible with functions exposed by
     * the native interface.
     */
    private final boolean forRFFI;

    /**
     * Context to be used in warnings.
     */
    private final ErrorContext warningContext;

    protected CastBaseNode(boolean preserveNames, boolean preserveDimensions, boolean preserveAttributes) {
        this(preserveNames, preserveDimensions, preserveAttributes, false);
    }

    protected CastBaseNode(boolean preserveNames, boolean preserveDimensions, boolean preserveAttributes, boolean forRFFI) {
        this(preserveNames, preserveDimensions, preserveAttributes, forRFFI, false, null);
    }

    protected CastBaseNode(boolean preserveNames, boolean preserveDimensions, boolean preserveAttributes, boolean forRFFI, boolean useClosure) {
        this(preserveNames, preserveDimensions, preserveAttributes, forRFFI, useClosure, null);
    }

    protected CastBaseNode(boolean preserveNames, boolean preserveDimensions, boolean preserveAttributes, boolean forRFFI, boolean useClosure, ErrorContext warningContext) {
        this.preserveNames = preserveNames;
        this.preserveDimensions = preserveDimensions;
        this.preserveAttributes = preserveAttributes;
        this.forRFFI = forRFFI;
        this.useClosure = useClosure;
        this.warningContext = warningContext;
        reuseClassProfile = useClosure ? ValueProfile.createClassProfile() : null;
    }

    protected final VectorFactory factory() {
        if (factory == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            factory = insert(VectorFactory.create());
        }
        return factory;
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
        return useClosure;
    }

    public final ErrorContext warningContext() {
        return warningContext;
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
            if (getNamesNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getNamesNode = insert(GetNamesAttributeNode.create());
            }
            return hasNamesProfile.profile(getNamesNode.getNames(operand));
        } else {
            return null;
        }
    }

    protected RList getPreservedDimNames(RAbstractContainer operand) {
        if (preserveDimensions()) {
            if (getDimNamesNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getDimNamesNode = insert(GetDimNamesAttributeNode.create());
            }
            return hasDimNamesProfile.profile(getDimNamesNode.getDimNames(operand));
        } else {
            return null;
        }
    }

    @Fallback
    @TruffleBoundary
    protected Object doOther(Object value) {
        Object mappedValue = RRuntime.asAbstractVector(value);
        if (forRFFI && mappedValue instanceof RBaseObject) {
            return doOtherRFFI(mappedValue);
        }
        throw error(Message.CANNOT_COERCE, RRuntime.getRTypeName(mappedValue), getTargetType().getName());
    }

    protected Object doOtherRFFI(Object mappedValue) {
        warning(Message.CANNOT_COERCE_RFFI, ((RBaseObject) mappedValue).getRType().getName(), getTargetType().getName());
        return RNull.instance;
    }

    protected boolean useClosure() {
        return useClosure;
    }

    protected RAbstractVector castWithReuse(RType targetType, RAbstractVector v, ConditionProfile naProfile) {
        assert useClosure();
        return reuseClassProfile.profile(v.castSafe(targetType, naProfile, preserveAttributes()));
    }
}
