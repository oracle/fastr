/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.primitive;

import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

/**
 * A typed binary map function for use in {@link BinaryMapNode}. The called methods depend on the
 * used argumentType and resultType in {@link BinaryMapNode}.
 */
@SuppressWarnings("unused")
public abstract class BinaryMapFunctionNode extends RBaseNode {

    public byte applyLogical(byte left, byte right) {
        throw RInternalError.shouldNotReachHere();
    }

    public byte applyLogical(int left, int right) {
        throw RInternalError.shouldNotReachHere();
    }

    public byte applyLogical(double left, double right) {
        throw RInternalError.shouldNotReachHere();
    }

    public byte applyLogical(RComplex left, RComplex right) {
        throw RInternalError.shouldNotReachHere();
    }

    public byte applyLogical(String left, String right) {
        throw RInternalError.shouldNotReachHere();
    }

    public int applyInteger(int left, int right) {
        throw RInternalError.shouldNotReachHere();
    }

    public double applyDouble(double left, double right) {
        throw RInternalError.shouldNotReachHere();
    }

    public double applyDouble(int left, int right) {
        throw RInternalError.shouldNotReachHere();
    }

    public byte applyRaw(byte left, byte right) {
        throw RInternalError.shouldNotReachHere();
    }

    public String applyCharacter(String left, String right) {
        throw RInternalError.shouldNotReachHere();
    }

    public RComplex applyComplex(RComplex left, RComplex right) {
        throw RInternalError.shouldNotReachHere();
    }

    /**
     * Returns <code>true</code> if one of the vector classes may require constant time folding with
     * {@link #tryFoldConstantTime(RAbstractVector, int, RAbstractVector, int)}.
     */
    public abstract boolean mayFoldConstantTime(Class<? extends RAbstractVector> left, Class<? extends RAbstractVector> right);

    /**
     * Returns a folded version of the left and right vector if both can be folded for this scalar
     * operation. Returns <code>null</code> if folding was not possible.
     */
    public abstract RAbstractVector tryFoldConstantTime(RAbstractVector left, int leftLength, RAbstractVector right, int rightLength);

    /**
     * Enables the node for the two operation. Invoked once for each BinaryMap operation invocation.
     */
    public void enable(RAbstractVector left, RAbstractVector right) {

    }

    /**
     * Returns <code>true</code> if the result can always be considered complete.
     */
    public boolean isComplete() {
        return true;
    }
}
