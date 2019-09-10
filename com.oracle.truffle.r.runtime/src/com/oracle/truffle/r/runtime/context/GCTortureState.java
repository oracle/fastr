/*
 * Copyright (c) 2019, 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.context;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;

/**
 * Encapsulates the state of whether GC torture is turned on/off. When GC torture is on, FastR (and
 * GNU-R) explicitly runs GC at points where GC protection issue may happen. It can be turned on the
 * same way as in GNU-R via dedicated R builtins or environment variable {@code R_GCTORTURE=steps}.
 */
public final class GCTortureState implements RContext.ContextState {
    private boolean on;
    private Assumption gcTortureOffAssumption = Truffle.getRuntime().createAssumption("GC Torture Off");

    private int stepThreshold;
    private int stepCounter;

    private GCTortureState() {
    }

    public static GCTortureState newContextState() {
        return new GCTortureState();
    }

    @Override
    public RContext.ContextState initialize(RContext context) {
        // R_GCTORTURE is also used by GNU-R with the same meaning
        String tortureEnvVar = System.getenv("R_GCTORTURE");
        if (tortureEnvVar == null) {
            tortureEnvVar = System.getenv("FASTR_GCTORTURE");
        }
        if (tortureEnvVar != null) {
            int steps = parsePositiveInt(tortureEnvVar);
            if (steps > 0) {
                gcTortureOffAssumption.invalidate("From env variable");
                this.on(steps);
            }
        }
        return this;
    }

    public int getSteps() {
        return stepThreshold;
    }

    public boolean isOn() {
        return on;
    }

    public void on() {
        gcTortureOffAssumption.invalidate();
        this.on = true;
        stepThreshold = 0;
        stepCounter = 0;
    }

    public void on(int step) {
        this.on();
        stepThreshold = step;
    }

    public void off() {
        this.on = false;
    }

    public void runGC() {
        if (gcTortureOffAssumption.isValid() || !on) {
            return;
        }
        doRunGC();
    }

    @TruffleBoundary
    private void doRunGC() {
        stepCounter++;
        if (stepThreshold != 0 && stepCounter < stepThreshold) {
            // run GC only every stepThreshold-th step
            return;
        }
        stepCounter = 0;
        System.gc();
    }

    private static int parsePositiveInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return -1;
        }
    }
}
