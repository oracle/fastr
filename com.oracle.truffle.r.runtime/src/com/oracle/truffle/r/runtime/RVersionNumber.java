/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

/**
 * Defines the R version number and it's release date. Separate from {@link RVersionInfo} to allow
 * use in {@code static final} fields and to finesse the fact that such fields cannot precede the
 * {@code enum} elements.
 */
public class RVersionNumber {
    public static final String MAJOR = "0";
    public static final String MINOR = "6";
    public static final String PATCH = "1";

    public static final String MAJOR_MINOR = MAJOR + "." + MINOR;
    public static final String FULL = MAJOR + "." + MINOR + "." + PATCH;

    public static final String RELEASE_YEAR = "2014";
    public static final String RELEASE_MONTH = "03";
    public static final String RELEASE_DAY = "11";

    public static final String VERSION_STRING = "FastR version " + FULL + " (" + RELEASE_YEAR + "-" + RELEASE_MONTH + "-" + RELEASE_DAY + ")";

}
