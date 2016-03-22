/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check
public class TestBuiltin_Syssetenv extends TestBase {

    @Test
    public void testSyssetenv1() {
        assertEval(Ignored.Unknown, "argv <- list('_R_NS_LOAD_', 'Matrix'); .Internal(Sys.setenv(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testSyssetenv2() {
        assertEval(Ignored.Unknown, "argv <- list('_R_NS_LOAD_', 'methods'); .Internal(Sys.setenv(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testSyssetenv3() {
        assertEval(Ignored.Unknown,
                        "argv <- list(c('BIBINPUTS', 'add'), c('.:.:/home/lzhao/hg/r-instrumented/share/texmf/bibtex/bib::/home/lzhao/hg/r-instrumented/share/texmf/bibtex/bib:', 'TRUE')); .Internal(Sys.setenv(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testSyssetenv5() {
        assertEval(Ignored.Unknown, "argv <- structure(list(TZ = 'EST5EDT'), .Names = 'TZ');do.call('Sys.setenv', argv)");
    }
}
