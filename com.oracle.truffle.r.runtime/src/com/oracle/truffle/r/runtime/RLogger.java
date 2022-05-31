/*
 * Copyright (c) 2019, 2022, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime;

import static com.oracle.truffle.r.runtime.RRuntime.R_LANGUAGE_ID;

import com.oracle.truffle.api.TruffleLogger;

/**
 * Some specialized loggers going beyond the scope of basic class level logging.<br/>
 * 
 * <p>
 * To turn on a specific logger:
 * <ul>
 * <li>via command-line - e.g.
 * <code>mx r --log.R.com.oracle.truffle.r.performaceWaning.level=FINE</code></li>
 * <li>via context options - e.g.
 * <code>Context.newBuilder().option("log.R.com.oracle.truffle.r.performaceWaning.level","INFO")</code>
 * </li>
 * </ul>
 * Note that the registered logger name is always denoted with the language-id prefix -
 * <code>R.loggerName</code>
 * </p>
 * 
 * <p>
 * To log to a file:
 * <ul>
 * <li>via command-line - e.g.
 * <code>mx r --log.R.com.oracle.truffle.r.performaceWaning.level=FINE --log.file=fastr.log</code>
 * </li>
 * <li>via context options - e.g. <code>Context.newBuilder().option("log.file", "fastr.log")</code>
 * </li>
 * </ul>
 * Note that <code>--log.file</code> applies to all loggers.
 * </p>
 */
public class RLogger {

    /**
     * Log all R function calls.
     */
    public static final String LOGGER_FUNCTION_CALLS = "com.oracle.truffle.r.functionCalls";

    /**
     * Log RFFI functionality:<br>
     * <ul>
     * <li>calls performed via .Call, .External, etc.</li>
     * <li>basic info about native data/mirrors</li>
     * </ul>
     * 
     * WARNING: stdout is problematic for embedded mode when using this logger. Always specify a log
     * file e.g. mx r --log.R.com.oracle.truffle.r.rffi.level=FINE --log.file=&lt;yourfile&gt;
     */
    public static final String LOGGER_RFFI = "com.oracle.truffle.r.rffi";

    /**
     * Log ALTREP framework functionality:<br>
     * <ul>
     * <li>Creation of altrep classes descriptors, or altrep instances</li>
     * <li>Calls to altrep native methods</li>
     * <li>etc...</li>
     * </ul>
     */
    public static final String LOGGER_ALTREP = "com.oracle.truffle.r.altrep";

    /**
     * Log a message for each non-trivial variable lookup.
     */
    public static final String LOGGER_COMPLEX_LOOKUPS = "com.oracle.truffle.r.complexLookups";

    /**
     * Log every system function call.
     */
    public static final String LOGGER_SYSTEM_FUNCTION = "com.oracle.truffle.r.systemFunction";

    /**
     * Log eager promises deopts and successful evaluations.
     */
    public static final String LOGGER_EAGER_PROMISES = "com.oracle.truffle.r.eagerPromises";

    /**
     * Log performance warnings.
     */
    public static final String LOGGER_PERFORMANCE_WARNINGS = "com.oracle.truffle.r.performanceWarnings";

    /**
     * Log file access.
     */
    public static final String LOGGER_FILE_ACCEESS = "com.oracle.truffle.r.fileAccess";

    /**
     * Log some return values and information from functions wrapping around PCRE2.
     */
    public static final String LOGGER_PCRE = "com.oracle.truffle.r.pcre";

    public static final String LOGGER_FRAMES = "com.oracle.truffle.r.frames";

    public static final String LOGGER_AST = "com.oracle.truffle.r.ast";

    public static TruffleLogger getLogger(String name) {
        return TruffleLogger.getLogger(R_LANGUAGE_ID, name);
    }
}
