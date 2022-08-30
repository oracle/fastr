/*
 * Copyright (c) 2013, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.runtime.context;

import static com.oracle.truffle.r.runtime.context.FastROptions.EagerEval;
import static com.oracle.truffle.r.runtime.context.FastROptions.EagerEvalConstants;
import static com.oracle.truffle.r.runtime.context.FastROptions.EagerEvalDefault;
import static com.oracle.truffle.r.runtime.context.FastROptions.EagerEvalExpressions;
import static com.oracle.truffle.r.runtime.context.FastROptions.EagerEvalVariables;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;

import org.graalvm.options.OptionKey;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleContext;
import com.oracle.truffle.api.TruffleFile;
import com.oracle.truffle.api.TruffleLanguage.Env;
import com.oracle.truffle.api.TruffleStackTraceElement;
import com.oracle.truffle.api.instrumentation.AllocationReporter;
import com.oracle.truffle.api.instrumentation.Instrumenter;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.r.launcher.RCmdOptions;
import com.oracle.truffle.r.launcher.RStartParams;
import com.oracle.truffle.r.runtime.LazyDBCache;
import com.oracle.truffle.r.runtime.PrimitiveMethodsInfo;
import com.oracle.truffle.r.runtime.RCaller;
import com.oracle.truffle.r.runtime.REnvVars;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RErrorHandling;
import com.oracle.truffle.r.runtime.RInternalCode.ContextStateImpl;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RLocale;
import com.oracle.truffle.r.runtime.RLogger;
import com.oracle.truffle.r.runtime.ROptions;
import com.oracle.truffle.r.runtime.RProfile;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RRuntimeASTAccess;
import com.oracle.truffle.r.runtime.RSerialize;
import com.oracle.truffle.r.runtime.ReturnException;
import com.oracle.truffle.r.runtime.SuppressFBWarnings;
import com.oracle.truffle.r.runtime.TempPathName;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.builtins.RBuiltinDescriptor;
import com.oracle.truffle.r.runtime.builtins.RBuiltinKind;
import com.oracle.truffle.r.runtime.builtins.RBuiltinLookup;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport;
import com.oracle.truffle.r.runtime.conn.StdConnections;
import com.oracle.truffle.r.runtime.data.LanguageClosureCache;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RUnboundValue;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.ffi.DLL;
import com.oracle.truffle.r.runtime.ffi.RFFIContext;
import com.oracle.truffle.r.runtime.ffi.RFFIFactory;
import com.oracle.truffle.r.runtime.instrument.InstrumentationState;
import com.oracle.truffle.r.runtime.interop.FastrInteropTryContextState;
import com.oracle.truffle.r.runtime.nodes.RCodeBuilder;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;
import com.oracle.truffle.r.runtime.rng.RRNG;

/**
 * Encapsulates the runtime state ("context") of an R session. All access to that state from the
 * implementation <b>must</b> go through this class. There can be multiple instances
 * (multiple-tenancy) active within a single process/Java-VM.
 *
 * FastR contexts are created during construction of the {@link org.graalvm.polyglot.Context}
 * instance. In case a FastR context needs to be configured, a {@link ChildContextInfo} instance
 * should be passed in {@link Env} configuration under {@link ChildContextInfo#CONFIG_KEY} key.
 *
 * The context provides a so-called {@link Engine} (accessed via {@link #getEngine()}), which
 * provides basic parsing and execution functionality .
 *
 * Context-specific state for implementation classes is managed by this class (or the associated
 * engine) and accessed through the {@code stateXyz} fields.
 *
 * Contexts can be destroyed
 */
public final class RContext {

    /**
     * This is a hack that allows us to pass {@link ChildContextInfo} into {@link RContext} when it
     * is created via Graal SDK that lacks the API to set configuration options for new contexts.
     * See GR-10356 for details.
     */
    public static ChildContextInfo childInfo;

    public enum ContextKind {
        /**
         * Essentially a clean restart, modulo the basic VM-wide initialization. which does include,
         * for example, reading the external environment variables. I.e., it is not a goal to create
         * a multi-user environment with different external inputs. This kind of context can be used
         * in a parallel computation. The initial context is always of this kind. The intent is that
         * all mutable state is localized to the context, which may not be completely achievable.
         * For example, shared native libraries are assumed to contain no mutable state and be
         * re-entrant.
         */
        SHARE_NOTHING,

        /**
         * Shares the set of loaded packages of the parent context at the time the context is
         * created, but creates its own custom global environment. This is used for unit testing
         * where we do not have to load all the packages for each test, but the changes in global
         * environment in one tests do not affect other tests.
         *
         * Cannot safely be used for parallel context evaluation. Must be created as a child of an
         * existing parent context of type {@link #SHARE_NOTHING} or {@link #SHARE_PARENT_RO} and
         * only one such child is allowed. (Strictly speaking the invariant should be only one
         * active child, but the implementation enforces it at creation time). Evidently any changes
         * made to the shared environment, e.g., loading a package, affect the parent.
         */
        SHARE_PARENT_RW,

