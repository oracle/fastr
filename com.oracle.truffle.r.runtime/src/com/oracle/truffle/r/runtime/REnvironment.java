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
 * Environments consist of a frame, or collection of named objects, and a pointer to an enclosing
 * environment.
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

    private interface InSearchList {

    }

    /**
     * Map is keyed by the simple name and, for packages, currently only contains the "package:xxx"
     * environment.
     */
    private static final Map<String, REnvironment> namedEnvironments = new HashMap<>();

    public static final String UNNAMED = "";

    private static final Empty emptyEnv = new Empty();
    private static Global globalEnv;
    private static Base basePackageEnv;
    private static NamespaceBase baseNamespaceEnv;
    private static Autoload autoloadEnv;
    private static String[] searchPath;

    private final REnvironment parent;
    private final String name;

    public static Empty emptyEnv() {
        return emptyEnv;
    }

    public static Global globalEnv() {
        assert globalEnv != null;
        return globalEnv;
    }

    public static Base baseEnv() {
        assert basePackageEnv != null;
        return basePackageEnv;
    }

    public static NamespaceBase baseNamespaceEnv() {
        assert baseNamespaceEnv != null;
        return baseNamespaceEnv;
    }

    public static Autoload autoloadEnv() {
        assert autoloadEnv != null;
        return autoloadEnv;
    }

    /**
     * Invoked on startup to setup the values of {@link #globalEnv} and {@link #basePackageEnv}.
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

    public static String[] searchPath() {
        if (searchPath == null) {
            searchPath = new String[]{".GlobalEnv", "Autoloads", "package:base"};
        }
        return searchPath;
    }

    protected REnvironment(REnvironment parent, String name) {
        this.parent = parent;
        this.name = name;
        if (!name.equals(UNNAMED) && this instanceof InSearchList) {
            namedEnvironments.put(name, this);
        }
    }

    public static REnvironment lookupByName(String name) {
        return namedEnvironments.get(name);
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

    public REnvironment getParent() {
        return parent;
    }

    /**
     * The "simple" name of the environment. E.g. "namespace:xxx" return "xxx". TODO Evidently this
     * can be changed using attributes.
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
     * Access to the "frame" component is subclass specific.
     */

    public abstract Object get(String key);

    public abstract void put(String key, Object value);

    public abstract RStringVector ls(boolean allNames, String pattern);

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
     * The environment for the "base" package.
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
     * The namespace:base environment.
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
     * The users workspace environment. The parent depends on the set of default packages loaded.
     */
    public static final class Global extends WithFrame implements InSearchList {

        private Global(REnvironment parent) {
            super(parent, "R_GlobalEnv");
        }

    }

    /**
     * {@link Function} environments are created when a function is defined see
     * {@code RFunctionDefinitonNode} and {@code RTruffleVisitor}. In that situation the
     * {@code parent} is the lexically enclosing environment. When a function is invoked a
     * {@link Function} environment may be created in response to the R {@code environment()} base
     * package function, but in this case the parent is the environment of the caller, which may
     * differ from the lexically enclosing environment (e.g. recursion).
     */
    public static final class Function extends WithFrame {

        private FrameDescriptor descriptor;

        public Function(REnvironment parent) {
            // function environments are not named
            super(parent, "");
            this.descriptor = new FrameDescriptor();
        }

        /**
         * Specifically for {@code ls()}, we don't care about the parent.
         */
        public static Function createLsCurrent(VirtualFrame frame) {
            Function result = new Function(null);
            result.setFrame(frame);
            return result;
        }

        public static Function create(REnvironment parent, PackedFrame frame) {
            Function result = new Function(parent);
            result.setFrame(frame.unpack());
            return result;
        }

        public FrameDescriptor getDescriptor() {
            return descriptor;
        }

    }

    /**
     * A named environment explicitly created with {@code new.env}.
     */
    public static final class User extends REnvironment {
        private Map<String, Object> map;

        public User(REnvironment parent, String name, int size) {
            super(parent, name);
            this.map = new LinkedHashMap<>(size);
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
     * to assign should cause an error.
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
