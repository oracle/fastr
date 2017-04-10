/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Map;
import java.util.Map.Entry;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;

/**
 * Options to control the behavior of the FastR system, that relate to the implementation, i.e., are
 * <b>not</b> part of the standard set of R options or command line options. The syntax follows that
 * for Graal options except that a {@code D} (for setting system property) must be the first
 * character,e.g. {@code -DR:+OptionName} to set a boolean valued option to {@code true}.
 *
 * N.B. The options must be initialized/processed at runtime for an AOT VM.
 */
public enum FastROptions {
    PrintErrorStacktraces("Prints Java and R stack traces for all errors", false),
    PrintErrorStacktracesToFile("Dumps Java and R stack traces to 'fastr_errors.log' for all errors", true),
    CheckResultCompleteness("Assert completeness of results vectors after evaluating unit tests and R shell commands", true),
    Debug("Debug=name1,name2,...; Turn on debugging output for 'name1', 'name2', etc.", null, true),
    TraceCalls("Trace all R function calls", false),
    TraceCallsToFile("TraceCalls output is sent to 'fastr_tracecalls.log'", false),
    TraceNativeCalls("Trace all native function calls (performed via .Call, .External, etc.)", false),
    Rdebug("Rdebug=f1,f2.,,,; list of R function to call debug on (implies +Instrument)", null, true),
    PerformanceWarnings("Print FastR performance warning", false),
    LoadBase("Load base package", true),
    PrintComplexLookups("Print a message for each non-trivial variable lookup", false),
    FullPrecisionSum("Use 128 bit arithmetic in sum builtin", false),
    InvisibleArgs("Argument writes do not trigger state transitions", true),
    RefCountIncrementOnly("Disable reference count decrements for experimental state transition implementation", false),
    UseInternalGridGraphics("Whether the internal (Java) grid graphics implementation should be used", true),
    UseSpecials("Whether the fast-path special call nodes should be created for simple enough arguments.", true),
    ForceSources("Generate source sections for unserialized code", false),

    // Promises optimizations
    EagerEval("If enabled, overrides all other EagerEval switches (see EagerEvalHelper)", false),
    EagerEvalConstants("Unconditionally evaluates constants before creating Promises", true),
    EagerEvalVariables("Enables optimistic eager evaluation of single variables reads", true),
    EagerEvalDefault("Enables optimistic eager evaluation of single variables reads (for default parameters)", false),
    EagerEvalExpressions("Enables optimistic eager evaluation of trivial expressions", false),
    PromiseCacheSize("Enables inline caches for promises evaluation", "3", true);

    private final String help;
    private final boolean isBoolean;
    private final Object defaultValue;
    @CompilationFinal Object value;

    FastROptions(String help, boolean defaultValue) {
        this(help, defaultValue, false);
    }

