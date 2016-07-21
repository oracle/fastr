/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

/**
 * Abstracted for use by {@code List2Env}, {@code AsEnvironment}, {@code SubsituteDirect}.
 */
public final class RList2EnvNode extends RBaseNode {

    @TruffleBoundary
    public REnvironment execute(RList list, REnvironment env) {
        if (list.getLength() == 0) {
            return env;
        }
        RStringVector names = list.getNames();
        if (names == null) {
            throw RError.error(this, RError.Message.LIST_NAMES_SAME_LENGTH);
        }
        for (int i = list.getLength() - 1; i >= 0; i--) {
            String name = names.getDataAt(i);
            if (name.length() == 0) {
                throw RError.error(this, RError.Message.ZERO_LENGTH_VARIABLE);
            }
            // in case of duplicates, last element in list wins
            if (env.get(name) == null) {
                env.safePut(name, list.getDataAt(i));
            }
        }
        return env;
    }
}
