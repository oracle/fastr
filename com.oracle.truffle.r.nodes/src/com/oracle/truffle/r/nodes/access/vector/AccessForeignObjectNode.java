/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.KeyInfo;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.nodes.access.vector.AccessForeignObjectNodeFactory.ReadElementsNodeGen;
import com.oracle.truffle.r.nodes.access.vector.AccessForeignObjectNodeFactory.WriteElementsNodeGen;
import com.oracle.truffle.r.nodes.profile.VectorLengthProfile;
import com.oracle.truffle.r.nodes.unary.CastStringNode;
import com.oracle.truffle.r.nodes.unary.FirstStringNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.interop.ConvertForeignObjectNode;
import com.oracle.truffle.r.runtime.interop.Foreign2R;
import com.oracle.truffle.r.runtime.interop.ForeignTypeCheck;
import com.oracle.truffle.r.runtime.interop.R2Foreign;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
import com.oracle.truffle.r.runtime.nodes.RBaseNodeWithWarnings;

abstract class AccessForeignObjectNode extends RBaseNodeWithWarnings {

    @ImportStatic({RRuntime.class, Message.class})
    protected abstract static class ReadElementsNode extends AccessForeignObjectNode {

        public static ReadElementsNode create() {
            return ReadElementsNodeGen.create();
        }

        protected abstract Object execute(TruffleObject object, Object positions);

        @Specialization(guards = {"isForeignObject(object)", "!positionsByVector(positions)", "!positionsByLogicalValue(positions)"})
        protected Object readField(TruffleObject object, Object[] positions,
                        @Cached("create()") ReadElementNode readElement,
                        @Cached("createClassProfile()") ValueProfile positionProfile,
                        @Cached("create()") VectorLengthProfile lengthProfile,
                        @Cached("create()") Foreign2R foreign2RNode) {
            Object[] pos = positionProfile.profile(positions);
            if (pos.length == 0) {
                throw error(RError.Message.GENERIC, "No positions for foreign access.");
            }
            Object result = object;
            for (int i = 0; i < lengthProfile.profile(pos.length); i++) {
                result = readElement.execute(pos[i], (TruffleObject) result);
                assert !(pos.length > 1 && i < pos.length - 1) || result instanceof TruffleObject;
            }
            return foreign2RNode.execute(result);
        }

