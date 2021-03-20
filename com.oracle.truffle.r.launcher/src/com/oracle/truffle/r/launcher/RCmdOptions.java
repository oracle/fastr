/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.launcher;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import com.oracle.truffle.r.launcher.RMain.PrintHelp;
import com.oracle.truffle.r.launcher.RMain.PrintVersion;

/**
 * (Abstract) definition of the standard R command line options. The setting of the values from the
 * environment is handled in some other class.
 */
public final class RCmdOptions {
    public enum Client {
        R {
            @Override
            public String usage() {
                return "\nUsage: R [options] [< infile] [> outfile]\n" + "   or: R CMD command [arguments]\n\n" + "Start R, a system for statistical computation and graphics, with the\n" +
                                "specified options, or invoke an R tool via the 'R CMD' interface.\n";
            }

            @Override
            public String argumentName() {
                return "R";
            }

            @Override
            public String[] processOptions(RCmdOptions options) {
                return preprocessROptions(options);
            }

            @Override
            public String getHelpMessage() {
                return "R version " + RVersionNumber.FULL + " (FastR)\n" +
                                RVersionNumber.COPYRIGHT +
                                "\n" +
                                "FastR is free software and comes with ABSOLUTELY NO WARRANTY.\n" +
                                "You are welcome to redistribute it under certain conditions.\n";
                // note: extra new-line at the end matches GNU-R's formatting
            }
        },

        RSCRIPT {
            @Override
            public String usage() {
                return "\nUsage: [--options] [-e expr [-e expr2 ...] | file] [args]\n";
            }

            @Override
            public String argumentName() {
                return "Rscript";
            }

            @Override
            public String[] processOptions(RCmdOptions options) {
                return preprocessRScriptOptions(options);
            }

            @Override
            public String getHelpMessage() {
                return String.format("R scripting front-end version %s%s", RVersionNumber.FULL, RVersionNumber.RELEASE_DATE);
            }
        },

        EITHER {
            @Override
            public String usage() {
                throw RMain.fatal("can't call usage() on Client.EITHER");
            }

            @Override
            public String argumentName() {
                return "either";
            }

            @Override
            public String[] processOptions(RCmdOptions options) {
                return options.getArguments();
            }

            @Override
            public String getHelpMessage() {
                return R.getHelpMessage();
            }
        };

        public abstract String usage();

        public abstract String argumentName();

        public abstract String[] processOptions(RCmdOptions options);

        public abstract String getHelpMessage();
    }

    private enum RCmdOptionType {
        BOOLEAN,
        STRING,
        REPEATED_STRING
    }

    public enum RCmdOption {
        VERSION(RCmdOptionType.BOOLEAN, true, "version", false, "Print version info and exit"),
        ENCODING(RCmdOptionType.STRING, false, "encoding=ENC", null, "Specify encoding to be used for stdin"),
        SAVE(RCmdOptionType.BOOLEAN, true, "save", false, "Do save workspace at the end of the session"),
        NO_SAVE(RCmdOptionType.BOOLEAN, true, "no-save", false, "Don't save it"),
        NO_ENVIRON(RCmdOptionType.BOOLEAN, true, "no-environ", false, "Don't read the site and user environment files"),
        NO_SITE_FILE(RCmdOptionType.BOOLEAN, true, "no-site-file", false, "Don't read the site-wide Rprofile"),
        NO_INIT_FILE(RCmdOptionType.BOOLEAN, true, "no-init-file", false, "Don't read the user R profile"),
        RESTORE(RCmdOptionType.BOOLEAN, true, "restore", true, "Do restore previously saved objects at startup"),
        NO_RESTORE_DATA(RCmdOptionType.BOOLEAN, true, "no-restore-data", false, "Don't restore previously saved objects"),
        NO_RESTORE_HISTORY(RCmdOptionType.BOOLEAN, false, "no-restore-history", false, "Don't restore the R history file"),
        NO_RESTORE(RCmdOptionType.BOOLEAN, true, "no-restore", false, "Don't restore anything"),
        VANILLA(RCmdOptionType.BOOLEAN, true, "vanilla", false, "Combine --no-save, --no-restore, --no-site-file,\n--no-init-file and --no-environ"),
        NO_READLINE(RCmdOptionType.BOOLEAN, true, "no-readline", false, "Don't use readline for command-line editing"),
        MAX_PPSIZE(RCmdOptionType.STRING, false, "max-ppsize", null, "Set max size of protect stack to N"),
        QUIET(RCmdOptionType.BOOLEAN, true, "q", "quiet", false, "Don't print startup message"),
        SILENT(RCmdOptionType.BOOLEAN, true, "silent", false, "Same as --quiet"),
        NO_ECHO(RCmdOptionType.BOOLEAN, true, "s", "no-echo", false, "Make R run as quietly as possible"),
        INTERACTIVE(RCmdOptionType.BOOLEAN, true, "interactive", false, "Force an interactive session"),
        VERBOSE(RCmdOptionType.BOOLEAN, true, "verbose", false, "Print more information about progress"),
        ARGS(RCmdOptionType.BOOLEAN, true, "args", false, "Skip the rest of the command line"),
        FILE(RCmdOptionType.STRING, true, "f FILE", "file=FILE", null, "Take input from 'FILE'"),
        EXPR(RCmdOptionType.REPEATED_STRING, true, "e EXPR", null, null, "Execute 'EXPR' and exit"),
        DEFAULT_PACKAGES(RCmdOptionType.STRING, Client.RSCRIPT, false, null, "default-packages=list", null, "Where 'list' is a comma-separated set\nof package names, or 'NULL'");

