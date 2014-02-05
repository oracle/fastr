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
import com.oracle.truffle.r.runtime.data.model.*;

@NodeChild(value = "operand", type = RNode.class)
@NodeFields({@NodeField(name = "printingAttributes", type = boolean.class), @NodeField(name = "printingVectorElements", type = boolean.class)})
public abstract class PrettyPrinterNode extends RNode {

    @Override
    public abstract String execute(VirtualFrame frame);

    public abstract String executeString(VirtualFrame frame, int o);

    public abstract String executeString(VirtualFrame frame, double o);

    public abstract String executeString(VirtualFrame frame, byte o);

    public abstract String executeString(VirtualFrame frame, Object o);

    public static final int CONSOLE_WIDTH = 80;

    private static final String NA_HEADER = "<NA>";

    @Child PrettyPrinterNode attributePrettyPrinter;
    @Child PrettyPrinterNode vectorElementPrettyPrinter;
    @Child PrettyPrinterNode recursivePrettyPrinter;

    protected abstract boolean isPrintingAttributes();

    protected abstract boolean isPrintingVectorElements();

    protected boolean printingVectorElements() {
        return isPrintingVectorElements();
    }

    private String prettyPrintAttributes(VirtualFrame frame, Object o) {
        if (attributePrettyPrinter == null) {
            CompilerDirectives.transferToInterpreter();
            attributePrettyPrinter = adoptChild(PrettyPrinterNodeFactory.create(null, true, false));
        }
        return attributePrettyPrinter.executeString(frame, o);
    }

    private String prettyPrintVectorElement(VirtualFrame frame, Object o) {
        if (vectorElementPrettyPrinter == null) {
            CompilerDirectives.transferToInterpreter();
            vectorElementPrettyPrinter = adoptChild(PrettyPrinterNodeFactory.create(null, false, true));
        }
        return vectorElementPrettyPrinter.executeString(frame, o);
    }

    private String prettyPrintRecursive(VirtualFrame frame, Object o) {
        if (recursivePrettyPrinter == null) {
            CompilerDirectives.transferToInterpreter();
            recursivePrettyPrinter = adoptChild(PrettyPrinterNodeFactory.create(null, isPrintingAttributes(), isPrintingVectorElements()));
        }
        return recursivePrettyPrinter.executeString(frame, o);
    }

    @Specialization
    @SuppressWarnings("unused")
    public String prettyPrint(RNull operand) {
        return "NULL";
    }

    @SlowPath
    @Specialization(order = 1, guards = "!printingVectorElements")
    public String prettyPrintVector(byte operand) {
        return "[1] " + RRuntime.logicalToString(operand);
    }

    @Specialization(order = 2, guards = "printingVectorElements")
    public String prettyPrint(byte operand) {
        return RRuntime.logicalToString(operand);
    }

    @SlowPath
    @Specialization(order = 10, guards = "!printingVectorElements")
    public String prettyPrintVector(int operand) {
        return "[1] " + RRuntime.intToString(operand, false);
    }

    @Specialization(order = 11, guards = "printingVectorElements")
    public String prettyPrint(int operand) {
        return RRuntime.intToString(operand, false);
    }

    @SlowPath
    @Specialization(order = 20, guards = "!printingVectorElements")
    public String prettyPrintVector(double operand) {
        return "[1] " + doubleToStringPrintFormat(operand, calcRoundFactor(operand, 10000000));
    }

    @Specialization(order = 21, guards = "printingVectorElements")
    public String prettyPrint(double operand) {
        return doubleToStringPrintFormat(operand, calcRoundFactor(operand, 10000000));
    }

    @SlowPath
    @Specialization(order = 30, guards = "!printingVectorElements")
    public String prettyPrintVector(RComplex operand) {
        double factor = calcRoundFactor(operand.getRealPart(), 10000000);
        return "[1] " +
                        operand.toString(doubleToStringPrintFormat(operand.getRealPart(), factor),
                                        doubleToStringPrintFormat(operand.getImaginaryPart(), calcRoundFactor(operand.getImaginaryPart(), 10000000)));
    }

    @Specialization(order = 31, guards = "printingVectorElements")
    public String prettyPrint(RComplex operand) {
        double factor = calcRoundFactor(operand.getRealPart(), 10000000);
        return operand.toString(doubleToStringPrintFormat(operand.getRealPart(), factor), doubleToStringPrintFormat(operand.getImaginaryPart(), calcRoundFactor(operand.getImaginaryPart(), 10000000)));
    }

