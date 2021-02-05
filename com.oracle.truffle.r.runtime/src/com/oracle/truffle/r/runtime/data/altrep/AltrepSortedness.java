/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.data.altrep;

import com.oracle.truffle.api.CompilerDirectives;

/**
 * This enum represents an information about sortedness of a vector and is transcribed from
 * Rinternals.h.
 */
public enum AltrepSortedness {
    SORTED_DECR_NA_1ST(-2),
    SORTED_DECR(-1),
    UNKNOWN_SORTEDNESS(Integer.MIN_VALUE),
    SORTED_INCR(1),
    SORTED_INCR_NA_1ST(2),
    KNOWN_UNSORTED(0);

    private final int value;

    AltrepSortedness(int value) {
        this.value = value;
    }

    public static AltrepSortedness fromInt(int value) {
        switch (value) {
            case -2:
                return SORTED_DECR_NA_1ST;
            case -1:
                return SORTED_DECR;
            case Integer.MIN_VALUE:
                return UNKNOWN_SORTEDNESS;
            case 1:
                return SORTED_INCR;
            case 2:
                return SORTED_INCR_NA_1ST;
            case 0:
                return KNOWN_UNSORTED;
        }
        assert false : value;
        throw CompilerDirectives.shouldNotReachHere("Got unexpected integer value.");
    }

    public int getValue() {
        return value;
    }
}