        /**
         * Intermediate between {@link #SHARE_NOTHING} and {@link #SHARE_PARENT_RW}, this is similar
         * to the standard shared code/copied data model provided by operating systems, although the
         * code/data distinction isn't completely applicable to a language like R. Unlike
         * {@link #SHARE_NOTHING}, where the ASTs for the functions in the default packages are
         * distinct copies in each context, in this kind of context, they are shared. Strictly
         * speaking, the bindings of R functions are shared, and this is achieved by creating a
         * shallow copy of the environments associated with the default packages of the parent
         * context at the time the context is created.
         */
        SHARE_PARENT_RO,

        /**
         * Shares all environments on the search path, but makes illusion of separation by replacing
         * any modified value with an instance of
         * {@link com.oracle.truffle.r.runtime.env.frame.FrameSlotChangeMonitor.MultiSlotData} that
         * holds separate value for each {@link RContext}.
         */
        SHARE_ALL;
    }

    /**
     * Defines the life cycle of an {@link RContext}.
     */
    private enum State {
        /**
         * The {@link RContext} object has been constructed, but not initialized.
         */
        CONSTRUCTED,
        /**
         * The thread has been attached, so {@link #getInstance} may be called, but initialization
         * has not completed.
         */
        ATTACHED,
        /**
         * The {@link RContext} object has been initialized by the {@link #initializeContext}
         * method, but is not active in the sense that the associated {@link Engine} instance has
         * been activated.
         */
        INITIALIZED,
        /**
         * The associated {@link Engine} instance has been activated.
         */
        ACTIVE,
        /**
         * The {@link RContext} object has been destroyed by the {@link #dispose} method. All
         * subsequent operations ideally should throw {@link IllegalStateException}.
         */
        DISPOSED
    }

    /**
     * Tagging interface denoting a (usually inner-) class that carries the context-specific state
     * for a class that has context-specific state. The class specific state must implement this
     * interface.
     */
    public interface ContextState {
        /**
         * Initialize the state. The initial creation should involve minimal computation; any
         * significant computation should be handled in this method.
         */
        @SuppressWarnings("unused")
        default ContextState initialize(RContext context) {
            return this;
        }

        /**
         * Called in response to the {@link RContext#dispose} method. Provides a hook for finalizing
         * any state before the context is destroyed.
         */
        @SuppressWarnings("unused")
        default void beforeDispose(RContext context) {
            // default empty implementation
        }

        /**
         * Called before context finalization ({@code com.oracle.truffle.api.TruffleLanguage#finalizeContext(Object)}).
         * Note that context finalization happens before context disposal, and generally, it should be safe to
         * call native functions in the context finalization.
         */
        default void beforeFinalize(RContext context) {
            // default empty implementation
        }
    }

    private final RStartParams startParameters;
    private final RCmdOptions cmdOptions;
    private final RContext.ContextKind contextKind;
    private final Map<Class<?>, RootCallTarget> cachedCallTargets = new HashMap<>();

    public RootCallTarget getOrCreateCachedCallTarget(Class<?> clazz, Supplier<RootCallTarget> createFunction) {
        RootCallTarget result = cachedCallTargets.get(clazz);
        if (result == null) {
            result = createFunction.get();
            cachedCallTargets.put(clazz, result);
        }
        return result;
    }

    public RFFIUpCallTargets getRFFIUpCallTargets() {
        return rffiUpCallTargets;
    }

    /**
     * Any context created by another has a parent.
     */
    private final RContext parentContext;
    private final int id;
    private final int multiSlotIndex;
    private TruffleContext truffleContext;

    private ExecutorService executor;

    private final InputStream stdin;
    private final OutputStreamWriter stdout;
    private final OutputStreamWriter stderr;

    private final Engine engine;
    private final TruffleRLanguage language;

    /**
     * A context-specific value that is checked in {@code HiddenInternalFunctions} to avoid an error
     * report on a {@code SUBSTITUTE} builtin. Not worth promoting to a {@link ContextState}.
     */
    private boolean loadingBase;

    /**
     * At most one shared child.
     */
    private RContext sharedChild;

    /**
     * Used by the MethodListDispatch class.
     */
    private boolean methodTableDispatchOn = false;
    private boolean allowPrimitiveMethods = true;
    private final HashMap<String, RStringVector> s4ExtendsTable = new HashMap<>();

    private boolean nullS4Object = false;

    private EnumSet<State> state = EnumSet.noneOf(State.class);

    @CompilationFinal private PrimitiveMethodsInfo primitiveMethodsInfo;

    /**
     * Set to {@code true} when in embedded mode to allow other parts of the system to determine
     * whether embedded mode is in effect, <b>before</b> the initial context is created.
     */
    private static boolean embedded;

    /*
     * Workarounds to finesse project circularities between runtime/nodes.
     */
    @CompilationFinal private static RCodeBuilder<RSyntaxNode> astBuilder;
    @CompilationFinal private static RRuntimeASTAccess runtimeASTAccess;
    @CompilationFinal private static RBuiltinLookup builtinLookup;
    @CompilationFinal private static boolean initialContextInitialized;
    @CompilationFinal private static int initialPid;