    FastROptions(String help, Object defaultValue, boolean isString) {
        this.help = help;
        this.isBoolean = !isString;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    public boolean getBooleanValue() {
        assert isBoolean;
        Object v = value;
        if (v instanceof Boolean) {
            return (boolean) v;
        } else {
            CompilerDirectives.transferToInterpreter();
            System.out.println("boolean option value expected with " + name() + " - forgot +/- ?");
            System.exit(2);
            return false;
        }
    }

    public String getStringValue() {
        assert !isBoolean;
        Object v = value;
        if (v == null || v instanceof String) {
            return (String) v;
        } else {
            CompilerDirectives.transferToInterpreter();
            System.out.println("string option value expected with " + name());
            System.exit(2);
            return "";
        }
    }

    public int getNonNegativeIntValue() {
        assert !isBoolean;
        Object v = value;
        if (v instanceof Integer) {
            return (Integer) v;
        }
        CompilerDirectives.transferToInterpreterAndInvalidate();
        if (v instanceof String) {
            try {
                int res = Integer.decode((String) v);
                if (res >= 0) {
                    value = res;
                    return res;
                } // else fall through to error message
            } catch (NumberFormatException x) {
                // fall through to error message
            }
        }

        System.out.println("non negative integer option value expected with " + name());
        System.exit(2);
        return -1;

    }

    private static FastROptions[] VALUES = values();

    static void setValue(String name, Object value) {
        for (FastROptions option : VALUES) {
            if (name.equals(option.name())) {
                option.value = value;
                return;
            }
        }
        System.out.println("unknown FastR option: " + name + " (value: " + value + ")");
        System.exit(2);
    }

    private static boolean initialized;

    public static void initialize() {
        if (initialized) {
            return;
        }
        // Check for help first
        if (System.getProperty("R:") != null || System.getProperty("R:help") != null) {
            FastROptions.printHelp();
            System.exit(0);
        }
        for (Entry<Object, Object> entry : System.getProperties().entrySet()) {
            String prop = (String) entry.getKey();
            if (prop.startsWith("R:")) {
                if (prop.startsWith("R:+") || prop.startsWith("R:-")) {
                    String name = prop.substring(3);
                    if (!"".equals(entry.getValue())) {
                        System.out.println("-DR:[+-]" + name + " expected");
                        System.exit(2);
                    }
                    FastROptions.setValue(name, prop.startsWith("R:+"));
                } else {
                    String name = prop.substring(2);
                    if (entry.getValue() == null) {
                        System.out.println("-DR:" + name + "=value expected");
                        System.exit(2);
                    }
                    FastROptions.setValue(name, entry.getValue());
                }
            }
        }
        for (Map.Entry<String, String> entry : System.getenv().entrySet()) {
            String name = entry.getKey();
            if (name.startsWith("FASTR_OPTION_")) {
                name = name.replace("FASTR_OPTION_", "");
                String value = entry.getValue();
                if (value == null || value.equals("true") || value.equals("")) {
                    FastROptions.setValue(name, true);
                } else if (value.equals("false")) {
                    FastROptions.setValue(name, false);
                } else {
                    FastROptions.setValue(name, value);
                }
            }
        }
        initialized = true;
    }

    static void printHelp() {
        for (FastROptions option : VALUES) {
            String prefix = "%35s %s (default: ";
            String format = prefix + (option.isBoolean ? "%b" : "%s") + ")\n";
            System.out.printf(format, "-DR:" + option.name(), option.help, option.defaultValue);
        }
    }

    /**
     * Convenience function for matching against the Debug option.
     *
     */
    public static boolean debugMatches(String element) {
        return matchesElement(element, FastROptions.Debug.getStringValue()) != null;
    }

    /**
     * Convenience function for matching against an option whose value is expected to be a comma
     * separated list. If the option is set without a value, i.e. just plain {@code -R:Option}, all
     * elements are deemed to match. Matching is done with {@link String#startsWith} to allow
     * additional data to be tagged onto the element.
     *
     * E.g.
     * <ul>
     * <li>{@code -R:Option} returns {@code ""} for all values of {@code element}.</li>
     * <li>{@code -R:Option=foo} returns {@code foo} iff {@code element.equals("foo")}, else
     * {@code null}.
     * <li>{@code -R:Option=foo,bar=xx} returns {@code bar=xx} iff {@code element.equals("bar")},
     * else {@code null}.
     *
     * @param element string to match against the option value list.
     * @return {@code ""} if the option is set with no {@code =value} component, the element if
     *         {@code element} matches an element, {@code null} otherwise.
     */
    public static String matchesElement(String element, String option) {
        if (option == null) {
            return null;
        } else if (option.length() == 0) {
            return option;
        } else {
            String[] parts = option.split(",");
            for (String part : parts) {
                if (part.startsWith(element)) {
                    return option;
                }
            }
        }
        return null;
    }

    /**
     * Updates the value of the Debug option, adding only. TODO maybe support removal if there is a
     * use-case.
     */
    public static void debugUpdate(String element) {
        String s = FastROptions.Debug.getStringValue();
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
        FastROptions.Debug.value = s;
    }

    public static boolean noEagerEval() {
        return !(EagerEval.getBooleanValue() || EagerEvalConstants.getBooleanValue() || EagerEvalVariables.getBooleanValue() || EagerEvalDefault.getBooleanValue() ||
                        EagerEvalExpressions.getBooleanValue());
    }
}
