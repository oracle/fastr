/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2015, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.engine.shell;

/**
 * Support for Rembedded. Defines startup parameters that can be customized by embedding apps.
 */
public class RStartParams {

    public enum SA_TYPE {
        NORESTORE,
        RESTORE,
        DEFAULT,
        NOSAVE,
        SAVE,
        SAVEASK,
        SUICIDE
    }

    private boolean quiet;
    private boolean slave;
    private boolean interactive = true;
    private boolean verbose;
    private boolean loadSiteFile = true;
    private boolean loadInitFile = true;
    private boolean debugInitFile;
    private SA_TYPE restoreAction = SA_TYPE.RESTORE;
    private SA_TYPE saveAction = SA_TYPE.SAVEASK;
    private boolean noRenviron;

    private static RStartParams active;

    private RStartParams() {

    }

    /**
     * Only upcalled from native code.
     */
    public RStartParams(boolean quiet, boolean slave, boolean interactive, boolean verbose, boolean loadSiteFile,
                    boolean loadInitFile, boolean debugInitFile, int restoreAction,
                    int saveAction, boolean noRenviron) {
        this.quiet = quiet;
        this.slave = slave;
        this.interactive = interactive;
        this.verbose = verbose;
        this.loadSiteFile = loadSiteFile;
        this.loadInitFile = loadInitFile;
        this.debugInitFile = debugInitFile;
        this.restoreAction = SA_TYPE.values()[restoreAction];
        this.saveAction = SA_TYPE.values()[saveAction];
        this.noRenviron = noRenviron;
    }

    public static RStartParams getActive() {
        if (active == null) {
            active = new RStartParams();
        }
        return active;
    }

    public static void setParams(RStartParams rs) {
        active = rs;
    }

}
