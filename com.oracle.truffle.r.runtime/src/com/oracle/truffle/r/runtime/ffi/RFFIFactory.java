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
    private enum Factory {
        JNI("com.oracle.truffle.r.runtime.ffi.jni.JNI_RFFIFactory"),
        LLVM("com.oracle.truffle.r.engine.interop.ffi.llvm.TruffleLLVM_RFFIFactory"),
        NFI("com.oracle.truffle.r.engine.interop.ffi.nfi.TruffleNFI_RFFIFactory");

        private final String klassName;

        Factory(String klassName) {
            this.klassName = klassName;
        }
    }

    private static final String FACTORY_CLASS_PROPERTY = "fastr.rffi.factory.class";
    private static final String FACTORY_CLASS_NAME_PROPERTY = "fastr.rffi.factory";
    private static final String FACTORY_CLASS_ENV = "FASTR_RFFI";
    private static final Factory DEFAULT_FACTORY = Factory.JNI;

    /**
     * Singleton instance of the factory.
     */
    private static RFFIFactory instance;

    @CompilationFinal protected static RFFI theRFFI;

    static {
        if (instance == null) {
            String klassName = getFactoryClassName();
            try {
                instance = (RFFIFactory) Class.forName(klassName).newInstance();
                theRFFI = instance.createRFFI();
            } catch (Exception ex) {
                throw Utils.rSuicide("Failed to instantiate class: " + klassName + ": " + ex);
            }
        }
    }

    private static String getFactoryClassName() {
        String prop = System.getProperty(FACTORY_CLASS_PROPERTY);
        if (prop != null) {
            return prop;
        }
        prop = System.getProperty(FACTORY_CLASS_NAME_PROPERTY);
        if (prop != null) {
            return checkFactoryName(prop);
        }
        prop = System.getenv(FACTORY_CLASS_ENV);
        if (prop != null) {
            return checkFactoryName(prop);
        }
        return DEFAULT_FACTORY.klassName;
    }

    private static String checkFactoryName(String prop) {
        try {
            Factory factory = Factory.valueOf(prop.toUpperCase());
            return factory.klassName;
        } catch (IllegalArgumentException ex) {
            throw Utils.rSuicide("No RFFI factory: " + prop);
        }

    }

    public static RFFIFactory getInstance() {
        assert instance != null;
        return instance;
    }

    public static RFFI getRFFI() {
        assert theRFFI != null : "RFFI factory is not initialized!";
        return theRFFI;
    }

    /**
     * Subclass implements this method to actually create the concrete {@link RFFI} instance.
     */
    protected abstract RFFI createRFFI();

    public abstract ContextState newContextState();
}
