/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.function;

import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.runtime.data.*;

/**
 * This class implements the behavior for {@link RMissing} which is needed inside this module, as it
 * would induce unnecessary dependencies otherwise.
 */
public class RMissingHelper {

    /**
     * This function determines, of an arguments value - given as 'value' - is missing. An argument
     * is missing, when it has not been provided to the current function call, OR if the value that
     * has been provided once was a missing argument. (cp. R language definition and Internals 1.5.1
     * Missingness).
     *
     * @param frame The frame in which to decide whether value is missing or not
     * @param value The value that should be examined
     * @return <code>true</code> iff this value is 'missing' in the definition of R
     */
    public static boolean isMissing(VirtualFrame frame, Object value) {
        if (value == RMissing.instance) {
            return true;
        }

        // This might be a promise...
        if (value instanceof RPromise) {
            return isMissing(frame, (RPromise) value);
        }
        return false;
    }

    /**
     * @param frame The frame in which to decide whether value is missing or not
     * @param promise The {@link RPromise} that might evaluate to be 'missing'
     * @return See {@link #isMissing(VirtualFrame, Object)}
     */
    public static boolean isMissing(VirtualFrame frame, RPromise promise) {
        // Represents a missing argument itself?
        if (promise.isDefaulted() || isMissingSymbol(promise)) {
            return true;
        }

        return false;
    }

    /**
     * This method determines whether a given {@link Symbol} is missing in the given frame. This is
     * used to determine whether an argument has to be replaced by its default value or not. In case
     * the given {@link Symbol} is associated with a promise, {@link #isMissingSymbol(RPromise)} is
     * called.
     *
     * @param frame The frame in which to decide whether value is missing or not
     * @param symbol The {@link Symbol} which to check
     * @return See {@link #isMissingSymbol(RPromise)}
     */
    public static boolean isMissingArgument(Frame frame, Symbol symbol) {
        // TODO IsDotDotSymbol: Anything special to do here?

        // Check symbols value
        Object value = getMissingValue(frame, symbol);
        if (value == RMissing.instance) {
            return true;
        }

        // Check for Promise
        if (value instanceof RPromise) {
            RPromise promise = (RPromise) value;
            return promise.isDefaulted() || isMissingSymbol(promise);
        }

        return false;
    }

    public static Object getMissingValue(Frame frame, Symbol symbol) {
        // Check binding
        FrameSlot frameSlot = frame.getFrameDescriptor().findFrameSlot(symbol.getName());
        if (frameSlot == null) {
            return null;
        }

        // Check symbols value
        try {
            return frame.getObject(frameSlot);
        } catch (FrameSlotTypeException e) {
            return null;
        }
    }

    /**
     * @param promise The {@link RPromise} which is checked whether it contains a
     *            {@link #isMissingArgument(Frame, Symbol)}.
     * @return Whether the given {@link RPromise} represents a symbol that is 'missing' in its frame
     */
    public static boolean isMissingSymbol(RPromise promise) {
        boolean result = false;
        // Missing RPromises throw an error on evaluation, so this might only be checked if it has
        // not been evaluated yet. (My guess, true?)
        if (!promise.isEvaluated()) {
            Object exprObj = promise.getRep();

            // Unfold WrapArgumentNode
            if (exprObj instanceof WrapArgumentNode) {
                exprObj = ((WrapArgumentNode) exprObj).getOperand();
            }

            // Check for symbol (ReadVariableNode)
            if (exprObj instanceof ReadVariableNode) {
                ReadVariableNode rvn = (ReadVariableNode) exprObj;
                Symbol symbol = rvn.getSymbol();

                // Check: If there is a cycle, return true. (This is done like in GNU R)
                if (promise.isUnderEvaluation()) {
                    return true;
                }
                try {
                    promise.setUnderEvaluation(true);
                    result = isMissingArgument(promise.getEnv().getFrame(), symbol);
                } finally {
                    promise.setUnderEvaluation(false);
                }
            }
        }
        return result;
    }
}
