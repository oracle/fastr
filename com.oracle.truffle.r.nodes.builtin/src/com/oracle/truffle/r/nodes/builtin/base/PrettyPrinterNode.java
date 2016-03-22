/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameInstance.FrameAccess;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.nodes.RRootNode;
import com.oracle.truffle.r.nodes.access.ConstantNode;
import com.oracle.truffle.r.nodes.builtin.base.PrettyPrinterNodeGen.PrettyPrinterSingleListElementNodeGen;
import com.oracle.truffle.r.nodes.builtin.base.PrettyPrinterNodeGen.PrettyPrinterSingleVectorElementNodeGen;
import com.oracle.truffle.r.nodes.builtin.base.PrettyPrinterNodeGen.PrintDimNodeGen;
import com.oracle.truffle.r.nodes.builtin.base.PrettyPrinterNodeGen.PrintVector2DimNodeGen;
import com.oracle.truffle.r.nodes.builtin.base.PrettyPrinterNodeGen.PrintVectorMultiDimNodeGen;
import com.oracle.truffle.r.nodes.function.FormalArguments;
import com.oracle.truffle.r.nodes.unary.CastStringNode;
import com.oracle.truffle.r.nodes.unary.CastStringNodeGen;
import com.oracle.truffle.r.nodes.unary.UnaryArithmeticNode;
import com.oracle.truffle.r.nodes.unary.UnaryArithmeticNodeGen;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RDeparse;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.conn.SocketConnections;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RAttributeProfiles;
import com.oracle.truffle.r.runtime.data.RAttributes;
import com.oracle.truffle.r.runtime.data.RAttributes.RAttribute;
import com.oracle.truffle.r.runtime.data.RBuiltinDescriptor;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDouble;
import com.oracle.truffle.r.runtime.data.RExpression;
import com.oracle.truffle.r.runtime.data.RExternalPtr;
import com.oracle.truffle.r.runtime.data.RFactor;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RLanguage;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.data.RRaw;
import com.oracle.truffle.r.runtime.data.RS4Object;
import com.oracle.truffle.r.runtime.data.RString;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.data.RVector;
import com.oracle.truffle.r.runtime.data.closures.RClosures;
import com.oracle.truffle.r.runtime.data.closures.RFactorToStringVectorClosure;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractRawVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

@SuppressWarnings("unused")
@NodeChildren({@NodeChild(value = "operand", type = RNode.class), @NodeChild(value = "listElementName", type = RNode.class), @NodeChild(value = "quote", type = RNode.class),
                @NodeChild(value = "right", type = RNode.class)})
@NodeField(name = "printingAttributes", type = boolean.class)
public abstract class PrettyPrinterNode extends RNode {

    @Override
    public abstract Object execute(VirtualFrame frame);

    public abstract Object executeString(int o, Object listElementName, byte quote, byte right);

    public abstract Object executeString(double o, Object listElementName, byte quote, byte right);

    public abstract Object executeString(byte o, Object listElementName, byte quote, byte right);

    public abstract Object executeString(Object o, Object listElementName, byte quote, byte right);

    @Child private PrettyPrinterNode attributePrettyPrinter;
    @Child private PrettyPrinterNode recursivePrettyPrinter;
    @Child private PrettyPrinterSingleListElementNode singleListElementPrettyPrinter;
    @Child private PrettyPrinterSingleVectorElementNode singleVectorElementPrettyPrinter;
    @Child private PrintVectorMultiDimNode multiDimPrinter;

    @Child private UnaryArithmeticNode re;
    @Child private UnaryArithmeticNode im;

    @Child private IndirectCallNode indirectCall = Truffle.getRuntime().createIndirectCallNode();

    private final RAttributeProfiles attrProfiles = RAttributeProfiles.create();

    protected abstract boolean isPrintingAttributes();

    private static final FrameAccess FRAME_ACCESS = FrameAccess.NONE;

    private String prettyPrintAttribute(Object o) {
        if (attributePrettyPrinter == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            attributePrettyPrinter = insert(PrettyPrinterNodeGen.create(null, null, null, null, true));
        }
        return (String) attributePrettyPrinter.executeString(o, null, RRuntime.LOGICAL_TRUE, RRuntime.LOGICAL_FALSE);
    }

    private String prettyPrintRecursive(Object o, Object listElementName, byte quote, byte right) {
        if (recursivePrettyPrinter == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            recursivePrettyPrinter = insert(PrettyPrinterNodeGen.create(null, null, null, null, isPrintingAttributes()));
        }
        return (String) recursivePrettyPrinter.executeString(o, listElementName, quote, right);
    }

    private String prettyPrintSingleListElement(Object o, Object listElementName, byte quote, byte right) {
        if (singleListElementPrettyPrinter == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            singleListElementPrettyPrinter = insert(PrettyPrinterSingleListElementNodeGen.create(null, null, null, null));
        }
        return (String) singleListElementPrettyPrinter.executeString(o, listElementName, quote, right);
    }

    private String prettyPrintSingleVectorElement(Object o, byte isQuoted) {
        if (singleVectorElementPrettyPrinter == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            singleVectorElementPrettyPrinter = insert(PrettyPrinterSingleVectorElementNodeGen.create(null, null));
        }
        return (String) singleVectorElementPrettyPrinter.executeString(o, isQuoted);
    }

    private String printVectorMultiDim(RAbstractVector vector, boolean isListOrStringVector, boolean isComplexOrRawVector, byte quote) {
        if (multiDimPrinter == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            multiDimPrinter = insert(PrintVectorMultiDimNodeGen.create(null, null, null, null));
        }
        StringBuilder sb = new StringBuilder();
        sb.append((String) multiDimPrinter.executeString(vector, RRuntime.asLogical(isListOrStringVector), RRuntime.asLogical(isComplexOrRawVector), quote));
        RAttributes attributes = vector.getAttributes();
        if (attributes != null) {
            sb.append(printAttributes(vector, attributes));
        }
        return builderToString(sb);
    }

    @TruffleBoundary
    @Specialization
    protected String prettyPrint(RNull operand, Object listElementName, byte quote, byte right) {
        return "NULL";
    }

    @TruffleBoundary
    @Specialization
    protected String prettyPrintVector(byte operand, Object listElementName, byte quote, byte right) {
        return concat("[1] ", prettyPrint(operand));
    }

    public static String prettyPrint(byte operand, int width) {
        StringBuilder sb = new StringBuilder();
        String valStr = RRuntime.logicalToString(operand);
        return spaces(sb, width - valStr.length()).append(valStr).toString();
    }

    public static String prettyPrint(byte operand) {
        return RRuntime.logicalToString(operand);
    }

    @TruffleBoundary
    @Specialization
    protected String prettyPrintVector(int operand, Object listElementName, byte quote, byte right) {
        return concat("[1] ", prettyPrint(operand));
    }

    public static String prettyPrint(int operand) {
        return RRuntime.intToString(operand);
    }

    @TruffleBoundary
    @Specialization
    protected String prettyPrintVector(double operand, Object listElementName, byte quote, byte right) {
        return concat("[1] ", prettyPrint(operand));
    }

    public static String prettyPrint(double operand) {
        return doubleToStringPrintFormat(operand, calcRoundFactor(operand, 10000000));
    }

    public static String prettyPrint(double operand, double roundFactor, int digitsBehindDot) {
        return doubleToStringPrintFormat(operand, roundFactor, digitsBehindDot);
    }

    @TruffleBoundary
    @Specialization
    protected String prettyPrintVector(RComplex operand, Object listElementName, byte quote, byte right) {
        return concat("[1] ", prettyPrint(operand));
    }

    public static String prettyPrint(RComplex operand) {
        double rfactor = calcRoundFactor(operand.getRealPart(), 10000000);
        double ifactor = calcRoundFactor(operand.getImaginaryPart(), 10000000);
        return operand.toString(doubleToStringPrintFormat(operand.getRealPart(), rfactor), doubleToStringPrintFormat(operand.getImaginaryPart(), ifactor));
    }

