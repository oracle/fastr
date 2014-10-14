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

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode.RCustomBuiltinNode;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.env.*;

/**
 * Denotes an R package that is built-in to the implementation. It consists of two parts:
 * <ul>
 * <li>Classes annotated with {@link RBuiltin} that implement the package functions directly in Java
 * either as "primitives" or as ".Internal".</li>
 * <li>R code that defines functions in the package (which typically call the @link RBuiltin}s</li>
 * </ul>
 * Note that, although several packages are built-in to the implementation, R allows the exact set
 * of packages to be controlled at runtime by the {@code R_DEFAULT_PACKAGES} environment variable.
 * Only the {@code base} package is always loaded.
 * <p>
 * The R code is expected to be found (as resources) in the 'R' sub-package (directory) associated
 * with the subclass package, e.g., {@code com.oracle.truffle.r.nodes.builtin.base.R}. For debugging
 * parsing errors we retain the R source code, although this is not functionally necessary.
 * <p>
 * <p>
 * To cope with a possible lack of reflection capability in an AOT compiled VM, initialization is
 * two phase, with all reflective code executed in code reachable only from static initializers.
 */
public abstract class RBuiltinPackage {

    private static class Component {
        public final String libContents;
        public final String libName;

        public Component(String libName, String libContents) {
            this.libContents = libContents;
            this.libName = libName;
        }

        @Override
        public String toString() {
            return libName;
        }
    }

    private static final HashMap<String, ArrayList<Component>> rSources = new HashMap<>();
    private static final TreeMap<String, RBuiltinFactory> builtins = new TreeMap<>();

    private static synchronized void putBuiltin(String name, RBuiltinFactory factory) {
        builtins.put(name, factory);
    }

    protected REnvironment env;

