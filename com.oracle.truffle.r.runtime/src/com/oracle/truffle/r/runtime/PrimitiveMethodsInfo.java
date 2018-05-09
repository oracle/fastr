/*
 * Copyright (c) 1995, 1996, 1997  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1995-2014, The R Core Team
 * Copyright (c) 2002-2008, The R Foundation
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates
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
package com.oracle.truffle.r.runtime;

import java.util.Arrays;

import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.env.REnvironment;

public class PrimitiveMethodsInfo {

    public static final int INVALID_INDEX = -1;

    public enum MethodCode {
        NO_METHODS,
        NEEDS_RESET,
        HAS_METHODS,
        SUPPRESSED
    }

    private MethodCode[] primMethodCodes;
    private RFunction[] primGenerics;
    private REnvironment[] primMethodsList;

    public PrimitiveMethodsInfo() {
        // start with length 0 is it's not filled consecutively anyway
        this(new MethodCode[0], new RFunction[0], new REnvironment[0]);
    }

    private PrimitiveMethodsInfo(MethodCode[] primMethodCodes, RFunction[] primGenerics, REnvironment[] primMethodList) {
        this.primMethodCodes = primMethodCodes;
        this.primGenerics = primGenerics;
        this.primMethodsList = primMethodList;
    }

    public MethodCode getPrimMethodCode(int index) {
        return primMethodCodes[index];
    }

    public void setPrimMethodCode(int index, MethodCode code) {
        primMethodCodes[index] = code;
    }

    public RFunction getPrimGeneric(int index) {
        return index < primGenerics.length ? primGenerics[index] : null;
    }

    public void setPrimGeneric(int index, RFunction generic) {
        primGenerics[index] = generic;
    }

    public REnvironment getPrimMethodList(int index) {
        return primMethodsList[index];
    }

    public void setPrimMethodList(int index, REnvironment methodsList) {
        primMethodsList[index] = methodsList;
    }

    public int getSize() {
        return primMethodCodes.length;
    }

    public PrimitiveMethodsInfo resize(int size) {
        assert size >= getSize();
        primMethodCodes = Arrays.copyOf(primMethodCodes, size);
        primGenerics = Arrays.copyOf(primGenerics, size);
        primMethodsList = Arrays.copyOf(primMethodsList, size);
        return this;
    }

    public PrimitiveMethodsInfo duplicate() {
        return new PrimitiveMethodsInfo(Arrays.copyOf(primMethodCodes, getSize()), Arrays.copyOf(primGenerics, getSize()), Arrays.copyOf(primMethodsList, getSize()));
    }
}
