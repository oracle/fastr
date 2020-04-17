/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.access.vector;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.nodes.access.vector.AccessForeignObjectNodeFactory.ExtractPositionNodeGen;
import com.oracle.truffle.r.nodes.access.vector.AccessForeignObjectNodeFactory.ReadPositionsNodeGen;
import com.oracle.truffle.r.nodes.access.vector.AccessForeignObjectNodeFactory.WritePositionsNodeGen;
import com.oracle.truffle.r.nodes.profile.VectorLengthProfile;
import com.oracle.truffle.r.nodes.unary.CastStringNode;
import com.oracle.truffle.r.nodes.unary.FirstStringNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.interop.AccessForeignElementNode;
import com.oracle.truffle.r.runtime.interop.AccessForeignElementNode.WriteElementNode;
import com.oracle.truffle.r.runtime.interop.ConvertForeignObjectNode;
import com.oracle.truffle.r.runtime.interop.Foreign2R;
import com.oracle.truffle.r.runtime.interop.ForeignTypeCheck;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
import com.oracle.truffle.r.runtime.nodes.RBaseNodeWithWarnings;

public abstract class AccessForeignObjectNode extends RBaseNodeWithWarnings {

    @ImportStatic({RRuntime.class})
    public abstract static class ReadPositionsNode extends AccessForeignObjectNode {

        public static ReadPositionsNode create() {
            return ReadPositionsNodeGen.create();
        }

        public abstract Object execute(TruffleObject object, Object positions);

        @Specialization(guards = {"isForeignObject(object)", "!positionsByVector(positions)", "!positionsByLogicalValue(positions)"})
        protected Object readField(TruffleObject object, Object[] positions,
                        @Cached("create()") ReadPositionNode read,
                        @Cached("createClassProfile()") ValueProfile positionProfile,
                        @Cached("create()") VectorLengthProfile lengthProfile,
                        @Cached("create()") Foreign2R foreign2RNode) {
            Object[] pos = positionProfile.profile(positions);
            if (pos.length == 0) {
                throw error(RError.Message.GENERIC, "No positions for foreign access.");
            }
            Object result = object;
            for (int i = 0; i < lengthProfile.profile(pos.length); i++) {
                result = read.execute(pos[i], (TruffleObject) result);
                assert !(pos.length > 1 && i < pos.length - 1) || result instanceof TruffleObject;
            }
            return foreign2RNode.convert(result);
        }

        @Specialization(guards = {"isForeignObject(object)", "positionsByVector(positions)",
                        "!positionsByLogicalVector(positions)"})
        protected Object readFieldByVectorPositions(TruffleObject object, Object[] positions,
                        @Cached("create()") ReadPositionNode read,
                        @Cached("create()") Foreign2R foreign2RNode) {
            RAbstractVector vec = (RAbstractVector) positions[0];
            ForeignTypeCheck typeCheck = new ForeignTypeCheck();
            Object[] elements = new Object[vec.getLength()];
            for (int i = 0; i < vec.getLength(); i++) {
                Object res = read.execute(vec.getDataAtAsObject(i), object);
                elements[i] = foreign2RNode.convert(res);
                typeCheck.check(elements[i]);
            }
            return ConvertForeignObjectNode.asAbstractVector(elements, typeCheck.getType());
        }

        @Specialization(guards = {"isForeignObject(object)", "positionsByLogicalValue(positions)"})
        protected Object readFieldByLogicalVectorPositions(TruffleObject object, Object[] positions) {
            if (RRuntime.fromLogical((byte) positions[0])) {
                return object;
            } else {
                // TODO return should correspond with the read array type - e.g. int[] ->
                // integer(0)
                return RDataFactory.createList();
            }
        }

        @Specialization(guards = {"isForeignObject(object)", "positionsByLogicalVector(positions)", "interop.hasArrayElements(object)"}, limit = "getInteropLibraryCacheSize()")
        protected Object readFieldByLogicalVectorPositions(TruffleObject object, Object[] positions,
                        @Cached("create()") ReadPositionNode read,
                        @CachedLibrary("object") InteropLibrary interop,
                        @Cached("create()") Foreign2R foreign2RNode) {

            RLogicalVector positionsVector = (RLogicalVector) positions[0];
            ForeignTypeCheck typeCheck = new ForeignTypeCheck();
            int elementsIdx = 0;
            int positionsIdx = 0;
            int size = RRuntime.getForeignArraySize(object, interop);
            Object[] readElements = new Object[size];

            for (int targetIdx = 0; targetIdx < size; targetIdx++) {
                if (positionsIdx >= positionsVector.getLength()) {
                    positionsIdx = 0;
                }
                if (RRuntime.fromLogical(positionsVector.getDataAt(positionsIdx))) {
                    Object element = read.execute(targetIdx + 1, object);
                    readElements[elementsIdx] = foreign2RNode.convert(element);
                    typeCheck.check(readElements[elementsIdx]);
                    elementsIdx++;
                }
                positionsIdx++;
            }
            Object[] resultTrimmed = new Object[elementsIdx];
            System.arraycopy(readElements, 0, resultTrimmed, 0, elementsIdx);
            return ConvertForeignObjectNode.asAbstractVector(resultTrimmed, typeCheck.getType());
        }

