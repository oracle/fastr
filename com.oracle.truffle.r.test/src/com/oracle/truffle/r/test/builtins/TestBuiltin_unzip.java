/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

public class TestBuiltin_unzip extends TestBase {

    @Test
    public void testunzip() {
        // writes out a small dummy zip file
        assertEval("n <- tempfile(); writeBin(con=n,c(67324752L, 10L, 1851785216L, -273200397L, 99292L, 65536L, 262144L, \n" +
                        "1868955676L, 1414869359L, -1560084471L, -1386647737L, 1968795463L, \n" +
                        "16780152L, 128260L, 1311744L, 1345388544L, 503447883L, 2563L, \n" +
                        "1610612736L, -1219824786L, 25418991L, 16777216L, 67108864L, 6144L, \n" +
                        "16777216L, -1543503872L, 129L, 1869571584L, 89412913L, 1201865472L, \n" +
                        "2020956527L, 67174411L, 501L, 5124L, 88821760L, 6L, 16777472L, \n" +
                        "18944L, 16128L))\n" +
                        "length(unzip(n,list=T))\n" +
                        "names(unzip(n,list=T))\n" +
                        "unzip(n,list=T)[1:2]\n" + // leave out date (depends on time zone)
                        "target <- tempdir()\n" +
                        "v <- unzip(n,exdir=target, files=c('bar','baz'))\n" +
                        "v\n" +
                        "file.exists(paste0(target, '/foo1'))\n" +
                        "v <- unzip(n,exdir=target)\n" +
                        "length(v)\n" +
                        "file.exists(v)\n" +
                        "readBin(paste0(target, '/foo1'), what='raw', n=1000)");
    }
}
