/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.CompilerDirectives.ValueType;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.RRuntime;
import static com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector.MEMBER_IM;
import static com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector.MEMBER_RE;

@ValueType
@ExportLibrary(InteropLibrary.class)
public final class RComplex implements RTruffleObject, ScalarWrapper {

    private final double realPart;
    private final double imaginaryPart;

    private RComplex(double realPart, double imaginaryPart) {
        this.realPart = realPart;
        this.imaginaryPart = imaginaryPart;
    }

    @ExportMessage
    public boolean isNull() {
        return RRuntime.isNA(this);
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    boolean hasMembers() {
        return true;
    }

    @ExportMessage
    Object getMembers(@SuppressWarnings("unused") boolean includeInternal,
                    @Cached.Shared("noMembers") @Cached("createBinaryProfile()") ConditionProfile noMembers) throws UnsupportedMessageException {
        if (noMembers.profile(!hasMembers())) {
            throw UnsupportedMessageException.create();
        }
        return RDataFactory.createStringVector(new String[]{MEMBER_RE, MEMBER_IM}, RDataFactory.COMPLETE_VECTOR);
    }

    @ExportMessage
    boolean isMemberReadable(String member,
                    @Cached.Shared("noMembers") @Cached("createBinaryProfile()") ConditionProfile noMembers) {
        if (noMembers.profile(!hasMembers())) {
            return false;
        }
        return MEMBER_RE.equals(member) || MEMBER_IM.equals(member);
    }

    @ExportMessage
    Object readMember(String member,
                    @Cached.Shared("noMembers") @Cached("createBinaryProfile()") ConditionProfile noMembers,
                    @Cached.Exclusive @Cached("createBinaryProfile()") ConditionProfile unknownIdentifier,
                    @Cached.Exclusive @Cached("createBinaryProfile()") ConditionProfile isNullIdentifier) throws UnknownIdentifierException, UnsupportedMessageException {
        if (noMembers.profile(!hasMembers())) {
            throw UnsupportedMessageException.create();
        }
        if (unknownIdentifier.profile(!isMemberReadable(member, noMembers))) {
            throw UnknownIdentifierException.create(member);
        }
        if (isNullIdentifier.profile(isNull())) {
            return new RInteropNA.RInteropComplexNA(this);
        }
        if (MEMBER_RE.equals(member)) {
            return getRealPart();
        } else {
            return getImaginaryPart();
        }
    }

    public static RComplex createNA() {
        return RRuntime.COMPLEX_NA;
    }

    public static RComplex createRealOne() {
        return RRuntime.COMPLEX_REAL_ONE;
    }

    public static RComplex createZero() {
        return RRuntime.COMPLEX_ZERO;
    }

    public static RComplex valueOf(double real, double imaginary) {
        return new RComplex(real, imaginary);
    }

    public double getRealPart() {
        return realPart;
    }

    public double getImaginaryPart() {
        return imaginaryPart;
    }

    @Override
    @TruffleBoundary
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        return toString(Double.toString(realPart), Double.toString(imaginaryPart));
    }

    public String toString(String realPartString, String imaginaryPartString) {
        CompilerAsserts.neverPartOfCompilation();
        return isNA() ? "NA" : realPartString + (imaginaryPart < 0.0 ? "" : "+") + imaginaryPartString + "i";
    }

    public boolean isNA() {
        return RRuntime.isNA(this);
    }

    public boolean isZero() {
        return realPart == 0.0 && imaginaryPart == 0.0;
    }

    public boolean isRealOne() {
        return realPart == 1.0 && imaginaryPart == 0.0;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof RComplex)) {
            return false;
        }
        RComplex other = (RComplex) obj;
        return isNA() && other.isNA() || (((Double) realPart).equals(other.getRealPart())) && (((Double) imaginaryPart).equals(other.getImaginaryPart()));
    }

    @Override
    public int hashCode() {
        return Double.hashCode(realPart) ^ Double.hashCode(imaginaryPart);
    }

    public static double abs(double re, double im) {
        if (!RRuntime.isFinite(re) || !RRuntime.isFinite(im)) {
            if (Double.isInfinite(re) || Double.isInfinite(im)) {
                return Double.POSITIVE_INFINITY;
            } else if (Double.isNaN(im)) {
                return im;
            } else {
                return re;
            }
        } else {
            return Math.sqrt(re * re + im * im);
        }
    }
}
