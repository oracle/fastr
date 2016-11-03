/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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

package com.oracle.truffle.r.test.tools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.oracle.truffle.r.nodes.builtin.RBuiltinFactory;
import com.oracle.truffle.r.nodes.builtin.base.BasePackage;
import com.oracle.truffle.r.runtime.RVersionNumber;
import com.oracle.truffle.r.runtime.RVisibility;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.builtins.RBuiltinKind;
import com.oracle.truffle.r.test.TestBase.Ignored;

/**
 * Utility to analyze builtins implemented in FastR vs. builtins available in GNU R. The GNU R
 * builtins are extracted from the source code, namely file main/names.c. The FastR builtins are
 * taken from the BasePackage class.
 * <p>
 * This Utility accepts two options:
 * <ul>
 * <li>--filter with values 'gnur-only', 'fastr-only', 'both' (builtins with matching signature),
 * 'both-diff' (matching name, but not signature, e.g. visibility). Multiple values can be combined
 * by ',', e.g. "--filter gnur-only,fastr-only".</li>
 * <li>--suite-path: the path to the FastR MX suite (in order to find main/names.c of GNU R, note:
 * this is provided by mx wrapper)</li>
 * </ul>
 */
public final class RBuiltinCheck {

    private static final String DEFAULT_NAMESC = "com.oracle.truffle.r.native/gnur/" + RVersionNumber.R_HYPHEN_FULL + "/src/main/names.c";
    private static final String BUILTIN_TEST_PATH = "com.oracle.truffle.r.test/src/com/oracle/truffle/r/test/builtins/TestBuiltin_%s.java";

    // old-style code annotation to get rid of javadoc error.
    /**
     * Regular expression used to match FUNTAB entries in GNU R names.c code. Example entry:
     * <code>{"if", do_if, 0, 200, -1, {PP_IF, PREC_FN, 1}}</code>
     */
    private static final String FUNTAB_REGEXP = "\\{\\s*\\\"(?<name>(\\.|[^\"])+)\\\"\\s*," +
                    "[^,]*,[^,]*,\\s*(?<eval>\\d{1,3})\\s*,\\s*(?<arity>-?\\d+)\\s*," +
                    "\\s*\\{\\s*(?<ppkind>\\w+)\\s*,\\s*(?<precedence>\\w+)\\s*,\\s*(?<rightassoc>[01])\\s*}\\s*}\\s*";

    public enum FilterOption {
        GNUR_ONLY,
        FASTR_ONLY,
        BOTH,
        BOTH_DIFF;
    }

    public static class BuiltinInfo {

        public final RVisibility visibility;

        public final boolean isInternal;

        public final boolean evalArgs;

        public final int arity;

        public BuiltinInfo(RVisibility visibility, boolean isInternal, boolean evalArgs, int arity) {
            this.visibility = visibility;
            this.isInternal = isInternal;
            this.evalArgs = evalArgs;
            this.arity = arity;
        }

        /**
         * If FastR visibility is set, it must match GnuR. Otherwise we do not check anything.
         */
        public static boolean visibilityMatch(BuiltinInfo fastr, BuiltinInfo gnur) {
            return fastr.visibility == RVisibility.CUSTOM || fastr.visibility == gnur.visibility;
        }

        /**
         * If both arities are set, they must match. Otherwise we do not check anything.
         */
        public static boolean arityMatch(BuiltinInfo fastr, BuiltinInfo gnur) {
            return fastr.arity == -1 || gnur.arity == -1 || fastr.arity == gnur.arity;
        }

        public static boolean isInternalMatch(BuiltinInfo fastr, BuiltinInfo gnur) {
            return fastr.isInternal == gnur.isInternal;
        }

        public static boolean evalArsgMatch(BuiltinInfo fastr, BuiltinInfo gnur) {
            return fastr.evalArgs == gnur.evalArgs;
        }

        public boolean matchesGnuR(BuiltinInfo gnur) {
            return isInternalMatch(this, gnur) && evalArsgMatch(this, gnur) && visibilityMatch(this, gnur) && arityMatch(this, gnur);
        }
    }

    public static class BuiltinTuple {
        public final BuiltinInfo gnur;
        public final BuiltinInfo fastr;

        public BuiltinTuple(BuiltinInfo gnur, BuiltinInfo fastr) {
            super();
            this.gnur = gnur;
            this.fastr = fastr;
        }
    }

