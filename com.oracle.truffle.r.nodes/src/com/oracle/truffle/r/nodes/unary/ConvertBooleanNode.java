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
package com.oracle.truffle.r.nodes.unary;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.ops.na.*;

public abstract class ConvertBooleanNode extends UnaryNode {

    private final NACheck check = NACheck.create();

    @Override
    public abstract byte executeByte(VirtualFrame frame);

    public abstract byte executeByte(VirtualFrame frame, Object operandValue);

    @Specialization
    public byte doLogical(VirtualFrame frame, byte value) {
        check.enable(value);
        if (check.check(value)) {
            throw RError.error(frame, getEncapsulatingSourceSection(), RError.Message.NA_UNEXP);
        }
        return value;
    }

    @Specialization
    public byte doInt(VirtualFrame frame, int value) {
        check.enable(value);
        if (check.check(value)) {
            throw RError.error(frame, getEncapsulatingSourceSection(), RError.Message.ARGUMENT_NOT_INTERPRETABLE_LOGICAL);
        }
        return RRuntime.asLogical(value != 0);
    }

    @Specialization
    public byte doDouble(VirtualFrame frame, double value) {
        check.enable(value);
        if (check.check(value)) {
            throw RError.error(frame, getEncapsulatingSourceSection(), RError.Message.ARGUMENT_NOT_INTERPRETABLE_LOGICAL);
        }
        return RRuntime.asLogical(value != 0D);
    }

    @Specialization
    public byte doComplex(VirtualFrame frame, RComplex value) {
        check.enable(value);
        if (check.check(value)) {
            throw RError.error(frame, getEncapsulatingSourceSection(), RError.Message.ARGUMENT_NOT_INTERPRETABLE_LOGICAL);
        }
        return RRuntime.asLogical(!value.isZero());
    }

    @Specialization
    public byte doString(VirtualFrame frame, String value) {
        check.enable(value);
        byte logicalValue = check.convertStringToLogical(value);
        check.enable(logicalValue);
        if (check.check(logicalValue)) {
            throw RError.error(frame, getEncapsulatingSourceSection(), RError.Message.ARGUMENT_NOT_INTERPRETABLE_LOGICAL);
        }
        return logicalValue;
    }

    @Specialization
    public byte doRaw(RRaw value) {
        return RRuntime.asLogical(value.getValue() != 0);
    }

    @Specialization
    public byte doIntSequence(RIntSequence value) {
        if (value.getLength() > 1) {
            moreThanOneElem.enter();
            RError.warning(this.getEncapsulatingSourceSection(), RError.Message.LENGTH_GT_1);
        }
        return RRuntime.asLogical(value.getStart() != 0);
    }

    @Specialization
    public byte doDoubleSequence(RDoubleSequence value) {
        if (value.getLength() > 1) {
            moreThanOneElem.enter();
            RError.warning(this.getEncapsulatingSourceSection(), RError.Message.LENGTH_GT_1);
        }
        return RRuntime.asLogical(value.getStart() != 0D);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "isEmpty")
    public byte doEmptyVector(VirtualFrame frame, RAbstractVector value) {
        throw RError.error(frame, this.getEncapsulatingSourceSection(), RError.Message.LENGTH_ZERO);
    }

    private final BranchProfile moreThanOneElem = new BranchProfile();

    @Specialization(guards = "!isEmpty")
    public byte doIntVector(VirtualFrame frame, RIntVector value) {
        if (value.getLength() > 1) {
            moreThanOneElem.enter();
            RError.warning(this.getEncapsulatingSourceSection(), RError.Message.LENGTH_GT_1);
        }
        check.enable(value);
        if (check.check(value.getDataAt(0))) {
            throw RError.error(frame, getEncapsulatingSourceSection(), RError.Message.ARGUMENT_NOT_INTERPRETABLE_LOGICAL);
        }
        return RRuntime.asLogical(value.getDataAt(0) != 0);
    }

    @Specialization(guards = "!isEmpty")
    public byte doDoubleVector(VirtualFrame frame, RDoubleVector value) {
        if (value.getLength() > 1) {
            moreThanOneElem.enter();
            RError.warning(this.getEncapsulatingSourceSection(), RError.Message.LENGTH_GT_1);
        }
        check.enable(value);
        if (check.check(value.getDataAt(0))) {
            throw RError.error(frame, getEncapsulatingSourceSection(), RError.Message.ARGUMENT_NOT_INTERPRETABLE_LOGICAL);
        }
        return RRuntime.asLogical(value.getDataAt(0) != 0D);
    }

    @Specialization(guards = "!isEmpty")
    public byte doLogicalVector(VirtualFrame frame, RLogicalVector value) {
        if (value.getLength() > 1) {
            moreThanOneElem.enter();
            RError.warning(this.getEncapsulatingSourceSection(), RError.Message.LENGTH_GT_1);
        }
        check.enable(value);
        if (check.check(value.getDataAt(0))) {
            throw RError.error(frame, getEncapsulatingSourceSection(), RError.Message.NA_UNEXP);
        }
        return RRuntime.asLogical(value.getDataAt(0) != RRuntime.LOGICAL_FALSE);
    }

    @Specialization(guards = "!isEmpty")
    public byte doComplexVector(VirtualFrame frame, RComplexVector value) {
        if (value.getLength() > 1) {
            moreThanOneElem.enter();
            RError.warning(this.getEncapsulatingSourceSection(), RError.Message.LENGTH_GT_1);
        }
        check.enable(value);
        if (check.check(value.getDataAt(0))) {
            throw RError.error(frame, getEncapsulatingSourceSection(), RError.Message.ARGUMENT_NOT_INTERPRETABLE_LOGICAL);
        }
        return RRuntime.asLogical(!value.getDataAt(0).isZero());
    }

    @Specialization(guards = "!isEmpty")
    public byte doRawVector(RRawVector value) {
        if (value.getLength() > 1) {
            moreThanOneElem.enter();
            RError.warning(this.getEncapsulatingSourceSection(), RError.Message.LENGTH_GT_1);
        }
        return RRuntime.asLogical(value.getDataAt(0).getValue() != 0);
    }

    @Specialization(guards = "!isEmpty")
    public byte doStringVector(VirtualFrame frame, RStringVector value) {
        if (value.getLength() > 1) {
            moreThanOneElem.enter();
            RError.warning(this.getEncapsulatingSourceSection(), RError.Message.LENGTH_GT_1);
        }
        check.enable(value);
        byte logicalValue = check.convertStringToLogical(value.getDataAt(0));
        check.enable(logicalValue);
        if (check.check(logicalValue)) {
            throw RError.error(frame, getEncapsulatingSourceSection(), RError.Message.ARGUMENT_NOT_INTERPRETABLE_LOGICAL);
        }
        return logicalValue;
    }

    protected boolean isEmpty(RAbstractVector vector) {
        return vector.getLength() == 0;
    }

    public static ConvertBooleanNode create(RNode node) {
        if (node instanceof ConvertBooleanNode) {
            return (ConvertBooleanNode) node;
        }
        return ConvertBooleanNodeFactory.create(node);
    }
}
