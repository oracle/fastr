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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import java.util.function.BiFunction;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.LoopConditionProfile;
import com.oracle.truffle.r.nodes.attributes.CopyOfRegAttributesNode;
import com.oracle.truffle.r.nodes.attributes.CopyOfRegAttributesNodeGen;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetDimAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetNamesAttributeNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.profile.VectorLengthProfile;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

public abstract class ToLowerOrUpper {

    public static final class StringMapNode extends RBaseNode {

        private final VectorLengthProfile lengthProfile = VectorLengthProfile.create();
        private final LoopConditionProfile loopProfile = LoopConditionProfile.createCountingProfile();
        private final NACheck na = NACheck.create();
        @Child private GetNamesAttributeNode getNames = GetNamesAttributeNode.create();

        @Child private CopyOfRegAttributesNode copyAttributes = CopyOfRegAttributesNodeGen.create();
        @Child private GetDimAttributeNode getDimNode = GetDimAttributeNode.create();

        private StringMapNode() {
            // nothing to do
        }

        public static StringMapNode create() {
            return new StringMapNode();
        }

        private String elementFunction(String value, int i, BiFunction<String, Integer, String> function) {
            return na.check(value) ? RRuntime.STRING_NA : function.apply(value, i);
        }

        public String apply(String value, BiFunction<String, Integer, String> function) {
            na.enable(value);
            return elementFunction(value, 0, function);
        }

        public RStringVector apply(RAbstractStringVector vector, BiFunction<String, Integer, String> function) {
            na.enable(vector);
            int length = lengthProfile.profile(vector.getLength());
            String[] stringVector = new String[length];
            loopProfile.profileCounted(length);
            for (int i = 0; loopProfile.inject(i < length); i++) {
                String value = vector.getDataAt(i);
                stringVector[i] = elementFunction(value, i, function);
            }
            RStringVector result = RDataFactory.createStringVector(stringVector, vector.isComplete(), getDimNode.getDimensions(vector), getNames.getNames(vector));
            copyAttributes.execute(vector, result);
            return result;
        }
    }

    @RBuiltin(name = "tolower", kind = INTERNAL, parameterNames = {"x"}, behavior = PURE)
    public abstract static class ToLower extends RBuiltinNode {

        @Child private StringMapNode mapNode = StringMapNode.create();

        static {
            Casts casts = new Casts(ToLower.class);
            casts.arg(0, "x").mustBe(stringValue()).asStringVector(true, true, true);
        }

        @TruffleBoundary
        private static String processElement(String value, @SuppressWarnings("unused") int i) {
            return value.toLowerCase();
        }

        @Specialization
        protected String toLower(String value) {
            return mapNode.apply(value, ToLower::processElement);
        }

        @Specialization
        protected RStringVector toLower(RAbstractStringVector vector) {
            return mapNode.apply(vector, ToLower::processElement);
        }
    }

    @RBuiltin(name = "toupper", kind = INTERNAL, parameterNames = {"x"}, behavior = PURE)
    public abstract static class ToUpper extends RBuiltinNode {

        @Child private StringMapNode mapNode = StringMapNode.create();

        static {
            Casts casts = new Casts(ToUpper.class);
            casts.arg(0, "x").mustBe(stringValue()).asStringVector(true, true, true);
        }

        @TruffleBoundary
        private static String processElement(String value, @SuppressWarnings("unused") int i) {
            return value.toUpperCase();
        }

        @Specialization
        protected String toLower(String value) {
            return mapNode.apply(value, ToUpper::processElement);
        }

        @Specialization
        protected RStringVector toLower(RAbstractStringVector vector) {
            return mapNode.apply(vector, ToUpper::processElement);
        }
    }
}
