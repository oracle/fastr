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

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.r.runtime.FastRConfig;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.context.RContext.ContextState;

/**
 * Factory class for the different possible implementations of the {@link RFFI} interface.
 *
 * The RFFI may need to do special things in the case of multiple contexts, hence any given factory
 * must support the {@link #newContextState()} method. Initialization of factory state that is
 * dependent on the system being properly initialized <b>must</b> be done in the
 * {@link ContextState#initialize} method and not in the constructor or {@link #createRFFI} method
 * as they are invoked in the static block and the system is not typically initialized at that
 * point.
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

    /**
     * Singleton instance of the factory.
     */
    private static RFFIFactory instance;

    @CompilationFinal protected static RFFI theRFFI;
    @CompilationFinal private static Type type;

    static {
        if (instance == null) {
            type = getFactoryType();
            String klassName = type.klassName;
            try {
                instance = (RFFIFactory) Class.forName(klassName).newInstance();
                theRFFI = instance.createRFFI();
            } catch (Exception ex) {
                throw Utils.rSuicide("Failed to instantiate class: " + klassName + ": " + ex);
            }
        }
    }

    private static Type getFactoryType() {
        String prop = System.getProperty(FACTORY_TYPE_PROPERTY);
        if (prop != null) {
            return checkFactoryName(prop);
        }
        prop = System.getenv(FACTORY_CLASS_ENV);
        if (prop != null) {
            return checkFactoryName(prop);
        }
        if (FastRConfig.ManagedMode) {
            return Type.MANAGED;
        }
        return DEFAULT_FACTORY;
    }

    private static Type checkFactoryName(String prop) {
        try {
            Type factory = Type.valueOf(prop.toUpperCase());
            return factory;
        } catch (IllegalArgumentException ex) {
            throw Utils.rSuicide("No RFFI factory: " + prop);
        }
    }

    public static RFFIFactory getInstance() {
        assert instance != null;
        return instance;
    }

    private static RFFI getRFFI() {
        assert theRFFI != null : "RFFI factory is not initialized!";
        return theRFFI;
    }

    /*
     * Some shortcuts to the specific RFFI interfaces:
     */

    public static BaseRFFI getBaseRFFI() {
        return getRFFI().getBaseRFFI();
    }

    public static LapackRFFI getLapackRFFI() {
        return getRFFI().getLapackRFFI();
    }

    public static RApplRFFI getRApplRFFI() {
        return getRFFI().getRApplRFFI();
    }

    public static StatsRFFI getStatsRFFI() {
        return getRFFI().getStatsRFFI();
    }

    public static ToolsRFFI getToolsRFFI() {
        return getRFFI().getToolsRFFI();
    }

    public static CRFFI getCRFFI() {
        return getRFFI().getCRFFI();
    }

    public static CallRFFI getCallRFFI() {
        return getRFFI().getCallRFFI();
    }

    public static UserRngRFFI getUserRngRFFI() {
        return getRFFI().getUserRngRFFI();
    }

    public static PCRERFFI getPCRERFFI() {
        return getRFFI().getPCRERFFI();
    }

    public static ZipRFFI getZipRFFI() {
        return getRFFI().getZipRFFI();
    }

    public static DLLRFFI getDLLRFFI() {
        return getRFFI().getDLLRFFI();
    }

    public static REmbedRFFI getREmbedRFFI() {
        return getRFFI().getREmbedRFFI();
    }

    public static MiscRFFI getMiscRFFI() {
        return getRFFI().getMiscRFFI();
    }

    public static Type getType() {
        return type;
    }

    /**
     * Subclass implements this method to actually create the concrete {@link RFFI} instance.
     */
    protected abstract RFFI createRFFI();

    public abstract ContextState newContextState();
}
