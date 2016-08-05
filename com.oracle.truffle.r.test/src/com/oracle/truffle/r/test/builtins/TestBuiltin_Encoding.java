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
public class TestBuiltin_Encoding extends TestBase {

    @Test
    public void testEncoding1() {
        assertEval("argv <- list('Byte Code Compiler'); .Internal(Encoding(argv[[1]]))");
    }

    @Test
    public void testEncoding2() {
        assertEval(Ignored.Unknown,
                        "argv <- list(c('\\n', '\\n', '## These cannot be run by examples() but should be OK when pasted\\n', '## into an interactive R session with the tcltk package loaded\\n', '\\n', 'tt <- tktoplevel()\\n', 'tkpack(txt.w <- tktext(tt))\\n', 'tkinsert(txt.w, \\\'0.0\\\', \\\'plot(1:10)\\\')\\n', '\\n', '# callback function\\n', 'eval.txt <- function()\\n', '   eval(parse(text = tclvalue(tkget(txt.w, \\\'0.0\\\', \\\'end\\\'))))\\n', 'tkpack(but.w <- tkbutton(tt, text = \\\'Submit\\\', command = eval.txt))\\n', '\\n', '## Try pressing the button, edit the text and when finished:\\n', '\\n', 'tkdestroy(tt)\\n', '\\n', '\\n')); .Internal(Encoding(argv[[1]]))");
    }

    @Test
    public void testEncoding3() {
        assertEval(Ignored.Unknown, "argv <- list('detaching ‘package:nlme’, ‘package:splines’'); .Internal(Encoding(argv[[1]]))");
    }

    @Test
    public void testEncoding4() {
        assertEval(Ignored.Unknown, "argv <- list(structure(character(0), class = 'check_code_usage_in_package')); .Internal(Encoding(argv[[1]]))");
    }

    @Test
    public void testEncoding5() {
        assertEval("argv <- list(structure('Type demo(PKG::FOO) to run demonstration PKG::FOO.', .Names = 'demo')); .Internal(Encoding(argv[[1]]))");
    }

    @Test
    public void testEncoding6() {
        assertEval("argv <- list('A shell of class documentation has been written to the file ./myTst2/man/DocLink-class.Rd.\\n'); .Internal(Encoding(argv[[1]]))");
    }

    @Test
    public void testEncoding7() {
        assertEval(Ignored.Unknown,
                        "argv <- list(c('* Edit the help file skeletons in man, possibly combining help files for multiple functions.', '* Edit the exports in NAMESPACE, and add necessary imports.', '* Put any C/C++/Fortran code in src.', '* If you have compiled code, add a useDynLib() directive to NAMESPACE.', '* Run R CMD build to build the package tarball.', '* Run R CMD check to check the package tarball.', '', 'Read Writing R Extensions for more information.')); .Internal(Encoding(argv[[1]]))");
    }

    @Test
    public void testEncoding9() {
        assertEval("argv <- structure(list(x = 'abc'), .Names = 'x');do.call('Encoding', argv)");
    }
}
