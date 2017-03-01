/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2015, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.runtime;

import static com.oracle.truffle.r.runtime.RCmdOptions.RCmdOption.NO_ENVIRON;
import static com.oracle.truffle.r.runtime.RCmdOptions.RCmdOption.NO_INIT_FILE;
import static com.oracle.truffle.r.runtime.RCmdOptions.RCmdOption.NO_READLINE;
import static com.oracle.truffle.r.runtime.RCmdOptions.RCmdOption.NO_RESTORE;
import static com.oracle.truffle.r.runtime.RCmdOptions.RCmdOption.NO_RESTORE_DATA;
import static com.oracle.truffle.r.runtime.RCmdOptions.RCmdOption.NO_SAVE;
import static com.oracle.truffle.r.runtime.RCmdOptions.RCmdOption.NO_SITE_FILE;
import static com.oracle.truffle.r.runtime.RCmdOptions.RCmdOption.QUIET;
import static com.oracle.truffle.r.runtime.RCmdOptions.RCmdOption.RESTORE;
import static com.oracle.truffle.r.runtime.RCmdOptions.RCmdOption.SAVE;
import static com.oracle.truffle.r.runtime.RCmdOptions.RCmdOption.SILENT;
import static com.oracle.truffle.r.runtime.RCmdOptions.RCmdOption.SLAVE;
import static com.oracle.truffle.r.runtime.RCmdOptions.RCmdOption.VANILLA;
import static com.oracle.truffle.r.runtime.RCmdOptions.RCmdOption.VERBOSE;

import com.oracle.truffle.r.runtime.context.RContext;

/**
 * Defines startup parameters (that can be customized by embedding apps).
 */
public class RStartParams {

    public enum SA_TYPE {
        NORESTORE(null),
        RESTORE(null),
        DEFAULT("default"),
        NOSAVE("no"),
        SAVE("yes"),
        SAVEASK("ask"),
        SUICIDE(null);

        private String userName;

        SA_TYPE(String userName) {
            this.userName = userName;
        }

        public static final String[] SAVE_VALUES = new String[]{"yes", "no", "ask", "default"};

        public String getUserName() {
            return userName;
        }

        public static SA_TYPE fromString(String s) {
            for (SA_TYPE t : values()) {
                if (t.userName != null && t.userName.equals(s)) {
                    return t;
                }
            }
            return null;
        }
    }

    private boolean quiet;
    private boolean slave;
    /**
     * The setting of this value in GNU R is unusual and not simply based on the value of the
     * --interactive option, so we do not check the option in
     * {@link #RStartParams(RCmdOptions, boolean)}, but later in {@code RCommand}.
     */
    private boolean interactive = true;
    private boolean verbose;
    private boolean loadSiteFile = true;
    private boolean loadInitFile = true;
    private boolean debugInitFile;
    private SA_TYPE restoreAction = SA_TYPE.RESTORE;
    private SA_TYPE saveAction = SA_TYPE.SAVEASK;
    private boolean noRenviron;
    /**
     * This is not a configurable option, but it is set on the command line and needs to be stored
     * somewhere.
     */
    private boolean noReadline;

    /**
     * The result from parsing the command line options.
     */
    private final RCmdOptions cmdOptions;

    /**
     * Indicates that FastR is running embedded.
     */
    private boolean embedded;

