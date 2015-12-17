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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.env.REnvironment;

/**
 * A temporary substitution to work around bug with {@code list(...)} used in R version.
 */
@RBuiltin(name = "structure", kind = SUBSTITUTE, parameterNames = {".Data", "..."})
public abstract class Structure extends RBuiltinNode {
    private final ConditionProfile instanceOfStringProfile = ConditionProfile.createBinaryProfile();
    private final RAttributeProfiles attrProfiles = RAttributeProfiles.create();

    @SuppressWarnings("unused")
    @Specialization
    protected Object structure(RMissing obj, RMissing args) {
        CompilerDirectives.transferToInterpreter();
        throw RError.error(this, RError.Message.ARGUMENT_MISSING, ".Data");
    }

    @Specialization
    protected Object structure(RAbstractContainer obj, @SuppressWarnings("unused") RMissing args) {
        return obj;
    }

    private static String fixupAttrName(String s) {
        // as per documentation of the "structure" function
        if (s.equals(".Dim")) {
            return "dim";
        } else if (s.equals(".Dimnames")) {
            return "dimnames";
        } else if (s.equals(".Names")) {
            return "names";
        } else if (s.equals(".Tsp")) {
            return "tsp";
        } else if (s.equals(".Label")) {
            return "levels";
        } else {
            return s.intern();
        }
    }

    @Specialization
    protected Object structure(RFunction obj, RArgsValuesAndNames args) {
        return doStructure(obj, args);
    }

    @Specialization
    protected Object structure(REnvironment obj, RArgsValuesAndNames args) {
        return doStructure(obj, args);
    }

    @Specialization
    protected Object structure(RSymbol obj, RArgsValuesAndNames args) {
        return doStructure(obj, args);
    }

    @Specialization
    protected Object structure(RExternalPtr obj, RArgsValuesAndNames args) {
        return doStructure(obj, args);
    }

    @Specialization
    protected Object structure(RAbstractContainer obj, RArgsValuesAndNames args) {
        RAttributable res = obj;
        // TODO: should we consider storing attributes with sequences?
        if (res instanceof RSequence) {
            res = ((RSequence) res).createVector();
        }
        return doStructure(res, args);
    }

    // Ideally we would only need this as a specialization, but then we fail due to
    // lack of auto-translation of, e.g. Double to RAbstractContainer.
    @TruffleBoundary
    protected Object doStructure(RAttributable obj, RArgsValuesAndNames args) {
        Object[] values = args.getArguments();
        ArgumentsSignature signature = getSuppliedSignature();
        validateArgNames(signature);
        RAttributable res = obj;
        for (int i = 0; i < values.length; i++) {
            Object value = fixupValue(values[i]);
            String attrName = fixupAttrName(signature.getName(i + 1));
            if (attrName.equals(RRuntime.CLASS_ATTR_KEY)) {
                if (value == RNull.instance) {
                    res = res.setClassAttr(null, true);
                } else {
                    res = res.setClassAttr((RStringVector) value, true);
                }
            } else {
                if (value == RNull.instance) {
                    res.removeAttr(attrProfiles, attrName);
                } else {
                    res.setAttr(attrName, value);
                }
            }
        }
        return res;
    }

    private Object fixupValue(Object value) {
        if (instanceOfStringProfile.profile(value instanceof String)) {
            return RDataFactory.createStringVectorFromScalar((String) value);
        } else {
            return value;
        }
    }

    @TruffleBoundary
    private void validateArgNames(ArgumentsSignature signature) {
        int containerIndex = 0;
        if (findNullIn(signature, containerIndex + 1)) {
            throw RError.error(this, RError.Message.ATTRIBUTES_NAMED);
        }
    }

    @TruffleBoundary
    private static boolean findNullIn(ArgumentsSignature signature, int startIndex) {
        for (int i = startIndex; i < signature.getLength(); i++) {
            if (signature.getName(i) == null) {
                return true;
            }
        }
        return false;
    }
}
