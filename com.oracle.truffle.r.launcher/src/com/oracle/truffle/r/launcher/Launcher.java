/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.launcher;

import static java.lang.Integer.max;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.graalvm.options.OptionCategory;
import org.graalvm.options.OptionDescriptor;
import org.graalvm.options.OptionDescriptors;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Instrument;
import org.graalvm.polyglot.Language;

public abstract class Launcher {
    private static final boolean STATIC_VERBOSE = Boolean.getBoolean("org.graalvm.launcher.verbose");

    private Engine tempEngine;

    private final boolean verbose;

    private boolean help;
    private boolean helpDebug;
    private boolean helpExpert;
    private boolean helpTools;
    private boolean helpLanguages;

    private OptionCategory helpCategory;
    private VersionAction versionAction = VersionAction.None;

    protected enum VersionAction {
        None,
        PrintAndExit,
        PrintAndContinue
    }

    public Launcher() {
        verbose = STATIC_VERBOSE || Boolean.valueOf(System.getenv("VERBOSE_GRAALVM_LAUNCHERS"));
    }

    private Engine getTempEngine() {
        if (tempEngine == null) {
            tempEngine = Engine.create();
        }
        return tempEngine;
    }

    void setHelpCategory(OptionCategory helpCategory) {
        this.helpCategory = helpCategory;
    }

    protected void setVersionAction(VersionAction versionAction) {
        this.versionAction = versionAction;
    }

    private static class AbortException extends RuntimeException {
        static final long serialVersionUID = 4681646279864737876L;

        AbortException(String message) {
            super(message, null);
        }

        @SuppressWarnings("sync-override")
        @Override
        public final Throwable fillInStackTrace() {
            return null;
        }
    }

    protected AbortException exit() {
        return abort(null, 0);
    }

    protected AbortException abort(String message) {
        return abort(message, 1);
    }

    protected AbortException abort(String message, int exitCode) {
        if (message != null) {
            System.err.println("ERROR: " + message);
        }
        System.exit(exitCode);
        throw new AbortException(message);
    }

    protected abstract void printHelp(OptionCategory maxCategory);

    protected abstract void printVersion();

    protected abstract void collectArguments(Set<String> options);

    protected static void printPolyglotVersions() {
        Engine engine = Engine.create();
        System.out.println("GraalVM Polyglot Engine Version " + engine.getVersion());
        printLanguages(engine, true);
        printInstruments(engine, true);
    }

    protected boolean isVerbose() {
        return verbose;
    }

    final boolean runPolyglotAction() {
        OptionCategory maxCategory = helpDebug ? OptionCategory.DEBUG : (helpExpert ? OptionCategory.EXPERT : (helpCategory != null ? helpCategory : OptionCategory.USER));

        switch (versionAction) {
            case PrintAndContinue:
                printVersion();
                return false;
            case PrintAndExit:
                printVersion();
                return true;
            case None:
                break;
        }
        boolean printDefaultHelp = helpCategory != null || help || ((helpExpert || helpDebug) && !helpTools && !helpLanguages);
        Engine tmpEngine = null;
        if (printDefaultHelp) {
            printHelp(maxCategory);
            // @formatter:off
            System.out.println();
            System.out.println("Runtime Options:");
            printOption("--polyglot",                   "Run with all other guest languages accessible.");
            printOption("--native",                     "Run using the native launcher with limited Java access (default).");
            printOption("--native.[option]",            "Pass options to the native image; for example, '--native.Xmx1G'. To see available options, use '--native.help'.");
            printOption("--jvm",                        "Run on the Java Virtual Machine with Java access.");
            printOption("--jvm.[option]",               "Pass options to the JVM; for example, '--jvm.classpath=myapp.jar'. To see available options. use '--jvm.help'.");
            printOption("--help",                       "Print this help message.");
            printOption("--help:languages",             "Print options for all installed languages.");
            printOption("--help:tools",                 "Print options for all installed tools.");
            printOption("--help:expert",                "Print additional engine options for experts.");
            if (helpExpert || helpDebug) {
                printOption("--help:debug",             "Print additional engine options for debugging.");
            }
            printOption("--version",                    "Print version information and exit.");
            printOption("--show-version",               "Print version information and continue execution.");
            // @formatter:on
            if (tmpEngine == null) {
                tmpEngine = Engine.create();
            }
            List<PrintableOption> engineOptions = new ArrayList<>();
            for (OptionDescriptor descriptor : tmpEngine.getOptions()) {
                if (!descriptor.getName().startsWith("engine.") && !descriptor.getName().startsWith("compiler.")) {
                    continue;
                }
                if (descriptor.getCategory().ordinal() <= maxCategory.ordinal()) {
                    engineOptions.add(asPrintableOption(descriptor));
                }
            }
            if (!engineOptions.isEmpty()) {
                printOptions(engineOptions, "Engine options:", 2);
            }
        }

        if (helpLanguages) {
            if (tmpEngine == null) {
                tmpEngine = Engine.create();
            }
            printLanguageOptions(tmpEngine, maxCategory);
        }

        if (helpTools) {
            if (tmpEngine == null) {
                tmpEngine = Engine.create();
            }
            printInstrumentOptions(tmpEngine, maxCategory);
        }

        if (printDefaultHelp || helpLanguages || helpTools) {
            System.out.println("\nSee http://www.oracle.com/technetwork/oracle-labs/program-languages/overview/index.html for more information.");
            return true;
        }

        return false;
    }

