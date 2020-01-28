/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.test.library.fastr;

final class Utils {
    static String errorIn(String left, String right) {
        return errorIn(left, right, false, false, "  ");
    }

    static String errorIn(String left, String right, boolean warning, boolean onlyText, String sep) {
        return errorIn(left, right, warning, onlyText, sep, true);
    }

    static String errorIn(String left, String right, boolean warning, boolean onlyText, String sep, boolean breakLongContext) {
        String errorIn;
        if (warning) {
            errorIn = "Warning message:\n";
        } else {
            errorIn = "Error in ";
        }
        String delim = " : ";

        StringBuilder sb = new StringBuilder();
        if (!onlyText) {
            sb.append("cat('");
        }
        sb.append(errorIn);
        sb.append(left.replaceAll("\\'", "\\\\\\'"));
        sb.append(delim);
        if (breakLongContext && errorIn.length() + left.length() + delim.length() + 1 + right.length() >= 75) {
            sb.append("', '\n', '");
        }
        sb.append(right.replaceAll("\\'", "\\\\\\'"));
        sb.append("', '\n");
        if (!onlyText) {
            if (sep != null) {
                sb.append("', sep='");
                sb.append(sep);
                sb.append("')");
            } else {
                sb.append("')");
            }
        }
        return sb.toString();
    }

}
