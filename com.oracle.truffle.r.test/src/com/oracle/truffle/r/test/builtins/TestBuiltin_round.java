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

import org.junit.*;

import com.oracle.truffle.r.test.*;

// Checkstyle: stop line length check

public class TestBuiltin_round extends TestBase {

    @Test
    public void testround1() {
        assertEval("argv <- list(3.98778192287757, 3);do.call('round', argv)");
    }

    @Test
    public void testround2() {
        assertEval("argv <- structure(list(c(37.9490090935718, 34.1981894015095),     digits = 3), .Names = c('', 'digits'));do.call('round', argv)");
    }

    @Test
    public void testround3() {
        assertEval("argv <- list(structure(list(lowerNorm = c(1, 0.7074793118252,     0.703783359109958, 0.633667085530785, 0.629171386131588,     0.55900804989023, 0.553693829615336, 0.547917347996141, 0.470383100744677,     0.397621760007547, 0.390548442517381, 0.382779091361949,     0.374191514453686, 0.276654053495554, 0.186268067402784,     0.176381170003996, 0.152703583557352, 0.138281755556403,     0.121518607618675), upperNorm = c(1, 1, 1, 0.979778292620476,     0.984273992019672, 0.946874303050947, 0.952188523325841,     0.957965004945035, 0.910009056118068, 0.857280200776766,     0.864353518266933, 0.872122869422365, 0.880710446330628,     0.798976198605286, 0.710090476014583, 0.719977373413371,     0.743654959860015, 0.758076787860964, 0.774839935798692),     lowerNormS = c(0.910985448809634, 0.683392923012911, 0.679522139376878,         0.614273605024573, 0.609653530675358, 0.543887035370979,         0.538488520130148, 0.532620411085642, 0.459604218176941,         0.390811451215735, 0.383715321807271, 0.375920913978781,         0.367305641565109, 0.274783502246108, 0.188735721130942,         0.178848823732154, 0.15517123728551, 0.140749409284561,         0.123986261346832), upperNorms = c(1, 0.996879185830627,         1, 0.969960088452818, 0.974580162802033, 0.937905681715855,         0.943304196956687, 0.949172306001193, 0.902674026454245,         0.851952320959801, 0.859048450368266, 0.866842858196755,         0.875458130610427, 0.797245309278501, 0.712558129742741,         0.722445027141529, 0.746122613588173, 0.760544441589122,         0.77730758952685)), .Names = c('lowerNorm', 'upperNorm',     'lowerNormS', 'upperNorms'), row.names = c(NA, -19L), class = 'data.frame'),     3);"
                        + "do.call('round', argv)");
    }

    @Test
    public void testRound() {
        assertEval("{ round(0.4) }");
        assertEval("{ round(0.5) }");
        assertEval("{ round(0.6) }");
        assertEval("{ round(1.5) }");
        assertEval("{ typeof(round(1L)) }");
        assertEval("{ round(-1.5) }");
        assertEval("{ round(1L) }");
        assertEval("{ round(1/0) }");
        assertEval("{ round(c(0,0.2,0.4,0.6,0.8,1)) }");
        assertEval("{ round(c(0,0.2,NaN,0.6,NA,1)) }");
        assertEval("{ round(as.complex(c(0,0.2,NaN,0.6,NA,1))) }");

        assertEval(Ignored.Unknown, "{ round(1.123456,digit=2.8) }");
    }
}
