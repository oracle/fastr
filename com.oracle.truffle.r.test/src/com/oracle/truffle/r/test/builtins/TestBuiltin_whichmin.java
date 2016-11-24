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
public class TestBuiltin_whichmin extends TestBase {

    @Test
    public void testwhichmin1() {
        assertEval("argv <- list(structure(c(345595, 172795, 69115, 34555, 23035, 11515, 5755, 2875, 1147, 571, 379, 187, 91, 27, 11, 3, 1, 3, 4.42857142857143, 4.73716632443532, 4.86858316221766, 4.95619438740589, 4.97809719370294, 4.98904859685147, 4.99452429842574, 4.99780971937029, 4.99890485968515, 4.99945242984257, 4.99978097193703, 4.99989048596851, 4.99994524298426, 4.9999780971937, 4.99998904859685), .Names = c('1 sec', '2 secs', '5 secs', '10 secs', '15 secs', '30 secs', '1 min', '2 mins', '5 mins', '10 mins', '15 mins', '30 mins', '1 hour', '3 hours', '6 hours', '12 hours', '1 DSTday', '2 DSTdays', '1 week', 'halfmonth', '1 month', '3 months', '6 months', '1 year', '2 years', '5 years', '10 years', '20 years', '50 years', '100 years', '200 years', '500 years', '1000 years'))); .Internal(which.min(argv[[1]]))");
    }

    @Test
    public void testwhichmin2() {
        assertEval("argv <- list(structure(c(295, 145, 55, 25, 15, 5, 0, 2.5, 4, 4.5, 4.66666666666667, 4.83333333333333, 4.91666666666667, 4.97222222222222, 4.98611111111111, 4.99305555555556, 4.99652777777778, 4.99826388888889, 4.99950396825397, 4.99977184576774, 4.99988592288387, 4.99996197429462, 4.99998098714731, 4.99999049357366, 4.99999524678683, 4.99999809871473, 4.99999904935737, 4.99999952467868, 4.99999980987147, 4.99999990493574, 4.99999995246787, 4.99999998098715, 4.99999999049357), .Names = c('1 sec', '2 secs', '5 secs', '10 secs', '15 secs', '30 secs', '1 min', '2 mins', '5 mins', '10 mins', '15 mins', '30 mins', '1 hour', '3 hours', '6 hours', '12 hours', '1 DSTday', '2 DSTdays', '1 week', 'halfmonth', '1 month', '3 months', '6 months', '1 year', '2 years', '5 years', '10 years', '20 years', '50 years', '100 years', '200 years', '500 years', '1000 years'))); .Internal(which.min(argv[[1]]))");
    }

    @Test
    public void testwhichmin3() {
        assertEval("argv <- list(NULL); .Internal(which.min(argv[[1]]))");
    }

    @Test
    public void testwhichmin4() {
        assertEval("argv <- list(list()); .Internal(which.min(argv[[1]]))");
    }

    @Test
    public void testwhichmin5() {
        assertEval("argv <- list(c(NA, 0.951840581382975, 0.805577027554469, 0.663985017923499, 0.53717416750558, 0.496765449963868, 0.472038350505409, 0.463306413812878, 0.485896454097402, 0.520777596351646, 0.524391122960607, 0.492063804965834, 0.513821989320989, 0.521702559081969, 0.533525525673351)); .Internal(which.min(argv[[1]]))");
    }

    @Test
    public void testwhichmin6() {
        assertEval("argv <- list(structure(c(NA, 87, 82, 75, 63, 50, 43, 32, 35, 60, 54, 55, 36, 39, NA, NA, 69, 57, 57, 51, 45, 37, 46, 39, 36, 24, 32, 23, 25, 32, NA, 32, 59, 74, 75, 60, 71, 61, 71, 57, 71, 68, 79, 73, 76, 71, 67, 75, 79, 62, 63, 57, 60, 49, 48, 52, 57, 62, 61, 66, 71, 62, 61, 57, 72, 83, 71, 78, 79, 71, 62, 74, 76, 64, 62, 57, 80, 73, 69, 69, 71, 64, 69, 62, 63, 46, 56, 44, 44, 52, 38, 46, 36, 49, 35, 44, 59, 65, 65, 56, 66, 53, 61, 52, 51, 48, 54, 49, 49, 61, NA, NA, 68, 44, 40, 27, 28, 25, 24, 24), .Tsp = c(1945, 1974.75, 4), class = 'ts')); .Internal(which.min(argv[[1]]))");
    }

    @Test
    public void testwhichmin8() {
        assertEval("argv <- structure(list(x = c(NA, NA, Inf)), .Names = 'x');do.call('which.min', argv)");
    }

    @Test
    public void testWhichMin() {
        assertEval("{ which.min(c(5,5,5,5,5)) }");
        assertEval("{ which.min(c(1,2,3,4,5)) }");
        assertEval("{ which.min(c(2,4))}");
        assertEval("{ which.min(c(2L,4L,3L))}");
        assertEval("{ which.min(c(1,2,3,4,5))}");
        assertEval("{ which.min(c(TRUE, TRUE))}");
        assertEval("{ which.min(c(TRUE, FALSE))}");
        assertEval("{ which.min(c(1:5))}");
        assertEval("{ which.min(c(5:1))}");
        assertEval("{ which.min(c(1:10000))}");
    }
}
