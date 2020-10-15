/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.size;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.runtime.data.nodes.attributes.UnaryCopyAttributesNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RLocale;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

@RBuiltin(name = "iconv", kind = INTERNAL, parameterNames = {"x", "from", "to", "sub", "mark", "toRaw"}, behavior = PURE)
public abstract class IConv extends RBuiltinNode.Arg6 {

    static {
        Casts casts = new Casts(IConv.class);
        casts.arg("x").allowNull().mustBe(stringValue(), RError.Message.NOT_CHARACTER_VECTOR, "x");
        // with default error message, NO_CALLER does not work
        casts.arg("from").defaultError(RError.Message.INVALID_ARGUMENT, "from").mustBe(stringValue()).asStringVector().mustBe(size(1)).findFirst();
        casts.arg("to").defaultError(RError.Message.INVALID_ARGUMENT, "to").mustBe(stringValue()).asStringVector().mustBe(size(1)).findFirst();
        casts.arg("sub").defaultError(RError.Message.INVALID_ARGUMENT, "sub").mustBe(stringValue()).asStringVector().mustBe(size(1)).findFirst();
        casts.arg("mark").asLogicalVector().findFirst(RRuntime.LOGICAL_FALSE).map(toBoolean());
        casts.arg("toRaw").asLogicalVector().findFirst(RRuntime.LOGICAL_FALSE).map(toBoolean());

    }

    @Specialization
    protected Object listLocales(@SuppressWarnings("unused") RNull value, @SuppressWarnings("unused") String from, @SuppressWarnings("unused") String to, @SuppressWarnings("unused") String sub,
                    @SuppressWarnings("unused") boolean mark, @SuppressWarnings("unused") boolean toRaw) {
        // GNU-R internally abuses this builtin to also list locales, this does not seem to be
        // documented, but is used from "listiconv".
        // NOTE: for this case GNU-R would not validate the remaining arguments, hopefully no-one is
        // relying on that...
        // TODO: GNU-R has code that creates the list #ifdef HAVE_ICONVLIST, otherwise it also
        // returns NULL
        return RNull.instance;
    }

    @Specialization
    @TruffleBoundary
    protected Object doIConv(RStringVector x, String from, String to, String sub, @SuppressWarnings("unused") boolean mark, boolean toRaw,
                    @Cached("create()") UnaryCopyAttributesNode copyAttributesNode) {

        Charset fromCharset = getCharset(from, from, to);
        Charset toCharset = getCharset(to, from, to);
        boolean complete = x.isComplete();
        // simulate the results of charset conversion
        CharsetEncoder fromEncoder = fromCharset.newEncoder();
        CharsetEncoder toEncoder = toCharset.newEncoder();
        CharsetDecoder toDecoder = toCharset.newDecoder();
        if (RRuntime.isNA(sub)) {
            fromEncoder.onUnmappableCharacter(CodingErrorAction.REPORT);
            fromEncoder.onMalformedInput(CodingErrorAction.REPORT);
            toEncoder.onUnmappableCharacter(CodingErrorAction.REPORT);
            toEncoder.onMalformedInput(CodingErrorAction.REPORT);
            toDecoder.onUnmappableCharacter(CodingErrorAction.REPORT);
            toDecoder.onMalformedInput(CodingErrorAction.REPORT);
        } else if ("byte".equals(sub)) {
            // TODO: special mode that inserts <hexcode>
            fromEncoder.onUnmappableCharacter(CodingErrorAction.IGNORE);
            fromEncoder.onMalformedInput(CodingErrorAction.IGNORE);
            toEncoder.onUnmappableCharacter(CodingErrorAction.IGNORE);
            toEncoder.onMalformedInput(CodingErrorAction.IGNORE);
            toDecoder.onUnmappableCharacter(CodingErrorAction.IGNORE);
            toDecoder.onMalformedInput(CodingErrorAction.IGNORE);
        } else if (sub.isEmpty()) {
            fromEncoder.onUnmappableCharacter(CodingErrorAction.IGNORE);
            fromEncoder.onMalformedInput(CodingErrorAction.IGNORE);
            toEncoder.onUnmappableCharacter(CodingErrorAction.IGNORE);
            toEncoder.onMalformedInput(CodingErrorAction.IGNORE);
            toDecoder.onUnmappableCharacter(CodingErrorAction.IGNORE);
            toDecoder.onMalformedInput(CodingErrorAction.IGNORE);
        } else {
            // ignore encoding errors
            fromEncoder.onUnmappableCharacter(CodingErrorAction.IGNORE);
            fromEncoder.onMalformedInput(CodingErrorAction.IGNORE);
            toEncoder.onUnmappableCharacter(CodingErrorAction.IGNORE);
            toEncoder.onMalformedInput(CodingErrorAction.IGNORE);
            // TODO: support more than one character in "replacement"
            toEncoder.replaceWith(sub.substring(0, 1).getBytes());
            toDecoder.replaceWith(sub.substring(0, 1));
            toDecoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
            toDecoder.onMalformedInput(CodingErrorAction.REPLACE);
        }
        int length = x.getLength();
        String[] data = new String[length];
        for (int i = 0; i < length; i++) {
            String value = x.getDataAt(i);
            if (RRuntime.isNA(value)) {
                complete = false;
                data[i] = RRuntime.STRING_NA;
            } else {
                try {
                    data[i] = toEncoder.canEncode(value) ? value : toDecoder.decode(fromEncoder.encode(CharBuffer.wrap(value))).toString();
                } catch (CharacterCodingException e) {
                    complete = false;
                    data[i] = RRuntime.STRING_NA;
                }
            }
        }
        RAbstractVector result;
        if (toRaw) {
            Object[] listData = new Object[data.length];
            for (int i = 0; i < listData.length; i++) {
                if (RRuntime.isNA(data[i])) {
                    listData[i] = RNull.instance;
                } else {
                    try {
                        ByteBuffer buffer = toEncoder.encode(CharBuffer.wrap(data[i]));
                        byte[] bytes = new byte[buffer.remaining()];
                        buffer.get(bytes);
                        listData[i] = RDataFactory.createRawVector(bytes);
                    } catch (CharacterCodingException e) {
                        listData[i] = RNull.instance;
                    }
                }
            }
            result = RDataFactory.createList(listData);
        } else {
            result = RDataFactory.createStringVector(data, complete);
        }
        copyAttributesNode.execute(result, x);
        return result;
    }

    private Charset getCharset(String name, String from, String to) {
        if (name.isEmpty()) {
            return RContext.getInstance().stateRLocale.getCharset(RLocale.CTYPE);
        }
        Charset toCharset;
        if ("C".equals(name)) {
            toCharset = StandardCharsets.US_ASCII;
        } else {
            try {
                final int iconvFlagDelim = name.indexOf("//");
                String chsName = name;
                if (iconvFlagDelim > 0) {
                    chsName = name.substring(0, iconvFlagDelim);
                }
                toCharset = Charset.forName(chsName);
            } catch (IllegalCharsetNameException | UnsupportedCharsetException e) {
                throw error(Message.UNSUPPORTED_ENCODING_CONVERSION, from, to);
            }
        }
        return toCharset;
    }
}
