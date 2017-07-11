/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.launcher.RCmdOptions.RCmdOption.EXPR;
import static com.oracle.truffle.r.launcher.RCmdOptions.RCmdOption.FILE;
import static com.oracle.truffle.r.launcher.RCmdOptions.RCmdOption.INTERACTIVE;
import static com.oracle.truffle.r.launcher.RCmdOptions.RCmdOption.NO_ENVIRON;
import static com.oracle.truffle.r.launcher.RCmdOptions.RCmdOption.NO_INIT_FILE;
import static com.oracle.truffle.r.launcher.RCmdOptions.RCmdOption.NO_READLINE;
import static com.oracle.truffle.r.launcher.RCmdOptions.RCmdOption.NO_RESTORE;
import static com.oracle.truffle.r.launcher.RCmdOptions.RCmdOption.NO_RESTORE_DATA;
import static com.oracle.truffle.r.launcher.RCmdOptions.RCmdOption.NO_SAVE;
import static com.oracle.truffle.r.launcher.RCmdOptions.RCmdOption.NO_SITE_FILE;
import static com.oracle.truffle.r.launcher.RCmdOptions.RCmdOption.QUIET;
import static com.oracle.truffle.r.launcher.RCmdOptions.RCmdOption.RESTORE;
import static com.oracle.truffle.r.launcher.RCmdOptions.RCmdOption.SAVE;
import static com.oracle.truffle.r.launcher.RCmdOptions.RCmdOption.SILENT;
import static com.oracle.truffle.r.launcher.RCmdOptions.RCmdOption.SLAVE;
import static com.oracle.truffle.r.launcher.RCmdOptions.RCmdOption.VANILLA;
import static com.oracle.truffle.r.launcher.RCmdOptions.RCmdOption.VERBOSE;

import java.util.List;

/**
 * Defines startup parameters (that can be customized by embedding apps).
 */
public class RStartParams {

    private final boolean quiet;
    private final boolean slave;

    /**
     * The setting of this value in GNU R is unusual and not simply based on the value of the
     * --interactive option, so we do not check the option in
     * {@link #RStartParams(RCmdOptions, boolean)}, but later in {@code RCommand}.
     */
    private final boolean interactive;
    private final boolean verbose;
    private final boolean loadSiteFile;
    private final boolean loadInitFile;
    private final boolean debugInitFile;
    private final boolean restoreAction;
    private final boolean askForSave;
    private final boolean save;
    private final boolean noRenviron;

    /**
     * This is not a configurable option, but it is set on the command line and needs to be stored
     * somewhere.
     */
    private final boolean noReadline;

    /**
     * Indicates that FastR is running embedded.
     */
    private final boolean embedded;
    private final String fileArgument;

    public RStartParams(RCmdOptions options, boolean embedded) {
        this.embedded = embedded;
        this.verbose = options.getBoolean(VERBOSE);
        this.quiet = options.getBoolean(QUIET) || options.getBoolean(SILENT) || options.getBoolean(SLAVE);
        this.loadSiteFile = options.getBoolean(NO_SITE_FILE);
        this.loadInitFile = !embedded && options.getBoolean(NO_INIT_FILE) && !options.getBoolean(VANILLA);
        this.noRenviron = embedded || options.getBoolean(NO_ENVIRON) || options.getBoolean(VANILLA);
        this.restoreAction = options.getBoolean(RESTORE) && !(options.getBoolean(NO_RESTORE) || options.getBoolean(NO_RESTORE_DATA) || options.getBoolean(VANILLA));
        this.noReadline = options.getBoolean(NO_READLINE);
        this.slave = options.getBoolean(SLAVE);

        /*
         * GnuR behavior differs from the manual entry for {@code interactive} in that {@code
         * --interactive} never applies to {@code -e/-f}, only to console input that has been
         * redirected from a pipe/file etc.
         */
        String file = options.getString(FILE);
        List<String> expressions = options.getStringList(EXPR);
        if (file != null) {
            if (expressions != null) {
                throw RCommand.fatal("cannot use -e with -f or --file");
            }
            this.askForSave = false;
            this.save = false;
            if (file.equals("-")) {
                // means stdin, but still implies NO_SAVE
                file = null;
            } else {
                file = RCommand.unescapeSpace(file);
            }
            this.interactive = false;
        } else if (expressions != null) {
            this.interactive = false;
            this.askForSave = false;
            this.save = false;
        } else {
            this.interactive = options.getBoolean(INTERACTIVE);
            if (slave || options.getBoolean(NO_SAVE) || options.getBoolean(VANILLA)) {
                this.askForSave = false;
                this.save = false;
            } else if (options.getBoolean(SAVE)) {
                this.askForSave = false;
                this.save = true;
            } else {
                this.askForSave = true;
                this.save = false;
            }
        }
        this.fileArgument = file;
        this.debugInitFile = false;
    }

    /**
     * Only upcalled from native code.
     */
    @SuppressWarnings("unused")
    public static void setParams(boolean quietA, boolean slaveA, boolean interactiveA, boolean verboseA, boolean loadSiteFileA,
                    boolean loadInitFileA, boolean debugInitFileA, int restoreActionA, int saveActionA, boolean noRenvironA) {
        assert false : "re-enable setParams";
        // RStartParams params = RContext.getInstance().getStartParams();
        // params.setQuiet(quietA);
        // params.setSlave(slaveA);
        // params.setInteractive(interactiveA);
        // params.setVerbose(verboseA);
        // params.setLoadSiteFile(loadSiteFileA);
        // params.setLoadInitFile(loadInitFileA);
        // params.setDebugInitFile(debugInitFileA);
        // params.setSaveAction(SA_TYPE.values()[saveActionA]);
        // params.setRestoreAction(SA_TYPE.values()[restoreActionA]);
        // params.setNoRenviron(noRenvironA);
    }

    public boolean isQuiet() {
        return this.quiet;
    }

    public boolean isSlave() {
        return slave;
    }

    public boolean isInteractive() {
        return this.interactive;
    }

    public boolean isVerbose() {
        return this.verbose;
    }

    public boolean getLoadSiteFile() {
        return this.loadSiteFile;
    }

    public boolean loadInitFile() {
        return this.loadInitFile;
    }

    public boolean debugInitFile() {
        return this.debugInitFile;
    }

    public boolean restore() {
        return this.restoreAction;
    }

    public boolean save() {
        return this.save;
    }

    public boolean askForSave() {
        return this.askForSave;
    }

    public boolean noRenviron() {
        return this.noRenviron;
    }

    public boolean noReadline() {
        return this.noReadline;
    }

    public boolean isEmbedded() {
        return embedded;
    }

    public String getFileArgument() {
        return fileArgument;
    }
}
