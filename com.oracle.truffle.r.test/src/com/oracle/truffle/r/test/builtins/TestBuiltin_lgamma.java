/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2012-2014, Purdue University
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check
public class TestBuiltin_lgamma extends TestBase {

    @Test
    public void testlgamma1() {
        assertEval(Ignored.Unknown, "argv <- list(structure(c(2, 1, 1, 1, 3, 1, 1, 1, 4), .Dim = c(3L, 3L)));lgamma(argv[[1]]);");
    }

    @Test
    public void testlgamma2() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(c(2, 3, 2, 1, 4, 4, 7, 2, 11, 11, 15, 10, 7, 8, 13, 12), .Dim = c(4L, 4L), .Dimnames = structure(list(income = c('< 15k', '15-25k', '25-40k', '> 40k'), satisfaction = c('VeryD', 'LittleD', 'ModerateS', 'VeryS')), .Names = c('income', 'satisfaction'))));lgamma(argv[[1]]);");
    }

    @Test
    public void testlgamma3() {
        assertEval("argv <- list(1.72926007700446);lgamma(argv[[1]]);");
    }

    @Test
    public void testlgamma4() {
        assertEval("argv <- list(0.999935539560166);lgamma(argv[[1]]);");
    }

    @Test
    public void testlgamma5() {
        assertEval("argv <- list(FALSE);lgamma(argv[[1]]);");
    }

    @Test
    public void testlgamma6() {
        assertEval("argv <- list(numeric(0));lgamma(argv[[1]]);");
    }

    @Test
    public void testlgamma7() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(c(11.4065045686104, 9.40650456861037, 8.40650456861037, 11.4065045686104, 9.40650456861037, 6.40650456861036, 15.4065045686104, 9.40650456861037, 9.40650456861037, 8.40650456861037, 6.40650456861036, 7.40650456861036, 8.40650456861037, 7.40650456861036, 9.40650456861037, 13.4065045686104, 10.4065045686104, 11.4065045686104, 14.4065045686104, 10.4065045686104, 16.4065045686104, 10.4065045686104, 7.40650456861036, 9.40650456861037, 7.40650456861036, 13.4065045686104, 17.4065045686104, 4.40650456861036, 10.4065045686104, 5.40650456861036, 6.40650456861036, 4.40650456861036, 5.40650456861036, 4.40650456861036, 4.40650456861036, 8.40650456861037, 9.40650456861037, 5.40650456861036, 9.40650456861037, 7.40650456861036, 7.40650456861036, 8.40650456861037), .Names = c('1', '2', '3', '4', '5', '6', '7', '8', '9', '10', '11', '12', '13', '14', '15', '16', '17', '18', '19', '20', '21', '22', '23', '24', '25', '26', '27', '28', '29', '30', '31', '32', '33', '34', '35', '36', '37', '38', '39', '40', '41', '42')));lgamma(argv[[1]]);");
    }

    @Test
    public void testlgamma9() {
        assertEval(Output.IgnoreWarningContext, "argv <- list(Inf);do.call('lgamma', argv)");
    }

    @Test
    public void testLgamma() {
        assertEval("{ lgamma(1) }");
        assertEval("{ lgamma(100) }");
        assertEval("{ lgamma(7.42) }");
        assertEval("{ lgamma(as.double(NA)) }");
        assertEval("{ lgamma(c(100, 2.2)) }");
        assertEval("{ lgamma(FALSE) }");
        assertEval("{ lgamma(as.raw(1)) }");
        assertEval(Output.IgnoreErrorContext, "{ lgamma(1+1i) }");
    }
}
