/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.test.library.stats;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

/**
 * Integration test for formula supporting functions. There are R wrappers and native C functions
 * that these invoke through {@code .External}. FastR implements these externals as ordinary R
 * functions and routes selected {@code .External} invocations to them. Here we invoke the wrappers.
 * See file modelTests.R in the same directory for pure R tests testing only the FastR R code
 * without the R wrappers from GnuR. When adding new test cases here, consider adding them to
 * modelTests.R too.
 */
public class TestFormulae extends TestBase {

    /** Creates R code that initializes all the variables used in formulae examples. */
    private static String getVarsInit() {
        char[] vars = new char[]{'y', 'z', 'k', 'w', 'm', 'u', 'v'};
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < vars.length; ++i) {
            result.append(vars[i]).append("<-").append(i).append(':').append(i + 9).append(';');
        }

        return result.toString();
    }

    private static final String INIT_VARIABLES = getVarsInit();

    // the same as INIT_VARIABLES, but variables k and z will be factors
    private static final String INIT_VARIABLES_FACTORS = INIT_VARIABLES +
                    "k <- factor(rep(c('m', 'f'), 5));" +
                    "z <- factor(c(rep(c('a', 'b', 'c'), 3), 'c')); ";

    private static final String[] INITS = new String[]{INIT_VARIABLES, INIT_VARIABLES_FACTORS};

    private static final String[] FORMULAE = new String[]{
                    "y~z", "y~1+z", "y~0+z", "y~-1+z", "y~z*k", "y~z*k+w*m", "u~z*k+w*m",
                    "y~z:k", "y~z^2", "y~(z+k)^2", "y~z*((m+w)^3)",
                    "y~(z+k)*(w+u)", "y~w%in%v", "y~w/k", "y~(1 + w/k)"
    };

    /**
     * R code to show attributes stored in 'attrs' variable in a predictable way. The problem is
     * that string sorting is somewhat unstable, giving different order for c('xxx','.xxx') on
     * different platforms. So we strip '.Environment' attribute and sort the rest.
     */
    private static final String SHOW_ATTRS = "envIdx <- which(names(attrs)=='.Environment'); print(attrs[envIdx]); attrs[sort(names(attrs[-envIdx]))]";

    @Test
    public void testTermsform() {
        // Note: terms.formula does not evaluate the variables
        assertEval(template("f <- terms.formula(%0); attrs <- attributes(f); " + SHOW_ATTRS, FORMULAE));
    }

    @Test
    public void testModelFrame() {
        assertEval(template("{%0; model.frame(terms.formula(%1)) }", INITS, FORMULAE));
    }

    @Test
    public void testModelMatrix() {
        assertEval(template("{%0; model.matrix(model.frame(terms.formula(%1))) }", INITS, FORMULAE));
    }

    @Test
    public void testSubsettingModelframe() {
        assertEval("{x<-y<-1:10; model.frame.default(x~y, subset=3:7); }");
    }

    @Test
    public void testSpecialsTermsform() {
        assertEval("f <- terms.formula(y~myfun(z)+x, c('myfun')); attrs <- attributes(f); " + SHOW_ATTRS);
    }

    @Test
    public void testExpandDostsTermsform() {
        assertEval("f <- terms.formula(cyl~hp*mpg+., data=mtcars); attrs <- attributes(f);" + SHOW_ATTRS);
    }

    @Test
    public void testExpandDostsAndSpecialsTermsform() {
        assertEval("f <- terms.formula(cyl~myfun(mpg)+., specials=c('myfun'), data=mtcars); attrs <- attributes(f); " + SHOW_ATTRS);
    }
}
