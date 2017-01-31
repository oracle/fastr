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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.notLogicalNA;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

@RBuiltin(name = "make.names", kind = INTERNAL, parameterNames = {"names", "allow_"}, behavior = PURE)
public abstract class MakeNames extends RBuiltinNode {

    private final ConditionProfile namesLengthZero = ConditionProfile.createBinaryProfile();
    private final NACheck dummyCheck = NACheck.create(); // never triggered (used for vector update)

    static {
        Casts casts = new Casts(MakeNames.class);
        casts.arg("names").mustBe(stringValue(), RError.SHOW_CALLER, RError.Message.NON_CHARACTER_NAMES);
        casts.arg("allow_").defaultError(RError.SHOW_CALLER, RError.Message.INVALID_VALUE, "allow_").asLogicalVector().findFirst().mustBe(notLogicalNA());
    }

    @TruffleBoundary
    private static String concat(String s1, String s2) {
        return s1 + s2;
    }

    private static String getKeyword(String s, boolean allowUnderscore) {
        switch (s) {
            case "if":
                return "if.";
            case "else":
                return "else.";
            case "repeat":
                return "repeat.";
            case "while":
                return "while.";
            case "function":
                return "function.";
            case "for":
                return "for.";
            case "in":
                return "in.";
            case "next":
                return "next.";
            case "break":
                return "break.";
            case "TRUE":
                return "TRUE.";
            case "FALSE":
                return "FALSE.";
            case "NULL":
                return "NULL.";
            case "Inf":
                return "Inf.";
            case "NaN":
                return "NaN.";
            case "NA":
                return "NA.";
            case "NA_integer_":
                return allowUnderscore ? "NA_integer_." : "NA.integer.";
            case "NA_real_":
                return allowUnderscore ? "NA_real_." : "NA.real.";
            case "NA_complex_":
                return allowUnderscore ? "NA_complex_." : "NA.complex.";
            case "NA_character_":
                return allowUnderscore ? "NA_character_." : "NA.character.";
            default:
                return null;
        }
    }

    private static RStringVector getNewNames(RAbstractStringVector names, RStringVector newNames) {
        RStringVector ret = newNames;
        if (ret == null) {
            ret = names.materialize();
            if (ret.isShared()) {
                ret = (RStringVector) ret.copy();
            }
        }
        return ret;
    }

    private static char[] getNameArray(String name, char[] nameArray) {
        char[] newNameArray = nameArray;
        if (newNameArray == null) {
            newNameArray = name.toCharArray();
        }
        return newNameArray;
    }

    private static String getName(String name, boolean allowUnderscore) {
        if (name.length() == 0) {
            return "X";
        }
        String newName = name;
        char[] nameArray = null;
        // if necessary, replace invalid characters with .
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (!Utils.isIsoLatinDigit(c) && !Character.isLetter(c) && (!allowUnderscore || c != '_')) {
                nameArray = getNameArray(name, nameArray);
                nameArray[i] = '.';
            }
        }
        if (nameArray != null) {
            newName = new String(nameArray);
        }
        if (Utils.isIsoLatinDigit(newName.charAt(0)) || (newName.length() > 1 && newName.charAt(0) == '.' && Utils.isIsoLatinDigit(newName.charAt(1))) ||
                        (newName.charAt(0) == '.' && name.charAt(0) != '.')) {
            newName = concat("X", newName);
        }

        return newName;
    }

    @Specialization
    protected RAbstractStringVector makeNames(RAbstractStringVector names, byte allowUnderScoreArg) {
        if (namesLengthZero.profile(names.getLength() == 0)) {
            return names;
        } else {
            boolean allowUnderscore = allowUnderScoreArg == RRuntime.LOGICAL_TRUE;
            RStringVector newNames = null;
            for (int i = 0; i < names.getLength(); i++) {
                String name = names.getDataAt(i);
                String newName = getKeyword(name, allowUnderscore);
                if (newName != null) {
                    newNames = getNewNames(names, newNames);
                    newNames.updateDataAt(i, newName, dummyCheck);
                } else {
                    newName = getName(name, allowUnderscore);
                    if (newName != name) {
                        // getName returns "name" in case nothing's changed
                        newNames = getNewNames(names, newNames);
                        newNames.updateDataAt(i, newName, dummyCheck);
                    }
                }
            }
            return newNames != null ? newNames : names;
        }
    }
}
