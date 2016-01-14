/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;

/**
 * Support for the {@code all.names} function.
 */
public class RAllNames {
    public static final class State {
        private final ArrayList<String> names = new ArrayList<>();
        private final boolean functions;
        private final boolean unique;
        private final int maxNames;

        private State(boolean functions, int maxNames, boolean unique) {
            this.functions = functions;
            this.unique = unique;
            this.maxNames = maxNames;
        }

        public static State create(boolean functions, int maxNames, boolean unique) {
            return new State(functions, maxNames, unique);
        }

        public String[] getNames() {
            String[] result = new String[names.size()];
            return names.toArray(result);
        }

        public boolean includeFunctions() {
            return functions;
        }

        public void addName(String name) {
            if (maxNames == -1 || names.size() < maxNames) {
                if (unique) {
                    // not worth using a hashset
                    for (String xname : names) {
                        if (xname.equals(name)) {
                            return;
                        }
                    }
                }
                names.add(name);
            }
        }
    }
}
