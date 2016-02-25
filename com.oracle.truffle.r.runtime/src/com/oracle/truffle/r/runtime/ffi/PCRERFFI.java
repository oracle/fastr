/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.ffi;

/**
 * An interface to the <a href="http://www.pcre.org/original/doc/html/index.html">PCRE</a> library
 * for Perl regular expressions.
 */
public interface PCRERFFI {
    int NOTBOL = 0x00000080;
    int CASELESS = 0x1;

    /**
     * PCRE uses call by reference for error-related information, which we encapsulate and sanitize
     * in this class. The {@code result} value (which is typically an opaque pointer to an internal
     * C struct), is the actual result of the function as per the PCRE spec.
     */
    class Result {
        public final long result;
        public final String errorMessage;
        public final int errOffset;

        public Result(long result, String errorMessage, int errOffset) {
            this.result = result;
            this.errorMessage = errorMessage;
            this.errOffset = errOffset;
        }

    }

    long maketables();

    Result compile(String pattern, int options, long tables);

    Result study(long code, int options);

    int exec(long code, long extra, String subject, int offset, int options, int[] ovector);

}
