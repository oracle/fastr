/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.data;

import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.NativeDataAccess.ToNativeNode;

/**
 * <p>
 * This class enables that foreign objects be passed from R to the native code and back.
 * </p>
 * <p>
 * Meant to be used only in {@link com.oracle.truffle.r.runtime.ffi.FFIWrapNode} and
 * {@link com.oracle.truffle.r.runtime.ffi.FFIUnwrapNode} together with
 * {@link RBaseObject#isPointer() }, {@link RBaseObject#asPointer()} and
 * {@link RBaseObject#toNative(ToNativeNode)}. Remaining interop messages are delegated to
 * {@link InteropLibrary} for the case that some some FastR specific native code should try doing
 * interop calls.
 * </p>
 * 
 * 
 */
@ExportLibrary(InteropLibrary.class)
public final class RForeignObjectWrapper extends RBaseObject implements RForeignVectorWrapper {

    protected final TruffleObject delegate;

    public RForeignObjectWrapper(TruffleObject delegate) {
        this.delegate = delegate;
    }

    public TruffleObject getDelegate() {
        return delegate;
    }

    @Override
    public String toString() {
        return RRuntime.NULL;
    }

    @Override
    public RType getRType() {
        return RType.TruffleObject;
    }

    @ExportMessage
    boolean isNull(@CachedLibrary("this.delegate") InteropLibrary interop) {
        return interop.isNull(delegate);
    }

    @ExportMessage
    boolean isInstantiable(@CachedLibrary("this.delegate") InteropLibrary interop) {
        return interop.isInstantiable(delegate);
    }

    @ExportMessage
    Object instantiate(Object[] arguments,
                    @CachedLibrary("this.delegate") InteropLibrary interop) throws UnsupportedTypeException, ArityException, UnsupportedMessageException {
        return interop.instantiate(delegate, arguments);
    }

    @ExportMessage
    boolean hasArrayElements(@CachedLibrary("this.delegate") InteropLibrary interop) {
        return interop.hasArrayElements(delegate);
    }

    @ExportMessage
    long getArraySize(@CachedLibrary("this.delegate") InteropLibrary interop) throws UnsupportedMessageException {
        return interop.getArraySize(delegate);
    }

    @ExportMessage
    boolean isArrayElementReadable(long index,
                    @CachedLibrary("this.delegate") InteropLibrary interop) {
        return interop.isArrayElementReadable(delegate, index);
    }

    @ExportMessage
    boolean isArrayElementModifiable(long index,
                    @CachedLibrary("this.delegate") InteropLibrary interop) {
        return interop.isArrayElementModifiable(delegate, index);
    }

    @ExportMessage
    boolean isArrayElementInsertable(long index,
                    @CachedLibrary("this.delegate") InteropLibrary interop) {
        return interop.isArrayElementInsertable(delegate, index);
    }

    @ExportMessage
    boolean isArrayElementRemovable(long index,
                    @CachedLibrary("this.delegate") InteropLibrary interop) {
        return interop.isArrayElementRemovable(delegate, index);
    }

    @ExportMessage
    Object readArrayElement(long index,
                    @CachedLibrary("this.delegate") InteropLibrary interop) throws UnsupportedMessageException, InvalidArrayIndexException {
        return interop.readArrayElement(delegate, index);
    }

    @ExportMessage
    void writeArrayElement(long index, Object value,
                    @CachedLibrary("this.delegate") InteropLibrary interop) throws UnsupportedMessageException, InvalidArrayIndexException, UnsupportedTypeException {
        interop.writeArrayElement(delegate, index, value);
    }

    @ExportMessage
    void removeArrayElement(long index,
                    @CachedLibrary("this.delegate") InteropLibrary interop) throws UnsupportedMessageException, InvalidArrayIndexException {
        interop.removeArrayElement(delegate, index);
    }

    @ExportMessage
    boolean hasMembers(@CachedLibrary("this.delegate") InteropLibrary interop) {
        return interop.hasMembers(delegate);
    }

    @ExportMessage
    Object getMembers(boolean includeInternal,
                    @CachedLibrary("this.delegate") InteropLibrary interop) throws UnsupportedMessageException {
        return interop.getMembers(delegate, includeInternal);
    }

    @ExportMessage
    boolean isMemberReadable(String member,
                    @CachedLibrary("this.delegate") InteropLibrary interop) {
        return interop.isMemberReadable(delegate, member);
    }

    @ExportMessage
    boolean isMemberModifiable(String member,
                    @CachedLibrary("this.delegate") InteropLibrary interop) {
        return interop.isMemberModifiable(delegate, member);
    }

    @ExportMessage
    boolean isMemberInsertable(String member,
                    @CachedLibrary("this.delegate") InteropLibrary interop) {
        return interop.isMemberInsertable(delegate, member);
    }

    @ExportMessage
    boolean isMemberRemovable(String member,
                    @CachedLibrary("this.delegate") InteropLibrary interop) {
        return interop.isMemberRemovable(delegate, member);
    }

    @ExportMessage
    boolean isMemberInvocable(String member,
                    @CachedLibrary("this.delegate") InteropLibrary interop) {
        return interop.isMemberInvocable(delegate, member);
    }

