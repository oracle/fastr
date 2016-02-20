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
package com.oracle.truffle.r.nodes.access;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.profiles.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.nodes.*;

@NodeChild(value = "rhs", type = RNode.class)
@NodeFields({@NodeField(name = "name", type = Object.class)})
/**
 * Common code/state for all the variants of {@code WriteVariableNode}.
 * At this level, we just have a {@code name} for the variable and expression {@code rhs} to be assigned to.
 *
 * There are no create methods as this class is truly abstract.
 */
abstract class BaseWriteVariableNode extends WriteVariableNode {

    private final ConditionProfile isCurrentProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile isShareableProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile isTemporaryProfile = FastROptions.NewStateTransition.getBooleanValue() ? null : ConditionProfile.createBinaryProfile();
    private final ConditionProfile isSharedProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile isRefCountUpdateable = FastROptions.NewStateTransition.getBooleanValue() ? ConditionProfile.createBinaryProfile() : null;

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
     * @see
     * com.oracle.truffle.r.nodes.access.AbstractWriteVariableNode#shareObjectValue(com.oracle.truffle
     * .api.frame.Frame, com.oracle.truffle.api.frame.FrameSlot, java.lang.Object,
     * com.oracle.truffle.r.nodes.access.AbstractWriteVariableNode.Mode, boolean)
     */
    protected final Object shareObjectValue(Frame frame, FrameSlot frameSlot, Object value, Mode mode, boolean isSuper) {
        CompilerAsserts.compilationConstant(mode);
        CompilerAsserts.compilationConstant(isSuper);
        Object newValue = value;
        // for the meaning of INVISIBLE mode see the comment preceding the current method;
        // also change state when assigning to the enclosing frame as there must
        // be a distinction between variables with the same name defined in
        // different scopes, for example to correctly support:
        // x<-1:3; f<-function() { x[2]<-10; x[2]<<-100; x[2]<-1000 }; f()
        // or
        // x<-c(1); f<-function() { x[[1]]<<-x[[1]] + 1; x }; a<-f(); b<-f(); c(a,b)
        if ((mode != Mode.INVISIBLE || isSuper) && !isCurrentProfile.profile(isCurrentValue(frame, frameSlot, value))) {
            if (isShareableProfile.profile(value instanceof RShareable)) {
                RShareable rShareable = (RShareable) shareableProfile.profile(value);
                if (mode == Mode.COPY) {
                    newValue = rShareable.copy();
                } else {
                    if (FastROptions.NewStateTransition.getBooleanValue()) {
                        if (isRefCountUpdateable.profile(!rShareable.isSharedPermanent())) {
                            if (isSuper) {
                                // if non-local assignment, increment conservatively
                                rShareable.incRefCount();
                            } else if (isSharedProfile.profile(!rShareable.isShared())) {
                                // don't increment if already shared - will not get "unshared" until
                                // this function exits anyway
                                rShareable.incRefCount();
                            }
                        }
                    } else {
                        if (isTemporaryProfile.profile(rShareable.isTemporary())) {
                            rShareable.markNonTemporary();
                        } else if (isSharedProfile.profile(rShareable.isShared())) {
                            rShareable.markNonTemporary();
                        } else {
                            rShareable.makeShared();
                        }
                    }
                }
            }
        }
        return newValue;
    }

    protected static boolean isCurrentValue(Frame frame, FrameSlot frameSlot, Object value) {
        try {
            return frame.isObject(frameSlot) && frame.getObject(frameSlot) == value;
        } catch (FrameSlotTypeException ex) {
            throw RInternalError.shouldNotReachHere();
        }
    }

    @SuppressWarnings("unused")
    protected boolean isLogicalKind(VirtualFrame frame, FrameSlot frameSlot) {
        return isKind(frameSlot, FrameSlotKind.Boolean);
    }

    @SuppressWarnings("unused")
    protected boolean isIntegerKind(VirtualFrame frame, FrameSlot frameSlot) {
        return isKind(frameSlot, FrameSlotKind.Int);
    }

    @SuppressWarnings("unused")
    protected boolean isDoubleKind(VirtualFrame frame, FrameSlot frameSlot) {
        return isKind(frameSlot, FrameSlotKind.Double);
    }

    private boolean isKind(FrameSlot frameSlot, FrameSlotKind kind) {
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
}
