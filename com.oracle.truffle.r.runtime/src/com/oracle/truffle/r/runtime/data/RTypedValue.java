/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995, 1996, 1997  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1995-2014, The R Core Team
 * Copyright (c) 2002-2008, The R Foundation
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
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
        return (getGPBits() & S4_MASK) == S4_MASK;
    }

    default void setS4() {
        setGPBits(getGPBits() | S4_MASK);
    }

    default void unsetS4() {
        setGPBits(getGPBits() & ~S4_MASK);
    }
}
