/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.instrument;

import com.oracle.truffle.r.runtime.FunctionUID;
import com.oracle.truffle.r.runtime.Utils;

public abstract class FunctionUIDFactory {
    private static final String FACTORY_CLASS_PROPERTY = "fastr.fuid.factory.class";
    private static final String PACKAGE_PREFIX = "com.oracle.truffle.r.nodes.function.";
    private static final String SUFFIX = "FunctionUIDFactory";
    private static final String DEFAULT_FACTORY = "along";
    private static final String DEFAULT_FACTORY_CLASS = mapSimpleName(DEFAULT_FACTORY);

    private static String mapSimpleName(String simpleName) {
        return PACKAGE_PREFIX + simpleName.toUpperCase() + SUFFIX;
    }

    private static FunctionUIDFactory instance;

    static {
        String prop = System.getProperty(FACTORY_CLASS_PROPERTY);
        if (prop != null) {
            if (!prop.contains(".")) {
                // simple name
                prop = mapSimpleName(prop);
            }
        } else {
            prop = DEFAULT_FACTORY_CLASS;
        }
        try {
            instance = (FunctionUIDFactory) Class.forName(prop).newInstance();
        } catch (Exception ex) {
            Utils.fail("Failed to instantiate class: " + prop + ": " + ex);
        }
    }

    public static FunctionUIDFactory get() {
        return instance;
    }

    public abstract FunctionUID createUID();
}
