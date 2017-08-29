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
package com.oracle.truffle.r.nodes.builtin.base.printer;

import java.io.IOException;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.r.nodes.attributes.SetFixedAttributeNode;
import com.oracle.truffle.r.nodes.binary.BoxPrimitiveNode;
import com.oracle.truffle.r.nodes.builtin.base.InheritsBuiltin;
import com.oracle.truffle.r.nodes.builtin.base.InheritsBuiltinNodeGen;
import com.oracle.truffle.r.nodes.builtin.base.IsMethodsDispatchOn;
import com.oracle.truffle.r.nodes.builtin.base.IsMethodsDispatchOnNodeGen;
import com.oracle.truffle.r.nodes.builtin.base.IsS4;
import com.oracle.truffle.r.nodes.builtin.base.IsS4NodeGen;
import com.oracle.truffle.r.nodes.builtin.base.IsTypeFunctions.IsArray;
import com.oracle.truffle.r.nodes.builtin.base.IsTypeFunctions.IsList;
import com.oracle.truffle.r.nodes.builtin.base.IsTypeFunctions.IsObject;
import com.oracle.truffle.r.nodes.builtin.base.IsTypeFunctionsFactory.IsArrayNodeGen;
import com.oracle.truffle.r.nodes.builtin.base.IsTypeFunctionsFactory.IsListNodeGen;
import com.oracle.truffle.r.nodes.builtin.base.IsTypeFunctionsFactory.IsObjectNodeGen;
import com.oracle.truffle.r.nodes.unary.CastStringNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.RAttributeStorage;
import com.oracle.truffle.r.runtime.data.RAttributesLayout;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RTypedValue;
import com.oracle.truffle.r.runtime.data.RVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

public final class ValuePrinterNode extends RBaseNode {

    @Child private IsArray isArrayBuiltIn = IsArrayNodeGen.create();
    @Child private IsList isListBuiltIn = IsListNodeGen.create();
    @Child private InheritsBuiltin inheritsBuiltinBuiltIn = InheritsBuiltinNodeGen.create();
    @Child private IsS4 isS4BuiltIn = IsS4NodeGen.create();
    @Child private IsObject isObjectBuiltIn = IsObjectNodeGen.create();
    @Child private IsMethodsDispatchOn isMethodDispatchOnBuiltIn = IsMethodsDispatchOnNodeGen.create();
    @Child private CastStringNode castStringNode = CastStringNode.createNonPreserving();
    @Child private BoxPrimitiveNode boxPrimitiveNode = BoxPrimitiveNode.create();

    @Child private ConvertTruffleObjectNode convertTruffleObject;

    /**
     * This node inspects non-R {@link TruffleObject}s and tries to create wrappers for them that
     * mimic R data structures.
     */
    public static final class ConvertTruffleObjectNode extends Node {

        @Child private Node hasSizeNode = com.oracle.truffle.api.interop.Message.HAS_SIZE.createNode();
        @Child private Node getSizeNode = com.oracle.truffle.api.interop.Message.GET_SIZE.createNode();
        @Child private Node readNode = com.oracle.truffle.api.interop.Message.READ.createNode();
        @Child private Node isBoxedNode = com.oracle.truffle.api.interop.Message.IS_BOXED.createNode();
        @Child private Node unboxNode = com.oracle.truffle.api.interop.Message.UNBOX.createNode();
        @Child private Node keysNode = com.oracle.truffle.api.interop.Message.KEYS.createNode();
        @Child private SetFixedAttributeNode namesAttrSetter = SetFixedAttributeNode.createNames();

