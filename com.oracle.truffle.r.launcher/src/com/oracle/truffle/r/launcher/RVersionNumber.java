/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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

/**
 * Defines the R version number and it's release date. Separate from {code RVersionInfo} to allow
 * use in {@code static final} fields and to finesse the fact that such fields cannot precede the
 * {@code enum} elements.
 * <p>
 * N.B. Since packages check against the version number, we have to have a GnuR version number and,
 * for consistency, we set the date to that of the corresponding GnuR release.
 */
public class RVersionNumber {
    public static final String MAJOR = "3";
    public static final String MINOR = "6";
    public static final String PATCH = "1";

    public static final int R_VERSION = (3 << 16) + (6 << 8) + 1;

    public static final String MAJOR_MINOR = MAJOR + "." + MINOR;
    public static final String MINOR_PATCH = MINOR + "." + PATCH;
    public static final String FULL = MAJOR + "." + MINOR + "." + PATCH;
    public static final String R_HYPHEN_FULL = "R-" + FULL;

    public static final String RELEASE_YEAR = "2019";
    public static final String RELEASE_MONTH = "07";
    public static final String RELEASE_DAY = "05";

    public static final String RELEASE_DATE = " (" + RELEASE_YEAR + "-" + RELEASE_MONTH + "-" + RELEASE_DAY + ")";

    public static final String VERSION_STRING = "FastR version " + FULL + RELEASE_DATE;

    public static final String COPYRIGHT = "Copyright (c) 2013-19, Oracle and/or its affiliates\n" +
                    "Copyright (c) 1995-2018, The R Core Team\n" +
                    "Copyright (c) 2018 The R Foundation for Statistical Computing\n" +
                    "Copyright (c) 2012-4 Purdue University\n" +
                    "Copyright (c) 1997-2002, Makoto Matsumoto and Takuji Nishimura\n" +
                    "All rights reserved.\n";

    /**
     * From {@code Rinternals.h} and {@code library.R}.
     */
    public static final String INTERNALS_UID = "2fdf6c18-697a-4ba7-b8ef-11c0d92f1327";

    public static void main(String[] args) {
        System.out.printf("R version %s", FULL);
    }

    public static final String LICENSE = "This software is distributed under the terms of the GNU General Public License\n" +
                    "Version 3, June 2007. The terms of the license are in a file called COPYING\n" +
                    "which you should have received with this software. A copy of the license can be\n" +
                    "found at http://www.gnu.org/licenses/gpl-3.0.html.\n" +
                    "\n" +
                    "'Share and Enjoy.'";
}
