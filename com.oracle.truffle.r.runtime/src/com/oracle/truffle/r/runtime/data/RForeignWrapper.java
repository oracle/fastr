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
package com.oracle.truffle.r.runtime.data;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

public abstract class RForeignWrapper implements RAbstractVector {

    protected static final Node GET_SIZE = Message.GET_SIZE.createNode();
    protected static final Node READ = Message.READ.createNode();

    protected final TruffleObject delegate;

    protected RForeignWrapper(TruffleObject delegate) {
        this.delegate = delegate;
    }

    @Override
    @TruffleBoundary
    public final int getLength() {
        try {
            return (int) ForeignAccess.sendGetSize(GET_SIZE, delegate);
        } catch (UnsupportedMessageException e) {
            throw RInternalError.shouldNotReachHere(e);
        }
    }

    @Override
    public final RAbstractContainer resize(int size) {
        return materialize().resize(size);
    }

    @Override
    public final boolean isComplete() {
        return true;
    }

    @Override
    public final void setComplete(boolean complete) {
        // sequences are always complete
    }

    @Override
    public final boolean hasDimensions() {
        return false;
    }

    @Override
    public final int[] getDimensions() {
        return null;
    }

    @Override
    public final void setDimensions(int[] newDimensions) {
        throw RInternalError.shouldNotReachHere();
    }

    @Override
    public final RAbstractVector copy() {
        throw RInternalError.shouldNotReachHere();
    }

    @Override
    public final RAbstractVector copyDropAttributes() {
        throw RInternalError.shouldNotReachHere();
    }

    @Override
    public final RAbstractVector copyWithNewDimensions(int[] newDimensions) {
        throw RInternalError.shouldNotReachHere();
    }

    @Override
    public final RStringVector getNames() {
        return null;
    }

    @Override
    public final void setNames(RStringVector newNames) {
        // should only be used on materialized sequence
        throw RInternalError.shouldNotReachHere();
    }

    @Override
    public final RList getDimNames() {
        return null;
    }

    @Override
    public final void setDimNames(RList newDimNames) {
        // should only be used on materialized sequence
        throw RInternalError.shouldNotReachHere();
    }

    @Override
    public final Object getRowNames() {
        return RNull.instance;
    }

    @Override
    public final void setRowNames(RAbstractVector rowNames) {
        // should only be used on materialized sequence
        throw RInternalError.shouldNotReachHere();
    }

    @Override
    public final DynamicObject initAttributes() {
        throw RInternalError.shouldNotReachHere();
    }

    @Override
    public final void initAttributes(DynamicObject newAttributes) {
        throw RInternalError.shouldNotReachHere();
    }

    @Override
    public final DynamicObject getAttributes() {
        return null;
    }

    @Override
    public final boolean isMatrix() {
        return false;
    }

    @Override
    public final boolean isArray() {
        return false;
    }

    @Override
    public final boolean isObject() {
        return false;
    }

    @Override
    public final RTypedValue getNonShared() {
        return materialize().getNonShared();
    }

    @Override
    public final int getTypedValueInfo() {
        return 0;
    }

    @Override
    public final void setTypedValueInfo(int value) {
        throw RInternalError.shouldNotReachHere();
    }

    @Override
    public final boolean isS4() {
        return false;
    }

    @Override
    public final Object getInternalStore() {
        return delegate;
    }

    @Override
    public final RVector<?> copyResized(int size, boolean fillNA) {
        throw RInternalError.shouldNotReachHere();
    }

    @Override
    public final RVector<?> copyResizedWithDimensions(int[] newDimensions, boolean fillNA) {
        throw RInternalError.shouldNotReachHere();
    }

    @Override
    public final RVector<?> createEmptySameType(int newLength, boolean newIsComplete) {
        throw RInternalError.shouldNotReachHere();
    }
}
