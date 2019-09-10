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

import java.util.Arrays;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.RFunction.ExplicitCall;
import com.oracle.truffle.r.runtime.gnur.SEXPTYPE;
import com.oracle.truffle.r.runtime.interop.R2Foreign;

/**
 * A simple wrapper class for passing the ... argument through RArguments
 */
@ExportLibrary(InteropLibrary.class)
public final class RArgsValuesAndNames extends RBaseObject {

    /**
     * Array of arguments; semantics have to be specified by child classes.
     */
    @CompilationFinal(dimensions = 1) private final Object[] values;

    /**
     * Array of arguments; semantics have to be specified by child classes.
     */
    private final ArgumentsSignature signature;

    private RPairList materialized;

    /**
     * Default instance for empty "..." ("..." that resolve to contain no expression at runtime).
     * The {@link RMissing#instance} for "...".
     */
    public static final RArgsValuesAndNames EMPTY = new RArgsValuesAndNames(new Object[0], ArgumentsSignature.empty(0));

    public RArgsValuesAndNames(Object[] values, ArgumentsSignature signature) {
        this.values = values;
        this.signature = signature;
        assert signature != null && signature.getLength() == values.length : Arrays.toString(values) + " " + signature;
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    boolean hasArrayElements() {
        return true;
    }

    @ExportMessage
    long getArraySize() {
        return getLength();
    }

    @ExportMessage
    boolean isArrayElementReadable(long index) {
        return index >= 0 && index < getLength();
    }

    @ExportMessage
    Object readArrayElement(long index,
                    @Shared("r2Foreign") @Cached() R2Foreign r2Foreign,
                    @Shared("unknownIdentifier") @Cached("createBinaryProfile()") ConditionProfile unknownIdentifier) throws InvalidArrayIndexException {
        if (unknownIdentifier.profile(!isArrayElementReadable(index))) {
            throw InvalidArrayIndexException.create(index);
        }
        Object value = getArgument((int) index);
        return r2Foreign.convert(value);
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    boolean hasMembers() {
        return true;
    }

    @ExportMessage
    Object getMembers(@SuppressWarnings("unused") boolean includeInternal) {
        String[] names = signature.getNames();
        if (names == null) {
            return RDataFactory.createStringVector(new String[signature.getLength()], RDataFactory.COMPLETE_VECTOR);
        }
        return RDataFactory.createStringVector(names, RDataFactory.COMPLETE_VECTOR).makeSharedPermanent();
    }

    @ExportMessage
    boolean isMemberReadable(String member) {
        return getMemberIndex(member) >= 0;
    }

    @ExportMessage
    boolean isMemberInvocable(String member,
                    @Shared("unknownIdentifier") @Cached("createBinaryProfile()") ConditionProfile unknownIdentifier) {
        int idx = getMemberIndex(member);
        if (unknownIdentifier.profile(idx < 0)) {
            return false;
        }
        return getArgument(idx) instanceof RFunction;
    }

    @ExportMessage
    Object readMember(String member,
                    @Shared("r2Foreign") @Cached() R2Foreign r2Foreign,
                    @Shared("unknownIdentifier") @Cached("createBinaryProfile()") ConditionProfile unknownIdentifier) throws UnknownIdentifierException {
        int idx = getMemberIndex(member);
        if (unknownIdentifier.profile(idx < 0)) {
            throw UnknownIdentifierException.create(member);
        }

        Object value = getArgument(idx);
        return r2Foreign.convert(value);
    }

    @ExportMessage
    Object invokeMember(String member, Object[] arguments,
                    @Shared("unknownIdentifier") @Cached("createBinaryProfile()") ConditionProfile unknownIdentifier,
                    @Cached() ExplicitCall c) throws UnknownIdentifierException, UnsupportedMessageException {
        int idx = getMemberIndex(member);
        if (unknownIdentifier.profile(idx < 0)) {
            throw UnknownIdentifierException.create(member);
        }
        Object arg = getArgument(idx);
        if (arg instanceof RFunction) {
            return c.execute((RFunction) arg, arguments);
        }
        throw UnsupportedMessageException.create();
    }

    private int getMemberIndex(String member) {
        ArgumentsSignature sig = getSignature();
        String[] names = sig.getNames();
        if (names == null) {
            return -1;
        }
        for (int i = 0; i < names.length; i++) {
            if (names[i].equals(member)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public RType getRType() {
        return RType.Dots;
    }

    public ArgumentsSignature getSignature() {
        return signature;
    }

    public int getLength() {
        return signature.getLength();
    }

    public Object[] getArguments() {
        return values;
    }

    public Object getArgument(int index) {
        return values[index];
    }

    public boolean isEmpty() {
        return signature.isEmpty();
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder().append(getClass().getSimpleName()).append(": ");
        for (int i = 0; i < values.length; i++) {
            str.append(i == 0 ? "" : ", ").append(signature.getName(i)).append(" = ").append(values[i]);
        }
        return str.toString();
    }

    public Object toPairlist() {
        // special case: empty lists are represented by "missing"
        if (isEmpty()) {
            return RMissing.instance;
        }

        RPairList[] existingCells = RPairList.getCells(materialized);
        Object current = RNull.instance;
        for (int i = getLength() - 1; i >= 0; i--) {
            String name = signature.getName(i);
            Object tag = name != null ? RDataFactory.createSymbol(name) : RNull.instance;
            Object car = getArgument(i);
            if (i < existingCells.length) {
                RPairList existing = existingCells[i];
                existing.setCar(car);
                existing.setTag(tag);
                existing.setCdr(current);
                current = existing;
            } else {
                current = RDataFactory.createPairList(car, current, tag, SEXPTYPE.DOTSXP);
            }
        }
        materialized = (RPairList) current;
        return current;
    }
}
