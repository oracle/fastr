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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

@ImportStatic({Message.class, RRuntime.class})
public abstract class ForeignArray2R extends RBaseNode {

    @Child private Foreign2R foreign2R;
    @Child private Node hasSize = Message.HAS_SIZE.createNode();
    @Child private Node getSize;
    @Child private Node read;
    @Child private Node isNull;
    @Child private Node isBoxed;
    @Child private Node unbox;

    private final ConditionProfile isArrayProfile = ConditionProfile.createBinaryProfile();

    public abstract Object execute(Object obj);

    @Specialization(guards = "isForeignObject(obj)")
    @TruffleBoundary
    public Object array2r(TruffleObject obj) {
        if (!isArrayProfile.profile(ForeignAccess.sendHasSize(hasSize, obj))) {
            return obj;
        }
        if (getSize == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            getSize = insert(Message.GET_SIZE.createNode());
        }
        int size;
        try {
            size = (int) ForeignAccess.sendGetSize(getSize, obj);
            if (size == 0) {
                return RDataFactory.createList();
            }
            Object[] elements = new Object[size];
            boolean allBoolean = true;
            boolean allInteger = true;
            boolean allDouble = true;
            boolean allString = true;
            for (int i = 0; i < size; i++) {
                if (read == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    read = insert(Message.READ.createNode());
                }
                Object element = ForeignAccess.sendRead(read, obj, i);
                if (element instanceof TruffleObject) {
                    if (isNull == null) {
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                        isNull = insert(Message.IS_NULL.createNode());
                    }
                    if (ForeignAccess.sendIsNull(isNull, (TruffleObject) element)) {
                        element = null;
                    } else {
                        if (isBoxed == null) {
                            CompilerDirectives.transferToInterpreterAndInvalidate();
                            isBoxed = insert(Message.IS_BOXED.createNode());
                        }
                        if (ForeignAccess.sendIsBoxed(isBoxed, (TruffleObject) element)) {
                            if (unbox == null) {
                                CompilerDirectives.transferToInterpreterAndInvalidate();
                                unbox = insert(Message.UNBOX.createNode());
                            }
                            element = ForeignAccess.sendIsBoxed(unbox, (TruffleObject) element);
                        }
                    }
                }
                allBoolean &= element instanceof Boolean;
                allInteger &= element instanceof Byte || element instanceof Integer || element instanceof Short;
                allDouble &= element instanceof Double || element instanceof Float || element instanceof Long;
                allString &= element instanceof Character || element instanceof String;

                if (foreign2R == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    foreign2R = insert(Foreign2RNodeGen.create());
                }
                elements[i] = foreign2R.execute(element);
            }

            if (allBoolean) {
                byte[] ret = new byte[size];
                for (int i = 0; i < size; i++) {
                    ret[i] = ((Number) elements[i]).byteValue();
                }
                return RDataFactory.createLogicalVector(ret, true);
            }
            if (allInteger) {
                int[] ret = new int[size];
                for (int i = 0; i < size; i++) {
                    ret[i] = ((Number) elements[i]).intValue();
                }
                return RDataFactory.createIntVector(ret, true);
            }
            if (allDouble) {
                double[] ret = new double[size];
                for (int i = 0; i < size; i++) {
                    ret[i] = ((Number) elements[i]).doubleValue();
                }
                return RDataFactory.createDoubleVector(ret, true);
            }
            if (allString) {
                String[] ret = new String[size];
                for (int i = 0; i < size; i++) {
                    ret[i] = String.valueOf(elements[i]);
                }
                return RDataFactory.createStringVector(ret, true);
            }
            return RDataFactory.createList(elements);
        } catch (UnsupportedMessageException | UnknownIdentifierException e) {
            throw error(RError.Message.GENERIC, "error while converting array: " + e.getMessage());
        }
    }

    @Fallback
    public Object object2r(Object obj) {
        return obj;
    }

}
