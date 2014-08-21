/*
 * Copyright (c) 2014, 2014, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.r.runtime.*;

/**
 * Factory class for the different possible implementations of the {@link RFFI} interface. The
 * choice of factory is made by the R engine and set here by the call to {@link #setRFFIFactory}.
 */
public abstract class RFFIFactory {

    protected static RFFI theRFFI;

    public static void setRFFIFactory(RFFIFactory factory) {
        theRFFI = factory.createRFFI();
    }

    public static RFFI getRFFI() {
        return theRFFI;
    }

    /**
     * Subclass implements this method to actually create the concrete {@link RFFI} instance.
     */
    protected abstract RFFI createRFFI();

    public LapackRFFI getLapackRFFI() {
        Utils.fail("getLapackRFFI not implemented");
        return null;
    }

    public RDerivedRFFI getRDerivedRFFI() {
        Utils.fail("getRDerivedRFFI not implemented");
        return null;
    }

    public CRFFI getCRFFI() {
        Utils.fail("getCRFFI not implemented");
        return null;
    }

    public CallRFFI getCallRFFI() {
        Utils.fail("getCallRFFI not implemented");
        return null;
    }

    public UserRngRFFI getUserRngRFFI() {
        Utils.fail("getUserRngRFFI not implemented");
        return null;
    }

}
