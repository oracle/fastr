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
package com.oracle.truffle.r.runtime.env;

import java.util.ArrayList;
import java.util.regex.Pattern;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.runtime.AnonymousFrameVariable;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.RErrorException;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.VirtualEvalFrame;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RAttributeProfiles;
import com.oracle.truffle.r.runtime.data.RAttributeStorage;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RTypedValue;
import com.oracle.truffle.r.runtime.env.frame.NSBaseMaterializedFrame;
import com.oracle.truffle.r.runtime.env.frame.REnvEmptyFrameAccess;
import com.oracle.truffle.r.runtime.env.frame.REnvFrameAccess;
import com.oracle.truffle.r.runtime.env.frame.REnvTruffleFrameAccess;

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
 * Multi-tenancy (multiple {@link RContext}s).
 * <p>
 * The logic for implementing the three different forms of
 * {@link com.oracle.truffle.r.runtime.context.RContext.ContextKind} is encapsulated in the
 * {@link #createContext} method.
 */
public abstract class REnvironment extends RAttributeStorage implements RTypedValue {

    private static final RStringVector implicitClass = RDataFactory.createStringVectorFromScalar(RType.Environment.getName());

    @Override
    public final RStringVector getImplicitClass() {
        return implicitClass;
    }

    public static class ContextStateImpl implements RContext.ContextState {
        private SearchPath searchPath;
        private final MaterializedFrame globalFrame;
        private Base baseEnv;
        private REnvironment namespaceRegistry;
        private MaterializedFrame parentGlobalFrame; // SHARED_PARENT_RW only

        ContextStateImpl(MaterializedFrame globalFrame, SearchPath searchPath) {
            this.globalFrame = globalFrame;
            this.searchPath = searchPath;
        }

        ContextStateImpl(MaterializedFrame globalFrame, SearchPath searchPath, Base baseEnv, REnvironment namespaceRegistry) {
            this(globalFrame, searchPath);
            this.baseEnv = baseEnv;
            this.namespaceRegistry = namespaceRegistry;
        }

        public REnvironment getGlobalEnv() {
            return RArguments.getEnvironment(globalFrame);
        }

        public MaterializedFrame getGlobalFrame() {
            return globalFrame;
        }

        public SearchPath getSearchPath() {
            return searchPath;
        }

        public Base getBaseEnv() {
            return baseEnv;
        }

        public REnvironment getBaseNamespace() {
            return baseEnv.getNamespace();
        }

        public REnvironment getNamespaceRegistry() {
            return namespaceRegistry;
        }

        private void setSearchPath(SearchPath searchPath) {
            this.searchPath = searchPath;
        }

        private void setBaseEnv(Base baseEnv) {
            this.baseEnv = baseEnv;
        }

        private void setNamespaceRegistry(REnvironment namespaceRegistry) {
            this.namespaceRegistry = namespaceRegistry;
        }

        @Override
        public void beforeDestroy(RContext context) {
            beforeDestroyContext(context, this);
        }

        public static ContextStateImpl newContext(RContext context) {
            return createContext(context, RRuntime.createNonFunctionFrame("global"));
        }
    }

    public static class PutException extends RErrorException {
        private static final long serialVersionUID = 1L;

        @TruffleBoundary
        public PutException(RError.Message msg, Object... args) {
            super(msg, args);
        }
    }

    public static class SearchPath {
        private final ArrayList<REnvironment> list = new ArrayList<>();

        void add(REnvironment env) {
            list.add(env);
        }

        void add(int index, REnvironment env) {
            list.add(index, env);
        }

        int size() {
            return list.size();
        }

        REnvironment get(int index) {
            return list.get(index);
        }

        void remove(int index) {
            list.remove(index);
        }

        void updateGlobal(Global globalEnv) {
            list.set(0, globalEnv);
        }
    }

    public static final String UNNAMED = new String("");
    private static final String NAME_ATTR_KEY = "name";
    private static final Empty emptyEnv = new Empty();

    private final String name;
    private final REnvFrameAccess frameAccess;
    private boolean locked;

    @Override
    public RType getRType() {
        return RType.Environment;
    }

    /**
     * Value returned by {@code emptyenv()}.
     */
    public static Empty emptyEnv() {
        return emptyEnv;
    }

    /**
     * Value returned by {@code globalenv()}.
     */
    public static REnvironment globalEnv() {
        return RContext.getInstance().stateREnvironment.getGlobalEnv();
    }

    /**
     * Returns {@code true} iff {@code frame} is that associated with {@code env}. N.B. The
     * environment associated with the frame may be {@code null} as {@link Function} environments
     * are created lazily. However, we maintain the invariant that whenever a {@link Function}
     * environment is created the value is stored in the associated frame. Therefore {@code env}
     * could never match lazy {@code null}.
     */
    private static boolean isFrameForEnv(Frame frame, REnvironment env) {
        return RArguments.getEnvironment(frame) == env;
    }

    /**
     * Check whether the given frame is indeed the frame stored in the global environment.
     */
    public static boolean isGlobalEnvFrame(Frame frame) {
        return isFrameForEnv(frame, RContext.getInstance().stateREnvironment.getGlobalEnv());
    }

    /**
     * Value returned by {@code baseenv()}. This is the "package:base" environment.
     */
    public static REnvironment baseEnv() {
        Base baseEnv = RContext.getInstance().stateREnvironment.getBaseEnv();
        assert baseEnv != null;
        return baseEnv;
    }

    /**
     * Value set in {@code .baseNameSpaceEnv} variable. This is the "namespace:base" environment.
     */
    public static REnvironment baseNamespaceEnv() {
        Base baseEnv = RContext.getInstance().stateREnvironment.getBaseEnv();
        assert baseEnv != null;
        return baseEnv.getNamespace();
    }

    /**
     * Invoked on startup to setup the {@code #baseEnv}, {@code namespaceRegistry} and package
     * search path.
     *
     * The base "package" is special, it has no "imports" and the parent of its associated namespace
     * is {@link #globalEnv}. Unlike other packages, there is no difference between the bindings in
     * "package:base" and its associated namespace. The way this is implemented in FastR is that the
     * underlying {@link MaterializedFrame} is shared. The {@link #frameAccess} value for
     * "namespace:base" refers to {@link NSBaseMaterializedFrame}, which delegates all its
     * operations to {@code baseFrame}, but it's "enclosingFrame" field in {@link RArguments}
     * differs, referring to {@code globalFrame}, as required by the R spec.
     */
    public static void baseInitialize(MaterializedFrame baseFrame, MaterializedFrame initialGlobalFrame) {
        // TODO if namespaceRegistry is ever used in an eval an internal env won't suffice.
        REnvironment namespaceRegistry = RDataFactory.createInternalEnv();
        ContextStateImpl state = RContext.getInstance().stateREnvironment;
        state.setNamespaceRegistry(namespaceRegistry);
        Base baseEnv = new Base(baseFrame, initialGlobalFrame);
        namespaceRegistry.safePut("base", baseEnv.namespaceEnv);

        Global globalEnv = new Global(initialGlobalFrame);
        RArguments.initializeEnclosingFrame(initialGlobalFrame, baseFrame);
        state.setBaseEnv(baseEnv);
        state.setSearchPath(initSearchList(globalEnv));
    }

    /**
     * {@link RContext} creation, with {@code globalFrame}. If this is a {@code SHARE_NOTHING}
     * context we only create the minimal search path with no packages as the package loading is
     * handled by the engine. For a {@code SHARE_PARENT_RW} context, we keep the existing search
     * path, just replacing the {@code globalenv} component. For a {@code SHARE_PARENT_RO} context
     * we make shallow copies of the package environments.
     *
     * N.B. {@link RContext#stateREnvironment} accesses the new, as yet uninitialized
     * {@link ContextStateImpl} object
     */
    private static ContextStateImpl createContext(RContext context, MaterializedFrame globalFrame) {
        switch (context.getKind()) {
            case SHARE_PARENT_RW: {
                /*
                 * To share the existing package structure, we create the new globalEnv with the
                 * parent of the previous global env. Then we create a copy of the SearchPath and
                 * patch the global entry.
                 */
                ContextStateImpl parentState = context.getParent().stateREnvironment;
                Base parentBaseEnv = parentState.getBaseEnv();
                NSBaseMaterializedFrame nsBaseFrame = (NSBaseMaterializedFrame) parentBaseEnv.namespaceEnv.getFrame();
                MaterializedFrame prevGlobalFrame = RArguments.getEnclosingFrame(nsBaseFrame);

                Global prevGlobalEnv = (Global) RArguments.getEnvironment(prevGlobalFrame);
                nsBaseFrame.updateGlobalFrame(globalFrame);
                Global newGlobalEnv = new Global(globalFrame);
                newGlobalEnv.setParent(prevGlobalEnv.getParent());
                SearchPath searchPath = initSearchList(prevGlobalEnv);
                searchPath.updateGlobal(newGlobalEnv);
                parentState.getBaseEnv().safePut(".GlobalEnv", newGlobalEnv);
                ContextStateImpl result = new ContextStateImpl(globalFrame, searchPath, parentBaseEnv, parentState.getNamespaceRegistry());
                result.parentGlobalFrame = prevGlobalFrame;
                return result;
            }

            case SHARE_PARENT_RO: {
                /* We make shallow copies of all the default package environments in the parent */
                ContextStateImpl parentState = context.getParent().stateREnvironment;
                SearchPath parentSearchPath = parentState.getSearchPath();
                // clone all the environments below global from the parent
                REnvironment e = parentSearchPath.get(1).cloneEnv(globalFrame);
                // create the new Global with clone top as parent
                Global newGlobalEnv = new Global(globalFrame);
                RArguments.initializeEnclosingFrame(globalFrame, e.getFrame());
                // create new namespaceRegistry and populate it while locating "base"
                REnvironment newNamespaceRegistry = RDataFactory.createInternalEnv();
                Base newBaseEnv = null;
                while (e != emptyEnv) {
                    if (e instanceof Base) {
                        newBaseEnv = (Base) e;
                    }
                    e = e.getParent();
                }
                assert newBaseEnv != null;
                copyNamespaceRegistry(parentState.namespaceRegistry, newNamespaceRegistry);
                newNamespaceRegistry.safePut("base", newBaseEnv.namespaceEnv);
                newBaseEnv.safePut(".GlobalEnv", newGlobalEnv);
                SearchPath newSearchPath = initSearchList(newGlobalEnv);
                return new ContextStateImpl(globalFrame, newSearchPath, newBaseEnv, newNamespaceRegistry);
            }

            case SHARE_NOTHING: {
                // SHARE_NOTHING: baseInitialize takes care of everything
                return new ContextStateImpl(globalFrame, new SearchPath());
            }

            default:
                throw RInternalError.shouldNotReachHere();
        }
    }

    private static void beforeDestroyContext(RContext context, RContext.ContextState state) {
        switch (context.getKind()) {
            case SHARE_PARENT_RW: {
                /*
                 * Since we updated the parent's baseEnv with the new .GlobalEnv value we need to
                 * restore that and the frame in NSBaseMaterializedFrame.
                 */
                MaterializedFrame parentGlobalFrame = ((ContextStateImpl) state).parentGlobalFrame;
                Global parentGlobalEnv = (Global) RArguments.getEnvironment(parentGlobalFrame);
                ContextStateImpl parentState = context.getParent().stateREnvironment;
                NSBaseMaterializedFrame nsBaseFrame = (NSBaseMaterializedFrame) parentState.baseEnv.namespaceEnv.getFrame();
                nsBaseFrame.updateGlobalFrame(parentGlobalFrame);
                parentState.baseEnv.safePut(".GlobalEnv", parentGlobalEnv);
                break;
            }

            default:
                // nothing to do
        }
    }

    private static SearchPath initSearchList(Global globalEnv) {
        SearchPath searchPath = new SearchPath();
        REnvironment env = globalEnv;
        do {
            searchPath.add(env);
            env = env.getParent();
        } while (env != emptyEnv);
        return searchPath;
    }

    /**
     * Clone an environment for a {@code SHARED_CODE} context. {@link Base} overrides the method,
     * which is why we pass {@code globalFrame} as it needs it for it's creation.
     */
    protected REnvironment cloneEnv(MaterializedFrame globalFrame) {
        REnvironment parentClone = getParent();
        if (parentClone != emptyEnv) {
            parentClone = parentClone.cloneEnv(globalFrame);
        }
        // N.B. Base overrides this method, so we only get here for package environments
        REnvironment newEnv = RDataFactory.createNewEnv(getName());
        RArguments.initializeEnclosingFrame(newEnv.getFrame(), parentClone.getFrame());
        if (attributes != null) {
            newEnv.attributes = attributes.copy();
        }
        copyBindings(newEnv);
        return newEnv;
    }

    private static void copyNamespaceRegistry(REnvironment parent, REnvironment child) {
        RStringVector bindings = parent.ls(true, null, false);
        for (int i = 0; i < bindings.getLength(); i++) {
            String name = bindings.getDataAt(i);
            if (name.equals("base")) {
                continue;
            }
            Object value = parent.get(name);
            REnvironment parentNamespace = (REnvironment) value;
            assert parentNamespace.isNamespaceEnv();
            REnvironment newNamespace = RDataFactory.createInternalEnv();
            parentNamespace.copyBindings(newNamespace);
            child.safePut(name, newNamespace);
        }
    }

    /**
     * Copies the bindings from {@code this} environment to {@code newEnv}, recursively copying any
     * bindings are are {@link REnvironment}s.
     */
    protected void copyBindings(REnvironment newEnv) {
        RStringVector bindings = ls(true, null, false);
        for (int i = 0; i < bindings.getLength(); i++) {
            String binding = bindings.getDataAt(i);
            Object value = get(binding);
            newEnv.safePut(binding, value);
        }
    }

    /**
     * Data for the {@code search} function.
     */
    public static String[] searchPath() {
        SearchPath searchPath = RContext.getInstance().stateREnvironment.getSearchPath();
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
        SearchPath searchPath = RContext.getInstance().stateREnvironment.getSearchPath();
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
        SearchPath searchPath = RContext.getInstance().stateREnvironment.getSearchPath();
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
        return RContext.getInstance().stateREnvironment.getNamespaceRegistry();
    }

    /**
     * Add name to namespace registry.
     *
     * @param name namespace name
     * @param env namespace value
     * @return {@code null} if name is already registered else {@code env}
     */
    public static Object registerNamespace(String name, REnvironment env) {
        REnvironment nsreg = RContext.getInstance().stateREnvironment.getNamespaceRegistry();
        try {
            nsreg.put(name, env);
            return env;
        } catch (PutException ex) {
            return null;
        }
    }

    /**
     * Remove name from namespace registry.
     *
     * @param name namespace name
     * @return {@code null} if name is not registered else namespace value
     */
    public static Object unregisterNamespace(String name) {
        REnvironment nsreg = RContext.getInstance().stateREnvironment.getNamespaceRegistry();
        Object ns = nsreg.get(name);
        if (ns != null) {
            try {
                nsreg.rm(name);
            } catch (PutException ex) {
                throw RInternalError.shouldNotReachHere(ex);
            }
        }
        return ns;
    }

    /**
     * Get the registered {@code namespace} environment {@code name}, or {@code null} if not found.
     * N.B. The package loading code in {@code namespace.R} uses a {code new.env} environment for a
     * namespace.
     */
    public static REnvironment getRegisteredNamespace(String name) {
        return (REnvironment) RContext.getInstance().stateREnvironment.getNamespaceRegistry().get(name);
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
        SearchPath searchPath = RContext.getInstance().stateREnvironment.getSearchPath();
        if (bpos > searchPath.size() - 1) {
            bpos = searchPath.size() - 1;
        }
        // Insert in the REnvironment search path, adjusting the parent fields appropriately
        // In the default case (pos == 2), envAbove is the Global env
        REnvironment envAbove = searchPath.get(bpos - 1);
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
     * Detach the environment at search position {@code pos}.
     *
     * @return the {@link REnvironment} that was detached.
     */
    public static REnvironment detach(int pos) throws DetachException {
        SearchPath searchPath = RContext.getInstance().stateREnvironment.getSearchPath();
        if (pos == searchPath.size()) {
            detachException(RError.Message.ENV_DETACH_BASE);
        }
        if (pos <= 0 || pos >= searchPath.size()) {
            detachException(RError.Message.INVALID_POS_ARGUMENT);
        }
        assert pos != 1; // explicitly checked in caller
        // N.B. pos is 1-based
        int bpos = pos - 1;
        REnvironment envAbove = searchPath.get(bpos - 1);
        REnvironment envToRemove = searchPath.get(bpos);
        searchPath.remove(bpos);
        MaterializedFrame aboveFrame = envAbove.frameAccess.getFrame();
        RArguments.detachFrame(aboveFrame);
        return envToRemove;
    }

    @TruffleBoundary
    private static void detachException(RError.Message message) throws DetachException {
        throw new DetachException(message);
    }

    /**
     * Converts a {@link Frame} to an {@link REnvironment}, which necessarily requires the frame to
     * be materialized.
     */
    public static REnvironment frameToEnvironment(MaterializedFrame frame) {
        MaterializedFrame f = frame instanceof VirtualEvalFrame ? ((VirtualEvalFrame) frame).getOriginalFrame() : frame;
        REnvironment env = RArguments.getEnvironment(f);
        if (env == null) {
            if (RArguments.getFunction(f) == null) {
                throw RInternalError.shouldNotReachHere();
            }
            env = createEnclosingEnvironments(f);
        }
        return env;
    }

    /**
     * This chain can be followed back to whichever "base" (i.e. non-function) environment the
     * outermost function was defined in, e.g. "global" or "base". The purpose of this method is to
     * create an analogous lexical parent chain of {@link Function} instances with the correct
     * {@link MaterializedFrame}.
     */
    @TruffleBoundary
    public static REnvironment createEnclosingEnvironments(MaterializedFrame frame) {
        MaterializedFrame f = frame instanceof VirtualEvalFrame ? ((VirtualEvalFrame) frame).getOriginalFrame() : frame;
        REnvironment env = RArguments.getEnvironment(f);
        if (env == null) {
            // parent is the env of the enclosing frame
            env = REnvironment.Function.create(f);
        }
        return env;
    }

    /**
     * Convert an {@link RList} to an {@link REnvironment}, which is needed in several builtins,
     * e.g. {@code substitute}.
     */
    @TruffleBoundary
    public static REnvironment createFromList(RAttributeProfiles attrProfiles, RList list, REnvironment parent) {
        REnvironment result = RDataFactory.createNewEnv(null);
        RArguments.initializeEnclosingFrame(result.getFrame(), parent.getFrame());
        RStringVector names = list.getNames(attrProfiles);
        for (int i = 0; i < list.getLength(); i++) {
            try {
                result.put(names.getDataAt(i), list.getDataAt(i));
            } catch (PutException ex) {
                throw RError.error(RError.SHOW_CALLER2, ex);
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
     */
    public boolean isNamespaceEnv() {
        if (this instanceof BaseNamespace) {
            return true;
        } else {
            RStringVector spec = getNamespaceSpec();
            return spec != null;
        }
    }

    /**
     * Returns {@code null} if this environment is not a package environment else the result from
     * {@link #getName}.
     */
    public String isPackageEnv() {
        String envName = getName();
        return envName.startsWith("package:") ? envName : null;
    }

    /**
     * If this is not a "package" environment return "this", otherwise return the associated
     * "namespace" env.
     */
    public REnvironment getPackageNamespaceEnv() {
        if (this == RContext.getInstance().stateREnvironment.getBaseEnv()) {
            return ((Base) this).namespaceEnv;
        }
        String envName;
        if (((envName = isPackageEnv()) != null)) {
            return REnvironment.getRegisteredNamespace(envName.replace("package:", ""));
        } else {
            return this;
        }
    }

    /**
     * Return the "spec" attribute of the "info" env in a namespace or {@code null} if not found.
     */
    public RStringVector getNamespaceSpec() {
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

    // end of static members

    /**
     * The basic constructor; just assigns the essential fields.
     */
    private REnvironment(String name, REnvFrameAccess frameAccess) {
        this.name = name;
        this.frameAccess = frameAccess;
    }

    /**
     * An environment associated with an already materialized frame.
     */
    private REnvironment(String name, MaterializedFrame frame) {
        this(name, new REnvTruffleFrameAccess(frame));

        // Associate frame with the environment
        RArguments.setEnvironment(frame, this);
    }

    public REnvironment getParent() {
        MaterializedFrame enclosingFrame = RArguments.getEnclosingFrame(getFrame());
        return enclosingFrame == null ? emptyEnv : frameToEnvironment(enclosingFrame);
    }

    /**
     * Explicitly set the parent of an environment. TODO Change the enclosingFrame of (any)
     * associated Truffle frame
     */
    public void setParent(REnvironment env) {
        if (getParent() != env) {
            RArguments.setEnclosingFrame(getFrame(), env.getFrame());
        }
    }

    /**
     * The "simple" name of the environment. This is the value returned by the R
     * {@code environmentName} function.
     */
    public String getName() {
        String attrName = attributes == null ? null : RRuntime.asString(attributes.get(NAME_ATTR_KEY));
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
            RStringVector spec = getNamespaceSpec();
            if (spec != null) {
                return "namespace:" + spec.getDataAt(0);
            } else {
                return String.format("%#x", hashCode());
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
        return frameAccess.getFrame();
    }

    public MaterializedFrame getFrame(ValueProfile frameAccessProfile) {
        return frameAccessProfile.profile(frameAccess).getFrame();
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

    @TruffleBoundary
    public Object get(String key) {
        return frameAccess.get(key);
    }

    @TruffleBoundary
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

    /**
     * Explicit search for a function {@code name}; used in startup sequence.
     *
     * @return the value of the function or {@code null} if not found.
     */
    public Object findFunction(String varName) {
        CompilerAsserts.neverPartOfCompilation();
        REnvironment env = this;
        while (env != emptyEnv) {
            Object value = env.get(varName);
            if (value != null && (value instanceof RFunction || value instanceof RPromise)) {
                return value;
            }
            env = env.getParent();
        }
        return null;
    }

    public RStringVector ls(boolean allNames, Pattern pattern, boolean sorted) {
        return frameAccess.ls(allNames, pattern, sorted);
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

    private static final class BaseNamespace extends REnvironment {
        private BaseNamespace(String name, MaterializedFrame frame) {
            super(name, frame);
            RArguments.setEnvironment(frame, this);
        }

        @Override
        protected String getPrintNameHelper() {
            return "namespace:" + getName();
        }
    }

    private static final class Base extends REnvironment {
        private final BaseNamespace namespaceEnv;

        private Base(MaterializedFrame baseFrame, MaterializedFrame globalFrame) {
            super("base", baseFrame);
            /*
             * We create the NSBaseMaterializedFrame using globalFrame as the enclosing frame. The
             * namespaceEnv parent field will change to globalEnv after the latter is created
             */
            NSBaseMaterializedFrame frame = new NSBaseMaterializedFrame(baseFrame, globalFrame);
            this.namespaceEnv = new BaseNamespace("base", frame);
        }

        @Override
        public void rm(String key) throws PutException {
            throw new PutException(RError.Message.ENV_REMOVE_VARIABLES, getPrintNameHelper());
        }

        @Override
        protected String getSearchName() {
            return "package:base";
        }

        public BaseNamespace getNamespace() {
            return namespaceEnv;
        }

        @Override
        protected REnvironment cloneEnv(MaterializedFrame globalFrame) {
            Base newBase = new Base(RRuntime.createNonFunctionFrame("base"), globalFrame);
            this.copyBindings(newBase);
            return newBase;
        }
    }

    /**
     * The users workspace environment (so called global). The parent depends on the set of default
     * packages loaded.
     */
    public static final class Global extends REnvironment {

        static final String SEARCHNAME = ".GlobalEnv";

        private Global(MaterializedFrame frame) {
            super("R_GlobalEnv", frame);
        }

        @Override
        protected String getSearchName() {
            return SEARCHNAME;
        }
    }

    /**
     * When a function is invoked a {@link Function} environment may be created in response to the R
     * {@code environment()} base package function, and it will have an associated frame. We hide
     * the creation of {@link Function} environments to ensure the <i>at most one>/i> invariant and
     * store the value in the frame immediately.
     */
    public static final class Function extends REnvironment {

        private Function(MaterializedFrame frame) {
            // function environments are not named
            super(UNNAMED, frame);
        }

        private static Function create(MaterializedFrame frame) {
            Function result = (Function) RArguments.getEnvironment(frame);
            if (result == null) {
                result = new Function(frame);
            }
            return result;
        }
    }

    /**
     * An environment explicitly created with, typically, {@code new.env}, but also used internally.
     * Such environments are always {@link #UNNAMED} but can later be given a name as an attribute.
     * This is the class used by the {@code new.env} function. We record but do not interpret the
     * {@code hash} input, as we always use a hashmap, for possible use by the serialization code
     * (GnuR generates different output format for hash environments).
     *
     */
    public static final class NewEnv extends REnvironment {
        private boolean hashed;
        private int initialSize;

        public NewEnv(MaterializedFrame frame, String name) {
            super(UNNAMED, frame);
            if (name != null) {
                setAttr(NAME_ATTR_KEY, name);
            }
        }

        public boolean isHashed() {
            return hashed;
        }

        public void setHashed(boolean hashed) {
            this.hashed = hashed;
        }

        public int getInitialSize() {
            return initialSize;
        }

        public void setInitialSize(int initialSize) {
            this.initialSize = initialSize;
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
        if (AnonymousFrameVariable.isAnonymous(nameToMatch)) {
            return false;
        }
        return true;
    }

    /**
     * The empty environment has no runtime state and so can be allocated statically.
     */
    private static final class Empty extends REnvironment {

        public static final String EMPTY_ENV_NAME = "R_EmptyEnv";

        private Empty() {
            super(EMPTY_ENV_NAME, new REnvEmptyFrameAccess());
        }

        @Override
        public void put(String key, Object value) throws PutException {
            throw new PutException(RError.Message.ENV_ASSIGN_EMPTY);
        }
    }
}
