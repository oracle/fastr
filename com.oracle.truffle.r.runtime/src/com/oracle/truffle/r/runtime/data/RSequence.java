/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.model.*;

public abstract class RSequence extends RBounded implements RAbstractVector {

    private final int length;

    protected RSequence(int length) {
        this.length = length;
    }

    @Override
    public final int getLength() {
        return length;
    }

    public final boolean isComplete() {
        return true;
    }

    public final boolean hasDimensions() {
        return false;
    }

    public final int[] getDimensions() {
        return null;
    }

    public final RVector createVector() {
        return internalCreateVector();
    }

    protected abstract RVector internalCreateVector();

    @Override
    public final RAbstractVector copy() {
        return createVector();
    }

    @Override
    public final RAbstractVector copyDropAttributes() {
        return createVector();
    }

    @Override
    public final RAbstractVector copyWithNewDimensions(int[] newDimensions) {
        return createVector().copyWithNewDimensions(newDimensions);
    }

    @Override
    public final RStringVector getNames(RAttributeProfiles attrProfiles) {
        return null;
    }

    @Override
    public final RList getDimNames() {
        return null;
    }

    @Override
    public final Object getRowNames(RAttributeProfiles attrProfiles) {
        return RNull.instance;
    }

    @Override
    public final RAttributes initAttributes() {
        // TODO implement
        assert false;
        return null;
    }

    @Override
    public final RAttributes getAttributes() {
        return null;
    }

    public final boolean isMatrix() {
        return false;
    }

    public final boolean isArray() {
        return false;
    }

    @Override
    public final boolean isObject(RAttributeProfiles attrProfiles) {
        return false;
    }

    @Override
    public final RVector materializeNonSharedVector() {
        RVector resultVector = this.materialize();
        assert !resultVector.isShared();
        return resultVector;
    }

    @Override
    public final RShareable materializeToShareable() {
        return this.materialize();
    }

    @Override
    public final void transferElementSameType(int toIndex, RAbstractVector fromVector, int fromIndex) {
        throw RInternalError.shouldNotReachHere();
    }

    @Override
    public final RVector copyResizedWithDimensions(int[] newDimensions) {
        // TODO support for higher dimensions
        assert newDimensions.length == 2;
        RVector result = copyResized(newDimensions[0] * newDimensions[1], false);
        result.setDimensions(newDimensions);
        return result;
    }

}
