/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.RRuntime;
import static com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector.MEMBER_IM;
import static com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector.MEMBER_RE;

/**
 * Represents an {@code RComplex} value passed to the interop. This value should never appear in the
 * FastR execution, it is only passed to interop and converted back to its original RComplex value
 * if passed back to FastR.
 */
@ExportLibrary(InteropLibrary.class)
public class RInteropComplex implements RTruffleObject {

    private final RComplex value;

    public RInteropComplex(RComplex value) {
        assert !RRuntime.isNA(value);
        this.value = value;
    }

    public Object getValue() {
        return value;
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    boolean hasMembers() {
        return true;
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    Object getMembers(@SuppressWarnings("unused") boolean includeInternal) {
        return RDataFactory.createStringVector(new String[]{MEMBER_RE, MEMBER_IM}, RDataFactory.COMPLETE_VECTOR);
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    boolean isMemberReadable(String member) {
        return MEMBER_RE.equals(member) || MEMBER_IM.equals(member);
    }

    @ExportMessage
    Object readMember(String member,
                    @Cached.Exclusive @Cached("createBinaryProfile()") ConditionProfile unknownIdentifier) throws UnknownIdentifierException {
        if (unknownIdentifier.profile(!isMemberReadable(member))) {
            throw UnknownIdentifierException.create(member);
        }
        if (MEMBER_RE.equals(member)) {
            return value.getDataAt(0).getRealPart();
        } else {
            return value.getDataAt(0).getImaginaryPart();
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof RInteropComplex)) {
            return false;
        }
        return value.equals(((RInteropComplex) obj).value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
