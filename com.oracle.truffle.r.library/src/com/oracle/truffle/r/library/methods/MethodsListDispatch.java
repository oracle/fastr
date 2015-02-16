/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2015, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.methods;

import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.env.*;

// Transcribed from src/library/methods/methods_list_dispatch.c

public class MethodsListDispatch {
    private static MethodsListDispatch singleton = new MethodsListDispatch();

    private boolean tableDispatchOn = true;

    public static MethodsListDispatch getInstance() {
        return singleton;
    }

    public REnvironment initMethodDispatch(REnvironment env) {
        // TODO initialize
        return env;
    }

    public byte setMethodDispatch(byte onOff) {
        boolean prev = tableDispatchOn;

        if (onOff == RRuntime.LOGICAL_NA) {
            return RRuntime.asLogical(prev);
        }
        boolean value = RRuntime.fromLogical(onOff);
        tableDispatchOn = value;
        if (value != prev) {
            // TODO
        }
        return RRuntime.asLogical(prev);
    }

    public String methodsPackageMetaName(String prefixString, String nameString, String pkgString) {
        if (pkgString.length() == 0) {
            return String.format(".__%s__%s", prefixString, nameString);
        } else {
            return String.format(".__%s__%s:%s", prefixString, nameString, pkgString);
        }
    }

    public Object getClassFromCache(REnvironment table, String klassString) {
        Object value = table.get(klassString);
        if (value == null) {
            return RNull.instance;
        } else {
            // TODO check PACKAGE equality
            return value;
        }

    }

}