    private static void printInstrumentOptions(Engine engine, OptionCategory maxCategory) {
        Map<Instrument, List<PrintableOption>> instrumentsOptions = new HashMap<>();
        List<Instrument> instruments = sortedInstruments(engine);
        for (Instrument instrument : instruments) {
            List<PrintableOption> options = new ArrayList<>();
            for (OptionDescriptor descriptor : instrument.getOptions()) {
                if (descriptor.getCategory().ordinal() <= maxCategory.ordinal()) {
                    options.add(asPrintableOption(descriptor));
                }
            }
            if (!options.isEmpty()) {
                instrumentsOptions.put(instrument, options);
            }
        }
        if (!instrumentsOptions.isEmpty()) {
            System.out.println();
            System.out.println("Tool options:");
            for (Instrument instrument : instruments) {
                List<PrintableOption> options = instrumentsOptions.get(instrument);
                if (options != null) {
                    printOptions(options, "  " + instrument.getName() + ":", 4);
                }
            }
        }
    }

    private static void printLanguageOptions(Engine engine, OptionCategory maxCategory) {
        Map<Language, List<PrintableOption>> languagesOptions = new HashMap<>();
        List<Language> languages = sortedLanguages(engine);
        for (Language language : languages) {
            List<PrintableOption> options = new ArrayList<>();
            for (OptionDescriptor descriptor : language.getOptions()) {
                if (descriptor.getCategory().ordinal() <= maxCategory.ordinal()) {
                    options.add(asPrintableOption(descriptor));
                }
            }
            if (!options.isEmpty()) {
                languagesOptions.put(language, options);
            }
        }
        if (!languagesOptions.isEmpty()) {
            System.out.println();
            System.out.println("Language Options:");
            for (Language language : languages) {
                List<PrintableOption> options = languagesOptions.get(language);
                if (options != null) {
                    printOptions(options, "  " + language.getName() + ":", 4);
                }
            }
        }
    }

    protected boolean parsePolyglotOption(String defaultOptionPrefix, Map<String, String> options, String arg) {
        switch (arg) {
            case "--help":
                help = true;
                return true;
            case "--help:debug":
                helpDebug = true;
                return true;
            case "--help:expert":
                helpExpert = true;
                return true;
            case "--help:tools":
                helpTools = true;
                return true;
            case "--help:languages":
                helpLanguages = true;
                return true;
            case "--version":
                versionAction = VersionAction.PrintAndExit;
                return true;
            case "--show-version":
                versionAction = VersionAction.PrintAndContinue;
                return true;
            case "--polyglot":
            case "--jvm":
            case "--native":
                return false;
            default:
                // getLanguageId() or null?
                if (arg.length() <= 2 || !arg.startsWith("--")) {
                    return false;
                }
                int eqIdx = arg.indexOf('=');
                String key;
                String value;
                if (eqIdx < 0) {
                    key = arg.substring(2);
                    value = null;
                } else {
                    key = arg.substring(2, eqIdx);
                    value = arg.substring(eqIdx + 1);
                }

                if (value == null) {
                    value = "true";
                }
                int index = key.indexOf('.');
                String group = key;
                if (index >= 0) {
                    group = group.substring(0, index);
                }
                OptionDescriptor descriptor = findPolyglotOptionDescriptor(group, key);
                if (descriptor == null) {
                    if (defaultOptionPrefix != null) {
                        descriptor = findPolyglotOptionDescriptor(defaultOptionPrefix, defaultOptionPrefix + "." + key);
                    }
                    if (descriptor == null) {
                        return false;
                    }
                }
                try {
                    descriptor.getKey().getType().convert(value);
                } catch (IllegalArgumentException e) {
                    throw abort(String.format("Invalid argument %s specified. %s'", arg, e.getMessage()));
                }
                options.put(key, value);
                return true;
        }
    }

    private OptionDescriptor findPolyglotOptionDescriptor(String group, String key) {
        OptionDescriptors descriptors = null;
        switch (group) {
            case "compiler":
            case "engine":
                descriptors = getTempEngine().getOptions();
                break;
            default:
                Engine engine = getTempEngine();
                if (engine.getLanguages().containsKey(group)) {
                    descriptors = engine.getLanguages().get(group).getOptions();
                } else if (engine.getInstruments().containsKey(group)) {
                    descriptors = engine.getInstruments().get(group).getOptions();
                }
                break;
        }
        if (descriptors == null) {
            return null;
        }
        return descriptors.get(key);
    }

