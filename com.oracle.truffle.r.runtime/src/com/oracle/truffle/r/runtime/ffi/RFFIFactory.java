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
 * Factory class for the different possible implementations of the {@link RFFI} interface.
 * Specification is based on system property {@value #FACTORY_CLASS_PROPERTY}. Current default is a
 * JNR-based implementation.
 */
public abstract class RFFIFactory {

    private static final String FACTORY_CLASS_PROPERTY = "fastr.ffi.factory.class";
    private static final String PACKAGE_PREFIX = "com.oracle.truffle.r.runtime.ffi.";
    private static final String SUFFIX = "_RFFIFactory";
    private static final String DEFAULT_FACTORY = "jnr";
    private static final String DEFAULT_FACTORY_CLASS = mapSimpleName(DEFAULT_FACTORY);

    static {
        String prop = System.getProperty(FACTORY_CLASS_PROPERTY);
        if (prop != null) {
            if (!prop.contains(".")) {
                // simple name
                prop = mapSimpleName(prop);
            }
        } else {
            prop = DEFAULT_FACTORY_CLASS;
        }
        try {
            theFactory = (RFFIFactory) Class.forName(prop).newInstance();
        } catch (Exception ex) {
            Utils.fail("Failed to instantiate class: " + prop);
        }
    }

    protected static RFFIFactory theFactory;
    protected static final RFFI theRFFI = theFactory.createRFFI();

    private static String mapSimpleName(String simpleName) {
        return PACKAGE_PREFIX + simpleName + "." + simpleName.toUpperCase() + SUFFIX;
    }

    protected static RFFIFactory getFactory() {
        return theFactory;
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

    public LinpackRFFI getLinpackRFFI() {
        Utils.fail("getLinpackRFFI not implemented");
        return null;
    }

    public CRFFI getCRFFI() {
        Utils.fail("getCCallRFFI not implemented");
        return null;
    }

    public UserRngRFFI getUserRngRFFI() {
        Utils.fail("getUserRngRFFI not implemented");
        return null;
    }

}