    public static boolean isInitialContextInitialized() {
        return initialContextInitialized;
    }

    /**
     * Initialize VM-wide static values.
     */
    public static void initializeGlobalState(RCodeBuilder<RSyntaxNode> rAstBuilder, RRuntimeASTAccess rRuntimeASTAccess, RBuiltinLookup rBuiltinLookup) {
        RContext.astBuilder = rAstBuilder;
        RContext.runtimeASTAccess = rRuntimeASTAccess;
        RContext.builtinLookup = rBuiltinLookup;
    }

    // need an additional flag as we don't want multi-slot processing to start until context
    // initialization is fully complete - singleContext flag is not good enough for that
    private static final Assumption isSingleContextAssumption = Truffle.getRuntime().createAssumption("is single RContext");

    private final Env env;
    private final boolean initial;
    /**
     * State that is used to support interposing on loadNamespace() for overrides.
     */
    @CompilationFinal private String nameSpaceName;

    /**
     * The set of classes for which the context manages context-specific state, and their state. We
     * could do this more dynamically with a registration process, perhaps driven by an annotation
     * processor, but the set is relatively small, so we just enumerate them here.
     */
    public final REnvVars stateREnvVars;
    public final RLocale.ContextStateImpl stateRLocale;
    public final TempPathName stateTempPath;
    public final RProfile stateRProfile;
    public final StdConnections.ContextStateImpl stateStdConnections;
    public final ROptions.ContextStateImpl stateROptions;
    public final REnvironment.ContextStateImpl stateREnvironment;
    public final RErrorHandling.ContextStateImpl stateRErrorHandling;
    public final FastrInteropTryContextState stateInteropTry;
    public List<TruffleStackTraceElement> lastInteropTrace;
    public final ConnectionSupport.ContextStateImpl stateRConnection;
    public final RRNG.ContextStateImpl stateRNG;
    public final RSerialize.ContextStateImpl stateRSerialize;
    public final LazyDBCache.ContextStateImpl stateLazyDBCache;
    public final InstrumentationState stateInstrumentation;
    public final ContextStateImpl stateInternalCode;
    public final DLL.ContextStateImpl stateDLL;
    public final GCTortureState gcTorture;
    public volatile EventLoopState eventLoopState;
    public final AltRepContext altRepContext;

    public final GlobalNativeVarContext stateglobalNativeVar;

    public final RFFIUpCallTargets rffiUpCallTargets;

    @CompilationFinal private RFFIContext stateRFFI;

    // Context specific state required for libraries, the initialization is handled lazily by the
    // concrete library.
    @CompilationFinal public Object gridContext = null;
    public final AtomicBoolean interruptResize = new AtomicBoolean(false);
    public boolean graphicsInitialized = false;

    public final WeakHashMap<String, WeakReference<String>> stringMap = new WeakHashMap<>();
    public final WeakHashMap<Source, REnvironment> sourceRefEnvironments = new WeakHashMap<>();
    public final WeakHashMap<TruffleFile, REnvironment> srcfileEnvironments = new WeakHashMap<>();
    public final List<String> libraryPaths = new ArrayList<>(1);
    public final Map<Integer, Thread> threads = new ConcurrentHashMap<>();
    public final LanguageClosureCache languageClosureCache = new LanguageClosureCache();
    public final Map<String, Source> sourceCache = new ConcurrentHashMap<>();

    private final AllocationReporter allocationReporter;

    private final FastROptions fastrOptions;

    private ContextState[] contextStates() {
        return new ContextState[]{stateREnvVars, stateRLocale, stateRProfile, stateTempPath, stateROptions, stateREnvironment, stateRErrorHandling, stateRConnection, stateStdConnections, stateRNG,
                        stateRFFI,
                        stateRSerialize, stateLazyDBCache, stateInstrumentation, stateDLL, stateglobalNativeVar};
    }

    public static void setEmbedded() {
        embedded = true;
    }

    /**
     * Returns {@code true} if this context is run in native embedding compatible with GNU-R
     * scenario. Note: this is not java embedding via GraalSDK.
     */
    public static boolean isEmbedded() {
        return embedded;
    }

