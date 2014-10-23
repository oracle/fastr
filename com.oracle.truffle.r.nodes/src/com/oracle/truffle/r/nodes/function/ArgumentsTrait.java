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
package com.oracle.truffle.r.nodes.function;

public interface ArgumentsTrait {

    public static final String VARARG_NAME = "...";
    public static final String VARARG_GETTER_PREFIX = "..";
    public static final int NO_VARARG = -1;

    String[] getNames();

    /**
     * @return The number of non-<code>null</code> values in the {@link #getNames()} String array
     */
    default int getNameCount() {
        return countNonNull(getNames());
    }

    /**
     * @param names
     * @return The number of non-<code>null</code> values in the given String array
     */
    static int countNonNull(String[] names) {
        int count = 0;
        for (int i = 0; i < names.length; i++) {
            if (names[i] != null) {
                count++;
            }
        }
        return count;
    }

    /**
     * @return {@link #getVarArgIndex(String[])}
     */
    default int getVarArgIndex() {
        return getVarArgIndex(getNames());
    }

    /**
     * @return Whether one of {@link #getNames()} matches {@link #VARARG_NAME} (
     *         {@value #VARARG_NAME})
     */
    default boolean hasVarArgs() {
        return getVarArgIndex() != NO_VARARG;
    }

    /**
     * @param names
     * @return The index of {@link #VARARG_NAME} ({@value #VARARG_NAME}) inside names, or
     *         {@link #NO_VARARG} ( {@value #NO_VARARG}) if there is none
     */
    static int getVarArgIndex(String[] names) {
        for (int i = 0; i < names.length; i++) {
            String name = names[i];
            if (VARARG_NAME.equals(name)) {
                return i;
            }
        }
        return NO_VARARG;
    }

    /**
     * Replaces each {@link String} with its {@link String#intern()} equivalent
     *
     * @param names
     */
    static void internalize(String[] names) {
        // Internalize names
        for (int i = 0; i < names.length; i++) {
            String name = names[i];
            names[i] = name != null ? name.intern() : null;
        }
    }

    static boolean isVarArg(String name) {
        return name.equals(VARARG_NAME);
    }

    static boolean isVarArgGetter(String name) {
        if (name.startsWith(VARARG_GETTER_PREFIX)) {
            return !name.equals(VARARG_NAME);
        }
        return false;
    }

}
