/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.asBoolean;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.asInteger;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.atomicLogicalValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.emptyStringVector;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.numericValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.singleElement;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;
import static com.oracle.truffle.r.runtime.RVisibility.OFF;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.IO;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import java.io.IOException;
import java.util.ArrayList;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.unary.ToStringNode;
import com.oracle.truffle.r.nodes.unary.ToStringNodeGen;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.conn.RConnection;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.REmpty;
import com.oracle.truffle.r.runtime.data.RExpression;
import com.oracle.truffle.r.runtime.data.RLanguage;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RTypedValue;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;

/**
 * The {@code cat .Internal}.
 */
@RBuiltin(name = "cat", visibility = OFF, kind = INTERNAL, parameterNames = {"arglist", "file", "sep", "fill", "labels", "append"}, behavior = IO)
public abstract class Cat extends RBuiltinNode.Arg6 {

    @Child private ToStringNode toString;

    private void ensureToString() {
        if (toString == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            toString = insert(ToStringNodeGen.create());
        }
    }

    static {
        Casts casts = new Casts(Cat.class);
        casts.arg("sep").mustBe(stringValue(), RError.Message.INVALID_SEP);

        casts.arg("file").defaultError(Message.INVALID_CONNECTION).mustNotBeNull().asIntegerVector().findFirst();

        casts.arg("fill").mustBe(numericValue()).asVector().mustBe(singleElement()).findFirst().mapIf(atomicLogicalValue(), asBoolean(), asInteger());

        casts.arg("labels").mapNull(emptyStringVector()).mustBe(stringValue()).asStringVector();

        // append is interpreted in the calling closure, but GnuR still checks for NA
        casts.arg("append").asLogicalVector().findFirst().map(toBoolean());
    }

    @Specialization
    @TruffleBoundary
    protected RNull cat(RList args, int file, RAbstractStringVector sepVec, boolean fill, RAbstractStringVector labels, boolean append) {
        int fillWidth = -1;
        if (fill) {
            fillWidth = RRuntime.asInteger(RContext.getInstance().stateROptions.getValue("width"));
        }
        return output(args, file, sepVec, fillWidth, labels, append);
    }

    @TruffleBoundary
    @Specialization
    protected RNull cat(RList args, int file, RAbstractStringVector sepVec, int givenFillWidth, RAbstractStringVector labels, boolean append) {
        int fillWidth = -1;
        if (givenFillWidth < 0) {
            warning(Message.NON_POSITIVE_FILL);
        } else if (givenFillWidth >= 1) {
            fillWidth = givenFillWidth;
        }
        return output(args, file, sepVec, fillWidth, labels, append);
    }

    @TruffleBoundary
    private RNull output(RList args, int file, RAbstractStringVector sepVec, int fillWidth, RAbstractStringVector labels, @SuppressWarnings("unused") boolean append) {
        for (int i = 0; i < args.getLength(); i++) {
            Object obj = args.getDataAt(i);
            if (obj == REmpty.instance) {
                // Note: we cannot easily get the name of the original argument, so we use a
                // different error message than GNUR.
                throw error(Message.MISSING_INVALID);
            }
        }

        RConnection conn = RConnection.fromIndex(file);
        boolean filling = fillWidth > 0;
        ensureToString();
        /*
         * cat converts its arguments to character vectors, concatenates them to a single character
         * vector, appends the given sep = string(s) to each element and then outputs them
         */
        int length = args.getLength();
        ArrayList<String> stringVecs = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            Object obj = args.getDataAt(i);
            if (obj instanceof RStringVector) {
                RStringVector stringVec = (RStringVector) obj;
                for (int j = 0; j < stringVec.getLength(); j++) {
                    stringVecs.add(stringVec.getDataAt(j));
                }
            } else if (obj == RNull.instance || obj == RMissing.instance) {
                continue;
            } else if (obj instanceof String) {
                stringVecs.add((String) obj);
            } else if (obj instanceof RAbstractContainer) {
                RAbstractContainer objVec = (RAbstractContainer) obj;
                // Empty containers produce no output, but they participate
                // in the sense that the sep value is appended
                if (objVec.getLength() == 0) {
                    stringVecs.add("");
                } else {
                    validateType(i + 1, obj);
                    for (int j = 0; j < objVec.getLength(); j++) {
                        stringVecs.add(toString.executeString(objVec.getDataAtAsObject(j), ""));
                    }
                }
            } else {
                stringVecs.add(toString.executeString(obj, ""));
            }
        }

        int sepLength = sepVec.getLength();
        int labelsLength = labels == null ? 0 : labels.getLength();

        boolean sepContainsNewline = sepContainsNewline(sepVec);
        StringBuilder sb = new StringBuilder();
        int fillCount = 0;
        int lineCount = 0;
        boolean lineStart = true;
        for (int i = 0; i < stringVecs.size(); i++) {
            String str = stringVecs.get(i);
            if (str != null) {
                String sep = sepVec.getDataAt(i % sepLength);
                int thisLength = str.length() + sep.length() + (lineStart && labelsLength > 0 ? labels.getDataAt(lineCount % labelsLength).length() + 1 : 0);
                if (filling && fillCount + thisLength > fillWidth) {
                    sb.append('\n');
                    fillCount = 0;
                    lineStart = true;
                }
                int startLength = sb.length();
                if (lineStart && labelsLength > 0) {
                    sb.append(labels.getDataAt(lineCount % labelsLength));
                    sb.append(' ');
                    lineCount++;
                }
                sb.append(str);
                if (i != stringVecs.size() - 1) {
                    sb.append(sep);
                    if (lineStart) {
                        lineStart = false;
                    }
                }
                fillCount += sb.length() - startLength;
            }
        }

        String data = sb.toString();
        if (filling || sepContainsNewline && stringVecs.size() > 0) {
            data = data + "\n";
        }
        try (RConnection openConn = conn.forceOpen("wt")) {
            openConn.writeLines(RDataFactory.createStringVectorFromScalar(data), "", false);
        } catch (IOException ex) {
            throw error(RError.Message.GENERIC, ex.getMessage());
        }

        return RNull.instance;
    }

    private void validateType(int argIndex, Object obj) {
        if (obj instanceof RList || obj instanceof RLanguage || obj instanceof RExpression) {
            RTypedValue rType = (RTypedValue) obj;
            throw error(Message.CAT_ARGUMENT_OF_TYPE, argIndex, rType.getRType().getName());
        }
    }

    @TruffleBoundary
    private static boolean zeroLength(Object obj) {
        if (obj instanceof RAbstractContainer) {
            return ((RAbstractContainer) obj).getLength() == 0;
        } else {
            return false;
        }
    }

    private static boolean sepContainsNewline(RAbstractStringVector sepVec) {
        for (int i = 0; i < sepVec.getLength(); i++) {
            if (sepVec.getDataAt(i).contains("\n")) {
                return true;
            }
        }
        return false;
    }
}
