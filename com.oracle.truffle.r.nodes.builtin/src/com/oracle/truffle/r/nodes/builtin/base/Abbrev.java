/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995, 1996  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1997-2014,  The R Core Team
 * Copyright (c) 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.*;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RAttributeProfiles;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

@RBuiltin(name = "abbreviate", kind = INTERNAL, parameterNames = {"x", "minlength", "use.classes"}, behavior = PURE)
public abstract class Abbrev extends RBuiltinNode {
    private final NACheck naCheck = NACheck.create();
    private final RAttributeProfiles attrProfiles = RAttributeProfiles.create();

    @Override
    public void createCasts(CastBuilder casts) {
        casts.arg("x").mustBe(stringValue());
        casts.arg("minlength").asIntegerVector().findFirst().notNA();
        casts.arg("use.classes").asLogicalVector().findFirst().notNA().map(toBoolean());
    }

    @Specialization
    protected RStringVector abbrev(RAbstractStringVector x, int minlength, boolean useClasses) {
        int len = x.getLength();
        String[] data = new String[len];
        naCheck.enable(true);
        for (int i = 0; i < len; i++) {
            String el = x.getDataAt(i);
            if (naCheck.check(el)) {
                data[i] = el;
            } else {
                if (el.length() > minlength) {
                    data[i] = stripChars(el, minlength, useClasses);
                } else {
                    data[i] = el;
                }
            }
        }
        RStringVector result = RDataFactory.createStringVector(data, naCheck.neverSeenNA());
        result.copyAttributesFrom(attrProfiles, x);
        return result;
    }

    private static boolean firstChar(StringBuilder s, int i) {
        return Character.isSpaceChar(s.charAt(i - 1));
    }

    private static boolean lastChar(StringBuilder s, int i) {
        return !Character.isSpaceChar(s.charAt(i - 1)) && (i + 1 >= s.length() || Character.isSpaceChar(s.charAt(i + 1)));
    }

    private static boolean lcVowel(StringBuilder s, int i) {
        char ch = s.charAt(i);
        return ch == 'a' || ch == 'e' || ch == 'i' || ch == 'o' || ch == 'u';
    }

    private static void shiftDown(StringBuilder s, int i) {
        int len = s.length();
        for (int j = i; j < len - 1; j++) {
            s.setCharAt(j, s.charAt(j + 1));
        }
        s.deleteCharAt(len - 1);
    }

    private static String stripChars(String inchar, int minlen, boolean usecl) {
        StringBuilder s = new StringBuilder(inchar.trim());
        int len = s.length();
        int i;
        int nspace = 0;

        if (len >= minlen) {
            // This "loop" exists solely to allow a "break" out of the subsequent logic
            donesc: while (true) {
                /* The for() loops never touch the first character */

                /* record spaces for removal later (as they act as word boundaries) */
                for (i = s.length() - 1; i > 0; i--) {
                    if (Character.isSpaceChar(s.charAt(i))) {
                        nspace++;
                    }
                    if (s.length() - nspace <= minlen) {
                        break donesc;
                    }
                }

                if (usecl) {
                    /*
                     * remove l/case vowels, which are not at the beginning of a word but are at the
                     * end
                     */
                    for (i = s.length() - 1; i > 0; i--) {
                        if (lcVowel(s, i) && lastChar(s, i)) {
                            shiftDown(s, i);
                        }
                        if (s.length() - nspace <= minlen) {
                            break donesc;
                        }
                    }

                    /* remove those not at the beginning of a word */
                    for (i = s.length() - 1; i > 0; i--) {
                        if (lcVowel(s, i) && !firstChar(s, i)) {
                            shiftDown(s, i);
                        }
                        if (s.length() - nspace <= minlen) {
                            break donesc;
                        }
                    }

                    /* Now do the same for remaining l/case chars */
                    for (i = s.length() - 1; i > 0; i--) {
                        if (Character.isLowerCase(s.charAt(i)) && lastChar(s, i)) {
                            shiftDown(s, i);
                        }
                        if (s.length() - nspace <= minlen) {
                            break donesc;
                        }
                    }

                    for (i = s.length() - 1; i > 0; i--) {
                        if (Character.isLowerCase(s.charAt(i)) && !firstChar(s, i)) {
                            shiftDown(s, i);
                        }
                        if (s.length() - nspace <= minlen) {
                            break donesc;
                        }
                    }
                }

                /* all else has failed so we use brute force */

                for (i = s.length() - 1; i > 0; i--) {
                    if (!firstChar(s, i) && !Character.isSpaceChar(s.charAt(i))) {
                        shiftDown(s, i);
                    }
                    if (s.length() - nspace <= minlen) {
                        break;
                    }
                }
            }
        }

        // remove internal spaces as required
        int upper = s.length();
        if (upper > minlen) {
            for (i = upper - 1; i > 0; i--) {
                if (Character.isSpaceChar(s.charAt(i))) {
                    shiftDown(s, i);
                }
            }
        }
        return s.toString();
    }

}