        private final RCmdOptionType type;
        @SuppressWarnings("unused") private final Client client;
        // Whether this option is actually implemented in FastR
        private final boolean implemented;
        // The short option name prefixed by {@code -} or {@code null} if no {@code -} form.
        final String shortName;
        // The option name prefixed by {@code --} or {@code null} if no {@code --} form.
        private final String name;
        // The plain option name as passed to the constructor.
        final String plainName;
        // The '=' separated suffix, e.g. {@code --file=FILE}.
        private final String suffix;
        // The space separated suffix, e.g. {@code -g TYPE}.
        private final String shortSuffix;
        private final Object defaultValue;
        private final String help;

        RCmdOption(RCmdOptionType type, Client client, boolean implemented, String shortName, String name, Object defaultValue, String help) {
            this.type = type;
            this.client = client;
            this.implemented = implemented;
            if (shortName == null) {
                this.shortName = null;
                this.shortSuffix = null;
            } else {
                int spx = shortName.indexOf(' ');
                this.shortName = "-" + (spx > 0 ? shortName.substring(0, spx) : shortName);
                this.shortSuffix = spx > 0 ? shortName.substring(spx) : null;
            }
            this.plainName = name;
            if (name == null) {
                this.name = null;
                this.suffix = null;
            } else {
                int eqx = name.indexOf('=');
                this.name = "--" + (eqx > 0 ? name.substring(0, eqx) : name);
                this.suffix = eqx > 0 ? name.substring(eqx) : null;
            }
            this.defaultValue = defaultValue;
            this.help = help;
        }

        RCmdOption(RCmdOptionType type, boolean implemented, String shortName, String name, Object defaultValue, String help) {
            this(type, Client.EITHER, implemented, shortName, name, defaultValue, help);
        }

        RCmdOption(RCmdOptionType type, boolean implemented, String name, Object defaultValue, String help) {
            this(type, Client.EITHER, implemented, null, name, defaultValue, help);
        }

        private String getHelpName() {
            String result = "";
            if (shortName != null) {
                result = shortName;
                if (shortSuffix != null) {
                    result += shortSuffix;
                }
                if (name != null) {
                    result += ", ";
                }
            }
            if (name != null) {
                result = result + name;
            }
            if (suffix != null) {
                result += suffix;
            }
            return result;
        }
    }

    private final Client client;
    private final EnumMap<RCmdOption, Object> optionValues;
    /**
     * The original {@code args} array, with element zero set to "FastR".
     */
    private String[] arguments;
    /**
     * Index in {@code args} of the first non-option argument or {@code args.length} if none.
     */
    private final int firstNonOptionArgIndex;

    private RCmdOptions(Client client, EnumMap<RCmdOption, Object> optionValues, String[] args, int firstNonOptionArgIndex) {
        this.client = client;
        this.optionValues = optionValues;
        this.arguments = args;
        this.firstNonOptionArgIndex = firstNonOptionArgIndex;
    }

    private static void setValue(EnumMap<RCmdOption, Object> optionValues, RCmdOption option, boolean value) {
        assert option.type == RCmdOptionType.BOOLEAN;
        optionValues.put(option, value);
    }

    @SuppressWarnings("unchecked")
    private static void setValue(EnumMap<RCmdOption, Object> optionValues, RCmdOption option, String value) {
        if (option.type == RCmdOptionType.REPEATED_STRING) {
            ArrayList<String> list = (ArrayList<String>) optionValues.get(option);
            if (list == null) {
                optionValues.put(option, list = new ArrayList<>());
            }
            list.add(value);
        } else {
            assert option.type == RCmdOptionType.STRING;
            optionValues.put(option, value);
        }
    }

