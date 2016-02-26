/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.runtime.RCmdOptions.RCmdOption.*;

import java.util.*;

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
        },

        RSCRIPT {
            @Override
            public String usage() {
                return "\nUsage: [--options] [-e expr [-e expr2 ...] | file] [args]\n";
            }

        },

        EITHER {
            @Override
            public String usage() {
                throw Utils.fail("can't call usage() on Client.EITHER");
            }
        };

        public abstract String usage();
    }

    public enum RCmdOptionType {
        BOOLEAN,
        STRING,
        REPEATED_STRING
    }

    public enum RCmdOption {
        HELP(RCmdOptionType.BOOLEAN, true, "h", "help", false, "Print short help message and exit"),
        VERSION(RCmdOptionType.BOOLEAN, true, "version", false, "Print version info and exit"),
        ENCODING(RCmdOptionType.STRING, false, "encoding=ENC", null, "Specify encoding to be used for stdin"),
        SAVE(RCmdOptionType.BOOLEAN, true, "save", false, "Do save workspace at the end of the session"),
        NO_SAVE(RCmdOptionType.BOOLEAN, true, "no-save", false, "Don't save it"),
        NO_ENVIRON(RCmdOptionType.BOOLEAN, false, "no-environ", false, "Don't read the site and user environment files"),
        NO_SITE_FILE(RCmdOptionType.BOOLEAN, false, "no-site-file", false, "Don't read the site-wide Rprofile"),
        NO_INIT_FILE(RCmdOptionType.BOOLEAN, false, "no-init-file", false, "Don't read the user R profile"),
        RESTORE(RCmdOptionType.BOOLEAN, true, "restore", true, "Do restore previously saved objects at startup"),
        NO_RESTORE_DATA(RCmdOptionType.BOOLEAN, true, "no-restore-data", false, "Don't restore previously saved objects"),
        NO_RESTORE_HISTORY(RCmdOptionType.BOOLEAN, false, "no-restore-history", false, "Don't restore the R history file"),
        NO_RESTORE(RCmdOptionType.BOOLEAN, true, "no-restore", false, "Don't restore anything"),
        VANILLA(RCmdOptionType.BOOLEAN, true, "vanilla", false, "Combine --no-save, --no-restore, --no-site-file,\n--no-init-file and --no-environ"),
        NO_READLINE(RCmdOptionType.BOOLEAN, true, "no-readline", false, "Don't use readline for command-line editing"),
        MAX_PPSIZE(RCmdOptionType.STRING, false, "max-ppsize", null, "Set max size of protect stack to N"),
        QUIET(RCmdOptionType.BOOLEAN, true, "q", "quiet", false, "Don't print startup message"),
        SILENT(RCmdOptionType.BOOLEAN, true, "silent", false, "Same as --quiet"),
        SLAVE(RCmdOptionType.BOOLEAN, true, "slave", false, "Make R run as quietly as possible"),
        INTERACTIVE(RCmdOptionType.BOOLEAN, false, "interactive", false, "Force an interactive session"),
        VERBOSE(RCmdOptionType.BOOLEAN, false, "verbose", false, "Print more information about progress"),
        DEBUGGER(RCmdOptionType.STRING, true, "d", "debugger=NAME", null, "Run R through debugger NAME"),
        DEBUGGER_ARGS(RCmdOptionType.STRING, false, "debugger-args=ARGS", null, "Pass ARGS as arguments to the debugger"),
        GUI(RCmdOptionType.STRING, false, "g TYPE", "gui=TYPE", null, "Use TYPE as GUI; possible values are 'X11' (default)\nand 'Tk'."),
        ARCH(RCmdOptionType.STRING, false, "arch=NAME", null, "Specify a sub-architecture"),
        ARGS(RCmdOptionType.BOOLEAN, true, "args", false, "Skip the rest of the command line"),
        FILE(RCmdOptionType.STRING, true, "f FILE", "file=FILE", null, "Take input from 'FILE'"),
        EXPR(RCmdOptionType.REPEATED_STRING, true, "e EXPR", null, null, "Execute 'EXPR' and exit"),
        DEFAULT_PACKAGES(RCmdOptionType.STRING, Client.RSCRIPT, false, null, "default-packages=list", null, "Where 'list' is a comma-separated set\nof package names, or 'NULL'");

        private final RCmdOptionType type;
        @SuppressWarnings("unused") private final Client client;
        // Whether this option is actually implemented in FastR
        private final boolean implemented;
        // The short option name prefixed by {@code -} or {@code null} if no {@code -} form.
        private final String shortName;
        // The option name prefixed by {@code --} or {@code null} if no {@code --} form.
        private final String name;
        // The plain option name as passed to the constructor.
        public final String plainName;
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
            this.help = help.replace("\n", "\n                          ");
        }

        RCmdOption(RCmdOptionType type, boolean implemented, String shortName, String name, Object defaultValue, String help) {
            this(type, Client.EITHER, implemented, shortName, name, defaultValue, help);
        }

        RCmdOption(RCmdOptionType type, boolean implemented, String name, Object defaultValue, String help) {
            this(type, Client.EITHER, implemented, null, name, defaultValue, help);
        }

        private boolean matches(String arg) {
            if (shortName != null && arg.equals(shortName)) {
                return true;
            }
            if (name != null && arg.equals(name)) {
                return true;
            }
            return false;
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

    private final EnumMap<RCmdOption, Object> optionValues;
    /**
     * The original {@code args} array, with element zero set to "FastR".
     */
    private String[] arguments;
    /**
     * Index in {@code args} of the first non-option argument or {@code args.length} if none.
     */
    private final int firstNonOptionArgIndex;

    private RCmdOptions(EnumMap<RCmdOption, Object> optionValues, String[] args, int firstNonOptionArgIndex) {
        this.optionValues = optionValues;
        this.arguments = args;
        this.firstNonOptionArgIndex = firstNonOptionArgIndex;
    }

    private static void setValue(EnumMap<RCmdOption, Object> optionValues, RCmdOption option, boolean value) {
        assert option.type == RCmdOptionType.BOOLEAN;
        optionValues.put(option, value);
    }

    private static void setValue(EnumMap<RCmdOption, Object> optionValues, RCmdOption option, String value) {
        if (option.type == RCmdOptionType.REPEATED_STRING) {
            @SuppressWarnings("unchecked")
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

    private static final class MatchResult {
        private final RCmdOption option;
        private final boolean matchedShort;

        MatchResult(RCmdOption option, boolean matchedShort) {
            this.option = option;
            this.matchedShort = matchedShort;
        }
    }

    private static MatchResult matchOption(String arg) {
        for (RCmdOption option : RCmdOption.values()) {
            if (option.type == RCmdOptionType.BOOLEAN) {
                // these must match exactly
                if (option.matches(arg)) {
                    return new MatchResult(option, false);
                }
            } else if (option.type == RCmdOptionType.STRING || option.type == RCmdOptionType.REPEATED_STRING) {
                // short forms must match exactly (and consume next argument)
                if (option.shortName != null && option.shortName.equals(arg)) {
                    return new MatchResult(option, true);
                } else if (arg.indexOf('=') > 0 && option.name != null && arg.startsWith(option.name)) {
                    return new MatchResult(option, false);
                }
            }
        }
        return null;
    }

    /**
     * Parse the arguments from the standard R/Rscript command line syntax, setting the
     * corresponding values.
     *
     * R supports {@code --arg=value} or {@code -arg value} for string-valued options.
     *
     * The spec for {@code commandArgs()} states that it returns the executable by which R was
     * invoked in element 0, which is consistent with the C {@code main} function, but defines the
     * exact form to be platform independent. Java does not provide the executable (for obvious
     * reasons) so we use "FastR".
     */
    public static RCmdOptions parseArguments(Client client, String[] args) {
        EnumMap<RCmdOption, Object> options = new EnumMap<>(RCmdOption.class);
        int i = 0;
        int firstNonOptionArgIndex = args.length;
        while (i < args.length) {
            final String arg = args[i];
            MatchResult result = matchOption(arg);
            if (result == null) {
                // for Rscript, this means we are done
                if (client == Client.RSCRIPT) {
                    firstNonOptionArgIndex = i;
                    break;
                }
                // GnuR does not abort, simply issues a warning
                System.out.printf("WARNING: unknown option '%s'%n", arg);
                i++;
                continue;
            } else {
                RCmdOption option = result.option;
                if (result.matchedShort && i == args.length - 1) {
                    System.out.println("usage:");
                    printHelp(client);
                    System.exit(1);
                }
                // check implemented
                if (!option.implemented) {
                    System.out.println("WARNING: option: " + arg + " is not implemented");
                }
                if (result.matchedShort) {
                    i++;
                    setValue(options, option, args[i]);
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
        String[] xargs = new String[args.length + 1];
        xargs[0] = "FastR";
        System.arraycopy(args, 0, xargs, 1, args.length);

        // adjust for inserted executable name
        return new RCmdOptions(options, xargs, firstNonOptionArgIndex + 1);
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

    public void printHelpAndVersion() {
        if (getBoolean(HELP)) {
            printHelpAndExit(RCmdOptions.Client.R);
        } else if (getBoolean(VERSION)) {
            printVersionAndExit();
        }
    }

    public static void printHelp(Client client) {
        System.out.println(client.usage());
        System.out.println("Options:");
        for (RCmdOption option : RCmdOption.values()) {
            System.out.printf("  %-22s  %s%n", option.getHelpName(), option.help);
        }
        System.out.println("\nFILE may contain spaces but not shell metacharacters.\n");
    }

    public static void printHelpAndExit(Client client) {
        printHelp(client);
        System.exit(0);
    }

    private static void printVersionAndExit() {
        System.out.print("FastR version ");
        System.out.println(RVersionNumber.FULL);
        System.out.println(RRuntime.LICENSE);
        System.exit(0);
    }

}
