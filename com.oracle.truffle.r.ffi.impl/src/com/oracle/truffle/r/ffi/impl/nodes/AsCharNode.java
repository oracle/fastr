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
package com.oracle.truffle.r.ffi.impl.nodes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.r.nodes.unary.CastStringNode;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.data.RTypes;
import com.oracle.truffle.r.runtime.data.model.RAbstractAtomicVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.ffi.CharSXPWrapper;

@TypeSystemReference(RTypes.class)
public abstract class AsCharNode extends FFIUpCallNode.Arg1 {
    private static final CharSXPWrapper CharSXPWrapper_NA = CharSXPWrapper.create(RRuntime.STRING_NA);

    public abstract CharSXPWrapper execute(Object obj);

    @Specialization
    protected CharSXPWrapper asChar(CharSXPWrapper obj) {
        return obj;
    }

    @Specialization
    protected CharSXPWrapper asChar(RAbstractStringVector obj) {
        if (obj.getLength() == 0) {
            return CharSXPWrapper_NA;
        } else {
            return CharSXPWrapper.create(obj.getDataAt(0));
        }
    }

    @Specialization
    protected CharSXPWrapper asChar(RSymbol obj) {
        return CharSXPWrapper.create(obj.getName());
    }

    @Specialization(guards = "obj.getLength() > 0")
    protected CharSXPWrapper asChar(RAbstractAtomicVector obj, //
                    @Cached("createNonPreserving()") CastStringNode castStringNode) {
        Object castObj = castStringNode.executeString(obj);
        if (castObj instanceof String) {
            return CharSXPWrapper.create((String) castObj);
        } else if (castObj instanceof RAbstractStringVector) {
            return CharSXPWrapper.create(((RAbstractStringVector) castObj).getDataAt(0));
        } else {
            throw RInternalError.shouldNotReachHere();
        }
    }

    @Fallback
    protected CharSXPWrapper asCharFallback(@SuppressWarnings("unused") Object obj) {
        return CharSXPWrapper_NA;
    }

    public static AsCharNode create() {
        return AsCharNodeGen.create();
    }
}