    @SlowPath
    @Specialization(order = 40, guards = "!printingVectorElements")
    public String prettyPrintVector(String operand) {
        return "[1] " + RRuntime.quoteString(operand);
    }

    @Specialization(order = 41, guards = "printingVectorElements")
    public String prettyPrint(String operand) {
        return RRuntime.quoteString(operand);
    }

    @SlowPath
    @Specialization(order = 50, guards = "!printingVectorElements")
    public String prettyPrintVector(RRaw operand) {
        return "[1] " + operand.toString();
    }

    @Specialization(order = 51, guards = "printingVectorElements")
    public String prettyPrint(RRaw operand) {
        return operand.toString();
    }

    @Specialization
    public String prettyPrint(RFunction operand) {
        return ((RRootNode) ((DefaultCallTarget) operand.getTarget()).getRootNode()).getSourceCode();
    }

    @SlowPath
    private String printAttributes(VirtualFrame frame, RAbstractVector vector, Map<String, Object> attributes) {
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
    private String printVector(VirtualFrame frame, RAbstractVector vector, String[] values, boolean isStringVector, boolean isRawVector) {
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

    @SlowPath
    protected static String padColHeader(int r, int dataColWidth, RAbstractVector vector, boolean isList) {
        RList dimNames = vector.getDimNames();
        StringBuilder sb = new StringBuilder();
        int wdiff;
        if (dimNames == null || dimNames.getDataAt(1) == RNull.instance) {
            String rs = Integer.toString(r);
            wdiff = dataColWidth - (rs.length() + 3); // 3: [,]
            if (!isList && wdiff > 0) {
                spaces(sb, wdiff);
            }
            sb.append("[,").append(rs).append(']');
        } else {
            RStringVector dimNamesVector = (RStringVector) dimNames.getDataAt(1);
            String dimId = dimNamesVector.getDataAt(r - 1);
            wdiff = dataColWidth - dimId.length();
            if (!isList && wdiff > 0) {
                spaces(sb, wdiff);
            }
            sb.append(dimId);
        }
        if (isList && wdiff > 0) {
            spaces(sb, wdiff);
        }
        return sb.toString();
    }

    @SlowPath
    protected static String rowHeader(int c, RAbstractVector vector) {
        RList dimNames = vector.getDimNames();
        if (dimNames == null || dimNames.getDataAt(0) == RNull.instance) {
            return new StringBuilder("[").append(c).append(",]").toString();
        } else {
            RStringVector dimNamesVector = (RStringVector) dimNames.getDataAt(0);
            return dimNamesVector.getDataAt(c - 1);
        }
    }

    @SlowPath
    protected static void spaces(StringBuilder sb, int s) {
        for (int i = 0; i < s; ++i) {
            sb.append(' ');
        }
    }

    private static String getDimId(RAbstractVector vector, int dimLevel, int dimInd) {
        String dimId;
        RList dimNames = vector.getDimNames();
        if (dimNames == null || dimNames.getDataAt(dimLevel - 1) == RNull.instance) {
            dimId = Integer.toString(dimInd + 1);
        } else {
            RStringVector dimNamesVector = (RStringVector) dimNames.getDataAt(dimLevel - 1);
            dimId = dimNamesVector.getDataAt(dimInd);
        }
        return dimId;
    }

    @SlowPath
    private String printDim(VirtualFrame frame, RAbstractVector vector, boolean isList, int currentDimLevel, int arrayBase, int accDimensions, String header) {
        int[] dimensions = vector.getDimensions();
        if (currentDimLevel == 3) {
            StringBuffer sb = new StringBuffer();
            int dimSize = dimensions[currentDimLevel - 1];
            int matrixSize = dimensions[0] * dimensions[1];
            for (int dimInd = 0; dimInd < dimSize; dimInd++) {
                // CheckStyle: stop system..print check
                sb.append(", , ");
                // CheckStyle: resume system..print check
                sb.append(getDimId(vector, currentDimLevel, dimInd));
                sb.append(", ");
                sb.append(header);
                sb.append("\n\n");
                sb.append(printVector2Dim(frame, vector, dimensions, arrayBase + (dimInd * matrixSize), isList));
                sb.append("\n");
                if ((arrayBase + (dimInd * matrixSize) + matrixSize) < vector.getLength()) {
                    sb.append("\n");
                }
            }
            return sb.toString();
        } else {
            StringBuffer sb = new StringBuffer();
            int dimSize = dimensions[currentDimLevel - 1];
            int newAccDimensions = accDimensions / dimSize;
            for (int dimInd = 0; dimInd < dimSize; dimInd++) {
                int newArrayBase = arrayBase + newAccDimensions * dimInd;
                String dimId = getDimId(vector, currentDimLevel, dimInd);
                sb.append(printDim(frame, vector, isList, currentDimLevel - 1, newArrayBase, newAccDimensions, dimId + ", " + header));
            }
            return sb.toString();
        }
    }

    @SlowPath
    private String printVectorMultiDim(VirtualFrame frame, RAbstractVector vector, boolean isList) {
        int[] dimensions = vector.getDimensions();
        assert dimensions != null;
        int numDimensions = dimensions.length;
        assert numDimensions > 1;
        if (numDimensions == 2) {
            return printVector2Dim(frame, vector, dimensions, 0, isList);
        } else if (numDimensions == 3) {
            StringBuffer sb = new StringBuffer();
            int dimSize = dimensions[numDimensions - 1];
            int matrixSize = dimensions[0] * dimensions[1];
            for (int dimInd = 0; dimInd < dimSize; dimInd++) {
                // CheckStyle: stop system..print check
                sb.append(", , ");
                // CheckStyle: resume system..print check
                sb.append(getDimId(vector, numDimensions, dimInd));
                sb.append("\n\n");
                sb.append(printVector2Dim(frame, vector, dimensions, dimInd * matrixSize, isList));
                sb.append("\n");
                if (dimInd < (dimSize - 1)) {
                    sb.append("\n");
                }
            }
            return sb.toString();
        } else {
            StringBuffer sb = new StringBuffer();
            int dimSize = dimensions[numDimensions - 1];
            int accDimensions = vector.getLength() / dimSize;
            for (int dimInd = 0; dimInd < dimSize; dimInd++) {
                int arrayBase = accDimensions * dimInd;
                String dimId = getDimId(vector, numDimensions, dimInd);
                sb.append(printDim(frame, vector, isList, numDimensions - 1, arrayBase, accDimensions, dimId));
            }
            return sb.toString();
        }
    }

    @SlowPath
    private String printVector2Dim(VirtualFrame frame, RAbstractVector vector, int[] dimensions, int offset, boolean isList) {
        int nrow = dimensions[0];
        int ncol = dimensions[1];

        // prepare data (relevant for column widths)
        String[] dataStrings = new String[nrow * ncol];
        int[] dataColWidths = new int[ncol];
        RList dimNames = vector.getDimNames();
        RStringVector columnDimNames = null;
        if (dimNames != null && dimNames.getDataAt(1) != RNull.instance) {
            columnDimNames = (RStringVector) dimNames.getDataAt(1);
        }
        for (int r = 0; r < nrow; ++r) {
            for (int c = 0; c < ncol; ++c) {
                int index = c * nrow + r;
                String data = prettyPrintVectorElement(frame, vector.getDataAtAsObject(index + offset));
                dataStrings[index] = data;
                if (data.length() > dataColWidths[c]) {
                    dataColWidths[c] = data.length();
                }
                if (columnDimNames != null) {
                    String columnName = columnDimNames.getDataAt(c);
                    if (columnName.length() > dataColWidths[c]) {
                        dataColWidths[c] = columnName.length();
                    }
                }
            }
        }

        int rowHeaderWidth = rowHeader(nrow, vector).length();
        String rowFormat = "%" + rowHeaderWidth + "s";

        StringBuilder b = new StringBuilder();

        // column header
        spaces(b, rowHeaderWidth + 1);
        for (int c = 1; c <= ncol; ++c) {
            b.append(padColHeader(c, dataColWidths[c - 1], vector, isList));
            if (c < ncol) {
                b.append(' ');
            }
        }
        b.append('\n');

        // rows
        for (int r = 1; r <= nrow; ++r) {
            b.append(String.format(rowFormat, rowHeader(r, vector))).append(' ');
            for (int c = 1; c <= ncol; ++c) {
                String dataString = dataStrings[(c - 1) * nrow + (r - 1)];
                if (isList) {
                    // list elements are aligned to the left and vector's to the right
                    b.append(dataString);
                    spaces(b, padColHeader(c, dataColWidths[c - 1], vector, isList).length() - dataString.length());
                } else {
                    String cellFormat = "%" + padColHeader(c, dataColWidths[c - 1], vector, isList).length() + "s";
                    b.append(String.format(cellFormat, dataString));
                }
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

    @SlowPath
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

    // FIXME support nesting levels >1
    @SlowPath
    private String prettyPrintList0(VirtualFrame frame, RList operand, Object listName) {
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
                if (listName != null) {
                    name = new StringBuilder(listName.toString()).append(name).toString();
                }
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

    // special case to handle "[1]" appropriately (depending on the "printing mode")
    @Specialization(order = 70, guards = {"isLengthOne", "printingVectorElements"})
    public String prettyPrintLengthOne(RAbstractIntVector operand) {
        return prettyPrint(operand.getDataAt(0));
    }

    @Specialization(order = 71, guards = {"isLengthOne", "printingVectorElements"})
    public String prettyPrintLengthOne(RAbstractDoubleVector operand) {
        return prettyPrint(operand.getDataAt(0));
    }

    @Specialization(order = 72, guards = {"isLengthOne", "printingVectorElements"})
    public String prettyPrintLengthOne(RAbstractLogicalVector operand) {
        return prettyPrint(operand.getDataAt(0));
    }

    @Specialization(order = 73, guards = {"isLengthOne", "printingVectorElements"})
    public String prettyPrintLengthOne(VirtualFrame frame, RList operand) {
        return prettyPrintList0(frame, operand, null);
    }

    @Specialization(order = 74, guards = {"isLengthOne", "printingVectorElements"})
    public String prettyPrintLengthOne(VirtualFrame frame, RAbstractVector operand) {
        return prettyPrintRecursive(frame, operand.getDataAtAsObject(0));
    }

    @SlowPath
    @Specialization(order = 80, guards = "printingVectorElements")
    public String prettyPrintElements(RList operand) {
        return "List," + operand.getLength();
    }

    @Specialization(order = 81, guards = "printingVectorElements")
    public String prettyPrintElements(RAbstractVector operand) {
        return RRuntime.classToStringCap(operand.getElementClass()) + "," + operand.getLength();
    }

    @Specialization(order = 100, guards = {"twoDimsOrMore", "!printingVectorElements"})
    public String prettyPrintM(VirtualFrame frame, RList operand) {
        return printVectorMultiDim(frame, operand, true);
    }

    @Specialization(order = 101, guards = {"twoDimsOrMore", "!printingVectorElements"})
    public String prettyPrintM(VirtualFrame frame, RAbstractVector operand) {
        return printVectorMultiDim(frame, operand, false);
    }

    @SlowPath
    @Specialization(order = 200, guards = {"!twoDimsOrMore", "!printingVectorElements"})
    public String prettyPrint(VirtualFrame frame, RList operand) {
        return prettyPrintList0(frame, operand, null);
    }

    @Specialization(order = 300, guards = {"!twoDimsOrMore", "!printingVectorElements"})
    public String prettyPrint(VirtualFrame frame, RAbstractDoubleVector operand) {
        int length = operand.getLength();
        String[] values = new String[length];
        for (int i = 0; i < length; i++) {
            double data = operand.getDataAt(i);
            values[i] = prettyPrint(data);
        }
        return printVector(frame, operand, values, false, false);
    }

    @Specialization(order = 400, guards = {"!twoDimsOrMore", "!printingVectorElements"})
    public String prettyPrint(VirtualFrame frame, RAbstractIntVector operand) {
        int length = operand.getLength();
        String[] values = new String[length];
        for (int i = 0; i < length; i++) {
            int data = operand.getDataAt(i);
            values[i] = prettyPrint(data);
        }
        return printVector(frame, operand, values, false, false);
    }

    @Specialization(order = 500, guards = {"!twoDimsOrMore", "!printingVectorElements"})
    public String prettyPrint(VirtualFrame frame, RAbstractStringVector operand) {
        int length = operand.getLength();
        String[] values = new String[length];
        for (int i = 0; i < length; i++) {
            String data = operand.getDataAt(i);
            values[i] = prettyPrint(data);
        }
        return printVector(frame, operand, values, true, false);
    }

    @Specialization(order = 600, guards = {"!twoDimsOrMore", "!printingVectorElements"})
    public String prettyPrint(VirtualFrame frame, RAbstractLogicalVector operand) {
        int length = operand.getLength();
        String[] values = new String[length];
        for (int i = 0; i < length; i++) {
            byte data = operand.getDataAt(i);
            values[i] = prettyPrint(data);
        }
        return printVector(frame, operand, values, false, false);
    }

    @Specialization(order = 700, guards = {"!twoDimsOrMore", "!printingVectorElements"})
    public String prettyPrint(VirtualFrame frame, RAbstractRawVector operand) {
        int length = operand.getLength();
        String[] values = new String[length];
        for (int i = 0; i < length; i++) {
            RRaw data = operand.getDataAt(i);
            values[i] = prettyPrint(data);
        }
        return printVector(frame, operand, values, false, true);
    }

    @Specialization(order = 800, guards = {"!twoDimsOrMore", "!printingVectorElements"})
    public String prettyPrint(VirtualFrame frame, RAbstractComplexVector operand) {
        int length = operand.getLength();
        String[] values = new String[length];
        for (int i = 0; i < length; i++) {
            RComplex data = operand.getDataAt(i);
            values[i] = prettyPrint(data);
        }
        return printVector(frame, operand, values, false, false);
    }

    protected static boolean twoDimsOrMore(RAbstractVector v) {
        return v.hasDimensions() && v.getDimensions().length > 1;
    }

    protected static boolean twoDimsOrMore(RAbstractDoubleVector v) {
        return v.hasDimensions() && v.getDimensions().length > 1;
    }

    protected static boolean twoDimsOrMore(RAbstractIntVector v) {
        return v.hasDimensions() && v.getDimensions().length > 1;
    }

    protected static boolean twoDimsOrMore(RAbstractStringVector v) {
        return v.hasDimensions() && v.getDimensions().length > 1;
    }

    protected static boolean twoDimsOrMore(RAbstractLogicalVector v) {
        return v.hasDimensions() && v.getDimensions().length > 1;
    }

    protected static boolean twoDimsOrMore(RAbstractRawVector v) {
        return v.hasDimensions() && v.getDimensions().length > 1;
    }

    protected static boolean twoDimsOrMore(RAbstractComplexVector v) {
        return v.hasDimensions() && v.getDimensions().length > 1;
    }

    protected static boolean twoDimsOrMore(RList l) {
        return l.hasDimensions() && l.getDimensions().length > 1;
    }

    protected static boolean isLengthOne(RAbstractIntVector v) {
        return v.getLength() == 1;
    }

    protected static boolean isLengthOne(RAbstractDoubleVector v) {
        return v.getLength() == 1;
    }

    protected static boolean isLengthOne(RAbstractLogicalVector v) {
        return v.getLength() == 1;
    }

    protected static boolean isLengthOne(RList v) {
        return v.getLength() == 1;
    }

    protected static boolean isLengthOne(RAbstractVector v) {
        return v.getLength() == 1;
    }

    @SlowPath
    protected Object printSingleListValue(VirtualFrame frame, Object argumentsValue0, Object listElementName) {
        if (RTYPES.isByte(argumentsValue0)) {
            byte argumentsValue0Cast = RTYPES.asByte(argumentsValue0);
            return prettyPrintVector(argumentsValue0Cast);
        }
        if (RTYPES.isInteger(argumentsValue0)) {
            int argumentsValue0Cast = RTYPES.asInteger(argumentsValue0);
            return prettyPrintVector(argumentsValue0Cast);
        }
        if (RTYPES.isDouble(argumentsValue0)) {
            double argumentsValue0Cast = RTYPES.asDouble(argumentsValue0);
            return prettyPrintVector(argumentsValue0Cast);
        }
        if (RTYPES.isRRaw(argumentsValue0)) {
            RRaw argumentsValue0Cast = RTYPES.asRRaw(argumentsValue0);
            return prettyPrintVector(argumentsValue0Cast);
        }
        if (RTYPES.isRComplex(argumentsValue0)) {
            RComplex argumentsValue0Cast = RTYPES.asRComplex(argumentsValue0);
            return prettyPrintVector(argumentsValue0Cast);
        }
        if (RTYPES.isString(argumentsValue0)) {
            String argumentsValue0Cast = RTYPES.asString(argumentsValue0);
            return prettyPrintVector(argumentsValue0Cast);
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
            return prettyPrintList0(frame, argumentsValue0Cast, listElementName);
        }
        if (RTYPES.isRInvisible(argumentsValue0)) {
            RInvisible argumentsValue0Cast = RTYPES.asRInvisible(argumentsValue0);
            return prettyPrint(frame, argumentsValue0Cast);
        }
        throw new UnsupportedOperationException("Unsupported values" + argumentsValue0);
    }

    @Specialization
    public String prettyPrint(VirtualFrame frame, RInvisible operand) {
        return prettyPrintRecursive(frame, operand.get());
    }

}
