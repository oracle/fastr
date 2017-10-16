/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.r.runtime.RRuntime;

/**
 * Internally GNU R distinguishes "strings" and "vectors of strings" using the {@code CHARSXP} and
 * {@code STRSXP} types, respectively. Although this difference is invisible at the R level, it
 * manifests itself in the R FFI as several functions traffic in the {@code CHARSXP} type. Since
 * FastR already uses {@code String} to denote a length-1 string vector, it cannot be used to
 * represent a {@code CHARSXP}, so this class exists to do so.
 *
 * N.B. Use limited to RFFI implementations.
 */
public final class CharSXPWrapper extends RObject implements RTruffleObject {
    private static final CharSXPWrapper NA = new CharSXPWrapper(RRuntime.STRING_NA);

    private String contents;

    private CharSXPWrapper(String contents) {
        this.contents = contents;
    }

    public String getContents() {
        return NativeDataAccess.getData(this, contents);
    }

    @Override
    public String toString() {
        return "CHARSXP(" + getContents() + ")";
    }

    public static CharSXPWrapper create(String contents) {
        if (contents == RRuntime.STRING_NA) {
            return NA;
        } else {
            return new CharSXPWrapper(contents);
        }
    }

    public long allocateNativeContents() {
        try {
            return NativeDataAccess.allocateNativeContents(this, contents);
        } finally {
            contents = null;
        }
    }
}
