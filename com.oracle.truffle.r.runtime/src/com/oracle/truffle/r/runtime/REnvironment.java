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
package com.oracle.truffle.r.runtime;

import java.util.*;

import com.oracle.truffle.api.CompilerDirectives.SlowPath;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.runtime.data.*;

/**
 * Denotes an R {@code environment}.
 *
 * Abstractly, environments consist of a frame, or collection of named objects, and a pointer to an
 * enclosing environment.
 *
 * R environments (also) can be named or unnamed. {@code base} is an example of a named environment.
 * Environments associated with function invocations are unnamed. The {@code environmentName}
 * builtin returns "" for an unnamed environment. However, unnamed environments print using a unique
 * numeric id in the place where the name would appear for a named environment. This is finessed
 * using the {@link #getPrintNameHelper} method. Finally, environments on the {@code search} path
 * return a yet different name in the result of {@code search}, e.g. ".GlobalEnv", "package:base".
 *
 * Environments are used for many different things in R, including something close to a
 * {@link java.util.Map} created in R code using the {@code new.env} function. This is the only case
 * where the {@code size} parameter is specified. All the other instances of environments are
 * implicitly created by the virtual machine, for example, on function call.
 *
 * The different kinds of environments are implemented as subclasses. Currently the variation in
 * behavior regarding access to the "frame" is handled by static subclassing. If an environment can
 * change its "kind" at runtime, then this will have to be handled by delegation instead.
 *
 * Packages have three associated environments, "package:xxx", "imports:xxx" and "namespace:xxx",
 * for package "xxx". The {@code base} package is a special case in that it does not have an
 * "imports" environment. The parent of "package:base" is the empty environment, but the parent of
 * "namespace:base" is the global environment.
 */
public abstract class REnvironment {

    /**
     * Tagging interface indicating that the environment class is a component of
     * {@link #searchListEnvironments}.
     */
    private interface InSearchList {
    }

    /**
     * Map is keyed by the simple name and, for packages, currently only contains the "package:xxx"
     * environment.
     */
    private static final Map<String, REnvironment> searchListEnvironments = new HashMap<>();

    public static final String UNNAMED = "";

    private static final Empty emptyEnv = new Empty();
    private static Global globalEnv;
    private static Base basePackageEnv;
    private static NamespaceBase baseNamespaceEnv;
    private static Autoload autoloadEnv;
    private static String[] searchPath;

    private final REnvironment parent;
    private final String name;

    /**
     * Value returned by {@code emptyenv()}.
     */
    public static Empty emptyEnv() {
        return emptyEnv;
    }

    /**
     * Value returned by {@code globalenv()}.
     */
    public static Global globalEnv() {
        assert globalEnv != null;
        return globalEnv;
    }

    /**
     * Value returned by {@code baseenv()}. This is the "package:base" environment.
     */
    public static Base baseEnv() {
        assert basePackageEnv != null;
        return basePackageEnv;
    }

    /**
     * Value set in {@code .baseNameSpaceEnv} variable. This is the "namespace:base" environment.
     */
    public static NamespaceBase baseNamespaceEnv() {
        assert baseNamespaceEnv != null;
        return baseNamespaceEnv;
    }

    /**
     * Value set in the {@code .AutoloadEnv} variable.
     */
    public static Autoload autoloadEnv() {
        assert autoloadEnv != null;
        return autoloadEnv;
    }

    /**
     * Invoked on startup to setup the global values.
     *
     */
    public static void initialize(VirtualFrame globalFrame) {
        basePackageEnv = new Base();
        autoloadEnv = new Autoload();
        // The following is only true if there are no other default packages loaded.
        globalEnv = new Global(autoloadEnv);
        globalEnv.setFrame(globalFrame);
        baseNamespaceEnv = new NamespaceBase();
    }

    /**
     * Intended for use by unit test environment to reset the state of the global frame.
     */
    public static void resetGlobalEnv(VirtualFrame globalFrame) {
        globalEnv.setFrame(globalFrame);
    }

    /**
     * Data for the {@code search} function.
     */
    public static String[] searchPath() {
        if (searchPath == null) {
            searchPath = new String[]{".GlobalEnv", "Autoloads", "package:base"};
        }
        return searchPath;
    }

