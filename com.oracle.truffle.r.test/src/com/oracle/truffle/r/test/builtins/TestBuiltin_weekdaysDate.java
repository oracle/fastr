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
import java.util.Locale;

// Checkstyle: stop line length check

public class TestBuiltin_weekdaysDate extends TestBase {

    @Test
    public void testweekdaysDate1() {
        // Explicit locale setting is a workaround to run this test on JDK9.
        // Otherwise the test was returning "Thu" instead of "Thursday"
        // due to JDK bug 8130845.
        Locale.setDefault(Locale.ENGLISH);

        assertEval("argv <- structure(list(x = structure(16352, class = 'Date')),     .Names = 'x');do.call('weekdays.Date', argv)");
    }
}