    /**
     * Sets the fields that do not depend on complex initialization.
     *
     * @param env values passed from {@code TruffleLanguage#createContext}
     * @param instrumenter value passed from {@code TruffleLanguage#createContext}
     * @param isInitial {@code true} if this is the initial (primordial) context.
     */
    private RContext(TruffleRLanguage language, Env env, Instrumenter instrumenter, boolean isInitial) {
        this.language = language;
        String[] args;
        if (env.getApplicationArguments().length == 0) {
            args = new String[]{"R", "--vanilla", "--no-echo", "--silent", "--no-restore"};
        } else {
            args = env.getApplicationArguments();
        }

        Object initialInfo = env.getConfig().get(ChildContextInfo.CONFIG_KEY);
        if (initialInfo == null) {
            initialInfo = childInfo;
        }
        Map<String, String> initialEnvVars;
        if (initialInfo == null) {
            /*
             * This implies that FastR is being invoked initially from another Truffle language or
             * via RCommand/RscriptCommand. TODO How to decide if session state is to be restored
             */
            this.cmdOptions = RCmdOptions.parseArguments(args, true);
            this.startParameters = new RStartParams(cmdOptions, false);
            this.contextKind = ContextKind.SHARE_NOTHING;
            this.parentContext = null;
            this.id = ChildContextInfo.contextInfoIds.incrementAndGet();
            this.multiSlotIndex = 0;
            this.truffleContext = null;
            this.executor = null;
            initialEnvVars = System.getenv();
        } else {
            // child spawned explicitly by R
            ChildContextInfo info = (ChildContextInfo) initialInfo;
            this.cmdOptions = RCmdOptions.parseArguments(args, true);
            this.startParameters = info.getStartParams();
            this.contextKind = info.getKind();
            this.parentContext = info.getParent();
            this.id = info.getId();
            this.multiSlotIndex = info.getMultiSlotInd();
            this.truffleContext = info.getTruffleContext();
            this.executor = info.executor;
            initialEnvVars = info.getEnv() == null ? Collections.emptyMap() : info.getEnv();
        }

        outputWelcomeMessage(startParameters);

        this.stdin = env.in();
        this.stdout = new OutputStreamWriter(env.out());
        this.stderr = new OutputStreamWriter(env.err());

        this.initial = isInitial;
        this.env = env;
        this.stateREnvVars = REnvVars.newContextState(initialEnvVars);
        this.stateRLocale = RLocale.ContextStateImpl.newContextState();
        this.stateTempPath = TempPathName.newContextState();
        this.stateROptions = ROptions.ContextStateImpl.newContextState(stateREnvVars);
        this.stateRProfile = RProfile.newContextState(stateREnvVars);
        this.stateStdConnections = StdConnections.ContextStateImpl.newContextState();
        this.stateREnvironment = REnvironment.ContextStateImpl.newContextState(this);
        this.stateRErrorHandling = RErrorHandling.ContextStateImpl.newContextState();
        this.stateInteropTry = FastrInteropTryContextState.newContextState();
        this.stateRConnection = ConnectionSupport.ContextStateImpl.newContextState();
        this.stateRNG = RRNG.ContextStateImpl.newContextState();
        this.stateRSerialize = RSerialize.ContextStateImpl.newContextState();
        this.stateLazyDBCache = LazyDBCache.ContextStateImpl.newContextState();
        this.stateInstrumentation = InstrumentationState.newContextState(instrumenter);
        this.stateInternalCode = ContextStateImpl.newContextState();
        this.stateDLL = DLL.ContextStateImpl.newContextState();

        this.rffiUpCallTargets = new RFFIUpCallTargets();

        this.gcTorture = GCTortureState.newContextState();
        this.altRepContext = AltRepContext.newContextState();
        this.stateglobalNativeVar = GlobalNativeVarContext.newContextState(this);
        this.engine = RContext.getRRuntimeASTAccess().createEngine(this);
        state.add(State.CONSTRUCTED);

        this.allocationReporter = env.lookup(AllocationReporter.class);
        this.allocationReporter.addActiveListener(ALLOCATION_ACTIVATION_LISTENER);
        RDataFactory.setAllocationTracingEnabled(allocationReporter.isActive());

        this.fastrOptions = new FastROptions(this);
    }

    static void outputWelcomeMessage(RStartParams rsp) {
        /*
         * Outputting the welcome message here has the virtue that the VM initialization delay
         * occurs later. However, it does not work in embedded mode as console redirects have not
         * been installed at this point. So we do it later in REmbedded.
         */
        if (!rsp.isQuiet() && !embedded) {
            System.out.println(RRuntime.WELCOME_MESSAGE);
        }
    }