    public RStartParams(RCmdOptions options, boolean embedded) {
        this.cmdOptions = options;
        this.embedded = embedded;
        if (options.getBoolean(VERBOSE)) {
            this.verbose = true;
        }
        if (options.getBoolean(QUIET) || options.getBoolean(SILENT)) {
            this.quiet = true;
        }
        if (options.getBoolean(NO_SITE_FILE)) {
            this.loadSiteFile = false;
        }
        if (options.getBoolean(NO_INIT_FILE)) {
            this.loadInitFile = false;
        }
        if (options.getBoolean(NO_ENVIRON)) {
            this.noRenviron = true;
        }
        if (options.getBoolean(SAVE)) {
            this.saveAction = SA_TYPE.SAVE;
        }
        if (options.getBoolean(NO_SAVE)) {
            this.saveAction = SA_TYPE.NOSAVE;
        }
        if (options.getBoolean(RESTORE)) {
            this.restoreAction = SA_TYPE.RESTORE;
        }
        if (options.getBoolean(NO_RESTORE) || options.getBoolean(NO_RESTORE_DATA)) {
            this.restoreAction = SA_TYPE.NORESTORE;
        }
        if (options.getBoolean(NO_READLINE)) {
            this.noReadline = true;
        }
        if (options.getBoolean(SLAVE)) {
            this.slave = true;
            this.quiet = true;
            this.setSaveAction(SA_TYPE.NOSAVE);
        }

        if (options.getBoolean(VANILLA)) {
            this.setSaveAction(SA_TYPE.NOSAVE);
            this.noRenviron = true;
            this.loadInitFile = false;
            this.restoreAction = SA_TYPE.NORESTORE;
        }
    }

    /**
     * Only upcalled from native code.
     */
    @SuppressWarnings("unused")
    private static void setParams(boolean quietA, boolean slaveA, boolean interactiveA, boolean verboseA, boolean loadSiteFileA,
                    boolean loadInitFileA, boolean debugInitFileA, int restoreActionA,
                    int saveActionA, boolean noRenvironA) {
        RStartParams params = RContext.getInstance().getStartParams();
        params.setQuiet(quietA);
        params.setSlave(slaveA);
        params.setInteractive(interactiveA);
        params.setVerbose(verboseA);
        params.setLoadSiteFile(loadSiteFileA);
        params.setLoadInitFile(loadInitFileA);
        params.setDebugInitFile(debugInitFileA);
        params.setSaveAction(SA_TYPE.values()[saveActionA]);
        params.setRestoreAction(SA_TYPE.values()[restoreActionA]);
        params.setNoRenviron(noRenvironA);
    }

    public boolean getQuiet() {
        return this.quiet;
    }

    public void setQuiet(boolean b) {
        this.quiet = b;
    }

    public boolean getSlave() {
        return slave;
    }

    public void setSlave(boolean b) {
        this.slave = b;
    }

    public boolean getInteractive() {
        return this.interactive;
    }

    public void setInteractive(boolean b) {
        this.interactive = b;
    }

    public boolean getVerbose() {
        return this.verbose;
    }

    public void setVerbose(boolean b) {
        this.verbose = b;
    }

    public boolean getLoadSiteFile() {
        return this.loadSiteFile;
    }

    public void setLoadSiteFile(boolean b) {
        this.loadSiteFile = b;
    }

    public boolean getLoadInitFile() {
        return this.loadInitFile;
    }

    public void setLoadInitFile(boolean b) {
        this.loadInitFile = b;
    }

    public boolean getDebugInitFile() {
        return this.debugInitFile;
    }

    public void setDebugInitFile(boolean b) {
        this.debugInitFile = b;
    }

    public SA_TYPE getRestoreAction() {
        return this.restoreAction;
    }

    public void setRestoreAction(SA_TYPE a) {
        this.restoreAction = a;
    }

    public SA_TYPE getSaveAction() {
        return this.saveAction;
    }

    public void setSaveAction(SA_TYPE a) {
        this.saveAction = a;
    }

    public boolean getNoRenviron() {
        return this.noRenviron;
    }

    public void setNoRenviron(boolean b) {
        this.noRenviron = b;
    }

    public boolean getNoReadline() {
        return this.noReadline;
    }

    public RCmdOptions getRCmdOptions() {
        return cmdOptions;
    }

    public String[] getArguments() {
        return cmdOptions.getArguments();
    }

    public void setEmbedded() {
        this.embedded = true;
    }

    public boolean getEmbedded() {
        return embedded;
    }
}
