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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.oracle.truffle.r.runtime.data.RDouble;
import com.oracle.truffle.r.runtime.data.RInteger;
import com.oracle.truffle.r.runtime.data.RLogical;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RString;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

public class Utils {

    public static final Set<String> keywords;

    static {
        String[] kw = {"NULL", "NA", "TRUE", "FALSE", "Inf", "NaN",
                        "NA_integer_", "NA_real_", "NA_character_", "NA_complex_", "function",
                        "while", "repeat", "for", "if", "in", "else", "next", "break", "..."};
        keywords = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(kw)));
    }

    public static String snprintf(int size, String format, Object... args) {
        String fs = String.format(format, args);
        return fs.length() <= size ? fs : fs.substring(0, size);
    }

    public static String asBlankArg(int blanks) {
        return blanks == 0 ? "" : blanks + "";
    }

    @SuppressWarnings("unchecked")
    public static <T> T castTo(Object x) {
        if (x instanceof RNull) {
            return null;
        } else {
            return (T) x;
        }
    }

    public static boolean canBeLogicalVector(Object o) {
        return o instanceof RAbstractLogicalVector || o instanceof Byte || o instanceof Boolean;
    }

    public static RAbstractLogicalVector toLogicalVector(Object o) {
        if (o instanceof Byte) {
            return RLogical.valueOf((Byte) o);
        }
        if (o instanceof Boolean) {
            return RLogical.valueOf((Boolean) o);
        }
        return Utils.<RAbstractLogicalVector> castTo(o);
    }

    public static boolean canBeStringVector(Object o) {
        return o instanceof RAbstractStringVector || o instanceof String;
    }

    public static RAbstractStringVector toStringVector(Object o) {
        if (o instanceof String) {
            return RString.valueOf((String) o);
        }
        return Utils.<RAbstractStringVector> castTo(o);
    }

    public static boolean canBeIntVector(Object o) {
        return o instanceof RAbstractIntVector || o instanceof Integer;
    }

    public static RAbstractIntVector toIntVector(Object o) {
        if (o instanceof Integer) {
            return RInteger.valueOf((Integer) o);
        }
        return Utils.<RAbstractIntVector> castTo(o);
    }

    public static boolean canBeDoubleVector(Object o) {
        return o instanceof RAbstractDoubleVector || o instanceof Double;
    }

    public static RAbstractDoubleVector toDoubleVector(Object o) {
        if (o instanceof Double) {
            return RDouble.valueOf((Double) o);
        }
        return Utils.<RAbstractDoubleVector> castTo(o);
    }

    public static <T> T getDataAt(RAbstractVector v, int index) {
        return index < v.getLength() ? Utils.<T> castTo(v.getDataAtAsObject(index)) : null;
    }

    public static boolean isValidName(String name) {
        if (name.isEmpty()) {
            return false;
        }

        if ("...".equals(name)) {
            return true;
        }

        char c = name.charAt(0);
        char cNext = name.length() > 1 ? name.charAt(0) : 0;
        if (c != '.' && !Character.isAlphabetic(c)) {
            return false;
        }
        if (c == '.' && Character.isDigit(cNext)) {
            return false;
        }
        for (int i = 1; i < name.length(); i++) {
            c = name.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != '.' && c != '_') {
                return false;
            }
        }

        return !keywords.contains(name);
    }

    public static int indexWidth(int n) {
        return (int) (Math.log10(n + 0.5) + 1);
    }

}