        @Specialization(guards = {"isForeignObject(object)", "positionsByVector(positions)",
                        "!positionsByLogicalVector(positions)"})
        protected Object readFieldByVectorPositions(TruffleObject object, Object[] positions,
                        @Cached("create()") ReadElementNode readElement,
                        @Cached("create()") Foreign2R foreign2RNode) {
            RAbstractVector vec = (RAbstractVector) positions[0];
            ForeignTypeCheck typeCheck = new ForeignTypeCheck();
            Object[] elements = new Object[vec.getLength()];
            for (int i = 0; i < vec.getLength(); i++) {
                Object res = readElement.execute(vec.getDataAtAsObject(i), object);
                elements[i] = foreign2RNode.execute(res);
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

        @Specialization(guards = {"isForeignObject(object)", "positionsByLogicalVector(positions)"})
        protected Object readFieldByLogicalVectorPositions(TruffleObject object, Object[] positions,
                        @Cached("create()") ReadElementNode readElement,
                        @Cached("HAS_SIZE.createNode()") Node hasSizeNode,
                        @Cached("GET_SIZE.createNode()") Node sizeNode,
                        @Cached("create()") Foreign2R foreign2RNode) {

            RAbstractLogicalVector positionsVector = (RAbstractLogicalVector) positions[0];
            ForeignTypeCheck typeCheck = new ForeignTypeCheck();
            int elementsIdx = 0;
            int positionsIdx = 0;
            if (!ForeignAccess.sendHasSize(hasSizeNode, object)) {
                // TODO maybe we should handle as if list?
                if (RRuntime.fromLogical(positionsVector.getDataAt(0))) {
                    return object;
                } else {
                    return RDataFactory.createList();
                }
            } else {
                int size = getSize(sizeNode, object, this);
                Object[] readElements = new Object[size];

                for (int targetIdx = 0; targetIdx < size; targetIdx++) {
                    if (positionsIdx >= positionsVector.getLength()) {
                        positionsIdx = 0;
                    }
                    if (RRuntime.fromLogical(positionsVector.getDataAt(positionsIdx))) {
                        Object element = readElement.execute(targetIdx + 1, object);
                        readElements[elementsIdx] = foreign2RNode.execute(element);
                        typeCheck.check(readElements[elementsIdx]);
                        elementsIdx++;
                    }
                    positionsIdx++;
                }
                Object[] resultTrimmed = new Object[elementsIdx];
                System.arraycopy(readElements, 0, resultTrimmed, 0, elementsIdx);
                return ConvertForeignObjectNode.asAbstractVector(resultTrimmed, typeCheck.getType());
            }
        }

        @Specialization(guards = "!isForeignObject(object)")
        protected Object read(@SuppressWarnings("unused") TruffleObject object, @SuppressWarnings("unused") Object[] positions) {
            throw RInternalError.shouldNotReachHere();
        }
    }

    @ImportStatic({RRuntime.class, ConvertForeignObjectNode.class, Message.class})
    protected abstract static class WriteElementsNode extends AccessForeignObjectNode {

        public static WriteElementsNode create() {
            return WriteElementsNodeGen.create();
        }

        protected abstract Object execute(TruffleObject object, Object[] positions, Object value);

        @Specialization(guards = {"isForeignObject(object)", "!positionsByVector(positions)", "!positionsByLogicalValue(positions)"})
        protected Object writeField(TruffleObject object, Object[] positions, Object value,
                        @Cached("create()") ReadElementNode readElement,
                        @Cached("create()") WriteElementNode writeElement,
                        @Cached("create()") VectorLengthProfile lengthProfile) {
            TruffleObject result = object;
            for (int i = 0; i < lengthProfile.profile(positions.length) - 1; i++) {
                result = (TruffleObject) readElement.execute(positions[i], result);
            }
            writeElement.execute(positions[positions.length - 1], result, value);
            return object;
        }

        @Specialization(guards = {"isForeignObject(object)", "positionsByVector(positions)",
                        "!positionsByLogicalVector(positions)"})
        protected Object writeFieldByVectorPositions(TruffleObject object, Object[] positions, Object value,
                        @Cached("create()") WriteElementNode writeElement,
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
                    writeElement.execute(positionsVector.getDataAtAsObject(i), object, values.getDataAtAsObject(valueIdx));
                    valueIdx++;
                }
            } else {
                for (int i = 0; i < positionsVector.getLength(); i++) {
                    writeElement.execute(positionsVector.getDataAtAsObject(i), object, value);
                }
            }
            return object;
        }

        @Specialization(guards = {"isForeignObject(object)", "positionsByLogicalValue(positions)"})
        protected Object writeField(TruffleObject object, Object[] positions, Object value,
                        @Cached("GET_SIZE.createNode()") Node getSize,
                        @Cached("create()") WriteElementNode write) {
            if (RRuntime.fromLogical((byte) positions[0])) {
                int size = getSize(getSize, object, this);
                if (size > 0) {
                    for (int i = 0; i < size; i++) {
                        write.execute(i + 1, object, value);
                    }
                }
            }
            return object;
        }

