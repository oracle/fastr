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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import com.oracle.truffle.api.CompilerDirectives.SlowPath;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.ops.na.*;

@RBuiltin(name = "substr<-", kind = INTERNAL)
public abstract class UpdateSubstr extends RBuiltinNode {

    protected final NACheck na = NACheck.create();

    private BranchProfile everSeenIllegalRange = new BranchProfile();

    protected static boolean rangeOk(String x, int start, int stop) {
        return start <= stop && start > 0 && stop > 0 && start <= x.length() && stop <= x.length();
    }

    @SlowPath
    private static String replaceSubstring(String x, String value, int actualStart, int replacementLength, int actualStop) {
        return x.substring(0, actualStart - 1) + value.substring(0, replacementLength) + x.substring(actualStop, x.length());
    }

    protected String substr0(String x, int start, int stop, String value) {
        na.enable(true);
        if (na.check(x) || na.check(start) || na.check(stop)) {
            return RRuntime.STRING_NA;
        }
        int actualStart = start;
        int actualStop = stop;
        if (!rangeOk(x, start, stop)) {
            everSeenIllegalRange.enter();
            if (start > stop || (start <= 0 && stop <= 0) || (start > x.length() && stop > x.length())) {
                return x;
            }
            if (start <= 0) {
                actualStart = 1;
            }
            if (stop > x.length()) {
                actualStop = x.length();
            }
        }
        int replacementLength = actualStop - (actualStart - 1);
        if (replacementLength > value.length()) {
            actualStop -= (replacementLength - value.length());
            replacementLength = value.length();
        }
        return replaceSubstring(x, value, actualStart, replacementLength, actualStop);
    }

    @SuppressWarnings("unused")
    @Specialization(order = 1, guards = "emptyArg")
    public RStringVector substrEmptyArg(RAbstractStringVector arg, RAbstractIntVector start, RAbstractIntVector stop, Object value) {
        return RDataFactory.createEmptyStringVector();
    }

    @SuppressWarnings("unused")
    @Specialization(order = 2, guards = {"!emptyArg", "wrongParams"})
    public RNull substrWrongParams(RAbstractStringVector arg, RAbstractIntVector start, RAbstractIntVector stop, Object value) {
        assert false; // should never happen
        return RNull.instance; // dummy
    }

    @SuppressWarnings("unused")
    @Specialization(order = 10, guards = {"!emptyArg", "!wrongParams"})
    public RStringVector substr(RAbstractStringVector arg, RAbstractIntVector start, RAbstractIntVector stop, RNull value) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_UNNAMED_VALUE);
    }

    @SuppressWarnings("unused")
    @Specialization(order = 11, guards = {"!emptyArg", "!wrongParams", "wrongValue"})
    public RStringVector substr(RAbstractStringVector arg, RAbstractIntVector start, RAbstractIntVector stop, RAbstractVector value) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_UNNAMED_VALUE);
    }

    @Specialization(order = 12, guards = {"!emptyArg", "!wrongParams", "!wrongValue"})
    public RStringVector substr(RAbstractStringVector arg, RAbstractIntVector start, RAbstractIntVector stop, RAbstractStringVector value) {
        String[] res = new String[arg.getLength()];
        for (int i = 0, j = 0, k = 0, l = 0; i < arg.getLength(); ++i, j = Utils.incMod(j, start.getLength()), k = Utils.incMod(k, stop.getLength()), l = Utils.incMod(k, value.getLength())) {
            res[i] = substr0(arg.getDataAt(i), start.getDataAt(j), stop.getDataAt(k), value.getDataAt(l));
        }
        return RDataFactory.createStringVector(res, na.neverSeenNA());

    }

    @SuppressWarnings("unused")
    protected boolean emptyArg(RAbstractStringVector arg, RAbstractIntVector start, RAbstractIntVector stop) {
        return arg.getLength() == 0;
    }

    protected boolean wrongParams(@SuppressWarnings("unused") RAbstractStringVector arg, RAbstractIntVector start, RAbstractIntVector stop) {
        if (start.getLength() == 0 || stop.getLength() == 0) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_ARGUMENTS_NO_QUOTE, "substring");
        }
        return false;
    }

    @SuppressWarnings("unused")
    protected boolean wrongValue(RAbstractStringVector arg, RAbstractIntVector start, RAbstractIntVector stop, RAbstractVector value) {
        return value.getElementClass() != RString.class || value.getLength() == 0;
    }

}