        @TruffleBoundary
        public Object convert(TruffleObject obj) {
            class RStringWrapper extends TruffleObjectWrapper implements RAbstractStringVector {
                final TruffleObject object;

                RStringWrapper(int length, TruffleObject object) {
                    super(length);
                    this.object = object;
                }

                @Override
                public Object getDataAtAsObject(int index) {
                    return getDataAt(index);
                }

                @Override
                public String getDataAt(int index) {
                    Object value;
                    try {
                        value = ForeignAccess.sendRead(readNode, object, index);
                        if (value instanceof TruffleObject && ForeignAccess.sendIsBoxed(isBoxedNode, (TruffleObject) value)) {
                            value = ForeignAccess.sendUnbox(unboxNode, (TruffleObject) value);
                        }
                    } catch (UnknownIdentifierException | UnsupportedMessageException e) {
                        throw RInternalError.shouldNotReachHere(e);
                    }
                    return String.valueOf(value);
                }

                @Override
                public RStringVector materialize() {
                    throw RInternalError.shouldNotReachHere();
                }
            }
            try {
                if (ForeignAccess.sendHasSize(hasSizeNode, obj)) {
                    int size = (Integer) ForeignAccess.sendGetSize(getSizeNode, obj);
                    boolean allBoolean = true;
                    boolean allInteger = true;
                    boolean allNumber = true;
                    boolean allCharacter = true;
                    boolean allString = true;
                    for (int i = 0; i < size; i++) {
                        Object value = ForeignAccess.sendRead(readNode, obj, i);
                        if (value instanceof TruffleObject && ForeignAccess.sendIsBoxed(isBoxedNode, (TruffleObject) value)) {
                            value = ForeignAccess.sendUnbox(unboxNode, (TruffleObject) value);
                        }
                        allBoolean &= value instanceof Boolean;
                        allInteger &= value instanceof Integer;
                        allNumber &= value instanceof Number;
                        allCharacter &= value instanceof Character;
                        allString &= value instanceof String;
                    }
                    if (allBoolean) {
                        class RLogicalWrapper extends TruffleObjectWrapper implements RAbstractLogicalVector {

                            RLogicalWrapper(int length) {
                                super(length);
                            }

                            @Override
                            public Object getDataAtAsObject(int index) {
                                return getDataAt(index);
                            }

                            @Override
                            public byte getDataAt(int index) {
                                Object value;
                                try {
                                    value = ForeignAccess.sendRead(readNode, obj, index);
                                    if (value instanceof TruffleObject && ForeignAccess.sendIsBoxed(isBoxedNode, (TruffleObject) value)) {
                                        value = ForeignAccess.sendUnbox(unboxNode, (TruffleObject) value);
                                    }
                                } catch (UnknownIdentifierException | UnsupportedMessageException e) {
                                    throw RInternalError.shouldNotReachHere(e);
                                }
                                return RRuntime.asLogical((Boolean) value);
                            }

                            @Override
                            public RLogicalVector materialize() {
                                throw RInternalError.shouldNotReachHere();
                            }
                        }
                        return new RLogicalWrapper(size);
                    } else if (allInteger) {
                        class RIntWrapper extends TruffleObjectWrapper implements RAbstractIntVector {

                            RIntWrapper(int length) {
                                super(length);
                            }

                            @Override
                            public Object getDataAtAsObject(int index) {
                                return getDataAt(index);
                            }

                            @Override
                            public int getDataAt(int index) {
                                Object value;
                                try {
                                    value = ForeignAccess.sendRead(readNode, obj, index);
                                    if (value instanceof TruffleObject && ForeignAccess.sendIsBoxed(isBoxedNode, (TruffleObject) value)) {
                                        value = ForeignAccess.sendUnbox(unboxNode, (TruffleObject) value);
                                    }
                                } catch (UnknownIdentifierException | UnsupportedMessageException e) {
                                    throw RInternalError.shouldNotReachHere(e);
                                }
                                return (Integer) value;
                            }

                            @Override
                            public RIntVector materialize() {
                                throw RInternalError.shouldNotReachHere();
                            }
                        }
                        return new RIntWrapper(size);
                    } else if (allNumber) {
                        class RDoubleWrapper extends TruffleObjectWrapper implements RAbstractDoubleVector {

                            RDoubleWrapper(int length) {
                                super(length);
                            }

                            @Override
                            public Object getDataAtAsObject(int index) {
                                return getDataAt(index);
                            }

                            @Override
                            public double getDataAt(int index) {
                                Object value;
                                try {
                                    value = ForeignAccess.sendRead(readNode, obj, index);
                                    if (value instanceof TruffleObject && ForeignAccess.sendIsBoxed(isBoxedNode, (TruffleObject) value)) {
                                        value = ForeignAccess.sendUnbox(unboxNode, (TruffleObject) value);
                                    }
                                } catch (UnknownIdentifierException | UnsupportedMessageException e) {
                                    throw RInternalError.shouldNotReachHere(e);
                                }
                                return ((Number) value).doubleValue();
                            }

                            @Override
                            public RDoubleVector materialize() {
                                throw RInternalError.shouldNotReachHere();
                            }
                        }
                        return new RDoubleWrapper(size);
                    } else if (allString || allCharacter) {
                        return new RStringWrapper(size, obj);
                    } else {
                        class RListWrapper extends TruffleObjectWrapper implements RAbstractListVector {

                            RListWrapper(int length) {
                                super(length);
                            }

                            @Override
                            public Object getDataAtAsObject(int index) {
                                return getDataAt(index);
                            }

                            @Override
                            public Object getDataAt(int index) {
                                Object value;
                                try {
                                    value = ForeignAccess.sendRead(readNode, obj, index);
                                    if (value instanceof TruffleObject && ForeignAccess.sendIsBoxed(isBoxedNode, (TruffleObject) value)) {
                                        value = ForeignAccess.sendUnbox(unboxNode, (TruffleObject) value);
                                    }
                                } catch (UnknownIdentifierException | UnsupportedMessageException e) {
                                    throw RInternalError.shouldNotReachHere(e);
                                }
                                return value;
                            }

                            @Override
                            public RList materialize() {
                                throw RInternalError.shouldNotReachHere();
                            }
                        }
                        return new RListWrapper(size);
                    }
                }
                TruffleObject keys = (TruffleObject) ForeignAccess.send(keysNode, obj);
                if (keys != null) {
                    int size = (Integer) ForeignAccess.sendGetSize(getSizeNode, keys);
                    RAbstractStringVector abstractNames = new RStringWrapper(size, keys);
                    RStringVector names = RDataFactory.createStringVector(size);
                    for (int i = 0; i < size; i++) {
                        names.setDataAt(names.getInternalStore(), i, abstractNames.getDataAt(i));
                    }

                    class RListWrapper extends TruffleObjectWrapper implements RAbstractListVector {

                        RListWrapper(int length) {
                            super(length);
                            DynamicObject attrs = RAttributesLayout.createNames(names);
                            initAttributes(attrs);
                        }

                        @Override
                        public Object getDataAtAsObject(int index) {
                            return getDataAt(index);
                        }

                        @Override
                        public Object getDataAt(int index) {
                            Object value;
                            try {
                                value = ForeignAccess.sendRead(readNode, obj, names.getDataAt(index));
                                if (value instanceof TruffleObject && ForeignAccess.sendIsBoxed(isBoxedNode, (TruffleObject) value)) {
                                    value = ForeignAccess.sendUnbox(unboxNode, (TruffleObject) value);
                                }
                            } catch (UnknownIdentifierException | UnsupportedMessageException e) {
                                throw RInternalError.shouldNotReachHere(e);
                            }
                            return value;
                        }

                        @Override
                        public RStringVector getNames() {
                            return names;
                        }

                        @Override
                        public RList materialize() {
                            throw RInternalError.shouldNotReachHere();
                        }
                    }
                    return new RListWrapper(size);
                }
            } catch (InteropException e) {
                // nothing to do
            }
            return obj;
        }
    }

