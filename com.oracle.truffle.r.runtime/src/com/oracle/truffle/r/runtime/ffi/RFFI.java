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

import com.oracle.truffle.r.runtime.context.RContext.ContextState;

/**
 * FastR foreign function interface. There are separate interfaces for the various kinds of foreign
 * functions that are possible in R:
 * <ul>
 * <li>{@link BaseRFFI}: the specific, typed, foreign functions required the built-in {@code base}
 * package.</li>
 * <li>{@link LapackRFFI}: the specific, typed, foreign functions required by the built-in
 * {@code Lapack} functions.</li>
 * <li>{@link StatsRFFI}: native functions in the {@code stats} package.</li>
 * <li>{@link ToolsRFFI}: native functions in the {@code tools} package.</li>
 * <li>{@link CRFFI}: {@code .C} and {@code .Fortran} call interface.</li>
 * <li>{@link CallRFFI}: {@code .Call} and {@code .External} call interface.</li>
 * <li>{@link UserRngRFFI}: specific interface to user-supplied random number generator.</li>
 * <li>{@link PCRERFFI}: interface to PCRE library (Perl regexp).</li>
 * <li>{@link ZipRFFI}: interface to zip compression</li>
 * <li>{@link DLLRFFI}: interface to dll functions, e.g., {@code dlopen}</li>
 * <li>{@link REmbedRFFI}: interface to embedded support</li>
 * <li>{@link MiscRFFI}: interface to miscellaneous native functions</li>
 * </ul>
 *
 * These interfaces may be implemented by one or more providers, specified either when the FastR
 * system is built or run.
 */
public abstract class RFFI implements ContextState {

    public final CRFFI cRFFI;
    public final BaseRFFI baseRFFI;
    public final CallRFFI callRFFI;
    public final DLLRFFI dllRFFI;
    public final UserRngRFFI userRngRFFI;
    public final ZipRFFI zipRFFI;
    public final PCRERFFI pcreRFFI;
    public final LapackRFFI lapackRFFI;
    public final StatsRFFI statsRFFI;
    public final ToolsRFFI toolsRFFI;
    public final REmbedRFFI embedRFFI;
    public final MiscRFFI miscRFFI;

    protected RFFI(CRFFI cRFFI, BaseRFFI baseRFFI, CallRFFI callRFFI, DLLRFFI dllRFFI, UserRngRFFI userRngRFFI, ZipRFFI zipRFFI, PCRERFFI pcreRFFI, LapackRFFI lapackRFFI, StatsRFFI statsRFFI,
                    ToolsRFFI toolsRFFI, REmbedRFFI embedRFFI, MiscRFFI miscRFFI) {
        this.cRFFI = cRFFI;
        this.baseRFFI = baseRFFI;
        this.callRFFI = callRFFI;
        this.dllRFFI = dllRFFI;
        this.userRngRFFI = userRngRFFI;
        this.zipRFFI = zipRFFI;
        this.pcreRFFI = pcreRFFI;
        this.lapackRFFI = lapackRFFI;
        this.statsRFFI = statsRFFI;
        this.toolsRFFI = toolsRFFI;
        this.embedRFFI = embedRFFI;
        this.miscRFFI = miscRFFI;
    }
}