    @ExportMessage
    boolean isMemberInternal(String member,
                    @CachedLibrary("this.delegate") InteropLibrary interop) {
        return interop.isMemberInternal(delegate, member);
    }

    @ExportMessage
    Object readMember(String member,
                    @CachedLibrary("this.delegate") InteropLibrary interop) throws UnsupportedMessageException, UnknownIdentifierException {
        return interop.readMember(delegate, member);
    }

    @ExportMessage
    void writeMember(String member, Object value,
                    @CachedLibrary("this.delegate") InteropLibrary interop) throws UnsupportedMessageException, UnknownIdentifierException, UnsupportedTypeException {
        interop.writeMember(delegate, member, value);
    }

    @ExportMessage
    boolean isExecutable(@CachedLibrary("this.delegate") InteropLibrary interop) {
        return interop.isExecutable(delegate);
    }

    @ExportMessage
    Object execute(Object[] arguments,
                    @CachedLibrary("this.delegate") InteropLibrary interop) throws UnsupportedTypeException, ArityException, UnsupportedMessageException {
        return interop.execute(delegate, arguments);
    }

    @ExportMessage
    Object invokeMember(String member, Object[] arguments,
                    @CachedLibrary("this.delegate") InteropLibrary interop) throws UnsupportedMessageException, ArityException, UnknownIdentifierException, UnsupportedTypeException {
        return interop.invokeMember(delegate, member, arguments);
    }

    @ExportMessage
    void removeMember(String member,
                    @CachedLibrary("this.delegate") InteropLibrary interop) throws UnsupportedMessageException, UnknownIdentifierException {
        interop.removeMember(delegate, member);
    }

    @ExportMessage
    boolean hasMemberReadSideEffects(String member,
                    @CachedLibrary("this.delegate") InteropLibrary interop) {
        return interop.hasMemberReadSideEffects(delegate, member);
    }

    @ExportMessage
    boolean hasMemberWriteSideEffects(String member,
                    @CachedLibrary("this.delegate") InteropLibrary interop) {
        return interop.hasMemberWriteSideEffects(delegate, member);
    }

    @ExportMessage
    boolean isString(@CachedLibrary("this.delegate") InteropLibrary interop) {
        return interop.isString(delegate);
    }

    @ExportMessage
    String asString(@CachedLibrary("this.delegate") InteropLibrary interop) throws UnsupportedMessageException {
        return interop.asString(delegate);
    }

    @ExportMessage
    boolean isBoolean(@CachedLibrary("this.delegate") InteropLibrary interop) {
        return interop.isBoolean(delegate);
    }

    @ExportMessage
    boolean asBoolean(@CachedLibrary("this.delegate") InteropLibrary interop) throws UnsupportedMessageException {
        return interop.asBoolean(delegate);
    }

    @ExportMessage
    boolean isNumber(@CachedLibrary("this.delegate") InteropLibrary interop) {
        return interop.isNumber(delegate);
    }

    @ExportMessage
    boolean fitsInByte(@CachedLibrary("this.delegate") InteropLibrary interop) {
        return interop.fitsInByte(delegate);
    }

    @ExportMessage
    byte asByte(@CachedLibrary("this.delegate") InteropLibrary interop) throws UnsupportedMessageException {
        return interop.asByte(delegate);
    }

    @ExportMessage
    boolean fitsInDouble(@CachedLibrary("this.delegate") InteropLibrary interop) {
        return interop.fitsInDouble(delegate);
    }

    @ExportMessage
    double asDouble(@CachedLibrary("this.delegate") InteropLibrary interop) throws UnsupportedMessageException {
        return interop.asDouble(delegate);
    }

    @ExportMessage
    boolean fitsInFloat(@CachedLibrary("this.delegate") InteropLibrary interop) {
        return interop.fitsInFloat(delegate);
    }

    @ExportMessage
    float asFloat(@CachedLibrary("this.delegate") InteropLibrary interop) throws UnsupportedMessageException {
        return interop.asFloat(delegate);
    }

    @ExportMessage
    boolean fitsInInt(@CachedLibrary("this.delegate") InteropLibrary interop) {
        return interop.fitsInInt(delegate);
    }

    @ExportMessage
    int asInt(@CachedLibrary("this.delegate") InteropLibrary interop) throws UnsupportedMessageException {
        return interop.asInt(delegate);
    }

    @ExportMessage
    boolean fitsInLong(@CachedLibrary("this.delegate") InteropLibrary interop) {
        return interop.fitsInLong(delegate);
    }

    @ExportMessage
    long asLong(@CachedLibrary("this.delegate") InteropLibrary interop) throws UnsupportedMessageException {
        return interop.asLong(delegate);
    }

    @ExportMessage
    boolean fitsInShort(@CachedLibrary("this.delegate") InteropLibrary interop) {
        return interop.fitsInShort(delegate);
    }

    @ExportMessage
    short asShort(@CachedLibrary("this.delegate") InteropLibrary interop) throws UnsupportedMessageException {
        return interop.asShort(delegate);
    }
}
