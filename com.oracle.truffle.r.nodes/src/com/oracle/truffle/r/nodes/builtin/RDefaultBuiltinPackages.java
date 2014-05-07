/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.runtime.*;

/**
 * The default set of builtin packages that are loaded on startup. TODO This class should go away
 * and {@link #load} move elsewhere, as the set is no longer static.
 */
public final class RDefaultBuiltinPackages extends RBuiltinPackages {

    private RDefaultBuiltinPackages() {
        // empty
    }

    private static final RDefaultBuiltinPackages instance = new RDefaultBuiltinPackages();

    public static RDefaultBuiltinPackages getInstance() {
        return instance;
    }

    public static void load(String name, @SuppressWarnings("unused") VirtualFrame frame) {
        try {
            String className = "com.oracle.truffle.r.nodes.builtin." + name + "." + name.substring(0, 1).toUpperCase() + name.substring(1) + "Package";
            instance.load((RBuiltinPackage) Class.forName(className).newInstance());
        } catch (Exception ex) {
            Utils.fail("cannot load builtin package " + name + " " + ex);
        }
    }
}
