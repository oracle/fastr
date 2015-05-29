/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.*;

import com.oracle.truffle.r.test.*;

// Checkstyle: stop line length check
public class TestBuiltin_tanh extends TestBase {

    @Test
    public void testtanh1() {
        assertEval(Ignored.Unknown, "argv <- list(c(0.57459950307683, 1.3311607364495));tanh(argv[[1]]);");
    }

    @Test
    public void testtanh2() {
        assertEval(Ignored.Unknown, "argv <- list(FALSE);tanh(argv[[1]]);");
    }

    @Test
    public void testtanh3() {
        assertEval(Ignored.Unknown,
                        "argv <- list(c(0.018063120710024, 0.202531388051386, 0.417573408622862, 1.63052300091743, 2.60085453772445, 2.75283670267494, 2.30083138197613, 1.47188976409943, 0.829803307993584, 0.295089115172324, 0.237719196109985, 0.617898787321681, 0.850777050382226, 0.516973890969527, 0.522699166681335, 0.850446724158497, 0.645479182912265, 0.193978409371909, 0.414456893353747, 0.492772947140595, 0.420563171733189, 0.369166401583374, 0.592867562934369, 1.21638206559229, 0.54564621330955, 0.672292186547141, 0.557193544541334, 0.112218530051911, -0.0391766542932368, 0.246991917518619, -0.0310729286667355, 0.100305401934259, 0.385595467685569, 0.347899688300561, 0.0900835492886662, -0.128526864819991));tanh(argv[[1]]);");
    }
}