    public static REnvironment lookupByName(String name) {
        return searchListEnvironments.get(name);
    }

    public static REnvironment lookupBySearchName(String name) {
        if (name.equals(".GlobalEnv")) {
            return globalEnv();
        } else if (name.equals("Autoloads")) {
            return autoloadEnv();
        } else if (name.startsWith("package:")) {
            return lookupByName(name.replace("package:", ""));
        } else {
            return null;
        }
    }

    // end of static members

    protected REnvironment(REnvironment parent, String name) {
        this.parent = parent;
        this.name = name;
        if (!name.equals(UNNAMED) && this instanceof InSearchList) {
            searchListEnvironments.put(name, this);
        }
    }

    public REnvironment getParent() {
        return parent;
    }

    /**
     * The "simple" name of the environment. E.g. "namespace:xxx" return "xxx". TODO Evidently this
     * can be changed using attributes, which is not yet supported..
     */
    public String getName() {
        return name;
    }

    @SlowPath
    public String getPrintName() {
        return new StringBuilder("<environment: ").append(getPrintNameHelper()).append('>').toString();
    }

    protected String getPrintNameHelper() {
        if (name.equals(UNNAMED)) {
            return String.format("%#x", hashCode());
        } else {
            return getName();
        }
    }

    @Override
    @SlowPath
    public String toString() {
        return getPrintName();
    }

    /*
     * Access to the "frame" component is subclass specific. The default implementations all throw
     * RuntimeException.
     */

    /**
     * Return the value of object named {@code name} or {@code null} if not found.
     */
    public Object get(@SuppressWarnings({"hiding", "unused"}) String name) {
        throw notImplemented("get");
    }

    /**
     * Set the value of object named {@code name} to {@code value}. if {@code value == null},
     * effectively removes the name.
     */
    public void put(@SuppressWarnings({"hiding", "unused"}) String name, @SuppressWarnings("unused") Object value) {
        throw notImplemented("put");
    }

    /**
     * Used by {@code ls()} function.
     *
     * @param allNames {@code true} means include names starting with ".".
     * @param pattern regexp to match names returned.
     */
    public RStringVector ls(boolean allNames, String pattern) {
        throw notImplemented("ls");
    }

    private RuntimeException notImplemented(String method) {
        return new RuntimeException(getClass().getName() + "." + method + " called but not implemented");
    }

    /**
     * Helper class that encapsulates access to actual execution frames.
     */
    private static class WithFrame extends REnvironment {

        private MaterializedFrame frame;

        WithFrame(REnvironment parent, String name) {
            super(parent, name);
        }

        void setFrame(Frame frame) {
            this.frame = frame.materialize();
        }

        @Override
        public Object get(String name) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void put(String name, Object value) {
            // TODO Auto-generated method stub

        }

        @Override
        public RStringVector ls(boolean allNames, String pattern) {
            // TODO support pattern
            FrameDescriptor fd = frame.getFrameDescriptor();
            String[] names = fd.getIdentifiers().toArray(RRuntime.STRING_ARRAY_SENTINEL);
            int undefinedIdentifiers = 0;
            for (int i = 0; i < names.length; ++i) {
                if (frame.getValue(fd.findFrameSlot(names[i])) == null) {
                    names[i] = null;
                    ++undefinedIdentifiers;
                }
            }
            String[] definedNames = new String[names.length - undefinedIdentifiers];
            int j = 0;
            for (int i = 0; i < names.length; ++i) {
                if (names[i] != null) {
                    definedNames[j++] = names[i];
                }
            }
            if (!allNames) {
                definedNames = removeHiddenNames(definedNames);
            }
            return RDataFactory.createStringVector(definedNames, RDataFactory.COMPLETE_VECTOR);
        }
    }

    /**
     * The environment for the {@code package:base} package.
     */
    private static class Base extends WithFrame implements InSearchList {

        private Base() {
            this(emptyEnv);
        }

        protected Base(REnvironment parent) {
            super(parent, "base");
        }
    }

    /**
     * The {@code namespace:base} environment.
     */
    private static final class NamespaceBase extends WithFrame {
        private NamespaceBase() {
            super(globalEnv, "base");
        }

