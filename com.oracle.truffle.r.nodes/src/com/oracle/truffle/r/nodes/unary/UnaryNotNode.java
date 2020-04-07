/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.unary;

import static com.oracle.truffle.r.runtime.RDispatch.OPS_GROUP_GENERIC;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE_ARITHMETIC;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.r.runtime.data.nodes.attributes.SpecialAttributesFunctions.ExtractDimNamesAttributeNode;
import com.oracle.truffle.r.runtime.data.nodes.attributes.SpecialAttributesFunctions.ExtractNamesAttributeNode;
import com.oracle.truffle.r.runtime.data.nodes.attributes.SpecialAttributesFunctions.GetDimAttributeNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDataFactory.VectorFactory;
import com.oracle.truffle.r.runtime.data.RRaw;
import com.oracle.truffle.r.runtime.data.RRawVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess.SequentialIterator;
import com.oracle.truffle.r.runtime.data.nodes.VectorReuse;
import com.oracle.truffle.r.runtime.interop.ConvertForeignObjectNode;
import com.oracle.truffle.r.runtime.ops.na.NAProfile;

@ImportStatic({RRuntime.class, ConvertForeignObjectNode.class, RType.class})
@RBuiltin(name = "!", kind = PRIMITIVE, parameterNames = {""}, dispatch = OPS_GROUP_GENERIC, behavior = PURE_ARITHMETIC)
public abstract class UnaryNotNode extends RBuiltinNode.Arg1 {

    private final NAProfile naProfile = NAProfile.create();

    @Child private GetDimAttributeNode getDims = GetDimAttributeNode.create();
    @Child private ExtractNamesAttributeNode extractNames = ExtractNamesAttributeNode.create();
    @Child private ExtractDimNamesAttributeNode extractDimNames = ExtractDimNamesAttributeNode.create();

    static {
        Casts.noCasts(UnaryNotNode.class);
    }

    private static byte not(byte value) {
        return (value == RRuntime.LOGICAL_TRUE ? RRuntime.LOGICAL_FALSE : RRuntime.LOGICAL_TRUE);
    }

    private static byte not(int operand) {
        return RRuntime.asLogical(operand == 0);
    }

    private static byte not(double operand) {
        return RRuntime.asLogical(operand == 0);
    }

    private static byte notRaw(RRaw operand) {
        return notRaw(operand.getValue());
    }

    private static byte notRaw(byte operand) {
        return (byte) (255 - operand);
    }

    @Specialization
    protected byte doLogical(byte operand) {
        return naProfile.isNA(operand) ? RRuntime.LOGICAL_NA : not(operand);
    }

    @Specialization
    protected byte doInt(int operand) {
        return naProfile.isNA(operand) ? RRuntime.LOGICAL_NA : not(operand);
    }

    @Specialization
    protected byte doDouble(double operand) {
        return naProfile.isNA(operand) ? RRuntime.LOGICAL_NA : not(operand);
    }

    @Specialization
    protected RRawVector doRaw(RRaw operand) {
        return RDataFactory.createRawVectorFromScalar(notRaw(operand));
    }

    @Specialization(guards = {"vectorAccess.supports(vector)", "reuse.supports(vector)"})
    protected RAbstractVector doLogicalVectorCached(RAbstractLogicalVector vector,
                    @Cached("vector.access()") VectorAccess vectorAccess,
                    @Cached("createTemporary(vector)") VectorReuse reuse) {
        RAbstractVector result = reuse.getResult(vector);
        VectorAccess resultAccess = reuse.access(result);
        try (SequentialIterator vectorIter = vectorAccess.access(vector); SequentialIterator resultIter = resultAccess.access(result)) {
            while (vectorAccess.next(vectorIter) && resultAccess.next(resultIter)) {
                byte value = vectorAccess.getLogical(vectorIter);
                resultAccess.setLogical(resultIter, vectorAccess.na.check(value) ? RRuntime.LOGICAL_NA : not(value));
            }
        }
        result.setComplete(vectorAccess.na.neverSeenNA());
        return result;
    }

