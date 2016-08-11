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
public class TestBuiltin_toupper extends TestBase {

    @Test
    public void testtoupper1() {
        assertEval("argv <- list('UTF-8'); .Internal(toupper(argv[[1]]))");
    }

    @Test
    public void testtoupper2() {
        assertEval("argv <- list(c('', '', 'remission', '', '', '', '', '', '')); .Internal(toupper(argv[[1]]))");
    }

    @Test
    public void testtoupper3() {
        assertEval("argv <- list(character(0)); .Internal(toupper(argv[[1]]))");
    }

    @Test
    public void testtoupper4() {
        assertEval("argv <- list(structure(c('BasicClasses', 'Classes', 'Documentation', 'environment-class', 'GenericFunctions', 'language-class', 'LinearMethodsList-class', 'MethodDefinition-class', 'MethodWithNext-class', 'Methods', 'MethodsList-class', 'callNextMethod', 'ObjectsWithPackage-class', 'S3Part', 'S4groupGeneric', 'SClassExtension-class', 'StructureClasses', 'TraceClasses', 'as', 'callGeneric', 'canCoerce', 'cbind2', 'className', 'classRepresentation-class', 'classesToAM', 'dotsMethods', 'evalSource', 'findClass', 'findMethods', 'fixPre1.8', 'genericFunction-class', 'getClass', 'getMethod', 'getPackageName', 'hasArg', 'implicitGeneric', 'inheritedSlotNames', 'initialize-methods', 'is', 'isSealedMethod', 'LocalReferenceClasses', 'method.skeleton', 'new', 'nonStructure-class', 'promptClass', 'promptMethods', 'ReferenceClasses', 'representation', 'selectSuperClasses', 'setClass', 'setClassUnion', 'setGeneric', 'setLoadActions', 'setMethod', 'setOldClass', 'makeClassRepresentation', 'show', 'showMethods', 'signature-class', 'slot', 'envRefClass-class', 'testInheritedMethods', 'validObject', '.BasicFunsList'), .Names = c('/home/lzhao/tmp/RtmpZy1R7l/ltx5573594f0cc9/BasicClasses.tex', '/home/lzhao/tmp/RtmpZy1R7l/ltx5573594f0cc9/Classes.tex', '/home/lzhao/tmp/RtmpZy1R7l/ltx5573594f0cc9/Documentation.tex', '/home/lzhao/tmp/RtmpZy1R7l/ltx5573594f0cc9/EnvironmentClass.tex', '/home/lzhao/tmp/RtmpZy1R7l/ltx5573594f0cc9/GenericFunctions.tex', '/home/lzhao/tmp/RtmpZy1R7l/ltx5573594f0cc9/LanguageClasses.tex', '/home/lzhao/tmp/RtmpZy1R7l/ltx5573594f0cc9/LinearMethodsList-class.tex', '/home/lzhao/tmp/RtmpZy1R7l/ltx5573594f0cc9/MethodDefinition-class.tex', '/home/lzhao/tmp/RtmpZy1R7l/ltx5573594f0cc9/MethodWithNext-class.tex', '/home/lzhao/tmp/RtmpZy1R7l/ltx5573594f0cc9/Methods.tex', '/home/lzhao/tmp/RtmpZy1R7l/ltx5573594f0cc9/MethodsList-class.tex', '/home/lzhao/tmp/RtmpZy1R7l/ltx5573594f0cc9/NextMethod.tex', '/home/lzhao/tmp/RtmpZy1R7l/ltx5573594f0cc9/ObjectsWithPackage-class.tex', '/home/lzhao/tmp/RtmpZy1R7l/ltx5573594f0cc9/S3Part.tex', '/home/lzhao/tmp/RtmpZy1R7l/ltx5573594f0cc9/S4groupGeneric.tex', '/home/lzhao/tmp/RtmpZy1R7l/ltx5573594f0cc9/SClassExtension-class.tex', '/home/lzhao/tmp/RtmpZy1R7l/ltx5573594f0cc9/StructureClasses.tex', '/home/lzhao/tmp/RtmpZy1R7l/ltx5573594f0cc9/TraceClasses.tex', '/home/lzhao/tmp/RtmpZy1R7l/ltx5573594f0cc9/as.tex', '/home/lzhao/tmp/RtmpZy1R7l/ltx5573594f0cc9/callGeneric.tex', '/home/lzhao/tmp/RtmpZy1R7l/ltx5573594f0cc9/canCoerce.tex', '/home/lzhao/tmp/RtmpZy1R7l/ltx5573594f0cc9/cbind2.tex', '/home/lzhao/tmp/RtmpZy1R7l/ltx5573594f0cc9/className.tex', '/home/lzhao/tmp/RtmpZy1R7l/ltx5573594f0cc9/classRepresentation-class.tex', '/home/lzhao/tmp/RtmpZy1R7l/ltx5573594f0cc9/classesToAM.tex', '/home/lzhao/tmp/RtmpZy1R7l/ltx5573594f0cc9/dotsMethods.tex', '/home/lzhao/tmp/RtmpZy1R7l/ltx5573594f0cc9/evalSource.tex', '/home/lzhao/tmp/RtmpZy1R7l/ltx5573594f0cc9/findClass.tex', '/home/lzhao/tmp/RtmpZy1R7l/ltx5573594f0cc9/findMethods.tex', '/home/lzhao/tmp/RtmpZy1R7l/ltx5573594f0cc9/fixPrevious.tex', '/home/lzhao/tmp/RtmpZy1R7l/ltx5573594f0cc9/genericFunction-class.tex', '/home/lzhao/tmp/RtmpZy1R7l/ltx5573594f0cc9/getClass.tex', '/home/lzhao/tmp/RtmpZy1R7l/ltx5573594f0cc9/getMethod.tex', '/home/lzhao/tmp/RtmpZy1R7l/ltx5573594f0cc9/getPackageName.tex', '/home/lzhao/tmp/RtmpZy1R7l/ltx5573594f0cc9/hasArg.tex', '/home/lzhao/tmp/RtmpZy1R7l/ltx5573594f0cc9/implicitGeneric.tex', '/home/lzhao/tmp/RtmpZy1R7l/ltx5573594f0cc9/inheritedSlotNames.tex', '/home/lzhao/tmp/RtmpZy1R7l/ltx5573594f0cc9/initialize-methods.tex', '/home/lzhao/tmp/RtmpZy1R7l/ltx5573594f0cc9/is.tex', '/home/lzhao/tmp/RtmpZy1R7l/ltx5573594f0cc9/isSealedMethod.tex', '/home/lzhao/tmp/RtmpZy1R7l/ltx5573594f0cc9/localRefClass.tex', '/home/lzhao/tmp/RtmpZy1R7l/ltx5573594f0cc9/method.skeleton.tex', '/home/lzhao/tmp/RtmpZy1R7l/ltx5573594f0cc9/new.tex', '/home/lzhao/tmp/RtmpZy1R7l/ltx5573594f0cc9/nonStructure-class.tex', '/home/lzhao/tmp/RtmpZy1R7l/ltx5573594f0cc9/promptClass.tex', '/home/lzhao/tmp/RtmpZy1R7l/ltx5573594f0cc9/promptMethods.tex', '/home/lzhao/tmp/RtmpZy1R7l/ltx5573594f0cc9/refClass.tex', '/home/lzhao/tmp/RtmpZy1R7l/ltx5573594f0cc9/representation.tex', '/home/lzhao/tmp/RtmpZy1R7l/ltx5573594f0cc9/selectSuperClasses.tex', '/home/lzhao/tmp/RtmpZy1R7l/ltx5573594f0cc9/setClass.tex', '/home/lzhao/tmp/RtmpZy1R7l/ltx5573594f0cc9/setClassUnion.tex', '/home/lzhao/tmp/RtmpZy1R7l/ltx5573594f0cc9/setGeneric.tex', '/home/lzhao/tmp/RtmpZy1R7l/ltx5573594f0cc9/setLoadActions.tex', '/home/lzhao/tmp/RtmpZy1R7l/ltx5573594f0cc9/setMethod.tex', '/home/lzhao/tmp/RtmpZy1R7l/ltx5573594f0cc9/setOldClass.tex', '/home/lzhao/tmp/RtmpZy1R7l/ltx5573594f0cc9/setSClass.tex', '/home/lzhao/tmp/RtmpZy1R7l/ltx5573594f0cc9/show.tex', '/home/lzhao/tmp/RtmpZy1R7l/ltx5573594f0cc9/showMethods.tex', '/home/lzhao/tmp/RtmpZy1R7l/ltx5573594f0cc9/signature-class.tex', '/home/lzhao/tmp/RtmpZy1R7l/ltx5573594f0cc9/slot.tex', '/home/lzhao/tmp/RtmpZy1R7l/ltx5573594f0cc9/stdRefClass.tex', '/home/lzhao/tmp/RtmpZy1R7l/ltx5573594f0cc9/testInheritedMethods.tex', '/home/lzhao/tmp/RtmpZy1R7l/ltx5573594f0cc9/validObject.tex', '/home/lzhao/tmp/RtmpZy1R7l/ltx5573594f0cc9/zBasicFunsList.tex'))); .Internal(toupper(argv[[1]]))");
    }

    @Test
    public void testtoupper6() {
        assertEval("argv <- structure(list(x = c('na', NA, 'banana')), .Names = 'x');do.call('toupper', argv)");
    }

    @Test
    public void testCharUtils() {
        assertEval("{ toupper(c(\"hello\",\"bye\")) }");
        assertEval("{ toupper(c()) }");
        assertEval("{ toupper(NA) }");

        assertEval(Ignored.OutputFormatting, "{ toupper(1E100) }");
        assertEval("{ m <- matrix(\"hi\") ; toupper(m) }");
        assertEval("{ toupper(c(a=\"hi\", \"hello\")) }");
    }
}
