/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.source.Source;

/**
 * Collection of strings that are used to indicate {@link Source} instances that have internal
 * descriptions.
 */
public enum RInternalSourceDescription {

    UNIT_TEST("<unit_test>"),
    SHELL_INPUT("<shell_input>"),
    EXPRESSION_INPUT("<expression_input>"),
    GET_ECHO("<get_echo>"),
    QUIT_EOF("<<quit_eof>>"),
    STARTUP_SHUTDOWN("<startup/shutdown>"),
    REPL_WRAPPER("<repl wrapper>"),
    EVAL_WRAPPER("<eval wrapper>"),
    NO_SOURCE("<no source>"),
    CONTEXT_EVAL("<context_eval>"),
    RF_FINDFUN("<Rf_findfun>"),
    BROWSER_INPUT("<browser_input>"),
    CLEAR_WARNINGS("<clear_warnings>"),
    DEPARSE("<deparse>"),
    GET_CONTEXT("<get_context>"),
    DEBUGTEST_FACTORIAL("<factorial.r>"),
    DEBUGTEST_DEBUG("<debugtest.r>"),
    DEBUGTEST_EVAL("<evaltest.r>"),
    TCK_INIT("<tck_initialization>"),
    PACKAGE("<package: %s deparse>"),
    DEPARSE_ERROR("<package: deparse_error>"),
    LAPPLY("<lapply>");

    public final String string;

    RInternalSourceDescription(String text) {
        this.string = text;
    }

    public static String createPackageDescription(String pkg) {
        return String.format(PACKAGE.string, pkg);

    }

}
