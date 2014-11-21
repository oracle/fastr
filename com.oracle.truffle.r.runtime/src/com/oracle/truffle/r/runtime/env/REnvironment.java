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
package com.oracle.truffle.r.runtime.env;

import java.util.*;
import java.util.regex.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.RError.RErrorException;
import com.oracle.truffle.r.runtime.RPackages.RPackage;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.env.frame.*;

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
 * regarding access to the "frame" is handled by delegation to an instance of
 * {@link REnvFrameAccess}. Conceptually, variables are searched for by starting in a given
 * environment and searching backwards through the "parent" chain. In practice, variables are
 * accessed in the Truffle environment using {@link Frame} instances which may, in some cases such
 * as compiled code, not even exist as actual objects. Therefore, we have to keep the name lookup in
 * the two worlds in sync. This is an issue during initialization, and when a new environment is
 * attached, cf. {@link #attach}.
 * <p>
 * Packages have three associated environments, "package:xxx", "imports:xxx" and "namespace:xxx",
 * for package "xxx". The {@code base} package is a special case in that it does not have an
 * "imports" environment. The parent of "package:base" is the empty environment, but the parent of
 * "namespace:base" is the global environment.
 *
 * Whereas R types generally use value semantics environments do not; they have reference semantics.
 * In particular in FastR, there is at exactly one environment created for any package frame and at
 * most one for a function frame, allowing equality to be tested using {@code ==}.
 *
 * TODO retire the {@code Package}, {@code Namespace} and {@code Imports} classes as they are only
 * used by the builtin packages, and will be completely redundant when they are loaded from
 * serialized package meta-data as will happen in due course.
 *
 */
public abstract class REnvironment extends RAttributeStorage implements RAttributable {
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

    public static class PutException extends RErrorException {
        private static final long serialVersionUID = 1L;

        public PutException(RError.Message msg, Object... args) {
            super(msg, args);
        }
    }

    private static final REnvFrameAccess defaultFrameAccess = new REnvFrameAccessBindingsAdapter();
    private static final REnvFrameAccess noFrameAccess = new REnvFrameAccess();

    public static final String UNNAMED = "";
    private static final String NAME_ATTR_KEY = "name";
    private static final String PATH_ATTR_KEY = "path";

    private static final Empty emptyEnv = new Empty();
    private static Global globalEnv;
    private static REnvironment initialGlobalEnvParent;
    private static Base baseEnv;
    private static Autoload autoloadEnv;
    private static REnvironment namespaceRegistry;

    /**
     * The environments returned by the R {@code search} function.
     */
    private static ArrayList<REnvironment> searchPath;

    protected REnvironment parent;
    private final String name;
    final REnvFrameAccess frameAccess;
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
     * Returns {@code true} iff {@code frame} is that associated with {@code env}. N.B. The
     * environment associated with the frame may be {@code null} as {@link Function} environments
     * are created lazily. However, we maintain the invariant that whenever a {@link Function}
     * environment is created the value is stored in the associated frame. Therefore {@code env}
     * could never match lazy {@code null}.
     */
    public static boolean isFrameForEnv(Frame frame, REnvironment env) {
        return RArguments.getEnvironment(frame) == env;
    }

    /**
     * Looks up the search path for an environment that is associated with {@code frame}.
     *
     * @param frame
     * @return the corresponding {@link REnvironment} or {@code null} if not found. If the
     *         environment is {@code base} the "namespace:base" instance is returned.
     */
    public static REnvironment lookupEnvForFrame(MaterializedFrame frame) {
        for (REnvironment env : searchPath) {
            if (isFrameForEnv(frame, env)) {
                if (env == baseEnv) {
                    return baseEnv.getNamespace();
                } else {
                    return env;
                }
            }
        }
        return null;
    }

    /**
     * Check whether the given frame is indeed the frame stored in the global environment.
     */
    public static boolean isGlobalEnvFrame(Frame frame) {
        return isFrameForEnv(frame, globalEnv);
    }

    /**
     * Value returned by {@code baseenv()}. This is the "package:base" environment.
     */
    public static Package baseEnv() {
        assert baseEnv != null;
        return baseEnv;
    }

    /**
     * Value set in {@code .baseNameSpaceEnv} variable. This is the "namespace:base" environment.
     */
    public static Namespace baseNamespaceEnv() {
        assert baseEnv != null;
        return baseEnv.getNamespace();
    }

    /**
     * Value set in the {@code .AutoloadEnv} variable.
     */
    public static Autoload autoloadEnv() {
        assert autoloadEnv != null;
        return autoloadEnv;
    }

    /**
     * Invoked on startup to setup the global values and package search path. Owing to the
     * restrictions on storing {@link VirtualFrame} instances, this method creates the
     * {@link VirtualFrame} instance(s) for the packages and evaluates any associated R code using
     * that frame and then installs it in the search path correctly so that Truffle code can locate
     * objects defined by the R code.
     *
     * @param globalFrame this is the anchor frame to which the package search path is attached
     * @param baseFrame this is for the base frame (we can't create it because our caller also needs
     *            to eval in it)
     */
    public static void baseInitialize(VirtualFrame globalFrame, VirtualFrame baseFrame) {
        // The base "package" is special, it has no "imports" and
        // its "namespace" parent is globalenv

        namespaceRegistry = new NewEnv(null);
        baseEnv = new Base(baseFrame);

        // autoload always next, has no R state
        autoloadEnv = new Autoload();
        globalEnv = new Global(autoloadEnv, globalFrame);
        initSearchList();

        // load base package first
        RContext.getEngine().loadDefaultPackage("base", baseFrame.materialize(), baseEnv);
    }

    public static void packagesInitialize(ArrayList<RPackage> rPackages) {
        // now load rPackages, we need a new VirtualFrame for each
        REnvironment pkgParent = autoloadEnv;
        for (RPackage rPackage : rPackages) {
            VirtualFrame pkgFrame = RRuntime.createNonFunctionFrame();
            Package pkgEnv = new Package(pkgParent, rPackage.name, pkgFrame, rPackage.path);
            RContext.getEngine().loadDefaultPackage(rPackage.name, pkgFrame.materialize(), pkgEnv);
            attach(2, pkgEnv);
            pkgParent = pkgEnv;
        }

        initialGlobalEnvParent = pkgParent;
        baseEnv.getNamespace().setParent(globalEnv);
        // set up the initial search path
    }

    private static void initSearchList() {
        searchPath = new ArrayList<>();
        REnvironment env = globalEnv;
        do {
            searchPath.add(env);
            env = env.parent;
        } while (env != emptyEnv);
    }

    /**
     * Intended for use by unit test environment to reset the environment to a clean state. We want
     * to reset the {@link #globalEnv}, and by extension {@link #searchPath} but not everything
     * else. This evidently depends on there not being destructive tests, and in particular any that
     * mess with the set of default packages.
     *
     */
    public static void resetForTest(VirtualFrame globalFrame) {
        globalEnv = new Global(initialGlobalEnvParent, globalFrame);
        // update .GlobalEnv
        try {
            baseEnv.put(".GlobalEnv", globalEnv);
        } catch (PutException ex) {
            Utils.fail("could not update .GlobalEnv");
        }
        initSearchList();
        // one more thing, namespace:base always has globalEnv as it's parent, so update that
        baseEnv.getNamespace().setParent(globalEnv);
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

    /**
     * Lookup an environment by name on the search path.
     *
     * @param name the name as it would appear in R the {@code search} function.
     * @return the environment or {@code null} if not found.
     */
    public static REnvironment lookupOnSearchPath(String name) {
        int i = lookupIndexOnSearchPath(name);
        return i <= 0 ? null : searchPath.get(i - 1);
    }

    /**
     * Lookup the index of an environment by name on the search path.
     *
     * @param name the name as it would appear in R the {@code search} function.
     * @return the index (1-based) or {@code 0} if not found.
     */
    public static int lookupIndexOnSearchPath(String name) {
        for (int i = 0; i < searchPath.size(); i++) {
            REnvironment env = searchPath.get(i);
            String searchName = env.getSearchName();
            if (searchName.equals(name)) {
                return i + 1;
            }
        }
        return 0;
    }

    public static REnvironment getNamespaceRegistry() {
        return namespaceRegistry;
    }

    public static void registerNamespace(String name, REnvironment env) {
        namespaceRegistry.safePut(name, env);
    }

    /**
     * Get the registered {@code namespace} environment {@code name}, or {@code null} if not found.
     * N.B. The package loading code in {@code namespace.R} uses a {code new.env} environment for a
     * namespace.
     */
    public static REnvironment getRegisteredNamespace(String name) {
        REnvironment pkgEnv = lookupOnSearchPath("package:" + name);
        if (pkgEnv == null) {
            return (REnvironment) namespaceRegistry.get(name);
        } else {
            if (pkgEnv instanceof Package) {
                return ((Package) pkgEnv).getNamespace();
            } else {
                // dynamically attached to search path
                return (REnvironment) namespaceRegistry.get(name);
            }
        }
    }

    /**
     * Attach (insert) an environment as position {@code pos} in the search path. TODO handle
     * packages
     *
     * @param pos position for insert, {@code pos >= 2}. As per GnuR, values beyond the index of
     *            "base" are truncated to the index before "base".
     */
    public static void attach(int pos, REnvironment env) {
        assert pos >= 2;
        // N.B. pos is 1-based
        int bpos = pos - 1;
        if (bpos > searchPath.size() - 1) {
            bpos = searchPath.size() - 1;
        }
        // Insert in the REnvironment search path, adjusting the parent fields appropriately
        // In the default case (pos == 2), envAbove is the Global env
        REnvironment envAbove = searchPath.get(bpos - 1);
        REnvironment envBelow = searchPath.get(bpos);
        env.parent = envBelow;
        envAbove.parent = env;
        searchPath.add(bpos, env);
        // Now must adjust the Frame world so that unquoted variable lookup works
        MaterializedFrame aboveFrame = envAbove.frameAccess.getFrame();
        MaterializedFrame envFrame = env.getFrame();
        RArguments.attachFrame(aboveFrame, envFrame);
    }

    public static class DetachException extends RErrorException {
        private static final long serialVersionUID = 1L;

        DetachException(RError.Message msg, Object... args) {
            super(msg, args);
        }
    }

    /**
     * Detach the environment at search position {@code pos}. TODO handle packages
     *
     * @param unload if {@code true} and env is a package, unload associated namespace
     * @param force the detach even if there are dependent packages
     * @return the {@link REnvironment} that was detached.
     */
    public static REnvironment detach(int pos, boolean unload, boolean force) throws DetachException {
        if (pos == searchPath.size()) {
            detachException(RError.Message.ENV_DETACH_BASE);
        }
        if (pos <= 0 || pos >= searchPath.size()) {
            detachException(RError.Message.ENV_SUBSCRIPT);
        }
        assert pos != 1; // explicitly checked in caller
        // N.B. pos is 1-based
        int bpos = pos - 1;
        REnvironment envAbove = searchPath.get(bpos - 1);
        REnvironment envToRemove = searchPath.get(bpos);
        envAbove.parent = envToRemove.parent;
        searchPath.remove(bpos);
        MaterializedFrame aboveFrame = envAbove.frameAccess.getFrame();
        RArguments.detachFrame(aboveFrame);
        if (envToRemove.frameAccess instanceof REnvMapFrameAccess) {
            ((REnvMapFrameAccess) envToRemove.frameAccess).detach();
        }
        return envToRemove;
    }

    @TruffleBoundary
    private static void detachException(RError.Message message) throws DetachException {
        throw new DetachException(message);
    }

    /**
     * Specifically for {@code ls()}, we don't care about the parent, as the use is transient.
     */
    public static REnvironment createLsCurrent(MaterializedFrame frame) {
        Function result = new Function(null, frame);
        return result;
    }

    /**
     * Converts a {@link Frame} to an {@link REnvironment}, which necessarily requires the frame to
     * be materialized.
     */
    public static REnvironment frameToEnvironment(MaterializedFrame frame) {
        REnvironment env = RArguments.getEnvironment(frame);
        if (env == null) {
            assert RArguments.getFunction(frame) != null;
            env = createEnclosingEnvironments(frame);
        }
        return env;
    }

    /**
     * When functions are defined, the associated {@code FunctionDefinitionNode} contains an
     * {@link FunctionDefinition} environment instance whose parent is the lexically enclosing
     * environment. This chain can be followed back to whichever "base" (i.e. non-function)
     * environment the outermost function was defined in, e.g. "global" or "base". The purpose of
     * this method is to create an analogous lexical parent chain of {@link Function} instances with
     * the correct {@link MaterializedFrame}.
     */
    @TruffleBoundary
    public static REnvironment createEnclosingEnvironments(MaterializedFrame frame) {
        REnvironment env = RArguments.getEnvironment(frame);
        if (env == null) {
            // parent is the env of the enclosing frame
            env = REnvironment.Function.create(createEnclosingEnvironments(RArguments.getEnclosingFrame(frame)), frame);
        }
        return env;
    }

    /**
     * Convert an {@link RList} to an {@link REnvironment}, which is needed in several builtins,
     * e.g. {@code substitute}.
     */
    public static REnvironment createFromList(RList list, REnvironment parent) {
        REnvironment result = new NewEnv(parent, 0);
        RStringVector names = (RStringVector) list.getNames();
        for (int i = 0; i < list.getLength(); i++) {
            try {
                result.put(names.getDataAt(i), list.getDataAt(i));
            } catch (PutException ex) {
                throw RError.error((SourceSection) null, ex);
            }
        }
        return result;
    }

    // END of static methods

    private static final String NAMESPACE_KEY = ".__NAMESPACE__.";

    /**
     * GnuR creates {@code Namespace} environments in {@code namespace.R} using {@code new.env} and
     * identifies them with the special element {@code .__NAMESPACE__.} which points to another
     * environment with a {@code spec} element. N.B. the {@code base} namespace does <b>not</b> have
     * a {@code .__NAMESPACE__.} entry.
     *
     * Currently, although this is scheduled to change, the "built-in" packages are created as
     * instances of {@link Namespace}. We fabricate a fake {@code .__NAMESPACE__.} entry to keep
     * {@code asNameSpace} in {@code namespace.R} happy.
     */
    public boolean isNamespaceEnv() {
        if (this instanceof Namespace) {
            return true;
        } else {
            RStringVector spec = getNamespaceSpec();
            return spec != null;
        }
    }

    /**
     * Another artefact of not loading builtin packages using the R machinery is the need to add any
     * defined functions to the "exports" environment of the namespace env. This matters for
     * qualified invocation, e.g. "package::func".
     */
    public void export() {
        if (this != baseEnv) {
            ((Package) this).populateExports();
        }
    }

    /**
     * Return the "spec" attribute of the "info" env in a namespace or {@code null} if not found.
     */
    private RStringVector getNamespaceSpec() {
        Object value = frameAccess.get(NAMESPACE_KEY);
        if (value instanceof REnvironment) {
            REnvironment info = (REnvironment) value;
            Object spec = info.frameAccess.get("spec");
            if ((spec != null) && spec instanceof RStringVector) {
                RStringVector infoVec = (RStringVector) spec;
                if (infoVec.getLength() > 0) {
                    return infoVec;
                }
            }
        }
        return null;
    }

    @TruffleBoundary
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
    protected REnvironment(REnvironment parent, String name, REnvFrameAccess frameAccess) {
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
        RArguments.setEnvironment(frame, this);
    }

    /**
     * Helper method to comply with constructor ordering rules.
     */
    private static REnvFrameAccess setEnclosingHelper(REnvironment parent, VirtualFrame frame) {
        RArguments.setEnclosingFrame(frame, parent.getFrame());
        // This call invokes frame.materialize();
        return new REnvTruffleFrameAccess(frame);
    }

    /**
     * An environment associated with an already materialized frame.
     */
    protected REnvironment(REnvironment parent, String name, MaterializedFrame frame) {
        this(parent, name, new REnvTruffleFrameAccess(frame));
    }

    public REnvironment getParent() {
        return parent;
    }

    /**
     * Explicity set the parent of an environment. TODO Change the enclosingFrame of (any)
     * associated Truffle frame
     */
    public void setParent(REnvironment env) {
        parent = env;
    }

    /**
     * The "simple" name of the environment. For "package:xxx", "namespace:xxx", "imports:xxx", this
     * is "xxx". If the environment has been given a "name" attribute, then it is that value. This
     * is the value returned by the R {@code environmentName} function.
     */
    public String getName() {
        String attrName = attributes == null ? null : (String) attributes.get(NAME_ATTR_KEY);
        return attrName != null ? attrName : name;
    }

    /**
     * The "print" name of an environment, i.e. what is output for {@code print(env)}.
     */
    @TruffleBoundary
    public String getPrintName() {
        return new StringBuilder("<environment: ").append(getPrintNameHelper()).append('>').toString();
    }

    protected String getPrintNameHelper() {
        String attrName = getName();
        if (name.equals(UNNAMED) && attrName.equals(UNNAMED)) {
            /*
             * namespaces are a special case; they have no name attribute, but they print with the
             * name which is buried.
             */
            if (frameAccess == noFrameAccess) {
                return "function def";
            } else {
                RStringVector spec = getNamespaceSpec();
                if (spec != null) {
                    return "namespace:" + spec.getDataAt(0);
                } else {
                    return String.format("%#x", hashCode());
                }
            }
        } else {
            return attrName;
        }
    }

    /**
     * Name returned by the {@code search()} function. The default is just the simple name, but
     * globalenv() is different.
     */
    protected String getSearchName() {
        String result = getName();
        return result;
    }

    /**
     * Return the {@link MaterializedFrame} associated with this environment, installing one if
     * there is none in the case of {@link NewEnv} environments.
     */
    public MaterializedFrame getFrame() {
        MaterializedFrame envFrame = frameAccess.getFrame();
        if (envFrame == null) {
            envFrame = getMaterializedFrame(this);
        }
        return envFrame;
    }

    /**
     * Ensures that {@code env} and all its parents have a {@link MaterializedFrame}. Used for
     * {@link NewEnv} environments that only need frames when they are used in {@code eval} etc.
     */
    @TruffleBoundary
    private static MaterializedFrame getMaterializedFrame(REnvironment env) {
        MaterializedFrame envFrame = env.frameAccess.getFrame();
        if (envFrame == null && env.parent != null) {
            MaterializedFrame parentFrame = getMaterializedFrame(env.parent);
            envFrame = new REnvMaterializedFrame((UsesREnvMap) env);
            RArguments.setEnclosingFrame(envFrame, parentFrame);
        }
        return envFrame;
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
                throw new PutException(RError.Message.ENV_ADD_BINDINGS);
            }
        }
        frameAccess.put(key, value);
    }

    public void safePut(String key, Object value) {
        try {
            put(key, value);
        } catch (PutException ex) {
            Utils.fail("exception in safePut");
        }
    }

    public void rm(String key) throws PutException {
        if (locked) {
            throw new PutException(RError.Message.ENV_REMOVE_BINDINGS);
        }
        frameAccess.rm(key);
    }

    public RStringVector ls(boolean allNames, Pattern pattern) {
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

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        return getPrintName();
    }

    /**
     * Denotes the "namespace:xxx" environment of an R package. The "parent" is the associated
     * "imports" environment, except for "base" where it is globalEnv
     */
    private static final class Namespace extends REnvironment {
        private Namespace(REnvironment parent, String name, REnvFrameAccess frameAccess) {
            super(parent, name, frameAccess);
            namespaceRegistry.safePut(name, this);
        }

        @Override
        protected String getPrintNameHelper() {
            return "namespace:" + getName();
        }
    }

    /**
     * Denotes the "imports:xxx" environment of an R package.
     */
    private static final class Imports extends REnvironment {

        private Imports(String name, REnvFrameAccess frameAccess) {
            super(baseEnv.getNamespace(), UNNAMED, frameAccess);
            setAttr(NAME_ATTR_KEY, "imports:" + name);
        }
    }

    /**
     * Denotes an environment associated with an R package. This represents the "package:xxx"; the
     * "namespace:xxx" and "imports:xxx" environments are stored as fields of this instance.
     */
    public static class Package extends REnvironment implements IsPackage {
        private final Imports importsEnv;
        private final Namespace namespaceEnv;

        private Package(REnvironment parent, String name, VirtualFrame frame, String path) {
            // This sets up the EnvFrameAccess instance, which is shared by the
            // Namespace (and Imports?) environments.
            super(parent, name, frame);
            this.importsEnv = new Imports(name, this.frameAccess);
            this.namespaceEnv = new Namespace(this.importsEnv, name, this.frameAccess);
            this.namespaceEnv.safePut(NAMESPACE_KEY, createNAMESPACE(name));
            setName(name);
            setPath(path);
        }

        private void setName(String name) {
            setAttr(NAME_ATTR_KEY, "package:" + name);
        }

        private void setPath(String path) {
            setAttr(PATH_ATTR_KEY, path);
        }

        private static REnvironment createNAMESPACE(String name) {
            REnvironment ns = new NewEnv(baseEnv, 0);
            String[] data = new String[]{name, RVersionNumber.FULL};
            String[] names = new String[]{"name", "version"};
            RStringVector spec = RDataFactory.createStringVector(data, RDataFactory.COMPLETE_VECTOR, RDataFactory.createStringVector(names, RDataFactory.COMPLETE_VECTOR));
            ns.safePut("spec", spec);
            REnvironment exports = new NewEnv(baseEnv, 0);
            // exports needs to be populated for "name::function" to work
            ns.safePut("exports", exports);
            ns.safePut("lazydata", new NewEnv(baseEnv, 0));
            return ns;
        }

        void populateExports() {
            REnvironment ns = (REnvironment) namespaceEnv.frameAccess.get(NAMESPACE_KEY);
            REnvironment exports = (REnvironment) ns.frameAccess.get("exports");
            RStringVector names = this.frameAccess.ls(true, null);
            for (int i = 0; i < names.getLength(); i++) {
                try {
                    String name = names.getDataAt(i);
                    if (!name.equals("NAMESPACE_KEY")) {
                        exports.frameAccess.put(name, RDataFactory.createStringVector(name));
                    }
                } catch (PutException ex) {
                    RInternalError.shouldNotReachHere();
                }
            }
        }

        /**
         * Constructor for {@link Base}. During initialization the parent is emptyEnv. Ultimately it
         * will be set to globalEnv.
         */
        protected Package(VirtualFrame frame) {
            super(emptyEnv, "base", frame);
            this.importsEnv = null;
            this.namespaceEnv = new Namespace(emptyEnv, "base", this.frameAccess);
            RArguments.setEnvironment(frame, this.namespaceEnv);
        }

        public Namespace getNamespace() {
            return namespaceEnv;
        }
    }

    private static final class Base extends Package {
        private Base(VirtualFrame frame) {
            super(frame);
        }

        @Override
        public void rm(String key) throws PutException {
            throw new PutException(RError.Message.ENV_REMOVE_VARIABLES, getPrintNameHelper());
        }

        @Override
        protected String getSearchName() {
            return "package:base";
        }
    }

    /**
     * The users workspace environment (so called global). The parent depends on the set of default
     * packages loaded.
     */
    public static final class Global extends REnvironment {

        private Global(REnvironment parent, VirtualFrame frame) {
            super(parent, "R_GlobalEnv", frame);
        }

        @Override
        protected String getSearchName() {
            return ".GlobalEnv";
        }
    }

    /**
     * When a function is invoked a {@link Function} environment may be created in response to the R
     * {@code environment()} base package function, and it will have an associated frame. We hide
     * the creation of {@link Function} environments to ensure the <i>at most one>/i> invariant and
     * store the value in the frame immediately.
     */
    private static final class Function extends REnvironment {

        private Function(REnvironment parent, MaterializedFrame frame) {
            // function environments are not named
            super(parent, UNNAMED, frame);
            // Associate frame with the environment
            RArguments.setEnvironment(frame, this);
        }

        private static Function create(REnvironment parent, MaterializedFrame frame) {
            Function result = (Function) RArguments.getEnvironment(frame);
            if (result == null) {
                result = new Function(parent, frame);
            }
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
        private final FrameDescriptor descriptor;

        public FunctionDefinition(REnvironment parent) {
            // function environments are not named
            super(parent, UNNAMED, noFrameAccess);
            this.descriptor = new FrameDescriptor();
        }

        public FrameDescriptor getDescriptor() {
            return descriptor;
        }
    }

    public interface UsesREnvMap {
        REnvMapFrameAccess getFrameAccess();
    }

    /**
     * An environment explicitly created with, typically, {@code new.env}. Such environments are
     * always {@link #UNNAMED} but can be given a {@value #NAME_ATTR_KEY}.
     */
    public static final class NewEnv extends REnvironment implements UsesREnvMap {

        /**
         * Constructor for the {@code new.env} function.
         */
        public NewEnv(REnvironment parent, int size) {
            super(parent, UNNAMED, new REnvMapFrameAccess(size));
        }

        /**
         * Constructor for environment without a parent, e.g., for use by {@link #attach}.
         */
        public NewEnv(String name) {
            this(null, 0);
            setAttr(NAME_ATTR_KEY, name);
        }

        public REnvMapFrameAccess getFrameAccess() {
            return (REnvMapFrameAccess) frameAccess;
        }
    }

    /**
     * Helper function for implementations of {@link REnvFrameAccess#ls}.
     */
    public static boolean includeName(String nameToMatch, boolean allNames, Pattern pattern) {
        if (!allNames && nameToMatch.charAt(0) == '.') {
            return false;
        }
        if (pattern != null && !pattern.matcher(nameToMatch).matches()) {
            return false;
        }
        return true;
    }

    /**
     * A placeholder for the package autoload mechanism. N.B. Although "unnamed", it is given a name
     * with {@code attr} in GnuR.
     */
    private static final class Autoload extends REnvironment {
        Autoload() {
            super(baseEnv(), UNNAMED, baseEnv().getFrame());
            setAttr(NAME_ATTR_KEY, "Autoloads");
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
            throw new PutException(RError.Message.ENV_ASSIGN_EMPTY);
        }
    }
}
