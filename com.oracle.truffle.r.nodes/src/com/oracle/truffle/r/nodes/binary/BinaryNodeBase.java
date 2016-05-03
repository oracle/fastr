/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.helpers.InheritsCheckNode;
import com.oracle.truffle.r.nodes.helpers.RFactorNodes;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;

/**
 * Provides some shared code for nodes implemented in this package.
 *
 * TODO: for the time being, this code is only concerned with R factor class. Code specific to
 * factors is necessary only because the proper R dispatch of operations implemented in this java
 * package is not implemented yet. Once this is done, we can remove this base class. It also seems
 * that {@link BinaryBooleanNode} and {@link BinaryArithmeticNode} are separate classes only to
 * handle the difference in R specializations that implement them, e.g. Ops.factor handles
 * 'arithmetic' subset of Ops differently than the 'logical'.
 */
abstract class BinaryNodeBase extends RBuiltinNode {

    @Child private InheritsCheckNode factorInheritCheck = new InheritsCheckNode(RRuntime.CLASS_FACTOR);
    @Child private RFactorNodes.GetOrdered isOrderedFactor = null;

    protected boolean isFactor(Object value) {
        return factorInheritCheck.execute(value);
    }

    protected boolean isOrderedFactor(RAbstractIntVector factor) {
        if (isOrderedFactor == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            isOrderedFactor = insert(new RFactorNodes.GetOrdered());
        }

        return isOrderedFactor.execute(factor);
    }

    protected RError.Message getFactorWarning(RAbstractIntVector factor) {
        return isOrderedFactor(factor) ? RError.Message.NOT_MEANINGFUL_FOR_ORDERED_FACTORS : RError.Message.NOT_MEANINGFUL_FOR_FACTORS;
    }
}
