/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import static com.oracle.truffle.r.runtime.interop.ConvertForeignObjectNode.isForeignArray;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
import java.util.ArrayList;
import java.util.List;

@ImportStatic({Message.class, RRuntime.class, ConvertForeignObjectNode.class})
public abstract class InspectForeignArrayNode extends RBaseNode {

    @Child protected Node hasSize = Message.HAS_SIZE.createNode();
    @Child Node getSize = Message.GET_SIZE.createNode();

    @Child protected Node read;
    @Child protected InspectForeignArrayNode inspectTruffleObject;
    @Child protected Foreign2R foreign2R;

    public static InspectForeignArrayNode create() {
        return InspectForeignArrayNodeGen.create();
    }

    /**
     * Determines the target R type and dimensions of a foreign array.
     * 
     * @param obj
     * @return ArrayInfo
     */
    public ArrayInfo getArrayInfo(TruffleObject obj) {
        ArrayInfo info = new ArrayInfo();
        if (execute(obj, true, info, 0)) {
            return info;
        }
        return null;
    }

    protected abstract boolean execute(Object obj, boolean recursive, ArrayInfo data, int depth);

    @Specialization(guards = {"isForeignArray(obj, hasSize)"})
    @CompilerDirectives.TruffleBoundary
    protected boolean inspectArray(TruffleObject obj, boolean recursive, ArrayInfo data, int depth) {
        try {
            ArrayInfo arrayInfo = data == null ? new ArrayInfo() : data;
            int size = (int) ForeignAccess.sendGetSize(getSize, obj);

            arrayInfo.addDimension(depth, size);

            if (size > 0) {
                for (int i = 0; i < size; i++) {
                    Object element = ForeignAccess.sendRead(getRead(), obj, i);
                    boolean isArray = isForeignArray(element, hasSize);

                    if (recursive && isArray) {
                        if (!recurse(arrayInfo, element, depth)) {
                            return false;
                        }
                    } else if (!recursive && isArray) {
                        arrayInfo.typeCheck.check(getForeign2R().execute(element));
                        arrayInfo.canUseDims = false;
                        // it is already clear, that this will result in a flat list,
                        // we do not need to inspect the remaining dimensions or types
                        return false;
                    } else {
                        RType elementType = arrayInfo.typeCheck.check(getForeign2R().execute(element));
                        if (elementType == RType.List) {
                            return false;
                        }
                    }
                }
            }
            return true;
        } catch (UnsupportedMessageException | UnknownIdentifierException e) {
            throw error(RError.Message.GENERIC, "error while converting array: " + e.getMessage());
        }
    }

    @Fallback
    protected boolean fallback(@SuppressWarnings("unused") Object obj, @SuppressWarnings("unused") boolean recursive, @SuppressWarnings("unused") ArrayInfo data,
                    @SuppressWarnings("unused") int depth) {
        return false;
    }

    private boolean recurse(ArrayInfo arrayInfo, Object element, int depth) {
        if (inspectTruffleObject == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            inspectTruffleObject = insert(create());
        }
        return inspectTruffleObject.execute(element, true, arrayInfo, depth + 1);
    }

    public static final class ArrayInfo {
        private final ForeignTypeCheck typeCheck = new ForeignTypeCheck();
        private final List<Integer> dims = new ArrayList<>();

        private boolean canUseDims = false;

        RType getType() {
            return typeCheck.getType();
        }

        @CompilerDirectives.TruffleBoundary
        public int[] getDims() {
            return isRectMultiDim() ? dims.stream().mapToInt((i) -> i.intValue()).toArray() : null;
        }

        private boolean isRectMultiDim() {
            return canUseDims && dims.size() > 1;
        }

        boolean isOneDim() {
            return canUseDims && dims.size() == 1;
        }

        void addDimension(int depth, int size) {
            if (dims.isEmpty()) {
                canUseDims = true; // at least one dimension was added
            }
            if (dims.size() == depth) {
                dims.add(size);
            } else if (depth < dims.size()) {
                if (dims.get(depth) != size) {
                    // had previously on the same depth an array with different length
                    // -> not rectangular, skip the dimensions
                    canUseDims = false;
                }
            }
        }
    }

    protected Node getRead() {
        if (read == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            read = insert(Message.READ.createNode());
        }
        return read;
    }

    protected Foreign2R getForeign2R() {
        if (foreign2R == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            foreign2R = insert(Foreign2RNodeGen.create());
        }
        return foreign2R;
    }
}
