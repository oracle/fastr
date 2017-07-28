/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.interop;

import java.util.ArrayList;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.interop.java.JavaInterop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

@ImportStatic({Message.class, RRuntime.class})
public abstract class ForeignArray2R extends RBaseNode {

    @Child protected Node hasSize = Message.HAS_SIZE.createNode();
    @Child private Foreign2R foreign2R;
    @Child private Node read;
    @Child private Node isNull;
    @Child private Node isBoxed;
    @Child private Node unbox;

    public abstract Object execute(Object obj);

    @Specialization(guards = {"isArray(obj, hasSize)"})
    @TruffleBoundary
    public RAbstractVector doArray(TruffleObject obj,
                    @Cached("GET_SIZE.createNode()") Node getSize) {
        int size;
        try {
            size = (int) ForeignAccess.sendGetSize(getSize, obj);
            if (size == 0) {
                return RDataFactory.createList();
            }

            CollectedElements ce = getArrayElements(size, obj);
            return asAbstractVector(ce.elements, ce.typeCheck);
        } catch (UnsupportedMessageException | UnknownIdentifierException e) {
            throw error(RError.Message.GENERIC, "error while converting array: " + e.getMessage());
        }
    }

    @Specialization(guards = "isJavaIterable(obj)")
    @TruffleBoundary
    protected RAbstractVector doJavaIterable(TruffleObject obj,
                    @Cached("createExecute(0).createNode()") Node execute) {

        try {
            CollectedElements ce = getIterableElements(obj, execute);
            return asAbstractVector(ce.elements, ce.typeCheck);
        } catch (UnsupportedMessageException | UnknownIdentifierException | UnsupportedTypeException | ArityException e) {
            throw error(RError.Message.GENERIC, "error while casting external object to list: " + e.getMessage());
        }
    }

    @Specialization(guards = {"!isJavaIterable(obj)", "!isArray(obj, hasSize)"})
    public Object doObject(TruffleObject obj) {
        return obj;
    }

    @Specialization
    public Object doObject(Object obj) {
        return obj;
    }

    private CollectedElements getArrayElements(int size, TruffleObject obj) throws UnsupportedMessageException, UnknownIdentifierException {
        CollectedElements ce = new CollectedElements();
        ce.elements = new Object[size];
        for (int i = 0; i < size; i++) {
            if (read == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                read = insert(Message.READ.createNode());
            }
            Object element = ForeignAccess.sendRead(read, obj, i);
            ce.elements[i] = element2R(element, ce);
        }
        return ce;
    }

    private CollectedElements getIterableElements(TruffleObject obj, Node execute)
                    throws UnknownIdentifierException, ArityException, UnsupportedMessageException, UnsupportedTypeException {
        List<Object> elements = new ArrayList<>();
        if (read == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            read = insert(Message.READ.createNode());
        }
        TruffleObject itFunction = (TruffleObject) ForeignAccess.sendRead(read, obj, "iterator");
        TruffleObject it = (TruffleObject) ForeignAccess.sendExecute(execute, itFunction);
        TruffleObject hasNextFunction = (TruffleObject) ForeignAccess.sendRead(read, it, "hasNext");

        CollectedElements ce = new CollectedElements();
        while ((boolean) ForeignAccess.sendExecute(execute, hasNextFunction)) {
            TruffleObject nextFunction = (TruffleObject) ForeignAccess.sendRead(read, it, "next");
            Object element = ForeignAccess.sendExecute(execute, nextFunction);
            elements.add(element2R(element, ce));
        }
        ce.elements = elements.toArray(new Object[elements.size()]);
        return ce;
    }

    private Object element2R(Object value, CollectedElements ce) throws UnsupportedMessageException {
        if (value instanceof TruffleObject) {
            if (isNull == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                isNull = insert(Message.IS_NULL.createNode());
            }
            if (ForeignAccess.sendIsNull(isNull, (TruffleObject) value)) {
                value = RNull.instance;
            } else {
                if (isBoxed == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    isBoxed = insert(Message.IS_BOXED.createNode());
                }
                if (ForeignAccess.sendIsBoxed(isBoxed, (TruffleObject) value)) {
                    if (unbox == null) {
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                        unbox = insert(Message.UNBOX.createNode());
                    }
                    value = ForeignAccess.sendUnbox(unbox, (TruffleObject) value);
                }
            }
        }
        ce.typeCheck.check(value);

        if (foreign2R == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            foreign2R = insert(Foreign2RNodeGen.create());
        }
        return foreign2R.execute(value);
    }

    public static RAbstractVector asAbstractVector(Object[] elements, InteropTypeCheck typeCheck) {
        InteropTypeCheck.RType type = typeCheck.getType();
        if (type == null) {
            return RDataFactory.createList(elements);
        }
        int size = elements.length;
        // TODO how to deal with NAs?
        boolean complete = true;
        switch (type) {
            case BOOLEAN:
                byte[] bytes = new byte[size];
                for (int i = 0; i < size; i++) {
                    bytes[i] = ((Number) elements[i]).byteValue();
                    complete &= RRuntime.isNA(bytes[i]);
                }
                return RDataFactory.createLogicalVector(bytes, complete);
            case DOUBLE:
                double[] doubles = new double[size];
                for (int i = 0; i < size; i++) {
                    doubles[i] = ((Number) elements[i]).doubleValue();
                    complete &= RRuntime.isNA(doubles[i]);
                }
                return RDataFactory.createDoubleVector(doubles, complete);
            case INTEGER:
                int[] ints = new int[size];
                for (int i = 0; i < size; i++) {
                    ints[i] = ((Number) elements[i]).intValue();
                    complete &= RRuntime.isNA(ints[i]);
                }
                return RDataFactory.createIntVector(ints, complete);
            case STRING:
                String[] strings = new String[size];
                for (int i = 0; i < size; i++) {
                    strings[i] = String.valueOf(elements[i]);
                    complete &= RRuntime.isNA(strings[i]);
                }
                return RDataFactory.createStringVector(strings, complete);
            default:
                assert false;
        }
        return null;
    }

    protected boolean isArray(TruffleObject obj, Node hasSize) {
        return RRuntime.isForeignObject(obj) && ForeignAccess.sendHasSize(hasSize, obj);
    }

    protected boolean isJavaIterable(TruffleObject obj) {
        return RRuntime.isForeignObject(obj) && JavaInterop.isJavaObject(Iterable.class, obj);
    }

    public static class InteropTypeCheck {
        public enum RType {
            BOOLEAN,
            DOUBLE,
            INTEGER,
            STRING;
        }

        private RType type = null;
        private boolean sameRType = true;

        public void check(Object value) {
            if (value instanceof Boolean) {
                setType(RType.BOOLEAN);
            } else if (value instanceof Byte || value instanceof Integer || value instanceof Short) {
                setType(RType.INTEGER);
            } else if (value instanceof Double || value instanceof Float || value instanceof Long) {
                setType(RType.DOUBLE);
            } else if (value instanceof Character || value instanceof String) {
                setType(RType.STRING);
            } else {
                this.type = null;
                sameRType = false;
            }
        }

        private void setType(RType check) {
            if (sameRType && this.type == null) {
                this.type = check;
            } else if (this.type != check) {
                this.type = null;
                sameRType = false;
            }
        }

        public RType getType() {
            return sameRType ? type : null;
        }
    }

    private class CollectedElements {
        Object[] elements;
        InteropTypeCheck typeCheck = new InteropTypeCheck();
    }
}
