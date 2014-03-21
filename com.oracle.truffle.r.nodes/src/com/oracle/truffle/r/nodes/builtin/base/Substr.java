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

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.ops.na.*;

@RBuiltin("substr")
public abstract class Substr extends RBuiltinNode {

    protected final NACheck na = NACheck.create();

    @CompilerDirectives.CompilationFinal protected boolean everSeenIllegalRange = false;

    @Child protected CastStringNode castString;

    protected static boolean rangeOk(String x, int start, int stop) {
        return start <= stop && start > 0 && stop > 0 && start <= x.length() && stop <= x.length();
    }

    protected String substr0(String x, int start, int stop) {
        na.enable(true);
        if (na.check(x)) {
            return RRuntime.STRING_NA;
        }
        int actualStart = start;
        int actualStop = stop;
        if (!rangeOk(x, start, stop)) {
            if (!everSeenIllegalRange) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                everSeenIllegalRange = true;
            }
            if (start > stop || (start <= 0 && stop <= 0) || (start > x.length() && stop > x.length())) {
                return "";
            }
            if (start <= 0) {
                actualStart = 1;
            }
            if (stop > x.length()) {
                actualStop = x.length();
            }
        }
        return x.substring(actualStart - 1, actualStop);
    }

    @Specialization(order = 10)
    public String substr(String x, int start, int stop) {
        return substr0(x, start, stop);
    }

    @Specialization(order = 11)
    public String substr(String x, double start, double stop) {
        na.enable(true);
        if (na.check(start) || na.check(stop)) {
            return RRuntime.STRING_NA;
        }
        return substr0(x, (int) start, (int) stop);
    }

    @Specialization(order = 100)
    public String substr(byte x, double start, double stop) {
        na.enable(true);
        if (na.check(x)) {
            return RRuntime.STRING_NA;
        }
        return substr0(RRuntime.logicalToString(x), (int) start, (int) stop);
    }

    @Specialization(order = 101)
    public String substr(String x, byte start, double stop) {
        na.enable(true);
        if (na.check(start)) {
            return RRuntime.STRING_NA;
        }
        return substr0(x, start, (int) stop);
    }

    @Specialization(order = 102)
    public String substr(String x, double start, byte stop) {
        na.enable(true);
        if (na.check(stop)) {
            return RRuntime.STRING_NA;
        }
        return substr0(x, (int) start, stop);
    }

    @Specialization(order = 103)
    public String substr(String x, RDoubleVector start, RIntVector stop) {
        return substr(x, start.getDataAt(0), stop.getDataAt(0));
    }

    protected RStringVector substr0(RAbstractStringVector x, int start, int stop) {
        String[] res = new String[x.getLength()];
        for (int i = 0; i < x.getLength(); ++i) {
            res[i] = substr0(x.getDataAt(i), start, stop);
        }
        return RDataFactory.createStringVector(res, na.neverSeenNA());
    }

    @Specialization(order = 50)
    public RStringVector substr(RAbstractStringVector x, int start, int stop) {
        return substr0(x, start, stop);
    }

    @Specialization(order = 53)
    public RStringVector substr(RAbstractStringVector x, int start, RIntVector stop) {
        String[] res = new String[x.getLength()];
        for (int i = 0, j = 0; i < x.getLength(); ++i, j = Utils.incMod(j, stop.getLength())) {
            res[i] = substr0(x.getDataAt(i), start, stop.getDataAt(j));
        }
        return RDataFactory.createStringVector(res, na.neverSeenNA());
    }

    @Specialization(order = 61)
    public RStringVector substr(RAbstractStringVector x, double start, double stop) {
        return substr0(x, (int) start, (int) stop);
    }

    @Specialization(order = 62)
    public RStringVector substr(RAbstractStringVector x, RDoubleVector start, double stop) {
        String[] res = new String[x.getLength()];
        for (int i = 0, j = 0; i < x.getLength(); ++i, j = Utils.incMod(j, start.getLength())) {
            res[i] = substr0(x.getDataAt(i), (int) start.getDataAt(j), (int) stop);
        }
        return RDataFactory.createStringVector(res, na.neverSeenNA());
    }

    @Specialization(order = 63)
    public RStringVector substr(RAbstractStringVector x, double start, RDoubleVector stop) {
        String[] res = new String[x.getLength()];
        for (int i = 0, j = 0; i < x.getLength(); ++i, j = Utils.incMod(j, stop.getLength())) {
            res[i] = substr0(x.getDataAt(i), (int) start, (int) stop.getDataAt(j));
        }
        return RDataFactory.createStringVector(res, na.neverSeenNA());
    }

    @Specialization(order = 64)
    public RStringVector substr(RAbstractStringVector x, double start, RIntVector stop) {
        String[] res = new String[x.getLength()];
        for (int i = 0, j = 0; i < x.getLength(); ++i, j = Utils.incMod(j, stop.getLength())) {
            res[i] = substr0(x.getDataAt(i), (int) start, stop.getDataAt(j));
        }
        return RDataFactory.createStringVector(res, na.neverSeenNA());
    }

    @Specialization(order = 65)
    public RStringVector substr(RAbstractStringVector x, RDoubleVector start, RDoubleVector stop) {
        String[] res = new String[x.getLength()];
        for (int i = 0, j = 0, k = 0; i < x.getLength(); ++i, j = Utils.incMod(j, start.getLength()), k = Utils.incMod(k, stop.getLength())) {
            res[i] = substr0(x.getDataAt(i), (int) start.getDataAt(j), (int) stop.getDataAt(k));
        }
        return RDataFactory.createStringVector(res, na.neverSeenNA());
    }

    @Specialization(order = 500, guards = "!isStringVector")
    public String substr(VirtualFrame frame, Object x, double start, double stop) {
        ensureCast();
        return substr((String) castString.executeCast(frame, x), start, stop);
    }

    @Specialization(order = 501, guards = "!isStringVector")
    public String substr(VirtualFrame frame, Object x, int start, int stop) {
        ensureCast();
        return substr((String) castString.executeCast(frame, x), start, stop);
    }

    @Specialization(order = 502, guards = "!isStringVector")
    public String substr(VirtualFrame frame, Object x, double start, int stop) {
        ensureCast();
        return substr((String) castString.executeCast(frame, x), start, stop);
    }

    @Specialization(order = 503, guards = "!isStringVector")
    public String substr(VirtualFrame frame, Object x, int start, double stop) {
        ensureCast();
        return substr((String) castString.executeCast(frame, x), start, stop);
    }

    protected boolean isStringVector(Object x) {
        return x instanceof RAbstractStringVector;
    }

    private void ensureCast() {
        if (castString == null) {
            CompilerDirectives.transferToInterpreter();
            castString = insert(CastStringNodeFactory.create(null, false, true, false));
        }
    }
}
