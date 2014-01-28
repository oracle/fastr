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
/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.r.nodes.builtin;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.*;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.impl.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.builtin.RBuiltin.LastParameterKind;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode.RCustomBuiltinNode;
import com.oracle.truffle.r.nodes.function.*;

/**
 * Intended to be subclassed by definitions of builtin functions.
 */
public abstract class RPackage {
    private final TreeMap<String, RBuiltinFactory> builtins = new TreeMap<>();

    protected RPackage() {
        loadSnippets();
    }

    public final RBuiltinFactory lookupByName(String methodName) {
        return builtins.get(methodName);
    }

    public final Collection<RBuiltinFactory> lookup() {
        return Collections.unmodifiableCollection(builtins.values());
    }

    public TreeMap<String, RBuiltinFactory> getBuiltins() {
        return builtins;
    }

    public final void loadSnippets() {
        String rSourceName = getName() + ".r";
        URL rSource = getClass().getResource(rSourceName);
        if (rSource != null) {
            RLibraryLoader loader = new RLibraryLoader(new File(rSource.getPath()));
            Map<String, FunctionExpressionNode.StaticFunctionExpressionNode> builtinDefs = loader.loadLibrary();
            for (Map.Entry<String, FunctionExpressionNode.StaticFunctionExpressionNode> def : builtinDefs.entrySet()) {
                NodeFactory<RBuiltinNode> nodeFactory = new RSnippetNodeFactory(def.getValue());
                RBuiltinFactory builtinFactory = new RBuiltinFactory(new String[]{def.getKey()}, LastParameterKind.VALUE, nodeFactory, new Object[0]);
                builtins.put(def.getKey(), builtinFactory);
            }
        }
    }

    public abstract String getName();

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
            builtins.put(name, builtin);
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
            try {
                Constructor<? extends RBuiltinNode> constructor = clazz.getConstructor(RBuiltinNode.class);
                constructor.setAccessible(true);
                nodeFactory = new ReflectiveNodeFactory(clazz, constructor);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException("No constructor with RBuiltinNode found for custom builtin " + clazz.getName() + ".");
            } catch (SecurityException e) {
                throw new RuntimeException(e);
            }
        }
        RBuiltinFactory factory = new RBuiltinFactory(aliases, lastParameterKind, nodeFactory, new Object[0]);
        for (String name : factory.getBuiltinNames()) {
            if (builtins.containsKey(name)) {
                throw new RuntimeException("Duplicate builtin " + name + " defined.");
            }
            builtins.put(name, factory);
        }
        return new RBuiltinBuilder(this, factory);
    }

    private static class ReflectiveNodeFactory implements NodeFactory<RBuiltinNode> {

        private final Class<? extends RBuiltinNode> clazz;
        private final Constructor<? extends RBuiltinNode> constructor;

        public ReflectiveNodeFactory(Class<? extends RBuiltinNode> clazz, Constructor<? extends RBuiltinNode> constructor) {
            this.clazz = clazz;
            this.constructor = constructor;
        }

        public RBuiltinNode createNode(Object... arguments) {
            try {
                RBuiltinNode builtin = new RCustomBuiltinNode((RNode[]) arguments[0], (RBuiltinFactory) arguments[1]);
                return constructor.newInstance(builtin);
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
        public RBuiltinNode createNodeGeneric(RBuiltinNode thisNode) {
            throw new UnsupportedOperationException();
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
