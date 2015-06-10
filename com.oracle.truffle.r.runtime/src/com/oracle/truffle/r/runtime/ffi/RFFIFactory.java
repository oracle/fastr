/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.runtime.*;

/**
 * Factory class for the different possible implementations of the {@link RFFI} interface. The
 * choice of factory is made by the R engine and set here by the call to {@link #setRFFIFactory}.
 *
 * The RFFI may need to do special things in the case of multiple contexts, hence any given factory
 * must support the {@link com.oracle.truffle.r.runtime.RContext.StateFactory} interface. However,
 * since we don't know exactly which factory will be used, {@link RContext} references the
 * {@link RFFIContextStateFactory} class.
 */
public abstract class RFFIFactory implements RContext.StateFactory {

    protected static RFFI theRFFI;

    public static void setRFFIFactory(RFFIFactory factory) {
        RFFIContextStateFactory.registerFactory(factory);
        theRFFI = factory.createRFFI();
    }

    public static RFFI getRFFI() {
        assert theRFFI != null : "RFFI factory is not initialized!";
        return theRFFI;
    }

    /**
     * This method will be called immediately after the factory instance is created allowing any
     * additional initialization that could not be done in the constructor.
     */
    protected void initialize() {
    }

    /**
     * Subclass implements this method to actually create the concrete {@link RFFI} instance.
     */
    protected abstract RFFI createRFFI();

    public LapackRFFI getLapackRFFI() {
        throw missing("Lapack");
    }

    public StatsRFFI getStatsRFFI() {
        throw missing("Stats");
    }

    public ToolsRFFI getToolsRFFI() {
        throw missing("Tools");
    }

    public RApplRFFI getRApplRFFI() {
        throw missing("RDerived");
    }

    public CRFFI getCRFFI() {
        throw missing("C");
    }

    public CallRFFI getCallRFFI() {
        throw missing("Call");
    }

    public UserRngRFFI getUserRngRFFI() {
        throw missing("UserRNG");
    }

    public PCRERFFI getPCRERFFI() {
        throw missing("PCRE");
    }

    private static RuntimeException missing(String ffi) throws RuntimeException {
        throw Utils.fail(ffi + " FFI not implemented");
    }

    @TruffleBoundary
    protected static IOException ioex(String errMsg) throws IOException {
        throw new IOException(errMsg);
    }

}
