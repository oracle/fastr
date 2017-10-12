/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.size;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

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
import com.oracle.truffle.r.nodes.attributes.UnaryCopyAttributesNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;

@RBuiltin(name = "iconv", kind = INTERNAL, parameterNames = {"x", "from", "to", "sub", "mark", "toRaw"}, behavior = PURE)
public abstract class IConv extends RBuiltinNode.Arg6 {

    static {
        Casts casts = new Casts(IConv.class);
        casts.arg("x").mustBe(stringValue(), RError.Message.NOT_CHARACTER_VECTOR, "x");
        // with default error message, NO_CALLER does not work
        casts.arg("from").defaultError(RError.Message.INVALID_ARGUMENT, "from").mustBe(stringValue()).asStringVector().mustBe(size(1)).findFirst();
        casts.arg("to").defaultError(RError.Message.INVALID_ARGUMENT, "to").mustBe(stringValue()).asStringVector().mustBe(size(1)).findFirst();
        casts.arg("sub").defaultError(RError.Message.INVALID_ARGUMENT, "sub").mustBe(stringValue()).asStringVector().mustBe(size(1)).findFirst();
        casts.arg("mark").asLogicalVector().findFirst(RRuntime.LOGICAL_FALSE).map(toBoolean());
        casts.arg("toRaw").asLogicalVector().findFirst(RRuntime.LOGICAL_FALSE).map(toBoolean());

    }

    @Specialization
    @TruffleBoundary
    protected RAbstractStringVector doIConv(RAbstractStringVector x, String from, String to, String sub, @SuppressWarnings("unused") boolean mark, boolean toRaw,
                    @Cached("create()") UnaryCopyAttributesNode copyAttributesNode) {

        if (toRaw) {
            throw RInternalError.unimplemented("iconv with toRaw=TRUE");
        }

        Charset fromCharset = getCharset(from, from, to);
        Charset toCharset = getCharset(to, from, to);
        boolean complete = x.isComplete();
        if (fromCharset == StandardCharsets.UTF_8 && toCharset == StandardCharsets.UTF_8) {
            // this conversion cannot change anything
            return x;
        } else {
            // simulate the results of charset conversion
            CharsetEncoder encoder = fromCharset.newEncoder();
            CharsetDecoder decoder = toCharset.newDecoder();
            if (RRuntime.isNA(sub)) {
                encoder.onUnmappableCharacter(CodingErrorAction.REPORT);
                decoder.onUnmappableCharacter(CodingErrorAction.REPORT);
                decoder.onMalformedInput(CodingErrorAction.REPORT);
            } else {
                decoder.replaceWith(sub);
                decoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
                decoder.onMalformedInput(CodingErrorAction.REPLACE);
                encoder.replaceWith(sub.getBytes(toCharset));
                encoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
            }
            int length = x.getLength();
            String[] data = new String[length];
            for (int i = 0; i < length; i++) {
                String value = x.getDataAt(i);
                if (!RRuntime.isNA(value)) {
                    try {
                        data[i] = decoder.decode(encoder.encode(CharBuffer.wrap(value))).toString();
                    } catch (CharacterCodingException e) {
                        complete = false;
                        data[i] = RRuntime.STRING_NA;
                    }
                }
            }
            RStringVector result = RDataFactory.createStringVector(data, complete);
            copyAttributesNode.execute(result, x);
            return result;
        }
    }

    private Charset getCharset(String name, String from, String to) {
        String toCharsetName = "".equals(name) ? LocaleFunctions.LC.CTYPE.getValue() : name;
        Charset toCharset;
        if ("C".equals(toCharsetName)) {
            toCharset = StandardCharsets.US_ASCII;
        } else {
            try {
                toCharset = Charset.forName(toCharsetName);
            } catch (IllegalCharsetNameException | UnsupportedCharsetException e) {
                throw error(Message.UNSUPPORTED_ENCODING_CONVERSION, from, to);
            }
        }
        return toCharset;
    }
}