    public static void main(String[] args) {
        // Simple arguments parsing:
        EnumSet<FilterOption> show = EnumSet.allOf(FilterOption.class);
        String suitePath = null;
        int i = 0;
        while (i < args.length) {
            if (args[i].startsWith("--filter") && i < args.length) {
                try {
                    i++;
                    show.clear();
                    Arrays.stream(args[i].split(",")).map(s -> s.toUpperCase().replace('-', '_')).forEach(s -> show.add(FilterOption.valueOf(s)));
                } catch (IllegalArgumentException ex) {
                    System.err.println("Unrecognized value for option --filter: '" + args[i] + "'.");
                    System.exit(1);
                }
            } else if (args[i].startsWith("--suite-path") && i < args.length) {
                i++;
                suitePath = args[i];
            } else {
                System.err.println("Unrecognized option: " + args[i]);
                System.exit(1);
            }
            i++;
        }

        // Build the sets with gnur/fastr builtins and their intersection
        final Map<String, BuiltinInfo> fastr = extractFastRBuiltins();
        final Map<String, BuiltinInfo> gnur;
        try {
            gnur = extractGnuRBuiltins(suitePath);
        } catch (IOException ex) {
            System.err.println("Cannot read names.c file from GNU R distribution. \n" + ex.getClass().getName() + ":" + ex.getMessage());
            System.exit(1);
            return; // so that gnur can be final
        }

        Set<String> both = new TreeSet<>(fastr.keySet());
        both.retainAll(gnur.keySet());

        Set<String> fastrOnly = keysWithout(fastr, both);
        Set<String> gnurOnly = keysWithout(gnur, both);

        Map<String, BuiltinTuple> descriptorsDiff = both.stream().filter(name -> !fastr.get(name).matchesGnuR(gnur.get(name))).collect(
                        Collectors.toMap(Function.identity(), name -> new BuiltinTuple(gnur.get(name), fastr.get(name))));
        both.removeAll(descriptorsDiff.keySet());

        // Print the results
        printOutput(suitePath, show, both, descriptorsDiff, fastrOnly, gnurOnly);
    }

    private static void printOutput(String suitePath, EnumSet<FilterOption> show, Set<String> both, Map<String, BuiltinTuple> descriptorsDiff, Set<String> fastrOnly, Set<String> gnurOnly) {
        String prefix = "";
        if (show.contains(FilterOption.BOTH)) {
            prefix = printHeader(prefix, show, "Builtins in both GNU R and FastR with matching descriptors: " + both.size());
            both.stream().forEach(b -> printBuiltinWithTest(b, suitePath));
        }

        if (show.contains(FilterOption.BOTH_DIFF)) {
            prefix = printHeader(prefix, show, "Builtins in both GNU R and FastR with different descriptors: " + descriptorsDiff.size());
            if (descriptorsDiff.size() > 1) {
                // differences are printed as a table:
                final String tableTempl = "%1$-22s|%2$15s|%3$15s|%4$12s|%5$10s\n";
                System.out.println("all cells: fastr/gnur");
                System.out.printf(tableTempl, " name ", " is internal ", " eval args ", " visibility ", " arity ");
                System.out.println(Stream.generate(() -> "-").limit(82).collect(Collectors.joining()));
                for (Map.Entry<String, BuiltinTuple> entry : descriptorsDiff.entrySet()) {
                    String isInternal = formatTableCell("%b / %b", entry.getValue(), x -> x.isInternal, BuiltinInfo::isInternalMatch);
                    String evalArgs = formatTableCell("%b / %b", entry.getValue(), x -> x.evalArgs, BuiltinInfo::evalArsgMatch);
                    String visibility = formatTableCell("%.3s / %.3s", entry.getValue(), x -> x.visibility, BuiltinInfo::visibilityMatch);
                    String arity = formatTableCell("%d / %d", entry.getValue(), x -> x.arity, BuiltinInfo::arityMatch);
                    System.out.printf(tableTempl, entry.getKey(), isInternal, evalArgs, visibility, arity);
                }
            }
        }

        if (show.contains(FilterOption.GNUR_ONLY)) {
            prefix = printHeader(prefix, show, "Builtins only in GNU R (i.e. TODO list): " + gnurOnly.size());
            gnurOnly.stream().forEach(System.out::println);
        }

        if (show.contains(FilterOption.FASTR_ONLY)) {
            printHeader(prefix, show, "Builtins only in FastR: " + fastrOnly.size());
            fastrOnly.stream().forEach(System.out::println);
        }
    }