    protected static List<Language> sortedLanguages(Engine engine) {
        List<Language> languages = new ArrayList<>(engine.getLanguages().values());
        languages.sort(Comparator.comparing(Language::getId));
        return languages;
    }

    protected static List<Instrument> sortedInstruments(Engine engine) {
        List<Instrument> instruments = new ArrayList<>(engine.getInstruments().values());
        instruments.sort(Comparator.comparing(Instrument::getId));
        return instruments;
    }

    protected static void printOption(OptionCategory maxCategory, OptionDescriptor descriptor) {
        if (descriptor.getCategory().ordinal() <= maxCategory.ordinal()) {
            printOption(asPrintableOption(descriptor));
        }
    }

    protected static PrintableOption asPrintableOption(OptionDescriptor descriptor) {
        StringBuilder key = new StringBuilder("--");
        key.append(descriptor.getName());
        Object defaultValue = descriptor.getKey().getDefaultValue();
        if (defaultValue instanceof Boolean && defaultValue == Boolean.FALSE) {
            // nothing to print
        } else {
            key.append("=<");
            key.append(descriptor.getKey().getType().getName());
            key.append(">");
        }
        return new PrintableOption(key.toString(), descriptor.getHelp());
    }

    protected static void printOption(String option, String description) {
        printOption(option, description, 2);
    }

    protected static void printOption(String option, String description, int indentation) {
        StringBuilder indent = new StringBuilder(indentation);
        for (int i = 0; i < indentation; i++) {
            indent.append(' ');
        }
        String desc = description != null ? description : "";
        if (option.length() >= 45 && description != null) {
            System.out.println(String.format("%s%s%n%s%-45s%s", indent, option, indent, "", desc));
        } else {
            System.out.println(String.format("%s%-45s%s", indent, option, desc));
        }
    }

    protected static void printOption(PrintableOption option) {
        printOption(option, 2);
    }

    protected static void printOption(PrintableOption option, int indentation) {
        printOption(option.option, option.description, indentation);
    }

    private static final class PrintableOption implements Comparable<PrintableOption> {
        final String option;
        final String description;

        private PrintableOption(String option, String description) {
            this.option = option;
            this.description = description;
        }

        @Override
        public int compareTo(PrintableOption o) {
            return this.option.compareTo(o.option);
        }
    }

    private static void printOptions(List<PrintableOption> options, String title, int indentation) {
        Collections.sort(options);
        System.out.println(title);
        for (PrintableOption option : options) {
            printOption(option, indentation);
        }
    }

    protected enum OS {
        Darwin,
        Linux,
        Solaris;

        private static OS findCurrent() {
            final String name = System.getProperty("os.name");
            if (name.equals("Linux")) {
                return Linux;
            }
            if (name.equals("SunOS")) {
                return Solaris;
            }
            if (name.equals("Mac OS X") || name.equals("Darwin")) {
                return Darwin;
            }
            throw new IllegalArgumentException("unknown OS: " + name);
        }

        private static final OS current = findCurrent();

        public static OS getCurrent() {
            return current;
        }
    }

    protected static void printLanguages(Engine engine, boolean printWhenEmpty) {
        if (engine.getLanguages().isEmpty()) {
            if (printWhenEmpty) {
                System.out.println("  Installed Languages: none");
            }
        } else {
            System.out.println("  Installed Languages:");
            List<Language> languages = new ArrayList<>(engine.getLanguages().size());
            int nameLength = 0;
            for (Language language : engine.getLanguages().values()) {
                languages.add(language);
                nameLength = max(nameLength, language.getName().length());
            }
            languages.sort(Comparator.comparing(Language::getId));
            String langFormat = "    %-" + nameLength + "s%s version %s%n";
            for (Language language : languages) {
                String version = language.getVersion();
                if (version == null || version.length() == 0) {
                    version = "";
                }
                System.out.printf(langFormat, language.getName().isEmpty() ? "Unnamed" : language.getName(), version);
            }
        }
    }

    protected static void printInstruments(Engine engine, boolean printWhenEmpty) {
        if (engine.getInstruments().isEmpty()) {
            if (printWhenEmpty) {
                System.out.println("  Installed Tools: none");
            }
        } else {
            System.out.println("  Installed Tools:");
            List<Instrument> instruments = sortedInstruments(engine);
            int nameLength = 0;
            for (Instrument instrument : instruments) {
                nameLength = max(nameLength, instrument.getName().length());
            }
            String instrumentFormat = "    %-" + nameLength + "s version %s%n";
            for (Instrument instrument : instruments) {
                String version = instrument.getVersion();
                if (version == null || version.length() == 0) {
                    version = "";
                }
                System.out.printf(instrumentFormat, instrument.getName().isEmpty() ? "Unnamed" : instrument.getName(), version);
            }
        }
    }
}
