/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.env.frame;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.runtime.*;

/**
 * This is meant to monitor updates performed on {@link FrameSlot}. Each {@link FrameSlot} holds an
 * {@link Assumption} in it's "info" field; it is valid as long as no non-local update has ever
 * taken place.<br/>
 * The background to this rather strange assumption is that non-local reads are very hard to keep
 * track of thanks to R powerful language features. To keep the maintenance for the assumption as
 * cheap as possible, it checks only local reads - which is fast - and does a more costly check on
 * "<<-" but invalidates the assumption as soon as "eval" and the like comes into play.<br/>
 *
 *
 * @see #createMonitor()
 * @see #checkAndInvalidate(Frame, FrameSlot)
 * @see #getMonitor(FrameSlot)
 * @see #isMonitorValid(FrameSlot)
 */
public final class FrameSlotChangeMonitor {
    private static final String ASSUMPTION_NAME = "slot change monitor";

    /**
     * @return An {@link Assumption} for this {@link FrameSlot} not be changed locally.
     */
    public static Assumption createMonitor() {
        return Truffle.getRuntime().createAssumption(ASSUMPTION_NAME);
    }

    /**
     * Retrieves the not-changed-locally {@link Assumption} in the {@link FrameSlot#getInfo()}
     * field.
     *
     * @param slot
     * @return The "not changed locally" assumption of the given {@link FrameSlot}
     *
     * @see FrameSlotChangeMonitor
     */
    public static Assumption getMonitor(FrameSlot slot) {
        Object info = slot.getInfo();
        if (!(info instanceof Assumption)) {
            CompilerDirectives.transferToInterpreter();
            throw RInternalError.shouldNotReachHere("Each FrameSlot should hold an Assumption in it's info field!");
        }
        return (Assumption) info;
    }

    /**
     * Convenience method for {@link #getMonitor(FrameSlot)} and {@link Assumption#isValid()}.
     *
     * @param slot
     * @return Directly returns whether a {@link FrameSlot} has ever been updated from a non-local
     *         frame.
     */
    public static boolean isMonitorValid(FrameSlot slot) {
        return getMonitor(slot).isValid();
    }

    /**
     * Invalidates the not-changed-locally {@link Assumption} of the given {@link FrameSlot}.
     *
     * @param slot
     */
    public static void invalidate(FrameSlot slot) {
        Assumption notChangedLocally = getMonitor(slot);
        notChangedLocally.invalidate();
    }

    /**
     * Checks if the assumption of the given {@link FrameSlot} has to be invalidated.
     *
     * @param curFrame
     * @param slot {@link FrameSlot}; its "info" is assumed to be an Assumption, throws an
     *            {@link RInternalError} otherwise
     * @param invalidateProfile Used to guard the invalidation code.
     */
    public static void checkAndInvalidate(Frame curFrame, FrameSlot slot, BranchProfile invalidateProfile) {
        assert curFrame.getFrameDescriptor() == slot.getFrameDescriptor();

        // Check whether current frame is used outside a regular stack
        if (RArguments.getIsIrregular(curFrame)) {
            // False positive: Also invalidates a slot in the current active frame if that one is
            // used inside eval or the like, but this cost is definitely neglectable.
            invalidateProfile.enter();
            getMonitor(slot).invalidate();
        }
    }

    /**
     * Checks if the assumption of the given {@link FrameSlot} has to be invalidated.
     *
     * @param curFrame
     * @param slot {@link FrameSlot}; its "info" is assumed to be an Assumption, throws an
     *            {@link RInternalError} otherwise
     * @param invalidateProfile Used to guard the invalidation code.
     */
    public static void checkAndInvalidate(VirtualFrame curFrame, FrameSlot slot, BranchProfile invalidateProfile) {
        assert curFrame.getFrameDescriptor() == slot.getFrameDescriptor();

        // Check whether current frame is used outside a regular stack
        if (RArguments.getIsIrregular(curFrame)) {
            // False positive: Also invalidates a slot in the current active frame if that one is
            // used inside eval or the like, but this cost is definitely neglectable.
            invalidateProfile.enter();
            getMonitor(slot).invalidate();
        }
    }

    /**
     * Checks if the assumption of the given {@link FrameSlot} has to be invalidated.
     *
     * @param curFrame
     * @param slot {@link FrameSlot}; its "info" is assumed to be an Assumption, throws an
     *            {@link RInternalError} otherwise
     */
    public static void checkAndInvalidate(Frame curFrame, FrameSlot slot) {
        assert curFrame.getFrameDescriptor() == slot.getFrameDescriptor();

        // Check whether current frame is used outside a regular stack
        if (RArguments.getIsIrregular(curFrame)) {
            // False positive: Also invalidates a slot in the current active frame if that one is
            // used inside eval or the like
            getMonitor(slot).invalidate();
        }
    }

    /**
     * Checks if the assumption of the given {@link FrameSlot} has to be invalidated.
     *
     * @param curFrame
     * @param slot {@link FrameSlot}; its "info" is assumed to be an Assumption, throws an
     *            {@link RInternalError} otherwise
     */
    public static void checkAndInvalidate(VirtualFrame curFrame, FrameSlot slot) {
        assert curFrame.getFrameDescriptor() == slot.getFrameDescriptor();

        // Check whether current frame is used outside a regular stack
        if (RArguments.getIsIrregular(curFrame)) {
            // False positive: Also invalidates a slot in the current active frame if that one is
            // used inside eval or the like
            getMonitor(slot).invalidate();
        }
    }

    public static FrameSlot findOrAddFrameSlot(FrameDescriptor fd, Object identifier) {
        return fd.findOrAddFrameSlot(identifier, createMonitor(), FrameSlotKind.Illegal);
    }

    public static FrameSlot findOrAddFrameSlot(FrameDescriptor fd, Object identifier, FrameSlotKind initialKind) {
        return fd.findOrAddFrameSlot(identifier, createMonitor(), initialKind);
    }
}
