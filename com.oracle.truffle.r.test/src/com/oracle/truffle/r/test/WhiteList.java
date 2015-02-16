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

import java.io.*;
import java.util.*;

import com.oracle.truffle.r.runtime.*;

public abstract class WhiteList {
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
    private final String whiteListResource;

    protected WhiteList(String whiteListResource) {
        this.whiteListResource = whiteListResource;
        String[] vars = new String[10];
        try {
            InputStream is = ResourceHandlerFactory.getHandler().getResourceAsStream(getClass(), whiteListResource);
            if (is == null) {
                throw new FileNotFoundException("Failed to locate: " + whiteListResource);
            } else {
                try (BufferedReader input = new BufferedReader(new InputStreamReader(is))) {
                    String line = null;
                    while ((line = input.readLine()) != null) {
                        // format: expr|fastr-result|gnur-result
                        if (line.startsWith("##")) {
                            String[] pair = line.substring(2).split("=");
                            int vn = Integer.parseInt(pair[0]);
                            vars[vn] = pair[1];
                            continue;
                        }
                        if (line.startsWith("#") || line.length() == 0) { // ignore line, comment
                            continue;
                        }
                        String[] s = line.split("\\" + SPLIT_CHAR);
                        assert s.length == 3;
                        map.put(s[0], new Results(expandVarsAndNLEscape(s[1], vars), expandVarsAndNLEscape(s[2], vars)));
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading " + whiteListResource + ": " + e);
            throw new RuntimeException(e);
        }

    }

    /**
     * Expand all the {@code $n} instances.
     */
    private static String expandVarsAndNLEscape(String s, String[] vars) {
        String result = s;
        int ix;
        while ((ix = result.indexOf('$')) >= 0) {
            char varIndexChar = result.charAt(ix + 1);
            assert '0' <= varIndexChar && varIndexChar <= '9';
            int varIndex = varIndexChar - '0';
            result = result.substring(0, ix) + vars[varIndex] + result.substring(ix + 2);
        }
        if (result.contains("\\n")) {
            result = result.replace("\\n", "\n");
        }

        return result;
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
            System.out.printf("%n%d unused entries in %s%n", unusedCount, whiteListResource);
        }
    }

    private static final String SPLIT_CHAR = "|";

}