        @Override
        protected String getPrintNameHelper() {
            return "namespace:" + getName();
        }

    }

    /**
     * The users workspace environment (global). The parent depends on the set of default packages
     * loaded.
     */
    public static final class Global extends WithFrame implements InSearchList {

        private Global(REnvironment parent) {
            super(parent, "R_GlobalEnv");
        }

    }

    /**
     * Denotes an environment associated with a function definition during AST building.
     *
     * {@link StaticFunction} environments are created when a function is defined see
     * {@code RFunctionDefinitonNode} and {@code RTruffleVisitor}. In that situation the
     * {@code parent} is the lexically enclosing environment and there is no associated frame.
     */

    public static final class StaticFunction extends REnvironment {
        private FrameDescriptor descriptor;

        public StaticFunction(REnvironment parent) {
            // function environments are not named
            super(parent, "");
            this.descriptor = new FrameDescriptor();
        }

        public FrameDescriptor getDescriptor() {
            return descriptor;
        }

    }

    /**
     * When a function is invoked a {@link DynamicFunction} environment may be created in response
     * to the R {@code environment()} base package function, and it will have an associated frame.
     */
    public static final class DynamicFunction extends WithFrame {

        public DynamicFunction(REnvironment parent) {
            super(parent, "");
        }

        /**
         * Specifically for {@code ls()}, we don't care about the parent, as the use is transient.
         */
        public static DynamicFunction createLsCurrent(VirtualFrame frame) {
            DynamicFunction result = new DynamicFunction(null);
            result.setFrame(frame);
            return result;
        }

        public static DynamicFunction create(REnvironment parent, PackedFrame frame) {
            DynamicFunction result = new DynamicFunction(parent);
            result.setFrame(frame.unpack());
            return result;
        }

    }

    /**
     * A named environment explicitly created with {@code new.env}.
     */
    public static final class User extends REnvironment {
        private Map<String, Object> map;

        public User(REnvironment parent, String name, int size) {
            super(parent, name);
            this.map = newHashMap(size);
        }

        @SlowPath
        private static LinkedHashMap<String, Object> newHashMap(int size) {
            return size == 0 ? new LinkedHashMap<>() : new LinkedHashMap<>(size);
        }

        @Override
        public Object get(String key) {
            return map.get(key);
        }

        @Override
        public void put(String key, Object value) {
            map.put(key, value);
        }

        @Override
        public RStringVector ls(boolean allNames, String pattern) {
            String[] names = map.keySet().toArray(RRuntime.STRING_ARRAY_SENTINEL);
            if (!allNames) {
                names = removeHiddenNames(names);
            }
            return RDataFactory.createStringVector(names, RDataFactory.COMPLETE_VECTOR);
        }
    }

    private static String[] removeHiddenNames(String[] names) {
        int hiddenCount = 0;
        for (String name : names) {
            if (name.charAt(0) == '.') {
                hiddenCount++;
            }
        }
        if (hiddenCount > 0) {
            String[] newNames = new String[names.length - hiddenCount];
            int i = 0;
            for (String name : names) {
                if (name.charAt(0) == '.') {
                    continue;
                } else {
                    newNames[i++] = name;
                }
            }
            return newNames;
        }
        return names;
    }

    /**
     * A placeholder for the package autload mechanism.
     */
    private static final class Autoload extends REnvironment implements InSearchList {
        Autoload() {
            super(baseEnv(), "");
        }

        @Override
        public Object get(String key) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void put(String key, Object value) {
            // TODO Auto-generated method stub

        }

        @Override
        public RStringVector ls(boolean allNames, String pattern) {
            // TODO Auto-generated method stub
            return null;
        }
    }

    /**
     * The empty environment has no runtime state and so can be allocated statically. TODO Attempts
     * to assign should cause an error, if not prevented in caller. TODO check.
     */
    private static final class Empty extends REnvironment implements InSearchList {

        private Empty() {
            super(null, "R_EmptyEnv");
        }

        @Override
        public Object get(String key) {
            return null;
        }

        @Override
        public void put(String key, Object value) {
            // empty
        }

        @Override
        public RStringVector ls(boolean allNames, String pattern) {
            return RDataFactory.createEmptyStringVector();
        }

    }

}
