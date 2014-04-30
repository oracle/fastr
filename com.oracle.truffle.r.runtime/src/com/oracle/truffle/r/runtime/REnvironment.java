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
import java.util.stream.*;

import com.oracle.truffle.api.CompilerDirectives.SlowPath;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.runtime.data.*;

/**
 * Denotes an R {@code environment}.
 *
 * Abstractly, environments consist of a frame (collection of named objects), and a pointer to an
 * enclosing environment.
 *
 * R environments can be named or unnamed. {@code base} is an example of a named environment.
 * Environments associated with function invocations are unnamed. The {@code environmentName}
 * builtin returns "" for an unnamed environment. However, unnamed environments print using a unique
 * numeric id in the place where the name would appear for a named environment. This is finessed
 * using the {@link #getPrintNameHelper} method. Further, environments on the {@code search} path
 * return a yet different name in the result of {@code search}, e.g. ".GlobalEnv", "package:base",
 * which is handled via {@link #getSearchName()}. Finally, environments can be given names using the
 * {@code attr} function, and (then) they print differently again. Of the default set of
 * environments, only "Autoloads" has a {@code name} attribute.
 * <p>
 * Environments can also be locked preventing any bindings from being added or removed. N.B. the
 * empty environment can't be assigned to but is not locked (see GnuR). Further, individual bindings
 * within an environment can be locked, although they can be removed unless the environment is also
 * locked.
 * <p>
 * Environments are used for many different things in R, including something close to a
 * {@link java.util.Map} created in R code using the {@code new.env} function. This is the only case
 * where the {@code size} parameter is specified. All the other instances of environments are
 * implicitly created by the virtual machine, for example, on function call.
 * <p>
 * The different kinds of environments are implemented as subclasses. The variation in behavior
 * regarding access to the "frame" is handled by delegation to an instance of {@link FrameAccess}.
 * <p>
 * Packages have three associated environments, "package:xxx", "imports:xxx" and "namespace:xxx",
 * for package "xxx". The {@code base} package is a special case in that it does not have an
 * "imports" environment. The parent of "package:base" is the empty environment, but the parent of
 * "namespace:base" is the global environment.
 *
 */
public abstract class REnvironment {
    public enum PackageKind {
        PACKAGE,
        IMPORTS,
        NAMESPACE
    }

    /**
     * Tagging interface that indicates this is a "package" environment.
     */
    private interface IsPackage {

    }

    public static class PutException extends Exception {
        private static final long serialVersionUID = 1L;

        PutException(String message) {
            super(message);
        }
    }

    /**
     * Access to the frame component, handled by delegation in {@link REnvironment}. The default
     * implementation throws an exception for all calls and is used in the FunctionDefinition
     * environment which has no associated frame.
     */
    private static class FrameAccess {
        /**
         * Return the value of object named {@code name} or {@code null} if not found.
         */
        Object get(@SuppressWarnings("unused") String key) {
            throw notImplemented("get");
        }

        /**
         * Set the value of object named {@code name} to {@code value}. if {@code value == null},
         * effectively removes the name.
         *
         * @throws PutException if the binding is locked
         */
        @SuppressWarnings("unused")
        void put(String key, Object value) throws REnvironment.PutException {
            throw notImplemented("put");
        }

        /**
         * Remove binding.
         */
        void rm(@SuppressWarnings("unused") String key) {
            throw notImplemented("rm");
        }

        @SuppressWarnings("unused")
        RStringVector ls(boolean allNames, String pattern) {
            throw notImplemented("ls");
        }

        void lockBindings() {
            throw notImplemented("lockBindings");
        }

        /**
         * Disallow updates to {@code key}.
         */
        void lockBinding(@SuppressWarnings("unused") String key) {
            throw notImplemented("lockBinding");
        }

        /**
         * Allow updates to (previously locked) {@code key}.
         */
        void unlockBinding(@SuppressWarnings("unused") String key) {
            throw notImplemented("unlockBinding");
        }

