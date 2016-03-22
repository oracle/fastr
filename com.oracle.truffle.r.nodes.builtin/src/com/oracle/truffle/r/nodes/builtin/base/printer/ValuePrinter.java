/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.base.printer;

import java.io.IOException;

interface ValuePrinter<T> {

    /**
     * This attribute instructs the <code>println</code> method not to print the new line character
     * since it has already been printed by an external printing routine, such as the
     * <code>show</code> R-function.
     */
    String DONT_PRINT_NL_ATTR = "no_nl";

    void print(T value, PrintContext printCtx) throws IOException;

    default void println(T value, PrintContext printCtx) throws IOException {
        print(value, printCtx);
        //
        if (!Boolean.TRUE.equals(printCtx.getAttribute(DONT_PRINT_NL_ATTR))) {
            printCtx.output().println();
        } else {
            // Clear the instruction attribute
            printCtx.setAttribute(DONT_PRINT_NL_ATTR, false);
        }
    }
}
