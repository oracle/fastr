package com.oracle.truffle.r.runtime.env.frame;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.runtime.*;

/**
 * This is meant to monitor updates performed on {@link FrameSlot}. Each {@link FrameSlot} holds an
 * {@link Assumption} in it's "info" field. This assumption is valid as long as no non-local update
 * has ever taken place. interested (e.g., Promises optimizations), it provides an
 * {@link Assumption} on whether a value has changed since they last checked.
 *
 * @see #invalidate(Object)
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
     * @see #invalidate(Object)
     */
    public static void invalidate(FrameSlot slot) {
        invalidate(slot.getInfo());
    }

    public static void checkAndUpdate(FrameSlot slot) {
        invalidate(slot);
    }

    /**
     * Assumes info to be an {@link Assumption} attached to a {@link FrameSlot} and invalidates it
     *
     * @param info Assumed to be an Assumption, throws an {@link RInternalError} otherwise
     */
    public static void invalidate(Object info) {
        if (!(info instanceof Assumption)) {
            throw RInternalError.shouldNotReachHere("Each FrameSlot should hold an Assumption in it's info field!");
        }
        Assumption notChangedLocally = (Assumption) info;
        notChangedLocally.invalidate();
    }
}
