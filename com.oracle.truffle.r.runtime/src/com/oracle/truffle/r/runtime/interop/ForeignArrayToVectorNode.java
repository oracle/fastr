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
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import static com.oracle.truffle.r.runtime.interop.ConvertForeignObjectNode.isForeignArray;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
import java.util.ArrayList;
import java.util.List;

/**
 * Copies the foreign array elements into an atomic vector.<br>
 * <b>Note</b> that it has to be assured by the caller that the array is homogenous and that the
 * provided type corresponds to the arrays elements.
 */
@ImportStatic({Message.class, RRuntime.class, ConvertForeignObjectNode.class, RType.class})
public abstract class ForeignArrayToVectorNode extends RBaseNode {

    @Child protected Node hasSize = Message.HAS_SIZE.createNode();
    @Child Node getSize = Message.GET_SIZE.createNode();

    @Child protected Node read;
    @Child protected Foreign2R foreign2R;
    @Child protected ForeignArrayToVectorNode recurse;

    protected abstract List<Object> execute(Object obj, boolean recursive, List<Object> elements);

    public static ForeignArrayToVectorNode create() {
        return ForeignArrayToVectorNodeGen.create();
    }

    /**
     * Simply copies recursively and the array elements into a vector of the given type. Dimensions
     * will be ignored.
     * 
     * @param obj foreign array
     * @param type the vector type
     * @return a vector
     */
    public RAbstractVector toVector(TruffleObject obj, RType type) {
        return toVector(obj, true, type, null, true);
    }

    /**
     * Copies the array elements into a vector of the given type.
     * 
     * @param obj foreign array
     * @param recursive resolve recursively
     * @param type the vector type
     * @param dims dimensions determine the elements positioning in the resulting vector (by
     *            column), no mater if <copy>dropDimensions</code> is set or not
     * @param dropDimensions if <code>true</code> dimensions attribute will be set on the resulting
     *            vector, otherwise not
     * @return a vector
     */
    RAbstractVector toVector(TruffleObject obj, boolean recursive, RType type, int[] dims, boolean dropDimensions) {
        List<Object> res = execute(obj, recursive, null);
        assert type != RType.List;
        return ConvertForeignObjectNode.asAbstractVector(res.toArray(new Object[res.size()]), dims, type, dropDimensions);
    }

    @Specialization(guards = {"isForeignArray(obj, hasSize)"})
    @CompilerDirectives.TruffleBoundary
    protected List<Object> copyArray(TruffleObject obj, boolean recursive, List<Object> elements) {
        try {
            List<Object> arrayElements = elements == null ? new ArrayList<>() : elements;

            int size = (int) ForeignAccess.sendGetSize(getSize, obj);

            if (size == 0) {
                return arrayElements;
            }
            for (int i = 0; i < size; i++) {
                Object element = ForeignAccess.sendRead(getRead(), obj, i);
                element = getForeign2R().execute(element);
                if (recursive && (isForeignArray(element, hasSize))) {
                    recurse(element, arrayElements);
                } else {
                    arrayElements.add(element);
                }
            }
            return arrayElements;

        } catch (UnsupportedMessageException | UnknownIdentifierException e) {
            throw error(RError.Message.GENERIC, "error while converting array: " + e.getMessage());
        }
    }

    private Object recurse(Object element, List<Object> elements) {
        if (recurse == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            recurse = insert(create());
        }
        return recurse.execute(element, true, elements);
    }

    @Fallback
    public List<Object> fallback(@SuppressWarnings("unused") Object obj, @SuppressWarnings("unused") boolean recursive, @SuppressWarnings("unused") List<Object> elements) {
        throw RInternalError.shouldNotReachHere();
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