        boolean bindingIsLocked(@SuppressWarnings("unused") String key) {
            throw notImplemented("bindingIsLocked");
        }

        MaterializedFrame getFrame() {
            throw notImplemented("getFrame");
        }

        private static RuntimeException notImplemented(String methodName) {
            return new RuntimeException("FrameAccess method '" + methodName + "' not implemented");
        }

    }

    /**
     * This adapter class handles the locking of bindings, but has null implementations of the basic
     * methods, which must be overridden by a subclass, while calling {@code super.method}
     * appropriately to invoke the locking logic.
     */
    private static class FrameAccessBindingsAdapter extends FrameAccess {
        /**
         * Records which bindings are locked. In normal use we don't expect any bindings to be
         * locked so this set is allocated lazily.
         */
        protected Set<String> lockedBindings;

        @Override
        Object get(String key) {
            return null;
        }

        @Override
        void put(String key, Object value) throws REnvironment.PutException {
            if (lockedBindings != null && lockedBindings.contains(key)) {
                throw createPutException(key);
            }
        }

        @Override
        void rm(String key) {
            if (lockedBindings != null) {
                lockedBindings.remove(key);
            }
        }

        @Override
        RStringVector ls(boolean allNames, String pattern) {
            return RDataFactory.createEmptyStringVector();
        }

        @Override
        @SlowPath
        void lockBindings() {
            Set<String> bindings = getBindingsForLock();
            if (bindings != null) {
                for (String binding : bindings) {
                    lockBinding(binding);
                }
            }
        }

        protected Set<String> getBindingsForLock() {
            return null;
        }

        @Override
        @SlowPath
        void lockBinding(String key) {
            if (lockedBindings == null) {
                lockedBindings = new HashSet<>();
            }
            lockedBindings.add(key);
        }

        @SlowPath
        PutException createPutException(String key) {
            return new PutException("cannot change value of locked binding for '" + key + "'");
        }

        @Override
        void unlockBinding(String key) {
            if (lockedBindings != null) {
                lockedBindings.remove(key);
            }
        }

        @Override
        boolean bindingIsLocked(String key) {
            return lockedBindings != null && lockedBindings.contains(key);
        }

        @Override
        MaterializedFrame getFrame() {
            return null;
        }
    }

    private static final FrameAccess defaultFrameAccess = new FrameAccessBindingsAdapter();
    private static final FrameAccess noFrameAccess = new FrameAccess();

    public static final String UNNAMED = "";

    private static final Empty emptyEnv = new Empty();
    private static Global globalEnv;
    private static PackageBase basePackageEnv;
    private static NamespaceBase baseNamespaceEnv;
    private static Autoload autoloadEnv;
    /**
     * The environments returned by the R {@code search} function.
     */
    private static ArrayList<REnvironment> searchPath;

    private REnvironment parent;
    private final String name;
    private final FrameAccess frameAccess;
    private Map<String, Object> attributes;
    private boolean locked;

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
     * Check whether the given frame is indeed the frame stored in the global environment.
     */
    public static boolean isGlobalEnvFrame(MaterializedFrame frame) {
        FrameSlot idSlot = frame.getFrameDescriptor().findFrameSlot(Global.GLOBAL_ENV_ID);
        if (idSlot == null) {
            return false;
        }
        try {
            return frame.getObject(idSlot) == Global.GLOBAL_ENV_ID;
        } catch (FrameSlotTypeException fste) {
            return false;
        }
    }