    public boolean isArray(Object o) {
        return RRuntime.fromLogical(isArrayBuiltIn.execute(o));
    }

    public boolean isList(Object o) {
        return RRuntime.fromLogical(isListBuiltIn.execute(o));
    }

    public boolean inherits(Object o, Object what) {
        return RRuntime.fromLogical((Byte) inheritsBuiltinBuiltIn.execute(o, what, false));
    }

    public boolean isS4(Object o) {
        return RRuntime.fromLogical(isS4BuiltIn.execute(o));
    }

    public boolean isObject(Object o) {
        return RRuntime.fromLogical(isObjectBuiltIn.execute(o));
    }

    public boolean isMethodDispatchOn() {
        return RRuntime.fromLogical(isMethodDispatchOnBuiltIn.execute());
    }

    public String castString(Object o) {
        return (String) castStringNode.executeString(o);
    }

    public Object boxPrimitive(Object o) {
        return boxPrimitiveNode.execute(o);
    }

    private Object convertTruffleObject(Object o) {
        if (RRuntime.isForeignObject(o)) {
            if (convertTruffleObject == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                convertTruffleObject = insert(new ConvertTruffleObjectNode());
            }
            return convertTruffleObject.convert((TruffleObject) o);
        }
        return null;
    }

    public String execute(Object o, Object digits, boolean quote, Object naPrint, Object printGap, boolean right, Object max, boolean useSource, boolean noOpt) {
        try {
            PrintParameters printParams = new PrintParameters(digits, quote, naPrint, printGap, right, max, useSource, noOpt);

            PrintContext printCtx = PrintContext.enter(this, printParams, RWriter::new);
            try {
                prettyPrint(o, printCtx);
                Object foreignObjectWrapper = convertTruffleObject(o);
                if (foreignObjectWrapper != null) {
                    prettyPrint(foreignObjectWrapper, printCtx);
                }
            } finally {
                PrintContext.leave();
            }
            return null;
        } catch (IOException ex) {
            throw RError.ioError(this, ex);
        }
    }

