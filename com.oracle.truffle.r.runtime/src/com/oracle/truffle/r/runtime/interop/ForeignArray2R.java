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
import com.oracle.truffle.api.dsl.Fallback;
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
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

@ImportStatic({Message.class, RRuntime.class})
public abstract class ForeignArray2R extends RBaseNode {

    @Child protected Node hasSize = Message.HAS_SIZE.createNode();
    @Child private Foreign2R foreign2R;
    @Child private ForeignArray2R foreignArray2R;
    @Child private Node read;
    @Child private Node isNull;
    @Child private Node isBoxed;
    @Child private Node unbox;

    public static ForeignArray2R createForeignArray2R() {
        return ForeignArray2RNodeGen.create();
    }

    public abstract Object execute(Object obj, boolean recursive);

    @Specialization(guards = {"isForeignArray(obj)"})
    @TruffleBoundary
    public RAbstractVector doArray(TruffleObject obj, boolean recursive,
                    @Cached("GET_SIZE.createNode()") Node getSize) {
        try {
            CollectedElements ce = new CollectedElements();
            collectArrayElements(ce, obj, recursive, getSize);
            if (ce.elements.isEmpty()) {
                return RDataFactory.createList();
            }

            return asAbstractVector(ce);
        } catch (UnsupportedMessageException | UnknownIdentifierException e) {
            throw error(RError.Message.GENERIC, "error while converting array: " + e.getMessage());
        }
    }

    @Specialization(guards = "isJavaIterable(obj)")
    @TruffleBoundary
    protected RAbstractVector doJavaIterable(TruffleObject obj, boolean recursive,
                    @Cached("createExecute(0).createNode()") Node execute) {

        try {
            CollectedElements ce = new CollectedElements();
            ce = getIterableElements(ce, obj, recursive, execute);
            return asAbstractVector(ce);
        } catch (UnsupportedMessageException | UnknownIdentifierException | UnsupportedTypeException | ArityException e) {
            throw error(RError.Message.GENERIC, "error while casting external object to list: " + e.getMessage());
        }
    }

    @Fallback
    public Object doObject(Object obj, @SuppressWarnings("unused") boolean recursive) {
        return obj;
    }

    private void collectArrayElements(CollectedElements ce, TruffleObject obj, boolean recursive, Node getSize) throws UnsupportedMessageException, UnknownIdentifierException {
        int size = (int) ForeignAccess.sendGetSize(getSize, obj);
        if (size == 0) {
            return;
        }
        for (int i = 0; i < size; i++) {
            if (read == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                read = insert(Message.READ.createNode());
            }
            Object element = ForeignAccess.sendRead(read, obj, i);
            if (recursive && (isForeignArray(element, hasSize) || isJavaIterable(element))) {
                recurse(ce, element);
            } else {
                ce.elements.add(element2R(element, ce));
            }
        }
    }

    private CollectedElements getIterableElements(CollectedElements ce, TruffleObject obj, boolean recursive, Node execute)
                    throws UnknownIdentifierException, ArityException, UnsupportedMessageException, UnsupportedTypeException {
        if (read == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            read = insert(Message.READ.createNode());
        }
        TruffleObject itFunction = (TruffleObject) ForeignAccess.sendRead(read, obj, "iterator");
        TruffleObject it = (TruffleObject) ForeignAccess.sendExecute(execute, itFunction);
        TruffleObject hasNextFunction = (TruffleObject) ForeignAccess.sendRead(read, it, "hasNext");

        while ((boolean) ForeignAccess.sendExecute(execute, hasNextFunction)) {
            TruffleObject nextFunction = (TruffleObject) ForeignAccess.sendRead(read, it, "next");
            Object element = ForeignAccess.sendExecute(execute, nextFunction);
            if (recursive && (isJavaIterable(element) || isForeignArray(element, hasSize))) {
                recurse(ce, element);
            } else {
                ce.elements.add(element2R(element, ce));
            }
        }
        return ce;
    }

    private void recurse(CollectedElements ce, Object element) {
        if (foreignArray2R == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            foreignArray2R = insert(createForeignArray2R());
        }
        RAbstractVector result = (RAbstractVector) foreignArray2R.execute(element, true);
        for (int i = 0; i < result.getLength(); i++) {
            Object value = result.getDataAtAsObject(i);
            ce.elements.add(value);
        }
        ce.typeCheck.checkVector(result);
    }