        @Specialization(guards = {"isForeignObject(object)", "positionsByLogicalVector(positions)", "!interop.hasArrayElements(object)",
                        "positionsTRUE(positions)"}, limit = "getInteropLibraryCacheSize()")
        protected Object readFieldByLogicalVectorPositionsTRUE(@SuppressWarnings("unused") TruffleObject object, @SuppressWarnings("unused") Object[] positions,
                        @SuppressWarnings("unused") @CachedLibrary("object") InteropLibrary interop) {
            // TODO maybe we should handle as if list?
            return object;
        }

        @Specialization(guards = {"isForeignObject(object)", "positionsByLogicalVector(positions)", "!interop.hasArrayElements(object)",
                        "!positionsTRUE(positions)"}, limit = "getInteropLibraryCacheSize()")
        protected Object readFieldByLogicalVectorPositionsFALSE(@SuppressWarnings("unused") TruffleObject object, @SuppressWarnings("unused") Object[] positions,
                        @SuppressWarnings("unused") @CachedLibrary("object") InteropLibrary interop) {
            // TODO maybe we should handle as if list?
            return RDataFactory.createList();
        }

        protected boolean positionsTRUE(Object[] positions) {
            return RRuntime.fromLogical(((RLogicalVector) positions[0]).getDataAt(0));
        }

        @Specialization(guards = "!isForeignObject(object)")
        protected Object read(@SuppressWarnings("unused") TruffleObject object, @SuppressWarnings("unused") Object[] positions) {
            throw RInternalError.shouldNotReachHere();
        }
    }

    @ImportStatic({RRuntime.class, ConvertForeignObjectNode.class})
    public abstract static class WritePositionsNode extends AccessForeignObjectNode {

        public static WritePositionsNode create() {
            return WritePositionsNodeGen.create();
        }

        public abstract Object execute(TruffleObject object, Object[] positions, Object value);

        @Specialization(guards = {"isForeignObject(object)", "!positionsByVector(positions)", "!positionsByLogicalValue(positions)"})
        protected Object writeField(TruffleObject object, Object[] positions, Object value,
                        @Cached("create()") ReadPositionNode read,
                        @Cached("create()") WritePositionNode write,
                        @Cached("create()") VectorLengthProfile lengthProfile) {
            TruffleObject result = object;
            for (int i = 0; i < lengthProfile.profile(positions.length) - 1; i++) {
                result = (TruffleObject) read.execute(positions[i], result);
            }
            write.execute(positions[positions.length - 1], result, value);
            return object;
        }

        @Specialization(guards = {"isForeignObject(object)", "positionsByVector(positions)",
                        "!positionsByLogicalVector(positions)"})
        protected Object writeFieldByVectorPositions(TruffleObject object, Object[] positions, Object value,
                        @Cached("create()") WritePositionNode write,
                        @Cached("createBinaryProfile()") ConditionProfile isValuesVector) {
            RAbstractVector positionsVector = (RAbstractVector) positions[0];
            if (isValuesVector.profile(value instanceof RAbstractVector)) {
                RAbstractVector values = (RAbstractVector) value;
                if (values.getLength() > 1 && positionsVector.getLength() % values.getLength() != 0) {
                    throw error(RError.Message.NOT_MULTIPLE_REPLACEMENT);
                }
                int valueIdx = 0;
                for (int i = 0; i < positionsVector.getLength(); i++) {
                    if (valueIdx >= values.getLength()) {
                        valueIdx = 0;
                    }
                    write.execute(positionsVector.getDataAtAsObject(i), object, values.getDataAtAsObject(valueIdx));
                    valueIdx++;
                }
            } else {
                for (int i = 0; i < positionsVector.getLength(); i++) {
                    write.execute(positionsVector.getDataAtAsObject(i), object, value);
                }
            }
            return object;
        }

        @Specialization(guards = {"isForeignObject(object)", "positionsByLogicalValue(positions)"}, limit = "getInteropLibraryCacheSize()")
        protected Object writeField(TruffleObject object, Object[] positions, Object value,
                        @CachedLibrary("object") InteropLibrary interop,
                        @Cached("create()") WritePositionNode write) {
            if (RRuntime.fromLogical((byte) positions[0])) {
                int size = RRuntime.getForeignArraySize(object, interop);
                if (size > 0) {
                    for (int i = 0; i < size; i++) {
                        write.execute(i + 1, object, value);
                    }
                }
            }
            return object;
        }

