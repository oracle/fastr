/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.r.runtime.Utils;

/**
 * Selects a particular subclass of {@link RFFIFactory}. Specification is based on system property
 * {@value #FACTORY_CLASS_PROPERTY}. Current default is a JNR-based implementation.
 */
public class Load_RFFIFactory {
    private static final String FACTORY_CLASS_PROPERTY = "fastr.ffi.factory.class";
    private static final String PACKAGE_PREFIX = "com.oracle.truffle.r.runtime.ffi.";
    private static final String SUFFIX = "_RFFIFactory";
    private static final String DEFAULT_FACTORY = "jnr";
    private static final String DEFAULT_FACTORY_CLASS = mapSimpleName(DEFAULT_FACTORY);

    private static String mapSimpleName(String simpleName) {
        return PACKAGE_PREFIX + simpleName + "." + simpleName.toUpperCase() + SUFFIX;
    }

    /**
     * Singleton instance of the factory. Typically initialized at runtime but may be initialized
     * during image build in an AOT VM, in which case {@code runtime} will be {@code false}.
     */
    private static RFFIFactory instance;

    public static RFFIFactory initialize(boolean runtime) {
        if (instance == null) {
            String prop = System.getProperty(FACTORY_CLASS_PROPERTY);
            try {
                if (prop != null) {
                    if (!prop.contains(".")) {
                        // simple name
                        prop = mapSimpleName(prop);
                    }
                } else {
                    prop = DEFAULT_FACTORY_CLASS;
                }
                instance = (RFFIFactory) Class.forName(prop).newInstance();
                RFFIFactory.setRFFIFactory(instance);
            } catch (Exception ex) {
                throw Utils.fail("Failed to instantiate class: " + prop + ": " + ex);
            }
        }
        instance.initialize(runtime);
        return instance;
    }
}
