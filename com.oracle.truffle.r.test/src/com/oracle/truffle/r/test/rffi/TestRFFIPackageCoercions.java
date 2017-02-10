/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.test.rffi;

import static com.oracle.truffle.r.runtime.gnur.SEXPTYPE.CPLXSXP;
import static com.oracle.truffle.r.runtime.gnur.SEXPTYPE.EXPRSXP;
import static com.oracle.truffle.r.runtime.gnur.SEXPTYPE.INTSXP;
import static com.oracle.truffle.r.runtime.gnur.SEXPTYPE.LGLSXP;
import static com.oracle.truffle.r.runtime.gnur.SEXPTYPE.NILSXP;
import static com.oracle.truffle.r.runtime.gnur.SEXPTYPE.RAWSXP;
import static com.oracle.truffle.r.runtime.gnur.SEXPTYPE.REALSXP;
import static com.oracle.truffle.r.runtime.gnur.SEXPTYPE.STRSXP;
import static com.oracle.truffle.r.runtime.gnur.SEXPTYPE.SYMSXP;
import static com.oracle.truffle.r.runtime.gnur.SEXPTYPE.VECSXP;

import java.util.Arrays;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.oracle.truffle.r.runtime.gnur.SEXPTYPE;
import com.oracle.truffle.r.test.rpackages.TestRPackages;

public class TestRFFIPackageCoercions extends TestRPackages {

    private static final String[] TEST_PACKAGES = new String[]{"testrffi"};

    @BeforeClass
    public static void setupInstallMyTestPackages() {
        setupInstallTestPackages(TEST_PACKAGES);
    }

    @AfterClass
    public static void tearDownUninstallMyTestPackages() {
        tearDownUninstallTestPackages();
    }

    private String addLib(String test) {
        return "{ library('testrffi', lib.loc = '" + TestRPackages.libLoc() + "'); x <- " + test + "; detach('package:testrffi', unload=T); x }";
    }

    private static final String[] COERCION_VALUES_FOR_EXPR = new String[]{
                    "2", "2.2", "T", "integer()", "numeric()", "logical()", "character()", "c(5,6)", "c(2.3, 3.4)", "c(T, F)",
                    "list()", "structure(2.2, names='b',dim=c(1,1),myattr='q')", "structure(T, names='c',dim=c(1,1),myattr='q')"};

    private static final String[] COERCION_VALUES = new String[]{
                    "1L", "2", "2.2", "T", "integer()", "numeric()", "logical()", "character()", "c(5,6)", "c(2.3, 3.4)",
                    "c(T, F)", "list()", "structure(1L,names='a',dim=c(1,1),myattr='q')", "structure(2.2, names='b',dim=c(1,1),myattr='q')",
                    "structure(T, names='c',dim=c(1,1),myattr='q')", "structure(list(1,'42'), names=c('q','w'),dim=c(2,1),myattr='q')"};

    private static final SEXPTYPE[] COERCION_TYPES = new SEXPTYPE[]{SYMSXP, NILSXP, VECSXP, INTSXP, REALSXP, LGLSXP, STRSXP, CPLXSXP, RAWSXP};

    private static final String[] COERCION_MODES = Arrays.stream(COERCION_TYPES).map(x -> Integer.toString(x.code)).toArray(n -> new String[n]);

    @Test
    public void testCoerceVector() {
        String[] tests = template(addLib("rffi.coerceVector(%0, %1)"), COERCION_VALUES, COERCION_MODES);
        assertEval(Output.MayIgnoreWarningContext, Output.MayIgnoreErrorContext, tests);
    }

    @Test
    public void testCoerceVectorToExpression() {
        // Note: inconsistency when printing expression(1L) FastR prints just "expression(1)"
        String[] tests = template(addLib("rffi.coerceVector(%0, %1)"), COERCION_VALUES_FOR_EXPR, new String[]{Integer.toString(EXPRSXP.code)});
        assertEval(Output.IgnoreErrorMessage, Output.MayIgnoreWarningContext, Output.MayIgnoreErrorContext, tests);
        // removes the attributes when its single value, but keeps them when it's a list
        assertEval(Ignored.Unimplemented, addLib("rffi.coerceVector(structure(list(1,'x'), names=c('q','w'),dim=c(2,1),myattr='q'), " + EXPRSXP.code + ")"));
    }
}
