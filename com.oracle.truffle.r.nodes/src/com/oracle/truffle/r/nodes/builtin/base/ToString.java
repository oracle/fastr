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
package com.oracle.truffle.r.nodes.builtin.base;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.SlowPath;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin("toString")
@SuppressWarnings("unused")
public abstract class ToString extends RBuiltinNode {

    @Child ToString recursiveToString;

    @CompilationFinal private boolean quotes = true;

    @CompilationFinal private String separator;

    @CompilationFinal private boolean intL = false;

    private String toStringRecursive(VirtualFrame frame, Object o) {
        if (recursiveToString == null) {
            CompilerDirectives.transferToInterpreter();
            recursiveToString = insert(ToStringFactory.create(new RNode[1], getBuiltin()));
            recursiveToString.setSeparator(separator);
            recursiveToString.setQuotes(quotes);
            recursiveToString.setIntL(intL);
        }
        return recursiveToString.executeString(frame, o);
    }

    // FIXME custom separators require breaking some rules
    // FIXME separator should be a @NodeField - not possible as default values cannot be set

    public ToString() {
        this.separator = DEFAULT_SEPARATOR;
    }

    public ToString(ToString prev) {
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
    public String toString(String value) {
        if (RRuntime.isNA(value)) {
            return value;
        }
        if (quotes) {
            return RRuntime.quoteString(value);
        }
        return value;

    }

    @Specialization
    public String toString(RNull vector) {
        controlVisibility();
        return "NULL";
    }

    @Specialization
    public String toString(RFunction function) {
        controlVisibility();
        return RRuntime.toString(function);
    }

    @Specialization
    public String toString(RComplex complex) {
        controlVisibility();
        return complex.toString();
    }

    @Specialization
    public String toString(RRaw raw) {
        controlVisibility();
        return raw.toString();
    }

    @Specialization()
    public String toString(int operand) {
        controlVisibility();
        return RRuntime.intToString(operand, intL);
    }

    @Specialization
    public String toString(double operand) {
        controlVisibility();
        return RRuntime.doubleToString(operand);
    }

    @Specialization
    public String toString(byte operand) {
        controlVisibility();
        return RRuntime.logicalToString(operand);
    }

    @SlowPath
    @Specialization(order = 100)
    public String toString(RIntVector vector) {
        controlVisibility();
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
    @Specialization(order = 200)
    public String toString(RDoubleVector vector) {
        controlVisibility();
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
    public String toString(RStringVector vector) {
        controlVisibility();
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
    @Specialization(order = 300)
    public String toString(RLogicalVector vector) {
        controlVisibility();
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
    public String toString(RRawVector vector) {
        controlVisibility();
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
    @Specialization(order = 500)
    public String toString(RComplexVector vector) {
        controlVisibility();
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
    @Specialization(order = 600)
    public String toString(VirtualFrame frame, RList vector) {
        controlVisibility();
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
    public String toString(VirtualFrame frame, RIntSequence vector) {
        controlVisibility();
        return toStringRecursive(frame, vector.createVector());
    }

    @Specialization
    public String toString(VirtualFrame frame, RDoubleSequence vector) {
        controlVisibility();
        return toStringRecursive(frame, vector.createVector());
    }

    @SlowPath
    @Specialization
    public String toString(REnvironment env) {
        controlVisibility();
        return env.toString();
    }

    public void setQuotes(boolean quotes) {
        this.quotes = quotes;
    }
}