    public void addInteractive() {
        setValue(RCmdOption.INTERACTIVE, true);
        if (arguments == null || arguments.length == 0) {
            arguments = new String[]{"--interactive"};
        } else {
            String[] oldArgs = arguments;
            arguments = new String[arguments.length + 1];
            arguments[0] = oldArgs[0];
            arguments[1] = "--interactive";
            for (int i = 1; i < oldArgs.length; i++) {
                arguments[i + 1] = oldArgs[i];
            }
        }
    }

    public void setValue(RCmdOption option, boolean value) {
        setValue(optionValues, option, value);
    }

    public void setValue(RCmdOption option, String value) {
        setValue(optionValues, option, value);
    }

    public boolean getBoolean(RCmdOption option) {
        assert option.type == RCmdOptionType.BOOLEAN;
        Object value = optionValues.get(option);
        return value == null ? (Boolean) option.defaultValue : (Boolean) value;
    }

    public String getString(RCmdOption option) {
        assert option.type == RCmdOptionType.STRING;
        Object value = optionValues.get(option);
        return value == null ? (String) option.defaultValue : (String) value;
    }

    @SuppressWarnings("unchecked")
    public List<String> getStringList(RCmdOption option) {
        assert option.type == RCmdOptionType.REPEATED_STRING;
        Object value = optionValues.get(option);
        return value == null ? (List<String>) option.defaultValue : (List<String>) value;
    }

    public Client getClient() {
        return client;
    }

    private static final class MatchResult {
        private final RCmdOption option;
        private final boolean matchedShort;

        MatchResult(RCmdOption option, boolean matchedShort) {
            this.option = option;
            this.matchedShort = matchedShort;
        }
    }

    private static MatchResult matchOption(String arg) {
        // --slave option is currently an undocumented alias for --no-echo option (GNU-R silently
        // treats --slave as --no-echo, and provides no help for it)
        if (arg.equals("--slave")) {
            return new MatchResult(RCmdOption.NO_ECHO, false);
        }

        for (RCmdOption option : RCmdOption.values()) {
            switch (option.type) {
                case BOOLEAN:
                    // these must match exactly
                    if (option.shortName != null && arg.equals(option.shortName) || option.name != null && arg.equals(option.name)) {
                        return new MatchResult(option, false);
                    }
                    break;
                case STRING:
                case REPEATED_STRING:
                    // short forms must match exactly (and consume next argument)
                    if (option.shortName != null && option.shortName.equals(arg)) {
                        return new MatchResult(option, true);
                    } else if (arg.indexOf('=') > 0 && option.name != null && arg.startsWith(option.name)) {
                        return new MatchResult(option, false);
                    }
                    break;
            }
        }
        return null;
    }

    /**
     * Parse the arguments from the standard R/Rscript command line syntax, setting the
     * corresponding values.
     * <p>
     * R supports {@code --arg=value} or {@code -arg value} for string-valued options.
     * <p>
     * The spec for {@code commandArgs()} states that it returns the executable by which R was
     * invoked in element 0, which is consistent with the C {@code main} function, but defines the
     * exact form to be platform independent. Java does not provide the executable (for obvious
     * reasons) so we use "FastR". However, embedded mode does pass the executable in
     * {@code args[0]} and we do not want to parse that!
     */
    public static RCmdOptions parseArguments(String[] args, boolean reparse) {
        return parseArguments(args, reparse, null);
    }

