/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.data;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;

/**
 * A simple wrapper class for passing the ... argument through RArguments
 */
public class RArgsValuesAndNames {
    /**
     * Default instance for empty "..." ("..." that resolve to contain no expression at runtime).
     * The {@link RMissing#instance} for "...".
     */
    public static final RArgsValuesAndNames EMPTY = new RArgsValuesAndNames(new Object[0], new String[0]);

    @CompilationFinal private final Object[] values;
    /**
     * May NOT be null. A single <code>null</code> name denotes "no name provided".
     */
    @CompilationFinal private final String[] names;
    /**
     * @see #isAllNamesEmpty()
     */
    private final boolean allNamesEmpty;

    public RArgsValuesAndNames(Object[] values, String[] names) {
        if (names != null) {
            assert names.length == values.length;
        }
        this.values = values;
        if (names == null) {
            this.names = new String[values.length];
            this.allNamesEmpty = true;
        } else {
            this.names = names;
            this.allNamesEmpty = allNamesNull(names);
        }
    }

    private static boolean allNamesNull(String[] names) {
        for (String name : names) {
            if (name != null) {
                return false;
            }
        }
        return true;
    }

    public Object[] getValues() {
        return values;
    }

    /**
     * @return {@link #names}
     */
    public String[] getNames() {
        return names;
    }

    /**
     * @return Returns {@link #names} OR <code>null</code> if {@link #allNamesEmpty}
     */
    public String[] getNamesNull() {
        if (allNamesEmpty) {
            return null;
        }
        return names;
    }

    public int length() {
        assert names == null || values.length == names.length;
        return values.length;
    }

    /**
     * @return {@link #isAllNamesEmpty()}
     */
    public boolean isAllNamesEmpty() {
        return allNamesEmpty;
    }

    /**
     * @return The same as {@link #isMissing()}, kept for semantic context.
     */
    public boolean isEmpty() {
        return length() == 0;
    }

    /**
     * @return The same as {@link #isEmpty()}, kept for semantic context.
     */
    public boolean isMissing() {
        return length() == 0;
    }
}
