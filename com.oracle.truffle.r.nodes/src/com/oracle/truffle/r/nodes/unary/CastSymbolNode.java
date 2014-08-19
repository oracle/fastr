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

import com.oracle.truffle.api.CompilerDirectives.SlowPath;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

@NodeField(name = "emptyVectorConvertedToNull", type = boolean.class)
public abstract class CastSymbolNode extends CastNode {
    @Child private ToStringNode toString = ToStringNodeFactory.create(null);

    public abstract Object executeSymbol(VirtualFrame frame, Object o);

    @SuppressWarnings("unused")
    @Specialization
    protected RSymbol doNull(VirtualFrame frame, RNull value) {
        throw RError.error(frame, getEncapsulatingSourceSection(), RError.Message.INVALID_TYPE_LENGTH, "symbol", 0);
    }

    @Specialization
    protected RSymbol doInteger(VirtualFrame frame, int value) {
        return backQuote(toString.executeString(frame, value));
    }

    @Specialization
    protected RSymbol doDouble(VirtualFrame frame, double value) {
        return backQuote(toString.executeString(frame, value));
    }

    @Specialization
    protected RSymbol doLogical(VirtualFrame frame, byte value) {
        return backQuote(toString.executeString(frame, value));
    }

    @Specialization
    protected RSymbol doString(String value) {
        return RDataFactory.createSymbol(value);
    }

    @Specialization
    protected RSymbol doStringVector(@SuppressWarnings("unused") VirtualFrame frame, RStringVector value) {
        // Only element 0 interpreted
        return doString(value.getDataAt(0));
    }

    @Specialization
    protected RSymbol doIntegerVector(VirtualFrame frame, RIntVector value) {
        return doInteger(frame, value.getDataAt(0));
    }

    @Specialization
    protected RSymbol doDoubleVector(VirtualFrame frame, RDoubleVector value) {
        return doDouble(frame, value.getDataAt(0));
    }

    @Specialization
    protected RSymbol doLogicalVector(VirtualFrame frame, RLogicalVector value) {
        return doLogical(frame, value.getDataAt(0));
    }

    @SlowPath
    private static RSymbol backQuote(String s) {
        return RDataFactory.createSymbol("`" + s + "`");
    }

}
