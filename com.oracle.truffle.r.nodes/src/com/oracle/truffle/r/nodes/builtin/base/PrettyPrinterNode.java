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

import java.util.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.SlowPath;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.impl.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.builtin.base.PrettyPrinterNodeFactory.PrettyPrinterSingleListElementNodeFactory;
import static com.oracle.truffle.r.nodes.RTypesGen.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@SuppressWarnings("unused")
@NodeChildren({@NodeChild(value = "operand", type = RNode.class), @NodeChild(value = "listElementName", type = RNode.class)})
@NodeField(name = "printingAttributes", type = boolean.class)
public abstract class PrettyPrinterNode extends RNode {

    @Override
    public abstract Object execute(VirtualFrame frame);

    public abstract Object executeString(VirtualFrame frame, int o, Object listElementName);

    public abstract Object executeString(VirtualFrame frame, double o, Object listElementName);

    public abstract Object executeString(VirtualFrame frame, byte o, Object listElementName);

    public abstract Object executeString(VirtualFrame frame, Object o, Object listElementName);

    public static final int CONSOLE_WIDTH = 80;

    private static final String NA_HEADER = "<NA>";

    @Child PrettyPrinterNode attributePrettyPrinter;
    @Child PrettyPrinterNode recursivePrettyPrinter;
    @Child PrettyPrinterSingleListElementNode singleListElementPrettyPrinter;

    protected abstract boolean isPrintingAttributes();

    private String prettyPrintAttributes(VirtualFrame frame, Object o) {
        if (attributePrettyPrinter == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            attributePrettyPrinter = adoptChild(PrettyPrinterNodeFactory.create(null, null, true));
        }
        return (String) attributePrettyPrinter.executeString(frame, o, null);
    }

    private String prettyPrintRecursive(VirtualFrame frame, Object o, Object listElementName) {
        if (recursivePrettyPrinter == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            recursivePrettyPrinter = adoptChild(PrettyPrinterNodeFactory.create(null, null, isPrintingAttributes()));
        }
        return (String) recursivePrettyPrinter.executeString(frame, o, listElementName);
    }

    private String prettyPrintSingleListElement(VirtualFrame frame, Object o, Object listElementName) {
        if (singleListElementPrettyPrinter == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            singleListElementPrettyPrinter = adoptChild(PrettyPrinterSingleListElementNodeFactory.create(null, null));
        }
        return (String) singleListElementPrettyPrinter.executeString(frame, o, listElementName);
    }

    @Specialization
    public String prettyPrint(RNull operand, Object listElementName) {
        return "NULL";
    }

    @Specialization(order = 1)
    public String prettyPrintVector(byte operand, Object listElementName) {
        return concat("[1] ", prettyPrint(operand, null));
    }

    public String prettyPrint(byte operand, Object listElementName) {
        return RRuntime.logicalToString(operand);
    }

    @Specialization(order = 10)
    public String prettyPrintVector(int operand, Object listElementName) {
        return concat("[1] ", prettyPrint(operand, null));
    }

    public String prettyPrint(int operand, Object listElementName) {
        return RRuntime.intToString(operand, false);
    }

    @Specialization(order = 20)
    public String prettyPrintVector(double operand, Object listElementName) {
        return concat("[1] ", prettyPrint(operand, null));
    }

    public String prettyPrint(double operand, Object listElementName) {
        return doubleToStringPrintFormat(operand, calcRoundFactor(operand, 10000000));
    }

    @Specialization(order = 30)
    public String prettyPrintVector(RComplex operand, Object listElementName) {
        return concat("[1] ", prettyPrint(operand, null));
    }

    public String prettyPrint(RComplex operand, Object listElementName) {
        double factor = calcRoundFactor(operand.getRealPart(), 10000000);
        return operand.toString(doubleToStringPrintFormat(operand.getRealPart(), factor), doubleToStringPrintFormat(operand.getImaginaryPart(), calcRoundFactor(operand.getImaginaryPart(), 10000000)));
    }