    protected RBuiltinPackage() {
        try {
            InputStream is = ResourceHandlerFactory.getHandler().getResourceAsStream(getClass(), "R");
            if (is == null) {
                return;
            }
            ArrayList<Component> componentList = new ArrayList<>();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(is))) {
                String line;
                while ((line = r.readLine()) != null) {
                    if (line.endsWith(".r") || line.endsWith(".R")) {
                        final String rResource = "R/" + line.trim();
                        String content = Utils.getResourceAsString(getClass(), rResource, true);
                        componentList.add(new Component(getClass().getSimpleName() + "/" + rResource, content));
                    }
                }
            }
            if (componentList.size() > 0) {
                rSources.put(getName(), componentList);
            }
            loadAuxClass("Options");
            loadAuxClass("Variables");
        } catch (IOException ex) {
            Utils.fail("error loading R snippets classes from " + getClass().getSimpleName() + " : " + ex);
        }
    }

    private void loadAuxClass(String auxName) {
        String auxClassName = getClass().getName().replace("Package", auxName);
        try {
            Class.forName(auxClassName).newInstance();
        } catch (ClassNotFoundException ex) {
            // ok, no aux class
        } catch (IllegalAccessException | InstantiationException ex) {
            Utils.fail("error instantiating " + auxClassName + ": " + ex);
        }
    }

    public static RBuiltinFactory lookupByName(String methodName) {
        return builtins.get(methodName);
    }

    public TreeMap<String, RBuiltinFactory> getBuiltins() {
        return builtins;
    }

    void setEnv(REnvironment env) {
        this.env = env;
    }

    /**
     * Runtime component of the package initialization process.
     */
    public void loadSources(MaterializedFrame frame, REnvironment envForFrame) {
        ArrayList<Component> sources = rSources.get(getName());
        if (sources != null) {
            for (Component src : sources) {
                RContext.getEngine().parseAndEval(src.libName, src.libContents, frame, envForFrame, false, false);
            }
        }
    }

    public abstract String getName();

    /**
     * Loads the {@link RBuiltin} annotated classes. N.B. This is driven from a static initializer
     * so it is ok to use reflection even in an AOT VM.
     */
    @SuppressWarnings("unchecked")
    protected final void loadBuiltins() {
        Class<?> builtinClassesClass = null;
        try {
            builtinClassesClass = Class.forName(getClass().getPackage().getName() + ".RBuiltinClasses");
        } catch (ClassNotFoundException ex) {
            return;
        }
        try {
            Field field = builtinClassesClass.getField("RBUILTIN_CLASSES");
            Class<?>[] builtinClasses = (Class<?>[]) field.get(null);
            for (Class<?> builtinClass : builtinClasses) {
                load((Class<? extends RBuiltinNode>) builtinClass);
            }
        } catch (NoSuchFieldException | IllegalAccessException ex) {
            Utils.fail("error loading RBuiltin classes from " + getClass().getSimpleName() + " : " + ex);
        }
    }

    protected final RBuiltinBuilder load(Class<? extends RBuiltinNode> clazz) {
        RBuiltin builtin = clazz.getAnnotation(RBuiltin.class);
        String[] names = null;
        if (builtin != null) {
            int al = builtin.aliases().length;
            names = new String[1 + al];
            String name = builtin.name();
            names[0] = name;
            if (al > 0) {
                System.arraycopy(builtin.aliases(), 0, names, 1, al);
            }
        }
        return loadImpl(clazz, names, builtin);
    }

    void updateNames(RBuiltinFactory builtin, String[] oldNames, String[] newNames) {
        for (String oldName : oldNames) {
            builtins.remove(oldName);
        }

        for (String name : newNames) {
            RBuiltinFactory registered = builtins.get(name);
            if (registered != null && registered != builtin) {
                throw new RuntimeException("Duplicate builtin " + name + " defined.");
            }
            putBuiltin(name, builtin);
        }
    }

    @SuppressWarnings("unchecked")
    private RBuiltinBuilder loadImpl(Class<? extends RBuiltinNode> clazz, String[] names, RBuiltin builtin) {
        if (!RBuiltinNode.class.isAssignableFrom(clazz)) {
            throw new RuntimeException(clazz.getName() + " is must be assignable to " + RBuiltinNode.class);
        }
        String[] aliases = names != null ? names : new String[0];
        NodeFactory<RBuiltinNode> nodeFactory;
        if (!RCustomBuiltinNode.class.isAssignableFrom(clazz)) {
            // normal builtin
            final String className = clazz.getName();
            String factoryClassName = className;
            if (className.contains("$")) {
                // nested class, Factory appear twice
                int dx = className.lastIndexOf('$');
                factoryClassName = className.substring(0, dx) + "Factory$" + className.substring(dx + 1);
            }
            factoryClassName += "Factory";
            try {
                nodeFactory = (NodeFactory<RBuiltinNode>) Class.forName(factoryClassName).getMethod("getInstance").invoke(null);
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException | ClassNotFoundException e) {
                throw new RuntimeException("Failed to load builtin " + clazz.getName(), e);
            }
        } else {
            // custom builtin
            if (Modifier.isAbstract(clazz.getModifiers())) {
                throw new RuntimeException("Custom builtin must not be abstract (builtin " + clazz.getName() + ").");
            }
            nodeFactory = new ReflectiveNodeFactory(clazz);
        }
        RBuiltinFactory factory = new RBuiltinFactory(aliases, builtin, nodeFactory, new Object[0], this);
        for (String name : factory.getBuiltinNames()) {
            if (builtins.containsKey(name)) {
                throw new RuntimeException("Duplicate builtin " + name + " defined.");
            }
            putBuiltin(name, factory);
        }
        return new RBuiltinBuilder(this, factory);
    }

    /**
     * A {@link NodeFactory} implementation used to create {@link RCustomBuiltinNode}s.
     * {@link #createNode(Object...)} uses {@link RBuiltinCustomConstructors} is maintained by hand.
     */
    private static class ReflectiveNodeFactory implements NodeFactory<RBuiltinNode> {

        private final Class<? extends RBuiltinNode> clazz;

        public ReflectiveNodeFactory(Class<? extends RBuiltinNode> clazz) {
            this.clazz = clazz;
        }

        public RBuiltinNode createNode(Object... arguments) {
            try {
                RBuiltinNode builtin = new RCustomBuiltinNode((RNode[]) arguments[0], (RBuiltinFactory) arguments[1], (String[]) arguments[2]);
                return RBuiltinCustomConstructors.createNode(clazz.getName(), builtin);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @SuppressWarnings("unused")
        public RBuiltinNode createNodeGeneric(RBuiltinNode thisNode) {
            throw new UnsupportedOperationException();
        }

        @SuppressWarnings("unchecked")
        public Class<RBuiltinNode> getNodeClass() {
            return (Class<RBuiltinNode>) clazz;
        }

        public List<List<Class<?>>> getNodeSignatures() {
            throw new UnsupportedOperationException();
        }

        public List<Class<? extends Node>> getExecutionSignature() {
            return Collections.emptyList();
        }
    }

}
