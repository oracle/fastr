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
package com.oracle.truffle.r.runtime;

/**
 * Support methods for regular expressions.
 */
public class RegExp {
    private static final String[] preDefinedClassesFrom = new String[]{"[:alpha:]", "[:alnum:]", "[:digit:]"};
    private static final String[] preDefinedClassesTo = new String[]{"a-zA-Z", "0-9A-Za-z", "0-9"};

    /**
     * R defines some short forms of character classes. E.g. {@code [[:alnum:]]} means
     * {@code [0-9A-Za-z]} but independent of locale and character encoding. So we have to translate
     * these for use with Java regexp. TODO handle the complete set and do locale and character
     * encoding
     */
    public static String checkPreDefinedClasses(String pattern) {
        String result = pattern;
        boolean none = false;
        while (!none && result.indexOf("[[") >= 0) {
            none = true;
            for (int i = 0; i < preDefinedClassesFrom.length; i++) {
                int ix = result.indexOf(preDefinedClassesFrom[i]);
                if (ix >= 0) {
                    result = result.substring(0, ix) + preDefinedClassesTo[i] + result.substring(ix + preDefinedClassesFrom[i].length());
                    none = false;
                }
            }
            // if none is still true, we didn't find any so we are done.
        }
        return result;
    }

}