        @Specialization(guards = {"isForeignObject(object)", "positionsByLogicalVector(positions)"})
        protected Object writeFieldByLogicalVectorPositions(TruffleObject object, Object[] positions, Object value,
                        @Cached("create()") WriteElementNode writeElement,
                        @Cached("GET_SIZE.createNode()") Node sizeNode,
                        @Cached("HAS_SIZE.createNode()") Node hasSizeNode,
                        @Cached("createBinaryProfile()") ConditionProfile isValuesVector) {
            RAbstractLogicalVector positionsVector = (RAbstractLogicalVector) positions[0];
            if (!ForeignAccess.sendHasSize(hasSizeNode, object)) {
                // TODO what now? replace all fields or none?
                throw error(RError.Message.GENERIC, "invalid index/identifier during foreign access: " + positions[0]);
            }
            int size = getSize(sizeNode, object, this);
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
                        writeElement.execute(targetIdx + 1, object, values.getDataAtAsObject(valueIdx));
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
                        writeElement.execute(targetIdx + 1, object, value);
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

    static final class ReadElementNode extends AccessElementNode {

        @Child private Node readStaticNode;

        public static ReadElementNode create() {
            return new ReadElementNode();
        }

        public Object execute(Object position, TruffleObject object) {
            Object pos = extractPosition(position);
            if (keyInfoNode == null) {
                try {
                    return ForeignAccess.sendRead(getReadNode(), object, pos);
                } catch (InteropException e) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    keyInfoNode = insert(Message.KEY_INFO.createNode());
                }
            }
            RContext context = RContext.getInstance();
            TruffleLanguage.Env env = context.getEnv();
            int info = ForeignAccess.sendKeyInfo(keyInfoNode, object, pos);
            try {
                if (KeyInfo.isExisting(info) && (KeyInfo.isReadable(info) || hasSize(object))) {
                    return ForeignAccess.sendRead(getReadNode(), object, pos);
                } else if (maybeJavaStaticField(pos, info, env, object)) {
                    if (readStaticNode == null) {
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                        readStaticNode = insert(Message.READ.createNode());
                    }
                    TruffleObject clazz = null;
                    try {
                        clazz = context.toJavaStatic(object, readStaticNode, getExecuteNode());
                        return ForeignAccess.sendRead(readStaticNode, clazz, pos);
                    } catch (UnknownIdentifierException e) {
                        throw invalidIdentifierError(pos);
                    } catch (InteropException e) {
                        if (clazz != null && KeyInfo.isReadable(ForeignAccess.sendKeyInfo(keyInfoNode, clazz, pos))) {
                            CompilerDirectives.transferToInterpreter();
                            throw error(RError.Message.GENERIC, "error in foreign access: " + pos + " " + e.getMessage());
                        }
                        throw e;
                    }
                }
            } catch (InteropException e) {
                CompilerDirectives.transferToInterpreter();
                throw RError.interopError(RError.findParentRBase(this), e, object);
            }
            throw invalidIdentifierError(pos);
        }
    }

    @ImportStatic(Message.class)
    static final class WriteElementNode extends AccessElementNode {

        @Child private Node writeStaticNode;
        @Child private Node writeNode = Message.WRITE.createNode();
        @Child private R2Foreign r2Foreign = R2Foreign.create();

        public static WriteElementNode create() {
            return new WriteElementNode();
        }

        protected void execute(Object position, TruffleObject object, Object writtenValue) {
            Object pos = extractPosition(position);
            Object value = r2Foreign.execute(writtenValue);
            if (keyInfoNode == null) {
                try {
                    ForeignAccess.sendWrite(writeNode, object, pos, value);
                    return;
                } catch (InteropException e) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    keyInfoNode = insert(Message.KEY_INFO.createNode());
                }
            }
            RContext context = RContext.getInstance();
            TruffleLanguage.Env env = context.getEnv();
            int info = ForeignAccess.sendKeyInfo(keyInfoNode, object, pos);
            try {
                if (KeyInfo.isExisting(info) && (KeyInfo.isWritable(info) || hasSize(object))) {
                    ForeignAccess.sendWrite(writeNode, object, pos, value);
                    return;
                } else if (maybeJavaStaticField(pos, info, env, object)) {
                    if (writeStaticNode == null) {
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                        writeStaticNode = insert(Message.WRITE.createNode());
                    }
                    TruffleObject clazz = null;
                    try {
                        clazz = context.toJavaStatic(object, getReadNode(), getExecuteNode());
                        ForeignAccess.sendWrite(writeStaticNode, clazz, pos, value);
                        return;
                    } catch (UnknownIdentifierException e) {
                        throw invalidIdentifierError(pos);
                    } catch (InteropException e) {
                        if (KeyInfo.isWritable(ForeignAccess.sendKeyInfo(keyInfoNode, clazz, pos))) {
                            CompilerDirectives.transferToInterpreter();
                            throw error(RError.Message.GENERIC, "error in foreign access: " + pos + " " + e.getMessage());
                        }
                        throw e;
                    }
                }
            } catch (InteropException ex) {
                CompilerDirectives.transferToInterpreter();
                throw RError.interopError(RError.findParentRBase(this), ex, object);
            }
            throw invalidIdentifierError(pos);
        }
    }