    private Object element2R(Object value, CollectedElements ce) throws UnsupportedMessageException {
        Object unboxedValue = value;
        if (unboxedValue instanceof TruffleObject) {
            if (isNull == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                isNull = insert(Message.IS_NULL.createNode());
            }
            if (ForeignAccess.sendIsNull(isNull, (TruffleObject) unboxedValue)) {
                unboxedValue = RNull.instance;
            } else {
                if (isBoxed == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    isBoxed = insert(Message.IS_BOXED.createNode());
                }
                if (ForeignAccess.sendIsBoxed(isBoxed, (TruffleObject) unboxedValue)) {
                    if (unbox == null) {
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                        unbox = insert(Message.UNBOX.createNode());
                    }
                    unboxedValue = ForeignAccess.sendUnbox(unbox, (TruffleObject) unboxedValue);
                }
            }
        }
        ce.typeCheck.checkForeign(unboxedValue);

        if (foreign2R == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            foreign2R = insert(Foreign2RNodeGen.create());
        }
        return foreign2R.execute(unboxedValue);
    }

    /**
     * Converts the elements collected from a foreign array or java iterable into a vector or list.
     */
    public static RAbstractVector asAbstractVector(CollectedElements ce) {
        InteropTypeCheck.RType type = ce.typeCheck.getType();
        if (type == null) {
            return RDataFactory.createList(ce.elements.toArray(new Object[ce.elements.size()]));
        }
        int size = ce.elements.size();
        boolean complete = true;
        switch (type) {
            case BOOLEAN:
                byte[] bytes = new byte[size];
                for (int i = 0; i < size; i++) {
                    bytes[i] = ((Number) ce.elements.get(i)).byteValue();
                    complete &= RRuntime.isNA(bytes[i]);
                }
                return RDataFactory.createLogicalVector(bytes, complete);
            case DOUBLE:
                double[] doubles = new double[size];
                for (int i = 0; i < size; i++) {
                    doubles[i] = ((Number) ce.elements.get(i)).doubleValue();
                    complete &= RRuntime.isNA(doubles[i]);
                }
                return RDataFactory.createDoubleVector(doubles, complete);
            case INTEGER:
                int[] ints = new int[size];
                for (int i = 0; i < size; i++) {
                    ints[i] = ((Number) ce.elements.get(i)).intValue();
                    complete &= RRuntime.isNA(ints[i]);
                }
                return RDataFactory.createIntVector(ints, complete);
            case STRING:
                String[] strings = new String[size];
                for (int i = 0; i < size; i++) {
                    strings[i] = String.valueOf(ce.elements.get(i));
                    complete &= RRuntime.isNA(strings[i]);
                }
                return RDataFactory.createStringVector(strings, complete);
            default:
                assert false;
        }

        // type != null but no vector created - how comes?
        assert false : "did not handle properly: " + type;
        return RDataFactory.createList(ce.elements.toArray(new Object[ce.elements.size()]));
    }

    protected boolean isForeignArray(Object obj) {
        return RRuntime.isForeignObject(obj) && ForeignAccess.sendHasSize(hasSize, (TruffleObject) obj);
    }

    protected boolean isForeignVector(Object obj) {
        return isJavaIterable(obj) || isForeignArray(obj, hasSize);
    }

    public static boolean isForeignArray(Object obj, Node hasSize) {
        return RRuntime.isForeignObject(obj) && ForeignAccess.sendHasSize(hasSize, (TruffleObject) obj);
    }

    public static boolean isJavaIterable(Object obj) {
        return RRuntime.isForeignObject(obj) && JavaInterop.isJavaObject(Iterable.class, (TruffleObject) obj);
    }

    public static boolean isForeignVector(Object obj, Node hasSize) {
        return isJavaIterable(obj) || isForeignArray(obj, hasSize);
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

        public void checkForeign(Object value) {
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

        public void checkVector(RAbstractVector value) {
            if (value instanceof RAbstractLogicalVector) {
                setType(RType.BOOLEAN);
            } else if (value instanceof RAbstractIntVector) {
                setType(RType.INTEGER);
            } else if (value instanceof RAbstractDoubleVector) {
                setType(RType.DOUBLE);
            } else if (value instanceof RAbstractStringVector) {
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

    public static class CollectedElements {
        private InteropTypeCheck typeCheck = new InteropTypeCheck();
        private List<Object> elements = new ArrayList<>();

        public InteropTypeCheck getTypeCheck() {
            return typeCheck;
        }

        public void setTypeCheck(InteropTypeCheck typeCheck) {
            this.typeCheck = typeCheck;
        }

        public List<Object> getElements() {
            return elements;
        }

        public void setElements(List<Object> elements) {
            this.elements = elements;
        }

    }
}
