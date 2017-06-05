/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RSource;
import com.oracle.truffle.r.runtime.ResourceHandlerFactory;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.builtins.RSpecialFactory;
import com.oracle.truffle.r.runtime.context.Engine.ParseException;
import com.oracle.truffle.r.runtime.context.RContext;

/**
 * Denotes an R package that is (partially) built-in to the implementation. Historically, several of
 * the default packages, e.g. {@code stats}, were partially built-in, but now only the {@code base}
 * package is built-in. However, this class is retained as it provides a mechanism, should it ever
 * be deemed beneficial, to "override" the default implementations provided by GnuR. Such overrides
 * can take the form of alternative R code or replacing a function with a built-in implementation.
 *
 * A built-in package consists of two parts:
 * <ul>
 * <li>Classes annotated with {@link RBuiltin} that implement the built-in functions directly in
 * Java, either "primitives" or ".Internal".</li>
 * <li>R code that overrides functions in the package. This may, and usually will, be empty</li>
 * </ul>
 * <p>
 * Any R code is expected to be found (as resources) in the 'R' sub-package (directory) associated
 * with the subclass package, e.g., {@code com.oracle.truffle.r.nodes.builtin.base.R}. For debugging
 * parsing errors we retain the R source code, although this is not functionally necessary.
 * <p>
 * To cope with a possible lack of reflection capability in an AOT compiled VM, initialization is
 * two phase, with all reflective code executed in code reachable only from static initializers.
 */
public abstract class RBuiltinPackage {

    private final String name;

    /**
     * Any "override" sources associated with the package.
     */
    private final HashMap<String, ArrayList<Source>> rSources = new HashMap<>();
    /**
     * The factories for the {@link RBuiltin}s defined by the package.
     */
    private final TreeMap<String, RBuiltinFactory> builtins = new TreeMap<>();

    private synchronized void putBuiltin(RBuiltinFactory factory) {
        putBuiltinInternal(factory.getName(), factory);
        for (String alias : factory.getAliases()) {
            putBuiltinInternal(alias, factory);
        }
    }

    private void putBuiltinInternal(String builtinName, RBuiltinFactory factory) {
        if (builtins.containsKey(builtinName)) {
            throw new RuntimeException("Duplicate builtin " + builtinName + " defined.");
        }
        builtins.put(builtinName, factory);
    }

    protected RBuiltinPackage(String name) {
        this.name = name;
        // Check for overriding R code
        ArrayList<Source> componentList = getRFiles(getName());
        if (componentList.size() > 0) {
            rSources.put(getName(), componentList);
        }
    }

    public final String getName() {
        return name;
    }

    private static final ConcurrentHashMap<String, Map<String, String>> rFilesCache = new ConcurrentHashMap<>();

    /**
     * Get a list of R override files for package {@code pkgName}, from the {@code pkgName/R}
     * subdirectory.
     */
    public static ArrayList<Source> getRFiles(String pkgName) {
        ArrayList<Source> componentList = new ArrayList<>();
        Map<String, String> rFileContents = rFilesCache.get(pkgName);
        if (rFileContents == null) {
            rFileContents = ResourceHandlerFactory.getHandler().getRFiles(RBuiltinPackage.class, pkgName);
            rFilesCache.put(pkgName, rFileContents);
        }
        for (String rFileContent : rFileContents.values()) {
            Source content = RSource.fromTextInternal(rFileContent, RSource.Internal.R_IMPL);
            componentList.add(content);
        }
        return componentList;
    }

    public RBuiltinFactory lookupByName(String methodName) {
        return builtins.get(methodName);
    }

    public TreeMap<String, RBuiltinFactory> getBuiltins() {
        return builtins;
    }

    /**
     * Runtime component of the package initialization process.
     */
    public void loadOverrides(MaterializedFrame baseFrame) {
        ArrayList<Source> sources = rSources.get(getName());
        if (sources != null) {
            for (Source source : sources) {
                try {
                    RContext.getEngine().parseAndEval(source, baseFrame, false);
                } catch (ParseException e) {
                    throw new RInternalError(e, "error while parsing overrides from %s", source.getName());
                }
            }
        }
    }

    protected void add(Class<?> builtinClass,
                    Supplier<RBuiltinNode> constructor) {
        add(builtinClass, constructor, null);
    }

    protected void add(Class<?> builtinClass, Supplier<RBuiltinNode> constructor, RSpecialFactory specialCall) {
        RBuiltin annotation = builtinClass.getAnnotation(RBuiltin.class);
        String[] parameterNames = annotation.parameterNames();
        parameterNames = Arrays.stream(parameterNames).map(n -> n.isEmpty() ? null : n).toArray(String[]::new);
        ArgumentsSignature signature = ArgumentsSignature.get(parameterNames);

        putBuiltin(new RBuiltinFactory(annotation.name(), builtinClass, annotation.visibility(), annotation.aliases(), annotation.kind(), signature, annotation.nonEvalArgs(), annotation.splitCaller(),
                        annotation.alwaysSplit(), annotation.dispatch(), annotation.genericName(), constructor, annotation.behavior(), specialCall));
    }
}