    /**
     * Value returned by {@code baseenv()}. This is the "package:base" environment.
     */
    public static PackageBase baseEnv() {
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
     */
    public static void initialize(VirtualFrame globalFrame, VirtualFrame baseFrame) {
        basePackageEnv = new PackageBase(baseFrame);
        autoloadEnv = new Autoload();
        // The following is only true if there are no other default packages loaded.
        globalEnv = new Global(autoloadEnv, globalFrame);
        baseNamespaceEnv = new NamespaceBase(basePackageEnv);
        // set up the initial search path
        searchPath = new ArrayList<>();
        REnvironment env = globalEnv;
        do {
            searchPath.add(env);
            env = env.parent;
        } while (env != emptyEnv);
    }

    /**
     * Intended for use by unit test environment to reset the global environment to a clean state.
     */
    public static void resetGlobalEnv(VirtualFrame globalFrame) {
        globalEnv = new Global(globalEnv.getParent(), globalFrame);
    }

    /**
     * Data for the {@code search} function.
     */
    public static String[] searchPath() {
        String[] result = new String[searchPath.size()];
        for (int i = 0; i < searchPath.size(); i++) {
            REnvironment env = searchPath.get(i);
            result[i] = env.getSearchName();
        }
        return result;
    }

    public static REnvironment lookupOnSearchPath(String name) {
        for (REnvironment env : searchPath) {
            String searchName = env.getSearchName();
            if (searchName.equals(name)) {
                return env;
            }
        }
        return null;
    }

    @SlowPath
    public static String packageQualName(PackageKind packageKind, String packageName) {
        StringBuffer sb = new StringBuffer();
        sb.append(packageKind.name().toLowerCase());
        sb.append(':');
        sb.append(packageName);
        return sb.toString();
    }

    // end of static members

    /**
     * The basic constructor; just assigns the essential fields.
     */
    protected REnvironment(REnvironment parent, String name, FrameAccess frameAccess) {
        this.parent = parent;
        this.name = name;
        this.frameAccess = frameAccess;
    }

    /**
     * An environment associated with a {@link VirtualFrame} where is it is important to establish
     * the parent environment's frame as the enclosing frame in {@code frame} <i>before</i> it is
     * materialised.
     */
    protected REnvironment(REnvironment parent, String name, VirtualFrame frame) {
        this(parent, name, setEnclosingHelper(parent, frame));
    }

    /**
     * Helper method to comply with constructor ordering rules.
     */
    private static FrameAccess setEnclosingHelper(REnvironment parent, VirtualFrame frame) {
        RArguments.setEnclosingFrame(frame, parent.getFrame());
        return new TruffleFrameAccess(frame.materialize());
    }

    /**
     * An environment associated with an already materialized frame.
     */
    protected REnvironment(REnvironment parent, String name, MaterializedFrame frame) {
        this(parent, name, new TruffleFrameAccess(frame));
    }

    public REnvironment getParent() {
        return parent;
    }

    public void setParent(REnvironment env) {
        parent = env;
    }

    /**
     * The "simple" name of the environment. For "package:xxx", "namespace:xxx", "imports:xxx", this
     * is "xxx". If the environment has been given a "name" attribute, then it is that value. This
     * is the value returned by the R {@code environmentName} function.
     */
    public String getName() {
        String attrName = attributes == null ? null : (String) attributes.get(RRuntime.NAMES_ATTR_KEY);
        return attrName != null ? attrName : name;
    }

    /**
     * The "print" name of an environment, i.e. what is output for {@code print(env)}.
     */
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

    /**
     * Name returned by the {@code search()} function. The default is just the simple name, but some
     * environments
     */
    protected String getSearchName() {
        String result = getName();
        if (this instanceof IsPackage) {
            result = "package:" + result;
        }
        return result;
    }

    /**
     * Return the {@link MaterializedFrame} associated with this environment, or {@code null} if
     * there is none.
     */
    public MaterializedFrame getFrame() {
        return frameAccess.getFrame();
    }

    public void lock(boolean bindings) {
        locked = true;
        if (bindings) {
            frameAccess.lockBindings();
        }
    }

    public boolean isLocked() {
        return locked;
    }

    public Object get(String key) {
        return frameAccess.get(key);
    }

    public void put(String key, Object value) throws PutException {
        if (locked) {
            // if the binding exists already, can try to update it
            if (frameAccess.get(key) == null) {
                throw new PutException("cannot add bindings to a locked environment");
            }
        }
        frameAccess.put(key, value);
    }

    public void rm(String key) throws PutException {
        if (locked) {
            throw new PutException("cannot remove bindings from a locked environment");
        }
        frameAccess.rm(key);
    }

    public RStringVector ls(boolean allNames, String pattern) {
        return frameAccess.ls(allNames, pattern);
    }

    public void lockBinding(String key) {
        frameAccess.lockBinding(key);

    }

    public void unlockBinding(String key) {
        frameAccess.unlockBinding(key);

    }

    public boolean bindingIsLocked(String key) {
        return frameAccess.bindingIsLocked(key);
    }

    @SlowPath
    public void setAttr(String attrName, Object value) {
        if (attributes == null) {
            attributes = new HashMap<>();
        }
        attributes.put(attrName, value);
    }

    @SlowPath
    public void removeAttr(String attrName) {
        if (attributes != null) {
            attributes.remove(attrName);
        }
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    @SlowPath
    public String toString() {
        return getPrintName();
    }

    /**
     * Variant of {@link FrameAccess} that provides access to an actual Truffle execution frame.
     */
    private static class TruffleFrameAccess extends FrameAccessBindingsAdapter {

        private MaterializedFrame frame;

        TruffleFrameAccess(MaterializedFrame frame) {
            this.frame = frame;
        }

        @Override
        MaterializedFrame getFrame() {
            return frame;
        }

        @Override
        Object get(String key) {
            FrameDescriptor fd = frame.getFrameDescriptor();
            FrameSlot slot = fd.findFrameSlot(key);
            if (slot == null) {
                return null;
            } else {
                return frame.getValue(slot);
            }
        }

        @Override
        void put(String key, Object value) throws PutException {
            // check locking
            super.put(key, value);
            FrameDescriptor fd = frame.getFrameDescriptor();
            FrameSlot slot = fd.findFrameSlot(key);
            if (slot != null) {
                frame.setObject(slot, value);
            } else {
                slot = fd.addFrameSlot(key, FrameSlotKind.Object);
                frame.setObject(slot, value);
            }
        }

        @Override
        void rm(String key) {
            super.rm(key);
        }

        @Override
        RStringVector ls(boolean allNames, String pattern) {
            // TODO support pattern
            FrameDescriptor fd = frame.getFrameDescriptor();
            String[] names = getStringIdentifiers(fd);
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

        @Override
        protected Set<String> getBindingsForLock() {
            // TODO Auto-generated method stub
            return null;
        }

        private static String[] getStringIdentifiers(FrameDescriptor fd) {
            return fd.getIdentifiers().stream().filter(e -> (e instanceof String)).collect(Collectors.toSet()).toArray(RRuntime.STRING_ARRAY_SENTINEL);
        }

    }

    /**
     * {@link PackageBase} and {@link NamespaceBase} share some characteristics that are implemented
     * in this adapter.
     */
    private static class BaseAdapter extends REnvironment {
        protected BaseAdapter(REnvironment parent, VirtualFrame frame) {
            super(parent, "base", frame);
        }

        protected BaseAdapter(REnvironment parent, PackageBase base) {
            super(parent, "base", new TruffleFrameAccess(base.getFrame()));
        }

        @Override
        public void rm(String key) throws PutException {
            throw new PutException("cannot remove variables from the " + getPrintNameHelper() + " environment");
        }
    }

    /**
     * The environment for the {@code package:base} package, which is in the search list.
     */
    private static class PackageBase extends BaseAdapter implements IsPackage {

        PackageBase(VirtualFrame frame) {
            super(emptyEnv, frame);
        }
    }

    /**
     * The {@code namespace:base} environment, which is <i>not</i> in the search list.
     */
    private static final class NamespaceBase extends BaseAdapter {
        private NamespaceBase(PackageBase base) {
            super(globalEnv, base);
        }

        @Override
        protected String getPrintNameHelper() {
            return "namespace:" + getName();
        }
    }

    /**
     * The users workspace environment (so called global). The parent depends on the set of default
     * packages loaded.
     */
    public static final class Global extends REnvironment {

        public static final Object GLOBAL_ENV_ID = new Object();

        private Global(REnvironment parent, VirtualFrame frame) {
            super(parent, "R_GlobalEnv", storeGlobalEnvID(frame));
        }

        @Override
        protected String getSearchName() {
            return ".GlobalEnv";
        }

        /**
         * The global environment ID is an object that is stored in the global environment frame and
         * is its own name.
         */
        private static VirtualFrame storeGlobalEnvID(VirtualFrame frame) {
            FrameDescriptor fd = frame.getFrameDescriptor();
            FrameSlot idSlot = fd.addFrameSlot(GLOBAL_ENV_ID);
            frame.setObject(idSlot, GLOBAL_ENV_ID);
            return frame;
        }

    }

    /**
     * When a function is invoked a {@link Function} environment may be created in response to the R
     * {@code environment()} base package function, and it will have an associated frame.
     */
    public static final class Function extends REnvironment {

        public Function(REnvironment parent, MaterializedFrame frame) {
            // function environments are not named
            super(parent, UNNAMED, frame);
        }

        /**
         * Specifically for {@code ls()}, we don't care about the parent, as the use is transient.
         */
        public static Function createLsCurrent(MaterializedFrame frame) {
            Function result = new Function(null, frame);
            return result;
        }

        public static Function create(REnvironment parent, MaterializedFrame frame) {
            Function result = new Function(parent, frame);
            return result;
        }

    }

    /**
     * Denotes an environment associated with a function definition during AST building.
     *
     * {@link FunctionDefinition} environments are created when a function is defined see
     * {@code RFunctionDefinitonNode} and {@code RTruffleVisitor}. In that situation the
     * {@code parent} is the lexically enclosing environment and there is no associated frame.
     */

    public static final class FunctionDefinition extends REnvironment {
        private FrameDescriptor descriptor;

        public FunctionDefinition(REnvironment parent) {
            // function environments are not named
            super(parent, UNNAMED, noFrameAccess);
            this.descriptor = new FrameDescriptor();
        }

        public FrameDescriptor getDescriptor() {
            return descriptor;
        }

    }

    /**
     * Variant of {@link FrameAccess} for {@link NewEnv} environments where the "frame" is a
     * {@link LinkedHashMap}.
     */
    private static class NewEnvFrameAccess extends FrameAccessBindingsAdapter {
        private final Map<String, Object> map;

        NewEnvFrameAccess(int size) {
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
        public void rm(String key) {
            super.rm(key);
            map.remove(key);
        }

        @SlowPath
        @Override
        public void put(String key, Object value) throws PutException {
            super.put(key, value);
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

        @Override
        protected Set<String> getBindingsForLock() {
            return map.keySet();
        }

    }

    /**
     * A named environment explicitly created with {@code new.env}.
     */
    public static final class NewEnv extends REnvironment {

        public NewEnv(REnvironment parent, String name, int size) {
            super(parent, name, new NewEnvFrameAccess(size));
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
     * A placeholder for the package autoload mechanism. N.B. Although "unnamed", it is given a name
     * with {@code attr} in GnuR.
     */
    private static final class Autoload extends REnvironment {
        Autoload() {
            super(baseEnv(), UNNAMED, baseEnv().getFrame());
            setAttr(RRuntime.NAMES_ATTR_KEY, "Autoloads");
        }

    }

    /**
     * The empty environment has no runtime state and so can be allocated statically. TODO Attempts
     * to assign should cause an R error, if not prevented in caller. TODO check.
     */
    private static final class Empty extends REnvironment {

        private Empty() {
            super(null, "R_EmptyEnv", defaultFrameAccess);
        }

        @Override
        public void put(String key, Object value) throws PutException {
            throw new PutException("cannot assign values in the empty environment");
        }

    }

}
