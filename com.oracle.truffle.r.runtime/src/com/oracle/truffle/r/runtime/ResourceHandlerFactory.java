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
package com.oracle.truffle.r.runtime;

import java.io.InputStream;
import java.net.URL;

public abstract class ResourceHandlerFactory {
    /**
     * Abstracts the mechanism for accessing resources in the sense of
     * {@link java.lang.Class#getResource(String)}, for environments that might not support that
     * functionality.
     */
    public interface Handler {
        /**
         * See {@link java.lang.Class#getResource(String)}.
         *
         * @return The path component of the {@link java.net URL} returned by
         *         {@link java.lang.Class#getResource(String)}
         */
        URL getResource(Class<?> accessor, String name);

        /**
         * See {@link java.lang.Class#getResourceAsStream(String)}.
         */
        InputStream getResourceAsStream(Class<?> accessor, String name);

        /**
         * Return the contents of all "R" files (ending with ".r" or ".R") relative to
         * {@code accessor} and {@code pkgname/R}. I.e. essentially a directory search.
         */
        String[] getRFiles(Class<?> accessor, String pkgName);
    }

    static {
        String prop = System.getProperty("fastr.resource.factory.class");
        if (prop == null) {
            if (FastRConfig.ManagedMode) {
                prop = "com.oracle.truffle.r.nodes.builtin.EagerResourceHandlerFactory";
            } else {
                prop = LazyResourceHandlerFactory.class.getName();
            }
        }
        try {
            theInstance = (ResourceHandlerFactory) Class.forName(prop).newInstance();
        } catch (Exception ex) {
            // CheckStyle: stop system..print check
            System.err.println("Failed to instantiate class: " + prop);
        }
    }

    private static ResourceHandlerFactory theInstance;

    public static ResourceHandlerFactory getInstance() {
        return theInstance;
    }

    public static Handler getHandler() {
        return getInstance().newHandler();
    }

    protected abstract Handler newHandler();
}
