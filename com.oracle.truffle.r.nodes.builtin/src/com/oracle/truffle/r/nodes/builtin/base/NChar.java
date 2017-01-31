/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.asIntegerVector;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.asStringVector;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.integerValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.LoopConditionProfile;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetDimAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetDimNamesAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetNamesAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.SetDimNamesAttributeNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;

// TODO interpret "type" and "allowNA" arguments
@RBuiltin(name = "nchar", kind = INTERNAL, parameterNames = {"x", "type", "allowNA", "keepNA"}, behavior = PURE)
public abstract class NChar extends RBuiltinNode {

    static {
        Casts casts = new Casts(NChar.class);
        casts.arg("x").allowNull().mapIf(integerValue(), asIntegerVector(), asStringVector(true, false, false));
        casts.arg("type").asStringVector().findFirst();
        casts.arg("allowNA").asLogicalVector().findFirst(RRuntime.LOGICAL_TRUE).map(toBoolean());
        casts.arg("keepNA").asLogicalVector().findFirst(RRuntime.LOGICAL_FALSE).map(toBoolean());
    }

    @SuppressWarnings("unused")
    @Specialization
    protected RIntVector nchar(RNull value, String type, boolean allowNA, boolean keepNA) {
        return RDataFactory.createEmptyIntVector();
    }

    @SuppressWarnings("unused")
    @Specialization
    protected RIntVector ncharInt(RAbstractIntVector vector, String type, boolean allowNA, boolean keepNA,
                    @Cached("createCountingProfile()") LoopConditionProfile loopProfile,
                    @Cached("createBinaryProfile()") ConditionProfile nullDimNamesProfile,
                    @Cached("create()") GetDimAttributeNode getDimNode,
                    @Cached("create()") SetDimNamesAttributeNode setDimNamesNode,
                    @Cached("create()") GetDimNamesAttributeNode getDimNamesNode,
                    @Cached("create()") GetNamesAttributeNode getNamesNode) {
        int len = vector.getLength();
        int[] result = new int[len];
        loopProfile.profileCounted(len);
        for (int i = 0; loopProfile.inject(i < len); i++) {
            int x = vector.getDataAt(i);
            if (x == RRuntime.INT_NA) {
                result[i] = 2;
            } else {
                result[i] = (int) (Math.log10(x) + 1); // not the fastest one
            }
        }
        RIntVector resultVector = RDataFactory.createIntVector(result, true, getDimNode.getDimensions(vector), getNamesNode.getNames(vector));
        RList dimNames = getDimNamesNode.getDimNames(vector);
        if (nullDimNamesProfile.profile(dimNames != null)) {
            setDimNamesNode.setDimNames(resultVector, dimNames);
        }
        return resultVector;
    }

    @SuppressWarnings("unused")
    @Specialization
    protected RIntVector nchar(RAbstractStringVector vector, String type, boolean allowNA, boolean keepNA,
                    @Cached("createCountingProfile()") LoopConditionProfile loopProfile,
                    @Cached("createBinaryProfile()") ConditionProfile nullDimNamesProfile,
                    @Cached("create()") GetDimAttributeNode getDimNode,
                    @Cached("create()") SetDimNamesAttributeNode setDimNamesNode,
                    @Cached("create()") GetDimNamesAttributeNode getDimNamesNode,
                    @Cached("create()") GetNamesAttributeNode getNamesNode) {
        int len = vector.getLength();
        int[] result = new int[len];
        loopProfile.profileCounted(len);
        for (int i = 0; loopProfile.inject(i < len); i++) {
            result[i] = vector.getDataAt(i).length();
        }
        RIntVector resultVector = RDataFactory.createIntVector(result, true, getDimNode.getDimensions(vector), getNamesNode.getNames(vector));
        RList dimNames = getDimNamesNode.getDimNames(vector);
        if (nullDimNamesProfile.profile(dimNames != null)) {
            setDimNamesNode.setDimNames(resultVector, dimNames);
        }
        return resultVector;
    }
}
