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

import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

public abstract class BinaryMapNAFunctionNode extends BinaryMapFunctionNode {

    protected final NACheck leftNACheck = NACheck.create();
    protected final NACheck rightNACheck = NACheck.create();

    /**
     * Enables all NA checks for the given input vectors.
     */
    @Override
    public final void enable(RAbstractVector left, RAbstractVector right) {
        leftNACheck.enable(left);
        rightNACheck.enable(right);
    }

    /**
     * Returns <code>true</code> if there was never a <code>NA</code> value encountered when using
     * this node. Make sure you have enabled the NA check properly using
     * {@link #enable(RAbstractVector, RAbstractVector)} before relying on this method.
     */
    @Override
    public final boolean isComplete() {
        return leftNACheck.neverSeenNA() && rightNACheck.neverSeenNA() && !introducesNA();
    }

    public final NACheck getLeftNACheck() {
        return leftNACheck;
    }

    public final NACheck getRightNACheck() {
        return rightNACheck;
    }

    /**
     * Returns <code>true</code> if the result of the operation might may contain NA results. This
     * is queried only after the operation has been executed. Ideally, this will only return true
     * once an NA result has been generated out of non-NA operands (because it is used to initialize
     * the "complete" flag of the result vector).
     */
    protected abstract boolean introducesNA();

}
