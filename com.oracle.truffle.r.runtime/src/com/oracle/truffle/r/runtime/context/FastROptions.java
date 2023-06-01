/*
 * Copyright (c) 2014, 2023, Oracle and/or its affiliates. All rights reserved.
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.graalvm.options.OptionCategory;
import org.graalvm.options.OptionDescriptor;
import org.graalvm.options.OptionDescriptors;
import org.graalvm.options.OptionKey;
import org.graalvm.options.OptionStability;
import org.graalvm.options.OptionValues;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Option;
import com.oracle.truffle.api.TruffleFile;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.FastRConfig;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RLogger;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.NativeDataAccess.NativeDataInspector;

/**
 * Options to control the behavior of the FastR system, that relate to the implementation, i.e., are
 * <b>not</b> part of the standard set of R options or command line options.<br/>
 * Can be set:
 * <ul>
 * <li>either via command-line - i.e. {@code bin/r --R.PerformanceWarnings=true}</li>
 * <li>or via {@code org.graal.polyglot.Context.Builder}
 * </ul>
 * N.B. The options must be initialized/processed at runtime for an AOT VM.
 */
// @formatter:off
@Option.Group(RRuntime.R_LANGUAGE_ID)
public class FastROptions {
    private static final FastROptionsOptionDescriptors descriptors = new FastROptionsOptionDescriptors();

    @Option(category = OptionCategory.INTERNAL, help = "Prints Java and R stack traces for all errors") //
    public static final OptionKey<Boolean> PrintErrorStacktraces = new OptionKey<>(false);
    @Option(category = OptionCategory.USER, stability = OptionStability.STABLE, help = "Dumps Java and R stack traces to 'fastr_errors-{context ID}_{PID}.log' for all internal errors") //
    public static final OptionKey<Boolean> PrintErrorStacktracesToFile = new OptionKey<>(false);
    @Option(category = OptionCategory.USER, stability = OptionStability.STABLE, usageSyntax = "native|llvm", help = "Specifies the RFFI backend.") //
    public static final OptionKey<String> BackEnd = new OptionKey<>("native");
    @Option(category = OptionCategory.USER, stability = OptionStability.STABLE, usageSyntax = "<package1>,<package2>,...", help = "Native code of specified packages will be executed by LLVM backend.") //
    public static final OptionKey<String> BackEndLLVM = new OptionKey<>("");
    @Option(category = OptionCategory.USER, stability = OptionStability.STABLE, usageSyntax = "<package1>,<package2>,...", help = "Native code of specified packages will be executed by native (NFI) backend.") //
    public static final OptionKey<String> BackEndNative = new OptionKey<>("");

    @Option(category = OptionCategory.EXPERT, stability = OptionStability.EXPERIMENTAL, help = "Enable or disable cache of AST instances specialized for given R environment.") //
    public static final OptionKey<Boolean> EnableClosureCallTargetsCache = new OptionKey<>(true);

