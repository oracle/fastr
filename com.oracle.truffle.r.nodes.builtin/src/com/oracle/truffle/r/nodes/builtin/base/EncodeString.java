/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.gte;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.gte0;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.intNA;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.lte;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.notLogicalNA;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.singleElement;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.READS_STATE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

@RBuiltin(name = "encodeString", kind = INTERNAL, parameterNames = {"x", "width", "quote", "justify", "na.encode"}, behavior = READS_STATE)
public abstract class EncodeString extends RBuiltinNode {

    private enum JUSTIFY {
        LEFT,
        RIGHT,
        CENTER,
        NONE;
    }

    private final NACheck na = NACheck.create();
    private final BranchProfile everSeenNA = BranchProfile.create();

    @Override
    protected void createCasts(CastBuilder casts) {
        casts.arg("x").mustBe(stringValue(), RError.Message.CHAR_VEC_ARGUMENT);

        casts.arg("width").asIntegerVector().findFirst().mustBe(intNA().or(gte0()));

        casts.arg("quote").mustBe(stringValue()).asStringVector().mustBe(singleElement()).findFirst();

        casts.arg("justify").asIntegerVector().findFirst().mustBe(gte(JUSTIFY.LEFT.ordinal()).and(lte(JUSTIFY.NONE.ordinal())));

        casts.arg("na.encode").asLogicalVector().findFirst(RRuntime.LOGICAL_FALSE).mustBe(notLogicalNA()).map(toBoolean());
    }

