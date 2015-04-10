/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import java.io.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.conn.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

/**
 * The {@code cat .Internal}. TODO Implement the "fill", "labels" and "append" arguments. Open/close
 * unopen connections for the duration.
 */
@RBuiltin(name = "cat", kind = INTERNAL, parameterNames = {"arglist", "file", "sep", "fill", "labels", "append"})
public abstract class Cat extends RInvisibleBuiltinNode {

    @Child private ToStringNode toString;

    private void ensureToString() {
        if (toString == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            toString = insert(ToStringNodeGen.create(null, null, null));
        }
    }

    @Specialization
    protected RNull cat(VirtualFrame frame, RList args, RConnection conn, RAbstractStringVector sepVec, byte fill, @SuppressWarnings("unused") RNull labels, byte append) {
        if (RRuntime.fromLogical(fill) || RRuntime.fromLogical(append)) {
            throw RError.nyi(getEncapsulatingSourceSection(), "fill/append = TRUE");
        }
        ensureToString();
        String sep = sepVec.getDataAt(0);
        int length = args.getLength();

        String[] values = new String[length];
        for (int i = 0; i < length; i++) {
            Object obj = args.getDataAt(i);
            if (!(obj instanceof RNull)) {
                if (zeroLength(obj)) {
                    values[i] = "";
                } else {
                    values[i] = toString.executeString(frame, obj, false, sep);
                }
            }
        }

        output(conn, sepVec, values);
        controlVisibility();
        return RNull.instance;
    }

    @TruffleBoundary
    private void output(RConnection conn, RAbstractStringVector sepVec, String[] values) {
        int sepLength = sepVec.getLength();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < values.length; i++) {
            String str = values[i];
            if (str != null) {
                sb.append(str);
                if (i != values.length - 1) {
                    sb.append(sepVec.getDataAt(i % sepLength));
                }
            }
        }

        boolean sepContainsNewline = sepContainsNewline(sepVec);
        if (sepContainsNewline && values.length > 0) {
            sb.append('\n');
        }
        try {
            conn.writeLines(RDataFactory.createStringVectorFromScalar(sb.toString()), "");
        } catch (IOException ex) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.GENERIC, ex.getMessage());
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
