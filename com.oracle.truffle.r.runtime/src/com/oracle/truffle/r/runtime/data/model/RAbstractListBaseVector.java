/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.data.model;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.library.ExportMessage.Ignore;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary;
import com.oracle.truffle.r.runtime.interop.R2Foreign;

/**
 * The base class for list-like objects: {@link com.oracle.truffle.r.runtime.data.RExpression} and
 * {@link com.oracle.truffle.r.runtime.data.RList}. It can be useful in situations where the same
 * algorithm applies to those two data structures.
 * 
 * Note on sharing mode for list elements: by default the sharing state of elements in a list can be
 * inconsistent, e.g. a list referenced by one local variable may contain temporary vectors, or list
 * references by more variables (shared list) may contain non-shared elements. The sharing state of
 * the elements should be made consistent on reading from the list by the callers! When we read from
 * shared list, we make the element shared. When we read from non-shared list, we make the element
 * at least non-shared. There is no possible way, how a list can contain a non-shared element not
 * owned by it, given that any element of some other list must be first read into variable (the
 * extraction from list makes it at least non-shared, then the write makes it shared) and only then
 * it can be put inside another list. This is however not true for internal code, which may read
 * data from a list and then put it into another list, in such case it is responsibility of the code
 * to increment the refcount of such data. Consult also the documentation of
 * {@code ExtractListElement}, which is a node that can extract an element of a list or abstract
 * vector and put it in the consistent sharing state.
 */
@ExportLibrary(InteropLibrary.class)
public abstract class RAbstractListBaseVector extends RAbstractVector {

    @SuppressWarnings("static-method")
    @ExportMessage
    public boolean hasMembers() {
        return true;
    }

    @ExportMessage
    public Object getMembers(@SuppressWarnings("unused") boolean includeInternal) {
        RStringVector names = getNames();
        return names != null ? names : RDataFactory.createEmptyStringVector();
    }

    @ExportMessage
    public boolean isMemberReadable(String member,
                    @CachedLibrary(limit = DATA_LIB_LIMIT) VectorDataLibrary dataLib) {
        int idx = getElementIndexByName(member);
        return isArrayElementReadable(idx, dataLib);
    }

    @ExportMessage
    public boolean isMemberInvocable(String member,
                    @CachedLibrary(limit = DATA_LIB_LIMIT) VectorDataLibrary dataLib) {
        int idx = getElementIndexByName(member);
        return isArrayElementReadable(idx, dataLib) && getDataAt(idx) instanceof RFunction;
    }

    @ExportMessage
    public Object readMember(String member,
                    @CachedLibrary(limit = DATA_LIB_LIMIT) VectorDataLibrary dataLib,
                    @Cached.Exclusive @Cached() R2Foreign r2Foreign,
                    @Cached.Shared("unknownIdentifier") @Cached("createBinaryProfile()") ConditionProfile unknownIdentifier) throws UnknownIdentifierException {
        int idx = getElementIndexByName(member);
        if (unknownIdentifier.profile(!isArrayElementReadable(idx, dataLib))) {
            throw UnknownIdentifierException.create(member);
        }
        return r2Foreign.convert(getDataAt(idx));
    }

    @ExportMessage
    public Object invokeMember(String member, Object[] arguments,
                    @CachedLibrary(limit = DATA_LIB_LIMIT) VectorDataLibrary dataLib,
                    @Cached.Shared("unknownIdentifier") @Cached("createBinaryProfile()") ConditionProfile unknownIdentifier,
                    @Cached() RFunction.ExplicitCall c) throws UnknownIdentifierException, UnsupportedMessageException {
        int idx = getElementIndexByName(member);
        if (unknownIdentifier.profile(!isArrayElementReadable(idx, dataLib))) {
            throw UnknownIdentifierException.create(member);
        }
        Object f = getDataAt(idx);
        if (f instanceof RFunction) {
            return c.execute((RFunction) f, arguments);
        }
        throw UnsupportedMessageException.create();
    }

    @Override
    protected final boolean boxReadElements() {
        return true;
    }

    @Ignore
    @Override
    public Object getDataAtAsObject(Object store, int i) {
        return getDataAt(i);
    }

    public abstract Object getDataAt(int index);

    @SuppressWarnings("unused")
    public void setDataAt(Object store, int index, Object value) {
        throw CompilerDirectives.shouldNotReachHere();
    }

    @Override
    @Ignore // AbstractContainerLibrary
    public RAbstractVector createEmptySameType(int newLength, boolean newIsComplete) {
        return RDataFactory.createList(newLength);
    }

    @Override
    public Object getInternalManagedData() {
        throw RInternalError.shouldNotReachHere();
    }

    @Override
    public Object[] getDataCopy() {
        throw RInternalError.shouldNotReachHere();
    }

    @Override
    public Object[] getReadonlyData() {
        throw RInternalError.shouldNotReachHere();
    }

    @Override
    public void setLength(int l) {
        throw RInternalError.shouldNotReachHere();
    }

    @Override
    public int getTrueLength() {
        throw RInternalError.shouldNotReachHere();
    }

    @Override
    public void setTrueLength(int l) {
        throw RInternalError.shouldNotReachHere();
    }

    protected static final Object[] resizeData(Object[] newData, Object[] oldData, int oldDataLength, boolean fillNA) {
        if (newData.length > oldDataLength) {
            if (fillNA) {
                for (int i = oldDataLength; i < newData.length; i++) {
                    newData[i] = RNull.instance;
                }
            } else {
                assert oldDataLength > 0 : "cannot call resize on empty vector if fillNA == false";
                for (int i = oldData.length, j = 0; i < newData.length; ++i, j = Utils.incMod(j, oldData.length)) {
                    newData[i] = oldData[j];
                }
            }
        }
        return newData;
    }
}
