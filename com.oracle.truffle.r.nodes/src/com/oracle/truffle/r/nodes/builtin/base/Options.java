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
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin(name = "options", kind = SUBSTITUTE, isCombine = true)
@NodeField(name = "argNames", type = String[].class)
/**
 * N.B. In the general case of option assignment via parameter names, the value may be of any type (i.e. {@code Object},
 * so we cannot (currently) specialize on any specific types, owing to the "stuck specialization" bug.
 *
 * TODO Revert to {@code INTERNAL} when argument names available.
 */
public abstract class Options extends RBuiltinNode {
    private static final Object[] PARAMETER_NAMES = new Object[]{"..."};

    @Override
    public Object[] getParameterNames() {
        return PARAMETER_NAMES;
    }

    public abstract String[] getArgNames();

    // @Specialization
    private RList options(@SuppressWarnings("unused") RMissing x) {
        controlVisibility();
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
    public Object options(Object args) {
        controlVisibility();
        if (args instanceof RMissing) {
            return options((RMissing) args);
        } else {
            Object[] values = args instanceof Object[] ? (Object[]) args : new Object[]{args};
            String[] argNames = getArgNames();
            Object[] data = new Object[values.length];
            String[] names = new String[values.length];
            // getting
            for (int i = 0; i < values.length; i++) {
                String argName = namedArgument(argNames, i);
                Object value = values[i];
                if (argName == null) {
                    // getting
                    String optionName = null;
                    if (value instanceof RStringVector) {
                        optionName = ((RStringVector) value).getDataAt(0); // ignore rest (cf GnuR)
                    } else if (value instanceof String) {
                        optionName = (String) value;
                    } else {
                        throw RError.error(getEncapsulatingSourceSection(), Message.INVALID_UNNAMED_ARGUMENT);
                    }
                    Object optionVal = ROptions.getValue(optionName);
                    data[i] = optionVal == null ? RNull.instance : optionVal;
                    names[i] = optionName;
                } else {
                    // setting
                    Object previousVal = ROptions.getValue(argName);
                    data[i] = previousVal == null ? RNull.instance : previousVal;
                    names[i] = argName;
                    ROptions.setValue(argName, value);
                    // any settings means result is invisible
                    RContext.setVisible(false);
                }
            }
            return RDataFactory.createList(data, RDataFactory.createStringVector(names, RDataFactory.COMPLETE_VECTOR));
        }
    }

    private static String namedArgument(String[] argNames, int i) {
        if (argNames == null) {
            return null;
        } else {
            return argNames[i];
        }
    }

// @Specialization
    public RList options(RAbstractStringVector vec) {
        String key = vec.getDataAt(0);
        Object value = ROptions.getValue(key);
        Object rObject = value == null ? RNull.instance : value;
        return RDataFactory.createList(new Object[]{rObject}, RDataFactory.createStringVectorFromScalar(key));
    }

// @Specialization
    public Object options(@SuppressWarnings("unused") double d) {
        // HACK ALERT - just to allow b25 test, it doesn't do anything,
        // as that would require the option name
        controlVisibility();
        return RNull.instance;
    }

// @Specialization
    public Object options(@SuppressWarnings("unused") RFunction f) {
        // HACK ALERT - just to allow b25 test, it doesn't do anything,
        // as that would require the option name
        controlVisibility();
        return RNull.instance;
    }

}
