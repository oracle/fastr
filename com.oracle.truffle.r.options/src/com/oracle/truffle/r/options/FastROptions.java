/*
 * Copyright (c) 2014, 2014, Oracle and/or its affiliates. All rights reserved.
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
 * Currently it is not possible to include the FastR options in Graal as FastR is not part of graal.
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
    @Option(help = "Turn on debugging output")
    public static final OptionValue<Boolean> Debug = new OptionValue<>(false);
    @Option(help = "Disable all Instrumentation")
    public static final OptionValue<Boolean> DisableInstrumentation = new OptionValue<>(false);
    @Option(help = "Instrument default packages")
    public static final OptionValue<Boolean> InstrumentDefaultPackages = new OptionValue<>(false);
    @Option(help = "Add function call counters")
    public static final OptionValue<Boolean> AddFunctionCounters = new OptionValue<>(false);
    //@formatter:on

    private static FastROptions_Options options = new FastROptions_Options();

    public static final void initialize() {

        Iterator<OptionDescriptor> iter = options.iterator();
        while (iter.hasNext()) {
            OptionDescriptor desc = iter.next();
            String name = desc.getName();
            Entry<Object, Object> propEntry = propEntry(name);
            if (propEntry != null) {
                switch (desc.getType().getSimpleName()) {
                    case "Boolean":
                        boolean value = booleanOptionValue((String) propEntry.getKey());
                        desc.getOptionValue().setValue(value);
                        break;
                    default:
                        assert false;
                }
            }
        }
    }

    private static Entry<Object, Object> propEntry(String name) {
        for (Entry<Object, Object> entry : System.getProperties().entrySet()) {
            String prop = (String) entry.getKey();
            if (prop.startsWith("R:") && prop.contains(name)) {
                return entry;
            }
        }
        return null;
    }

    private static boolean booleanOptionValue(String option) {
        return option.charAt(2) == '+';
    }
}
