/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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
import java.util.Map.Entry;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.r.runtime.RCmdOptions.*;

/**
 * Options to control the behavior of the FastR system, that relate to the implementation, i.e., are
 * <b>not</b> part of the standard set of R options or command line options.
 *
 * Currently it is not possible to include the FastR options in Graal as FastR is not part of Graal.
 * Someday it may be possible to register such options with the Graal VM, but in the interim, we
 * override the implementation to use system properties. The syntax follows that for Graal options
 * except that a {@code D} (for setting system property) must be the first character,e.g.
 * {@code -DR:+OptionName} to set a boolean valued option to {@code true}.
 */
public class FastROptions {
    private static final List<Option<?>> optionsList = new ArrayList<>();

    //@formatter:off
    public static final Option<Boolean> PrintErrorStacktraces = newBooleanOption("PrintErrorStacktraces", false, "Prints Java and R stack traces for all errors");
    public static final Option<Boolean> PrintErrorStacktracesToFile = newBooleanOption("PrintErrorStacktracesToFile", true, "Dumps Java and R stack traces to file for all errors");
    public static final Option<Boolean> CheckResultCompleteness = newBooleanOption("CheckResultCompleteness", true, "Assert completeness of results vectors after evaluating unit tests and R shell commands");
    public static final Option<String>  Debug = newStringOption("Debug", null, "Debug=name1,name2,...; Turn on debugging output for 'name1', 'name2', etc.");
    public static final Option<Boolean> Instrument = newBooleanOption("Instrument", false, "Enable Instrumentation");
    public static final Option<Boolean> TraceCalls = newBooleanOption("TraceCalls", false, "Trace all R function calls (implies +Instrument)");
    public static final Option<String>  PerfStats = newStringOption("PerfStats", null, "PerfStats=p1,p2,...; Collect performance stats identified by p1, etc.");
    public static final Option<String>  PerfStatsFile = newStringOption("PerfStatsFile", null, "PerfStatsFile=file; Send performance stats to 'file', default stdout");
    public static final Option<String>  Rdebug = newStringOption("Rdebug", null, "Rdebug=f1,f2.,,,; list of R function to call debug on (implies +Instrument)");
    public static final Option<Boolean> PerformanceWarnings = newBooleanOption("PerformanceWarnings", false, "Print FastR performance warning");
    public static final Option<Boolean> LoadBase = newBooleanOption("LoadBase", true, "Load base package");
    public static final Option<Boolean> PrintComplexLookups = newBooleanOption("PrintComplexLookups", false, "Print a message for each non-trivial variable lookup");
    public static final Option<Boolean> IgnoreVisibility = newBooleanOption("IgnoreVisibility", false, "Ignore setting of the visibility flag");
    public static final Option<Boolean> LoadPkgSourcesIndex = newBooleanOption("LoadPkgSourcesIndex", true, "Load R package sources index");
    public static final Option<Boolean> InvisibleArgs = newBooleanOption("InvisibleArgs", true, "Argument writes do not trigger state transitions");
    public static final Option<Boolean> ExperimentalStateTrans = newBooleanOption("ExperimentalStateTrans", false, "Eperimental state transition implementation");
    public static final Option<Boolean> RefCountIncOnly = newBooleanOption("RefCountIncOnly", true, "Disable reference count decrements for eperimental state transition implementation");

    // Promises optimizations
    public static final Option<Boolean> EagerEval = newBooleanOption("EagerEval", false, "If enabled, overrides all other EagerEval switches (see EagerEvalHelper)");
    public static final Option<Boolean> EagerEvalConstants = newBooleanOption("EagerEvalConstants", true, "Unconditionally evaluates constants before creating Promises");
    public static final Option<Boolean> EagerEvalVariables = newBooleanOption("EagerEvalVariables", true, "Enables optimistic eager evaluation of single variables reads");
    public static final Option<Boolean> EagerEvalDefault = newBooleanOption("EagerEvalDefault", false, "Enables optimistic eager evaluation of single variables reads (for default parameters)");
    public static final Option<Boolean> EagerEvalExpressions = newBooleanOption("EagerEvalExpressions", false, "Enables optimistic eager evaluation of trivial expressions");
    //@formatter:on

