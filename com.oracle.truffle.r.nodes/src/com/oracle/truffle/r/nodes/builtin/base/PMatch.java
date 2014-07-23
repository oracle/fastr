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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin(name = "pmatch", kind = INTERNAL)
public abstract class PMatch extends RBuiltinNode {

    @CreateCast("arguments")
    public RNode[] castArguments(RNode[] arguments) {
        arguments[2] = CastIntegerNodeFactory.create(arguments[2], false, false, false);
        arguments[3] = CastLogicalNodeFactory.create(arguments[3], false, false, false);
        return arguments;
    }

    @Specialization
    public RIntVector doPMatch(RAbstractStringVector x, RAbstractStringVector table, int nomatch, byte duplicatesOk) {
        int xl = x.getLength();
        int tl = table.getLength();
        boolean dupsOk = RRuntime.fromLogical(duplicatesOk);
        int[] matches = new int[xl];
        boolean[] matched = new boolean[xl];
        boolean[] used = new boolean[tl];
        // set up default result
        for (int i = 0; i < matches.length; i++) {
            matches[i] = nomatch;
        }
        // check for exact matches, then partial matches
        for (int p = 0; p < 2; p++) {
            for (int i = 0; i < xl; i++) {
                String xs = x.getDataAt(i);
                if (matched[i]) {
                    continue;
                }
                // empty string matches nothing, not even another empty string
                if (xs.length() == 0) {
                    continue;
                }
                if (xs == RRuntime.STRING_NA) {
                    xs = "NA";
                }
                for (int t = 0; t < tl; t++) {
                    if (!used[t]) {
                        boolean match = p == 0 ? xs.equals(table.getDataAt(t)) : table.getDataAt(t).startsWith(xs);
                        if (match) {
                            matches[i] = t + 1;
                            matched[i] = true;
                            if (!dupsOk) {
                                used[t] = true;
                            }
                        }
                    }
                }
            }
        }
        boolean complete = RDataFactory.COMPLETE_VECTOR;
        if (nomatch == RRuntime.INT_NA) {
            for (int i = 0; i < matches.length; i++) {
                if (matches[i] == RRuntime.INT_NA) {
                    complete = RDataFactory.INCOMPLETE_VECTOR;
                    break;
                }
            }
        }
        return RDataFactory.createIntVector(matches, complete);
    }
}
