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
import com.oracle.truffle.api.impl.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.builtin.RBuiltin.LastParameterKind;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode.RCustomBuiltinNode;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.runtime.*;

/**
 * Intended to be subclassed by packages defining builtin functions. Much of the initialization is
 * done statically on startup but, currently, the parsing and generation of the snippet ASTs is
 * delayed until run-time.
 */
public abstract class RBuiltinPackage {
    private static final Map<String, String> snippetResources = new HashMap<>();
    private static TreeMap<String, RBuiltinFactory> builtins = new TreeMap<>();
    private static boolean snippetsLoaded;

    private static void putBuiltin(String name, RBuiltinFactory factory) {
        builtins.put(name, factory);
    }

    protected RBuiltinPackage() {
        String rSourceName = getName() + ".r";
        String content = Utils.getResourceAsString(getClass(), rSourceName, false);
        if (content != null) {
            snippetResources.put(rSourceName, content);
        }
    }

    public static RBuiltinFactory lookupByName(String methodName) {
        return builtins.get(methodName);
    }

    public TreeMap<String, RBuiltinFactory> getBuiltins() {
        return builtins;
    }

    public static void loadSnippets() {
        if (!snippetsLoaded) {
            for (Map.Entry<String, String> entry : snippetResources.entrySet()) {
                RLibraryLoader loader = new RLibraryLoader(entry.getKey(), entry.getValue());
                Map<String, FunctionExpressionNode.StaticFunctionExpressionNode> builtinDefs = loader.loadLibrary();
                for (Map.Entry<String, FunctionExpressionNode.StaticFunctionExpressionNode> def : builtinDefs.entrySet()) {
                    NodeFactory<RBuiltinNode> nodeFactory = new RSnippetNodeFactory(def.getValue());
                    RBuiltinFactory builtinFactory = new RBuiltinFactory(new String[]{def.getKey()}, LastParameterKind.VALUE, nodeFactory, new Object[0]);
                    putBuiltin(def.getKey(), builtinFactory);
                }
            }
            snippetsLoaded = true;
        }
    }

    public abstract String getName();

    @SuppressWarnings("unchecked")
    protected final void loadBuiltins() {
        try (BufferedReader r = new BufferedReader(new InputStreamReader(ResourceHandlerFactory.getHandler().getResourceAsStream(getClass(), ".")))) {
            while (true) {
                String l = r.readLine();
                if (l == null) {
                    break;
                }
                if (l.endsWith(".class")) {
                    Class<?> clazz = Class.forName(getClass().getPackage().getName() + "." + l.replace(".class", ""));
                    if (clazz.getAnnotation(RBuiltin.class) != null) {
                        load((Class<? extends RBuiltinNode>) clazz);
                    }
                }
            }
        } catch (IOException | ClassNotFoundException ex) {
            Utils.fail("error loading RBuiltIn classes from " + getClass().getSimpleName() + " : " + ex);
        }

    }

    protected final RBuiltinBuilder load(Class<? extends RBuiltinNode> clazz) {
        RBuiltin builtin = clazz.getAnnotation(RBuiltin.class);
        String[] aliases = null;
        LastParameterKind lastParameterKind = LastParameterKind.VALUE;
        if (builtin != null) {
            aliases = builtin.value();
            lastParameterKind = builtin.lastParameterKind();
        }
        return loadImpl(clazz, aliases, lastParameterKind);
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
    private RBuiltinBuilder loadImpl(Class<? extends RBuiltinNode> clazz, String[] names, LastParameterKind lastParameterKind) {
        if (!RBuiltinNode.class.isAssignableFrom(clazz)) {
            throw new RuntimeException(clazz.getName() + " is must be assignable to " + RBuiltinNode.class);
        }
        String[] aliases = names != null ? names : new String[0];
        NodeFactory<RBuiltinNode> nodeFactory;
        if (!RCustomBuiltinNode.class.isAssignableFrom(clazz)) {
            // normal builtin
            try {
                nodeFactory = (NodeFactory<RBuiltinNode>) Class.forName(clazz.getCanonicalName() + "Factory").getMethod("getInstance").invoke(null);
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
        RBuiltinFactory factory = new RBuiltinFactory(aliases, lastParameterKind, nodeFactory, new Object[0]);
        for (String name : factory.getBuiltinNames()) {
            if (builtins.containsKey(name)) {
                throw new RuntimeException("Duplicate builtin " + name + " defined.");
            }
            putBuiltin(name, factory);
        }
        return new RBuiltinBuilder(this, factory);
    }

    private static class ReflectiveNodeFactory implements NodeFactory<RBuiltinNode> {

        private final Class<? extends RBuiltinNode> clazz;

        public ReflectiveNodeFactory(Class<? extends RBuiltinNode> clazz) {
            this.clazz = clazz;
        }

        public RBuiltinNode createNode(Object... arguments) {
            try {
                RBuiltinNode builtin = new RCustomBuiltinNode((RNode[]) arguments[0], (RBuiltinFactory) arguments[1]);
                return RBuiltinCustomConstructors.createNode(clazz.getName(), builtin);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

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
            return Arrays.<Class<? extends Node>> asList(RNode.class);
        }

    }

    private static class RSnippetNodeFactory implements NodeFactory<RBuiltinNode> {

        private final FunctionExpressionNode.StaticFunctionExpressionNode function;
        private final Class<? extends RBuiltinNode> clazz;
        private final int nargs;

        public RSnippetNodeFactory(FunctionExpressionNode.StaticFunctionExpressionNode function) {
            this.function = (FunctionExpressionNode.StaticFunctionExpressionNode) function.copy();
            clazz = RBuiltinNode.RSnippetNode.class;
            nargs = ((FunctionDefinitionNode) ((DefaultCallTarget) function.getFunction().getTarget()).getRootNode()).getParameterCount();
        }

        @Override
        public RBuiltinNode createNode(Object... arguments) {
            RBuiltinNode builtin = new RBuiltinNode.RSnippetNode((RNode[]) arguments[0], (RBuiltinFactory) arguments[1], function);
            return builtin;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Class<RBuiltinNode> getNodeClass() {
            return (Class<RBuiltinNode>) clazz;
        }

        @Override
        public List<List<Class<?>>> getNodeSignatures() {
            throw new UnsupportedOperationException();
        }

        @Override
        @SuppressWarnings("unchecked")
        public List<Class<? extends Node>> getExecutionSignature() {
            Class<? extends Node>[] sig = new Class[nargs];
            Arrays.fill(sig, RNode.class);
            return Arrays.<Class<? extends Node>> asList(sig);
        }

    }

}