    /**
     * Performs the real initialization of the context, invoked from
     * {@code TruffleLanguage#initializeContext}.
     */
    @SuppressFBWarnings(value = "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD", justification = "intentional")
    public RContext initializeContext() {
        fastrOptions.initialize();

        // this must happen before engine activation in the code below
        if (contextKind == ContextKind.SHARE_NOTHING) {
            if (parentContext == null) {
                this.primitiveMethodsInfo = new PrimitiveMethodsInfo();
            } else {
                // share nothing contexts need their own copy of the primitive methods meta-data as
                // they can run (and update this meta data) concurrently with the parent;
                // alternative would be to copy on-write but we would need some kind of locking
                // machinery to avoid races
                assert parentContext.getPrimitiveMethodsInfo() != null;
                this.primitiveMethodsInfo = parentContext.getPrimitiveMethodsInfo().duplicate();
            }
        }

        /*
         * Activate the context by initializing the {@link ContextState} objects.
         */
        state.add(State.ATTACHED);

        stateDLL.initialize(this);
        switch (contextKind) {
            case SHARE_NOTHING:
            case SHARE_ALL:
                stateRFFI = RFFIFactory.create();
                break;
            case SHARE_PARENT_RO:
            case SHARE_PARENT_RW:
                stateRFFI = parentContext.getStateRFFI();
                break;
            default:
                throw RInternalError.shouldNotReachHere();
        }
        // separate in case initialize calls getStateRFFI()!
        getStateRFFI().initialize(this);

        if (!embedded) {
            doEnvOptionsProfileInitialization();
        }

        stateRLocale.initialize(this);
        stateREnvironment.initialize(this);
        stateRErrorHandling.initialize(this);
        stateRConnection.initialize(this);
        stateStdConnections.initialize(this);
        stateRNG.initialize(this);
        stateRSerialize.initialize(this);
        stateLazyDBCache.initialize(this);
        stateInstrumentation.initialize(this);
        stateInternalCode.initialize(this);
        gcTorture.initialize(this);
        state.add(State.INITIALIZED);

        if (!embedded) {
            validateContextStates();
            engine.activate(stateREnvironment);
            state.add(State.ACTIVE);
        }

        if (contextKind == ContextKind.SHARE_PARENT_RW) {
            if (parentContext.sharedChild != null) {
                throw RError.error(RError.SHOW_CALLER2, RError.Message.GENERIC, "can't have multiple active SHARED_PARENT_RW contexts");
            }
            parentContext.sharedChild = this;
            // this one must be shared between contexts - otherwise testing contexts do not know
            // that methods package is loaded
            this.methodTableDispatchOn = parentContext.methodTableDispatchOn;
        }

        if (initial) {
            RContext.initialPid = Utils.getPid();
        }

        if (initial && !embedded) {
            initialContextInitialized = true;
        }
        return this;
    }

    /**
     * Factored out for embedded setup, where this initialization may be customized after the
     * context is initialized but before VM really starts execution.
     */
    private void doEnvOptionsProfileInitialization() {
        stateREnvVars.initialize(this);
        stateTempPath.initialize(this);
        stateROptions.initialize(this);
        stateRProfile.initialize(this);
    }

    @SuppressFBWarnings(value = "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD", justification = "intentional")
    public void completeEmbeddedInitialization() {
        doEnvOptionsProfileInitialization();
        validateContextStates();
        engine.activate(stateREnvironment);
        state.add(State.ACTIVE);
        stateROptions.updateDotOptions();
        initialContextInitialized = true;
    }

    private void validateContextStates() {
        for (ContextState contextState : contextStates()) {
            assert contextState != null;
        }
    }

    /**
     * Create a context with the given configuration.
     *
     * @param isInitial {@code true} iff this is the initial context
     */
    public static RContext create(TruffleRLanguage language, Env env, Instrumenter instrumenter, boolean isInitial) {
        return new RContext(language, env, instrumenter, isInitial);
    }

    public void finalizeContext() {
        if (state.contains(State.INITIALIZED)) {
            // Engine deactive must be called from finalizeContext, because we need to call some
            // native functions from there, and for that, we need the context not to be in the disposal.
            if (!embedded) {
                engine.deactivate();
            }
            for (ContextState contextState : contextStates()) {
                contextState.beforeFinalize(this);
            }
        }
    }

    /**
     * Destroy this context.
     */
    public synchronized void dispose() {
        if (!state.contains(State.DISPOSED)) {
            if (state.contains(State.INITIALIZED)) {
                for (ContextState contextState : contextStates()) {
                    contextState.beforeDispose(this);
                }
            }
            if (contextKind == ContextKind.SHARE_PARENT_RW) {
                parentContext.sharedChild = null;
            }
            state = EnumSet.of(State.DISPOSED);

            assert !initial || EvalThread.threadCnt.get() == 0 : "Did not close all children contexts";

            this.allocationReporter.removeActiveListener(ALLOCATION_ACTIVATION_LISTENER);
            EventLoopState eventLoopStateLocal = this.eventLoopState;
            if (eventLoopStateLocal != null) {
                eventLoopStateLocal.removeTemporaryDirectory();
            }
        }
    }

    public RContext getParent() {
        return parentContext;
    }

    public Env getEnv() {
        return env;
    }

    public FastROptions getFastROptions() {
        return fastrOptions;
    }

    @SuppressWarnings("unchecked")
    public <T> T getOption(OptionKey<T> key) {
        return fastrOptions.getValue(key);
    }

    public int getNonNegativeIntOption(OptionKey<Integer> key) {
        int res = getOption(key);
        if (res >= 0) {
            return res;
        }
        CompilerDirectives.transferToInterpreter();
        throw RInternalError.shouldNotReachHere(String.format("R option '%s' has invalid value"));
    }

    public double getNonNegativeDoubleOption(OptionKey<Double> key) {
        double res = getOption(key);
        if (res >= 0) {
            return res;
        }
        CompilerDirectives.transferToInterpreter();
        throw RInternalError.shouldNotReachHere(String.format("R option '%s' has invalid value"));
    }

