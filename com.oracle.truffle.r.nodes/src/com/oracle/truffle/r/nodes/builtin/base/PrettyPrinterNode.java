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

import static com.oracle.truffle.r.nodes.RTypesGen.*;

import java.util.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.SlowPath;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.impl.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

@NodeChild(value = "operand", type = RNode.class)
@NodeField(name = "printingAttributes", type = boolean.class)
public abstract class PrettyPrinterNode extends RNode {

    @Child protected ToString toString;

    @Override
    public abstract String execute(VirtualFrame frame);

    public abstract String executeString(VirtualFrame frame, Object o);

    public static final int CONSOLE_WIDTH = 80;

    private static final String NA_HEADER = "<NA>";

    @Child PrettyPrinterNode recursivePrettyPrinter;

    protected abstract boolean isPrintingAttributes();

    private Object prettyPrintAttributes(VirtualFrame frame, Object o) {
        if (recursivePrettyPrinter == null) {
            CompilerDirectives.transferToInterpreter();
            recursivePrettyPrinter = adoptChild(PrettyPrinterNodeFactory.create(null, true));
        }
        return recursivePrettyPrinter.executeString(frame, o);
    }

    @Specialization
    @SuppressWarnings("unused")
    public String prettyPrint(RNull operand) {
        return "NULL";
    }

    @Specialization
    public String prettyPrint(VirtualFrame frame, byte operand) {
        return prettyPrint(frame, RDataFactory.createLogicalVectorFromScalar(operand));
    }

    @Specialization
    public String prettyPrint(VirtualFrame frame, int operand) {
        return prettyPrint(frame, RDataFactory.createIntVectorFromScalar(operand));
    }

    @Specialization
    public String prettyPrint(VirtualFrame frame, double operand) {
        return prettyPrint(frame, RDataFactory.createDoubleVectorFromScalar(operand));
    }

    @Specialization
    public String prettyPrint(VirtualFrame frame, RComplex operand) {
        return prettyPrint(frame, RDataFactory.createComplexVectorFromScalar(operand));
    }

    @Specialization
    public String prettyPrint(VirtualFrame frame, String operand) {
        return prettyPrint(frame, RDataFactory.createStringVectorFromScalar(operand));
    }

    @Specialization
    public String prettyPrint(VirtualFrame frame, RRaw operand) {
        return prettyPrint(frame, RDataFactory.createRawVectorFromScalar(operand));
    }

    @Specialization
    public String prettyPrint(RFunction operand) {
        return ((DefaultCallTarget) operand.getTarget()).getRootNode().getSourceSection().getCode();
    }

    @Specialization
    public String prettyPrint(VirtualFrame frame, RIntVector operand) {
        int length = operand.getLength();
        String[] values = new String[length];
        for (int i = 0; i < length; i++) {
            int data = operand.getDataAt(i);
            values[i] = RRuntime.intToString(data, false);
        }
        return printVector(frame, operand, values, false, false);
    }

