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
package com.oracle.truffle.r.options;

import java.util.*;
import java.util.Map.Entry;

import com.oracle.graal.options.*;

/**
 * Options to control the behavior of the FastR system, that relate to the implementation, i.e., are
 * <b>not</b> part of the standard set of R options or command line options.
 *
 * Currently it is not possible to include the FastR options in Graal as FastR is not part of Graal.
 * Someday it may be possible to register such options with the Graal VM, but in the interim, we
 * override the implementation to use system properties. The syntax follows that for Graal options
 * except that a {@code D} (for setting system property) must be the first character,e.g.
 * {@code -DR:+OptionName} to set a boolean valued option to {@code true}.
 *
 * TODO help output
 */
public class FastROptions {
    //@formatter:off
    @Option(help = "Disable prototypical group generics implementation")
    public static final OptionValue<Boolean> DisableGroupGenerics = new OptionValue<>(false);
    @Option(help = "Prints Java and R stack traces for all R errors")
    public static final OptionValue<Boolean> PrintErrorStacktraces = new OptionValue<>(false);
    @Option(help = "Assert completeness of results vectors after evaluating unit tests and R shell commands")
    public static final OptionValue<Boolean> CheckResultCompleteness = new OptionValue<>(true);
    @Option(help = "Debug=name1,name2,...; Turn on debugging output for 'name1', 'name2', etc.")
    public static final OptionValue<String> Debug = new OptionValue<>(null);
    @Option(help = "Enable Instrumentation")
    public static final OptionValue<Boolean> Instrument = new OptionValue<>(false);
    @Option(help = "Trace all R function calls (requires +Instrumentation)")
    public static final OptionValue<Boolean> TraceCalls = new OptionValue<>(false);
    @Option(help = "PerfStats=p1,p2,...; Collect performance stats identified by p1, etc.")
    public static final OptionValue<String> PerfStats = new OptionValue<>(null);
    @Option(help = "PerfStatsFile=file; Send performance stats to 'file', default stdout")
    public static final OptionValue<String> PerfStatsFile = new OptionValue<>(null);
    @Option(help = "Rdebug=f1,f2.,,,; list of R function to call debug on (implies +Instrument)")
    public static final OptionValue<String> Rdebug = new OptionValue<>(null);
    @Option(help = "Print FastR performance warning")
    public static final OptionValue<Boolean> PerformanceWarnings = new OptionValue<>(false);
    @Option(help = "Load base package")
    public static final OptionValue<Boolean> LoadBase = new OptionValue<>(true);
    @Option(help = "Print a message for each non-trivial variable lookup")
    public static final OptionValue<Boolean> PrintComplexLookups = new OptionValue<>(false);

    // Promises optimizations
    @Option(help = "If enabled, overrides all other EagerEval switches (see EagerEvalHelper)")
    public static final OptionValue<Boolean> EagerEval = new OptionValue<>(false);
    @Option(help = "Unconditionally evaluates constants before creating Promises")
    public static final OptionValue<Boolean> EagerEvalConstants = new OptionValue<>(true);
    @Option(help = "Enables optimistic eager evaluation of single variables reads")
    public static final OptionValue<Boolean> EagerEvalVariables = new OptionValue<>(false);
    @Option(help = "Enables optimistic eager evaluation of trivial expressions")
    public static final OptionValue<Boolean> EagerEvalExpressions = new OptionValue<>(false);
    //@formatter:on

    private static FastROptions_Options options = new FastROptions_Options();
    private static boolean initialized;

    public static void initialize() {
        if (initialized) {
            return;
        }
        for (Entry<Object, Object> entry : System.getProperties().entrySet()) {
            String prop = (String) entry.getKey();
            if (prop.startsWith("R:")) {
                String optName = optionName(prop);
                OptionDescriptor desc = findOption(optName);
                if (desc == null) {
                    System.err.println(prop + " is not a valid FastR option");
                    System.exit(2);
                } else {
                    switch (desc.getType().getSimpleName()) {
                        case "Boolean": {
                            boolean value = booleanOptionValue(prop);
                            desc.getOptionValue().setValue(value);
                            break;
                        }

                        case "String": {
                            String value = (String) entry.getValue();
                            desc.getOptionValue().setValue(value);
                            break;
                        }
                        default:
                            assert false;
                    }
                }
            }
        }
        initialized = true;
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
    public static String matchesElement(String element, OptionValue<String> stringOption) {
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

    private static OptionDescriptor findOption(String key) {
        Iterator<OptionDescriptor> iter = options.iterator();
        while (iter.hasNext()) {
            OptionDescriptor desc = iter.next();
            String name = desc.getName();
            if (name.equals(key)) {
                return desc;
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

}
