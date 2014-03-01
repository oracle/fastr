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
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.builtin.base.PrettyPrinterNodeFactory.PrettyPrinterSingleListElementNodeFactory;
import com.oracle.truffle.r.nodes.builtin.base.PrettyPrinterNodeFactory.PrettyPrinterSingleVectorElementNodeFactory;
import com.oracle.truffle.r.nodes.builtin.base.PrettyPrinterNodeFactory.PrintDimNodeFactory;
import com.oracle.truffle.r.nodes.builtin.base.PrettyPrinterNodeFactory.PrintVector2DimNodeFactory;
import com.oracle.truffle.r.nodes.builtin.base.PrettyPrinterNodeFactory.PrintVectorMultiDimNodeFactory;

import static com.oracle.truffle.r.nodes.RTypesGen.*;

import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.sun.jmx.snmp.defaults.*;

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
    @Child PrintVectorMultiDimNode multiDimPrinter;

    @Child Re re;
    @Child Im im;

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

    private String printVectorMultiDim(VirtualFrame frame, RAbstractVector vector, boolean isListOrStringVector, boolean isComplexOrRawVector) {
        if (multiDimPrinter == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            multiDimPrinter = adoptChild(PrintVectorMultiDimNodeFactory.create(null, null, null));
        }
        StringBuilder sb = new StringBuilder();
        sb.append((String) multiDimPrinter.executeString(frame, vector, RRuntime.asLogical(isListOrStringVector), RRuntime.asLogical(isComplexOrRawVector)));
        Map<String, Object> attributes = vector.getAttributes();
        if (attributes != null) {
            sb.append(printAttributes(frame, vector, attributes));
        }
        return builderToString(sb);
    }

    @Specialization
    public String prettyPrint(RNull operand, Object listElementName) {
        return "NULL";
    }

    @Specialization(order = 1)
    public String prettyPrintVector(byte operand, Object listElementName) {
        return concat("[1] ", prettyPrint(operand));
    }

    public static String prettyPrint(byte operand) {
        return RRuntime.logicalToString(operand);
    }

    @Specialization(order = 10)
    public String prettyPrintVector(int operand, Object listElementName) {
        return concat("[1] ", prettyPrint(operand));
    }

    public static String prettyPrint(int operand) {
        return RRuntime.intToString(operand, false);
    }

    @Specialization(order = 20)
    public String prettyPrintVector(double operand, Object listElementName) {
        return concat("[1] ", prettyPrint(operand));
    }

    public static String prettyPrint(double operand) {
        return doubleToStringPrintFormat(operand, calcRoundFactor(operand, 10000000));
    }

    @Specialization(order = 30)
    public String prettyPrintVector(RComplex operand, Object listElementName) {
        return concat("[1] ", prettyPrint(operand));
    }

    public static String prettyPrint(RComplex operand) {
        double rfactor = calcRoundFactor(operand.getRealPart(), 10000000);
        double ifactor = calcRoundFactor(operand.getImaginaryPart(), 10000000);
        return operand.toString(doubleToStringPrintFormat(operand.getRealPart(), rfactor), doubleToStringPrintFormat(operand.getImaginaryPart(), ifactor));
    }

    @Specialization(order = 40)
    public String prettyPrintVector(String operand, Object listElementName) {
        return concat("[1] ", prettyPrint(operand));
    }

    public static String prettyPrint(String operand) {
        return RRuntime.quoteString(operand);
    }

    @Specialization(order = 50)
    public String prettyPrintVector(RRaw operand, Object listElementName) {
        return concat("[1] ", prettyPrint(operand));
    }

    public static String prettyPrint(RRaw operand) {
        return operand.toString();
    }

    @Specialization
    public String prettyPrint(RFunction operand, Object listElementName) {
        return ((RRootNode) ((DefaultCallTarget) operand.getTarget()).getRootNode()).getSourceCode();
    }

    @Specialization
    public String prettyPrint(REnvironment operand, Object listElementName) {
        return operand.toString();
    }

    private String printAttributes(VirtualFrame frame, RAbstractVector vector, Map<String, Object> attributes) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, Object> attr : attributes.entrySet()) {
            if (attr.getKey().equals(RRuntime.NAMES_ATTR_KEY) && !vector.hasDimensions()) {
                // names attribute already printed
                continue;
            }
            if (attr.getKey().equals(RRuntime.DIM_ATTR_KEY) || attr.getKey().equals(RRuntime.DIMNAMES_ATTR_KEY)) {
                // dim and dimnames attributes never gets printed
                continue;
            }
            builder.append("\n");
            builder.append(concat("attr(,\"", attr.getKey(), "\")\n"));
            if (attr.getKey().equals(RRuntime.CLASS_ATTR_KEY)) {
                builder.append(prettyPrintAttributes(frame, RDataFactory.createStringVector((String[]) attr.getValue(), true)));
                continue;
            }
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
                maxPositionLength = intString(vector.getLength()).length();
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

    @SlowPath
    protected static String padColHeader(int r, int dataColWidth, RAbstractVector vector, boolean isListOrStringVector) {
        RList dimNames = vector.getDimNames();
        StringBuilder sb = new StringBuilder();
        int wdiff;
        if (dimNames == null || dimNames.getDataAt(1) == RNull.instance) {
            String rs = intString(r);
            wdiff = dataColWidth - (rs.length() + 3); // 3: [,]
            if (!isListOrStringVector && wdiff > 0) {
                spaces(sb, wdiff);
            }
            sb.append("[,").append(rs).append(']');
        } else {
            RStringVector dimNamesVector = (RStringVector) dimNames.getDataAt(1);
            String dimId = dimNamesVector.getDataAt(r - 1);
            if (dimId == RRuntime.STRING_NA) {
                dimId = NA_HEADER;
            }
            wdiff = dataColWidth - dimId.length();
            if (!isListOrStringVector && wdiff > 0) {
                spaces(sb, wdiff);
            }
            sb.append(dimId);
        }
        if (isListOrStringVector && wdiff > 0) {
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
            String dimId = dimNamesVector.getDataAt(c - 1);
            if (dimId == RRuntime.STRING_NA) {
                dimId = NA_HEADER;
            }
            return dimId;
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
                    name = concat(RRuntime.toString(listElementName), RRuntime.toString(name));
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

    public static String prettyPrint(RList operand) {
        return concat("List,", intString(operand.getLength()));
    }

    public static String prettyPrint(RAbstractVector operand) {
        return concat(RRuntime.classToStringCap(operand.getElementClass()), ",", intString(operand.getLength()));
    }

    @Specialization(order = 100, guards = "twoDimsOrMore")
    public String prettyPrintM(VirtualFrame frame, RList operand, Object listElementName) {
        return printVectorMultiDim(frame, operand, true, false);
    }

    @Specialization(order = 101, guards = "twoDimsOrMore")
    public String prettyPrintM(VirtualFrame frame, RAbstractStringVector operand, Object listElementName) {
        return printVectorMultiDim(frame, operand, true, false);
    }

    @Specialization(order = 103, guards = "twoDimsOrMore")
    public String prettyPrintM(VirtualFrame frame, RAbstractComplexVector operand, Object listElementName) {
        return printVectorMultiDim(frame, operand, false, true);
    }

    @Specialization(order = 104, guards = "twoDimsOrMore")
    public String prettyPrintM(VirtualFrame frame, RAbstractRawVector operand, Object listElementName) {
        return printVectorMultiDim(frame, operand, false, true);
    }

    @Specialization(order = 105, guards = "twoDimsOrMore")
    public String prettyPrintM(VirtualFrame frame, RAbstractVector operand, Object listElementName) {
        return printVectorMultiDim(frame, operand, false, false);
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
            values[i] = prettyPrint(data);
        }
        padTrailingDecimalPointAndZeroesIfRequired(values);
        return printVector(frame, operand, values, false, false);
    }

    @Specialization(order = 400, guards = "!twoDimsOrMore")
    public String prettyPrint(VirtualFrame frame, RAbstractIntVector operand, Object listElementName) {
        int length = operand.getLength();
        String[] values = new String[length];
        for (int i = 0; i < length; i++) {
            int data = operand.getDataAt(i);
            values[i] = prettyPrint(data);
        }
        return printVector(frame, operand, values, false, false);
    }

    @Specialization(order = 500, guards = "!twoDimsOrMore")
    public String prettyPrint(VirtualFrame frame, RAbstractStringVector operand, Object listElementName) {
        int length = operand.getLength();
        String[] values = new String[length];
        for (int i = 0; i < length; i++) {
            String data = operand.getDataAt(i);
            values[i] = prettyPrint(data);
        }
        return printVector(frame, operand, values, true, false);
    }

    @Specialization(order = 600, guards = "!twoDimsOrMore")
    public String prettyPrint(VirtualFrame frame, RAbstractLogicalVector operand, Object listElementName) {
        int length = operand.getLength();
        String[] values = new String[length];
        for (int i = 0; i < length; i++) {
            byte data = operand.getDataAt(i);
            values[i] = prettyPrint(data);
        }
        return printVector(frame, operand, values, false, false);
    }

    @Specialization(order = 700, guards = "!twoDimsOrMore")
    public String prettyPrint(VirtualFrame frame, RAbstractRawVector operand, Object listElementName) {
        int length = operand.getLength();
        String[] values = new String[length];
        for (int i = 0; i < length; i++) {
            RRaw data = operand.getDataAt(i);
            values[i] = prettyPrint(data);
        }
        return printVector(frame, operand, values, false, true);
    }

    @Specialization(order = 800, guards = "!twoDimsOrMore")
    public String prettyPrint(VirtualFrame frame, RAbstractComplexVector operand, Object listElementName) {
        if (re == null) {
            // the two are allocated side by side; checking for re is sufficient
            CompilerDirectives.transferToInterpreterAndInvalidate();
            RBuiltinPackages packages = (RBuiltinPackages) RContext.getInstance().getLookup();
            re = adoptChild(ReFactory.create(new RNode[1], packages.lookupBuiltin("Re")));
            im = adoptChild(ImFactory.create(new RNode[1], packages.lookupBuiltin("Im")));
        }

        RDoubleVector realParts = (RDoubleVector) re.executeRDoubleVector(frame, operand);
        RDoubleVector imaginaryParts = (RDoubleVector) im.executeRDoubleVector(frame, operand);

        int length = operand.getLength();
        String[] realValues = new String[length];
        String[] imaginaryValues = new String[length];
        for (int i = 0; i < length; ++i) {
            realValues[i] = prettyPrint(realParts.getDataAt(i));
            imaginaryValues[i] = prettyPrint(imaginaryParts.getDataAt(i));
        }
        padTrailingDecimalPointAndZeroesIfRequired(realValues);
        padTrailingDecimalPointAndZeroesIfRequired(imaginaryValues);
        removeLeadingMinus(imaginaryValues);
        rightJustify(imaginaryValues);

        String[] values = new String[length];
        for (int i = 0; i < length; i++) {
            values[i] = operand.getDataAt(i).isNA() ? "NA" : concat(realValues[i], imaginaryParts.getDataAt(i) < 0.0 ? "-" : "+", imaginaryValues[i], "i");
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

    @SlowPath
    private static String builderToString(StringBuilder sb) {
        return sb.toString();
    }

    @SlowPath
    private static String builderToSubstring(StringBuilder sb, int start, int end) {
        return sb.substring(start, end);
    }

    @SlowPath
    private static String intString(int x) {
        return Integer.toString(x);
    }

    @SlowPath
    private static String stringFormat(String format, Object arg) {
        return String.format(format, arg);
    }

    private static String concat(String... ss) {
        StringBuilder sb = new StringBuilder();
        for (String s : ss) {
            sb.append(s);
        }
        return builderToString(sb);
    }

    @SlowPath
    private static String substring(String s, int start) {
        return s.substring(start);
    }

    private static int requiresDecimalPointsAndTrailingZeroes(String[] values, int[] decimalPointOffsets, int[] lenAfterPoint) {
        boolean foundWithDecimalPoint = false;
        boolean foundWithoutDecimalPoint = false;
        boolean inequalLenAfterPoint = false;
        int maxLenAfterPoint = -1;
        for (int i = 0; i < values.length; ++i) {
            String v = values[i];
            decimalPointOffsets[i] = v.indexOf('.');
            if (decimalPointOffsets[i] == -1) {
                foundWithoutDecimalPoint = true;
                lenAfterPoint[i] = 0;
            } else {
                foundWithDecimalPoint = true;
                int lap = substring(v, decimalPointOffsets[i] + 1).length();
                lenAfterPoint[i] = lap;
                if (lap > maxLenAfterPoint) {
                    if (maxLenAfterPoint == -1) {
                        inequalLenAfterPoint = true;
                    }
                    maxLenAfterPoint = lap;
                }
            }
        }
        return (foundWithDecimalPoint && foundWithoutDecimalPoint) || inequalLenAfterPoint ? maxLenAfterPoint : -1;
    }

    private static void padTrailingDecimalPointAndZeroesIfRequired(String[] values) {
        int[] decimalPointOffsets = new int[values.length];
        int[] lenAfterPoint = new int[values.length];
        int maxLenAfterPoint = requiresDecimalPointsAndTrailingZeroes(values, decimalPointOffsets, lenAfterPoint);
        if (maxLenAfterPoint == -1) {
            return;
        }

        for (int i = 0; i < values.length; ++i) {
            String v = values[i];
            if (v == RRuntime.STRING_NA) {
                continue;
            }
            if (decimalPointOffsets[i] == -1) {
                v = concat(v, ".");
            }
            if (lenAfterPoint[i] < maxLenAfterPoint) {
                values[i] = concat(v, stringFormat(concat("%0", intString(maxLenAfterPoint - lenAfterPoint[i]), "d"), 0));
            }
        }
    }

    private static void rightJustify(String[] values) {
        int maxLen = 0;
        boolean inequalLengths = false;
        int lastLen = 0;
        for (int i = 0; i < values.length; ++i) {
            String v = values[i];
            if (v == RRuntime.STRING_NA) {
                // do not use NA for deciding alignment
                continue;
            }
            int l = v.length();
            maxLen = Math.max(maxLen, l);
            inequalLengths = lastLen != 0 && lastLen != l;
            lastLen = l;
        }
        if (!inequalLengths) {
            return;
        }
        for (int i = 0; i < values.length; ++i) {
            String v = values[i];
            int l = v.length();
            if (l < maxLen) {
                int d = maxLen - l;
                if (d == 1) {
                    values[i] = concat(" ", v);
                } else {
                    values[i] = concat(stringFormat(concat("%", intString(d), "s"), " "), v);
                }
            }
        }
    }

    private static void removeLeadingMinus(String[] values) {
        for (int i = 0; i < values.length; ++i) {
            String v = values[i];
            if (v.charAt(0) == '-') {
                values[i] = substring(v, 1);
            }
        }
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
        public String prettyPrintListElement(VirtualFrame frame, RNull operand, Object listElementName) {
            return prettyPrintSingleElement(frame, operand, listElementName);
        }

        @Specialization
        public String prettyPrintListElement(VirtualFrame frame, byte operand, Object listElementName) {
            return prettyPrintSingleElement(frame, operand, listElementName);
        }

        @Specialization
        public String prettyPrintListElement(VirtualFrame frame, int operand, Object listElementName) {
            return prettyPrintSingleElement(frame, operand, listElementName);
        }

        @Specialization
        public String prettyPrintListElement(VirtualFrame frame, double operand, Object listElementName) {
            return prettyPrintSingleElement(frame, operand, listElementName);
        }

        @Specialization
        public String prettyPrintListElement(VirtualFrame frame, RComplex operand, Object listElementName) {
            return prettyPrintSingleElement(frame, operand, listElementName);
        }

        @Specialization
        public String prettyPrintListElement(VirtualFrame frame, String operand, Object listElementName) {
            return prettyPrintSingleElement(frame, operand, listElementName);
        }

        @Specialization
        public String prettyPrintListElement(VirtualFrame frame, RRaw operand, Object listElementName) {
            return prettyPrintSingleElement(frame, operand, listElementName);
        }

        @Specialization
        public String prettyPrintListElement(VirtualFrame frame, RInvisible operand, Object listElementName) {
            return prettyPrintSingleElement(frame, operand, listElementName);
        }

        @Specialization
        public String prettyPrintListElement(VirtualFrame frame, RAbstractVector operand, Object listElementName) {
            return prettyPrintSingleElement(frame, operand, listElementName);
        }
    }

    @NodeChild(value = "operand", type = RNode.class)
    abstract static class PrettyPrinterSingleVectorElementNode extends RNode {

        @Child PrettyPrinterSingleVectorElementNode recursivePrettyPrinter;

        private String prettyPrintRecursive(Object o) {
            if (recursivePrettyPrinter == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                recursivePrettyPrinter = adoptChild(PrettyPrinterSingleVectorElementNodeFactory.create(null));
            }
            return (String) recursivePrettyPrinter.executeString(null, o);
        }

        public abstract Object executeString(VirtualFrame frame, Object o);

        @Specialization
        public String prettyPrintVectorElement(RNull operand) {
            return "NULL";
        }

        @Specialization
        public String prettyPrintVectorElement(byte operand) {
            return prettyPrint(operand);
        }

        @Specialization
        public String prettyPrintVectorElement(int operand) {
            return prettyPrint(operand);
        }

        @Specialization
        public String prettyPrintVectorElement(double operand) {
            return prettyPrint(operand);
        }

        @Specialization
        public String prettyPrintVectorElement(RComplex operand) {
            return prettyPrint(operand);
        }

        @Specialization
        public String prettyPrintVectorElement(String operand) {
            return prettyPrint(operand);
        }

        @Specialization
        public String prettyPrintVectorElement(RRaw operand) {
            return prettyPrint(operand);
        }

        @Specialization
        public String prettyPrintVectorElement(RInvisible operand) {
            return prettyPrintRecursive(operand.get());
        }

        @Specialization(order = 1, guards = "!isLengthOne")
        public String prettyPrintVectorElement(RList operand) {
            return prettyPrint(operand);
        }

        @Specialization(order = 2, guards = "!isLengthOne")
        public String prettyPrintVectorElement(RAbstractVector operand) {
            return prettyPrint(operand);
        }

        @Specialization(order = 3, guards = "isLengthOne")
        public String prettyPrintVectorElementLengthOne(RAbstractVector operand) {
            return prettyPrintRecursive(operand.getDataAtAsObject(0));
        }

        protected static boolean isLengthOne(RList v) {
            return v.getLength() == 1;
        }

        protected static boolean isLengthOne(RAbstractVector v) {
            return v.getLength() == 1;
        }

    }

    @NodeChildren({@NodeChild(value = "vector", type = RNode.class), @NodeChild(value = "isListOrStringVector", type = RNode.class), @NodeChild(value = "isComplexOrRawVector", type = RNode.class)})
    abstract static class PrintVectorMultiDimNode extends RNode {

        @Child PrintVector2DimNode vector2DimPrinter;
        @Child PrintDimNode dimPrinter;

        private String printVector2Dim(VirtualFrame frame, RAbstractVector vector, RIntVector dimensions, int offset, byte isListOrStringVector, byte isComplexOrRawVector) {
            if (vector2DimPrinter == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                vector2DimPrinter = adoptChild(PrintVector2DimNodeFactory.create(null, null, null, null, null));
            }
            return (String) vector2DimPrinter.executeString(frame, vector, dimensions, offset, isListOrStringVector, isComplexOrRawVector);
        }

        private String printDim(VirtualFrame frame, RAbstractVector vector, byte isListOrStringVector, byte isComplexOrRawVector, int currentDimLevel, int arrayBase, int accDimensions, String header) {
            if (dimPrinter == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                dimPrinter = adoptChild(PrintDimNodeFactory.create(null, null, null, null, null, null, null));
            }
            return (String) dimPrinter.executeString(frame, vector, isListOrStringVector, isComplexOrRawVector, currentDimLevel, arrayBase, accDimensions, header);
        }

        public abstract Object executeString(VirtualFrame frame, RAbstractVector vector, byte isListOrStringVector, byte isComplexOrRawVector);

        @Specialization
        public String printVectorMultiDim(VirtualFrame frame, RAbstractVector vector, byte isListOrStringVector, byte isComplexOrRawVector) {
            int[] dimensions = vector.getDimensions();
            RIntVector dimensionsVector = RDataFactory.createIntVector(dimensions, RDataFactory.COMPLETE_VECTOR);
            assert dimensions != null;
            int numDimensions = dimensions.length;
            assert numDimensions > 1;
            if (numDimensions == 2) {
                return printVector2Dim(frame, vector, dimensionsVector, 0, isListOrStringVector, isComplexOrRawVector);
            } else {
                int dimSize = dimensions[numDimensions - 1];
                if (dimSize == 0) {
                    return "";
                }
                StringBuilder sb = new StringBuilder();
                if (numDimensions == 3) {
                    int matrixSize = dimensions[0] * dimensions[1];
                    for (int dimInd = 0; dimInd < dimSize; dimInd++) {
                        // CheckStyle: stop system..print check
                        sb.append(", , ");
                        // CheckStyle: resume system..print check
                        sb.append(getDimId(vector, numDimensions, dimInd));
                        sb.append("\n\n");
                        sb.append(printVector2Dim(frame, vector, dimensionsVector, dimInd * matrixSize, isListOrStringVector, isComplexOrRawVector));
                        sb.append("\n");
                        if (dimInd < (dimSize - 1) && vector.getLength() > 0 || vector.getLength() == 0) {
                            sb.append("\n");
                        }
                    }
                } else {
                    int accDimensions = vector.getLength() / dimSize;
                    for (int dimInd = 0; dimInd < dimSize; dimInd++) {
                        int arrayBase = accDimensions * dimInd;
                        String dimId = getDimId(vector, numDimensions, dimInd);
                        String innerDims = printDim(frame, vector, isListOrStringVector, isComplexOrRawVector, numDimensions - 1, arrayBase, accDimensions, dimId);
                        if (innerDims == null) {
                            return "";
                        } else {
                            sb.append(innerDims);
                        }
                    }
                }
                if (vector.getLength() == 0) {
                    // remove last line break
                    return builderToSubstring(sb, 0, sb.length() - 1);
                } else {
                    return builderToString(sb);
                }
            }
        }
    }

    @NodeChildren({@NodeChild(value = "vector", type = RNode.class), @NodeChild(value = "dimensions", type = RNode.class), @NodeChild(value = "offset", type = RNode.class),
                    @NodeChild(value = "isListOrStringVector", type = RNode.class), @NodeChild(value = "isComplexOrRawVector", type = RNode.class)})
    abstract static class PrintVector2DimNode extends RNode {

        @Child PrettyPrinterSingleVectorElementNode singleVectorElementPrettyPrinter;

        private String prettyPrintSingleVectorElement(VirtualFrame frame, Object o) {
            if (singleVectorElementPrettyPrinter == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                singleVectorElementPrettyPrinter = adoptChild(PrettyPrinterSingleVectorElementNodeFactory.create(null));
            }
            return (String) singleVectorElementPrettyPrinter.executeString(frame, o);
        }

        public abstract Object executeString(VirtualFrame frame, RAbstractVector vector, RIntVector dimensions, int offset, byte isListOrStringVector, byte isComplexOrRawVector);

        private static String getDimId(RList dimNames, int dimension, int ind, byte isComplexOrRawVector) {
            StringBuilder sb = new StringBuilder();
            if (dimNames == null || dimNames.getDataAt(dimension) == RNull.instance) {
                String rs = intString(ind);
                sb.append("[");
                if (dimension == 1) {
                    // columns
                    sb.append(',');
                }
                sb.append(rs);
                if (dimension == 0) {
                    // rows
                    sb.append(',');
                }
                sb.append(']');
            } else {
                RStringVector dimNamesVector = (RStringVector) dimNames.getDataAt(dimension);
                String dimId = dimNamesVector.getDataAt(ind - 1);
                if (dimension == 1 && isComplexOrRawVector == RRuntime.LOGICAL_TRUE && dimId.length() == 1) {
                    sb.append(' ');
                }
                sb.append(dimId);
            }
            return builderToString(sb);
        }

        @Specialization(order = 1, guards = "isEmpty")
        public String printVector2DimEmpty(VirtualFrame frame, RAbstractVector vector, RIntVector dimensions, int offset, byte isListOrStringVector, byte isComplexOrRawVector) {
            int nrow = dimensions.getDataAt(0);
            int ncol = dimensions.getDataAt(1);

            if (nrow == 0 && ncol == 0) {
                if (dimensions.getLength() == 2) {
                    return "<0 x 0 matrix>";
                } else {
                    return "";
                }
            }

            StringBuilder sb = new StringBuilder();
            RList dimNames = vector.getDimNames();
            if (ncol > 0) {
                sb.append("     ");
                for (int c = 1; c <= ncol; ++c) {
                    sb.append(getDimId(dimNames, 1, c, isComplexOrRawVector));
                    if (c < ncol) {
                        sb.append(' ');
                    }
                }
            }
            if (nrow > 0) {
                sb.append('\n');
                for (int r = 1; r <= nrow; ++r) {
                    sb.append(getDimId(dimNames, 0, r, isComplexOrRawVector));
                    if (r < nrow) {
                        sb.append('\n');
                    }
                }
            }
            return builderToString(sb);
        }

        @Specialization(order = 2, guards = "!isEmpty")
        public String printVector2Dim(VirtualFrame frame, RAbstractVector vector, RIntVector dimensions, int offset, byte isListOrStringVector, byte isComplexOrRawVector) {
            int nrow = dimensions.getDataAt(0);
            int ncol = dimensions.getDataAt(1);

            // prepare data (relevant for column widths)
            String[] dataStrings = new String[nrow * ncol];
            int[] dataColWidths = new int[ncol];
            RList dimNames = vector.getDimNames();
            RStringVector columnDimNames = null;
            if (dimNames != null && dimNames.getDataAt(1) != RNull.instance) {
                columnDimNames = (RStringVector) dimNames.getDataAt(1);
            }
            int rowHeaderWidth = 0;
            for (int r = 0; r < nrow; ++r) {
                for (int c = 0; c < ncol; ++c) {
                    int index = c * nrow + r;
                    String data = prettyPrintSingleVectorElement(frame, vector.getDataAtAsObject(index + offset));
                    dataStrings[index] = data;
                    if (data.length() > dataColWidths[c]) {
                        dataColWidths[c] = data.length();
                    }
                    if (columnDimNames != null) {
                        String columnName = columnDimNames.getDataAt(c);
                        if (columnName == RRuntime.STRING_NA) {
                            columnName = NA_HEADER;
                        }
                        if (columnName.length() > dataColWidths[c]) {
                            dataColWidths[c] = columnName.length();
                        }
                    }
                }
                rowHeaderWidth = Math.max(rowHeaderWidth, rowHeader(r + 1, vector).length());
            }

            String rowFormat = concat("%", intString(rowHeaderWidth), "s");

            StringBuilder b = new StringBuilder();

            // column header
            spaces(b, rowHeaderWidth + 1);
            for (int c = 1; c <= ncol; ++c) {
                b.append(padColHeader(c, dataColWidths[c - 1], vector, isListOrStringVector == RRuntime.LOGICAL_TRUE));
                if (c < ncol) {
                    b.append(' ');
                }
            }
            b.append('\n');

            // rows
            for (int r = 1; r <= nrow; ++r) {
                String headerString = rowHeader(r, vector);
                b.append(headerString);
                spaces(b, rowHeaderWidth - headerString.length() + 1);
                for (int c = 1; c <= ncol; ++c) {
                    String dataString = dataStrings[(c - 1) * nrow + (r - 1)];
                    if (isListOrStringVector == RRuntime.LOGICAL_TRUE) {
                        // list elements are aligned to the left and vector's to the right
                        b.append(dataString);
                        spaces(b, padColHeader(c, dataColWidths[c - 1], vector, isListOrStringVector == RRuntime.LOGICAL_TRUE).length() - dataString.length());
                    } else {
                        String cellFormat = concat("%", intString(padColHeader(c, dataColWidths[c - 1], vector, isListOrStringVector == RRuntime.LOGICAL_TRUE).length()), "s");
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

        public boolean isEmpty(RAbstractVector vector, RIntVector dimensions, int offset, byte isListOrStringVector, byte isComplexOrRawVector) {
            return vector.getLength() == 0;
        }

    }

    @NodeChildren({@NodeChild(value = "vector", type = RNode.class), @NodeChild(value = "isListOrStringVector", type = RNode.class), @NodeChild(value = "isComplexOrRawVector", type = RNode.class),
                    @NodeChild(value = "currentDimLevel", type = RNode.class), @NodeChild(value = "arrayBase", type = RNode.class), @NodeChild(value = "accDimensions", type = RNode.class),
                    @NodeChild(value = "header", type = RNode.class)})
    abstract static class PrintDimNode extends RNode {

        public abstract Object executeString(VirtualFrame frame, RAbstractVector vector, byte isListOrStringVector, byte isComplexOrRawVector, int currentDimLevel, int arrayBase, int accDimensions,
                        String header);

        @Child PrintVector2DimNode vector2DimPrinter;
        @Child PrintDimNode dimPrinter;

        private String printVector2Dim(VirtualFrame frame, RAbstractVector vector, RIntVector dimensions, int offset, byte isListOrStringVector, byte isComplexOrRawVector) {
            if (vector2DimPrinter == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                vector2DimPrinter = adoptChild(PrintVector2DimNodeFactory.create(null, null, null, null, null));
            }
            return (String) vector2DimPrinter.executeString(frame, vector, dimensions, offset, isListOrStringVector, isComplexOrRawVector);
        }

        private String printDimRecursive(VirtualFrame frame, RAbstractVector vector, byte isListOrStringVector, byte isComplexOrRawVector, int currentDimLevel, int arrayBase, int accDimensions,
                        String header) {
            if (dimPrinter == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                dimPrinter = adoptChild(PrintDimNodeFactory.create(null, null, null, null, null, null, null));
            }
            return (String) dimPrinter.executeString(frame, vector, isListOrStringVector, isComplexOrRawVector, currentDimLevel, arrayBase, accDimensions, header);
        }

        @Specialization
        public String printDim(VirtualFrame frame, RAbstractVector vector, byte isListOrStringVector, byte isComplexOrRawVector, int currentDimLevel, int arrayBase, int accDimensions, String header) {
            int[] dimensions = vector.getDimensions();
            RIntVector dimensionsVector = RDataFactory.createIntVector(dimensions, RDataFactory.COMPLETE_VECTOR);
            StringBuilder sb = new StringBuilder();
            int dimSize = dimensions[currentDimLevel - 1];
            if (dimSize == 0) {
                return null;
            }
            if (currentDimLevel == 3) {
                int matrixSize = dimensions[0] * dimensions[1];
                for (int dimInd = 0; dimInd < dimSize; dimInd++) {
                    // CheckStyle: stop system..print check
                    sb.append(", , ");
                    // CheckStyle: resume system..print check
                    sb.append(getDimId(vector, currentDimLevel, dimInd));
                    sb.append(", ");
                    sb.append(header);
                    sb.append("\n\n");
                    sb.append(printVector2Dim(frame, vector, dimensionsVector, arrayBase + (dimInd * matrixSize), isListOrStringVector, isComplexOrRawVector));
                    sb.append("\n");
                    if ((arrayBase + (dimInd * matrixSize) + matrixSize) < vector.getLength() || vector.getLength() == 0) {
                        sb.append("\n");
                    }
                }
            } else {
                int newAccDimensions = accDimensions / dimSize;
                for (int dimInd = 0; dimInd < dimSize; dimInd++) {
                    int newArrayBase = arrayBase + newAccDimensions * dimInd;
                    String dimId = getDimId(vector, currentDimLevel, dimInd);
                    String innerDims = printDimRecursive(frame, vector, isListOrStringVector, isComplexOrRawVector, currentDimLevel - 1, newArrayBase, newAccDimensions, concat(dimId, ", ", header));
                    if (innerDims == null) {
                        return null;
                    } else {
                        sb.append(innerDims);
                    }
                }
                return builderToString(sb);
            }
            return builderToString(sb);
        }
    }

}
