package com.oracle.truffle.r.runtime.env.frame;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.SlowPath;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.frame.FrameInstance.*;
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
        doInvalidate(slot);
    }

    /**
     * Assumes info to be an {@link Assumption} attached to a {@link FrameSlot} and invalidates it
     *
     * @param frame
     * @param slot {@link FrameSlot}; its "info" is assumed to be an Assumption, throws an
     *            {@link RInternalError} otherwise
     */
    public static void checkAndInvalidate(Frame frame, FrameSlot slot) {
        doCheckAndInvalidate(RArguments.getDepth(frame), slot);
    }

    @SlowPath
    private static void doCheckAndInvalidate(int depth, FrameSlot slot) {
        VirtualFrame frame = Utils.getActualCurrentFrame(FrameAccess.READ_ONLY);
        if (frame == null || depth == RArguments.getDepth(frame)) {
            return;
        }

        doInvalidate(slot);
    }

    @SlowPath
    private static void doInvalidate(FrameSlot slot) {
        Assumption notChangedLocally = getMonitor(slot);
        notChangedLocally.invalidate();
        System.err.println("Invalidated " + RRuntime.toString(slot.getIdentifier()));
    }

    public static void checkAndUpdate(Frame frame, FrameSlot slot) {
        checkAndInvalidate(frame, slot);
    }
}
