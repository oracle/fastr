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
            recursiveToString = adoptChild(ToStringFactory.create(new RNode[1], getBuiltin()));
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
        return "NULL";
    }

    @Specialization
    public String toString(RFunction function) {
        return function.toString();
    }

    @Specialization
    public String toString(RComplex complex) {
        return complex.toString();
    }

    @Specialization
    public String toString(RRaw complex) {
        return complex.toString();
    }

    @Specialization()
    public String toString(int operand) {
        return RRuntime.intToString(operand, intL);
    }

    @Specialization
    public String toString(double operand) {
        return RRuntime.doubleToString(operand);
    }

    @Specialization
    public String toString(byte operand) {
        return RRuntime.logicalToString(operand);
    }

    @SlowPath
    @Specialization(order = 100, guards = "!hasDimensions")
    public String toString(RIntVector vector) {
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
        return b.toString();
    }

    @Specialization(order = 101, guards = "hasDimensions")
    public String toStringDim(final RIntVector vector) {
        assert vector.getDimensions().length == 2;
        return toStringDim0(vector.getDimensions()[0], vector.getDimensions()[1], new DataToStringClosure() {

            @Override
            @SlowPath
            String dataToString(int index) {
                return ToString.this.toString(vector.getDataAt(index));
            }
        });
    }

    @SlowPath
    @Specialization(order = 200, guards = "!hasDimensions")
    public String toString(RDoubleVector vector) {
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
        return b.toString();
    }

    @SlowPath
    @Specialization(order = 201, guards = "hasDimensions")
    public String toStringDim(final RDoubleVector vector) {
        assert vector.getDimensions().length == 2;
        return toStringDim0(vector.getDimensions()[0], vector.getDimensions()[1], new DataToStringClosure() {

            @Override
            @SlowPath
            String dataToString(int index) {
                return ToString.this.toString(vector.getDataAt(index));
            }
        });
    }

    @SlowPath
    @Specialization
    public String toString(RStringVector vector) {
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
        return b.toString();
    }

    @SlowPath
    @Specialization(order = 300, guards = "!hasDimensions")
    public String toString(RLogicalVector vector) {
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
        return b.toString();
    }

    @Specialization(order = 301, guards = "hasDimensions")
    public String toStringDim(final RLogicalVector vector) {
        assert vector.getDimensions().length == 2;
        return toStringDim0(vector.getDimensions()[0], vector.getDimensions()[1], new DataToStringClosure() {

            @Override
            @SlowPath
            String dataToString(int index) {
                return ToString.this.toString(vector.getDataAt(index));
            }
        });
    }

    @SlowPath
    @Specialization
    public String toString(RRawVector vector) {
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
        return b.toString();
    }

    @SlowPath
    @Specialization(order = 500, guards = "!hasDimensions")
    public String toString(RComplexVector vector) {
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
        return b.toString();
    }

    @SlowPath
    @Specialization(order = 501, guards = "hasDimensions")
    public String toStringDim(final RComplexVector vector) {
        assert vector.getDimensions().length == 2;
        return toStringDim0(vector.getDimensions()[0], vector.getDimensions()[1], new DataToStringClosure() {

            @Override
            @SlowPath
            String dataToString(int index) {
                return ToString.this.toString(vector.getDataAt(index));
            }
        });
    }

    @SlowPath
    @Specialization(order = 600, guards = "!hasDimensions")
    public String toString(VirtualFrame frame, RList vector) {
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
        return b.toString();
    }

    @SlowPath
    @Specialization(order = 601, guards = "hasDimensions")
    public String toStringDim(final VirtualFrame frame, final RList vector) {
        int length = vector.getLength();
        if (length == 0) {
            return "list()";
        }
        assert vector.getDimensions().length == 2;
        return toStringDim0(vector.getDimensions()[0], vector.getDimensions()[1], new DataToStringClosure() {

            @Override
            String dataToString(int index) {
                Object d = vector.getDataAt(index);
                if (d instanceof RList) {
                    return "List," + ((RList) d).getLength();
                } else {
                    return toStringRecursive(frame, d);
                }
            }
        });
    }

    @Specialization
    public String toString(VirtualFrame frame, RIntSequence vector) {
        return toStringRecursive(frame, vector.createVector());
    }

    @Specialization
    public String toString(VirtualFrame frame, RDoubleSequence vector) {
        return toStringRecursive(frame, vector.createVector());
    }

    @SlowPath
    @Specialization
    public String toString(REnvironment env) {
        return env.toString();
    }

    @Specialization
    public String toString(VirtualFrame frame, RInvisible inv) {
        return toStringRecursive(frame, inv.get());
    }

    public void setQuotes(boolean quotes) {
        this.quotes = quotes;
    }

    protected static boolean hasDimensions(RAbstractVector v) {
        // for printing purposes, no dimensions and dimensions == 1 is the same
        return v.hasDimensions() && v.getDimensions().length > 1;
    }

    @SlowPath
    protected static String padColHeader(int r, int dataColWidth) {
        StringBuilder sb = new StringBuilder();
        String rs = Integer.toString(r);
        int wdiff = dataColWidth - (rs.length() + 3); // 3: [,]
        if (wdiff > 0) {
            spaces(sb, wdiff);
        }
        return sb.append("[,").append(rs).append(']').toString();
    }

    @SlowPath
    protected static String rowHeader(int c) {
        return new StringBuilder("[").append(c).append(",]").toString();
    }

    @SlowPath
    protected static void spaces(StringBuilder sb, int s) {
        for (int i = 0; i < s; ++i) {
            sb.append(' ');
        }
    }

    @SlowPath
    protected String toStringDim0(int nrow, int ncol, DataToStringClosure dtsc) {
        // FIXME support more than two dimensions
        // FIXME support empty matrices

        // prepare data (relevant for column widths)
        String[] dataStrings = new String[nrow * ncol];
        int[] dataColWidths = new int[ncol];
        for (int r = 0; r < nrow; ++r) {
            for (int c = 0; c < ncol; ++c) {
                int index = c * nrow + r;
                String data = dtsc.dataToString(index);
                dataStrings[index] = data;
                if (data.length() > dataColWidths[c]) {
                    dataColWidths[c] = data.length();
                }
            }
        }

        int rowHeaderWidth = rowHeader(nrow).length();
        String rowFormat = "%" + rowHeaderWidth + "s";

        StringBuilder b = new StringBuilder();

        // column header
        spaces(b, rowHeaderWidth + 1);
        for (int c = 1; c <= ncol; ++c) {
            b.append(padColHeader(c, dataColWidths[c - 1]));
            if (c < ncol) {
                b.append(' ');
            }
        }
        b.append('\n');

        // rows
        for (int r = 1; r <= nrow; ++r) {
            b.append(String.format(rowFormat, rowHeader(r))).append(' ');
            for (int c = 1; c <= ncol; ++c) {
                String cellFormat = "%" + padColHeader(c, dataColWidths[c - 1]).length() + "s";
                b.append(String.format(cellFormat, dataStrings[(c - 1) * nrow + (r - 1)]));
                if (c < ncol) {
                    b.append(' ');
                }
            }
            if (r < nrow) {
                b.append('\n');
            }
        }

        return b.toString();
    }

    private abstract class DataToStringClosure {

        abstract String dataToString(int index);
    }

}
