/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.singleElement;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.runtime.RError.Message.X_LONGER_THAN_Y;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.attributes.RemoveRegAttributesNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.function.opt.ReuseTemporaryNode;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;

@RBuiltin(name = "chartr", kind = INTERNAL, parameterNames = {"old", "new", "x"}, behavior = PURE)
public abstract class CharTr extends RBuiltinNode.Arg3 {

    static {
        Casts casts = new Casts(CharTr.class);
        casts.arg("old").mustBe(stringValue()).asStringVector().shouldBe(singleElement(), Message.ARGUMENT_ONLY_FIRST, "old").findFirst();
        casts.arg("new").mustBe(stringValue()).asStringVector().shouldBe(singleElement(), Message.ARGUMENT_ONLY_FIRST, "new").findFirst();
        casts.arg("x").mustBe(stringValue()).asStringVector();
    }

    public static CharTr create() {
        return CharTrNodeGen.create();
    }

    @Specialization
    RStringVector doIt(String oldStr, String newStr, RAbstractStringVector values,
                    @Cached("create()") RemoveRegAttributesNode removeRegAttributesNode,
                    @Cached("create()") ReuseTemporaryNode reuseTemporaryNode) {
        if (newStr.length() < oldStr.length()) {
            throw error(X_LONGER_THAN_Y, "old", "new");
        }
        RStringVector result = (RStringVector) reuseTemporaryNode.execute(values);
        removeRegAttributesNode.execute(result);
        Object store = result.getInternalStore();
        for (int i = 0; i < result.getLength(); i++) {
            String value = result.getDataAt(store, i);
            if (RRuntime.isNA(value)) {
                continue;
            }
            int replaceIdx = 0;
            while (replaceIdx < oldStr.length()) {
                if (replaceIdx + 2 < oldStr.length() && oldStr.charAt(replaceIdx + 1) == '-') {
                    value = replaceRange(replaceIdx, oldStr, newStr, value);
                    replaceIdx += 3;
                } else {
                    value = value.replace(oldStr.charAt(replaceIdx), newStr.charAt(replaceIdx));
                    replaceIdx++;
                }
            }
            result.setDataAt(store, i, value);
        }
        return result;
    }

    private String replaceRange(int replaceIdx, String oldStr, String newStr, String value) {
        if (replaceIdx + 2 >= newStr.length() || newStr.charAt(replaceIdx + 1) != '-') {
            throw error(X_LONGER_THAN_Y, "old", "new");
        }
        int oldEnd = oldStr.charAt(replaceIdx + 2);
        int oldStart = oldStr.charAt(replaceIdx);
        int newStart = newStr.charAt(replaceIdx);
        int newEnd = newStr.charAt(replaceIdx + 2);
        if (newEnd - newStart < oldEnd - oldStart) {
            throw error(X_LONGER_THAN_Y, "old", "new");
        }
        for (int rangeIdx = 0; rangeIdx <= oldEnd - oldStart; rangeIdx++) {
            value = value.replace((char) (oldStart + rangeIdx), (char) (newStart + rangeIdx));
        }
        return value;
    }
}