    @TruffleBoundary
    @Specialization
    protected String prettyPrintVector(String operand, Object listElementName, byte quote, byte right) {
        if (RRuntime.fromLogical(quote)) {
            return concat("[1] ", prettyPrint(operand));
        }
        return concat("[1] ", operand);
    }

    public static String prettyPrint(String operand) {
        return RRuntime.quoteString(operand, false);
    }

    @TruffleBoundary
    @Specialization
    protected String prettyPrintVector(RRaw operand, Object listElementName, byte quote, byte right) {
        return concat("[1] ", prettyPrint(operand));
    }

    public static String prettyPrint(RRaw operand) {
        return operand.toString();
    }

    @TruffleBoundary
    @Specialization
    protected String prettyPrint(RFunction operand, Object listElementName, byte quote, byte right) {
        return prettyPrintFunction(operand, listElementName, quote, right, true);
    }

    public String prettyPrintFunction(RFunction operand, Object listElementName, byte quote, byte right, boolean useSource) {
        String string;
        if (operand.isBuiltin()) {
            RBuiltinDescriptor rBuiltin = operand.getRBuiltin();
            RRootNode node = (RRootNode) operand.getTarget().getRootNode();
            FormalArguments formals = node.getFormalArguments();
            StringBuffer sb = new StringBuffer();
            sb.append("function (");
            ArgumentsSignature signature = formals.getSignature();
            for (int i = 0; i < signature.getLength(); i++) {
                RNode defaultArg = formals.getDefaultArgument(i);
                sb.append(signature.getName(i));
                if (defaultArg != null) {
                    sb.append(" = ");
                    Object value = ((ConstantNode) defaultArg).getValue();
                    String printValue = prettyPrintRecursive(value, listElementName, quote, right);
                    // remove the "[1] "
                    sb.append(printValue.substring(4));
                }
                if (i != signature.getLength() - 1) {
                    sb.append(", ");
                }
            }
            sb.append(")  .Primitive(\"");
            sb.append(rBuiltin.getName());
            sb.append("\")");
            string = sb.toString();
        } else {
            String source = ((RRootNode) operand.getTarget().getRootNode()).getSourceCode();
            if (source == null || !useSource) {
                source = RDeparse.deparseForPrint(operand);
            }
            REnvironment env = RArguments.getEnvironment(operand.getEnclosingFrame());
            if (env != null && env.isNamespaceEnv()) {
                source += "\n" + env.getPrintName();
            }
            string = source;
        }
        return printValueAndAttributes(string, operand, false);
    }

    @TruffleBoundary
    @Specialization
    protected String prettyPrint(REnvironment operand, Object listElementName, byte quote, byte right) {
        return printValueAndAttributes(operand.toString(), operand, false);
    }

    @TruffleBoundary
    @Specialization
    protected String prettyPrint(RExpression expr, Object listElementName, byte quote, byte right) {
        StringBuilder builder = new StringBuilder();
        builder.append("expression(");
        RList exprs = expr.getList();
        RStringVector names = (RStringVector) expr.getAttr(attrProfiles, RRuntime.NAMES_ATTR_KEY);
        for (int i = 0; i < exprs.getLength(); i++) {
            if (i != 0) {
                builder.append(", ");
            }
            if (names != null && names.getDataAt(i) != null) {
                builder.append(names.getDataAt(i));
                builder.append(" = ");
            }
            builder.append(prettyPrintSingleVectorElement(exprs.getDataAt(i), quote));
        }
        builder.append(')');
        return builderToString(builder);
    }

    @TruffleBoundary
    @Specialization
    protected String prettyPrintSymbol(RSymbol operand, Object listElementName, byte quote, byte right) {
        return printValueAndAttributes(operand.getName(), operand, false);
    }

    @TruffleBoundary
    @Specialization
    protected String prettyPrintExternalPtr(RExternalPtr operand, Object listElementName, byte quote, byte right) {
        return printValueAndAttributes(String.format("<pointer: %#x>", operand.getAddr()), operand, false);
    }

    @TruffleBoundary
    @Specialization
    protected String prettyPrintPromise(RPromise promise, Object listElementName, byte quote, byte right) {
        if (promise.isEvaluated()) {
            return prettyPrintRecursive(promise.getValue(), listElementName, quote, right);
        } else {
            return prettyPrintPromise(promise);
        }
    }

    @TruffleBoundary
    @Specialization
    protected String prettyPrintLanguage(RLanguage language, Object listElementName, byte quote, byte right) {
        return printValueAndAttributes(prettyPrintLanguageInternal(language), language, true);
    }

