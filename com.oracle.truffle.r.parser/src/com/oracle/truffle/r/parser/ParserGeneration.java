/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.parser;

import com.oracle.truffle.r.parser.processor.GenerateRParser;

/**
 * This class exists simply to cause the annotation processor that creates the R parser/lexer files
 * to execute.
 *
 * Note that simply changing the grammar file, R.g, will not cause regeneration, but editing this
 * file will. The suggestion is to add a line to {@link #CHANGE_HISTORY}.
 *
 */
@GenerateRParser
@SuppressWarnings("unused")
public class ParserGeneration {

    // @formatter:off
    private static final String[] CHANGE_HISTORY = new String[]{
        "Initial version",
        "for vector updates, make sure vectors are copied from enclosing frame if they are not found in current frame",
        "grammar source formatting",
        "source attribution prototype",
        "recognise negative numbers as single elements",
        "remove unused handling of --EOF--",
        "source attribution in parser AST",
        "avoid inclusion of brackets around expressions when generating source attribution",
        "source attribution for while loops includes opening curly brace or body expression",
        "support :: and ::: at parser level",
        "support NA_integer_",
        "allow string literals as argument names",
        "treat arguments as nodes and argument lists as generic lists",
        "more verbose debug output for source attribution",
        "adopt changed SourceSection creation API",
        "support NA_real_",
        "support NA_character_",
        "basic support for formula parsing",
        "improved error handling in parser",
        "clean up ... handling in parser",
        "dedicated AST nodes for ..N",
        "support strings in field accesses",
        "allow NULL= in switch",
        "support NA_complex_",
        "simplified unary and binary operations",
        "allow unary ! in normal expressions",
        "added \\a, \\v and \\` escape sequences",
        "added octal escape sequences for strings",
        "handles escapes in `xxx` form",
        "added \\  escape sequence",
        "allow -/+/! after ^",
        "adapt to RError API change",
        "allow multiple semicolons in {}",
        "allow .. as identifier",
        "rename Operator class",
        "remove FieldAccess, small refactorings",
        "allow backslash at line end",
        "maintain proper operator source sections",
        "remove special handling for formulas",
        "remove source section identifiers",
        "transform parser to a generic class via the annotation processor",
        "use RComplex.createNA()",
        "inlined ParseUtils"
    };
}