        @Specialization(guards = {"isForeignObject(object)", "positionsByLogicalVector(positions)"}, limit = "getInteropLibraryCacheSize()")
        protected Object writeFieldByLogicalVectorPositions(TruffleObject object, Object[] positions, Object value,
                        @Cached("create()") WritePositionNode write,
                        @CachedLibrary("object") InteropLibrary interop,
                        @Cached("createBinaryProfile()") ConditionProfile isValuesVector) {
            RLogicalVector positionsVector = (RLogicalVector) positions[0];
            int size = RRuntime.getForeignArraySize(object, interop);
            int positionsIdx = 0;
            if (isValuesVector.profile(value instanceof RAbstractVector)) {
                RAbstractVector values = (RAbstractVector) value;
                if (values.getLength() > 1) {
                    // yes, generates always a warning as in v[c(T,F)] <- c(11, 22)
                    warning(RError.Message.NOT_MULTIPLE_REPLACEMENT);
                }
                int valueIdx = 0;
                for (int targetIdx = 0; targetIdx < size; targetIdx++) {
                    if (positionsIdx >= positionsVector.getLength()) {
                        positionsIdx = 0;
                    }
                    if (valueIdx >= values.getLength()) {
                        valueIdx = 0;
                    }
                    if (RRuntime.fromLogical(positionsVector.getDataAt(positionsIdx))) {
                        write.execute(targetIdx + 1, object, values.getDataAtAsObject(valueIdx));
                        valueIdx++;
                    }
                    positionsIdx++;
                }
            } else {
                for (int targetIdx = 0; targetIdx < size; targetIdx++) {
                    if (positionsIdx >= positionsVector.getLength()) {
                        positionsIdx = 0;
                    }
                    if (RRuntime.fromLogical(positionsVector.getDataAt(positionsIdx))) {
                        write.execute(targetIdx + 1, object, value);
                    }
                    positionsIdx++;
                }
            }
            return object;
        }

        @Specialization(guards = "!isForeignObject(object)")
        protected Object read(@SuppressWarnings("unused") TruffleObject object, @SuppressWarnings("unused") Object[] positions, @SuppressWarnings("unused") Object value) {
            throw RInternalError.shouldNotReachHere();
        }
    }

    static final class ReadPositionNode extends RBaseNode {

        @Child protected AccessForeignElementNode.ReadElementNode read = AccessForeignElementNode.ReadElementNode.create();
        @Child protected ExtractPositionNode extractPosition = ExtractPositionNodeGen.create();

        public static ReadPositionNode create() {
            return new ReadPositionNode();
        }

        public Object execute(Object position, TruffleObject object) {
            return read.execute(object, extractPosition.execute(position));
        }
    }

    static final class WritePositionNode extends RBaseNode {

        @Child protected WriteElementNode write = WriteElementNode.create();
        @Child protected ExtractPositionNode extractPosition = ExtractPositionNodeGen.create();

        public static WritePositionNode create() {
            return new WritePositionNode();
        }

        protected void execute(Object position, TruffleObject object, Object writtenValue) {
            write.execute(object, extractPosition.execute(position), writtenValue);
        }
    }

    abstract static class ExtractPositionNode extends RBaseNode {

        public abstract Object execute(Object object);

        @Specialization
        public Object doInt(int i) {
            return i - 1;
        }

        @Specialization
        public Object doDouble(double i) {
            return i - 1;
        }

        @Specialization
        public Object doString(String str) {
            return str;
        }

        @Specialization
        public Object doIntVector(RIntVector vector) {
            if (vector.getLength() == 0) {
                throw error(RError.Message.GENERIC, "invalid index during foreign access");
            }
            return vector.getDataAt(0) - 1;
        }

        @Specialization
        public Object doDoubleVector(RDoubleVector vector) {
            if (vector.getLength() == 0) {
                throw error(RError.Message.GENERIC, "invalid index during foreign access");
            }
            return vector.getDataAt(0) - 1;
        }

        @Specialization
        public Object doStringVector(RAbstractStringVector vector,
                        @Cached("create()") CastStringNode castNode,
                        @Cached("createFirstString()") FirstStringNode firstString) {
            return firstString.executeString(castNode.doCast(vector));
        }

        @Fallback
        public Object fallback(@SuppressWarnings("unused") Object vector) {
            throw error(RError.Message.GENERIC, "invalid index during foreign access");
        }

        protected static FirstStringNode createFirstString() {
            return FirstStringNode.createWithError(RError.Message.GENERIC, "Cannot coerce position to character for foreign access.");
        }
    }

    protected static boolean positionsByLogicalValue(Object[] positions) {
        return positions.length == 1 && positions[0] instanceof Byte;
    }

    protected static boolean positionsByLogicalVector(Object[] positions) {
        return positions.length == 1 && positions[0] instanceof RLogicalVector && ((RLogicalVector) positions[0]).getLength() > 1;
    }

    protected static boolean positionsByVector(Object[] positions) {
        return positions.length == 1 && positions[0] instanceof RAbstractVector && ((RAbstractVector) positions[0]).getLength() > 1;
    }
}
