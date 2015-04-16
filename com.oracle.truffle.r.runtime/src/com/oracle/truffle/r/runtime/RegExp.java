/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime;

import java.util.regex.*;

import com.oracle.truffle.api.CompilerDirectives.*;

/**
 * Support methods for regular expressions.
 */
public class RegExp {

    private static enum Predefined {
        alnum("\\p{Alnum}"),
        alpha("\\p{Alpha}"),
        blank("\\p{Blank}"),
        cntrl("\\p{Cntrl}"),
        digit("\\p{Digit}"),
        graph("\\p{Graph}"),
        lower("\\p{Lower}"),
        print("\\p{Print}"),
        punct("\\p{Punct}"),
        space("\\p{Space}"),
        upper("\\p{Upper}"),
        xdigit("\\p{XDigit}");

        private final String replacement;
        private final String syntax;
        private final int syntaxLength;

        private Predefined(String replacement) {
            this.replacement = replacement;
            syntax = "[:" + name() + ":]";
            syntaxLength = syntax.length();
        }
    }

    private static final Predefined[] preDefinedClasses = Predefined.values();

    /**
     * R defines some short forms of character classes. E.g. {@code [[:alnum:]]} means
     * {@code [0-9A-Za-z]} but independent of locale and character encoding. So we have to translate
     * these for use with Java regexp. TODO handle the complete set and do locale and character
     * encoding
     */
    @TruffleBoundary
    public static String checkPreDefinedClasses(String pattern) {
        String result = pattern;
        int xxIndex;
        while ((xxIndex = result.indexOf("[[")) >= 0) {
            boolean predefined = false;
            for (int i = 0; i < preDefinedClasses.length; i++) {
                Predefined preDefined = preDefinedClasses[i];
                int ix = result.indexOf(preDefined.syntax);
                if (ix >= 0) {
                    String replacement = preDefined.replacement;
                    result = result.substring(0, ix) + replacement + result.substring(ix + preDefined.syntaxLength);
                    predefined = true;
                }
            }
            if (!predefined) {
                // it's a freestanding [[, which is legal in R but not in Java.
                result = result.substring(0, xxIndex) + "[\\[" + result.substring(xxIndex + 2);
            }
        }
        // this loop replaces "[[]" (illegal in Java regex) to "[\[]"
        boolean withinCharClass = false;
        int i = 0;
        while (i < result.length()) {
            switch (result.charAt(i)) {
                case '\\':
                    i++;
                    break;
                case '[':
                    if (withinCharClass) {
                        result = result.substring(0, i) + '\\' + result.substring(i);
                        i++;
                    } else {
                        withinCharClass = true;
                    }
                    break;
                case ']':
                    withinCharClass = false;
                    break;
            }
            i++;
        }
        return result;
    }

    public static void main(String[] args) {
        String result = checkPreDefinedClasses(args[0]);
        try {
            Pattern.compile(result);
        } catch (PatternSyntaxException ex) {
            System.console();
        }
    }

}
