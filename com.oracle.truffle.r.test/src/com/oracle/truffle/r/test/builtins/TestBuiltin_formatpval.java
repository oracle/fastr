/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

public class TestBuiltin_formatpval extends TestBase {

    @Test
    public void testformatpval1() {
        assertEval("argv <- structure(list(pv = 0.200965994008331, digits = 3), .Names = c('pv',     'digits'));do.call('format.pval', argv)");
    }
}
