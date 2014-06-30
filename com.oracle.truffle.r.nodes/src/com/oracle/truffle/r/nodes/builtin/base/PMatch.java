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

import java.util.*;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

/**
 * TODO Implement completely. Currently just what is needed for {@code set.seed}.
 */
@RBuiltin(name = "pmatch", kind = INTERNAL)
public abstract class PMatch extends RBuiltinNode {

    @Specialization
    public RIntVector doPMatch(String x, RAbstractStringVector table) {
        ArrayList<Integer> matches = new ArrayList<>(table.getLength());
        for (int i = 0; i < table.getLength(); i++) {
            if (x.equals(table.getDataAt(i))) {
                matches.add(i + 1);
            }
        }
        int size = matches.size();
        if (size == 0) {
            return RDataFactory.createIntVectorFromScalar(RRuntime.INT_NA);
        } else if (size == 1) {
            return RDataFactory.createIntVectorFromScalar(matches.get(0));
        } else {
            throw RError.nyi(getEncapsulatingSourceSection(), "pmatch aspect");
        }
    }
}
