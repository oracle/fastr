/*
 * Copyright (c) 2014, 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.access.array.read;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@NodeChildren({@NodeChild(value = "vector", type = RNode.class), @NodeChild(value = "operand", type = RNode.class)})
public abstract class MultiDimPosConverterNode extends RNode {

    public abstract RIntVector executeConvert(VirtualFrame frame, Object vector, Object p);

    private final boolean isSubset;

    protected MultiDimPosConverterNode(boolean isSubset) {
        this.isSubset = isSubset;
    }

    protected MultiDimPosConverterNode(MultiDimPosConverterNode other) {
        this.isSubset = other.isSubset;
    }

    @Specialization(guards = {"!singleOpNegative", "!multiPos"})
    protected RAbstractIntVector doIntVector(@SuppressWarnings("unused") Object vector, RAbstractIntVector positions) {
        return positions;
    }

    @Specialization(guards = {"!singleOpNegative", "multiPos"})
    protected RAbstractIntVector doIntVectorMultiPos(@SuppressWarnings("unused") Object vector, RAbstractIntVector positions) {
        if (isSubset) {
            return positions;
        } else {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_MORE_1);
        }
    }

    @Specialization(guards = {"singleOpNA"})
    protected RAbstractIntVector doIntVectorNA(Object vector, RAbstractIntVector positions) {
        if (isSubset || vector == RNull.instance) {
            return positions;
        } else {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.SUBSCRIPT_BOUNDS);
        }
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"singleOpNegative", "!singleOpNA"})
    protected RAbstractIntVector doIntVectorNegative(Object vector, RAbstractIntVector positions) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_MORE_1);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "noPosition")
    protected Object accessListEmptyPos(RAbstractVector vector, RList positions) {
        if (!isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_LESS_1);
        } else {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "list");
        }
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "onePosition")
    protected Object accessListOnePos(RAbstractVector vector, RList positions) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "list");
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "multiPos")
    protected Object accessListMultiPos(RAbstractVector vector, RList positions) {
        if (!isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_MORE_1);
        } else {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "list");
        }
    }

    @SuppressWarnings("unused")
    @Specialization
    protected Object accessListOnePos(RAbstractVector vector, RComplex positions) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "complex");
    }

    @SuppressWarnings("unused")
    @Specialization
    protected Object accessListOnePos(RAbstractVector vector, RRaw positions) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "raw");
    }

    @SuppressWarnings("unused")
    protected static boolean singleOpNegative(Object vector, RAbstractIntVector p) {
        return p.getLength() == 1 && p.getDataAt(0) < 0;
    }

    @SuppressWarnings("unused")
    protected static boolean singleOpNA(Object vector, RAbstractIntVector p) {
        return p.getLength() == 1 && RRuntime.isNA(p.getDataAt(0));
    }

    @SuppressWarnings("unused")
    protected static boolean onePosition(RAbstractVector vector, RAbstractVector p) {
        return p.getLength() == 1;
    }

    @SuppressWarnings("unused")
    protected static boolean noPosition(RAbstractVector vector, RAbstractVector p) {
        return p.getLength() == 0;
    }

    @SuppressWarnings("unused")
    protected static boolean multiPos(RAbstractVector vector, RAbstractVector positions) {
        return positions.getLength() > 1;
    }

    @SuppressWarnings("unused")
    protected static boolean multiPos(Object vector, RAbstractVector positions) {
        return positions.getLength() > 1;
    }
}
