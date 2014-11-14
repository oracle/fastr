/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.ops.na.*;

@RBuiltin(name = "encodeString", kind = RBuiltinKind.INTERNAL, parameterNames = {"x", "width", "quote", "justify", "na.encode"})
public abstract class EncodeString extends RBuiltinNode {

    private static enum JUSTIFY {
        LEFT,
        RIGHT,
        CENTER,
        NONE;
    }

    private final NACheck na = NACheck.create();
    private final BranchProfile everSeenNA = BranchProfile.create();

    @Override
    public RNode[] getParameterValues() {
        return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(0), ConstantNode.create(""), ConstantNode.create("left"), ConstantNode.create(RRuntime.LOGICAL_TRUE)};
    }

    @CreateCast("arguments")
    public RNode[] castArguments(RNode[] arguments) {
        arguments[1] = CastIntegerNodeFactory.create(arguments[1], true, false, false);
        arguments[3] = CastIntegerNodeFactory.create(arguments[3], true, false, false);
        arguments[4] = CastLogicalNodeFactory.create(arguments[4], true, false, false);
        return arguments;
    }

    private int computeWidth(RAbstractStringVector x, int width, final String quote) {
        if (!RRuntime.isNA(width)) {
            return width == 0 ? 1 : width;
        }
        int maxElWidth = -1;
        na.enable(x);
        // Find the element in x with the largest width.
        for (int i = 0; i < x.getLength(); ++i) {
            if (!na.check(x.getDataAt(i))) {
                int curLen = x.getDataAt(i).length();
                if (maxElWidth < curLen) {
                    maxElWidth = curLen;
                }
            }
        }
        maxElWidth += quote.length() > 0 ? 2 : 0; // Accounting for the quote.
        if (!RRuntime.isNA(width) && width > maxElWidth) {
            maxElWidth = width;
        }
        return maxElWidth;
    }

    @TruffleBoundary
    private static String concat(Object... args) {
        StringBuffer sb = new StringBuffer();
        for (Object arg : args) {
            sb.append(arg);
        }
        return sb.toString();
    }

    @TruffleBoundary
    private static String format(final String format, final String arg) {
        return String.format(format, arg);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"isValidWidth", "leftJustify", "isEncodeNA"})
    protected RStringVector encodeStringLeftJustifyEncodeNA(RAbstractStringVector x, int width, RAbstractStringVector quote, RAbstractIntVector justify, byte encodeNA) {
        final String quoteEl = quote.getDataAt(0);
        final int maxElWidth = computeWidth(x, width, quoteEl);
        final String[] result = new String[x.getLength()];
        na.enable(x);
        for (int i = 0; i < x.getLength(); ++i) {
            String currentEl = x.getDataAt(i);
            if (na.check(currentEl)) {
                everSeenNA.enter();
                if (quoteEl.isEmpty()) {
                    currentEl = concat("<", currentEl, ">");
                }
                result[i] = format(concat("%-", maxElWidth, "s"), currentEl);
            } else {
                result[i] = format(concat("%-", maxElWidth, "s"), concat(quoteEl, currentEl, quoteEl));
            }
        }
        return RDataFactory.createStringVector(result, RDataFactory.COMPLETE_VECTOR);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"isValidWidth", "leftJustify", "!isEncodeNA"})
    protected RStringVector encodeStringLeftJustify(RAbstractStringVector x, int width, RAbstractStringVector quote, RAbstractIntVector justify, byte encodeNA) {
        final String quoteEl = quote.getDataAt(0);
        final int maxElWidth = computeWidth(x, width, quoteEl);
        final String[] result = new String[x.getLength()];
        na.enable(x);
        boolean seenNA = false;
        for (int i = 0; i < x.getLength(); ++i) {
            final String currentEl = x.getDataAt(i);
            if (na.check(currentEl)) {
                everSeenNA.enter();
                result[i] = currentEl;
                seenNA = true;
            } else {
                result[i] = format(concat("%-", maxElWidth, "s"), concat(quoteEl, currentEl, quoteEl));
            }
        }
        return RDataFactory.createStringVector(result, !seenNA);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"isValidWidth", "rightJustify", "isEncodeNA"})
    protected RStringVector encodeStringRightJustifyEncodeNA(RAbstractStringVector x, int width, RAbstractStringVector quote, RAbstractIntVector justify, byte encodeNA) {
        final String quoteEl = quote.getDataAt(0);
        final int maxElWidth = computeWidth(x, width, quoteEl);
        final String[] result = new String[x.getLength()];
        na.enable(x);
        for (int i = 0; i < x.getLength(); ++i) {
            String currentEl = x.getDataAt(i);
            if (na.check(currentEl)) {
                everSeenNA.enter();
                if (quoteEl.isEmpty()) {
                    currentEl = concat("<", currentEl, ">");
                }
                result[i] = format(concat("%", maxElWidth, "s"), currentEl);
            } else {
                result[i] = format(concat("%", maxElWidth, "s"), concat(quoteEl, currentEl, quoteEl));
            }
        }
        return RDataFactory.createStringVector(result, RDataFactory.COMPLETE_VECTOR);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"isValidWidth", "rightJustify", "!isEncodeNA"})
    protected RStringVector encodeStringRightJustify(RAbstractStringVector x, int width, RAbstractStringVector quote, RAbstractIntVector justify, byte encodeNA) {
        final String quoteEl = quote.getDataAt(0);
        final int maxElWidth = computeWidth(x, width, quoteEl);
        final String[] result = new String[x.getLength()];
        na.enable(x);
        boolean seenNA = false;
        for (int i = 0; i < x.getLength(); ++i) {
            final String currentEl = x.getDataAt(i);
            if (na.check(currentEl)) {
                everSeenNA.enter();
                result[i] = currentEl;
                seenNA = true;
            } else {
                result[i] = format(concat("%", maxElWidth, "s"), concat(quoteEl, currentEl, quoteEl));
            }
        }
        return RDataFactory.createStringVector(result, !seenNA);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"isValidWidth", "centerJustify", "isEncodeNA"})
    protected RStringVector encodeStringCenterJustifyEncodeNA(RAbstractStringVector x, int width, RAbstractStringVector quote, RAbstractIntVector justify, byte encodeNA) {
        final String quoteEl = quote.getDataAt(0);
        final int maxElWidth = computeWidth(x, width, quoteEl);
        final String[] result = new String[x.getLength()];
        final int quoteLength = quoteEl.length() > 0 ? 2 : 0;
        final int padding = maxElWidth - quoteLength;

        na.enable(x);
        for (int i = 0; i < x.getLength(); ++i) {
            final String currentEl = x.getDataAt(i);
            int totalPadding = padding - currentEl.length();
            if (na.check(currentEl)) {
                everSeenNA.enter();
                if (quoteEl.isEmpty()) {
                    // Accounting for <> in <NA>
                    totalPadding -= 2;
                } else {
                    totalPadding += quoteLength;
                }
            }
            final int leftPadding = totalPadding >> 1;
            final int rightPadding = totalPadding - leftPadding;
            result[i] = addPadding(currentEl, leftPadding, rightPadding, quoteEl);
        }
        return RDataFactory.createStringVector(result, RDataFactory.COMPLETE_VECTOR);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"isValidWidth", "centerJustify", "!isEncodeNA"})
    protected RStringVector encodeStringCenterJustify(RAbstractStringVector x, int width, RAbstractStringVector quote, RAbstractIntVector justify, byte encodeNA) {
        final String quoteEl = quote.getDataAt(0);
        final int maxElWidth = computeWidth(x, width, quoteEl);
        final String[] result = new String[x.getLength()];
        final int quoteLength = quoteEl.length() > 0 ? 2 : 0;
        final int padding = maxElWidth - quoteLength;
        na.enable(x);
        boolean seenNA = false;
        for (int i = 0; i < x.getLength(); ++i) {
            final String currentEl = x.getDataAt(i);
            if (na.check(currentEl)) {
                everSeenNA.enter();
                result[i] = currentEl;
                seenNA = true;
            } else {
                final int totalPadding = padding - currentEl.length();
                final int leftPadding = totalPadding >> 1;
                final int rightPadding = totalPadding - leftPadding;
                result[i] = addPaddingIgnoreNA(currentEl, leftPadding, rightPadding, quoteEl);
            }
        }
        return RDataFactory.createStringVector(result, !seenNA);
    }

    @TruffleBoundary
    private static String addPaddingIgnoreNA(final String el, final int leftPadding, final int rightPadding, final String quoteEl) {
        final StringBuffer sb = new StringBuffer();
        for (int j = 0; j < leftPadding; ++j) {
            sb.append(" ");
        }
        sb.append(quoteEl);
        sb.append(el);
        sb.append(quoteEl);
        for (int j = 0; j < rightPadding; ++j) {
            sb.append(" ");
        }
        return sb.toString();
    }

    @TruffleBoundary
    private String addPadding(final String el, final int leftPadding, final int rightPadding, final String quoteEl) {
        final StringBuffer sb = new StringBuffer();
        for (int j = 0; j < leftPadding; ++j) {
            sb.append(" ");
        }
        if (RRuntime.isNA(el)) {
            everSeenNA.enter();
            if (quoteEl.isEmpty()) {
                sb.append("<");
                sb.append(el);
                sb.append(">");
            } else {
                sb.append(el);
            }
        } else {
            sb.append(quoteEl);
            sb.append(el);
            sb.append(quoteEl);
        }
        for (int j = 0; j < rightPadding; ++j) {
            sb.append(" ");
        }
        return sb.toString();
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"isValidWidth", "noJustify", "isEncodeNA"})
    protected RStringVector encodeStringNoJustifyEncodeNA(RAbstractStringVector x, int width, RAbstractStringVector quote, RAbstractIntVector justify, byte encodeNA) {
        final String quoteEl = quote.getDataAt(0);
        final String[] result = new String[x.getLength()];
        na.enable(x);
        for (int i = 0; i < x.getLength(); ++i) {
            final String currentEl = x.getDataAt(i);
            if (na.check(currentEl)) {
                everSeenNA.enter();
                result[i] = new String(currentEl);
            } else {
                result[i] = concat(quoteEl, currentEl, quoteEl);
            }
        }
        return RDataFactory.createStringVector(result, RDataFactory.COMPLETE_VECTOR);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"isValidWidth", "noJustify", "!isEncodeNA"})
    protected RStringVector encodeStringNoJustify(RAbstractStringVector x, int width, RAbstractStringVector quote, RAbstractIntVector justify, byte encodeNA) {
        final String quoteEl = quote.getDataAt(0);
        final String[] result = new String[x.getLength()];
        na.enable(x);
        boolean seenNA = false;
        for (int i = 0; i < x.getLength(); ++i) {
            final String currentEl = x.getDataAt(i);
            if (na.check(currentEl)) {
                everSeenNA.enter();
                result[i] = currentEl;
                seenNA = true;
            } else {
                result[i] = concat(quoteEl, currentEl, quoteEl);
            }
        }
        return RDataFactory.createStringVector(result, !seenNA);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "!isString")
    protected RStringVector encodeStringInvalidFirstArgument(Object x, int width, RAbstractStringVector quote, RAbstractIntVector justify, byte encodeNA) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.CHAR_VEC_ARGUMENT);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "!isValidWidth")
    protected RStringVector encodeStringInvalidWidth(RAbstractStringVector x, int width, RAbstractStringVector quote, RAbstractIntVector justify, byte encodeNA) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_VALUE, "width");
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "!isValidQuote")
    protected RStringVector encodeStringInvalidQuote(RAbstractStringVector x, int width, Object quote, RAbstractIntVector justify, byte encodeNA) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_VALUE, "quote");
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "!isValidJustify")
    protected RStringVector encodeStringInvalidJustify(RAbstractStringVector x, int width, RAbstractStringVector quote, RAbstractIntVector justify, byte encodeNA) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_VALUE, "justify");
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "!isValidEncodeNA")
    protected RStringVector encodeStringInvalidEncodeNA(RAbstractStringVector x, int width, RAbstractStringVector quote, RAbstractIntVector justify, byte encodeNA) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_VALUE, "na.encode");
    }

    @SuppressWarnings("unused")
    protected boolean isString(Object x, int width, RAbstractStringVector quote, RAbstractIntVector justify, byte encodeNA) {
        return x.getClass() == String.class || x.getClass() == RStringVector.class;
    }

    @SuppressWarnings("unused")
    protected boolean isValidWidth(RAbstractStringVector x, int width, RAbstractStringVector quote, RAbstractIntVector justify, byte encodeNA) {
        return RRuntime.isNA(width) || width >= 0;
    }

    @SuppressWarnings("unused")
    protected boolean isValidQuote(RAbstractStringVector x, int width, Object quote, RAbstractIntVector justify, byte encodeNA) {
        return quote.getClass() == String.class || (quote.getClass() == RStringVector.class && ((RStringVector) quote).getLength() == 1);
    }

    @SuppressWarnings("unused")
    protected boolean isValidJustify(RAbstractStringVector x, int width, RAbstractStringVector quote, RAbstractIntVector justify, byte encodeNA) {
        return justify.getDataAt(0) >= JUSTIFY.LEFT.ordinal() && justify.getDataAt(0) <= JUSTIFY.NONE.ordinal();
    }

    @SuppressWarnings("unused")
    protected boolean isValidEncodeNA(RAbstractStringVector x, int width, RAbstractStringVector quote, RAbstractIntVector justify, byte encodeNA) {
        return !RRuntime.isNA(encodeNA);
    }

    @SuppressWarnings("unused")
    protected boolean isEncodeNA(RAbstractStringVector x, int width, RAbstractStringVector quote, RAbstractIntVector justify, byte encodeNA) {
        return RRuntime.fromLogical(encodeNA);
    }

    @SuppressWarnings("unused")
    protected boolean leftJustify(RAbstractStringVector x, int width, RAbstractStringVector quote, RAbstractIntVector justify, byte encodeNA) {
        return justify.getDataAt(0) == JUSTIFY.LEFT.ordinal();
    }

    @SuppressWarnings("unused")
    protected boolean rightJustify(RAbstractStringVector x, int width, RAbstractStringVector quote, RAbstractIntVector justify, byte encodeNA) {
        return justify.getDataAt(0) == JUSTIFY.RIGHT.ordinal();
    }

    @SuppressWarnings("unused")
    protected boolean centerJustify(RAbstractStringVector x, int width, RAbstractStringVector quote, RAbstractIntVector justify, byte encodeNA) {
        return justify.getDataAt(0) == JUSTIFY.CENTER.ordinal();
    }

    @SuppressWarnings("unused")
    protected boolean noJustify(RAbstractStringVector x, int width, RAbstractStringVector quote, RAbstractIntVector justify, byte encodeNA) {
        return justify.getDataAt(0) == JUSTIFY.NONE.ordinal() || width == 0;
    }
}