    private static String prettyPrintLanguageInternal(RLanguage language) {
        String[] lines = RDeparse.deparse(language, 60, false, 0, -1);
        if (lines.length == 1) {
            return lines[0];
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < lines.length; i++) {
                sb.append(lines[i]);
                if (i == lines.length - 1) {
                    continue;
                }
                sb.append('\n');
            }
            return sb.toString();

        }
    }

    private static String prettyPrintPromise(RPromise promise) {
        RNode node = (RNode) promise.getRep();
        SourceSection ss = node.asRSyntaxNode().getSourceSection();
        if (ss == null) {
            return "<no source available>";
        } else {
            return ss.getCode();
        }
    }

    @TruffleBoundary
    @Specialization
    protected String prettyPrintPairList(RPairList pairList, Object listElementName, byte quote, byte right) {
        StringBuilder sb = new StringBuilder();
        int i = 1;
        Object plObject = pairList;
        RPairList pl = pairList;
        while (!RPairList.isNull(plObject)) {
            if (!pl.isNullTag()) {
                sb.append('$');
                sb.append(pl.getTag());
            } else {
                sb.append("[[");
                sb.append(Integer.toString(i));
                sb.append("]]");
            }
            sb.append('\n');
            if (!RPairList.isNull(pl.car())) {
                sb.append(prettyPrintRecursive(pl.car(), listElementName, quote, right));
                sb.append('\n');
            }
            plObject = pl.cdr();
            if (!RPairList.isNull(plObject)) {
                pl = (RPairList) plObject;
                sb.append('\n');
            }
            i++;
        }
        return sb.toString();
    }

    @TruffleBoundary
    @Specialization
    protected String prettyPrintMissing(RMissing missing, Object listElementName, byte quote, byte right) {
        return "";
    }

    @TruffleBoundary
    @Specialization
    protected String prettyPringS4(RS4Object o, Object listElementName, byte quote, byte right) {
        StringBuilder sb = new StringBuilder();
        sb.append("<S4 Type Object>");
        for (RAttribute attr : o.getAttributes()) {
            printAttribute(sb, attr);
        }
        return sb.toString();
    }

    private static String getStringFromObj(Object o, String msg) {
        if (o instanceof String) {
            return (String) o;
        } else if (o instanceof RStringVector && ((RStringVector) o).getLength() == 1) {
            return ((RStringVector) o).getDataAt(0);
        } else {
            throw RError.error(RError.SHOW_CALLER2, RError.Message.GENERIC, msg);
        }
    }

    private String printAttributes(RAbstractVector vector, RAttributes attributes) {
        StringBuilder builder = new StringBuilder();
        for (RAttribute attr : attributes) {
            if (attr.getName().equals(RRuntime.NAMES_ATTR_KEY) && !vector.hasDimensions()) {
                // names attribute already printed
                continue;
            }
            if (attr.getName().equals(RRuntime.DIM_ATTR_KEY) || attr.getName().equals(RRuntime.DIMNAMES_ATTR_KEY)) {
                // dim and dimnames attributes never gets printed
                continue;
            }
            printAttribute(builder, attr);
        }
        return builderToString(builder);
    }

    /**
     * Encapsulates the printing of the value and attributes for {@link RAttributable} types.
     *
     * @param ignoreNames TODO
     */
    private String printValueAndAttributes(String value, RAttributable object, boolean ignoreNames) {
        RAttributes attributes = object.getAttributes();
        if (attributes == null) {
            return value;
        } else {
            StringBuilder builder = new StringBuilder(value);
            for (RAttribute attr : attributes) {
                if (ignoreNames && attr.getName().equals(RRuntime.NAMES_ATTR_KEY)) {
                    continue;
                }
                printAttribute(builder, attr);
            }
            return builderToString(builder);
        }
    }

    private void printAttribute(StringBuilder builder, RAttribute attr) {
        builder.append("\n");
        builder.append(concat("attr(,\"", attr.getName(), "\")\n"));
        builder.append(prettyPrintAttribute(attr.getValue()));
    }

    private static int getMaxPrintLength() {
        int maxPrint = -1; // infinity
        Object maxPrintObj = RContext.getInstance().stateROptions.getValue("max.print");
        if (maxPrintObj != null) {

            if (maxPrintObj instanceof Integer) {
                return (int) maxPrintObj;
            } else if (maxPrintObj instanceof Double) {
                return RRuntime.double2int(((double) maxPrintObj));
            } else if (maxPrintObj instanceof RAbstractVector) {
                RAbstractVector maxPrintVec = (RAbstractVector) maxPrintObj;
                if (maxPrintVec.getLength() > 0) {
                    if (maxPrintObj instanceof RAbstractIntVector) {
                        maxPrint = ((RAbstractIntVector) maxPrintVec).getDataAt(0);
                    }
                    if (maxPrintObj instanceof RAbstractDoubleVector) {
                        maxPrint = RRuntime.double2int(((RAbstractDoubleVector) maxPrintVec).getDataAt(0));
                    }
                }
            }
        }
        return maxPrint;
    }

    private String printVector(RAbstractVector vector, String[] values, boolean isStringVector, boolean isRawVector) {
        assert vector.getLength() == values.length;
        int maxPrint = getMaxPrintLength();
        if (values.length == 0) {
            String result = concat(RRuntime.classToString(vector.getElementClass()), "(0)");
            if (vector.getNames(attrProfiles) != null) {
                result = concat("named ", result);
            }
            return result;
        } else {
            boolean printNamesHeader = ((!vector.hasDimensions() || (vector.getDimensions().length == 1 && vector.getDimNames(attrProfiles) != null)) && vector.getNames(attrProfiles) != null);
            RStringVector names = printNamesHeader ? vector.getNames(attrProfiles) : null;
            int maxWidth = 0;
            for (String s : values) {
                maxWidth = Math.max(maxWidth, s.length());
            }
            if (printNamesHeader) {
                for (int i = 0; i < names.getLength(); i++) {
                    String s = names.getDataAt(i);
                    if (RRuntime.isNA(s)) {
                        s = RRuntime.NA_HEADER;
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
            int forColumns = RContext.getInstance().getConsoleHandler().getWidth() - leftWidth;
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
                    appendSpaces(builder, maxPositionLength - positionString.length());
                    builder.append("[").append(positionString).append("]");
                }
                for (int j = 0; j < numberOfColumns && index < vector.getLength(); j++) {
                    String valueString = values[index];
                    if (!printNamesHeader) {
                        builder.append(' ');
                        // for some reason vectors of strings are printed differently
                        if (isStringVector) {
                            builder.append(valueString);
                            appendSpaces(builder, (columnWidth - 1) - valueString.length());
                        } else {
                            appendSpaces(builder, (columnWidth - 1) - valueString.length());
                            builder.append(valueString);
                        }
                    } else {
                        int actualColumnWidth = columnWidth;
                        if (j == 0) {
                            actualColumnWidth--;
                        }
                        // for some reason vectors of raw values are printed differently
                        if (!isRawVector) {
                            appendSpaces(builder, actualColumnWidth - valueString.length());
                        }
                        builder.append(valueString);
                        if (isRawVector) {
                            builder.append(' ');
                        }
                        String headerString = names.getDataAt(index);
                        if (RRuntime.isNA(headerString)) {
                            headerString = RRuntime.NA_HEADER;
                        }
                        appendSpaces(headerBuilder, actualColumnWidth - headerString.length());
                        headerBuilder.append(headerString);
                    }
                    index++;
                    if (index == maxPrint) {
                        break;
                    }
                }
                builder.append('\n');
                if (printNamesHeader) {
                    headerBuilder.append('\n');
                    headerBuilder.append(builderToString(builder));
                    builder = new StringBuilder();
                }
                if (index == maxPrint) {
                    break;
                }
            }
            StringBuilder resultBuilder = printNamesHeader ? headerBuilder : builder;
            resultBuilder.deleteCharAt(resultBuilder.length() - 1);
            if (index == maxPrint) {
                resultBuilder.append("\n [ reached getOption(\"max.print\") -- omitted ");
                resultBuilder.append(vector.getLength() - maxPrint);
                resultBuilder.append(" entries ]");
            }
            if (!(vector instanceof RFactorToStringVectorClosure)) {
                // it's a bit of a hack, but factors are meant to be printed using the S3 function
                // anyway - the idea is to suppress attribute printing for factors nested in lists
                RAttributes attributes = vector.getAttributes();
                if (attributes != null) {
                    resultBuilder.append(printAttributes(vector, attributes));
                }
            }
            return builderToString(resultBuilder);
        }
    }

    private static void appendSpaces(StringBuilder builder, int spaces) {
        for (int k = 0; k < spaces; k++) {
            builder.append(' ');
        }
    }

    protected static String padColHeader(int r, int dataColWidth, RAbstractVector vector, boolean isListOrStringVector, RAttributeProfiles attrProfiles) {
        RList dimNames = vector.getDimNames(attrProfiles);
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
            String dimId;
            if (dimNames.getDataAt(1) instanceof String) {
                assert r == 1;
                dimId = (String) dimNames.getDataAt(1);
            } else {
                RStringVector dimNamesVector = (RStringVector) dimNames.getDataAt(1);
                dimId = dimNamesVector.getDataAt(r - 1);
            }
            if (RRuntime.isNA(dimId)) {
                dimId = RRuntime.NA_HEADER;
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

    protected static boolean rowHeaderUsesIndices(RList dimNames) {
        return dimNames == null || dimNames.getDataAt(0) == RNull.instance;
    }

    protected static String rowHeader(int c, RAbstractVector vector, RAttributeProfiles attrProfiles) {
        RList dimNames = vector.getDimNames(attrProfiles);
        if (rowHeaderUsesIndices(dimNames)) {
            return concat("[", intString(c), ",]");
        } else {
            RAbstractStringVector dimNamesVector = (RAbstractStringVector) getDimNamesAt(dimNames, 1);
            String dimId = dimNamesVector.getDataAt(c - 1);
            if (RRuntime.isNA(dimId)) {
                dimId = RRuntime.NA_HEADER;
            }
            return dimId;
        }
    }

    public static StringBuilder spaces(StringBuilder sb, int s) {
        if (s > 0) {
            appendSpaces(sb, s);
        }
        return sb;
    }

    private static String getDimId(RAbstractVector vector, int dimLevel, int dimInd, RAttributeProfiles attrProfiles) {
        String dimId;
        RList dimNames = vector.getDimNames(attrProfiles);
        if (dimNames == null || getDimNamesAt(dimNames, dimLevel) == RNull.instance) {
            dimId = intString(dimInd + 1);
        } else {
            RAbstractStringVector dimNamesVector = (RAbstractStringVector) getDimNamesAt(dimNames, dimLevel);
            dimId = dimNamesVector.getDataAt(dimInd);
        }
        return dimId;
    }

    private static Object getDimNamesAt(RList dimNames, int dimLevel) {
        Object result = dimNames.getDataAt(dimLevel - 1);
        if (result instanceof String) {
            return RString.valueOf((String) result);
        }
        return result;
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

    private static String doubleToStringPrintFormat(double input, double roundFactor, int digitsBehindDot) {
        double data = input;
        if (digitsBehindDot == -1) {
            // processing a single double value or a complex value; use rounding instead of
            // digitsBehindDot (which is in this case invalid) to determine precision
            if (!Double.isNaN(data) && !Double.isInfinite(data)) {
                if (roundFactor < 1) {
                    double inverse = 1 / roundFactor;
                    data = Math.round(data / inverse) * inverse;
                } else {
                    data = Math.round(data * roundFactor) / roundFactor;
                }
            }
        }
        return RRuntime.doubleToString(data, digitsBehindDot);
    }

    private static String doubleToStringPrintFormat(double input, double roundFactor) {
        return doubleToStringPrintFormat(input, roundFactor, -1);
    }

    private String prettyPrintList0(RList operand, Object listElementName, byte quote, byte right) {
        int length = operand.getLength();
        if (length == 0) {
            String result = "list()";
            if (operand.getNames(attrProfiles) != null) {
                result = concat("named ", result);
            }
            return result;
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
                sb.append(prettyPrintSingleListElement(value, name, quote, right)).append("\n\n");
            }
            sb.deleteCharAt(sb.length() - 1);
            RAttributes attributes = operand.getAttributes();
            if (attributes != null) {
                sb.append(printAttributes(operand, attributes));
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

    @TruffleBoundary
    @Specialization(guards = "twoDimsOrMore(operand)")
    protected String prettyPrintM(RList operand, Object listElementName, byte quote, byte right) {
        return printVectorMultiDim(operand, true, false, quote);
    }

    @TruffleBoundary
    @Specialization(guards = "twoDimsOrMore(operand)")
    protected String prettyPrintM(RAbstractStringVector operand, Object listElementName, byte quote, byte right) {
        return printVectorMultiDim(operand, right == RRuntime.LOGICAL_FALSE, false, quote);
    }

    @TruffleBoundary
    @Specialization(guards = "twoDimsOrMore(operand)")
    protected String prettyPrintM(RAbstractComplexVector operand, Object listElementName, byte quote, byte right) {
        return printVectorMultiDim(operand, false, true, quote);
    }

    @TruffleBoundary
    @Specialization(guards = "twoDimsOrMore(operand)")
    protected String prettyPrintM(RAbstractRawVector operand, Object listElementName, byte quote, byte right) {
        return printVectorMultiDim(operand, false, true, quote);
    }

    @TruffleBoundary
    @Specialization(guards = "twoDimsOrMore(operand)")
    protected String prettyPrintM(RAbstractDoubleVector operand, Object listElementName, byte quote, byte right) {
        return printVectorMultiDim(operand, false, false, quote);
    }

    @TruffleBoundary
    @Specialization(guards = "twoDimsOrMore(operand)")
    protected String prettyPrintM(RAbstractIntVector operand, Object listElementName, byte quote, byte right) {
        return printVectorMultiDim(operand, false, false, quote);
    }

    @TruffleBoundary
    @Specialization(guards = "twoDimsOrMore(operand)")
    protected String prettyPrintM(RAbstractLogicalVector operand, Object listElementName, byte quote, byte right) {
        return printVectorMultiDim(operand, false, false, quote);
    }

    @TruffleBoundary
    @Specialization(guards = "!twoDimsOrMore(operand)")
    protected String prettyPrint(RList operand, Object listElementName, byte quote, byte right) {
        return prettyPrintList0(operand, listElementName, quote, right);
    }

    private static double getMaxRoundFactor(RAbstractDoubleVector operand) {
        double maxRoundFactor = 0;
        for (int i = 0; i < operand.getLength(); i++) {
            double data = operand.getDataAt(i);
            double roundFactor = calcRoundFactor(data, 10000000);
            if (roundFactor > maxRoundFactor) {
                maxRoundFactor = roundFactor;
            }
        }
        return maxRoundFactor;
    }

    private static int getMaxDigitsBehindDot(double maxRoundFactor) {
        int maxDigitsBehindDot = 0;
        for (double j = 1; j < maxRoundFactor; j *= 10) {
            maxDigitsBehindDot++;
        }
        return maxDigitsBehindDot;
    }

    @TruffleBoundary
    @Specialization(guards = "!twoDimsOrMore(operand)")
    protected String prettyPrint(RAbstractDoubleVector operand, Object listElementName, byte quote, byte right) {
        int length = operand.getLength();
        String[] values = new String[length];
        double maxRoundFactor = getMaxRoundFactor(operand);
        int maxDigitsBehindDot = getMaxDigitsBehindDot(maxRoundFactor);
        for (int i = 0; i < length; i++) {
            double data = operand.getDataAt(i);
            values[i] = prettyPrint(data, maxRoundFactor, maxDigitsBehindDot);
        }
        padTrailingDecimalPointAndZeroesIfRequired(values);
        return printVector(operand, values, false, false);
    }

    @TruffleBoundary
    @Specialization(guards = "!twoDimsOrMore(operand)")
    protected String prettyPrint(RAbstractIntVector operand, Object listElementName, byte quote, byte right) {
        int length = operand.getLength();
        String[] values = new String[length];
        for (int i = 0; i < length; i++) {
            int data = operand.getDataAt(i);
            values[i] = prettyPrint(data);
        }
        return printVector(operand, values, false, false);
    }

    @TruffleBoundary
    @Specialization(guards = "!twoDimsOrMore(operand)")
    protected String prettyPrint(RAbstractStringVector operand, Object listElementName, byte quote, byte right) {
        int length = operand.getLength();
        String[] values = new String[length];
        for (int i = 0; i < length; i++) {
            String data = operand.getDataAt(i);
            if (RRuntime.fromLogical(quote)) {
                values[i] = prettyPrint(data);
            } else {
                if (RRuntime.isNA(data)) {
                    values[i] = RRuntime.NA_HEADER;
                } else {
                    values[i] = data;
                }
            }
        }
        return printVector(operand, values, true, false);
    }

    @TruffleBoundary
    @Specialization(guards = "!twoDimsOrMore(operand)")
    protected String prettyPrint(RAbstractLogicalVector operand, Object listElementName, byte quote, byte right) {
        int length = operand.getLength();
        String[] values = new String[length];
        for (int i = 0; i < length; i++) {
            byte data = operand.getDataAt(i);
            values[i] = prettyPrint(data);
        }
        return printVector(operand, values, false, false);
    }

    @TruffleBoundary
    @Specialization(guards = "!twoDimsOrMore(operand)")
    protected String prettyPrint(RAbstractRawVector operand, Object listElementName, byte quote, byte right) {
        int length = operand.getLength();
        String[] values = new String[length];
        for (int i = 0; i < length; i++) {
            RRaw data = operand.getDataAt(i);
            values[i] = prettyPrint(data);
        }
        return printVector(operand, values, false, true);
    }

    @TruffleBoundary
    @Specialization(guards = "!twoDimsOrMore(operand)")
    protected String prettyPrint(RAbstractComplexVector operand, Object listElementName, byte quote, byte right) {
        if (re == null) {
            // the two are allocated side by side; checking for re is sufficient
            CompilerDirectives.transferToInterpreterAndInvalidate();
            re = insert(UnaryArithmeticNodeGen.create(Re.RE, RError.Message.NON_NUMERIC_MATH, RType.Double));
            im = insert(UnaryArithmeticNodeGen.create(Im.IM, RError.Message.NON_NUMERIC_MATH, RType.Double));
        }

        RAbstractDoubleVector realParts = (RAbstractDoubleVector) re.execute(operand);
        RAbstractDoubleVector imaginaryParts = (RAbstractDoubleVector) im.execute(operand);

        int length = operand.getLength();
        String[] realValues = new String[length];
        String[] imaginaryValues = new String[length];
        for (int i = 0; i < length; i++) {
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
        return printVector(operand, values, false, false);
    }

    protected static boolean twoDimsOrMore(RAbstractContainer v) {
        return v.hasDimensions() && v.getDimensions().length > 1;
    }

    protected static boolean isLengthOne(RAbstractIntVector v) {
        return v.getLength() == 1;
    }

    private static String builderToString(StringBuilder sb) {
        return sb.toString();
    }

    private static String builderToSubstring(StringBuilder sb, int start, int end) {
        return sb.substring(start, end);
    }

    private static String intString(int x) {
        return Integer.toString(x);
    }

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

    private static String substring(String s, int start) {
        return s.substring(start);
    }

    private static int requiresDecimalPointsAndTrailingZeroes(String[] values, int[] decimalPointOffsets, int[] lenAfterPoint) {
        boolean foundWithDecimalPoint = false;
        boolean foundWithoutDecimalPoint = false;
        boolean inequalLenAfterPoint = false;
        int maxLenAfterPoint = -1;
        for (int i = 0; i < values.length; i++) {
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

    public static void padTrailingDecimalPointAndZeroesIfRequired(String[] values) {
        int[] decimalPointOffsets = new int[values.length];
        int[] lenAfterPoint = new int[values.length];
        int maxLenAfterPoint = requiresDecimalPointsAndTrailingZeroes(values, decimalPointOffsets, lenAfterPoint);
        if (maxLenAfterPoint == -1) {
            return;
        }

        for (int i = 0; i < values.length; i++) {
            String v = values[i];
            if (RRuntime.isNA(v) || "NaN".equals(v)) {
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
        for (int i = 0; i < values.length; i++) {
            String v = values[i];
            if (RRuntime.isNA(v)) {
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
        for (int i = 0; i < values.length; i++) {
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
        for (int i = 0; i < values.length; i++) {
            String v = values[i];
            if (v.charAt(0) == '-') {
                values[i] = substring(v, 1);
            }
        }
    }

    public static String prettyPrintDefault(Object value) {
        return (String) Truffle.getRuntime().createCallTarget(new RootNode(TruffleLanguage.class, null, null) {

            @Child PrettyPrinterNode node = PrettyPrinterNodeGen.create(null, null, null, null, false);

            @Override
            public Object execute(VirtualFrame frame) {
                return node.executeString(frame.getArguments()[0], null, RRuntime.LOGICAL_FALSE, RRuntime.LOGICAL_FALSE);
            }
        }).call(value);
    }

    @NodeChildren({@NodeChild(value = "operand", type = RNode.class), @NodeChild(value = "listElementName", type = RNode.class), @NodeChild(value = "quote", type = RNode.class),
                    @NodeChild(value = "right", type = RNode.class)})
    abstract static class PrettyPrinterSingleListElementNode extends RNode {

        @Child private PrettyPrinterNode prettyPrinter;
        @Child private CastStringNode castStringNode;

        private final RAttributeProfiles attrProfiles = RAttributeProfiles.create();

        private final NACheck naCheck = NACheck.create();

        private void initCast(Object listElementName) {
            if (prettyPrinter == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                prettyPrinter = insert(PrettyPrinterNodeGen.create(null, null, null, null, false));
            }
        }

        private void initCast() {
            if (castStringNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                castStringNode = insert(CastStringNodeGen.create(false, false, false, false));
            }
        }

        private String prettyPrintSingleElement(byte o, Object listElementName, byte quote, byte right) {
            initCast(listElementName);
            return (String) prettyPrinter.executeString(o, listElementName, quote, right);
        }

        private String prettyPrintSingleElement(int o, Object listElementName, byte quote, byte right) {
            initCast(listElementName);
            return (String) prettyPrinter.executeString(o, listElementName, quote, right);
        }

        private String prettyPrintSingleElement(double o, Object listElementName, byte quote, byte right) {
            initCast(listElementName);
            return (String) prettyPrinter.executeString(o, listElementName, quote, right);
        }

        private String prettyPrintSingleElement(Object o, Object listElementName, byte quote, byte right) {
            initCast(listElementName);
            return (String) prettyPrinter.executeString(o, listElementName, quote, right);
        }

        public abstract Object executeString(int o, Object listElementName, byte quote, byte right);

        public abstract Object executeString(double o, Object listElementName, byte quote, byte right);

        public abstract Object executeString(byte o, Object listElementName, byte quote, byte right);

        public abstract Object executeString(Object o, Object listElementName, byte quote, byte right);

        @Specialization
        protected String prettyPrintListElement(SocketConnections.RSocketConnection operand, Object listElementName, byte quote, byte right) {
            // TODO; fixing this properly would likely require overhaul of the whole
            // formatting/printing infrastructure, which does not seem worth it at this point
            return "SOCKET CONNECTION";
        }

        @TruffleBoundary
        @Specialization
        protected String prettyPrintListElement(RNull operand, Object listElementName, byte quote, byte right) {
            return prettyPrintSingleElement(operand, listElementName, quote, right);
        }

        @TruffleBoundary
        @Specialization
        protected String prettyPrintListElement(byte operand, Object listElementName, byte quote, byte right) {
            return prettyPrintSingleElement(operand, listElementName, quote, right);
        }

        @TruffleBoundary
        @Specialization
        protected String prettyPrintListElement(int operand, Object listElementName, byte quote, byte right) {
            return prettyPrintSingleElement(operand, listElementName, quote, right);
        }

        @TruffleBoundary
        @Specialization
        protected String prettyPrintListElement(double operand, Object listElementName, byte quote, byte right) {
            return prettyPrintSingleElement(operand, listElementName, quote, right);
        }

        @TruffleBoundary
        @Specialization
        protected String prettyPrintListElement(RComplex operand, Object listElementName, byte quote, byte right) {
            return prettyPrintSingleElement(operand, listElementName, quote, right);
        }

        @TruffleBoundary
        @Specialization
        protected String prettyPrintListElement(String operand, Object listElementName, byte quote, byte right) {
            return prettyPrintSingleElement(operand, listElementName, quote, right);
        }

        @TruffleBoundary
        @Specialization
        protected String prettyPrintListElement(RRaw operand, Object listElementName, byte quote, byte right) {
            return prettyPrintSingleElement(operand, listElementName, quote, right);
        }

        @TruffleBoundary
        @Specialization
        protected String prettyPrintListElement(RAbstractVector operand, Object listElementName, byte quote, byte right) {
            return prettyPrintSingleElement(operand, listElementName, quote, right);
        }

        @TruffleBoundary
        @Specialization
        protected String prettyPrintListElement(RSymbol operand, Object listElementName, byte quote, byte right) {
            return prettyPrintSingleElement(operand, listElementName, quote, right);
        }

        @TruffleBoundary
        @Specialization
        protected String prettyPrintListElement(RLanguage operand, Object listElementName, byte quote, byte right) {
            return prettyPrintSingleElement(operand, listElementName, quote, right);
        }

        @TruffleBoundary
        @Specialization
        protected String prettyPrintListElement(REnvironment operand, Object listElementName, byte quote, byte right) {
            return prettyPrintSingleElement(operand, listElementName, quote, right);
        }

        @TruffleBoundary
        @Specialization
        protected String prettyPrintListElement(RFunction operand, Object listElementName, byte quote, byte right) {
            return prettyPrintSingleElement(operand, listElementName, quote, right);
        }

        @TruffleBoundary
        @Specialization
        protected String prettyPrintListElement(RExternalPtr operand, Object listElementName, byte quote, byte right) {
            return prettyPrintSingleElement(operand, listElementName, quote, right);
        }

        // TODO: this should be handled by an S3 function
        @Specialization
        protected String prettyPrintListElement(RFactor operand, Object listElementName, byte quote, byte right) {
            RVector vec = operand.getLevels(attrProfiles);
            String[] strings;
            if (vec == null) {
                strings = new String[0];
            } else {
                initCast();
                strings = new String[vec.getLength()];
                for (int i = 0; i < vec.getLength(); i++) {
                    strings[i] = (String) castStringNode.executeString(vec.getDataAtAsObject(i));
                }
            }
            return formatLevelStrings(operand, listElementName, right, vec, strings);
        }

        @TruffleBoundary
        private String formatLevelStrings(RFactor operand, Object listElementName, byte right, RVector vec, String[] strings) {
            StringBuilder sb = new StringBuilder(prettyPrintSingleElement(RClosures.createFactorToVector(operand, true, attrProfiles), listElementName, RRuntime.LOGICAL_FALSE, right));
            sb.append("\nLevels:");
            if (vec != null) {
                for (int i = 0; i < vec.getLength(); i++) {
                    sb.append(" ");
                    sb.append(strings[i]);
                }
            }
            return sb.toString();
        }
    }

    @NodeChildren({@NodeChild(value = "operand", type = RNode.class), @NodeChild(value = "isQuoted", type = RNode.class)})
    abstract static class PrettyPrinterSingleVectorElementNode extends RNode {

        @Child private PrettyPrinterSingleVectorElementNode recursivePrettyPrinter;

        private String prettyPrintRecursive(Object o, byte isQuoted) {
            if (recursivePrettyPrinter == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                recursivePrettyPrinter = insert(PrettyPrinterSingleVectorElementNodeGen.create(null, null));
            }
            return (String) recursivePrettyPrinter.executeString(o, isQuoted);
        }

        public abstract Object executeString(Object o, byte isQuoted);

        @TruffleBoundary
        @Specialization
        protected String prettyPrintVectorElement(RNull operand, byte isQuoted) {
            return "NULL";
        }

        @TruffleBoundary
        @Specialization
        protected String prettyPrintVectorElement(byte operand, byte isQuoted) {
            return prettyPrint(operand);
        }

        @TruffleBoundary
        @Specialization
        protected String prettyPrintVectorElement(int operand, byte isQuoted) {
            return prettyPrint(operand);
        }

        @TruffleBoundary
        @Specialization
        protected String prettyPrintVectorElement(double operand, byte isQuoted) {
            return prettyPrint(operand);
        }

        @TruffleBoundary
        @Specialization
        protected String prettyPrintVectorElement(RComplex operand, byte isQuoted) {
            return prettyPrint(operand);
        }

        @TruffleBoundary
        @Specialization
        protected String prettyPrintVectorElement(String operand, byte isQuoted) {
            if (RRuntime.fromLogical(isQuoted)) {
                return prettyPrint(operand);
            }
            return operand;
        }

        @TruffleBoundary
        @Specialization
        protected String prettyPrintVectorElement(RRaw operand, byte isQuoted) {
            return prettyPrint(operand);
        }

        @TruffleBoundary
        @Specialization
        protected String prettyPrintVectorElement(RList operand, byte isQuoted) {
            return prettyPrint(operand);
        }

        @TruffleBoundary
        @Specialization(guards = {"operand.getLength() != 1", "!isVectorList(operand)"})
        protected String prettyPrintVectorElement(RAbstractVector operand, byte isQuoted) {
            return prettyPrint(operand);
        }

        @TruffleBoundary
        @Specialization(guards = {"operand.getLength() == 1", "!isVectorList(operand)"})
        protected String prettyPrintVectorElementLengthOne(RAbstractVector operand, byte isQuoted) {
            return prettyPrintRecursive(operand.getDataAtAsObject(0), isQuoted);
        }

        @TruffleBoundary
        @Specialization
        protected String prettyPrintVectorElement(RLanguage operand, byte isQuoted) {
            return prettyPrintLanguageInternal(operand);
        }

        @TruffleBoundary
        @Specialization
        protected String prettyPrintVectorElement(RSymbol operand, byte isQuoted) {
            return operand.getName();
        }

        protected static boolean isVectorList(RAbstractVector v) {
            return v instanceof RList;
        }

        protected static boolean isLengthOne(RAbstractVector v) {
            return v.getLength() == 1;
        }
    }

    @NodeChildren({@NodeChild(value = "vector", type = RNode.class), @NodeChild(value = "isListOrStringVector", type = RNode.class), @NodeChild(value = "isComplexOrRawVector", type = RNode.class),
                    @NodeChild(value = "isQuoted", type = RNode.class)})
    abstract static class PrintVectorMultiDimNode extends RNode {

        @Child private PrintVector2DimNode vector2DimPrinter;
        @Child private PrintDimNode dimPrinter;

        private final RAttributeProfiles attrProfiles = RAttributeProfiles.create();

        private String printVector2Dim(RAbstractVector vector, RIntVector dimensions, int offset, byte isListOrStringVector, byte isComplexOrRawVector, byte isQuoted) {
            if (vector2DimPrinter == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                vector2DimPrinter = insert(PrintVector2DimNodeGen.create(null, null, null, null, null, null));
            }
            return (String) vector2DimPrinter.executeString(vector, dimensions, offset, isListOrStringVector, isComplexOrRawVector, isQuoted);
        }

        private String printDim(RAbstractVector vector, byte isListOrStringVector, byte isComplexOrRawVector, int currentDimLevel, int arrayBase, int accDimensions, String header, byte isQuoted) {
            if (dimPrinter == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                dimPrinter = insert(PrintDimNodeGen.create(null, null, null, null, null, null, null, null));
            }
            return (String) dimPrinter.executeString(vector, isListOrStringVector, isComplexOrRawVector, currentDimLevel, arrayBase, accDimensions, header, isQuoted);
        }

        public abstract Object executeString(RAbstractVector vector, byte isListOrStringVector, byte isComplexOrRawVector, byte isQuoted);

        @TruffleBoundary
        @Specialization
        protected String printVectorMultiDim(RAbstractVector vector, byte isListOrStringVector, byte isComplexOrRawVector, byte isQuoted) {
            int[] dimensions = vector.getDimensions();
            RIntVector dimensionsVector = RDataFactory.createIntVector(dimensions, RDataFactory.COMPLETE_VECTOR);
            assert dimensions != null;
            int numDimensions = dimensions.length;
            assert numDimensions > 1;
            if (numDimensions == 2) {
                return printVector2Dim(vector, dimensionsVector, 0, isListOrStringVector, isComplexOrRawVector, isQuoted);
            } else {
                int dimSize = dimensions[numDimensions - 1];
                if (dimSize == 0) {
                    return "";
                }
                StringBuilder sb = new StringBuilder();
                if (numDimensions == 3) {
                    int matrixSize = dimensions[0] * dimensions[1];
                    for (int dimInd = 0; dimInd < dimSize; dimInd++) {
                        // Checkstyle: stop
                        sb.append(", , ");
                        // Checkstyle: resume
                        sb.append(getDimId(vector, numDimensions, dimInd, attrProfiles));
                        sb.append("\n\n");
                        sb.append(printVector2Dim(vector, dimensionsVector, dimInd * matrixSize, isListOrStringVector, isComplexOrRawVector, isQuoted));
                        sb.append("\n");
                        if (dimInd < (dimSize - 1) && vector.getLength() > 0 || vector.getLength() == 0) {
                            sb.append("\n");
                        }
                    }
                } else {
                    int accDimensions = vector.getLength() / dimSize;
                    for (int dimInd = 0; dimInd < dimSize; dimInd++) {
                        int arrayBase = accDimensions * dimInd;
                        String dimId = getDimId(vector, numDimensions, dimInd, attrProfiles);
                        String innerDims = printDim(vector, isListOrStringVector, isComplexOrRawVector, numDimensions - 1, arrayBase, accDimensions, dimId, isQuoted);
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
                    @NodeChild(value = "isListOrStringVector", type = RNode.class), @NodeChild(value = "isComplexOrRawVector", type = RNode.class), @NodeChild(value = "isQuoted", type = RNode.class)})
    abstract static class PrintVector2DimNode extends RNode {

        @Child private PrettyPrinterSingleVectorElementNode singleVectorElementPrettyPrinter;

        private final RAttributeProfiles attrProfiles = RAttributeProfiles.create();

        private String prettyPrintSingleVectorElement(Object o, byte isQuoted) {
            if (singleVectorElementPrettyPrinter == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                singleVectorElementPrettyPrinter = insert(PrettyPrinterSingleVectorElementNodeGen.create(null, null));
            }
            return (String) singleVectorElementPrettyPrinter.executeString(o, isQuoted);
        }

        public abstract Object executeString(RAbstractVector vector, RIntVector dimensions, int offset, byte isListOrStringVector, byte isComplexOrRawVector, byte isQuoted);

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

        @TruffleBoundary
        @Specialization(guards = "vector.getLength() == 0")
        protected String printVector2DimEmpty(RAbstractVector vector, RIntVector dimensions, int offset, byte isListOrStringVector, byte isComplexOrRawVector, byte isQuoted) {
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
            RList dimNames = vector.getDimNames(attrProfiles);
            if (ncol > 0) {
                sb.append("     ");
                for (int c = 1; c <= ncol; c++) {
                    sb.append(getDimId(dimNames, 1, c, isComplexOrRawVector));
                    if (c < ncol) {
                        sb.append(' ');
                    }
                }
            }
            if (nrow > 0) {
                sb.append('\n');
                for (int r = 1; r <= nrow; r++) {
                    sb.append(getDimId(dimNames, 0, r, isComplexOrRawVector));
                    if (r < nrow) {
                        sb.append('\n');
                    }
                }
            }
            return builderToString(sb);
        }

        @TruffleBoundary
        @Specialization(guards = "vector.getLength() != 0")
        protected String printVector2Dim(RAbstractDoubleVector vector, RIntVector dimensions, int offset, byte isListOrStringVector, byte isComplexOrRawVector, byte isQuoted) {
            int nrow = dimensions.getDataAt(0);
            int ncol = dimensions.getDataAt(1);

            // prepare data (relevant for column widths)
            String[] dataStrings = new String[nrow * ncol];
            int[] dataColWidths = new int[ncol];
            RList dimNames = vector.getDimNames(attrProfiles);
            RStringVector columnDimNames = null;
            if (dimNames != null && dimNames.getDataAt(1) != RNull.instance) {
                columnDimNames = (RStringVector) dimNames.getDataAt(1);
            }
            double[] maxRoundFactors = new double[ncol];
            int[] maxDigitsBehindDot = new int[ncol];
            for (int c = 0; c < ncol; c++) {
                maxRoundFactors[c] = 0;
                for (int r = 0; r < nrow; r++) {
                    int index = c * nrow + r;
                    double data = vector.getDataAt(index + offset);
                    double roundFactor = calcRoundFactor(data, 10000000);
                    if (roundFactor > maxRoundFactors[c]) {
                        maxRoundFactors[c] = roundFactor;
                    }
                }
                maxDigitsBehindDot[c] = getMaxDigitsBehindDot(maxRoundFactors[c]);
            }
            int rowHeaderWidth = 0;
            for (int c = 0; c < ncol; c++) {
                for (int r = 0; r < nrow; r++) {
                    int index = c * nrow + r;
                    dataStrings[index] = prettyPrint(vector.getDataAt(index + offset), maxRoundFactors[c], maxDigitsBehindDot[c]);
                    maintainColumnData(dataColWidths, columnDimNames, c, dataStrings[index]);
                    rowHeaderWidth = Math.max(rowHeaderWidth, rowHeader(r + 1, vector, attrProfiles).length());
                }
            }

            // probably add trailing decimal points and zeroes
            // iterate over columns
            for (int c = 0; c < ncol; c++) {
                postProcessDoubleColumn(dataStrings, nrow, ncol, c);
                // final adjustment of column width
                boolean hasNegative = false;
                for (int r = 0; r < nrow; r++) {
                    // do not count minus signs
                    String data = dataStrings[c * nrow + r];
                    boolean isNegative = data.charAt(0) == '-';
                    hasNegative = hasNegative || isNegative;
                    int l = isNegative ? data.length() - 1 : data.length();
                    if (l > dataColWidths[c]) {
                        dataColWidths[c] = l;
                    }
                }
                if (hasNegative) {
                    dataColWidths[c] = -dataColWidths[c];
                }
            }

            return formatResult(vector, nrow, ncol, dataStrings, dataColWidths, rowHeaderWidth, isListOrStringVector == RRuntime.LOGICAL_TRUE);
        }

        @TruffleBoundary
        @Specialization(guards = "vector.getLength() != 0")
        protected String printVector2Dim(RAbstractComplexVector vector, RIntVector dimensions, int offset, byte isListOrStringVector, byte isComplexOrRawVector, byte isQuoted) {
            int nrow = dimensions.getDataAt(0);
            int ncol = dimensions.getDataAt(1);

            // prepare data (relevant for column widths)
            String[] reStrings = new String[nrow * ncol];
            String[] imStrings = new String[nrow * ncol];
            String[] siStrings = new String[nrow * ncol];
            int[] dataColWidths = new int[ncol];
            RList dimNames = vector.getDimNames(attrProfiles);
            RStringVector columnDimNames = null;
            if (dimNames != null && dimNames.getDataAt(1) != RNull.instance) {
                columnDimNames = (RStringVector) dimNames.getDataAt(1);
            }
            int rowHeaderWidth = 0;
            for (int r = 0; r < nrow; r++) {
                for (int c = 0; c < ncol; c++) {
                    int index = c * nrow + r;
                    reStrings[index] = prettyPrintSingleVectorElement(vector.getDataAt(index + offset).getRealPart(), isQuoted);
                    imStrings[index] = prettyPrintSingleVectorElement(vector.getDataAt(index + offset).getImaginaryPart(), isQuoted);
                    siStrings[index] = vector.getDataAt(index + offset).getImaginaryPart() < 0.0 ? "-" : "+";
                    // "" because column width is computed later
                    maintainColumnData(dataColWidths, columnDimNames, c, "");
                }
                rowHeaderWidth = Math.max(rowHeaderWidth, rowHeader(r + 1, vector, attrProfiles).length());
            }

            // adjust formatting
            // iterate over columns
            for (int c = 0; c < ncol; c++) {
                postProcessComplexColumn(reStrings, imStrings, nrow, ncol, c);
            }

            String[] dataStrings = new String[nrow * ncol];
            for (int i = 0; i < dataStrings.length; i++) {
                dataStrings[i] = vector.getDataAt(i).isNA() ? "NA" : concat(reStrings[i], siStrings[i], imStrings[i], "i");
            }

            // final adjustment of column width
            for (int c = 0; c < ncol; c++) {
                for (int r = 0; r < nrow; r++) {
                    // do not count minus signs
                    String data = dataStrings[c * nrow + r];
                    int l = data.charAt(0) == '-' ? data.length() - 1 : data.length();
                    if (l > dataColWidths[c]) {
                        dataColWidths[c] = l;
                    }
                }
            }

            return formatResult(vector, nrow, ncol, dataStrings, dataColWidths, rowHeaderWidth, isListOrStringVector == RRuntime.LOGICAL_TRUE);
        }

        @TruffleBoundary
        @Specialization(guards = {"vector.getLength() != 0", "notDoubleOrComplex(vector)"})
        protected String printVector2Dim(RAbstractVector vector, RIntVector dimensions, int offset, byte isListOrStringVector, byte isComplexOrRawVector, byte isQuoted) {
            int nrow = dimensions.getDataAt(0);
            int ncol = dimensions.getDataAt(1);

            // prepare data (relevant for column widths)
            String[] dataStrings = new String[nrow * ncol];
            int[] dataColWidths = new int[ncol];
            RList dimNames = vector.getDimNames(attrProfiles);
            RStringVector columnDimNames = null;
            if (dimNames != null && dimNames.getDataAt(1) != RNull.instance) {
                if (dimNames.getDataAt(1) instanceof String) {
                    columnDimNames = RDataFactory.createStringVector((String) dimNames.getDataAt(1));
                } else {
                    columnDimNames = (RStringVector) dimNames.getDataAt(1);
                }
            }
            int rowHeaderWidth = 0;
            for (int r = 0; r < nrow; r++) {
                for (int c = 0; c < ncol; c++) {
                    int index = c * nrow + r;
                    dataStrings[index] = prettyPrintSingleVectorElement(vector.getDataAtAsObject(index + offset), isQuoted);
                    maintainColumnData(dataColWidths, columnDimNames, c, dataStrings[index]);
                }
                rowHeaderWidth = Math.max(rowHeaderWidth, rowHeader(r + 1, vector, attrProfiles).length());
            }

            return formatResult(vector, nrow, ncol, dataStrings, dataColWidths, rowHeaderWidth, isListOrStringVector == RRuntime.LOGICAL_TRUE);
        }

        protected boolean notDoubleOrComplex(RAbstractVector vector) {
            return vector.getElementClass() != RDouble.class && vector.getElementClass() != RComplex.class;
        }

        private static void postProcessDoubleColumn(String[] dataStrings, int nrow, int ncol, int col) {
            // create and populate array with column data
            String[] columnData = new String[nrow];
            for (int r = 0; r < nrow; r++) {
                columnData[r] = dataStrings[col * nrow + r];
            }
            padTrailingDecimalPointAndZeroesIfRequired(columnData);
            // put possibly changed data back
            for (int r = 0; r < nrow; r++) {
                dataStrings[col * nrow + r] = columnData[r];
            }
        }

        private static void postProcessComplexColumn(String[] re, String[] im, int nrow, int ncol, int col) {
            // create and populate arrays with column data
            String[] cre = new String[nrow];
            String[] cim = new String[nrow];
            for (int r = 0; r < nrow; r++) {
                cre[r] = re[col * nrow + r];
                cim[r] = im[col * nrow + r];
            }

            padTrailingDecimalPointAndZeroesIfRequired(cre);
            padTrailingDecimalPointAndZeroesIfRequired(cim);
            removeLeadingMinus(cim);
            rightJustify(cre);
            rightJustify(cim);

            // put possibly changed data back
            for (int r = 0; r < nrow; r++) {
                re[col * nrow + r] = cre[r];
                im[col * nrow + r] = cim[r];
            }
        }

        private static void maintainColumnData(int[] dataColWidths, RStringVector columnDimNames, int c, String data) {
            // do not count minus signs
            int dataLength = !data.equals("") && data.charAt(0) == '-' ? data.length() - 1 : data.length();
            if (dataLength > dataColWidths[c]) {
                dataColWidths[c] = dataLength;
            }
            if (columnDimNames != null) {
                String columnName = columnDimNames.getDataAt(c);
                if (RRuntime.isNA(columnName)) {
                    columnName = RRuntime.NA_HEADER;
                }
                if (columnName.length() > dataColWidths[c]) {
                    dataColWidths[c] = columnName.length();
                }
            }
        }

        private String formatResult(RAbstractVector vector, int nrow, int ncol, String[] dataStrings, int[] dataColWidths, int rowHeaderWidth, boolean isListOrStringVector) {
            boolean isComplexVector = vector.getElementClass() == RComplex.class;
            boolean isDoubleVector = vector.getElementClass() == RDouble.class;
            String rowFormat = concat("%", intString(rowHeaderWidth), "s");

            StringBuilder b = new StringBuilder();

            int colInd = 0;
            while (true) {
                int totalWidth = rowHeaderWidth + 1;
                int startColInd = colInd;
                for (; colInd < dataColWidths.length; colInd++) {
                    boolean hasNegative = dataColWidths[colInd] < 0;
                    totalWidth += Math.abs(dataColWidths[colInd]) + ((isDoubleVector || isComplexVector) && hasNegative ? 2 : 1);
                    if (totalWidth > RContext.getInstance().getConsoleHandler().getWidth()) {
                        if (colInd == startColInd) {
                            // the first column is already too wide but needs to be printed
                            // nevertheless
                            colInd++;
                        }
                        break;
                    }
                }

                // column header
                spaces(b, rowHeaderWidth + 1);
                for (int c = startColInd + 1; c <= colInd; c++) {
                    boolean hasNegative = dataColWidths[c - 1] < 0;
                    // header of the first column needs extra padding if this column has negative
                    // numbers
                    int padding = Math.abs(dataColWidths[c - 1]) + ((isDoubleVector || isComplexVector) && hasNegative ? 1 : 0);
                    b.append(padColHeader(c, padding, vector, isListOrStringVector, attrProfiles));
                    if (c < colInd) {
                        b.append(" ");
                    }
                }
                b.append('\n');

                boolean indexRowHeaders = rowHeaderUsesIndices(vector.getDimNames(attrProfiles));

                // rows
                for (int r = 1; r <= nrow; r++) {
                    String headerString = rowHeader(r, vector, attrProfiles);
                    if (indexRowHeaders) {
                        spaces(b, rowHeaderWidth - headerString.length());
                        b.append(headerString).append(' ');
                    } else {
                        b.append(headerString);
                        spaces(b, rowHeaderWidth - headerString.length() + 1);
                    }
                    for (int c = startColInd + 1; c <= colInd; c++) {
                        String dataString = dataStrings[(c - 1) * nrow + (r - 1)];
                        boolean hasNegative = dataColWidths[c - 1] < 0;
                        int padding = Math.abs(dataColWidths[c - 1]) + ((isDoubleVector || isComplexVector) && hasNegative ? 1 : 0);
                        if (isListOrStringVector || (isComplexVector && !RRuntime.STRING_NA.equals(dataString))) {
                            // list elements are left-justified, and so are complex matrix elements
                            // that are not NA
                            b.append(dataString);
                            spaces(b, padColHeader(c, padding, vector, isListOrStringVector, attrProfiles).length() - dataString.length());
                        } else {
                            // vector elements are right-justified, and so are NAs in complex
                            // matrices
                            String cellFormat = concat("%", intString(padColHeader(c, padding, vector, isListOrStringVector, attrProfiles).length()), "s");
                            b.append(stringFormat(cellFormat, dataString));
                        }
                        if (c < colInd) {
                            b.append(' ');
                        }
                    }
                    if (r < nrow) {
                        b.append('\n');
                    }
                }
                if (colInd < dataColWidths.length) {
                    b.append('\n');
                } else {
                    break;
                }
            }
            return builderToString(b);
        }
    }

    @NodeChildren({@NodeChild(value = "vector", type = RNode.class), @NodeChild(value = "isListOrStringVector", type = RNode.class), @NodeChild(value = "isComplexOrRawVector", type = RNode.class),
                    @NodeChild(value = "currentDimLevel", type = RNode.class), @NodeChild(value = "arrayBase", type = RNode.class), @NodeChild(value = "accDimensions", type = RNode.class),
                    @NodeChild(value = "header", type = RNode.class), @NodeChild(value = "isQuoted", type = RNode.class)})
    abstract static class PrintDimNode extends RNode {

        public abstract Object executeString(RAbstractVector vector, byte isListOrStringVector, byte isComplexOrRawVector, int currentDimLevel, int arrayBase, int accDimensions,
                        String header, byte isQuoted);

        @Child private PrintVector2DimNode vector2DimPrinter;
        @Child private PrintDimNode dimPrinter;

        private final RAttributeProfiles attrProfiles = RAttributeProfiles.create();

        private String printVector2Dim(RAbstractVector vector, RIntVector dimensions, int offset, byte isListOrStringVector, byte isComplexOrRawVector, byte isQuoted) {
            if (vector2DimPrinter == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                vector2DimPrinter = insert(PrintVector2DimNodeGen.create(null, null, null, null, null, null));
            }
            return (String) vector2DimPrinter.executeString(vector, dimensions, offset, isListOrStringVector, isComplexOrRawVector, isQuoted);
        }

        private String printDimRecursive(RAbstractVector vector, byte isListOrStringVector, byte isComplexOrRawVector, int currentDimLevel, int arrayBase, int accDimensions, String header,
                        byte isQuoted) {
            if (dimPrinter == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                dimPrinter = insert(PrintDimNodeGen.create(null, null, null, null, null, null, null, null));
            }
            return (String) dimPrinter.executeString(vector, isListOrStringVector, isComplexOrRawVector, currentDimLevel, arrayBase, accDimensions, header, isQuoted);
        }

        @TruffleBoundary
        @Specialization
        protected String printDim(RAbstractVector vector, byte isListOrStringVector, byte isComplexOrRawVector, int currentDimLevel, int arrayBase, int accDimensions, String header, byte isQuoted) {
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
                    // Checkstyle: stop
                    sb.append(", , ");
                    // Checkstyle: resume
                    sb.append(getDimId(vector, currentDimLevel, dimInd, attrProfiles));
                    sb.append(", ");
                    sb.append(header);
                    sb.append("\n\n");
                    sb.append(printVector2Dim(vector, dimensionsVector, arrayBase + (dimInd * matrixSize), isListOrStringVector, isComplexOrRawVector, isQuoted));
                    sb.append("\n");
                    if ((arrayBase + (dimInd * matrixSize) + matrixSize) < vector.getLength() || vector.getLength() == 0) {
                        sb.append("\n");
                    }
                }
            } else {
                int newAccDimensions = accDimensions / dimSize;
                for (int dimInd = 0; dimInd < dimSize; dimInd++) {
                    int newArrayBase = arrayBase + newAccDimensions * dimInd;
                    String dimId = getDimId(vector, currentDimLevel, dimInd, attrProfiles);
                    String innerDims = printDimRecursive(vector, isListOrStringVector, isComplexOrRawVector, currentDimLevel - 1, newArrayBase, newAccDimensions, concat(dimId, ", ", header),
                                    isQuoted);
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
