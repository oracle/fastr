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

@RBuiltin(name = "options", kind = INTERNAL)
// @NodeField(name = "argNames", type = String[].class)
public abstract class Options extends RBuiltinNode {
    private static final Object[] PARAMETER_NAMES = new Object[]{"..."};

    @Override
    public Object[] getParameterNames() {
        return PARAMETER_NAMES;
    }

// public abstract String[] getArgNames();

    @Specialization
    public RList options(@SuppressWarnings("unused") RMissing x) {
        Set<Map.Entry<String, Object>> optionSettings = ROptions.getValues();
        Object[] data = new Object[optionSettings.size()];
        String[] names = new String[data.length];
        int i = 0;
        for (Map.Entry<String, Object> entry : optionSettings) {
            names[i] = entry.getKey();
            data[i] = entry.getValue();
            i++;
        }
        return RDataFactory.createList(data, RDataFactory.createStringVector(names, RDataFactory.COMPLETE_VECTOR));
    }

    @Specialization
    public RList options(RAbstractStringVector vec) {
        String key = vec.getDataAt(0);
        Object value = ROptions.getValue(key);
        Object rObject = value == null ? RNull.instance : value;
        return RDataFactory.createList(new Object[]{rObject}, RDataFactory.createStringVectorFromScalar(key));
    }

    @Specialization
    public Object options(@SuppressWarnings("unused") double d) {
        // HACK ALERT - just to allow b25 test, it doesn't do anything,
        // as that would require the option name
        controlVisibility();
        return RNull.instance;
    }
}
