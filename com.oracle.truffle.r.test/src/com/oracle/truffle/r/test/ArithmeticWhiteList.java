/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.test;

public class ArithmeticWhiteList {
    public static final WhiteList WHITELIST = WhiteList.create("arithmetic");

    /**
     * This list was generated on Mac OS X El Capitan on Oct 19th 2016 using the results from
     * running R-3.3.0, and the {@code AnalyzeExpectedTestOutput} tool. Since FastR is consistent in
     * its results across platforms, unlike GnuR, this whitelist can be used on any platform with an
     * {@code ExpectedTestOutput.test} file generated on a Mac OS X system.
     *
     * However, if the entire {@code ExpectedTestOutput.test} file were to be regenerated on, say, a
     * Linux platform, this whitelist would be incomplete and need to be updated.
     */
    static {
        WHITELIST.add("{ abs((-0-1i)/(0+0i)) }", "[1] NaN\n", "[1] Inf\n");
        WHITELIST.add("{ abs((-1-0i)/(0+0i)) }", "[1] NaN\n", "[1] Inf\n");
        WHITELIST.add("{ ((0+1i)/0) * ((0+1i)/0) }", "[1] NaN+NaNi\n", "[1] -Inf+NaNi\n");
        WHITELIST.add("{ ((0-1i)/0) * ((-1-1i)/0) }", "[1] NaN+NaNi\n", "[1] -Inf+Infi\n");
        WHITELIST.add("{ ((0-1i)/0) * ((0+1i)/0) }", "[1] NaN+NaNi\n", "[1] Inf+NaNi\n");
        WHITELIST.add("{ ((0-1i)/0) * ((0-1i)/0) }", "[1] NaN+NaNi\n", "[1] -Inf+NaNi\n");
        WHITELIST.add("{ ((0-1i)/0) * ((1-1i)/0) }", "[1] NaN+NaNi\n", "[1] -Inf-Infi\n");
        WHITELIST.add("{ (-1+0i)/(0+0i) }", "[1] NaN+NaNi\n", "[1] -Inf+NaNi\n");
        WHITELIST.add("{ (-1-1i)/(0+0i) }", "[1] NaN+NaNi\n", "[1] -Inf-Infi\n");
        WHITELIST.add("{ (0+1i)/(0+0i) }", "[1] NaN+NaNi\n", "[1] NaN+Infi\n");
        WHITELIST.add("{ (1+0i)/(0+0i) }", "[1] NaN+NaNi\n", "[1] Inf+NaNi\n");
        WHITELIST.add("{ (1+1i)/(0+0i) }", "[1] NaN+NaNi\n", "[1] Inf+Infi\n");
        WHITELIST.add("{ (1+2i) / ((0-1i)/(0+0i)) }", "[1] NaN+NaNi\n", "[1] 0+0i\n");
        WHITELIST.add("{ 1/((1+0i)/(0+0i)) }", "[1] NaN+NaNi\n", "[1] 0+0i\n");
        WHITELIST.add("{ ((1+0i)/(0+0i)) ^ (-3) }", "[1] NaN+NaNi\n", "[1] 0+0i\n");
        WHITELIST.add("{ ((1+1i)/(0+0i)) ^ (-3) }", "[1] NaN+NaNi\n", "[1] 0+0i\n");
        WHITELIST.add("{ -((0+1i)/0)  }", "[1] NaN+NaNi\n", "[1] NaN-Infi\n");
        WHITELIST.add("{ -((1+0i)/0)  }", "[1] NaN+NaNi\n", "[1] -Inf+NaNi\n");
        WHITELIST.add("{ -c((1+0i)/0,2) }", "[1] NaN+NaNi  -2+  0i\n", "[1] -Inf+NaNi   -2+  0i\n");
        WHITELIST.add("{ c(0/0+1i,2+1i) == c(1+1i,2+1i) }", "[1] FALSE  TRUE\n", "[1]   NA TRUE\n");
        WHITELIST.add("{ c(1+1i,2+1i) == c(0/0+1i,2+1i) }", "[1] FALSE  TRUE\n", "[1]   NA TRUE\n");
        WHITELIST.add("exp(-abs((0+1i)/(0+0i)))", "[1] NaN\n", "[1] 0\n");
        WHITELIST.add("((0/0)+1i)*(-(1/0))", "[1] NaN+NaNi\n", "[1] NaN-Infi\n");
        WHITELIST.add("((0/0)+1i)*(1/0)", "[1] NaN+NaNi\n", "[1] NaN+Infi\n");
        WHITELIST.add("((0/0)+1i)-(1+NA)", "[1] NA\n", "[1] NaN+1i\n");
        WHITELIST.add("((0/0)+1i)-(3.4+NA)", "[1] NA\n", "[1] NaN+1i\n");
        WHITELIST.add("((0/0)+1i)/(-0.0)", "[1] NaN+NaNi\n", "[1] NaN-Infi\n");
        WHITELIST.add("((0/0)+1i)/FALSE", "[1] NaN+NaNi\n", "[1] NaN+Infi\n");
        WHITELIST.add("((0/0)+1i)/c(FALSE,FALSE,FALSE)", "[1] NaN+NaNi NaN+NaNi NaN+NaNi\n", "[1] NaN+Infi NaN+Infi NaN+Infi\n");
        WHITELIST.add("(-(1/0))*((0/0)+1i)", "[1] NaN+NaNi\n", "[1] NaN-Infi\n");
        WHITELIST.add("(-(1/0))*(1i+NA)", "[1] NA\n", "[1] NaN+NaNi\n");
        WHITELIST.add("(-(1/0))^(1i+NA)", "[1] NA\n", "[1] NaN+NaNi\n");
        WHITELIST.add("(1+NA)+((0/0)+1i)", "[1] NA\n", "[1] NaN+1i\n");
        WHITELIST.add("(1+NA)/((0/0)+1i)", "[1] NA\n", "[1] NaN+NaNi\n");
        WHITELIST.add("(1+NA)^((0/0)+1i)", "[1] NA\n", "[1] NaN+NaNi\n");
        WHITELIST.add("(1/0)*((0/0)+1i)", "[1] NaN+NaNi\n", "[1] NaN+Infi\n");
        WHITELIST.add("(1/0)*(1i+NA)", "[1] NA\n", "[1] NaN+NaNi\n");
        WHITELIST.add("(1/0)^(1i+NA)", "[1] NA\n", "[1] NaN+NaNi\n");
        WHITELIST.add("(1i+NA)*(-(1/0))", "[1] NA\n", "[1] NaN+NaNi\n");
        WHITELIST.add("(1i+NA)*(1/0)", "[1] NA\n", "[1] NaN+NaNi\n");
        WHITELIST.add("(1i+NA)^((0/0)+1i)", "[1] NA\n", "[1] NaN+NaNi\n");
        WHITELIST.add("(1i+NA)^(-(0/0))", "[1] NA\n", "[1] NaN+NaNi\n");
        WHITELIST.add("(1i+NA)^(-(1/0))", "[1] NA\n", "[1] NaN+NaNi\n");
        WHITELIST.add("(1i+NA)^(0/0)", "[1] NA\n", "[1] NaN+NaNi\n");
        WHITELIST.add("(1i+NA)^(1/0)", "[1] NA\n", "[1] NaN+NaNi\n");
        WHITELIST.add("(3.4+NA)+((0/0)+1i)", "[1] NA\n", "[1] NaN+1i\n");
        WHITELIST.add("(3.4+NA)/((0/0)+1i)", "[1] NA\n", "[1] NaN+NaNi\n");
        WHITELIST.add("(3.4+NA)^((0/0)+1i)", "[1] NA\n", "[1] NaN+NaNi\n");
        WHITELIST.add("1i/(-0.0)", "[1] NaN+NaNi\n", "[1] NaN-Infi\n");
        WHITELIST.add("1i/FALSE", "[1] NaN+NaNi\n", "[1] NaN+Infi\n");
        WHITELIST.add("1i/c(FALSE,FALSE,FALSE)", "[1] NaN+NaNi NaN+NaNi NaN+NaNi\n", "[1] NaN+Infi NaN+Infi NaN+Infi\n");
        WHITELIST.add("c(1i,1i,1i)/(-0.0)", "[1] NaN+NaNi NaN+NaNi NaN+NaNi\n", "[1] NaN-Infi NaN-Infi NaN-Infi\n");
        WHITELIST.add("c(1i,1i,1i)/FALSE", "[1] NaN+NaNi NaN+NaNi NaN+NaNi\n", "[1] NaN+Infi NaN+Infi NaN+Infi\n");
        WHITELIST.add("c(1i,1i,1i)/c(FALSE,FALSE,FALSE)", "[1] NaN+NaNi NaN+NaNi NaN+NaNi\n", "[1] NaN+Infi NaN+Infi NaN+Infi\n");
        WHITELIST.add("{ as.raw(c(1,4)) | raw() }", "raw(0)\n", "logical(0)\n");
        WHITELIST.add("{ raw() | as.raw(c(1,4))}", "raw(0)\n", "logical(0)\n");
    }
}
