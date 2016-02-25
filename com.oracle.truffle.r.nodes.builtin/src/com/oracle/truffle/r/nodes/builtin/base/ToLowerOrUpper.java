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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.attributes.CopyOfRegAttributesNode;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.api.profiles.LoopConditionProfile;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.ops.na.NACheck;
import com.oracle.truffle.r.runtime.ops.na.NAProfile;

public abstract class ToLowerOrUpper extends RBuiltinNode {

    @RBuiltin(name = "tolower", kind = INTERNAL, parameterNames = {"x"})
    public static final class ToLower {
    }

    @RBuiltin(name = "toupper", kind = INTERNAL, parameterNames = {"x"})
    public static final class ToUpper {
    }

    public static ToLowerOrUpper createToLower(RNode[] arguments, RBuiltinFactory builtin, ArgumentsSignature suppliedSignature) {
        return ToLowerOrUpperNodeGen.create(true, arguments, builtin, suppliedSignature);
    }

    public static ToLowerOrUpper createToUpper(RNode[] arguments, RBuiltinFactory builtin, ArgumentsSignature suppliedSignature) {
        return ToLowerOrUpperNodeGen.create(false, arguments, builtin, suppliedSignature);
    }

    private final boolean lower;

    public ToLowerOrUpper(boolean lower) {
        this.lower = lower;
    }

    @TruffleBoundary
    protected String processElement(String value) {
        return lower ? value.toLowerCase() : value.toUpperCase();
    }

    @Specialization
    protected String toLower(String value, //
                    @Cached("create()") NAProfile na) {
        controlVisibility();
        return na.isNA(value) ? RRuntime.STRING_NA : processElement(value);
    }

    @Specialization
    protected RStringVector toLower(RAbstractStringVector vector, //
                    @Cached("createCountingProfile()") LoopConditionProfile loopProfile, //
                    @Cached("create()") NACheck na, //
                    @Cached("create()") CopyOfRegAttributesNode copyAttributes) {
        controlVisibility();
        na.enable(vector);
        String[] stringVector = new String[vector.getLength()];
        loopProfile.profileCounted(vector.getLength());
        for (int i = 0; loopProfile.inject(i < vector.getLength()); i++) {
            String value = vector.getDataAt(i);
            stringVector[i] = na.check(value) ? RRuntime.STRING_NA : processElement(value);
        }
        RStringVector result = RDataFactory.createStringVector(stringVector, vector.isComplete(), vector.getDimensions());
        copyAttributes.execute(vector, result);
        return result;
    }
}
