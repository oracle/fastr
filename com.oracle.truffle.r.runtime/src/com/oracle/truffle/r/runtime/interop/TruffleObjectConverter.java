/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.interop;

import java.util.ArrayList;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.RStringVector;

public final class TruffleObjectConverter {

    public static final Object UNREADABLE = new Object();

    public static Node[] getSubNodes() {
        return new Node[]{getInterop(), Foreign2R.getUncached()};
    }

    @TruffleBoundary
    public static Object convert(TruffleObject obj) {
        try {
            if (getInterop().hasArrayElements(obj)) {
                int size = RRuntime.getForeignArraySize(obj, getInterop());
                ForeignTypeCheck typeCheck = new ForeignTypeCheck();
                for (int i = 0; i < size; i++) {
                    Object value = getInterop().readArrayElement(obj, i);
                    if (typeCheck.check(Foreign2R.getUncached().convert(value)) == RType.List) {
                        break;
                    }
                }
                switch (typeCheck.getType()) {
                    case Logical:
                        return RLogicalVector.createForeignWrapper(obj);
                    case Integer:
                        return RIntVector.createForeignWrapper(obj);
                    case Double:
                        return RDoubleVector.createForeignWrapper(obj);
                    case Character:
                        return RStringVector.createForeignWrapper(obj, size);
                    case List:
                    case Null:
                        return RList.createForeignWrapper(obj, size);
                    default:
                        throw RInternalError.shouldNotReachHere();
                }
            }

            try {
                ArrayList<String> names = new ArrayList<>();
                ArrayList<Object> values = new ArrayList<>();
                if (!ConvertForeignObjectNode.isForeignArray(obj, getInterop())) {
                    TruffleObject classStatic = ToJavaStaticNode.getUncached().execute(obj);
                    if (classStatic != null) {
                        readMembers(classStatic, names, values);
                    }
                }

                readMembers(obj, names, values);
                RStringVector namesVec = RDataFactory.createStringVector(names.toArray(new String[0]), true);
                return RDataFactory.createList(values.toArray(), namesVec);
            } catch (InteropException e) {
                throw RInternalError.shouldNotReachHere(e);
            }

        } catch (InteropException e) {
            // nothing to do
        }
        return obj;
    }

    private static void readMembers(TruffleObject obj, ArrayList<String> names, ArrayList<Object> values)
                    throws InteropException {
        InteropLibrary interop = getInterop();
        Object members = interop.getMembers(obj);
        int size = RRuntime.getForeignArraySize(members, interop);
        for (int i = 0; i < size; i++) {
            String memberName = interop.asString(interop.readArrayElement(members, i));
            values.add(read(interop, obj, memberName));
            names.add(memberName);
        }
    }

    private static Object read(InteropLibrary interop, TruffleObject obj, String memberName) throws UnsupportedMessageException, UnknownIdentifierException {
        if (interop.isMemberReadable(obj, memberName)) {
            return Foreign2R.getUncached().convert(interop.readMember(obj, memberName));
        } else {
            return UNREADABLE;
        }
    }

    private static InteropLibrary getInterop() {
        return InteropLibrary.getFactory().getUncached();
    }
}
