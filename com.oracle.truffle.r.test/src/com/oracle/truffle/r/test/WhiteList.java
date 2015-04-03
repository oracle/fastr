/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.test;

import java.util.*;

public final class WhiteList implements TestTrait {
    public static class Results {

        public final String fastR;
        public final String expected;
        private boolean used;

        public Results(String fastR, String expected) {
            this.fastR = fastR;
            this.expected = expected;
        }

        @Override
        public String toString() {
            return "FastR: " + fastR + ", Expected: " + expected;
        }
    }

    private final Map<String, Results> map = new HashMap<>();
    private final String name;

    public WhiteList(String name) {
        this.name = name;
    }

    public void add(String input, String actual, String expected) {
        map.put(input, new Results(actual, expected));
    }

    Results get(String expression) {
        return map.get(expression);
    }

    void markUsed(String expression) {
        map.get(expression).used = true;
    }

    public void report() {
        int unusedCount = map.size();
        for (Map.Entry<String, Results> entry : map.entrySet()) {
            if (entry.getValue().used) {
                unusedCount--;
            }
        }
        if (unusedCount > 0) {
            System.out.printf("%n%d unused entries in whitelist (%s)%n", unusedCount, name);
        }
    }
}
