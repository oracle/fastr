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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.data.RString;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

@RBuiltin(name = "make.unique", kind = INTERNAL, parameterNames = {"names", "sep"})
public abstract class MakeUnique extends RBuiltinNode {

    private final ConditionProfile namesProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile duplicatesProfile = ConditionProfile.createBinaryProfile();
    private final NACheck dummyCheck = NACheck.create(); // never triggered (used for vector update)

    @Specialization
    protected RAbstractStringVector makeUnique(RAbstractStringVector names, String sep) {
        controlVisibility();
        if (namesProfile.profile(names.getLength() == 0 || names.getLength() == 1)) {
            return names;
        } else {
            // TODO: perhaps for longer vectors there is a faster algorithm using hash maps, but
            // then it would probably have to be put on the slow path even for cases when no
            // duplicates actually exist
            int[] duplicates = new int[names.getLength()];
            boolean duplicatesExist = false;
            for (int i = 0; i < duplicates.length; i++) {
                if (duplicates[i] > 0) {
                    // already processed
                    continue;
                }
                String current = names.getDataAt(i);
                int duplicatesCount = 0;
                for (int j = i + 1; j < duplicates.length; j++) {
                    if (current.equals(names.getDataAt(j))) {
                        duplicatesExist = true;
                        duplicates[j] = ++duplicatesCount;
                    }
                }
            }
            if (duplicatesProfile.profile(!duplicatesExist)) {
                return names;
            } else {
                RStringVector newNames = names.materialize();
                if (newNames.isShared()) {
                    newNames = (RStringVector) newNames.copy();
                }
                // start with 1 as the first one is never the duplicate
                for (int i = 1; i < duplicates.length; i++) {
                    if (duplicates[i] > 0) {
                        newNames.updateDataAt(i, concat(newNames.getDataAt(i), sep, duplicates[i]), dummyCheck);
                    }
                }
                return newNames;
            }
        }
    }

    @TruffleBoundary
    private static String concat(String s1, String sep, int index) {
        return s1 + sep + index;
    }

    @Specialization(guards = "sepIsString(sep)")
    protected RAbstractStringVector makeUnique(RAbstractStringVector names, RAbstractVector sep) {
        return makeUnique(names, (String) sep.getDataAtAsObject(0));
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "!sepIsString(sep)")
    protected RAbstractStringVector makeUniqueWrongSep(RAbstractStringVector names, RAbstractVector sep) {
        controlVisibility();
        throw RError.error(this, RError.Message.MUST_BE_STRING, "sep");
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "!namesIsStringVector(names)")
    protected RAbstractStringVector makeUnique(RAbstractVector names, Object sep) {
        controlVisibility();
        throw RError.error(this, RError.Message.NOT_CHARACTER_VECTOR, "names");
    }

    protected boolean namesIsStringVector(RAbstractVector names) {
        return names.getElementClass() == RString.class;
    }

    protected boolean sepIsString(RAbstractVector sep) {
        return sep.getElementClass() == RString.class && sep.getLength() == 1;
    }
}