    abstract static class AccessElementNode extends RBaseNode {
        @Child protected Node keyInfoNode;
        @Child private Node executeNode;
        @Child private Node hasSizeNode;
        @Child private Node readNode;
        @Child private CastStringNode castNode;
        @Child private FirstStringNode firstString;

        private final ConditionProfile isIntProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile isDoubleProfile = ConditionProfile.createBinaryProfile();

        protected final Object extractPosition(Object position) {
            Object pos = position;
            if (isIntProfile.profile(pos instanceof Integer)) {
                pos = ((int) pos) - 1;
            } else if (isDoubleProfile.profile(pos instanceof Double)) {
                pos = ((double) pos) - 1;
            } else if (pos instanceof RAbstractDoubleVector) {
                RAbstractDoubleVector vector = (RAbstractDoubleVector) pos;
                if (vector.getLength() == 0) {
                    throw error(RError.Message.GENERIC, "invalid index during foreign access");
                }
                pos = vector.getDataAt(0) - 1;
            } else if (pos instanceof RAbstractIntVector) {
                RAbstractIntVector vector = (RAbstractIntVector) pos;
                if (vector.getLength() == 0) {
                    throw error(RError.Message.GENERIC, "invalid index during foreign access");
                }
                pos = vector.getDataAt(0) - 1;
            } else if (pos instanceof RAbstractStringVector) {
                if (castNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    castNode = insert(CastStringNode.create());
                }
                if (firstString == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    firstString = insert(createFirstString());
                }
                pos = firstString.executeString(castNode.doCast(pos));
            } else if (!(pos instanceof String)) {
                throw error(RError.Message.GENERIC, "invalid index during foreign access");
            }
            return pos;
        }

        protected Node getExecuteNode() {
            if (executeNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                executeNode = insert(Message.EXECUTE.createNode());
            }
            return executeNode;
        }

        protected Node getReadNode() {
            if (readNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                readNode = insert(Message.READ.createNode());
            }
            return readNode;
        }

        protected final boolean hasSize(TruffleObject object) {
            if (hasSizeNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                hasSizeNode = insert(Message.HAS_SIZE.createNode());
            }
            return ForeignAccess.sendHasSize(hasSizeNode, object);
        }

        protected static boolean maybeJavaStaticField(Object pos, int info, TruffleLanguage.Env env, TruffleObject object) {
            return pos instanceof String && !KeyInfo.isExisting(info) && env.isHostObject(object) && !(env.asHostObject(object) instanceof Class);
        }

        protected RError invalidIdentifierError(Object pos) throws RError {
            CompilerDirectives.transferToInterpreter();
            throw error(RError.Message.GENERIC, "invalid index/identifier during foreign access: " + pos);
        }
    }

    protected static FirstStringNode createFirstString() {
        return FirstStringNode.createWithError(RError.Message.GENERIC, "Cannot coerce position to character for foreign access.");
    }

    protected static boolean positionsByLogicalValue(Object[] positions) {
        return positions.length == 1 && positions[0] instanceof Byte;
    }

    protected static boolean positionsByLogicalVector(Object[] positions) {
        return positions.length == 1 && positions[0] instanceof RAbstractLogicalVector && ((RAbstractLogicalVector) positions[0]).getLength() > 1;
    }

    protected static boolean positionsByVector(Object[] positions) {
        return positions.length == 1 && positions[0] instanceof RAbstractVector && ((RAbstractVector) positions[0]).getLength() > 1;
    }

    private static int getSize(Node sizeNode, TruffleObject object, Node caller) throws RError {
        int size;
        try {
            size = (int) ForeignAccess.sendGetSize(sizeNode, object);
        } catch (UnsupportedMessageException ex) {
            CompilerDirectives.transferToInterpreter();
            throw RError.interopError(RError.findParentRBase(caller), ex, object);
        }
        return size;
    }
}
