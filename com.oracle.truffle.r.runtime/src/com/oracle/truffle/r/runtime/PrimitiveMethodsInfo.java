/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995, 1996, 1997  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1995-2014, The R Core Team
 * Copyright (c) 2002-2008, The R Foundation
 * Copyright (c) 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
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
        return primGenerics[index];
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
