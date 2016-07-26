/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.builtins.RBuiltinKind;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.ffi.RFFIFactory;

@RBuiltin(name = "strtoi", kind = RBuiltinKind.INTERNAL, parameterNames = {"x", "base"})
public abstract class Strtoi extends RBuiltinNode {
    @Specialization
    @TruffleBoundary
    protected RIntVector doStrtoi(RAbstractStringVector vec, int baseArg) {
        int base = baseArg;
        int[] data = new int[vec.getLength()];
        boolean complete = RDataFactory.COMPLETE_VECTOR;
        for (int i = 0; i < data.length; i++) {
            int dataValue = RRuntime.INT_NA;
            try {
                String s = vec.getDataAt(i);
                if (s.length() == 0) {
                    complete = RDataFactory.INCOMPLETE_VECTOR;
                } else {
                    if (base == 0) {
                        char ch0 = s.charAt(0);
                        if (ch0 == '0') {
                            if (s.length() > 1 && (s.charAt(1) == 'x' || s.charAt(1) == 'X')) {
                                base = 16;
                            } else {
                                base = 8;
                            }
                        } else {
                            base = 10;
                        }
                    }
                    long value = RFFIFactory.getRFFI().getBaseRFFI().strtol(s, base);
                    if (value > Integer.MAX_VALUE || value < Integer.MIN_VALUE) {
                        complete = RDataFactory.INCOMPLETE_VECTOR;
                    } else {
                        dataValue = (int) value;
                    }
                }
            } catch (IllegalArgumentException ex) {
                complete = RDataFactory.INCOMPLETE_VECTOR;
            }
            data[i] = dataValue;
        }
        return RDataFactory.createIntVector(data, complete);
    }
}
