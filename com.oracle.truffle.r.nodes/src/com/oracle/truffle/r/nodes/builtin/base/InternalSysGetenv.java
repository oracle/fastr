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

import java.util.*;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

/**
 * This is the internal component of {@code Sys.getenv}, normally invoked by
 * {@code .Internal(Sys.getenv(...))}. As {@code .Internal} is not yet implemented, we rename it and
 * invoke the renamed version explicitly from a modified version of {@code getenv.R}.
 */
@RBuiltin({".Internal.Sys.getenv"})
public abstract class InternalSysGetenv extends RBuiltinNode {

    @Specialization()
    public Object sysGetEnv(RAbstractStringVector x, String unset) {
        Map<String, String> envMap = REnvVars.getMap();
        int len = x.getLength();
        String[] data = new String[len == 0 ? envMap.size() : len];
        if (len == 0) {
            // all
            int i = 0;
            for (Map.Entry<String, String> entry : envMap.entrySet()) {
                data[i++] = entry.getKey() + '=' + entry.getValue();
            }
            return RDataFactory.createStringVector(data, true);
        } else {
            // just those in 'x' without the 'name=' which is handled in the R snippet
            boolean complete = RDataFactory.COMPLETE_VECTOR;
            for (int i = 0; i < len; i++) {
                String name = x.getDataAt(i);
                String value = envMap.get(name);
                if (value != null) {
                    data[i] = value;
                } else {
                    data[i] = unset;
                    if (unset == RRuntime.STRING_NA) {
                        complete = RDataFactory.INCOMPLETE_VECTOR;
                    }
                }
            }
            return RDataFactory.createStringVector(data, complete);
        }
    }

    @Generic
    public Object sysGetEnvGeneric(@SuppressWarnings("unused") Object x, @SuppressWarnings("unused") Object unset) {
        throw RError.getWrongTypeOfArgument(getSourceSection());
    }

}
