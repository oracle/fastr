/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.base.printer;

import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

final class Utils {

    private Utils() {
        // no instance allowed
    }

    public static String snprintf(int size, String format, Object... args) {
        String fs = String.format(format, args);
        return fs.length() <= size ? fs : fs.substring(0, size);
    }

    public static String asBlankArg(int blanks) {
        return blanks == 0 ? "" : blanks + "";
    }

    @SuppressWarnings("unchecked")
    static <T> T castTo(Object x) {
        if (x instanceof RNull) {
            return null;
        } else {
            return (T) x;
        }
    }

    static <T> T getDataAt(RAbstractVector v, int index) {
        return index < v.getLength() ? Utils.<T> castTo(v.getDataAtAsObject(index)) : null;
    }

    static int indexWidth(int n) {
        return (int) (Math.log10(n + 0.5) + 1);
    }
}