    private int computeWidth(RAbstractStringVector x, int width, final String quote) {
        if (!RRuntime.isNA(width)) {
            return width == 0 ? 1 : width;
        }
        int maxElWidth = -1;
        na.enable(x);
        // Find the element in x with the largest width.
        for (int i = 0; i < x.getLength(); i++) {
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

    @SuppressWarnings("unused")
    @Specialization(guards = {"leftJustify(justify)", "encodeNA"})
    protected RStringVector encodeStringLeftJustifyEncodeNA(RAbstractStringVector x, int width, final String quoteEl, RAbstractIntVector justify, boolean encodeNA) {
        final int maxElWidth = computeWidth(x, width, quoteEl);
        final String[] result = new String[x.getLength()];
        na.enable(x);
        for (int i = 0; i < x.getLength(); i++) {
            String currentEl = x.getDataAt(i);
            if (na.check(currentEl)) {
                everSeenNA.enter();
                if (quoteEl.isEmpty()) {
                    currentEl = concat("<", currentEl, ">");
                }
                result[i] = Utils.stringFormat(concat("%-", maxElWidth, "s"), currentEl);
            } else {
                result[i] = Utils.stringFormat(concat("%-", maxElWidth, "s"), concat(quoteEl, currentEl, quoteEl));
            }
        }
        return RDataFactory.createStringVector(result, RDataFactory.COMPLETE_VECTOR);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"leftJustify(justify)", "!encodeNA"})
    protected RStringVector encodeStringLeftJustify(RAbstractStringVector x, int width, final String quoteEl, RAbstractIntVector justify, boolean encodeNA) {
        final int maxElWidth = computeWidth(x, width, quoteEl);
        final String[] result = new String[x.getLength()];
        na.enable(x);
        boolean seenNA = false;
        for (int i = 0; i < x.getLength(); i++) {
            final String currentEl = x.getDataAt(i);
            if (na.check(currentEl)) {
                everSeenNA.enter();
                result[i] = currentEl;
                seenNA = true;
            } else {
                result[i] = Utils.stringFormat(concat("%-", maxElWidth, "s"), concat(quoteEl, currentEl, quoteEl));
            }
        }
        return RDataFactory.createStringVector(result, !seenNA);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"rightJustify(justify)", "encodeNA"})
    protected RStringVector encodeStringRightJustifyEncodeNA(RAbstractStringVector x, int width, final String quoteEl, RAbstractIntVector justify, boolean encodeNA) {
        final int maxElWidth = computeWidth(x, width, quoteEl);
        final String[] result = new String[x.getLength()];
        na.enable(x);
        for (int i = 0; i < x.getLength(); i++) {
            String currentEl = x.getDataAt(i);
            if (na.check(currentEl)) {
                everSeenNA.enter();
                if (quoteEl.isEmpty()) {
                    currentEl = concat("<", currentEl, ">");
                }
                result[i] = Utils.stringFormat(concat("%", maxElWidth, "s"), currentEl);
            } else {
                result[i] = Utils.stringFormat(concat("%", maxElWidth, "s"), concat(quoteEl, currentEl, quoteEl));
            }
        }
        return RDataFactory.createStringVector(result, RDataFactory.COMPLETE_VECTOR);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"rightJustify(justify)", "!encodeNA"})
    protected RStringVector encodeStringRightJustify(RAbstractStringVector x, int width, final String quoteEl, RAbstractIntVector justify, boolean encodeNA) {
        final int maxElWidth = computeWidth(x, width, quoteEl);
        final String[] result = new String[x.getLength()];
        na.enable(x);
        boolean seenNA = false;
        for (int i = 0; i < x.getLength(); i++) {
            final String currentEl = x.getDataAt(i);
            if (na.check(currentEl)) {
                everSeenNA.enter();
                result[i] = currentEl;
                seenNA = true;
            } else {
                result[i] = Utils.stringFormat(concat("%", maxElWidth, "s"), concat(quoteEl, currentEl, quoteEl));
            }
        }
        return RDataFactory.createStringVector(result, !seenNA);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"centerJustify(justify)", "encodeNA"})
    protected RStringVector encodeStringCenterJustifyEncodeNA(RAbstractStringVector x, int width, String quoteEl, RAbstractIntVector justify, boolean encodeNA) {
        final int maxElWidth = computeWidth(x, width, quoteEl);
        final String[] result = new String[x.getLength()];
        final int quoteLength = quoteEl.length() > 0 ? 2 : 0;
        final int padding = maxElWidth - quoteLength;

        na.enable(x);
        for (int i = 0; i < x.getLength(); i++) {
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
    @Specialization(guards = {"centerJustify(justify)", "!encodeNA"})
    protected RStringVector encodeStringCenterJustify(RAbstractStringVector x, int width, final String quoteEl, RAbstractIntVector justify, boolean encodeNA) {
        final int maxElWidth = computeWidth(x, width, quoteEl);
        final String[] result = new String[x.getLength()];
        final int quoteLength = quoteEl.length() > 0 ? 2 : 0;
        final int padding = maxElWidth - quoteLength;
        na.enable(x);
        boolean seenNA = false;
        for (int i = 0; i < x.getLength(); i++) {
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
        for (int j = 0; j < leftPadding; j++) {
            sb.append(" ");
        }
        sb.append(quoteEl);
        sb.append(el);
        sb.append(quoteEl);
        for (int j = 0; j < rightPadding; j++) {
            sb.append(" ");
        }
        return sb.toString();
    }

    @TruffleBoundary
    private String addPadding(final String el, final int leftPadding, final int rightPadding, final String quoteEl) {
        final StringBuffer sb = new StringBuffer();
        for (int j = 0; j < leftPadding; j++) {
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
        for (int j = 0; j < rightPadding; j++) {
            sb.append(" ");
        }
        return sb.toString();
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"noJustify(width, justify)", "encodeNA"})
    protected RStringVector encodeStringNoJustifyEncodeNA(RAbstractStringVector x, int width, String quoteEl, RAbstractIntVector justify, boolean encodeNA) {
        final String[] result = new String[x.getLength()];
        na.enable(x);
        for (int i = 0; i < x.getLength(); i++) {
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
    @Specialization(guards = {"noJustify(width, justify)", "!encodeNA"})
    protected RStringVector encodeStringNoJustify(RAbstractStringVector x, int width, final String quoteEl, RAbstractIntVector justify, boolean encodeNA) {
        final String[] result = new String[x.getLength()];
        na.enable(x);
        boolean seenNA = false;
        for (int i = 0; i < x.getLength(); i++) {
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

    protected boolean leftJustify(RAbstractIntVector justify) {
        return justify.getDataAt(0) == JUSTIFY.LEFT.ordinal();
    }

    protected boolean rightJustify(RAbstractIntVector justify) {
        return justify.getDataAt(0) == JUSTIFY.RIGHT.ordinal();
    }

    protected boolean centerJustify(RAbstractIntVector justify) {
        return justify.getDataAt(0) == JUSTIFY.CENTER.ordinal();
    }

    protected boolean noJustify(int width, RAbstractIntVector justify) {
        return justify.getDataAt(0) == JUSTIFY.NONE.ordinal() || width == 0;
    }
}
