/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess.SequentialIterator;
import com.oracle.truffle.r.runtime.data.nodes.VectorReuse;

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

    @Specialization(guards = "vectorReuse.supports(values)")
    RAbstractStringVector doIt(String oldStr, String newStr, RAbstractStringVector values,
                    @Cached("createTemporary(values)") VectorReuse vectorReuse,
                    @Cached("create()") RemoveRegAttributesNode removeRegAttributesNode) {
        if (newStr.length() < oldStr.length()) {
            throw error(X_LONGER_THAN_Y, "old", "new");
        }
        RAbstractStringVector result = vectorReuse.getResult(values);
        VectorAccess resultAccess = vectorReuse.access(result);
        removeRegAttributesNode.execute(result);
        try (SequentialIterator readIter = resultAccess.access(result); SequentialIterator writeIter = resultAccess.access(result)) {
            while (resultAccess.next(readIter)) {
                resultAccess.next(writeIter);
                String value = resultAccess.getString(readIter);
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
                resultAccess.setString(writeIter, value);
            }
        }
        return result;
    }

    @Specialization(replaces = "doIt")
    RAbstractStringVector doItGeneric(String oldStr, String newStr, RAbstractStringVector values,
                    @Cached("createTemporaryGeneric()") VectorReuse vectorReuse,
                    @Cached("create()") RemoveRegAttributesNode removeRegAttributesNode) {
        return doIt(oldStr, newStr, values, vectorReuse, removeRegAttributesNode);
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
        String replacedValue = value;
        for (int rangeIdx = 0; rangeIdx <= oldEnd - oldStart; rangeIdx++) {
            replacedValue = value.replace((char) (oldStart + rangeIdx), (char) (newStart + rangeIdx));
        }
        return replacedValue;
    }
}