    @CompilationFinal public static boolean NewStateTransition;
    @CompilationFinal public static boolean RefCountIncrementOnly;

    private static boolean initialized;

    public static Option<Boolean> newBooleanOption(String name, boolean defaultValue, String help) {
        Option<Boolean> option = new Option<>(true, OptionType.BOOLEAN, null, name, help, defaultValue);
        optionsList.add(option);
        return option;
    }

    public static Option<String> newStringOption(String name, String defaultValue, String help) {
        Option<String> option = new Option<>(null, true, OptionType.STRING, null, name, help, defaultValue);
        optionsList.add(option);
        return option;
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        CompilerDirectives.transferToInterpreterAndInvalidate();
        initialized = true;
        for (Entry<Object, Object> entry : System.getProperties().entrySet()) {
            String prop = (String) entry.getKey();
            if (prop.equals("R")) {
                printHelp();
                Utils.exit(0);
            }
            if (prop.startsWith("R:")) {
                String optName = optionName(prop);
                Option<?> option = findOption(optName);
                if (option == null) {
                    System.err.println(prop + " is not a valid FastR option");
                    System.exit(2);
                } else {
                    switch (option.type) {
                        case BOOLEAN: {
                            boolean value = booleanOptionValue(prop);
                            option.setValue(value);
                            break;
                        }

                        case STRING: {
                            String value = (String) entry.getValue();
                            option.setValue(value);
                            break;
                        }

                        default:
                            throw RInternalError.shouldNotReachHere();
                    }
                }
            }
        }
        NewStateTransition = FastROptions.ExperimentalStateTrans.getValue();
        RefCountIncrementOnly = FastROptions.RefCountIncOnly.getValue();
        // debug();
    }

    private static void printHelp() {
        for (Option<?> option : optionsList) {
            String helpName = option.plainName;
            System.out.printf("    -DR:%s", helpName);
            int spaces;
            int optLength = helpName.length() + 8;
            if (optLength >= 22) {
                System.out.println();
                spaces = 22;
            } else {
                spaces = 22 - optLength;
            }
            for (int i = 0; i < spaces; i++) {
                System.out.print(' ');
            }
            System.out.println(option.help);
        }
    }

    /**
     * Convenience function for matching against the Debug option.
     *
     */
    public static boolean debugMatches(String element) {
        return matchesElement(element, Debug) != null;
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
    public static String matchesElement(String element, Option<String> stringOption) {
        initialize();
        String s = stringOption.getValue();
        if (s == null) {
            return null;
        } else if (s.length() == 0) {
            return s;
        } else {
            String[] parts = s.split(",");
            for (String part : parts) {
                if (part.startsWith(element)) {
                    return s;
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
        String s = Debug.getValue();
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
        Debug.setValue(s);
    }

    private static Option<?> findOption(String key) {
        for (Option<?> option : optionsList) {
            if (option.plainName.equals(key)) {
                return option;
            }
        }
        return null;
    }

    private static String optionName(String key) {
        String s = key.substring(2);
        if (s.charAt(0) == '+' || s.charAt(0) == '-') {
            return s.substring(1);
        } else if (s.indexOf('=') > 0) {
            return s.substring(0, s.indexOf('='));
        } else {
            return s;
        }
    }

    private static boolean booleanOptionValue(String option) {
        return option.charAt(2) == '+';
    }

    @SuppressWarnings("unused")
    private static void debug() {
        for (Option<?> option : optionsList) {
            System.out.printf("%s: ", option.plainName);
            System.out.println(option.getValue());
        }
    }

}
