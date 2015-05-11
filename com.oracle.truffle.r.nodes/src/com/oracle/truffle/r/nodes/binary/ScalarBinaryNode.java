/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.binary;

import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.ops.na.*;

/**
 * Encapsulates an abstract scalar binary operation to be executed multiple times when calculating
 * vectors or primitive types. Invoke {@link #enable(RAbstractVector, RAbstractVector)} before use.
 * This class represents one abstract scalar operation for {@link VectorBinaryNode}.
 */
@SuppressWarnings("unused")
public abstract class ScalarBinaryNode extends Node {

    protected final NACheck leftNACheck = new NACheck();
    protected final NACheck rightNACheck = new NACheck();
    protected final NACheck resultNACheck = new NACheck();

    /**
     * Returns <code>true</code> if one of the vector classes may require constant time folding with
     * {@link #tryFoldConstantTime(RAbstractVector, int, RAbstractVector, int)}.
     */
    public boolean mayFoldConstantTime(Class<? extends RAbstractVector> left, Class<? extends RAbstractVector> right) {
        return false;
    }

    /**
     * Returns a folded version of the left and right vector if both can be folded for this scalar
     * operation. Returns <code>null</code> if folding was not possible.
     */
    public RAbstractVector tryFoldConstantTime(RAbstractVector left, int leftLength, RAbstractVector right, int rightLength) {
        return null;
    }

    /**
     * Enables all NA checks for the given input vectors.
     */
    public final void enable(RAbstractVector left, RAbstractVector right) {
        leftNACheck.enable(left);
        rightNACheck.enable(right);
        resultNACheck.enable(resultNeedsNACheck());
    }

    /**
     * Returns <code>true</code> if there was never a <code>null</code> value encountered when using
     * this node. Make you have enabled the NA check properly using
     * {@link #enable(RAbstractVector, RAbstractVector)} before relying on this method.
     */
    public final boolean isComplete() {
        return leftNACheck.neverSeenNA() && rightNACheck.neverSeenNA() && resultNACheck.neverSeenNA();
    }

    /**
     * Returns <code>true</code> if the result of the operation might produce NA results.
     */
    protected abstract boolean resultNeedsNACheck();

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

}
