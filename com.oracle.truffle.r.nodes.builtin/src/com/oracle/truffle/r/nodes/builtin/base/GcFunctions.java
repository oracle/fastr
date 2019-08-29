/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;
import static com.oracle.truffle.r.runtime.RVisibility.OFF;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.COMPLEX;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import java.util.Arrays;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.context.FastROptions;
import com.oracle.truffle.r.runtime.context.GCTortureState;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;

/**
 * Implementation of GC related builtins.
 */

public final class GcFunctions {

    @RBuiltin(name = "gc", kind = INTERNAL, parameterNames = {"verbose", "reset", "full"}, behavior = COMPLEX)
    public abstract static class Gc extends RBuiltinNode.Arg3 {

        static {
            Casts casts = new Casts(Gc.class);
            casts.arg("verbose").asLogicalVector().findFirst().map(toBoolean());
            casts.arg("reset").asLogicalVector().findFirst().map(toBoolean());
            casts.arg("full").asLogicalVector().findFirst().map(toBoolean());
        }

        @SuppressWarnings("unused")
        @Specialization
        protected RDoubleVector gc(boolean verbose, boolean reset, boolean full,
                        @Cached BranchProfile doRunGCProfile) {
            /*
             * It is rarely advisable to actually force a gc in Java, therefore we simply ignore
             * this builtin unless explicitly specified.
             */
            RContext ctx = RContext.getInstance();
            if (ctx.getOption(FastROptions.EnableExplicitGC)) {
                doRunGCProfile.enter();
                doRunGC();
            }
            // produce at-least similarly shaped data:
            double[] data = new double[14];
            Arrays.fill(data, RRuntime.DOUBLE_NA);
            return RDataFactory.createDoubleVector(data, RDataFactory.INCOMPLETE_VECTOR);
        }

        @TruffleBoundary
        private static void doRunGC() {
            System.gc();
        }
    }

    @RBuiltin(name = "gctorture", visibility = OFF, kind = INTERNAL, parameterNames = "on", behavior = PURE)
    public abstract static class Gctorture extends RBuiltinNode.Arg1 {

        static {
            Casts casts = new Casts(Gctorture.class);
            casts.arg("on").allowNull().asLogicalVector().findFirst().map(toBoolean());
        }

        @TruffleBoundary
        @Specialization
        protected byte gctorture(boolean on) {
            GCTortureState gcTorture = RContext.getInstance().gcTorture;
            boolean result = gcTorture.isOn();
            if (on) {
                gcTorture.on();
            } else {
                gcTorture.off();
            }
            return RRuntime.asLogical(result);
        }
    }

    @RBuiltin(name = "gctorture2", kind = INTERNAL, parameterNames = {"step", "wait", "inhibit_release"}, behavior = PURE)
    public abstract static class Gctorture2 extends RBuiltinNode.Arg3 {

        static {
            Casts casts = new Casts(Gctorture2.class);
            casts.arg("step").allowNull().asIntegerVector().findFirst();
            casts.arg("inhibit_release").allowNull().asLogicalVector().findFirst().map(toBoolean());
        }

        @TruffleBoundary
        @Specialization
        protected Object gctorture2(int step, @SuppressWarnings("unused") Object wait, @SuppressWarnings("unused") Object inhibitRelease) {
            GCTortureState gcTorture = RContext.getInstance().gcTorture;
            int previous = gcTorture.getSteps();
            if (step != 0) {
                gcTorture.on(step);
            } else {
                gcTorture.off();
            }
            return previous;
        }
    }

}
