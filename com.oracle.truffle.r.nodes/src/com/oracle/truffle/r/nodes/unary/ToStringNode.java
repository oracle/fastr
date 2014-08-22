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
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.SlowPath;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.env.*;

public abstract class ToStringNode extends UnaryNode {

    @Child ToStringNode recursiveToString;

    @CompilationFinal private boolean quotes = true;

    @CompilationFinal private String separator;

    @CompilationFinal private boolean intL = false;

    private String toStringRecursive(VirtualFrame frame, Object o) {
        if (recursiveToString == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            recursiveToString = insert(ToStringNodeFactory.create(null));
            recursiveToString.setSeparator(separator);
            recursiveToString.setQuotes(quotes);
            recursiveToString.setIntL(intL);
        }
        return recursiveToString.executeString(frame, o);
    }

    // FIXME custom separators require breaking some rules
    // FIXME separator should be a @NodeField - not possible as default values cannot be set

    public ToStringNode() {
        this.separator = DEFAULT_SEPARATOR;
    }

    public ToStringNode(ToStringNode prev) {
        this.separator = prev.separator;
        this.quotes = prev.quotes;
        this.intL = prev.intL;
    }

    public static final String DEFAULT_SEPARATOR = ", ";

    public final void setSeparator(String separator) {
        this.separator = separator;
    }

    public final void setIntL(boolean intL) {
        this.intL = intL;
    }

    @Override
    public abstract String execute(VirtualFrame frame);

    public abstract String execute(VirtualFrame frame, Object o);

    public abstract String executeString(VirtualFrame frame, Object o);

    @Specialization
    protected String toString(String value) {
        if (RRuntime.isNA(value)) {
            return value;
        }
        if (quotes) {
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
        return RRuntime.intToString(operand, intL);
    }

    @Specialization
    protected String toString(double operand) {
        return RRuntime.doubleToString(operand);
    }

    @Specialization
    protected String toString(byte operand) {
        return RRuntime.logicalToString(operand);
    }

    @SlowPath
    @Specialization
    protected String toString(RIntVector vector) {
        int length = vector.getLength();
        if (length == 0) {
            return "integer(0)";
        }
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < length; i++) {
            b.append(toString(vector.getDataAt(i)));
            if (i < length - 1) {
                b.append(separator);
            }
        }
        return RRuntime.toString(b);
    }

    @SlowPath
    @Specialization
    protected String toString(RDoubleVector vector) {
        int length = vector.getLength();
        if (length == 0) {
            return "numeric(0)";
        }
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < length; i++) {
            b.append(toString(vector.getDataAt(i)));
            if (i < length - 1) {
                b.append(separator);
            }
        }
        return RRuntime.toString(b);
    }

    @SlowPath
    @Specialization
    protected String toString(RStringVector vector) {
        int length = vector.getLength();
        if (length == 0) {
            return "character(0)";
        }
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < length; ++i) {
            b.append(toString(vector.getDataAt(i)));
            if (i < length - 1) {
                b.append(separator);
            }
        }
        return RRuntime.toString(b);
    }

    @SlowPath
    @Specialization
    protected String toString(RLogicalVector vector) {
        int length = vector.getLength();
        if (length == 0) {
            return "logical(0)";
        }
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < length; ++i) {
            b.append(toString(vector.getDataAt(i)));
            if (i < length - 1) {
                b.append(separator);
            }
        }
        return RRuntime.toString(b);
    }

    @SlowPath
    @Specialization
    protected String toString(RRawVector vector) {
        int length = vector.getLength();
        if (length == 0) {
            return "raw(0)";
        }
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < length; ++i) {
            b.append(toString(vector.getDataAt(i)));
            if (i < length - 1) {
                b.append(separator);
            }
        }
        return RRuntime.toString(b);
    }

    @SlowPath
    @Specialization
    protected String toString(RComplexVector vector) {
        int length = vector.getLength();
        if (length == 0) {
            return "complex(0)";
        }
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < length; ++i) {
            b.append(toString(vector.getDataAt(i)));
            if (i < length - 1) {
                b.append(separator);
            }
        }
        return RRuntime.toString(b);
    }

    @SlowPath
    @Specialization
    protected String toString(VirtualFrame frame, RList vector) {
        int length = vector.getLength();
        if (length == 0) {
            return "list()";
        }
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < length; ++i) {
            Object value = vector.getDataAt(i);
            if (value instanceof RList) {
                RList l = (RList) value;
                if (l.getLength() == 0) {
                    b.append("list()");
                } else {
                    b.append("list(").append(toStringRecursive(frame, l)).append(')');
                }
            } else {
                b.append(toStringRecursive(frame, value));
            }
            if (i < length - 1) {
                b.append(separator);
            }
        }
        return RRuntime.toString(b);
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

    public void setQuotes(boolean quotes) {
        this.quotes = quotes;
    }
}
