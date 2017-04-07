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
package com.oracle.truffle.r.nodes.access;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.data.RShareable;
import com.oracle.truffle.r.runtime.env.frame.ActiveBinding;
import com.oracle.truffle.r.runtime.env.frame.FrameSlotChangeMonitor;
import com.oracle.truffle.r.runtime.nodes.RNode;

@NodeChild(value = "rhs", type = RNode.class)
/**
 * Common code/state for all the variants of {@code WriteVariableNode}. At this level, we just have
 * a {@code name} for the variable and expression {@code rhs} to be assigned to.
 *
 * There are no create methods as this class is truly abstract.
 */
abstract class BaseWriteVariableNode extends WriteVariableNode {

    protected BaseWriteVariableNode(String name) {
        super(name);
    }

    private final ConditionProfile isObjectProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile isCurrentProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile isShareableProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile isSharedProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile isSharedPermanent = ConditionProfile.createBinaryProfile();

    private final BranchProfile initialSetKindProfile = BranchProfile.create();

    private final ValueProfile shareableProfile = ValueProfile.createClassProfile();

    /*
     * setting value of the mode parameter to COPY is meant to induce creation of a copy of the RHS;
     * this needed for the implementation of the replacement forms of builtin functions whose last
     * argument can be mutated; for example, in "dimnames(x)<-list(1)", the assigned value list(1)
     * must become list("1"), with the latter value returned as a result of the call; TODO: is there
     * a better way than to eagerly create a copy of RHS? the above, however, is not necessary for
     * vector updates, which never coerces RHS to a different type; in this case we set the mode
     * parameter to INVISIBLE is meant to prevent changing state altogether setting value of the
     * mode parameter to TEMP is meant to modify how the state is changed; this is needed for the
     * replacement forms of vector updates where a vector is assigned to a temporary (visible)
     * variable and then, again, to the original variable (which would cause the vector to be copied
     * each time); (non-Javadoc)
     *
     * @see com.oracle.truffle.r.nodes.access.AbstractWriteVariableNode#shareObjectValue(com.oracle.
     * truffle .api.frame.Frame, com.oracle.truffle.api.frame.FrameSlot, java.lang.Object,
     * com.oracle.truffle.r.nodes.access.AbstractWriteVariableNode.Mode, boolean)
     */
    protected final Object shareObjectValue(Frame frame, FrameSlot frameSlot, Object value, Mode mode, boolean isSuper) {
        CompilerAsserts.compilationConstant(mode);
        CompilerAsserts.compilationConstant(isSuper);
        // for the meaning of INVISIBLE mode see the comment preceding the current method;
        // also change state when assigning to the enclosing frame as there must
        // be a distinction between variables with the same name defined in
        // different scopes, for example to correctly support:
        // x<-1:3; f<-function() { x[2]<-10; x[2]<<-100; x[2]<-1000 }; f()
        // or
        // x<-c(1); f<-function() { x[[1]]<<-x[[1]] + 1; x }; a<-f(); b<-f(); c(a,b)
        if ((mode != Mode.INVISIBLE || isSuper)) {
            if (isShareableProfile.profile(value instanceof RShareable)) {

                // this comparison does not work consistently for boxing objects, so it's important
                // to do the RShareable check first.
                if (isCurrentValue(frame, frameSlot, value)) {
                    return value;
                }
                RShareable rShareable = (RShareable) shareableProfile.profile(value);
                if (mode == Mode.COPY) {
                    return rShareable.copy();
                } else {
                    if (!isSharedPermanent.profile(rShareable.isSharedPermanent())) {
                        if (isSuper) {
                            // if non-local assignment, increment conservatively
                            rShareable.incRefCount();
                        } else if (!isSharedProfile.profile(rShareable.isShared())) {
                            // don't increment if already shared - will not get "unshared" until
                            // this function exits anyway
                            rShareable.incRefCount();
                        }
                    }
                }
            }
        }
        return value;
    }

    private boolean isCurrentValue(Frame frame, FrameSlot frameSlot, Object value) {
        try {
            return isObjectProfile.profile(frame.isObject(frameSlot)) && isCurrentProfile.profile(FrameSlotChangeMonitor.getObject(frameSlot, frame) == value);
        } catch (FrameSlotTypeException ex) {
            throw RInternalError.shouldNotReachHere();
        }
    }

    /*
     * The frame parameters are needed to keep the guards from being considered static.
     */

    protected boolean isLogicalKind(@SuppressWarnings("unused") Frame frame, FrameSlot frameSlot) {
        return isKind(frameSlot, FrameSlotKind.Boolean);
    }

    protected boolean isIntegerKind(@SuppressWarnings("unused") Frame frame, FrameSlot frameSlot) {
        return isKind(frameSlot, FrameSlotKind.Int);
    }

    protected boolean isDoubleKind(@SuppressWarnings("unused") Frame frame, FrameSlot frameSlot) {
        return isKind(frameSlot, FrameSlotKind.Double);
    }

    protected boolean isKind(FrameSlot frameSlot, FrameSlotKind kind) {
        if (frameSlot.getKind() == kind) {
            return true;
        } else {
            initialSetKindProfile.enter();
            return initialSetKind(frameSlot, kind);
        }
    }

    private static boolean initialSetKind(FrameSlot frameSlot, FrameSlotKind kind) {
        if (frameSlot.getKind() == FrameSlotKind.Illegal) {
            frameSlot.setKind(kind);
            return true;
        }
        return false;
    }

    /**
     * Handles an assignment to an active binding.
     *
     * @param execFrame The frame to be used for executing the function associated with the symbol.
     * @param lookupFrame The frame to lookup the symbol (must not be {@code null}).
     * @param value The value to set.
     * @param frameSlot The frame slot of the value.
     * @param invalidateProfile The invalidation profile.
     */
    protected static Object handleActiveBinding(VirtualFrame execFrame, Frame lookupFrame, Object value, FrameSlot frameSlot, BranchProfile invalidateProfile,
                    ConditionProfile isActiveBindingProfile) {
        Object object;
        try {
            object = lookupFrame.getObject(frameSlot);
        } catch (FrameSlotTypeException e) {
            object = null;
        }

        if (isActiveBindingProfile.profile(object != null && ActiveBinding.isActiveBinding(object))) {
            return ((ActiveBinding) object).writeValue(value);
        } else {
            FrameSlotChangeMonitor.setObjectAndInvalidate(lookupFrame, frameSlot, value, false, invalidateProfile);
        }
        return value;
    }
}
