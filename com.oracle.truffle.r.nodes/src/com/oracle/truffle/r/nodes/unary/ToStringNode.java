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

import java.util.function.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.env.*;

@NodeFields({@NodeField(name = "quotes", type = boolean.class), @NodeField(name = "separator", type = String.class), @NodeField(name = "appendIntL", type = boolean.class)})
public abstract class ToStringNode extends UnaryNode {

    public static final String DEFAULT_SEPARATOR = ", ";

    @Child private ToStringNode recursiveToString;
    @CompilationFinal private Boolean separatorContainsNewlineCache;

    protected abstract boolean isQuotes();

    protected abstract String getSeparator();

    protected abstract boolean isAppendIntL();

    private String toStringRecursive(VirtualFrame frame, Object o) {
        if (recursiveToString == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            recursiveToString = insert(ToStringNodeFactory.create(null, isQuotes(), getSeparator(), isAppendIntL()));
        }
        return recursiveToString.executeString(frame, o);
    }

    // FIXME custom separators require breaking some rules
    // FIXME separator should be a @NodeField - not possible as default values cannot be set

    @Override
    public abstract String execute(VirtualFrame frame);

    public abstract String execute(VirtualFrame frame, Object o);

    public abstract String executeString(VirtualFrame frame, Object o);

    @Specialization
    protected String toString(String value) {
        if (RRuntime.isNA(value)) {
            return value;
        }
        if (isQuotes()) {
            return RRuntime.quoteString(value);
        }
        return value;

    }

    @Specialization
    protected String toString(@SuppressWarnings("unused") RNull vector) {
        return "NULL";
    }

    @Specialization
    protected String toString(RFunction function) {
        return RRuntime.toString(function);
    }

    @Specialization
    protected String toString(RComplex complex) {
        return complex.toString();
    }

    @Specialization
    protected String toString(RRaw raw) {
        return raw.toString();
    }

    @Specialization
    protected String toString(int operand) {
        return RRuntime.intToString(operand, isAppendIntL());
    }

    @Specialization
    protected String toString(double operand) {
        return RRuntime.doubleToString(operand);
    }

    @Specialization
    protected String toString(byte operand) {
        return RRuntime.logicalToString(operand);
    }

    @TruffleBoundary
    private String createResultForVector(RAbstractVector vector, String empty, IntFunction<String> elementFunction) {
        int length = vector.getLength();
        if (length == 0) {
            return empty;
        }
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < length; i++) {
            if (i > 0) {
                b.append(getSeparator());
            }
            b.append(elementFunction.apply(i));
        }
        return RRuntime.toString(b);
    }

    @Specialization
    protected String toString(RIntVector vector) {
        return createResultForVector(vector, "integer(0)", index -> toString(vector.getDataAt(index)));
    }

    @TruffleBoundary
    @Specialization
    protected String toString(RDoubleVector vector) {
        return createResultForVector(vector, "numeric(0)", index -> toString(vector.getDataAt(index)));
    }

    @TruffleBoundary
    @Specialization
    protected String toString(RStringVector vector) {
        return createResultForVector(vector, "character(0)", index -> toString(vector.getDataAt(index)));
    }

    @Specialization
    protected String toString(RLogicalVector vector) {
        return createResultForVector(vector, "logical(0)", index -> toString(vector.getDataAt(index)));
    }

    @Specialization
    protected String toString(RRawVector vector) {
        return createResultForVector(vector, "raw(0)", index -> toString(vector.getDataAt(index)));
    }

    @Specialization
    protected String toString(RComplexVector vector) {
        return createResultForVector(vector, "complex(0)", index -> toString(vector.getDataAt(index)));
    }

    @Specialization
    protected String toString(VirtualFrame frame, RList vector) {
        return createResultForVector(vector, "list()", index -> {
            Object value = vector.getDataAt(index);
            if (value instanceof RList) {
                RList l = (RList) value;
                if (l.getLength() == 0) {
                    return "list()";
                } else {
                    return "list(" + toStringRecursive(frame, l) + ')';
                }
            } else {
                return toStringRecursive(frame, value);
            }
        });
    }

    @Specialization
    protected String toString(VirtualFrame frame, RIntSequence vector) {
        return toStringRecursive(frame, vector.createVector());
    }

    @Specialization
    protected String toString(VirtualFrame frame, RDoubleSequence vector) {
        return toStringRecursive(frame, vector.createVector());
    }

    @Specialization
    protected String toString(REnvironment env) {
        return env.toString();
    }
}
