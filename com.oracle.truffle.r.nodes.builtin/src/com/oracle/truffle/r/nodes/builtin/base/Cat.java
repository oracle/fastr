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

import static com.oracle.truffle.r.runtime.RBuiltinKind.INTERNAL;

import java.io.IOException;
import java.util.ArrayList;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.unary.ToStringNode;
import com.oracle.truffle.r.nodes.unary.ToStringNodeGen;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RVisibility;
import com.oracle.truffle.r.runtime.conn.RConnection;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

/**
 * The {@code cat .Internal}.
 */
@RBuiltin(name = "cat", visibility = RVisibility.OFF, kind = INTERNAL, parameterNames = {"arglist", "file", "sep", "fill", "labels", "append"})
public abstract class Cat extends RBuiltinNode {

    @Child private ToStringNode toString;

    private void ensureToString() {
        if (toString == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            toString = insert(ToStringNodeGen.create());
        }
    }

    @Override
    protected void createCasts(CastBuilder casts) {
        casts.toLogical(5);
    }

    private void checkFillLength(RAbstractVector fill) throws RError {
        if (fill.getLength() > 1) {
            throw RError.error(this, RError.Message.INVALID_ARGUMENT, "fill");
        }
    }

    private RStringVector checkLabels(Object labels) throws RError {
        if (labels == RNull.instance) {
            return null;
        } else {
            if (labels instanceof String) {
                return RDataFactory.createStringVectorFromScalar((String) labels);
            } else if (labels instanceof RStringVector) {
                return (RStringVector) labels;
            } else {
                throw RError.error(this, RError.Message.INVALID_ARGUMENT, "labels");
            }
        }
    }

    public boolean numericFill(RAbstractVector fill) {
        return fill instanceof RIntVector || fill instanceof RDoubleVector;
    }

    @Specialization
    @TruffleBoundary
    protected RNull cat(RList args, RConnection conn, RAbstractStringVector sepVec, RAbstractLogicalVector fill, Object labels, byte append) {
        checkFillLength(fill);
        int fillWidth = -1;
        if (RRuntime.fromLogical(fill.getDataAt(0))) {
            fillWidth = RRuntime.asInteger(RContext.getInstance().stateROptions.getValue("width"));
        }
        return output(args, conn, sepVec, fillWidth, checkLabels(labels), append);
    }

    @Specialization(guards = "numericFill(fill)")
    @TruffleBoundary
    protected RNull cat(RList args, RConnection conn, RAbstractStringVector sepVec, RAbstractVector fill, Object labels, byte append) {
        checkFillLength(fill);
        int fillWidth = -1;
        int givenFillWidth = RRuntime.asInteger(fill);
        if (givenFillWidth < 1) {
            RError.warning(this, RError.Message.NON_POSITIVE_FILL);
        } else {
            fillWidth = givenFillWidth;
        }
        return output(args, conn, sepVec, fillWidth, checkLabels(labels), append);
    }

    @SuppressWarnings("unused")
    @Fallback
    @TruffleBoundary
    protected RNull cat(Object args, Object conn, Object sepVec, Object fillObj, Object labels, Object append) {
        throw RError.error(this, RError.Message.INVALID_OR_UNIMPLEMENTED_ARGUMENTS);
    }

    @TruffleBoundary
    private RNull output(RList args, RConnection conn, RAbstractStringVector sepVec, int fillWidth, RStringVector labels, byte append) {
        boolean filling = fillWidth > 0;
        // append is interpreted in the calling closure, but GnuR still checks for NA
        if (RRuntime.isNA(append)) {
            throw RError.error(this, RError.Message.INVALID_ARGUMENT, "append");
        }
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
            } else if (obj instanceof RNull) {
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
                    for (int j = 0; j < objVec.getLength(); j++) {
                        stringVecs.add(toString.executeString(objVec.getDataAtAsObject(j), false, ""));
                    }
                }
            } else {
                stringVecs.add(toString.executeString(obj, false, ""));
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
            throw RError.error(this, RError.Message.GENERIC, ex.getMessage());
        }

        return RNull.instance;
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
