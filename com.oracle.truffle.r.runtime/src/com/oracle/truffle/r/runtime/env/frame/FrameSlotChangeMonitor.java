package com.oracle.truffle.r.runtime.env.frame;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.frame.*;

/**
 * This is meant to monitor updates performed on its associated {@link FrameSlot}. For those who are
 * interested (e.g., Promises optimizations), it provides an {@link Assumption} on whether a value
 * has changed since they last checked.
 *
 * @see #observeSlot()
 * @see #invalidate()
 */
public class FrameSlotChangeMonitor {
    private static final String ASSUMPTION_NAME = new String("slot change monitor");

    private Assumption slotNotChanged = null;

    /**
     * @return An {@link Assumption} that can tell whether the associated {@link FrameSlot} had
     *         changed since the call to this method.
     */
    public Assumption observeSlot() {
        if (slotNotChanged == null) {
            slotNotChanged = Truffle.getRuntime().createAssumption(ASSUMPTION_NAME);
        }
        return slotNotChanged;
    }

    /**
     * Has to be called on every update of the {@link FrameSlot} this {@link FrameSlotChangeMonitor}
     * belongs to.
     */
    public void invalidate() {
        if (slotNotChanged != null) {
            slotNotChanged.invalidate();
            slotNotChanged = null;
        }
    }
}
