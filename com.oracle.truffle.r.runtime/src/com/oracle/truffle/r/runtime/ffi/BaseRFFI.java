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

import java.io.*;

/**
 * A statically typed interface to exactly those native functions required by the base package.
 * These methods do not necessarily map 1-1 to a native function, they may involve the invocation of
 * several native functions.
 */
public interface BaseRFFI extends RFFI {
    int getpid();

    String getwd();

    int setwd(String dir);

    /**
     * Try to convert a symbolic link to it's target.
     * 
     * @param path the link path
     * @return the target if {@code path} is a link else {@code null}
     * @throws IOException for any other error except "not a link"
     */
    String readlink(String path) throws IOException;

    void sleep(int seconds);

    /**
     * Returns {@code true} if an only if {@code path} denotes a writeable directory.
     */
    boolean isWriteableDirectory(String path);

    /**
     * Creates a temporary directory using {@code template} and return the resulting path or
     * {@code null} if error.
     */
    String mkdtemp(String template);

    /**
     * Returns {@code true} iff the file denoted by {@code path} exists.
     */
    boolean exists(String path);
}
