/*
 * Copyright (c) 2014, 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.data;

import static com.oracle.truffle.r.runtime.data.RCall.CallRep.Mode.*;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.SlowPath;
import com.oracle.truffle.r.runtime.*;

/**
 * Meta-level representation of an R function call. Internally, the representation consists of a
 * {@link String} and a collection of {@link RArgsValuesAndNames argument names and values}.
 */
public final class RCall extends RLanguageRep {

    public static final class CallRep {

        public static enum Mode {
            NAME,
            FUNCTION
        }

        private final Mode mode;
        private final Object repInternal;
        private final RArgsValuesAndNames args;

        public CallRep(String name, RArgsValuesAndNames args) {
            this.mode = NAME;
            this.repInternal = name;
            this.args = args;
        }

        public CallRep(RFunction function, RArgsValuesAndNames args) {
            this.mode = FUNCTION;
            this.repInternal = function;
            this.args = args;
        }

        public String getName() {
            if (mode != NAME) {
                throw RInternalError.shouldNotReachHere("call not defined by name");
            }
            return (String) repInternal;
        }

        public RFunction getFunction() {
            if (mode != FUNCTION) {
                throw RInternalError.shouldNotReachHere("call not defined by function");
            }
            return (RFunction) repInternal;
        }

        public RArgsValuesAndNames getArgs() {
            return args;
        }

    }

    public RCall(String name, RArgsValuesAndNames args) {
        super(new CallRep(name, args));
    }

    public RCall(RFunction function, RArgsValuesAndNames args) {
        super(new CallRep(function, args));
    }

    @CompilationFinal private String stringRep;

    @Override
    public String toString() {
        if (stringRep == null) {
            stringRep = toString0();
        }
        return stringRep;
    }

    @SlowPath
    private String toString0() {
        CallRep rep = (CallRep) getRep();
        StringBuilder sb = new StringBuilder();
        switch (rep.mode) {
            case NAME:
                sb.append(rep.getName());
                break;
            case FUNCTION:
                sb.append(deparse(rep.getFunction()));
                break;
            default:
                throw RInternalError.shouldNotReachHere();
        }
        sb.append('(');
        RArgsValuesAndNames args = rep.getArgs();
        if (args != null) {
            String[] names = args.getNames();
            Object[] values = args.getValues();
            for (int i = 0; i < args.length(); ++i) {
                if (names != null && names[i] != null) {
                    sb.append(names[i]).append(" = ");
                }
                // TODO not sure deparse is the right way to do this (might be better to get hold of
                // the source sections of the arguments)
                sb.append(deparse(values[i]));
                if (i + 1 < args.length()) {
                    sb.append(", ");
                }
            }
        }
        return RRuntime.toString(sb.append(')'));
    }

    private static String deparse(Object o) {
        // pass default values to deparse
        return RDeparse.deparse(o, 60, false, -1)[0];
    }

}
