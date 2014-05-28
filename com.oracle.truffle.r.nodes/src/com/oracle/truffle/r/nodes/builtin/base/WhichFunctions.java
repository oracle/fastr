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

import static com.oracle.truffle.r.nodes.builtin.RBuiltinKind.*;

import java.util.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

/**
 * Encapsulates all which* as nested static classes.
 */
public class WhichFunctions {

    @RBuiltin(name = "which", kind = INTERNAL)
    public static abstract class Which extends RBuiltinNode {

        @Specialization
        public RIntVector which(RAbstractLogicalVector x) {
            controlVisibility();
            ArrayList<Integer> w = new ArrayList<>();
            for (int i = 0; i < x.getLength(); ++i) {
                if (x.getDataAt(i) == RRuntime.LOGICAL_TRUE) {
                    w.add(i);
                }
            }
            int[] result = new int[w.size()];
            for (int i = 0; i < result.length; ++i) {
                result[i] = w.get(i) + 1;
            }
            return RDataFactory.createIntVector(result, RDataFactory.COMPLETE_VECTOR);
        }
    }

    @RBuiltin(name = "which.max", kind = RBuiltinKind.INTERNAL)
    public static abstract class WhichMax extends RBuiltinNode {

        @CreateCast("arguments")
        public RNode[] castArguments(RNode[] arguments) {
            arguments[0] = CastDoubleNodeFactory.create(arguments[0], true, false, false);
            return arguments;
        }

        @Specialization
        public int which(RAbstractDoubleVector x) {
            controlVisibility();
            double max = x.getDataAt(0);
            int max_index = 0;
            for (int i = 0; i < x.getLength(); i++) {
                if (x.getDataAt(i) > max) {
                    max = x.getDataAt(i);
                    max_index = i;
                }
            }
            return max_index + 1;
        }

        @Specialization
        public int which(@SuppressWarnings("unused") Object x) {
            controlVisibility();
            CompilerDirectives.transferToInterpreter();
            throw RError.getNonNumericMath(this.getEncapsulatingSourceSection());
        }
    }

    @RBuiltin(name = "which.min", kind = RBuiltinKind.INTERNAL)
    public static abstract class WhichMin extends RBuiltinNode {

        @CreateCast("arguments")
        public RNode[] castArguments(RNode[] arguments) {
            arguments[0] = CastDoubleNodeFactory.create(arguments[0], true, false, false);
            return arguments;
        }

        @Specialization
        public int which(RAbstractDoubleVector x) {
            controlVisibility();
            double minimum = x.getDataAt(0);
            int min_index = 0;
            for (int i = 0; i < x.getLength(); i++) {
                if (x.getDataAt(i) < minimum) {
                    minimum = x.getDataAt(i);
                    min_index = i;
                }
            }
            return min_index + 1;
        }

        @Specialization
        public int which(@SuppressWarnings("unused") Object x) {
            controlVisibility();
            CompilerDirectives.transferToInterpreter();
            throw RError.getNonNumericMath(this.getEncapsulatingSourceSection());
        }
    }
}
