/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.ops;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.runtime.data.*;

public abstract class BooleanOperation extends Operation {

    public BooleanOperation(boolean commutative, boolean associative) {
        super(commutative, associative);
    }

    public abstract String opName();

    public abstract byte op(int left, int right);

    public abstract byte op(double left, double right);

    public abstract byte op(String left, String right);

    public abstract byte op(RComplex left, RComplex right);

    // methods below are to be overridden by subclasses that need them

    /*
     * Determines if evaluation of the entire operation requires evaluation of the right operand (or
     * if it can proceed based on just the left operand)
     */
    public boolean requiresRightOperand(@SuppressWarnings("unused") byte leftOperand) {
        // in most cases it will be true (false only for the operand with short-circuit semantics)
        return true;
    }

    @TruffleBoundary
    public RRaw op(@SuppressWarnings("unused") RRaw left, @SuppressWarnings("unused") RRaw right) {
        throw new UnsupportedOperationException();
    }

    @TruffleBoundary
    public byte op(@SuppressWarnings("unused") int left, @SuppressWarnings("unused") String right) {
        throw new UnsupportedOperationException();
    }

    @TruffleBoundary
    public byte op(@SuppressWarnings("unused") String left, @SuppressWarnings("unused") int right) {
        throw new UnsupportedOperationException();
    }

    @TruffleBoundary
    public byte op(@SuppressWarnings("unused") double left, @SuppressWarnings("unused") String right) {
        throw new UnsupportedOperationException();
    }

    @TruffleBoundary
    public byte op(@SuppressWarnings("unused") String left, @SuppressWarnings("unused") double right) {
        throw new UnsupportedOperationException();
    }

    @TruffleBoundary
    public byte op(@SuppressWarnings("unused") RNull left, @SuppressWarnings("unused") Object right) {
        throw new UnsupportedOperationException();
    }

    @TruffleBoundary
    public byte op(@SuppressWarnings("unused") Object left, @SuppressWarnings("unused") RNull right) {
        throw new UnsupportedOperationException();
    }

    @TruffleBoundary
    public byte op(@SuppressWarnings("unused") RRaw left, @SuppressWarnings("unused") Object right) {
        throw new UnsupportedOperationException();
    }

    @TruffleBoundary
    public byte op(@SuppressWarnings("unused") Object left, @SuppressWarnings("unused") RRaw right) {
        throw new UnsupportedOperationException();
    }

}