    @Option(category = OptionCategory.INTERNAL, usageSyntax = "<name1>,<name2>,...", help = "Turn on debugging output for 'name1', 'name2', etc.")//
    public static final OptionKey<String> Debug = new OptionKey<>("");
    @Option(category = OptionCategory.INTERNAL, usageSyntax = "<function1>,<function2>,...", help = "List of R function to call debug on (implies +Instrument).") //
    public static final OptionKey<String> Rdebug = new OptionKey<>("");
    @Option(category = OptionCategory.EXPERT, stability = OptionStability.EXPERIMENTAL, usageSyntax = "<package1>,<package2>,...", help = "Activates LLVM debugging of shared libraries.") //
    public static final OptionKey<String> DebugLLVMLibs = new OptionKey<>("");
    @Option(category = OptionCategory.EXPERT, help = "Load the system, site and user profile scripts.") //
    public static final OptionKey<Boolean> LoadProfiles = new OptionKey<>(!FastRConfig.ManagedMode);
    @Option(category = OptionCategory.EXPERT, help = "Use 128 bit arithmetic in sum builtin.") //
    public static final OptionKey<Boolean> FullPrecisionSum = new OptionKey<>(false);
    @Option(category = OptionCategory.EXPERT, help = "Argument writes do not trigger state transitions.") //
    public static final OptionKey<Boolean> InvisibleArgs = new OptionKey<>(true);
    @Option(category = OptionCategory.EXPERT, help = "Disable reference count decrements.") //
    public static final OptionKey<Boolean> RefCountIncrementOnly = new OptionKey<>(false);
    @Option(category = OptionCategory.INTERNAL, help = "Whether the fast-path special call nodes should be created for simple enough arguments.") //
    public static final OptionKey<Boolean> UseSpecials = new OptionKey<>(true);
    @Option(category = OptionCategory.EXPERT, help = "Generate source sections for unserialized code.") //
    public static final OptionKey<Boolean> ForceSources = new OptionKey<>(false);
    @Option(category = OptionCategory.INTERNAL, help = "Whether all child contexts are to be shared contexts.") //
    public static final OptionKey<Boolean> SharedContexts = new OptionKey<>(true);
    @Option(category = OptionCategory.INTERNAL, help = "Whether all promises for frames on shared path are forced in presence of shared contexts.") //
    public static final OptionKey<Boolean> SearchPathForcePromises = new OptionKey<>(false);
    @Option(category = OptionCategory.EXPERT, help = "Load native code of packages, including builtin packages.") //
    public static final OptionKey<Boolean> LoadPackagesNativeCode = new OptionKey<>(!FastRConfig.ManagedMode);
    // Promises optimizations
    @Option(category = OptionCategory.INTERNAL, help = "If enabled, overrides all other EagerEval switches (see EagerEvalHelper).") //
    public static final OptionKey<Boolean> EagerEval = new OptionKey<>(false);
    @Option(category = OptionCategory.INTERNAL, help = "Unconditionally evaluates constants before creating Promises.") //
    public static final OptionKey<Boolean> EagerEvalConstants = new OptionKey<>(true);
    @Option(category = OptionCategory.INTERNAL, help = "Enables optimistic eager evaluation of single variables reads.") //
    public static final OptionKey<Boolean> EagerEvalVariables = new OptionKey<>(true);
    @Option(category = OptionCategory.INTERNAL, help = "Enables optimistic eager evaluation of single variables reads (for default parameters).") //
    public static final OptionKey<Boolean> EagerEvalDefault = new OptionKey<>(false);
    @Option(category = OptionCategory.INTERNAL, help = "Enables optimistic eager evaluation of trivial expressions.") //
    public static final OptionKey<Boolean> EagerEvalExpressions = new OptionKey<>(false);
    @Option(category = OptionCategory.INTERNAL, usageSyntax = "[0,inf)", help = "Enables inline caches for promises evaluation.") //
    public static final OptionKey<Integer> PromiseCacheSize = new OptionKey<>(3);
    @Option(category = OptionCategory.INTERNAL, usageSyntax = "[0, inf)", help = "Factor by which are multiplied all DSL 'limit' values where applicable.") //
    public static final OptionKey<Double> DSLCacheSizeFactor = new OptionKey<>(1.0);

    // Miscellaneous
    @Option(category = OptionCategory.INTERNAL, help = "Silently ignore unimplemented functions from graphics package.") //
    public static final OptionKey<Boolean> IgnoreGraphicsCalls = new OptionKey<>(false);
    @Option(category = OptionCategory.INTERNAL,
            usageSyntax = "<optionName>:<value>,<optionName>:<value>,...",
            help = "List of R level options default values. Value can be 'T' or 'F' in which case it is interpreted as boolean, otherwise as string.") //
    public static final OptionKey<String> AdditionalOptions = new OptionKey<>("");
    @Option(category = OptionCategory.INTERNAL, usageSyntax = "[0, inf)", help = "Enables timeout (in seconds) when receiving messages from a channel.") //
    public static final OptionKey<Integer> ChannelReceiveTimeout = new OptionKey<>(0);
    @Option(category = OptionCategory.EXPERT, help = "Restrict force splitting of call targets.") //
    public static final OptionKey<Boolean> RestrictForceSplitting = new OptionKey<>(true);
    @Option(category = OptionCategory.INTERNAL, help = "Turn on explicit GC via the gc built-in. Otherwise calls to gc are ignored.") //
    public static final OptionKey<Boolean> EnableExplicitGC = new OptionKey<>(false);