    public static RCmdOptions parseArguments(String[] args, boolean reparse, boolean[] recognizedArgsIndices) {
        assert recognizedArgsIndices == null || recognizedArgsIndices.length == args.length;

        EnumMap<RCmdOption, Object> options = new EnumMap<>(RCmdOption.class);

        Client client = null;
        int clientIdx = -1;
        for (int i = 0; i < args.length; i++) {
            final String arg = args[i];
            if (!arg.startsWith("-") && !arg.startsWith("--")) {
                switch (arg) {
                    case "R":
                        client = Client.R;
                        break;
                    case "Rscript":
                        client = Client.RSCRIPT;
                        break;
                }
                clientIdx = i;
                if (recognizedArgsIndices != null) {
                    recognizedArgsIndices[i] = true;
                }
                break;
            }
        }

        int firstNonOptionArgIndex = args.length;
        int i = 0;
        while (i < args.length) {
            if (i == clientIdx) {
                i++;
                continue;
            }
            final String arg = args[i];
            MatchResult result = matchOption(arg);
            if (result == null) {
                boolean isOption = arg.startsWith("--") || arg.startsWith("-");
                if (!isOption && client == Client.RSCRIPT) {
                    // for Rscript, this means we are done
                    if (recognizedArgsIndices != null) {
                        recognizedArgsIndices[i] = true;
                    }
                    firstNonOptionArgIndex = i;
                    break;
                }
                if (!reparse) {
                    // GnuR does not abort, simply issues a warning
                    System.out.printf("WARNING: unknown option '%s'%n", arg);
                    if (recognizedArgsIndices != null) {
                        recognizedArgsIndices[i] = true;
                    }
                }
                i++;
                continue;
            } else {
                if (recognizedArgsIndices != null) {
                    recognizedArgsIndices[i] = true;
                }
                RCmdOption option = result.option;
                if (result.matchedShort && i == args.length - 1) {
                    System.out.println("usage:");
                    printHelp(client);
                    System.exit(1);
                }
                // check implemented
                if (!option.implemented) {
                    if (!reparse) {
                        System.out.println("WARNING: option: " + arg + " is not implemented");
                    }
                }
                if (result.matchedShort) {
                    i++;
                    setValue(options, option, args[i]);
                    if (recognizedArgsIndices != null) {
                        recognizedArgsIndices[i] = true;
                    }
                } else {
                    if (option.type == RCmdOptionType.BOOLEAN) {
                        setValue(options, option, true);
                    } else if (option.type == RCmdOptionType.STRING) {
                        int eqx = arg.indexOf('=');
                        setValue(options, option, arg.substring(eqx + 1));
                    }
                }
                i++;
                // check for --args, in which case stop parsing
                if (option == RCmdOption.ARGS) {
                    firstNonOptionArgIndex = i;
                    break;
                }
            }
        }

        if (recognizedArgsIndices != null) {
            // mark the all non-option arguments (the tail) as recognized
            for (int j = firstNonOptionArgIndex; j < recognizedArgsIndices.length; j++) {
                recognizedArgsIndices[j] = true;
            }
        }

        // adjust for inserted executable name
        return new RCmdOptions(client, options, args, firstNonOptionArgIndex);

    }

    public String[] getArguments() {
        return arguments;
    }

    public int getFirstNonOptionArgIndex() {
        return firstNonOptionArgIndex;
    }

    public void setArguments(String[] arguments) {
        this.arguments = arguments;
    }

    static void printHelp(Client client) {
        System.out.println(client.usage());
        System.out.println("Options:");
        for (RCmdOption option : RCmdOption.values()) {
            System.out.printf("  %-22s  %s%n", option.getHelpName(), option.help.replace("\n", "\n                          "));
        }
        System.out.println("\nFILE may contain spaces but not shell metacharacters.\n");
    }

    private static String[] preprocessROptions(RCmdOptions options) {
        if (options.getBoolean(RCmdOption.VERSION)) {
            throw new PrintVersion();
        }
        return options.getArguments();
    }

    // CheckStyle: stop system..print check

    private static String[] preprocessRScriptOptions(RCmdOptions options) {
        if (options.getBoolean(RCmdOption.VERSION)) {
            throw new PrintVersion();
        }

        String[] arguments = options.getArguments();
        int resultArgsLength = arguments.length;
        int firstNonOptionArgIndex = options.getFirstNonOptionArgIndex();
        // Now reformat the args, setting --no-echo and --no-restore as per the spec
        ArrayList<String> adjArgs = new ArrayList<>(resultArgsLength + 1);
        adjArgs.add(arguments[0]);
        adjArgs.add("--no-echo");
        options.setValue(RCmdOption.NO_ECHO, true);
        adjArgs.add("--no-restore");
        options.setValue(RCmdOption.NO_RESTORE, true);
        // Either -e options are set or first non-option arg is a file
        if (options.getStringList(RCmdOption.EXPR) == null) {
            if (firstNonOptionArgIndex == resultArgsLength) {
                throw new PrintHelp();
            } else {
                options.setValue(RCmdOption.FILE, arguments[firstNonOptionArgIndex]);
            }
        }
        String defaultPackagesArg = options.getString(RCmdOption.DEFAULT_PACKAGES);
        String defaultPackagesEnv = System.getenv("R_DEFAULT_PACKAGES");
        if (defaultPackagesArg == null && defaultPackagesEnv == null) {
            defaultPackagesArg = "datasets,utils,grDevices,graphics,stats";
        }
        if (defaultPackagesEnv == null) {
            options.setValue(RCmdOption.DEFAULT_PACKAGES, defaultPackagesArg);
        }
        // copy up to non-option args
        int rx = 1;
        while (rx < firstNonOptionArgIndex) {
            adjArgs.add(arguments[rx]);
            rx++;
        }
        if (options.getString(RCmdOption.FILE) != null) {
            adjArgs.add("--file=" + options.getString(RCmdOption.FILE));
            rx++; // skip over file arg
            firstNonOptionArgIndex++;
        }

        if (firstNonOptionArgIndex < resultArgsLength) {
            adjArgs.add("--args");
            while (rx < resultArgsLength) {
                adjArgs.add(arguments[rx++]);
            }
        }
        return adjArgs.toArray(new String[adjArgs.size()]);
    }
}
