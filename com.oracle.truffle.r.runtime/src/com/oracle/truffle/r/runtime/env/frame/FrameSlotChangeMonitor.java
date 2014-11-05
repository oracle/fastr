package com.oracle.truffle.r.runtime.env.frame;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.runtime.*;

/**
 * This is meant to monitor updates performed on {@link FrameSlot}. Each {@link FrameSlot} holds an
 * {@link Assumption} in it's "info" field. This assumption is valid as long as no non-local update
 * has ever taken place. TODO Extend
 *
 * @see #createMonitor()
 * @see #checkAndInvalidate(Frame, FrameSlot)
 * @see #getMonitor(FrameSlot)
 * @see #isMonitorValid(FrameSlot)
 */
public final class FrameSlotChangeMonitor {
    private static final String ASSUMPTION_NAME = "slot change monitor";

    /**
     * @return TODO
     */
    public static Assumption createMonitor() {
        return Truffle.getRuntime().createAssumption(ASSUMPTION_NAME);
    }

    /**
     * TODO
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
     * Convenience method for {@link #getMonitor(FrameSlot)} and {@link Assumption#isValid()}
     *
     * @param slot
     * @return Directly returns whether a {@link FrameSlot} has ever been updated from a non-local
     *         frame.
     */
    public static boolean isMonitorValid(FrameSlot slot) {
        return getMonitor(slot).isValid();
    }

    public static void invalidate(FrameSlot slot) {
        Assumption notChangedLocally = getMonitor(slot);
        notChangedLocally.invalidate();
    }

    /**
     * Assumes info to be an {@link Assumption} attached to a {@link FrameSlot} and invalidates it
     *
     * @param curFrame
     * @param slot {@link FrameSlot}; its "info" is assumed to be an Assumption, throws an
     *            {@link RInternalError} otherwise
     */
    public static void checkAndInvalidate(Frame curFrame, FrameSlot slot, BranchProfile invalidateProfile) {
        assert curFrame.getFrameDescriptor() == slot.getFrameDescriptor();

        // Check whether current frame is used outside a regular stack
        if (RArguments.getIsIrregular(curFrame)) {
            // False positive: Also invalidates a slot in the current active frame if that one is
            // used inside eval or the like
            invalidateProfile.enter();
            getMonitor(slot).invalidate();
        }
    }

    public static void checkAndInvalidate(Frame curFrame, FrameSlot slot) {
        assert curFrame.getFrameDescriptor() == slot.getFrameDescriptor();

        // Check whether current frame is used outside a regular stack
        if (RArguments.getIsIrregular(curFrame)) {
            // False positive: Also invalidates a slot in the current active frame if that one is
            // used inside eval or the like
            getMonitor(slot).invalidate();
        }
    }

// public static void checkAndInvalidate(Frame curFrame, FrameSlot slot, BranchProfile
// invalidateProfile) {
// // if (!isMonitorValid(slot)) {
// // // For performance reasons: Avoid Utils.getActualCurrentFrame if not absolutely
// // // necessary
// // return;
// // }
// //
// // Fast check
// if (checkLightAndInvalidate(curFrame, slot)) {
// return;
// }
//
// // int depth = RArguments.getDepth(curFrame);
// // Frame topFrame = Utils.getActualCurrentFrame(FrameAccess.READ_ONLY);
// // // TODO Current stackDepth implementation creates false negatives!
// // if (topFrame == null || depth == RArguments.getDepth(topFrame)) {
// // return;
// // }
//
// invalidateProfile.enter();
// invalidate(slot);
// }
}
