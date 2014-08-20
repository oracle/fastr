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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import java.lang.management.*;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

@RBuiltin(name = "proc.time", kind = PRIMITIVE, parameterNames = {})
public abstract class ProcTime extends RBuiltinNode {

    private static String[] NAMES = new String[]{"user.self", "sys.self", "elapsed", "user.child", "sys.child"};

    private static ThreadMXBean bean;
    private static RStringVector RNAMES;

    @Specialization
    protected RDoubleVector procTime() {
        controlVisibility();
        double[] data = new double[5];
        long nowInNanos = RContext.getEngine().elapsedTimeInNanos();
        if (bean == null) {
            bean = ManagementFactory.getThreadMXBean();
        }
        long userTimeInNanos = bean.getCurrentThreadUserTime();
        long sysTimeInNanos = bean.getCurrentThreadCpuTime() - userTimeInNanos;
        data[0] = asDoubleSecs(userTimeInNanos);
        data[1] = asDoubleSecs(sysTimeInNanos);
        data[2] = asDoubleSecs(nowInNanos);
        long[] childTimes = RContext.getEngine().childTimesInNanos();
        boolean na = childTimes[0] < 0 || childTimes[1] < 0;
        boolean complete = na ? RDataFactory.INCOMPLETE_VECTOR : RDataFactory.COMPLETE_VECTOR;
        data[3] = na ? RRuntime.DOUBLE_NA : asDoubleSecs(childTimes[0]);
        data[4] = na ? RRuntime.DOUBLE_NA : asDoubleSecs(childTimes[1]);
        if (RNAMES == null) {
            RNAMES = RDataFactory.createStringVector(NAMES, RDataFactory.COMPLETE_VECTOR);
        }
        return RDataFactory.createDoubleVector(data, complete, RNAMES);
    }

    private static final long T = 1000;

    private static double asDoubleSecs(long tInNanos) {
        long tInMillis = tInNanos / (T * T);
        // round to millis (spec says)
        long rtInMillis = tInMillis < T ? tInMillis : (tInMillis * T) / T;
        return (double) rtInMillis / T;
    }

}
