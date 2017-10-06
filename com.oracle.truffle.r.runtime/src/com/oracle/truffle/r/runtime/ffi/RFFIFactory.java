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
package com.oracle.truffle.r.runtime.ffi;

import com.oracle.truffle.r.runtime.FastRConfig;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.RContext.ContextState;

/**
 * Factory class for the different possible implementations of the {@link RFFI} interface.
 *
 * Initialization of factory state that is dependent on the system being properly initialized
 * <b>must</b> be done in the {@link ContextState#initialize} method and not in the constructor or
 * {@link #createRFFIContext()} method as they are invoked in the static block and the system is not
 * typically initialized at that point.
 */
public abstract class RFFIFactory {
    public enum Type {
        LLVM("com.oracle.truffle.r.ffi.impl.llvm.TruffleLLVM_RFFIFactory"),
        MANAGED("com.oracle.truffle.r.ffi.impl.managed.Managed_RFFIFactory"),
        NFI("com.oracle.truffle.r.ffi.impl.nfi.TruffleNFI_RFFIFactory");

        private final String klassName;

        Type(String klassName) {
            this.klassName = klassName;
        }
    }

    private static final String FACTORY_TYPE_PROPERTY = "fastr.rffi.factory.type";
    private static final String FACTORY_CLASS_ENV = "FASTR_RFFI";
    private static final Type DEFAULT_FACTORY = Type.NFI;

    private static Type type;
    private static RFFIFactory rffiFactory;

    static {
        String property = System.getProperty(FACTORY_TYPE_PROPERTY);
        String env = System.getenv(FACTORY_CLASS_ENV);
        if (property != null) {
            type = checkFactoryName(property);
        } else if (env != null) {
            type = checkFactoryName(env);
        } else if (FastRConfig.ManagedMode) {
            type = Type.MANAGED;
        } else {
            type = DEFAULT_FACTORY;
        }
        String klassName = getFactoryType().klassName;
        try {
            rffiFactory = (RFFIFactory) Class.forName(klassName).newInstance();
        } catch (Exception ex) {
            throw Utils.rSuicide("Failed to instantiate class: " + klassName + ": " + ex);
        }
    }

    public static Type getFactoryType() {
        return type;
    }

    private static Type checkFactoryName(String prop) {
        try {
            return Type.valueOf(prop.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw Utils.rSuicide("No RFFI factory: " + prop);
        }
    }

    private static RFFI getRFFI() {
        return RContext.getInstance().getStateRFFI();
    }

    /*
     * Some shortcuts to the specific RFFI interfaces:
     */

    public static BaseRFFI getBaseRFFI() {
        return getRFFI().baseRFFI;
    }

    public static LapackRFFI getLapackRFFI() {
        return getRFFI().lapackRFFI;
    }

    public static StatsRFFI getStatsRFFI() {
        return getRFFI().statsRFFI;
    }

    public static ToolsRFFI getToolsRFFI() {
        return getRFFI().toolsRFFI;
    }

    public static CRFFI getCRFFI() {
        return getRFFI().cRFFI;
    }

    public static CallRFFI getCallRFFI() {
        return getRFFI().callRFFI;
    }

    public static UserRngRFFI getUserRngRFFI() {
        return getRFFI().userRngRFFI;
    }

    public static PCRERFFI getPCRERFFI() {
        return getRFFI().pcreRFFI;
    }

    public static ZipRFFI getZipRFFI() {
        return getRFFI().zipRFFI;
    }

    public static DLLRFFI getDLLRFFI() {
        return getRFFI().dllRFFI;
    }

    public static REmbedRFFI getREmbedRFFI() {
        return getRFFI().embedRFFI;
    }

    public static MiscRFFI getMiscRFFI() {
        return getRFFI().miscRFFI;
    }

    /**
     * Subclass implements this method to actually create the concrete {@link RFFIContext} instance.
     */
    protected abstract RFFIContext createRFFIContext();

    public static RFFIContext create() {
        return rffiFactory.createRFFIContext();
    }
}