    @Specialization(order = 40)
    public String prettyPrintVector(String operand, Object listElementName) {
        return concat("[1] ", prettyPrint(operand, null));
    }

    public String prettyPrint(String operand, Object listElementName) {
        return RRuntime.quoteString(operand);
    }

    @Specialization(order = 50)
    public String prettyPrintVector(RRaw operand, Object listElementName) {
        return concat("[1] ", prettyPrint(operand, null));
    }

    public String prettyPrint(RRaw operand, Object listElementName) {
        return operand.toString();
    }

    @Specialization
    public String prettyPrint(RFunction operand, Object listElementName) {
        return ((RRootNode) ((DefaultCallTarget) operand.getTarget()).getRootNode()).getSourceCode();
    }

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
        return builderToString(builder);
    }

    private String printVector(VirtualFrame frame, RAbstractVector vector, String[] values, boolean isStringVector, boolean isRawVector) {
        assert vector.getLength() == values.length;
        if (values.length == 0) {
            return concat(RRuntime.classToString(vector.getElementClass()), "(0)");
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
                    String positionString = intString(position);
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
                    headerBuilder.append(builderToString(builder));
                    builder = new StringBuilder();
                }
            }
            StringBuilder resultBuilder = printNamesHeader ? headerBuilder : builder;
            resultBuilder.deleteCharAt(resultBuilder.length() - 1);
            Map<String, Object> attributes = vector.getAttributes();
            if (attributes != null) {
                resultBuilder.append(printAttributes(frame, vector, attributes));
            }
            return builderToString(resultBuilder);
        }
    }

    protected static String padColHeader(int r, int dataColWidth, RAbstractVector vector, boolean isList) {
        RList dimNames = vector.getDimNames();
        StringBuilder sb = new StringBuilder();
        int wdiff;
        if (dimNames == null || dimNames.getDataAt(1) == RNull.instance) {
            String rs = intString(r);
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
        return builderToString(sb);
    }

    protected static String rowHeader(int c, RAbstractVector vector) {
        RList dimNames = vector.getDimNames();
        if (dimNames == null || dimNames.getDataAt(0) == RNull.instance) {
            return concat("[", intString(c), ",]");
        } else {
            RStringVector dimNamesVector = (RStringVector) dimNames.getDataAt(0);
            return dimNamesVector.getDataAt(c - 1);
        }
    }

    protected static void spaces(StringBuilder sb, int s) {
        for (int i = 0; i < s; ++i) {
            sb.append(' ');
        }
    }

    private static String getDimId(RAbstractVector vector, int dimLevel, int dimInd) {
        String dimId;
        RList dimNames = vector.getDimNames();
        if (dimNames == null || dimNames.getDataAt(dimLevel - 1) == RNull.instance) {
            dimId = intString(dimInd + 1);
        } else {
            RStringVector dimNamesVector = (RStringVector) dimNames.getDataAt(dimLevel - 1);
            dimId = dimNamesVector.getDataAt(dimInd);
        }
        return dimId;
    }

    private String printDim(RAbstractVector vector, boolean isList, int currentDimLevel, int arrayBase, int accDimensions, String header) {
        int[] dimensions = vector.getDimensions();
        if (currentDimLevel == 3) {
            StringBuilder sb = new StringBuilder();
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
                sb.append(printVector2Dim(vector, dimensions, arrayBase + (dimInd * matrixSize), isList));
                sb.append("\n");
                if ((arrayBase + (dimInd * matrixSize) + matrixSize) < vector.getLength()) {
                    sb.append("\n");
                }
            }
            return builderToString(sb);
        } else {
            StringBuilder sb = new StringBuilder();
            int dimSize = dimensions[currentDimLevel - 1];
            int newAccDimensions = accDimensions / dimSize;
            for (int dimInd = 0; dimInd < dimSize; dimInd++) {
                int newArrayBase = arrayBase + newAccDimensions * dimInd;
                String dimId = getDimId(vector, currentDimLevel, dimInd);
                sb.append(printDim(vector, isList, currentDimLevel - 1, newArrayBase, newAccDimensions, concat(dimId, ", ", header)));
            }
            return builderToString(sb);
        }
    }

    private String printVectorMultiDim(RAbstractVector vector, boolean isList) {
        int[] dimensions = vector.getDimensions();
        assert dimensions != null;
        int numDimensions = dimensions.length;
        assert numDimensions > 1;
        if (numDimensions == 2) {
            return printVector2Dim(vector, dimensions, 0, isList);
        } else if (numDimensions == 3) {
            StringBuilder sb = new StringBuilder();
            int dimSize = dimensions[numDimensions - 1];
            int matrixSize = dimensions[0] * dimensions[1];
            for (int dimInd = 0; dimInd < dimSize; dimInd++) {
                // CheckStyle: stop system..print check
                sb.append(", , ");
                // CheckStyle: resume system..print check
                sb.append(getDimId(vector, numDimensions, dimInd));
                sb.append("\n\n");
                sb.append(printVector2Dim(vector, dimensions, dimInd * matrixSize, isList));
                sb.append("\n");
                if (dimInd < (dimSize - 1)) {
                    sb.append("\n");
                }
            }
            return builderToString(sb);
        } else {
            StringBuilder sb = new StringBuilder();
            int dimSize = dimensions[numDimensions - 1];
            int accDimensions = vector.getLength() / dimSize;
            for (int dimInd = 0; dimInd < dimSize; dimInd++) {
                int arrayBase = accDimensions * dimInd;
                String dimId = getDimId(vector, numDimensions, dimInd);
                sb.append(printDim(vector, isList, numDimensions - 1, arrayBase, accDimensions, dimId));
            }
            return builderToString(sb);
        }
    }

    private String printVector2Dim(RAbstractVector vector, int[] dimensions, int offset, boolean isList) {
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
                String data = printVectorElement(vector.getDataAtAsObject(index + offset));
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
        String rowFormat = concat("%", intString(rowHeaderWidth), "s");

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
                    String cellFormat = concat("%", intString(padColHeader(c, dataColWidths[c - 1], vector, isList).length()), "s");
                    b.append(stringFormat(cellFormat, dataString));
                }
                if (c < ncol) {
                    b.append(' ');
                }
            }
            if (r < nrow) {
                b.append('\n');
            }
        }

        return builderToString(b);
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

    private String prettyPrintList0(VirtualFrame frame, RList operand, Object listElementName) {
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
                if (listElementName != null) {
                    name = concat(objectString(listElementName), objectString(name));
                }
                sb.append(name).append('\n');
                Object value = operand.getDataAt(i);
                sb.append(prettyPrintSingleListElement(frame, value, name)).append("\n\n");
            }
            sb.deleteCharAt(sb.length() - 1);
            Map<String, Object> attributes = operand.getAttributes();
            if (attributes != null) {
                sb.append(printAttributes(frame, operand, attributes));
            }
            return builderToString(sb);
        }
    }

    public String prettyPrintElements(RList operand, Object listElementName) {
        return concat("List,", intString(operand.getLength()));
    }

    public String prettyPrintElements(RAbstractVector operand, Object listElementName) {
        return concat(RRuntime.classToStringCap(operand.getElementClass()), ",", intString(operand.getLength()));
    }

    @Specialization(order = 100, guards = "twoDimsOrMore")
    public String prettyPrintM(RList operand, Object listElementName) {
        return printVectorMultiDim(operand, true);
    }

    @Specialization(order = 101, guards = "twoDimsOrMore")
    public String prettyPrintM(RAbstractVector operand, Object listElementName) {
        return printVectorMultiDim(operand, false);
    }

    @Specialization(order = 200, guards = "!twoDimsOrMore")
    public String prettyPrint(VirtualFrame frame, RList operand, Object listElementName) {
        return prettyPrintList0(frame, operand, listElementName);
    }

    @Specialization(order = 300, guards = "!twoDimsOrMore")
    public String prettyPrint(VirtualFrame frame, RAbstractDoubleVector operand, Object listElementName) {
        int length = operand.getLength();
        String[] values = new String[length];
        for (int i = 0; i < length; i++) {
            double data = operand.getDataAt(i);
            values[i] = prettyPrint(data, listElementName);
        }
        return printVector(frame, operand, values, false, false);
    }

    @Specialization(order = 400, guards = "!twoDimsOrMore")
    public String prettyPrint(VirtualFrame frame, RAbstractIntVector operand, Object listElementName) {
        int length = operand.getLength();
        String[] values = new String[length];
        for (int i = 0; i < length; i++) {
            int data = operand.getDataAt(i);
            values[i] = prettyPrint(data, listElementName);
        }
        return printVector(frame, operand, values, false, false);
    }

    @Specialization(order = 500, guards = "!twoDimsOrMore")
    public String prettyPrint(VirtualFrame frame, RAbstractStringVector operand, Object listElementName) {
        int length = operand.getLength();
        String[] values = new String[length];
        for (int i = 0; i < length; i++) {
            String data = operand.getDataAt(i);
            values[i] = prettyPrint(data, listElementName);
        }
        return printVector(frame, operand, values, true, false);
    }

    @Specialization(order = 600, guards = "!twoDimsOrMore")
    public String prettyPrint(VirtualFrame frame, RAbstractLogicalVector operand, Object listElementName) {
        int length = operand.getLength();
        String[] values = new String[length];
        for (int i = 0; i < length; i++) {
            byte data = operand.getDataAt(i);
            values[i] = prettyPrint(data, listElementName);
        }
        return printVector(frame, operand, values, false, false);
    }

    @Specialization(order = 700, guards = "!twoDimsOrMore")
    public String prettyPrint(VirtualFrame frame, RAbstractRawVector operand, Object listElementName) {
        int length = operand.getLength();
        String[] values = new String[length];
        for (int i = 0; i < length; i++) {
            RRaw data = operand.getDataAt(i);
            values[i] = prettyPrint(data, listElementName);
        }
        return printVector(frame, operand, values, false, true);
    }

    @Specialization(order = 800, guards = "!twoDimsOrMore")
    public String prettyPrint(VirtualFrame frame, RAbstractComplexVector operand, Object listElementName) {
        int length = operand.getLength();
        String[] values = new String[length];
        for (int i = 0; i < length; i++) {
            RComplex data = operand.getDataAt(i);
            values[i] = prettyPrint(data, listElementName);
        }
        return printVector(frame, operand, values, false, false);
    }

    @Specialization
    public String prettyPrint(VirtualFrame frame, RInvisible operand, Object listElementName) {
        return prettyPrintRecursive(frame, operand.get(), listElementName);
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
    private static String builderToString(StringBuilder sb) {
        return sb.toString();
    }

    @SlowPath
    private static String intString(int x) {
        return Integer.toString(x);
    }

    @SlowPath
    private static String stringFormat(String format, String arg) {
        return String.format(format, arg);
    }

    @SlowPath
    private static String objectString(Object o) {
        return o.toString();
    }

    private static String concat(String... ss) {
        StringBuilder sb = new StringBuilder();
        for (String s : ss) {
            sb.append(s);
        }
        return builderToString(sb);
    }

    private String printVectorElement(Object vectorElement) {
        if (vectorElement == RNull.instance) {
            return "NULL";
        }
        if (RTYPES.isByte(vectorElement)) {
            byte vectorElementCast = RTYPES.asByte(vectorElement);
            return prettyPrint(vectorElementCast, null);
        }
        if (RTYPES.isInteger(vectorElement)) {
            int vectorElementCast = RTYPES.asInteger(vectorElement);
            return prettyPrint(vectorElementCast, null);
        }
        if (RTYPES.isDouble(vectorElement)) {
            double vectorElementCast = RTYPES.asDouble(vectorElement);
            return prettyPrint(vectorElementCast, null);
        }
        if (RTYPES.isRComplex(vectorElement)) {
            RComplex vectorElementCast = RTYPES.asRComplex(vectorElement);
            return prettyPrint(vectorElementCast, null);
        }
        if (RTYPES.isString(vectorElement)) {
            String vectorElementCast = RTYPES.asString(vectorElement);
            return prettyPrint(vectorElementCast, null);
        }
        if (RTYPES.isRRaw(vectorElement)) {
            RRaw vectorElementCast = RTYPES.asRRaw(vectorElement);
            return prettyPrint(vectorElementCast, null);
        }
        if (RTYPES.isRIntSequence(vectorElement)) {
            RIntSequence vectorElementCast = RTYPES.asRIntSequence(vectorElement);
            if (vectorElementCast.getLength() == 1) {
                return printVectorElement(vectorElementCast.getDataAt(0));
            } else {
                return prettyPrintElements(vectorElementCast, null);
            }
        }
        if (RTYPES.isRDoubleSequence(vectorElement)) {
            RDoubleSequence vectorElementCast = RTYPES.asRDoubleSequence(vectorElement);
            if (vectorElementCast.getLength() == 1) {
                return printVectorElement(vectorElementCast.getDataAt(0));
            } else {
                return prettyPrintElements(vectorElementCast, null);
            }
        }
        if (RTYPES.isRIntVector(vectorElement)) {
            RIntVector vectorElementCast = RTYPES.asRIntVector(vectorElement);
            if (vectorElementCast.getLength() == 1) {
                return printVectorElement(vectorElementCast.getDataAt(0));
            } else {
                return prettyPrintElements(vectorElementCast, null);
            }
        }
        if (RTYPES.isRDoubleVector(vectorElement)) {
            RDoubleVector vectorElementCast = RTYPES.asRDoubleVector(vectorElement);
            if (vectorElementCast.getLength() == 1) {
                return printVectorElement(vectorElementCast.getDataAt(0));
            } else {
                return prettyPrintElements(vectorElementCast, null);
            }
        }
        if (RTYPES.isRRawVector(vectorElement)) {
            RRawVector vectorElementCast = RTYPES.asRRawVector(vectorElement);
            if (vectorElementCast.getLength() == 1) {
                return printVectorElement(vectorElementCast.getDataAt(0));
            } else {
                return prettyPrintElements(vectorElementCast, null);
            }
        }
        if (RTYPES.isRComplexVector(vectorElement)) {
            RComplexVector vectorElementCast = RTYPES.asRComplexVector(vectorElement);
            if (vectorElementCast.getLength() == 1) {
                return printVectorElement(vectorElementCast.getDataAt(0));
            } else {
                return prettyPrintElements(vectorElementCast, null);
            }
        }
        if (RTYPES.isRStringVector(vectorElement)) {
            RStringVector vectorElementCast = RTYPES.asRStringVector(vectorElement);
            if (vectorElementCast.getLength() == 1) {
                return printVectorElement(vectorElementCast.getDataAt(0));
            } else {
                return prettyPrintElements(vectorElementCast, null);
            }
        }
        if (RTYPES.isRLogicalVector(vectorElement)) {
            RLogicalVector vectorElementCast = RTYPES.asRLogicalVector(vectorElement);
            if (vectorElementCast.getLength() == 1) {
                return printVectorElement(vectorElementCast.getDataAt(0));
            } else {
                return prettyPrintElements(vectorElementCast, null);
            }
        }
        if (RTYPES.isRList(vectorElement)) {
            RList vectorElementCast = RTYPES.asRList(vectorElement);
            if (vectorElementCast.getLength() == 1) {
                return printVectorElement(vectorElementCast.getDataAt(0));
            } else {
                return prettyPrintElements(vectorElementCast, null);
            }
        }
        if (RTYPES.isRInvisible(vectorElement)) {
            RInvisible vectorElementCast = RTYPES.asRInvisible(vectorElement);
            return printVectorElement(vectorElementCast.get());
        }
        if (RTYPES.isRFunction(vectorElement)) {
            RFunction vectorElementCast = RTYPES.asRFunction(vectorElement);
            return prettyPrint(vectorElementCast, null);
        }
        throw new UnsupportedOperationException(concat("Unsupported values ", objectString(vectorElement)));
    }

    @NodeChildren({@NodeChild(value = "operand", type = RNode.class), @NodeChild(value = "listElementName", type = RNode.class)})
    abstract static class PrettyPrinterSingleListElementNode extends RNode {

        @Child PrettyPrinterNode prettyPrinter;

        private void initCast(Object listElementName) {
            if (prettyPrinter == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                prettyPrinter = adoptChild(PrettyPrinterNodeFactory.create(null, null, false));
            }
        }

        private String prettyPrintSingleElement(VirtualFrame frame, byte o, Object listElementName) {
            initCast(listElementName);
            return (String) prettyPrinter.executeString(frame, o, listElementName);
        }

        private String prettyPrintSingleElement(VirtualFrame frame, int o, Object listElementName) {
            initCast(listElementName);
            return (String) prettyPrinter.executeString(frame, o, listElementName);
        }

        private String prettyPrintSingleElement(VirtualFrame frame, double o, Object listElementName) {
            initCast(listElementName);
            return (String) prettyPrinter.executeString(frame, o, listElementName);
        }

        private String prettyPrintSingleElement(VirtualFrame frame, Object o, Object listElementName) {
            initCast(listElementName);
            return (String) prettyPrinter.executeString(frame, o, listElementName);
        }

        public abstract Object executeString(VirtualFrame frame, int o, Object listElementName);

        public abstract Object executeString(VirtualFrame frame, double o, Object listElementName);

        public abstract Object executeString(VirtualFrame frame, byte o, Object listElementName);

        public abstract Object executeString(VirtualFrame frame, Object o, Object listElementName);

        @Specialization
        public String prettyPrintVector(VirtualFrame frame, RNull operand, Object listElementName) {
            return prettyPrintSingleElement(frame, operand, listElementName);
        }

        @Specialization
        public String prettyPrintVector(VirtualFrame frame, byte operand, Object listElementName) {
            return prettyPrintSingleElement(frame, operand, listElementName);
        }

        @Specialization
        public String prettyPrintVector(VirtualFrame frame, int operand, Object listElementName) {
            return prettyPrintSingleElement(frame, operand, listElementName);
        }

        @Specialization
        public String prettyPrintVector(VirtualFrame frame, double operand, Object listElementName) {
            return prettyPrintSingleElement(frame, operand, listElementName);
        }

        @Specialization
        public String prettyPrintVector(VirtualFrame frame, RComplex operand, Object listElementName) {
            return prettyPrintSingleElement(frame, operand, listElementName);
        }

        @Specialization
        public String prettyPrintVector(VirtualFrame frame, String operand, Object listElementName) {
            return prettyPrintSingleElement(frame, operand, listElementName);
        }

        @Specialization
        public String prettyPrintVector(VirtualFrame frame, RRaw operand, Object listElementName) {
            return prettyPrintSingleElement(frame, operand, listElementName);
        }

        @Specialization
        public String prettyPrintVector(VirtualFrame frame, RInvisible operand, Object listElementName) {
            return prettyPrintSingleElement(frame, operand, listElementName);
        }

        @Specialization
        public String prettyPrintVector(VirtualFrame frame, RAbstractVector operand, Object listElementName) {
            return prettyPrintSingleElement(frame, operand, listElementName);
        }
    }

}
