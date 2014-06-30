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

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.ops.na.*;

public abstract class ConvertBooleanNode extends UnaryNode {

    private final NACheck check = NACheck.create();

    @Override
    public abstract byte executeByte(VirtualFrame frame);

    public abstract byte executeByte(VirtualFrame frame, Object operandValue);

    @Specialization
    public byte doLogical(byte value) {
        check.enable(value);
        if (check.check(value)) {
            CompilerDirectives.transferToInterpreter();
            throw unexpectedNAError();
        }
        return value;
    }

    @Specialization
    public byte doInt(int value) {
        check.enable(value);
        if (check.check(value)) {
            CompilerDirectives.transferToInterpreter();
            throw unexpectedNAError();
        }
        return RRuntime.asLogical(value != 0);
    }

    @Specialization
    public byte doDouble(double value) {
        check.enable(value);
        if (check.check(value)) {
            CompilerDirectives.transferToInterpreter();
            throw unexpectedNAError();
        }
        return RRuntime.asLogical(value != 0D);
    }

    @Specialization
    public byte doComplex(RComplex value) {
        check.enable(value);
        if (check.check(value)) {
            CompilerDirectives.transferToInterpreter();
            throw unexpectedNAError();
        }
        return RRuntime.asLogical(!value.isZero());
    }

    @Specialization
    public byte doRaw(RRaw value) {
        return RRuntime.asLogical(value.getValue() != 0);
    }

    @Specialization
    public byte doIntSequence(RIntSequence value) {
        return RRuntime.asLogical(value.getStart() != 0);
    }

    @Specialization
    public byte doDoubleSequence(RDoubleSequence value) {
        return RRuntime.asLogical(value.getStart() != 0D);
    }

    @Specialization
    public byte doIntVector(RIntVector value) {
        if (isNotEmpty(value)) {
            check.enable(value);
            if (check.check(value.getDataAt(0))) {
                CompilerDirectives.transferToInterpreter();
                throw unexpectedNAError();
            }
            return RRuntime.asLogical(value.getDataAt(0) != 0);
        } else {
            CompilerDirectives.transferToInterpreter();
            throw lengthZeroError();
        }
    }

    @Specialization
    public byte doDoubleVector(RDoubleVector value) {
        if (isNotEmpty(value)) {
            check.enable(value);
            if (check.check(value.getDataAt(0))) {
                CompilerDirectives.transferToInterpreter();
                throw unexpectedNAError();
            }
            return RRuntime.asLogical(value.getDataAt(0) != 0D);
        } else {
            CompilerDirectives.transferToInterpreter();
            throw lengthZeroError();
        }
    }

    @Specialization
    public byte doLogicalVector(RLogicalVector value) {
        if (isNotEmpty(value)) {
            check.enable(value);
            if (check.check(value.getDataAt(0))) {
                CompilerDirectives.transferToInterpreter();
                throw unexpectedNAError();
            }
            return RRuntime.asLogical(value.getDataAt(0) != RRuntime.LOGICAL_FALSE);
        } else {
            CompilerDirectives.transferToInterpreter();
            throw lengthZeroError();
        }
    }

    @Specialization
    public byte doComplexVector(RComplexVector value) {
        if (isNotEmpty(value)) {
            check.enable(value);
            if (check.check(value.getDataAt(0))) {
                CompilerDirectives.transferToInterpreter();
                throw unexpectedNAError();
            }
            return RRuntime.asLogical(!value.getDataAt(0).isZero());
        } else {
            CompilerDirectives.transferToInterpreter();
            throw lengthZeroError();
        }
    }

    @Specialization
    public byte doRawVector(RRawVector value) {
        if (isNotEmpty(value)) {
            return RRuntime.asLogical(value.getDataAt(0).getValue() != 0);
        } else {
            CompilerDirectives.transferToInterpreter();
            throw lengthZeroError();
        }
    }

    private BranchProfile everSeenLengthZero = new BranchProfile();

    private boolean isNotEmpty(RVector value) {
        // TODO output warning if vector length > 1
        if (value.getLength() != 0) {
            return true;
        }
        everSeenLengthZero.enter();
        return false;
    }

    @Generic
    public byte doInvalid(@SuppressWarnings("unused") Object value) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.ARGUMENT_NOT_INTERPRETABLE_LOGICAL);
    }

    private RError unexpectedNAError() {
        return RError.error(getEncapsulatingSourceSection(), RError.Message.NA_UNEXP);
    }

    private RError lengthZeroError() {
        return RError.error(getEncapsulatingSourceSection(), RError.Message.LENGTH_ZERO);
    }

    public static ConvertBooleanNode create(RNode node) {
        if (node instanceof ConvertBooleanNode) {
            return (ConvertBooleanNode) node;
        }
        return ConvertBooleanNodeFactory.create(node);
    }
}
