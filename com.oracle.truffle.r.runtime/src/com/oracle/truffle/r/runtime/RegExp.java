/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.runtime;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

/**
 * Support methods for regular expressions.
 */
public class RegExp {

    private enum Predefined {
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

        Predefined(String replacement) {
            this.replacement = replacement;
            syntax = "[:" + name() + ":]";
            syntaxLength = syntax.length();
        }
    }

    /**
     * Transforms given pattern into a pattern that can be used by the Java regexp library.
     * 
     * @param pattern Pattern that can be used directly by GNU-R engine.
     * @return Pattern that can be used directly by Java regexp library.
     */
    public static String transformPatternToGnurCompatible(String pattern) {
        String transformedPattern = pattern;
        transformedPattern = checkPreDefinedClasses(transformedPattern);
        transformedPattern = checkSpacesInQuantifiers(transformedPattern);
        return transformedPattern;
    }

    /**
     * R defines some short forms of character classes. E.g. {@code [[:alnum:]]} means
     * {@code [0-9A-Za-z]} but independent of locale and character encoding. So we have to translate
     * these for use with Java regexp. TODO handle the complete set and do locale and character
     * encoding
     */
    @TruffleBoundary
    private static String checkPreDefinedClasses(String pattern) {
        String result = pattern;
        /*
         * this loop replaces "[[]" (illegal in Java regex) with "[\[]", "[\]" with "[\\]" and
         * predefined classes like "[:alpha:]" with "\p{Alpha}".
         */
        boolean withinCharClass = false;
        int parensNesting = 0;
        int i = 0;
        while (i < result.length()) {
            switch (result.charAt(i)) {
                case '(':
                    if (withinCharClass) {
                        result = result.substring(0, i) + '\\' + result.substring(i);
                        i++; // skip the newly inserted '\\'
                    } else {
                        parensNesting++;
                    }
                    break;
                case ')':
                    if (withinCharClass || parensNesting == 0) {
                        result = result.substring(0, i) + '\\' + result.substring(i);
                        i++; // skip the newly inserted '\\'
                    } else {
                        parensNesting--;
                    }
                    break;
                case '\\':
                    if (withinCharClass) {
                        result = result.substring(0, i) + '\\' + result.substring(i);
                        i++; // skip the newly inserted '\\'
                    } else {
                        i++; // skip the next character
                    }
                    break;
                case '[':
                    if (withinCharClass) {
                        boolean predefined = false;
                        if (i + 1 < result.length() && result.charAt(i + 1) == ':') {
                            for (Predefined pre : Predefined.values()) {
                                if (pre.syntax.regionMatches(0, result, i, pre.syntaxLength)) {
                                    result = result.substring(0, i) + pre.replacement + result.substring(i + pre.syntaxLength);
                                    i += pre.replacement.length() - 1;
                                    predefined = true;
                                    break;
                                }
                            }
                        }
                        if (!predefined) {
                            result = result.substring(0, i) + '\\' + result.substring(i);
                            i++;
                        }
                    } else {
                        withinCharClass = true;
                    }
                    break;
                case ']':
                    // Detecting that the current ']' follows the initial '[^' (i.e. excluding
                    // character class)
                    boolean followsCaret = (i == 2 && result.charAt(0) == '[' && result.charAt(1) == '^');
                    // Detecting that the current ']' closes "empty brackets '[]'
                    boolean closingEmptyBrackets = followsCaret || (i > 0 && result.charAt(i - 1) == '[' &&
                                    (i < 2 || result.charAt(i - 2) != '\\'));
                    // To leave a character class open we must already be within some and the
                    // current ']' must be closing empty brackets.
                    // Examples:
                    // ] - there is no character class, so the current ']' has no effect
                    // [\[] - the ']' closes the character class
                    // []\[] - the 1st ']' leaves the character class open, while the 2nd one closes
                    // it
                    // [^]] - the first ']' leaves the character class open
                    withinCharClass &= closingEmptyBrackets;
                    break;
            }
            i++;
        }
        return result;
    }

    /**
     * GNU-R ignores some spaces in quantifiers, e.g., in "{3, }", the space in front of the last
     * bracket is ignored. In PCRE, this is a valid expression that is, however, not interpreted as
     * a quantifier. In Java regexp library, this is not a valid expression.
     * 
     * @param pattern Pattern, potentially with spaces in quantifiers.
     * @return Pattern without spaces in quantifiers.
     */
    @TruffleBoundary
    private static String checkSpacesInQuantifiers(String pattern) {
        boolean escapedOpeningBracket = false;
        int i = 0;
        StringBuilder sb = new StringBuilder();
        while (i < pattern.length()) {
            char currentChar = pattern.charAt(i);
            switch (currentChar) {
                case '{':
                    if (!escapedOpeningBracket) {
                        int idxOfClosingBracket = pattern.indexOf("}", i);
                        if (idxOfClosingBracket == -1) {
                            // This is a pattern syntax error, just forward the pattern as is.
                            return pattern;
                        }
                        // quantifierContent is without the opening bracket.
                        String quantifierContent = pattern.substring(i + 1, idxOfClosingBracket);
                        int idxOfComma = quantifierContent.indexOf(",");
                        if (idxOfComma == -1) {
                            return pattern;
                        }
                        String quantifierContentBeforeComma = quantifierContent.substring(0, idxOfComma);
                        String quantifierContentAfterComma = quantifierContent.substring(idxOfComma + 1);
                        sb.append("{");
                        sb.append(quantifierContentBeforeComma);
                        sb.append(",");
                        if (!quantifierContentAfterComma.isEmpty() && containsOnlyWhiteSpaces(quantifierContentAfterComma)) {
                            sb.append(removeWhiteSpaces(quantifierContentAfterComma));
                        } else {
                            sb.append(quantifierContentAfterComma);
                        }
                        sb.append("}");
                        i += 3 + quantifierContentBeforeComma.length() + quantifierContentAfterComma.length();
                    } else {
                        escapedOpeningBracket = false;
                        sb.append(currentChar);
                        i++;
                    }
                    break;
                case '\\':
                    if (i < pattern.length() - 1 && pattern.charAt(i + 1) == '{') {
                        escapedOpeningBracket = true;
                    }
                    sb.append(currentChar);
                    i++;
                    break;
                default:
                    sb.append(currentChar);
                    i++;
                    break;
            }
        }
        return sb.toString();
    }

    private static boolean containsOnlyWhiteSpaces(String string) {
        for (int i = 0; i < string.length(); i++) {
            if (!isWhiteSpace(string.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static int indexOfWhiteSpace(String string) {
        for (int i = 0; i < string.length(); i++) {
            if (isWhiteSpace(string.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    private static boolean isWhiteSpace(char c) {
        return c == ' ';
    }

    private static String removeWhiteSpaces(String string) {
        int idxOfWhiteSpace = indexOfWhiteSpace(string);
        assert idxOfWhiteSpace != -1;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < string.length(); i++) {
            if (!isWhiteSpace(string.charAt(i))) {
                sb.append(string.charAt(i));
            }
        }
        return sb.toString();
    }
}