    public boolean noEagerEvalOption() {
        return !(getOption(EagerEval) || getOption(EagerEvalConstants) ||
                        getOption(EagerEvalVariables) || getOption(EagerEvalDefault) ||
                        getOption(EagerEvalExpressions));
    }

    /**
     * Convenience function for matching against an option whose value is expected to be a comma
     * separated list. If the option is set as an empty steing, i.e. {@code --R.Option=""}, all
     * elements are deemed to match. Matching is done with {@link String#startsWith} to allow
     * additional data to be tagged onto the element.
     *
     * E.g.
     * <ul>
     * <li>{@code --R.Option} returns {@code true} for all values of {@code element}.</li>
     * <li>{@code --R.Option=foo} returns {@code true} if {@code element.equals("foo")}, else
     * {@code false}.
     * <li>{@code --R.Option=foo,bar=xx} returns {@code true} if {@code element.equals("bar")}, else
     * {@code false}.
     *
     * @param element string to match against the option value list.
     * @return {@code true} if the option is set with no {@code =value} component or if the given
     *         {@code element} matches an element in the options value (comma separated list),
     *         {@code false} otherwise.
     */
    public boolean matchesOption(OptionKey<String> key, String element) {
        String value = getOption(key);

        if (value == null) {
            return false;
        } else if (value.length() == 0) {
            return true;
        } else {
            String[] parts = value.split(",");
            for (String part : parts) {
                if (part.startsWith(element)) {
                    return true;
                }
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public <T> void setOption(OptionKey<T> key, T value) {
        fastrOptions.setValue(key, value);
    }

    /**
     * Convenience function for adding new values to an option whose value is expected to be a comma
     * separated list.
     *
     * TODO maybe support removal if there is a use-case.
     *
     * @see #matchesOption(org.graalvm.options.OptionKey, java.lang.String)
     */
    public void updateOption(OptionKey<String> key, String element) {
        String s = getOption(key);
        if (s == null) {
            // nothing was set
            s = element;
        } else if (s.length() == 0) {
            // everything on, we can't change this
        } else {
            String[] parts = s.split(",");
            for (String part : parts) {
                if (part.startsWith(element)) {
                    // already on
                    return;
                }
            }
            s = s + "," + element;
        }
        setOption(key, s);
    }

    public boolean isLLVMPackage(TruffleFile libPath) {
        return fastrOptions.isLLVMPackage(libPath);
    }

    public boolean isLLVMPackage(String libName) {
        return fastrOptions.isLLVMPackage(libName);
    }

    public InstrumentationState getInstrumentationState() {
        return stateInstrumentation;
    }

    public ContextKind getKind() {
        return contextKind;
    }

    public int getId() {
        return id;
    }

    public int getMultiSlotInd() {
        return multiSlotIndex;
    }

    public static boolean isSingle() {
        return isSingleContextAssumption.isValid();
    }

    public static void markNonSingle() {
        isSingleContextAssumption.invalidate();
    }

    public static RContext getInstance(Node node) {
        return TruffleRLanguage.getCurrentContext(node);
    }

    public static RContext getInstance() {
        CompilerAsserts.neverPartOfCompilation();
        return TruffleRLanguage.getCurrentContext(null);
    }

    public boolean isInteractive() {
        return startParameters.isInteractive();
    }

    /**
     * Access to the engine, when an {@link RContext} object is available, and/or when {@code this}
     * context is not active.
     */
    public Engine getThisEngine() {
        return engine;
    }

    public boolean isMethodTableDispatchOn() {
        return methodTableDispatchOn;
    }

    public void setMethodTableDispatchOn(boolean on) {
        methodTableDispatchOn = on;
    }

    public boolean isNullS4Object() {
        return nullS4Object;
    }

    public void setNullS4Object(boolean on) {
        nullS4Object = on;
    }

    public boolean allowPrimitiveMethods() {
        return allowPrimitiveMethods;
    }

    public void setAllowPrimitiveMethods(boolean on) {
        allowPrimitiveMethods = on;
    }

    public RStringVector getS4Extends(String key) {
        return s4ExtendsTable.get(key);
    }

    public void putS4Extends(String key, RStringVector value) {
        s4ExtendsTable.put(key, value);
    }

    public void removeS4Extends(String key) {
        s4ExtendsTable.remove(key);
    }

    public PrimitiveMethodsInfo getPrimitiveMethodsInfo() {
        if (primitiveMethodsInfo == null) {
            // shared contexts do not run concurrently with their parent and re-use primitive
            // methods information
            assert contextKind != ContextKind.SHARE_NOTHING;
            assert parentContext != null;
            return parentContext.getPrimitiveMethodsInfoWithBoundary();
        } else {
            return primitiveMethodsInfo;
        }
    }

    @TruffleBoundary
    private PrimitiveMethodsInfo getPrimitiveMethodsInfoWithBoundary() {
        return getPrimitiveMethodsInfo();
    }

    /**
     * This is a static property of the implementation and not context-specific.
     */
    public static RCodeBuilder<RSyntaxNode> getASTBuilder() {
        return astBuilder;
    }

    /**
     * This is a static property of the implementation and not context-specific.
     */
    public static RRuntimeASTAccess getRRuntimeASTAccess() {
        return runtimeASTAccess;
    }

    /**
     * Is {@code name} a builtin function (but not a {@link RBuiltinKind#INTERNAL}?
     */
    public static boolean isPrimitiveBuiltin(String name) {
        return builtinLookup.isPrimitiveBuiltin(name);
    }

    /**
     * Return the {@link RFunction} for the builtin {@code name}.
     */
    @TruffleBoundary
    public RFunction lookupBuiltin(String name) {
        return builtinLookup.lookupBuiltin(language, name);
    }

    /**
     * Returns the descriptor for the builtin with the given name. This does not cause an RFunction
     * to be created.
     */
    public static RBuiltinDescriptor lookupBuiltinDescriptor(String name) {
        return builtinLookup.lookupBuiltinDescriptor(name);
    }

    public RCmdOptions getCmdOptions() {
        return cmdOptions;
    }

    public RStartParams getStartParams() {
        return startParameters;
    }

    public void initExecutor() {
        this.executor = Executors.newSingleThreadExecutor();
    }

    public boolean hasExecutor() {
        return executor != null;
    }

    public Object getExecutor() {
        return env.asGuestValue(this.executor);
    }

    /**
     * Allows another thread to schedule some code to be run in this context's thread. The action
     * can be scheduled only if this context was created with an Executor.
     */
    public void schedule(Runnable action) {
        assert hasExecutor() : "Cannot run RContext#schedule() when there is no executor.";
        executor.execute(action);
    }

    public <V> Future<V> submit(Callable<V> action) {
        assert hasExecutor() : "Cannot run RContext#schedule() when there is no executor.";
        return executor.submit(action);
    }

    @Override
    public String toString() {
        return "RContext: " + id;
    }

    /*
     * static functions necessary in code where the context is only implicit in the thread(s)
     * running an evaluation
     */

    public static Engine getEngine() {
        return RContext.getInstance().engine;
    }

    public TruffleRLanguage getLanguage() {
        return language;
    }

    public RFFIContext getRFFI() {
        assert getStateRFFI() != null;
        return getStateRFFI();
    }

    public <C extends RFFIContext> C getRFFI(Class<C> rffiCtxClass) {
        assert getStateRFFI(rffiCtxClass) != null;
        return getStateRFFI(rffiCtxClass);
    }

    public TruffleContext getTruffleContext() {
        return truffleContext;
    }

    public boolean isInitial() {
        return initial;
    }

    public void setLoadingBase(boolean b) {
        loadingBase = b;
    }

    public boolean getLoadingBase() {
        return loadingBase;
    }

    public String getNamespaceName() {
        return nameSpaceName;
    }

    public void setNamespaceName(String name) {
        nameSpaceName = name;
    }

    public static Class<? extends TruffleRLanguage> getTruffleRLanguage() {
        return getRRuntimeASTAccess().getTruffleRLanguage();
    }

    public AllocationReporter getAllocationReporter() {
        return this.allocationReporter;
    }

    public TruffleFile getSafeTruffleFile(String path) {
        String expandedPath = Utils.tildeExpand(path);
        TruffleFile origFile = env.getInternalTruffleFile(expandedPath);
        TruffleFile f = origFile;
        try {
            if (origFile.exists()) {
                try {
                    f = origFile.getCanonicalFile();
                } catch (NoSuchFileException e) {
                    // absolute path "exists", but cannonical does not
                    // happens e.g. during install.packages: file.exists("/{rHome}/inst/..")
                    // lets optimistically FALLBACK on absolute path
                }
            }
        } catch (IOException e) {
            RLogger.getLogger(RLogger.LOGGER_FILE_ACCEESS).log(Level.SEVERE, "Unable to access file " + expandedPath + " " + e.getMessage(), e);
            throw RError.error(RError.SHOW_CALLER, RError.Message.FILE_OPEN_ERROR);
        }

        final TruffleFile home = REnvVars.getRHomeTruffleFile(this);
        if (f.startsWith(home) && isLibraryFile(home.relativize(f))) {
            return origFile;
        } else {
            try {
                return env.getPublicTruffleFile(expandedPath);
            } catch (SecurityException e) {
                RLogger.getLogger(RLogger.LOGGER_FILE_ACCEESS).log(Level.SEVERE, "Unable to access file " + expandedPath + " " + e.getMessage(), e);
                throw RError.error(RError.SHOW_CALLER, RError.Message.FILE_OPEN_ERROR);
            }
        }
    }

    private static boolean isLibraryFile(TruffleFile relativePathFromHome) {
        final String fileName = relativePathFromHome.getName();
        if (fileName == null) {
            return false;
        }
        return relativePathFromHome.startsWith("library");
    }

    private static final Consumer<Boolean> ALLOCATION_ACTIVATION_LISTENER = active -> RDataFactory.setAllocationTracingEnabled(active);

    public final class ConsoleIO {

        private final CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();

        {
            decoder.onMalformedInput(CodingErrorAction.IGNORE);
            decoder.onUnmappableCharacter(CodingErrorAction.IGNORE);
        }

        @TruffleBoundary
        public String readLine() {
            /*
             * We cannot use an InputStreamReader because it buffers characters internally, whereas
             * readLine() should not buffer across newlines.
             */

            ByteBuffer bytes = ByteBuffer.allocate(16);
            CharBuffer chars = CharBuffer.allocate(16);
            StringBuilder str = new StringBuilder();
            decoder.reset();
            while (true) {
                int inputByte;
                try {
                    inputByte = stdin.read();
                } catch (IOException e) {
                    throw new RInternalError(e, "error writing to stderr");
                }
                if (inputByte == -1) {
                    return str.toString();
                }
                bytes.put((byte) inputByte);
                bytes.flip();
                decoder.decode(bytes, chars, false);
                chars.flip();
                while (chars.hasRemaining()) {
                    char c = chars.get();
                    if (c == '\n' || c == '\r') {
                        return str.toString();
                    }
                    str.append(c);
                }
                bytes.compact();
                chars.clear();
            }
        }

        @TruffleBoundary
        public void print(String message) {
            try {
                stdout.write(message);
                stdout.flush();
            } catch (IOException e) {
                throw new RInternalError(e, "error writing to stdout");
            }
        }

        @TruffleBoundary
        public void println(String message) {
            try {
                stdout.write(message);
                stdout.write('\n');
                stdout.flush();
            } catch (IOException e) {
                throw new RInternalError(e, "error writing to stdout");
            }
        }

        @TruffleBoundary
        public void printf(String format, Object... args) {
            try {
                stdout.write(String.format(format, args));
                stdout.flush();
            } catch (IOException e) {
                throw new RInternalError(e, "error writing to stdout");
            }
        }

        @TruffleBoundary
        public void printError(String message) {
            try {
                stderr.write(message);
                stderr.flush();
            } catch (IOException e) {
                throw new RInternalError(e, "error writing to stderr");
            }
        }

        @TruffleBoundary
        public void printErrorln(String message) {
            try {
                stderr.write(message);
                stderr.write('\n');
                stderr.flush();
            } catch (IOException e) {
                throw new RInternalError(e, "error writing to stderr");
            }
        }

        @TruffleBoundary
        public String getPrompt() {
            if (handler != null) {
                Object result;
                try {
                    InteropLibrary interop = InteropLibrary.getFactory().getUncached();
                    Object f = interop.readMember(handler, "getPrompt");
                    result = interop.execute(f);
                } catch (UnknownIdentifierException | UnsupportedMessageException | UnsupportedTypeException | ArityException | ClassCastException e) {
                    throw new RInternalError(e, "error while reading prompt");
                }
                return (String) result;
            }
            return "";
        }

        @TruffleBoundary
        public void setPrompt(String prompt) {
            if (handler != null) {
                try {
                    InteropLibrary interop = InteropLibrary.getFactory().getUncached();
                    Object f = interop.readMember(handler, "setPrompt");
                    interop.execute(f, prompt);
                } catch (UnknownIdentifierException | UnsupportedMessageException | UnsupportedTypeException | ArityException | ClassCastException e) {
                    throw new RInternalError(e, "error while writing prompt from object " + handler);
                }
            }
        }

        public InputStream getStdin() {
            return env.in();
        }

        public OutputStream getStdout() {
            return env.out();
        }

        public OutputStream getStderr() {
            return env.err();
        }

        private TruffleObject handler;

        public void setHandler(TruffleObject handler) {
            this.handler = handler;
        }
    }

    private final ConsoleIO console = new ConsoleIO();

    public ConsoleIO getConsole() {
        return console;
    }

    public RFFIContext getStateRFFI() {
        return stateRFFI;
    }

    public <C extends RFFIContext> C getStateRFFI(Class<C> rffiCtxClass) {
        return stateRFFI.as(rffiCtxClass);
    }

    /**
     * Returns the PID as retrieved by the initial context.
     */
    public static int getInitialPid() {
        return initialPid;
    }

    /**
     * A special caller object for the {@link ReturnException} thrown by
     * {@link RContext#checkPendingRepaintRequest()}.
     */
    public static final RCaller dispatchPrimFunNodeCaller = RCaller.createInvalid(null);

    /**
     * Check whether we are running as a display list callback and whether there is a new repaint
     * request. If so, break the playing back of the display list for the waiting repaint request to
     * be processed.
     */
    public void checkPendingRepaintRequest() {
        if (getStateRFFI().rffiContextState.primFunBeingDispatched && interruptResize.get()) {
            throw new ReturnException(RUnboundValue.instance, dispatchPrimFunNodeCaller);
        }
    }

}
