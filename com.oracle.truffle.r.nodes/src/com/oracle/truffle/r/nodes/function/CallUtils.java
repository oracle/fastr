/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.function;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxConstant;
import com.oracle.truffle.r.runtime.nodes.RSyntaxLookup;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

public final class CallUtils {

    private CallUtils() {
    }

    /**
     * Turns unevaluated field access argument, e.g. {@code foo} in {@code bar$foo}, into
     * corresponding string.
     */
    public static String unevaluatedArgAsFieldName(RNode callNode, RSyntaxNode argNode) {
        Object value = null;
        if (argNode instanceof RSyntaxLookup) {
            return ((RSyntaxLookup) argNode).getIdentifier();
        } else if (argNode instanceof RSyntaxConstant) {
            value = ((RSyntaxConstant) argNode).getValue();
            if (value instanceof String) {
                return (String) value;
            } else if (value instanceof RAbstractStringVector && ((RAbstractStringVector) value).getLength() == 1) {
                return ((RAbstractStringVector) value).getDataAt(0);
            }
        }
        throw raiseInvalidSubscript(callNode, value);
    }

    public static RError raiseInvalidSubscript(RNode callNode, Object nameObj) {
        CompilerDirectives.transferToInterpreter();
        String type = getPromiseValueType(nameObj);
        throw RError.error(callNode, RError.Message.INVALID_SUBSCRIPT_TYPE, type);
    }

    private static String getPromiseValueType(Object value) {
        if (value != null) {
            RType rType = RType.getRType(value);
            if (rType != null) {
                return rType.getName();
            }
        }
        return "language";
    }
}
