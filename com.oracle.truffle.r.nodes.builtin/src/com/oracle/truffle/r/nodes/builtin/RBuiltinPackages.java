/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.r.nodes.builtin.base.BasePackage;
import com.oracle.truffle.r.nodes.builtin.base.BaseVariables;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.RBuiltinKind;
import com.oracle.truffle.r.runtime.RBuiltinLookup;
import com.oracle.truffle.r.runtime.RDeparse;
import com.oracle.truffle.r.runtime.REnvVars;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.context.Engine.ParseException;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RBuiltinDescriptor;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.env.REnvironment.PutException;
import com.oracle.truffle.r.runtime.ffi.DLL;
import com.oracle.truffle.r.runtime.ffi.DLL.DLLException;

/**
 * Support for loading the base package and also optional overrides for the packages provided with
 * the system.
 */
public final class RBuiltinPackages implements RBuiltinLookup {

    private static final RBuiltinPackages instance = new RBuiltinPackages();
    private static final RBuiltinPackage basePackage = new BasePackage();

    public static RBuiltinPackages getInstance() {
        return instance;
    }

    @SuppressWarnings("deprecation")
    public static void loadBase(MaterializedFrame baseFrame) {
        RBuiltinPackage pkg = basePackage;
        REnvironment baseEnv = REnvironment.baseEnv();
        BaseVariables.initialize(baseEnv);
        /*
         * All the RBuiltin PRIMITIVE methods that were created earlier need to be added to the
         * environment so that lookups through the environment work as expected.
         */
        Map<String, RBuiltinFactory> builtins = pkg.getBuiltins();
        for (Map.Entry<String, RBuiltinFactory> entrySet : builtins.entrySet()) {
            String methodName = entrySet.getKey();
            RBuiltinFactory builtinFactory = entrySet.getValue();
            if (builtinFactory.getKind() != RBuiltinKind.INTERNAL) {
                RFunction function = createFunction(builtinFactory, methodName);
                try {
                    baseEnv.put(methodName, function);
                    baseEnv.lockBinding(methodName);
                } catch (PutException ex) {
                    Utils.fail("failed to install builtin function: " + methodName);
                }
            }
        }
        // Now "load" the package
        Path baseDirPath = FileSystems.getDefault().getPath(REnvVars.rHome(), "library", "base");
        Path basePathbase = baseDirPath.resolve("R").resolve("base");
        Source baseSource = null;
        try {
            baseSource = Source.fromFileName(basePathbase.toString());
        } catch (IOException ex) {
            Utils.fail(String.format("unable to open the base package %s", basePathbase));
        }
        // Load the (stub) DLL for base
        try {
            DLL.loadPackageDLL(baseDirPath.resolve("libs").resolve("base.so").toString(), true, true);
        } catch (DLLException ex) {
            Utils.fail(ex.getMessage());
        }
        // Any RBuiltinKind.SUBSTITUTE functions installed above should not be overridden
        try {
            RContext.getInstance().setLoadingBase(true);
            try {
                RContext.getEngine().parseAndEval(baseSource, baseFrame, false);
            } catch (ParseException e) {
                throw new RInternalError(e, "error while parsing base source from %s", baseSource.getName());
            }
        } finally {
            RContext.getInstance().setLoadingBase(false);
        }
        pkg.loadOverrides(baseFrame);
    }

    public static void loadDefaultPackageOverrides(String pkgName) {
        ArrayList<Source> componentList = RBuiltinPackage.getRFiles(pkgName);
        if (componentList.size() > 0) {
            /*
             * Only the overriding code can know which environment to update, package or namespace.
             */
            REnvironment env = REnvironment.baseEnv();
            for (Source source : componentList) {
                try {
                    RContext.getEngine().parseAndEval(source, env.getFrame(), false);
                } catch (ParseException e) {
                    throw new RInternalError(e, "error while parsing default package override from %s", source.getName());
                }
            }
        }
    }

    /**
     * Global builtin cache.
     */
    private static final HashMap<Object, RFunction> cachedBuiltinFunctions = new HashMap<>();

    @Override
    public RFunction lookupBuiltin(String methodName) {
        CompilerAsserts.neverPartOfCompilation();
        RFunction function = cachedBuiltinFunctions.get(methodName);
        if (function != null) {
            return function;
        }

        RBuiltinFactory builtin = lookupBuiltinDescriptor(methodName);
        if (builtin == null) {
            return null;
        }
        return createFunction(builtin, methodName);
    }

    private static RFunction createFunction(RBuiltinFactory builtinFactory, String methodName) {
        try {
            RootCallTarget callTarget = RBuiltinNode.createArgumentsCallTarget(builtinFactory);
            RFunction function = RDataFactory.createFunction(builtinFactory.getName(), callTarget, builtinFactory, null);
            cachedBuiltinFunctions.put(methodName, function);
            return function;
        } catch (Throwable t) {
            throw new RuntimeException("error while creating builtin " + methodName + " / " + builtinFactory, t);
        }
    }

    @Override
    public RBuiltinFactory lookupBuiltinDescriptor(String name) {
        CompilerAsserts.neverPartOfCompilation();
        return basePackage.lookupByName(name);
    }

    /**
     * Used by {@link RDeparse} to detect whether a symbol is a builtin (or special), i.e. not an
     * {@link RBuiltinKind#INTERNAL}. N.B. special functions are not explicitly denoted currently,
     * only by virtue of the {@link RBuiltin#nonEvalArgs} attribute.
     */
    @Override
    public boolean isPrimitiveBuiltin(String name) {
        RBuiltinPackage pkg = basePackage;
        RBuiltinDescriptor rbf = pkg.lookupByName(name);
        if (rbf != null && rbf.getKind() != RBuiltinKind.INTERNAL) {
            return true;
        }
        return false;
    }
}
