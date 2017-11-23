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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RForeignBooleanWrapper;
import com.oracle.truffle.r.runtime.data.RForeignDoubleWrapper;
import com.oracle.truffle.r.runtime.data.RForeignIntWrapper;
import com.oracle.truffle.r.runtime.data.RForeignListWrapper;
import com.oracle.truffle.r.runtime.data.RForeignNamedListWrapper;
import com.oracle.truffle.r.runtime.data.RForeignStringWrapper;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;

public final class TruffleObjectConverter {

    private Node hasSizeNode = com.oracle.truffle.api.interop.Message.HAS_SIZE.createNode();
    private Node getSizeNode = com.oracle.truffle.api.interop.Message.GET_SIZE.createNode();
    private Node readNode = com.oracle.truffle.api.interop.Message.READ.createNode();
    private Node isBoxedNode = com.oracle.truffle.api.interop.Message.IS_BOXED.createNode();
    private Node unboxNode = com.oracle.truffle.api.interop.Message.UNBOX.createNode();
    private Node keysNode = com.oracle.truffle.api.interop.Message.KEYS.createNode();

    public Node[] getSubNodes() {
        return new Node[]{hasSizeNode, getSizeNode, readNode, isBoxedNode, unboxNode, keysNode};
    }

    @TruffleBoundary
    public Object convert(TruffleObject obj) {
        try {
            if (ForeignAccess.sendHasSize(hasSizeNode, obj)) {
                int size = (Integer) ForeignAccess.sendGetSize(getSizeNode, obj);
                ForeignArray2R.InteropTypeCheck typeCheck = new ForeignArray2R.InteropTypeCheck();
                for (int i = 0; i < size; i++) {
                    Object value = ForeignAccess.sendRead(readNode, obj, i);
                    if (value instanceof TruffleObject && ForeignAccess.sendIsBoxed(isBoxedNode, (TruffleObject) value)) {
                        value = ForeignAccess.sendUnbox(unboxNode, (TruffleObject) value);
                    }
                    if (typeCheck.checkForeign(value) == ForeignArray2R.InteropTypeCheck.RType.NONE) {
                        break;
                    }
                }
                switch (typeCheck.getType()) {
                    case BOOLEAN:
                        return new RForeignBooleanWrapper(obj);
                    case INTEGER:
                        return new RForeignIntWrapper(obj);
                    case DOUBLE:
                        return new RForeignDoubleWrapper(obj);
                    case STRING:
                        return new RForeignStringWrapper(obj);
                    case NONE:
                        return new RForeignListWrapper(obj);
                    default:
                        throw RInternalError.shouldNotReachHere();
                }
            }
            TruffleObject keys = (TruffleObject) ForeignAccess.send(keysNode, obj);
            if (keys != null) {
                int size = (Integer) ForeignAccess.sendGetSize(getSizeNode, keys);
                RAbstractStringVector abstractNames = new RForeignStringWrapper(keys);
                String[] namesData = new String[size];
                boolean namesComplete = true;
                for (int i = 0; i < size; i++) {
                    namesData[i] = abstractNames.getDataAt(i);
                    namesComplete &= RRuntime.isNA(namesData[i]);
                }
                RStringVector names = RDataFactory.createStringVector(namesData, namesComplete);

                return new RForeignNamedListWrapper(obj, names);
            }
        } catch (InteropException e) {
            // nothing to do
        }
        return obj;
    }
}
