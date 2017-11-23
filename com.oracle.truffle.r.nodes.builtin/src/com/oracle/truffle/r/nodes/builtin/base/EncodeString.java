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
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.attributes.UnaryCopyAttributesNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

@RBuiltin(name = "encodeString", kind = INTERNAL, parameterNames = {"x", "width", "quote", "justify", "na.encode"}, behavior = READS_STATE)
public abstract class EncodeString extends RBuiltinNode.Arg5 {

    private enum JUSTIFY {
        LEFT,
        RIGHT,
        CENTER,
        NONE;
    }

    private final NACheck na = NACheck.create();

    static {
        Casts casts = new Casts(EncodeString.class);
        casts.arg("x").mustBe(stringValue(), RError.Message.CHAR_VEC_ARGUMENT);

        casts.arg("width").asIntegerVector().findFirst().mustBe(intNA().or(gte0()));

        casts.arg("quote").mustBe(stringValue()).asStringVector().mustBe(singleElement()).findFirst();

        casts.arg("justify").asIntegerVector().findFirst().mustBe(gte(JUSTIFY.LEFT.ordinal()).and(lte(JUSTIFY.NONE.ordinal())));

        casts.arg("na.encode").asLogicalVector().findFirst(RRuntime.LOGICAL_FALSE).mustBe(notLogicalNA()).map(toBoolean());
    }

    private static StringBuilder append(String source, int offset, StringBuilder str, String snippet) {
        if (str == null) {
            StringBuilder newStr = new StringBuilder(source.length() + 2);
            newStr.append(source, 0, offset);
            return newStr.append(snippet);
        }
        return str.append(snippet);
    }

    @TruffleBoundary
    private static String encodeString(String value, char quote) {
        StringBuilder str = null;
        int offset = 0;
        while (offset < value.length()) {
            int codepoint = value.codePointAt(offset);
            switch (codepoint) {
                case '\n':
                    str = append(value, offset, str, "\\n");
                    break;
                case '\r':
                    str = append(value, offset, str, "\\r");
                    break;
                case '\t':
                    str = append(value, offset, str, "\\t");
                    break;
                case '\b':
                    str = append(value, offset, str, "\\b");
                    break;
                case 7:
                    str = append(value, offset, str, "\\a");
                    break;
                case '\f':
                    str = append(value, offset, str, "\\f");
                    break;
                case 11:
                    str = append(value, offset, str, "\\v");
                    break;
                case '\\':
                    str = append(value, offset, str, "\\\\");
                    break;
                case '"':
                    if (quote == '"') {
                        str = append(value, offset, str, "\\\"");
                    } else {
                        if (str != null) {
                            str.appendCodePoint(codepoint);
                        }
                    }
                    break;
                case '\'':
                    if (quote == '\'') {
                        str = append(value, offset, str, "\\'");
                    } else {
                        if (str != null) {
                            str.appendCodePoint(codepoint);
                        }
                    }
                    break;
                default:
                    if (codepoint < 32 || codepoint == 0x7f) {
                        str.append("\\").append(codepoint >>> 6).append((codepoint >>> 3) & 0x7).append(codepoint & 0x7);
                    } else if (codepoint > 64967) { // determined by experimentation
                        if (codepoint < 0x10000) {
                            str.append("\\u").append(String.format("%04x", codepoint));
                        } else {
                            str.append("\\U").append(String.format("%08x", codepoint));
                        }
                    } else {
                        if (str != null) {
                            str.appendCodePoint(codepoint);
                        }
                    }
                    break;
            }
            offset += Character.charCount(codepoint);
        }
        return str == null ? value : str.toString();
    }

    @TruffleBoundary
    private static String justifyAndQuote(int width, int justify, int quoteLength, char quoteChar, String currentEl) {
        String res;
        if (currentEl.length() + quoteLength >= width || justify == JUSTIFY.NONE.ordinal()) {
            if (quoteLength == 0) {
                res = currentEl;
            } else {
                res = new StringBuilder(currentEl.length() + quoteLength).append(quoteChar).append(currentEl).append(quoteChar).toString();
            }
        } else {
            StringBuilder str = new StringBuilder(width);
            int remainder = width - currentEl.length() - quoteLength;
            int before = justify == JUSTIFY.RIGHT.ordinal() ? remainder : justify == JUSTIFY.CENTER.ordinal() ? remainder / 2 : 0;
            int whitespaces = 0;
            for (; whitespaces < before; whitespaces++) {
                str.append(' ');
            }
            if (quoteLength > 0) {
                str.append(quoteChar);
            }
            str.append(currentEl);
            if (quoteLength > 0) {
                str.append(quoteChar);
            }
            for (; whitespaces < remainder; whitespaces++) {
                str.append(' ');
            }
            res = str.toString();
        }
        return res;
    }

    @Specialization
    protected RStringVector encodeStringLeftJustifyEncodeNA(RAbstractStringVector x, int width, final String quoteEl, int justify, boolean encodeNA,
                    @Cached("create()") UnaryCopyAttributesNode copyAttributesNode,
                    @Cached("createBinaryProfile()") ConditionProfile widthNAProfile,
                    @Cached("createBinaryProfile()") ConditionProfile encodeNAProfile) {
        int quoteLength = quoteEl.isEmpty() ? 0 : 2; // only the first char of quoteEl is used
        char quoteChar = quoteEl.isEmpty() ? 0 : quoteEl.charAt(0);
        String[] result = new String[x.getLength()];
        na.enable(x);
        for (int i = 0; i < x.getLength(); i++) {
            String currentEl = x.getDataAt(i);
            if (na.check(currentEl)) {
                result[i] = RRuntime.STRING_NA;
            } else {
                result[i] = encodeString(currentEl, quoteChar);
            }
        }
        int maxWidth;
        if (widthNAProfile.profile(RRuntime.isNA(width))) {
            maxWidth = 0;
            for (int i = 0; i < x.getLength(); i++) {
                int w;
                if (na.check(result[i])) {
                    w = encodeNA ? 2 + quoteLength : 0;
                } else {
                    w = result[i].length() + quoteLength;
                }
                maxWidth = Math.max(maxWidth, w);
            }
        } else {
            maxWidth = width;
        }
        boolean complete = true;
        for (int i = 0; i < x.getLength(); i++) {
            String currentEl = result[i];
            if (na.check(currentEl)) {
                if (encodeNAProfile.profile(encodeNA)) {
                    result[i] = justifyAndQuote(maxWidth, justify, 0, (char) 0, quoteLength == 0 ? "<NA>" : "NA");
                } else {
                    result[i] = RRuntime.STRING_NA;
                    complete = false;
                }
            } else {
                result[i] = justifyAndQuote(maxWidth, justify, quoteLength, quoteChar, currentEl);
            }
        }
        RStringVector resultVector = RDataFactory.createStringVector(result, complete);
        copyAttributesNode.execute(resultVector, x);
        return resultVector;
    }
}
