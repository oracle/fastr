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
package com.oracle.truffle.r.runtime;

import java.util.*;

import com.oracle.truffle.r.runtime.data.*;

/**
 * Central location for all R options, that is for the {@code options(...)} and {@code getOption}
 * builtins.
 *
 * An unset option does not appear in the map but is represented as the value {@link RNull#instance}
 * . Setting with {@link RNull#instance} removes the option from the map and, therefore, from being
 * visible in a call to {@code options()}.
 *
 */
public class ROptions {

    private static final HashMap<String, Object> map = new HashMap<>();

    // Transcribed from src/main.options.c

    public static void initialize() {
        ROptions.setValue("add.smooth", RDataFactory.createLogicalVectorFromScalar(true));
        ROptions.setValue("check.bounds", RDataFactory.createLogicalVectorFromScalar(false));
        ROptions.setValue("continue", RDataFactory.createStringVector("+ "));
        ROptions.setValue("deparse.cutoff", RDataFactory.createIntVectorFromScalar(60));
        ROptions.setValue("digits", RDataFactory.createIntVectorFromScalar(7));
        ROptions.setValue("echo", RDataFactory.createLogicalVectorFromScalar(true));
        ROptions.setValue("echo", RDataFactory.createStringVector("native.enc"));
        ROptions.setValue("expressions", RDataFactory.createIntVectorFromScalar(5000));
        boolean keepPkgSource = optionFromEnvVar("R_KEEP_PKG_SOURCE");
        ROptions.setValue("keep.source", RDataFactory.createLogicalVectorFromScalar(keepPkgSource));
        ROptions.setValue("keep.source.pkgs", RDataFactory.createLogicalVectorFromScalar(keepPkgSource));
        ROptions.setValue("OutDec", RDataFactory.createStringVector("."));
        ROptions.setValue("prompt", RDataFactory.createStringVector("> "));
        ROptions.setValue("verbose", RDataFactory.createLogicalVectorFromScalar(false));
        ROptions.setValue("nwarnings", RDataFactory.createIntVectorFromScalar(50));
        ROptions.setValue("warning.length", RDataFactory.createIntVectorFromScalar(1000));
        ROptions.setValue("width", RDataFactory.createIntVectorFromScalar(80));
        ROptions.setValue("browserNLdisabled", RDataFactory.createLogicalVectorFromScalar(false));
        boolean cBoundsCheck = optionFromEnvVar("R_C_BOUNDS_CHECK");
        ROptions.setValue("CBoundsCheck", RDataFactory.createLogicalVectorFromScalar(cBoundsCheck));
    }

    private static boolean optionFromEnvVar(String envVar) {
        String envValue = REnvVars.get(envVar);
        return envValue != null && envValue.equals("yes");

    }

    public static Set<Map.Entry<String, Object>> getValues() {
        Set<Map.Entry<String, Object>> result = new HashSet<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (entry.getValue() != null) {
                result.add(entry);
            }
        }
        return result;
    }

    public static Object getValue(String key) {
        Object value = map.get(key);
        if (value == null) {
            value = RNull.instance;
        }
        return value;
    }

    public static Object setValue(String key, Object value) {
        Object previous = map.get(key);
        assert value != null;
        if (value == RNull.instance) {
            map.remove(key);
        } else {
            map.put(key, value);
        }
        return previous;
    }

    public static void addOption(String name, Object value) {
        setValue(name, value);
    }

}