    @SlowPath
    private String printAttributes(VirtualFrame frame, RVector vector, Map<String, Object> attributes) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, Object> attr : attributes.entrySet()) {
            if (attr.getKey().equals(RRuntime.NAMES_ATTR_KEY) && !vector.hasDimensions()) {
                // names attribute already printed
                continue;
            }
            if (attr.getKey().equals(RRuntime.DIM_ATTR_KEY)) {
                // dim attribute never gets printed
                continue;
            }
            builder.append("\n");
            builder.append("attr(,\"" + attr.getKey() + "\")\n");
            builder.append(prettyPrintAttributes(frame, attr.getValue()));
        }
        return builder.toString();
    }

    @SlowPath
    private String printVector(VirtualFrame frame, RVector vector, String[] values, boolean isStringVector, boolean isRawVector) {
        assert vector.getLength() == values.length;
        if (values.length == 0) {
            return RRuntime.classToString(vector.getElementClass()) + "(0)";
        } else {
            boolean printNamesHeader = (!vector.hasDimensions() && vector.getNames() != null && vector.getNames() != RNull.instance);
            RStringVector names = printNamesHeader ? (RStringVector) vector.getNames() : null;
            int maxWidth = 0;
            for (String s : values) {
                maxWidth = Math.max(maxWidth, s.length());
            }
            if (printNamesHeader) {
                for (int i = 0; i < names.getLength(); i++) {
                    String s = names.getDataAt(i);
                    if (s == RRuntime.STRING_NA) {
                        s = NA_HEADER;
                    }
                    maxWidth = Math.max(maxWidth, s.length());
                }
            }
            int columnWidth = maxWidth + 1; // There is a blank before each column.
            int leftWidth = 0;
            int maxPositionLength = 0;
            if (!printNamesHeader) {
                maxPositionLength = Integer.toString(vector.getLength()).length();
                leftWidth = maxPositionLength + 2; // There is [] around the number.
            }
            int forColumns = CONSOLE_WIDTH - leftWidth;
            int numberOfColumns = Math.max(1, forColumns / columnWidth);

            int index = 0;
            StringBuilder builder = new StringBuilder();
            StringBuilder headerBuilder = null;
            if (printNamesHeader) {
                headerBuilder = new StringBuilder();
            }
            while (index < vector.getLength()) {
                if (!printNamesHeader) {
                    int position = index + 1;
                    String positionString = Integer.toString(position);
                    for (int i = 0; i < maxPositionLength - positionString.length(); ++i) {
                        builder.append(' ');
                    }
                    builder.append("[").append(positionString).append("]");
                }
                for (int j = 0; j < numberOfColumns && index < vector.getLength(); ++j) {
                    String valueString = values[index];
                    if (!printNamesHeader) {
                        builder.append(' ');
                        // for some reason vectors of strings are printed differently
                        if (isStringVector) {
                            builder.append(valueString);
                        }
                        for (int k = 0; k < (columnWidth - 1) - valueString.length(); ++k) {
                            builder.append(' ');
                        }
                        if (!isStringVector) {
                            builder.append(valueString);
                        }
                    } else {
                        int actualColumnWidth = columnWidth;
                        if (j == 0) {
                            actualColumnWidth--;
                        }
                        // for some reason vectors of raw values are printed differently
                        if (!isRawVector) {
                            for (int k = 0; k < actualColumnWidth - valueString.length(); ++k) {
                                builder.append(' ');
                            }
                        }
                        builder.append(valueString);
                        if (isRawVector) {
                            builder.append(' ');
                        }
                        String headerString = names.getDataAt(index);
                        if (headerString == RRuntime.STRING_NA) {
                            headerString = NA_HEADER;
                        }
                        for (int k = 0; k < actualColumnWidth - headerString.length(); ++k) {
                            headerBuilder.append(' ');
                        }
                        headerBuilder.append(headerString);
                    }
                    index++;
                }
                builder.append('\n');
                if (printNamesHeader) {
                    headerBuilder.append('\n');
                    headerBuilder.append(builder);
                    builder = new StringBuilder();
                }
            }
            String result = (!printNamesHeader ? builder.deleteCharAt(builder.length() - 1).toString() : headerBuilder.deleteCharAt(headerBuilder.length() - 1).toString());
            Map<String, Object> attributes = vector.getAttributes();
            if (attributes != null) {
                result = result + printAttributes(frame, vector, attributes);
            }
            return result;
        }
    }

    private String printMatrix(VirtualFrame frame, RVector vector) {
        if (toString == null) {
            CompilerDirectives.transferToInterpreter();
            toString = adoptChild(ToStringFactory.create(new RNode[1], null, null));
        }
        return toString.executeString(frame, vector);
    }

    @Specialization(order = 100, guards = "isMatrix")
    public String prettyPrintM(VirtualFrame frame, RDoubleVector operand) {
        return printMatrix(frame, operand);
    }

    @Specialization(order = 101, guards = "!isMatrix")
    public String prettyPrint(VirtualFrame frame, RDoubleVector operand) {
        int length = operand.getLength();
        String[] values = new String[length];
        for (int i = 0; i < length; i++) {
            double data = operand.getDataAt(i);
            values[i] = doubleToStringPrintFormat(data, calcRoundFactor(data, 10000000));
        }
        return printVector(frame, operand, values, false, false);
    }

    protected static boolean isMatrix(RDoubleVector v) {
        return v.isMatrix();
    }

    private static double calcRoundFactor(double input, long maxFactor) {
        if (Double.isNaN(input) || Double.isInfinite(input) || input == 0.0) {
            return maxFactor * 10;
        }
        double data = input;
        double factor = 1;
        if (Math.abs(data) > 1000000000000L) {
            while (Math.abs(data) > 10000000L) {
                data = data / 10;
                factor /= 10;
            }
        } else if ((int) data != 0) {
            while (Math.abs(data) < maxFactor / 10) {
                data = data * 10;
                factor *= 10;
            }
        } else {
            long current = maxFactor / 10;
            while (Math.abs(data) < 1 && current > 1) {
                data = data * 10;
                current = current * 10;
            }
            return current;
        }
        return factor;
    }

    private static String doubleToStringPrintFormat(double input, double roundFactor) {
        double data = input;
        if (!Double.isNaN(data) && !Double.isInfinite(data)) {
            if (roundFactor < 1) {
                double inverse = 1 / roundFactor;
                data = Math.round(data / inverse) * inverse;
            } else {
                data = Math.round(data * roundFactor) / roundFactor;
            }
        }
        return RRuntime.doubleToString(data);
    }

    @Specialization
    public String prettyPrint(VirtualFrame frame, RStringVector operand) {
        int length = operand.getLength();
        String[] values = new String[length];
        for (int i = 0; i < length; i++) {
            String data = operand.getDataAt(i);
            values[i] = RRuntime.quoteString(data);
        }
        return printVector(frame, operand, values, true, false);
    }

    @Specialization
    public String prettyPrint(VirtualFrame frame, RLogicalVector operand) {
        int length = operand.getLength();
        String[] values = new String[length];
        for (int i = 0; i < length; i++) {
            byte data = operand.getDataAt(i);
            values[i] = RRuntime.logicalToString(data);
        }
        return printVector(frame, operand, values, false, false);
    }

    @Specialization
    public String prettyPrint(VirtualFrame frame, RRawVector operand) {
        int length = operand.getLength();
        String[] values = new String[length];
        for (int i = 0; i < length; i++) {
            RRaw data = operand.getDataAt(i);
            values[i] = data.toString();
        }
        return printVector(frame, operand, values, false, true);
    }

    @Specialization
    public String prettyPrint(VirtualFrame frame, RComplexVector operand) {
        int length = operand.getLength();
        String[] values = new String[length];
        for (int i = 0; i < length; i++) {
            RComplex data = operand.getDataAt(i);
            double factor = calcRoundFactor(data.getRealPart(), 10000000);
            values[i] = data.toString(doubleToStringPrintFormat(data.getRealPart(), factor), doubleToStringPrintFormat(data.getImaginaryPart(), calcRoundFactor(data.getImaginaryPart(), 10000000)));
        }
        return printVector(frame, operand, values, false, false);
    }

    @SlowPath
    @Specialization(order = 1000, guards = "!isMatrix")
    public String prettyPrint(VirtualFrame frame, RList operand) {
        int length = operand.getLength();
        if (length == 0) {
            return "list()";
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < length; i++) {
                if (isPrintingAttributes() && operand.elementNamePrefix != null) {
                    sb.append(operand.elementNamePrefix);
                }
                Object name = operand.getNameAt(i);
                sb.append(name).append('\n');
                Object value = operand.getDataAt(i);
                sb.append(printSingleListValue(frame, value, name)).append("\n\n");
            }
            String result = sb.deleteCharAt(sb.length() - 1).toString();
            Map<String, Object> attributes = operand.getAttributes();
            if (attributes != null) {
                result = result + printAttributes(frame, operand, attributes);
            }
            return result;

        }
    }

    // TODO refactor: too much code reuse
    // FIXME support nesting levels >1
    public String prettyPrintNestedList(VirtualFrame frame, RList operand, Object listName) {
        int length = operand.getLength();
        if (length == 0) {
            return "list()";
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < length; i++) {
                if (isPrintingAttributes() && operand.elementNamePrefix != null) {
                    sb.append(operand.elementNamePrefix);
                } else {
                    sb.append(listName);
                }
                Object name = operand.getNameAt(i);
                sb.append(name).append('\n');
                Object value = operand.getDataAt(i);
                sb.append(printSingleListValue(frame, value, name)).append("\n\n");
            }
            String result = sb.deleteCharAt(sb.length() - 1).toString();
            Map<String, Object> attributes = operand.getAttributes();
            if (attributes != null) {
                result = result + printAttributes(frame, operand, attributes);
            }
            return result;

        }
    }

    @Specialization(order = 1001, guards = "isMatrix")
    public String prettyPrintM(VirtualFrame frame, RList operand) {
        int length = operand.getLength();
        if (length == 0) {
            return "list()";
        } else {
            return printMatrix(frame, operand);
        }
    }

    protected static boolean isMatrix(RList l) {
        return l.isMatrix();
    }

    @SlowPath
    protected Object printSingleListValue(VirtualFrame frame, Object argumentsValue0, Object listElementName) {
        if (RTYPES.isByte(argumentsValue0)) {
            byte argumentsValue0Cast = RTYPES.asByte(argumentsValue0);
            return prettyPrint(frame, argumentsValue0Cast);
        }
        if (RTYPES.isInteger(argumentsValue0)) {
            int argumentsValue0Cast = RTYPES.asInteger(argumentsValue0);
            return prettyPrint(frame, argumentsValue0Cast);
        }
        if (RTYPES.isDouble(argumentsValue0)) {
            double argumentsValue0Cast = RTYPES.asDouble(argumentsValue0);
            return prettyPrint(frame, argumentsValue0Cast);
        }
        if (RTYPES.isRRaw(argumentsValue0)) {
            RRaw argumentsValue0Cast = RTYPES.asRRaw(argumentsValue0);
            return prettyPrint(frame, argumentsValue0Cast);
        }
        if (RTYPES.isRComplex(argumentsValue0)) {
            RComplex argumentsValue0Cast = RTYPES.asRComplex(argumentsValue0);
            return prettyPrint(frame, argumentsValue0Cast);
        }
        if (RTYPES.isString(argumentsValue0)) {
            String argumentsValue0Cast = RTYPES.asString(argumentsValue0);
            return prettyPrint(frame, argumentsValue0Cast);
        }
        if (RTYPES.isRIntSequence(argumentsValue0)) {
            RIntSequence argumentsValue0Cast = RTYPES.asRIntSequence(argumentsValue0);
            return prettyPrint(frame, argumentsValue0Cast);
        }
        if (RTYPES.isRDoubleSequence(argumentsValue0)) {
            RDoubleSequence argumentsValue0Cast = RTYPES.asRDoubleSequence(argumentsValue0);
            return prettyPrint(frame, argumentsValue0Cast);
        }
        if (RTYPES.isRIntVector(argumentsValue0)) {
            RIntVector argumentsValue0Cast = RTYPES.asRIntVector(argumentsValue0);
            return prettyPrint(frame, argumentsValue0Cast);
        }
        if (RTYPES.isRDoubleVector(argumentsValue0)) {
            RDoubleVector argumentsValue0Cast = RTYPES.asRDoubleVector(argumentsValue0);
            return prettyPrint(frame, argumentsValue0Cast);
        }
        if (RTYPES.isRRawVector(argumentsValue0)) {
            RRawVector argumentsValue0Cast = RTYPES.asRRawVector(argumentsValue0);
            return prettyPrint(frame, argumentsValue0Cast);
        }
        if (RTYPES.isRComplexVector(argumentsValue0)) {
            RComplexVector argumentsValue0Cast = RTYPES.asRComplexVector(argumentsValue0);
            return prettyPrint(frame, argumentsValue0Cast);
        }
        if (RTYPES.isRStringVector(argumentsValue0)) {
            RStringVector argumentsValue0Cast = RTYPES.asRStringVector(argumentsValue0);
            return prettyPrint(frame, argumentsValue0Cast);
        }
        if (RTYPES.isRLogicalVector(argumentsValue0)) {
            RLogicalVector argumentsValue0Cast = RTYPES.asRLogicalVector(argumentsValue0);
            return prettyPrint(frame, argumentsValue0Cast);
        }
        if (RTYPES.isRFunction(argumentsValue0)) {
            RFunction argumentsValue0Cast = RTYPES.asRFunction(argumentsValue0);
            return prettyPrint(argumentsValue0Cast);
        }
        if (RTYPES.isRNull(argumentsValue0)) {
            RNull argumentsValue0Cast = RTYPES.asRNull(argumentsValue0);
            return prettyPrint(argumentsValue0Cast);
        }
        if (RTYPES.isRList(argumentsValue0)) {
            RList argumentsValue0Cast = RTYPES.asRList(argumentsValue0);
            return prettyPrintNestedList(frame, argumentsValue0Cast, listElementName);
        }
        if (RTYPES.isRInvisible(argumentsValue0)) {
            RInvisible argumentsValue0Cast = RTYPES.asRInvisible(argumentsValue0);
            return prettyPrint(frame, argumentsValue0Cast);
        }
        throw new UnsupportedOperationException("Unsupported values" + argumentsValue0);
    }

    @Specialization
    public String prettyPrint(VirtualFrame frame, RIntSequence operand) {
        return prettyPrint(frame, (RIntVector) operand.createVector());
    }

    @Specialization
    public String prettyPrint(VirtualFrame frame, RDoubleSequence operand) {
        return prettyPrint(frame, (RDoubleVector) operand.createVector());
    }

    @Specialization
    public String prettyPrint(VirtualFrame frame, RInvisible operand) {
        return (String) printSingleListValue(frame, operand.get(), "");
    }

}
