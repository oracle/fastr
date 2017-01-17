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
package com.oracle.truffle.r.engine.interop.ffi;

import com.oracle.truffle.r.runtime.context.RContext.ContextState;
import com.oracle.truffle.r.runtime.ffi.CRFFI;
import com.oracle.truffle.r.runtime.ffi.CallRFFI;
import com.oracle.truffle.r.runtime.ffi.DLLRFFI;
import com.oracle.truffle.r.runtime.ffi.RFFI;
import com.oracle.truffle.r.runtime.ffi.StatsRFFI;
import com.oracle.truffle.r.runtime.ffi.UserRngRFFI;
import com.oracle.truffle.r.runtime.ffi.jni.JNI_RFFIFactory;

/**
 * Incremental approach to using Truffle, defaults to the JNI factory.
 *
 */
public class Truffle_RFFIFactory extends JNI_RFFIFactory implements RFFI {

    @Override
    protected void initialize(boolean runtime) {
        super.initialize(runtime);
    }

    @Override
    public ContextState newContextState() {
        return new TruffleRFFIContextState();
    }

    private CRFFI cRFFI;

    @Override
    public CRFFI getCRFFI() {
        if (cRFFI == null) {
            cRFFI = new TruffleC();
        }
        return cRFFI;
    }

    private DLLRFFI dllRFFI;

    @Override
    public DLLRFFI getDLLRFFI() {
        if (dllRFFI == null) {
            dllRFFI = new TruffleDLL();
        }
        return dllRFFI;
    }

    private UserRngRFFI truffleUserRngRFFI;

    @Override
    public UserRngRFFI getUserRngRFFI() {
        if (truffleUserRngRFFI == null) {
            truffleUserRngRFFI = new TruffleUserRng();
        }
        return truffleUserRngRFFI;
    }

    private CallRFFI truffleCallRFFI;

    @Override
    public CallRFFI getCallRFFI() {
        if (truffleCallRFFI == null) {
            truffleCallRFFI = new TruffleCall();
        }
        return truffleCallRFFI;
    }

    private StatsRFFI truffleStatsRFFI;

    @Override
    public StatsRFFI getStatsRFFI() {
        if (TruffleDLL.isBlacklisted("stats")) {
            return super.getStatsRFFI();
        }
        if (truffleStatsRFFI == null) {
            truffleStatsRFFI = new TruffleStats();
        }
        return truffleStatsRFFI;
    }
}
