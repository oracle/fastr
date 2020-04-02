/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.nodes.unary;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RS4Object;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.interop.ConvertForeignObjectNode;

@ImportStatic(RRuntime.class)
public abstract class CastToVectorNode extends CastNode {

    private final boolean preserveNonVector;

    protected CastToVectorNode(boolean preserveNonVector) {
        this.preserveNonVector = preserveNonVector;
    }

    public final boolean isPreserveNonVector() {
        return preserveNonVector;
    }

    @Specialization
    protected Object castNull(@SuppressWarnings("unused") RNull rnull) {
        if (preserveNonVector) {
            return RNull.instance;
        } else {
            return RDataFactory.createList();
        }
    }

    @Specialization
    protected Object castMissing(@SuppressWarnings("unused") RMissing missing) {
        if (preserveNonVector) {
            return RMissing.instance;
        } else {
            return RDataFactory.createList();
        }
    }

    @Specialization
    protected Object castFunction(RFunction f) {
        if (preserveNonVector) {
            return f;
        } else {
            return RDataFactory.createList();
        }
    }

    @Specialization
    protected RAbstractVector cast(RAbstractVector vector) {
        return vector;
    }

    @Specialization
    protected RAbstractVector cast(@SuppressWarnings("unused") RS4Object s4obj) {
        // TODO implement according to function 'R_getS4DataSlot' in 'attrib.c'
        throw error(RError.Message.CANNOT_COERCE_S4_TO_VECTOR);
    }

    @Specialization
    protected Object cast(REnvironment env) {
        if (preserveNonVector) {
            return env;
        } else {
            return RDataFactory.createList();
        }
    }

    @Specialization(guards = "isForeignObject(truffleObject)")
    protected Object castForeign(TruffleObject truffleObject,
                    @Cached("create()") ConvertForeignObjectNode convertForeign) {
        if (preserveNonVector) {
            return truffleObject;
        } else {
            Object o = convertForeign.convert(truffleObject);
            if (!RRuntime.isForeignObject(o)) {
                return o;
            }
        }
        throw error(RError.Message.CANNOT_COERCE_EXTERNAL_OBJECT_TO_VECTOR, "vector");
    }

    public static CastToVectorNode create() {
        return CastToVectorNodeGen.create(false);
    }
}