    @Specialization(replaces = "doLogicalVectorCached")
    @TruffleBoundary
    protected RAbstractVector doLogicalGenericGeneric(RAbstractLogicalVector vector,
                    @Cached("createTemporaryGeneric()") VectorReuse reuse) {
        return doLogicalVectorCached(vector, vector.slowPathAccess(), reuse);
    }

    @Specialization(guards = {"vectorAccess.supports(vector)", "!isRAbstractLogicalVector(vector)"})
    protected RAbstractVector doVectorCached(RAbstractVector vector,
                    @Cached("vector.access()") VectorAccess vectorAccess,
                    @Cached("createNew(Logical)") VectorAccess resultAccess,
                    @Cached("createNew(Raw)") VectorAccess rawResultAccess,
                    @Cached("create()") VectorFactory factory) {
        try (SequentialIterator vectorIter = vectorAccess.access(vector)) {
            int length = vectorAccess.getLength(vectorIter);
            RAbstractVector result;
            switch (vectorAccess.getType()) {
                case Character:
                case List:
                case Expression:
                    // special cases:
                    if (length == 0) {
                        return factory.createEmptyLogicalVector();
                    } else {
                        throw error(RError.Message.INVALID_ARG_TYPE);
                    }
                case Raw:
                    result = factory.createRawVector(length);
                    try (SequentialIterator resultIter = rawResultAccess.access(result)) {
                        // raw does not produce a logical result, but (255 - value)
                        while (vectorAccess.next(vectorIter) && rawResultAccess.next(resultIter)) {
                            rawResultAccess.setRaw(resultIter, notRaw(vectorAccess.getRaw(vectorIter)));
                        }
                    }
                    (result).copyAttributesFrom(vector);
                    break;
                default:
                    result = factory.createLogicalVector(length, false);
                    try (SequentialIterator resultIter = resultAccess.access(result)) {
                        while (vectorAccess.next(vectorIter) && resultAccess.next(resultIter)) {
                            byte value = vectorAccess.getLogical(vectorIter);
                            resultAccess.setLogical(resultIter, vectorAccess.na.check(value) ? RRuntime.LOGICAL_NA : not(value));
                        }
                    }
                    if (vectorAccess.getType() == RType.Logical) {
                        (result).copyAttributesFrom(vector);
                    } else {
                        factory.reinitializeAttributes(result, getDims.getDimensions(vector), extractNames.execute(vector), extractDimNames.execute(vector));
                    }
                    break;
            }
            result.setComplete(vectorAccess.na.neverSeenNA());
            return result;
        }
    }

    @Specialization(replaces = "doVectorCached", guards = "!isRAbstractLogicalVector(vector)")
    @TruffleBoundary
    protected RAbstractVector doGenericGeneric(RAbstractVector vector,
                    @Cached("create()") VectorFactory factory) {
        return doVectorCached(vector, vector.slowPathAccess(), VectorAccess.createSlowPathNew(RType.Logical), VectorAccess.createSlowPathNew(RType.Raw), factory);
    }

    @Specialization(guards = {"isForeignArray(obj, interop)"}, limit = "getInteropLibraryCacheSize()")
    protected Object doForeign(VirtualFrame frame, TruffleObject obj,
                    @Cached("create()") ConvertForeignObjectNode convertForeign,
                    @Cached("createRecursive()") UnaryNotNode recursive,
                    @SuppressWarnings("unused") @CachedLibrary("obj") InteropLibrary interop) {
        Object vec = convertForeign.convert(obj);
        return recursive.execute(frame, vec);
    }

    @Specialization(guards = {"isForeignObject(obj)", "!isForeignArray(obj, interop)"}, limit = "getInteropLibraryCacheSize()")
    protected Object doForeign(@SuppressWarnings("unused") VirtualFrame frame, TruffleObject obj,
                    @SuppressWarnings("unused") @CachedLibrary("obj") InteropLibrary interop) {
        return invalidArgType(obj);
    }

    protected UnaryNotNode createRecursive() {
        return UnaryNotNodeGen.create();
    }

    @Fallback
    protected Object invalidArgType(@SuppressWarnings("unused") Object operand) {
        throw error(RError.Message.INVALID_ARG_TYPE);
    }
}