    // Discontinued since rc12
    // only a warning is printed to use the default logger mechanism
    // TODO remove at some later point
    @Option(category = OptionCategory.EXPERT, deprecated = true, deprecationMessage = "Deprecated since rc12", help = "Print FastR performance warning") //
    public static final OptionKey<Boolean> PerformanceWarnings = new OptionKey<>(false);
    @Option(category = OptionCategory.EXPERT, deprecated = true, deprecationMessage = "Deprecated since rc12", help = "Print a message for each non-trivial variable lookup")//
    public static final OptionKey<Boolean> PrintComplexLookups = new OptionKey<>(false);
    @Option(category = OptionCategory.INTERNAL, deprecated = true, deprecationMessage = "Deprecated since rc12", help = "Trace all R function calls") //
    public static final OptionKey<Boolean> TraceCalls = new OptionKey<>(false);
    @Option(category = OptionCategory.INTERNAL, deprecated = true, deprecationMessage = "Deprecated since rc12", help = "TraceCalls output is sent to 'fastr_tracecalls.log'") //
    public static final OptionKey<Boolean> TraceCallsToFile = new OptionKey<>(false);
    @Option(category = OptionCategory.INTERNAL, deprecated = true, deprecationMessage = "Deprecated since rc12", help = "Trace all native function calls (performed via .Call, .External, etc.)") //
    public static final OptionKey<Boolean> TraceNativeCalls = new OptionKey<>(false);

    @Option(category = OptionCategory.INTERNAL, help = "Internal option used by the experimental C embedding interface") //
    public static final OptionKey<Boolean> IsNativeEmbeddedMode = new OptionKey<>(false);

    /**
     * The presence of the <code>NATIVE_DATA_INSPECTOR</code> variable in the environment activates
     * the native data inspector JMX bean {@link NativeDataInspector} that can be used (e.g. via
     * VisualVM) to inspect native data mirrors.
     */
    public static final String NATIVE_DATA_INSPECTOR = "NATIVE_DATA_INSPECTOR";

    /**
     * For now we enforce that this option is set JVM wide, so that we can avoid reading it via
     * {@link RContext} on the fastr path.
     */
    @CompilationFinal public static boolean sharedContextsOptionValue;

    private final RContext context;

    private final Map<OptionKey<?>, Object> values = new HashMap<>();

    FastROptions(RContext context) {
        this.context = context;
    }

    @SuppressWarnings("unchecked")
    @TruffleBoundary
    <T> T getValue(OptionKey<T> key) {
        return (T) values.get(key);
    }

    @TruffleBoundary
    <T> void setValue(OptionKey<T> key, T value) {
        values.put(key, value);
    }

    private static boolean initializedFirstOptions;
    private boolean initialized;

    void initialize() {
        if (initialized) {
            return;
        }
        Iterator<OptionDescriptor> it = descriptors.iterator();
        while (it.hasNext()) {
            OptionDescriptor d = it.next();
            OptionKey<?> key = d.getKey();

            OptionValues optionValues = context.getEnv().getOptions();
            Object value = optionValues.get(key);
            if (!d.getKey().hasBeenSet(optionValues)) {
                if (value instanceof String && value.equals("")) {
                    // Truffle does not allow null as a default value,
                    // but FastR has some logic differing between "" and null (e.g. Debug)
                    value = null;
                }
                String name = d.getName();
                // skip the "R." prefix
                String fastrOptionName = name.substring(2, name.length());
                Object envValue = getEnvValue(fastrOptionName, value);
                if (envValue != null) {
                    RLogger.getLogger(this.getClass().getName()).fine("Note: FastR option " + name + " is overriden by env variable FASTR_OPTION_" + fastrOptionName + "=" + envValue);
                    value = envValue;
                }
            }
            values.put(key, value);
        }

        checkObsoleteJVMArgs();
        DSLConfig.initialize(getValue(DSLCacheSizeFactor));
        if (initializedFirstOptions && sharedContextsOptionValue != getValue(SharedContexts)) {
            throw RError.error(RError.NO_CALLER, Message.GENERIC, "FastR option ShareContexts can be set only to a single value per JVM/native-image instance.");
        }
        sharedContextsOptionValue = getValue(SharedContexts);
        initializedFirstOptions = true;
        initialized = true;
    }