    @TruffleBoundary
    private static void prettyPrint(Object o, PrintContext printCtx) throws IOException {
        ValuePrinters.INSTANCE.print(o, printCtx);
        ValuePrinters.printNewLine(printCtx);
    }

    private abstract static class TruffleObjectWrapper extends RAttributeStorage implements RAbstractVector {

        private final int length;

        TruffleObjectWrapper(int length) {
            this.length = length;
        }

        @Override
        public RAbstractVector copy() {
            throw RInternalError.shouldNotReachHere();
        }

        @Override
        public RVector<?> copyResized(int size, boolean fillNA) {
            throw RInternalError.shouldNotReachHere();
        }

        @Override
        public RAbstractVector copyWithNewDimensions(int[] newDimensions) {
            throw RInternalError.shouldNotReachHere();
        }

        @Override
        public RVector<?> copyResizedWithDimensions(int[] newDimensions, boolean fillNA) {
            throw RInternalError.shouldNotReachHere();
        }

        @Override
        public RAbstractVector copyDropAttributes() {
            throw RInternalError.shouldNotReachHere();
        }

        @Override
        public RVector<?> createEmptySameType(int newLength, boolean newIsComplete) {
            throw RInternalError.shouldNotReachHere();
        }

        @Override
        public boolean isMatrix() {
            return false;
        }

        @Override
        public boolean isArray() {
            return false;
        }

        @Override
        public boolean checkCompleteness() {
            throw RInternalError.shouldNotReachHere();
        }

        @Override
        public void setComplete(boolean complete) {
            throw RInternalError.shouldNotReachHere();
        }

        @Override
        public boolean isComplete() {
            return false;
        }

        @Override
        public int getLength() {
            return length;
        }

        @Override
        public RAbstractContainer resize(int size) {
            throw RInternalError.shouldNotReachHere();
        }

        @Override
        public boolean hasDimensions() {
            return false;
        }

        @Override
        public int[] getDimensions() {
            return null;
        }

        @Override
        public void setDimensions(int[] newDimensions) {
            throw RInternalError.shouldNotReachHere();
        }

        @Override
        public RTypedValue getNonShared() {
            throw RInternalError.shouldNotReachHere();
        }

        @Override
        public RStringVector getNames() {
            return null;
        }

        @Override
        public final void setNames(RStringVector newNames) {
            throw RInternalError.shouldNotReachHere();
        }

        @Override
        public RList getDimNames() {
            return null;
        }

        @Override
        public void setDimNames(RList newDimNames) {
            throw RInternalError.shouldNotReachHere();
        }

        @Override
        public Object getRowNames() {
            return null;
        }

        @Override
        public void setRowNames(RAbstractVector rowNames) {
            throw RInternalError.shouldNotReachHere();
        }

        @Override
        public RType getRType() {
            return RType.Integer;
        }

        @Override
        public ForeignAccess getForeignAccess() {
            throw RInternalError.shouldNotReachHere();
        }
    }

    @TruffleBoundary
    public Object prettyPrint(Object v, WriterFactory wf) {
        PrintParameters printParams = new PrintParameters();
        printParams.setDefaults();
        printParams.setSuppressIndexLabels(true);
        PrintContext printCtx = PrintContext.enter(this, printParams, wf);
        try {
            ValuePrinters.INSTANCE.print(v, printCtx);
            return printCtx.output().getPrintReport();
        } catch (IOException ex) {
            throw RError.ioError(this, ex);
        } finally {
            PrintContext.leave();
        }
    }
}
