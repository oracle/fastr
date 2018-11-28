/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.test.library.fastr;

import java.io.IOException;

public class TestExceptionsClass {

    public TestExceptionsClass() {

    }

    public TestExceptionsClass(String className) throws Throwable {
        if (className != null) {
            throwEx(className);
        }
    }

    public TestExceptionsClass(String className, String msg) throws Throwable {
        if (className != null) {
            throwEx(className, msg);
        }
    }

    public static void exception() throws Throwable {
        throwEx(null);
    }

    public static void exception(String className) throws Throwable {
        throwEx(className);
    }

    public static void exception(String className, String msg) throws Throwable {
        throwEx(className, msg);
    }

    private static void throwEx(String className) throws Throwable {
        throwEx(className, null);
    }

    private static void throwEx(String className, String msg) throws Throwable {
        if ("java.lang.RuntimeException".equals(className)) {
            throw new RuntimeException(msg);
        } else if ("java.io.IOException".equals(className)) {
            throw new IOException(msg);
        } else {
            throw new RuntimeException();
        }
    }
}
