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

import java.util.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.builtin.base.*;
import com.oracle.truffle.r.nodes.builtin.fastr.*;
import com.oracle.truffle.r.nodes.builtin.methods.*;
import com.oracle.truffle.r.nodes.builtin.stats.*;
import com.oracle.truffle.r.nodes.builtin.utils.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.env.*;

/**
 * Support for the default set of packages in an R session. Setup is a two-phase process. The
 * meta-data for the possible set of default packages (which is a known set) is established
 * statically (to support an AOT-based VM without runtime reflection), and then the dynamic state is
 * established for a given subset of the packages at runtime, through the {@link #load} method.
 */
public final class RBuiltinPackages implements RBuiltinLookup {

    private static final HashMap<String, RBuiltinPackage> packages = new HashMap<>(6);

    private static final RBuiltinPackages instance = new RBuiltinPackages();

    static {
        RBuiltinPackages.add(new BasePackage());
        RBuiltinPackages.add(new FastRPackage());
        RBuiltinPackages.add(new StatsPackage());
        RBuiltinPackages.add(new MethodsPackage());
        RBuiltinPackages.add(new UtilsPackage());
    }

    protected static void add(RBuiltinPackage builtins) {
        packages.put(builtins.getName(), builtins);
    }

    public static RBuiltinPackages getInstance() {
        return instance;
    }

    public static Map<String, RBuiltinPackage> getPackages() {
        return packages;
    }

    public static void load(String name, MaterializedFrame frame, REnvironment envForFrame) {
        RBuiltinPackage pkg = packages.get(name);
        if (pkg == null) {
            Utils.fail("unknown default package: " + name);
        } else {
            pkg.loadSources(frame, envForFrame);
        }
    }

    @Override
    public RFunction lookup(String methodName) {
        RFunction function = RContext.getInstance().getCachedFunction(methodName);
        if (function != null) {
            return function;
        }

        RBuiltinFactory builtin = lookupBuiltin(methodName);
        if (builtin == null) {
            return null;
        }
        return createFunction(builtin, methodName);
    }

    private static RFunction createFunction(RBuiltinFactory builtinFactory, String methodName) {
        RootCallTarget callTarget = RBuiltinNode.createArgumentsCallTarget(builtinFactory);
        RBuiltin builtin = builtinFactory.getRBuiltin();
        assert builtin != null;
        return RContext.getInstance().putCachedFunction(methodName, new RFunction(builtinFactory.getBuiltinNames()[0], callTarget, builtin, builtinFactory.getEnv().getFrame()));
    }

    public static RBuiltinFactory lookupBuiltin(String name) {
        return RBuiltinPackage.lookupByName(name);
    }

}