    private static void printBuiltinWithTest(String builtin, String suitePath) {
        String name = builtin.replace(".", "");
        name = name.replace("<-", "assign");
        try {
            String content = Files.lines(Paths.get(suitePath, String.format(BUILTIN_TEST_PATH, name))).collect(Collectors.joining("\n"));
            int tests = content.split("assertEval").length - 1;
            int ignoredTests = content.split(Ignored.class.getSimpleName()).length - 1;
            if (tests > 0) {
                String testDetails = String.format("%3d%% (%d/%d)", (tests - ignoredTests) * 100 / tests, tests - ignoredTests, tests);
                System.out.printf("%-15s %s%n", builtin, testDetails);
                return;
            }
        } catch (IOException e) {
            // nothing to do
        }
        System.out.printf("%-20s (no tests found)%n", builtin);
    }

    private static String formatTableCell(String format, BuiltinTuple tuple, Function<BuiltinInfo, Object> selector, BiPredicate<BuiltinInfo, BuiltinInfo> cmp) {
        String result = String.format(format, selector.apply(tuple.fastr), selector.apply(tuple.gnur));
        if (!cmp.test(tuple.fastr, tuple.gnur)) {
            return '»' + result + '«';
        }
        return ' ' + result + ' ';
    }

    /**
     * Prints the header if show has more than one flag and returns new prefix.
     */
    private static String printHeader(String prefix, EnumSet<FilterOption> show, String header) {
        if (show.size() > 1) {
            System.out.println(prefix + header);
        }
        return "\n\n";
    }

    private static Set<String> keysWithout(Map<String, BuiltinInfo> map, Set<String> both) {
        Set<String> mapOnly = new TreeSet<>(map.keySet());
        mapOnly.removeAll(both);
        return mapOnly;
    }

    private static Map<String, BuiltinInfo> extractFastRBuiltins() {
        BasePackage base = new BasePackage();
        Map<String, BuiltinInfo> result = new TreeMap<>();
        for (Map.Entry<String, RBuiltinFactory> builtin : base.getBuiltins().entrySet()) {
            Class<?> clazz = builtin.getValue().getBuiltinNodeClass();
            RBuiltin annotation = clazz.getAnnotation(RBuiltin.class);
            result.put(builtin.getKey(), new BuiltinInfo(
                            builtin.getValue().getVisibility(),
                            annotation.kind() == RBuiltinKind.INTERNAL,
                            annotation.nonEvalArgs().length == 0,
                            getFastRArity(annotation)));
        }

        return result;
    }

    private static Map<String, BuiltinInfo> extractGnuRBuiltins(String suiteDir) throws IOException {
        String content = Files.lines(Paths.get(suiteDir, DEFAULT_NAMESC)).collect(Collectors.joining("\n"));
        Matcher matcher = Pattern.compile(FUNTAB_REGEXP).matcher(content);
        Map<String, BuiltinInfo> result = new TreeMap<>();
        while (matcher.find()) {
            // normalize eval to three digits, e.g. '2' to '002'
            String eval = String.format("%1$3s", matcher.group("eval")).replace(' ', '0');
            result.put(matcher.group("name"), new BuiltinInfo(
                            parseVisibilityFromGnuR(eval.charAt(0)),
                            eval.charAt(1) == '1',
                            eval.charAt(2) == '1',
                            Integer.parseInt(matcher.group("arity"))));
        }

        return result;
    }

    private static int getFastRArity(RBuiltin annotation) {
        if (annotation.parameterNames() == null) {
            System.err.println("Warning: FastR builtin '" + annotation.name() + "' does not set parameters array.");
            return -1;
        }

        if (Arrays.stream(annotation.parameterNames()).anyMatch(s -> "...".equals(s))) {
            return -1;
        }

        return annotation.parameterNames().length;
    }

    private static RVisibility parseVisibilityFromGnuR(char digit) {
        switch (digit) {
            case '0':
                return RVisibility.ON;
            case '1':
                return RVisibility.OFF;
            case '2':
                return RVisibility.CUSTOM;
            default:
                throw new AssertionError("Regex for eval should not return any other digit.");
        }
    }
}
