/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.data;

import java.util.ArrayDeque;
import java.util.HashSet;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractRawVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.env.REnvironment;

/**
 * Support for the sizing of the objects that flow through the interpreter, i.e., mostly
 * {@link RTypedValue}, but also including scalar types like {@code String}.
 *
 */
public class RObjectSize {
    public static final int INT_SIZE = 32;
    public static final int DOUBLE_SIZE = 64;
    public static final int BYTE_SIZE = 8;

    private static final int CHAR_SIZE = 16;
    private static final int OBJECT_SIZE = 64;

    /**
     * Returns an estimate of the size of the this object in bits, excluding the size of any
     * object-valued fields. Evidently this is a snapshot and the size can change as, e.g.,
     * attributes are added/removed.
     *
     * If called immediately after creation by {@link RDataFactory} provides an approximation of the
     * incremental memory usage of the system.
     */
    @TruffleBoundary
    public static long getObjectSize(Object obj) {
        return getObjectSizeImpl(obj);
    }

    /**
     * Returns an estimate of the size of the this object in bits, including the size of any
     * object-valued fields, recursively. Evidently this is a snapshot and the size can change as,
     * e.g., attributes are added/removed.
     */
    @TruffleBoundary
    public static long getRecursiveObjectSize(Object target) {
        ArrayDeque<Object> stack = new ArrayDeque<>();
        HashSet<Object> visited = new HashSet<>();
        stack.push(target);
        visited.add(target);

        long result = 0;
        while (!stack.isEmpty()) {
            Object obj = stack.pop();
            result += getObjectSizeImpl(obj);
            if (obj != null) {
                pushReferences(stack, visited, obj);
            }
        }
        return result;
    }

    private static void pushReferences(ArrayDeque<Object> stack, HashSet<Object> visited, Object obj) {
        if (obj instanceof RAttributable) {
            DynamicObject attrs = ((RAttributable) obj).getAttributes();
            if (attrs != null) {
                Shape shape = attrs.getShape();
                for (Property prop : shape.getProperties()) {
                    Object propVal = prop.get(attrs, shape);
                    pushIfNotPresent(stack, visited, propVal);
                }
            }
        }
        if (obj instanceof RAbstractListVector) {
            RAbstractListVector list = (RAbstractListVector) obj;
            for (int i = 0; i < list.getLength(); i++) {
                pushIfNotPresent(stack, visited, list.getDataAt(i));
            }
        } else if (obj instanceof RArgsValuesAndNames) {
            RArgsValuesAndNames args = (RArgsValuesAndNames) obj;
            for (int i = 0; i < args.getLength(); i++) {
                pushIfNotPresent(stack, visited, args.getArgument(i));
            }
        }
        // Note: environments are ignored
    }

    private static void pushIfNotPresent(ArrayDeque<Object> stack, HashSet<Object> visited, Object obj) {
        if (!visited.contains(obj)) {
            stack.push(obj);
            visited.add(obj);
        }
    }

    private static long getObjectSizeImpl(Object obj) {
        // Note: if this gets too complex, it may be replaced by a system of providers or getSize
        // abstract method on RTypedValue. For now, we do not want to add yet another abstract
        // method to already complicated hierarchy and providers would only mean OO version of the
        // same code below.
        if (obj == null) {
            return 0;
        }
        // Primitive types:
        if (obj instanceof Integer) {
            return INT_SIZE;
        } else if (obj instanceof Double) {
            return DOUBLE_SIZE;
        } else if (obj instanceof Byte) {
            return BYTE_SIZE;
        } else if (obj instanceof String) {
            return CHAR_SIZE * ((String) obj).length();
        }
        // Check that we have RTypedValue:
        if (!(obj instanceof RTypedValue)) {
            // We ignore objects from other languages for now
            if (!(obj instanceof TruffleObject)) {
                reportWarning(obj);
            }
            return 0;
        }
        long attributesSize = 0;
        if (obj instanceof RAttributable) {
            DynamicObject attrs = ((RAttributable) obj).getAttributes();
            if (attrs != null) {
                attributesSize = attrs.size() * OBJECT_SIZE;
            }
        }
        // Individual RTypedValues:
        if (obj instanceof RPromise || obj instanceof RAbstractListVector || obj instanceof REnvironment || obj instanceof RExternalPtr || obj instanceof RFunction) {
            // promise: there is no value allocated yet, we may use the size of the closure
            return OBJECT_SIZE + attributesSize;
        } else if (obj instanceof RStringSequence) {
            RStringSequence seq = (RStringSequence) obj;
            if (seq.getLength() == 0) {
                return OBJECT_SIZE + INT_SIZE * 2;  // we cannot get prefix/suffix...
            } else {
                return OBJECT_SIZE + seq.getDataAt(0).length() * CHAR_SIZE;
            }
        } else if (obj instanceof RSequence) {
            // count: start, stride, length
            return OBJECT_SIZE + 2 * getElementSize((RAbstractVector) obj) + INT_SIZE + attributesSize;
        } else if (obj instanceof RAbstractStringVector) {
            RAbstractStringVector strVec = (RAbstractStringVector) obj;
            long result = OBJECT_SIZE;
            for (int i = 0; i < strVec.getLength(); i++) {
                result += strVec.getDataAt(i).length() * CHAR_SIZE;
            }
            return result + attributesSize;
        } else if (obj instanceof RAbstractVector) {
            RAbstractVector vec = (RAbstractVector) obj;
            return OBJECT_SIZE + getElementSize(vec) * vec.getLength() + attributesSize;
        } else if (obj instanceof RScalar) {
            // E.g. singletons RNull or REmpty. RInteger, RLogical etc. already caught by
            // RAbstractVector branch
            return 0;
        } else if (obj instanceof RArgsValuesAndNames) {
            return getArgsAndValuesSize((RArgsValuesAndNames) obj);
        } else {
            reportWarning(obj);
            return OBJECT_SIZE;
        }
    }

    private static int getElementSize(RAbstractVector vector) {
        if (vector instanceof RAbstractDoubleVector) {
            return DOUBLE_SIZE;
        } else if (vector instanceof RAbstractIntVector) {
            return INT_SIZE;
        } else if (vector instanceof RAbstractLogicalVector || vector instanceof RAbstractRawVector) {
            return BYTE_SIZE;
        } else if (vector instanceof RAbstractComplexVector) {
            return DOUBLE_SIZE * 2;
        }
        reportWarning(vector);
        return INT_SIZE;
    }

    private static long getArgsAndValuesSize(RArgsValuesAndNames args) {
        long result = OBJECT_SIZE + args.getLength() * OBJECT_SIZE;
        ArgumentsSignature signature = args.getSignature();
        for (int i = 0; i < signature.getLength(); i++) {
            String name = signature.getName(i);
            if (name != null) {
                result += name.length() * CHAR_SIZE;
            }
        }
        return result;
    }

    private static void reportWarning(Object obj) {
        RError.warning(RError.NO_CALLER, Message.UNEXPECTED_OBJ_IN_SIZE, obj.getClass().getSimpleName());
    }
}
