/*
 * Copyright (c) 1995, 1996, 1997  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1995-2014, The R Core Team
 * Copyright (c) 2002-2008, The R Foundation
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, a copy is available at
 * https://www.R-project.org/Licenses/
 */
package com.oracle.truffle.r.runtime.data;

import com.oracle.truffle.r.runtime.RType;

/**
 * Interface for R values that are publicly flowing through the interpreter. Be aware that also the
 * primitive values {@link Integer}, {@link Double}, {@link Byte} and {@link String} flow but are
 * not implementing this interface.
 */
public interface RTypedValue extends RTruffleObject {

    // mask values are the same as in GNU R
    // as is the layout of data (but it's never exposed so it does not matter for correctness)
    int S4_MASK = 1 << 4;
    int GP_BITS_MASK_SHIFT = 8;
    int GP_BITS_MASK = 0xFFFF << GP_BITS_MASK_SHIFT;

    int S4_MASK_SHIFTED = 1 << (4 + GP_BITS_MASK_SHIFT);
    int ASCII_MASK_SHIFTED = 1 << 14;

    int BYTES_MASK = 1 << 1;
    int LATIN1_MASK = 1 << 2;
    int UTF8_MASK = 1 << 3;
    int CACHED_MASK = 1 << 5;
    int ASCII_MASK = 1 << 6;

    RType getRType();

    int getTypedValueInfo();

    void setTypedValueInfo(int value);

    default int getGPBits() {
        return (getTypedValueInfo() & GP_BITS_MASK) >>> GP_BITS_MASK_SHIFT;
    }

    default void setGPBits(int gpbits) {
        setTypedValueInfo((getTypedValueInfo() & ~GP_BITS_MASK) | (gpbits << GP_BITS_MASK_SHIFT));
    }

    default boolean isS4() {
        return (getTypedValueInfo() & S4_MASK_SHIFTED) != 0;
    }

    default void setS4() {
        setTypedValueInfo(getTypedValueInfo() | S4_MASK_SHIFTED);
    }

    default void unsetS4() {
        setTypedValueInfo(getTypedValueInfo() & ~S4_MASK_SHIFTED);
    }
}