    public static String getForwardedOptions(RContext context) {
        StringBuilder sb = new StringBuilder();
        if (context.getOption(PrintErrorStacktracesToFile)) {
            sb.append("--R.PrintErrorStacktracesToFile=true ");
        }
        if (context.getOption(EnableExplicitGC)) {
            sb.append("--R.EnableExplicitGC=true ");
        }
        if ("llvm".equals(context.getOption(BackEnd))) {
            sb.append("--R.BackEnd=llvm ");
        }
        if (context.getOption(BackEndLLVM) != null) {
            sb.append("--R.BackEndLLVM=").append(context.getOption(BackEndLLVM)).append(" ");
        }
        if (context.getOption(BackEndNative) != null) {
            sb.append("--R.BackEndNative=").append(context.getOption(BackEndNative)).append(" ");
        }
        return sb.toString();
    }

    @TruffleBoundary
    private static void checkObsoleteJVMArgs() {
        // The jvm arg syntax was dicontinued in rc13
        // For the time being a warning is printed
        // TODO remove at some later point
        for (Map.Entry<Object, Object> entry : System.getProperties().entrySet()) {
            String prop = (String) entry.getKey();
            if (prop.startsWith("R:")) {
                String name;
                if (prop.startsWith("R:+") || prop.startsWith("R:-")) {
                    name = prop.substring(3);
                } else {
                    name = prop.substring(2);
                }
                System.out.println("WARNING: The " + prop + " option was discontinued.\n" +
                                "You can rerun FastR with --R." + name + "[=...]");
            }
        }
    }

    public static OptionDescriptors getDescriptors() {
        return descriptors;
    }

    public static Object getEnvValue(String name, Object defaultValue) {
        String value = System.getenv().get("FASTR_OPTION_" + name);
        if (value == null || value.isEmpty()) {
            return value;
        }
        try {
            if (defaultValue instanceof String) {
                return value;
            } else if (defaultValue instanceof Boolean) {
                return Boolean.parseBoolean(value);
            } else if (defaultValue instanceof Double) {
                return Double.parseDouble(value);
            } else if (defaultValue instanceof Integer) {
                return Integer.parseInt(value);
            }
            return value;
        } catch (NumberFormatException e) {
            System.out.println("failed to parse value " + value + "for numeric option FASTR_OPTION_" + name);
            System.exit(2);
        }
        return null;
    }

    public static String getName(OptionKey<?> key) {
        Iterator<OptionDescriptor> it = FastROptions.getDescriptors().iterator();
        while (it.hasNext()) {
            OptionDescriptor d = it.next();
            if (d.getKey() == key) {
                return d.getName();
            }
        }
        throw RInternalError.shouldNotReachHere();
    }

    private Set<String> explicitPackages = null;
    private boolean isLLVMDefault;

    boolean isLLVMPackage(TruffleFile libPath) {
        if (explicitPackages == null) {
            initLLVMPackages();
        }
        if (explicitPackages.isEmpty()) {
            return isLLVMDefault;
        }

        assert libPath != null;
        String libName = libPath.getName();
        libName = libName.substring(0, libName.lastIndexOf('.'));
        return isLLVMPackage(libName);
    }

    boolean isLLVMPackage(String libName) {
        if (explicitPackages == null) {
            initLLVMPackages();
        }
        if (explicitPackages.isEmpty()) {
            return isLLVMDefault;
        }

        boolean isExplicitPkg = explicitPackages.contains(libName);
        return isExplicitPkg ^ isLLVMDefault;
    }

    private void initLLVMPackages() {
        if ("llvm".equals(System.getenv().get("FASTR_RFFI"))) {
            isLLVMDefault = true;
            explicitPackages = java.util.Collections.emptySet();
        } else {
            String backendOpt = getValue(FastROptions.BackEnd);
            String explicitPkgsOpt;
            if ("native".equals(backendOpt)) {
                isLLVMDefault = false;
                explicitPkgsOpt = getValue(FastROptions.BackEndLLVM);
            } else {
                // llvm
                isLLVMDefault = true;
                explicitPkgsOpt = getValue(FastROptions.BackEndNative);
            }

            String[] explicitPkgsOptSplit = explicitPkgsOpt == null ? null : explicitPkgsOpt.split(",");
            if (explicitPkgsOptSplit == null || explicitPkgsOptSplit.length == 0) {
                explicitPackages = java.util.Collections.emptySet();
            } else {
                explicitPackages = new HashSet<>();
                for (String pkg : explicitPkgsOptSplit) {
                    explicitPackages.add(pkg);
                }
            }
        }
    }
}
// @formatter:on
