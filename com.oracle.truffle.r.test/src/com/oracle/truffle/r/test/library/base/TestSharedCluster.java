/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.test.library.base;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check
public class TestSharedCluster extends TestBase {

    @Before
    public void beforeMethod() {
        org.junit.Assume.assumeTrue(!"llvm".equals(System.getenv().get("FASTR_RFFI")));
    }

    @Test
    @Ignore("Transient hangs")
    public void testSharedCluster() {
        // TODO: debug and unignore
        assertEval(TestBase.template(
                        "library(parallel); fun <- function(data) { cl <- makeCluster(%0, ifelse(exists('engine', where=R.version),'SHARED','PSOCK')); parLapply(cl, data, function(x) x+1); stopCluster(cl) }; fun(1:100)",
                        "123456789".split("")));
    }
}
