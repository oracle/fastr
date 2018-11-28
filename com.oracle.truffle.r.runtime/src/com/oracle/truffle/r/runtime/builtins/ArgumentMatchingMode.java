/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.builtins;

/**
 * The argument matching mode for {@link RBuiltinKind#PRIMITIVE} builtins.
 */
public enum ArgumentMatchingMode {
    /**
     * Do not match any arguments by name.
     */
    NO_MATCH_BY_NAME,
    /**
     * Match arguments by name, but ignore the first one. Moreover, any named actual arguments can
     * match formal argument by position, which is normally not the case (normally they are left for
     * "..."). In GNU-R these primitives use {@code ExtractArg} to extract the first exact match
     * from the actual signature.
     */
    MATCH_BY_NAME_EXACT_SKIP_FIRST,
    /**
     * Match all arguments by name. In GNU-R these primitives use {@code matchArgs} for this.
     */
    MATCH_BY_NAME;

    // Note: synchronize with the default value in the RBuiltin annotation
    public static final ArgumentMatchingMode DEFAULT = ArgumentMatchingMode.MATCH_BY_NAME;

    public boolean matchByName() {
        return this == MATCH_BY_NAME || this == MATCH_BY_NAME_EXACT_SKIP_FIRST;
    }

    public boolean isExactOnly() {
        return this == MATCH_BY_NAME_EXACT_SKIP_FIRST;
    }

    /**
     * Number of actual arguments to skip when matching by name. Example:
     * {@code `[`(drop=c(1,2,3), 3)} the fact that the first argument is named "drop" is ignored.
     */
    public int getSkipArgsCount() {
        return this == MATCH_BY_NAME_EXACT_SKIP_FIRST ? 1 : 0;
    }

    /**
     * Normally named actual arguments cannot match formal arguments by position. Example:
     * {@code `[`(drop=c(1,2,3), 3)} although the first argument has name "drop", it can be used in
     * positional matching and will match the first yet un-matched formal argument.
     */
    public boolean allowPositionalMatchOfNamed() {
        return this == MATCH_BY_NAME_EXACT_SKIP_FIRST || this == NO_MATCH_BY_NAME;
    }
}
