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

import java.util.HashSet;
import java.util.Set;

import com.oracle.truffle.api.source.Source;

/**
 * Collection of strings that are used to indicate {@link Source} instances that have internal
 * descriptions.
 */
public class RInternalSourceDescriptions {
    private static final Set<String> set = new HashSet<>();

    public static final String SHELL_INPUT = add("<shell_input>");
    public static final String EXPRESSION_INPUT = add("<expression_input>");
    public static final String GET_ECHO = add("<get_echo>");
    public static final String QUIT_EOF = add("<<quit_eof>>");
    public static final String STARTUP_SHUTDOWN = add("<startup/shutdown>");
    public static final String REPL_WRAPPER = add("<repl wrapper>");
    public static final String NO_SOURCE = add("<no source>");
    public static final String CONTEXT_EVAL = add("<context_eval>");

    private static String add(String s) {
        set.add(s);
        return s;
    }

    public static boolean isInternal(String s) {
        return set.contains(s);
    }
}
