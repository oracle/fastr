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

/**
 * Options to control the behavior of the FastR system, that relate to the implementation, i.e., are
 * <b>not</b> part of the standard set of R options or command line options. The syntax follows that
 * for Graal options except that a {@code D} (for setting system property) must be the first
 * character,e.g. {@code -DR:+OptionName} to set a boolean valued option to {@code true}.
 */
public class FastROptions {

    private static Map<String, Object> options = new HashMap<>();
    private static boolean printHelp;
    static {
        for (Entry<Object, Object> entry : System.getProperties().entrySet()) {
            String prop = (String) entry.getKey();
            if (prop.startsWith("R:")) {
                if (prop.equals("R:")) {
                    printHelp = true;
                } else if (prop.startsWith("R:+") || prop.startsWith("R:-")) {
                    String name = prop.substring(3);
                    if (!"".equals(entry.getValue())) {
                        System.out.println("-DR:[+-]" + name + " expected");
                        System.exit(2);
                    }
                    options.put(name, prop.startsWith("R:+"));
                } else {
                    String name = prop.substring(2);
                    if (entry.getValue() == null) {
                        System.out.println("-DR:" + name + "=value expected");
                        System.exit(2);
                    }
                    options.put(name, entry.getValue());
                }
            }
        }
    }

    public static final boolean PrintErrorStacktraces = parseBooleanOption("PrintErrorStacktraces", false, "Prints Java and R stack traces for all errors");
    public static final boolean PrintErrorStacktracesToFile = parseBooleanOption("PrintErrorStacktracesToFile", true, "Dumps Java and R stack traces to file for all errors");
    public static final boolean CheckResultCompleteness = parseBooleanOption("CheckResultCompleteness", true, "Assert completeness of results vectors after evaluating unit tests and R shell commands");
    public static String Debug = parseStringOption("Debug", null, "Debug=name1,name2,...; Turn on debugging output for 'name1', 'name2', etc.");
    public static final boolean Instrument = parseBooleanOption("Instrument", false, "Enable Instrumentation");
    public static final boolean TraceCalls = parseBooleanOption("TraceCalls", false, "Trace all R function calls (implies +Instrument)");
    public static final String PerfStats = parseStringOption("PerfStats", null, "PerfStats=p1,p2,...; Collect performance stats identified by p1, etc.");
    public static final String PerfStatsFile = parseStringOption("PerfStatsFile", null, "PerfStatsFile=file; Send performance stats to 'file', default stdout");
    public static final String Rdebug = parseStringOption("Rdebug", null, "Rdebug=f1,f2.,,,; list of R function to call debug on (implies +Instrument)");
    public static final boolean PerformanceWarnings = parseBooleanOption("PerformanceWarnings", false, "Print FastR performance warning");
    public static final boolean LoadBase = parseBooleanOption("LoadBase", true, "Load base package");
    public static final boolean PrintComplexLookups = parseBooleanOption("PrintComplexLookups", false, "Print a message for each non-trivial variable lookup");
    public static final boolean IgnoreVisibility = parseBooleanOption("IgnoreVisibility", false, "Ignore setting of the visibility flag");
    public static final boolean LoadPkgSourcesIndex = parseBooleanOption("LoadPkgSourcesIndex", true, "Load R package sources index");
    public static final boolean InvisibleArgs = parseBooleanOption("InvisibleArgs", true, "Argument writes do not trigger state transitions");
    public static final boolean NewStateTransition = parseBooleanOption("NewStateTransition", false, "Experimental state transition implementation");
    public static final boolean RefCountIncrementOnly = parseBooleanOption("RefCountIncrementOnly", false, "Disable reference count decrements for eperimental state transition implementation");
    public static final boolean UseNewVectorNodes = parseBooleanOption("UseNewVectorNodes", false, "temporary option");

    // Promises optimizations
    public static final boolean EagerEval = parseBooleanOption("EagerEval", false, "If enabled, overrides all other EagerEval switches (see EagerEvalHelper)");
    public static final boolean EagerEvalConstants = parseBooleanOption("EagerEvalConstants", true, "Unconditionally evaluates constants before creating Promises");
    public static final boolean EagerEvalVariables = parseBooleanOption("EagerEvalVariables", true, "Enables optimistic eager evaluation of single variables reads");
    public static final boolean EagerEvalDefault = parseBooleanOption("EagerEvalDefault", false, "Enables optimistic eager evaluation of single variables reads (for default parameters)");
    public static final boolean EagerEvalExpressions = parseBooleanOption("EagerEvalExpressions", false, "Enables optimistic eager evaluation of trivial expressions");

    static {
        if (options != null && !options.isEmpty()) {
            System.err.println(options.keySet() + " are not valid FastR options");
            System.exit(2);
        }
        if (printHelp) {
            System.exit(0);
        }
    }

    public static boolean parseBooleanOption(String name, boolean defaultValue, String help) {
        if (printHelp) {
            System.out.printf("%35s %s (default: %b)\n", "-DR:" + name, help, defaultValue);
            return false;
        } else {
            Object optionValue = options.remove(name);
            if (optionValue == null) {
                return defaultValue;
            } else {
                if (!(optionValue instanceof Boolean)) {
                    System.out.println("-DR:[+-]" + name + " expected");
                    System.exit(2);
                }
                return (Boolean) optionValue;
            }
        }
    }

    public static String parseStringOption(String name, String defaultValue, String help) {
        if (printHelp) {
            System.out.printf("%35s %s (default: %b)\n", "-DR:" + name, help, defaultValue);
            return "";
        } else {
            Object optionValue = options.remove(name);
            if (optionValue == null) {
                return defaultValue;
            } else {
                if (!(optionValue instanceof String)) {
                    System.out.println("-DR:" + name + "=value expected");
                    System.exit(2);
                    return null;
                }
                return (String) optionValue;
            }
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
        String s = Debug;
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
        Debug = s;
    }
}
