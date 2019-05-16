/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.missingValue;
import static com.oracle.truffle.r.runtime.RDispatch.INTERNAL_GENERIC;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.foreign;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.unary.CastRawNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RRawVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractRawVector;
import com.oracle.truffle.r.runtime.data.nodes.VectorReuse;
import com.oracle.truffle.r.runtime.interop.ConvertForeignObjectNode;

@RBuiltin(name = "as.raw", kind = PRIMITIVE, dispatch = INTERNAL_GENERIC, parameterNames = {"x"}, behavior = PURE)
@ImportStatic(RRuntime.class)
public abstract class AsRaw extends RBuiltinNode.Arg1 {

    static {
        Casts casts = new Casts(AsRaw.class);
        casts.arg("x").castForeignObjects(false).defaultWarningContext(RError.SHOW_CALLER).mustBe(missingValue().not(), RError.Message.ARGUMENTS_PASSED, 0, "'as.raw'", 1).returnIf(
                        foreign()).asRawVector();
    }

    @Specialization
    protected RAbstractRawVector asRaw(@SuppressWarnings("unused") RNull n) {
        return RDataFactory.createEmptyRawVector();
    }

    @Specialization(guards = "reuseTemporaryNode.supports(v)", limit = "getVectorAccessCacheSize()")
    protected RAbstractRawVector asRawVec(RAbstractRawVector v,
                    @Cached("createTemporary(v)") VectorReuse reuseTemporaryNode,
                    @Cached("createBinaryProfile()") ConditionProfile noAttributes) {
        if (noAttributes.profile(v.getAttributes() == null)) {
            return v;
        } else {
            RRawVector res = (RRawVector) reuseTemporaryNode.getMaterializedResult(v);
            res.resetAllAttributes(true);
            return res;
        }
    }

    @Specialization(replaces = "asRawVec")
    protected RAbstractRawVector asRawVecGeneric(RAbstractRawVector v,
                    @Cached("createTemporaryGeneric()") VectorReuse reuseTemporaryNode,
                    @Cached("createBinaryProfile()") ConditionProfile noAttributes) {
        return asRawVec(v, reuseTemporaryNode, noAttributes);
    }

    @Specialization(guards = "isForeignObject(obj)")
    protected RAbstractRawVector asRawForeign(VirtualFrame frame, TruffleObject obj,
                    @Cached("create()") ConvertForeignObjectNode convertForeign,
                    @Cached("createNonPreserving()") CastRawNode castRaw,
                    @Cached("create()") AsRaw asRaw) {
        Object o = convertForeign.convert(obj, true, false, true);
        if (!RRuntime.isForeignObject(o)) {
            return (RAbstractRawVector) asRaw.execute(frame, castRaw.executeRaw(o));
        }
        throw error(RError.Message.CANNOT_COERCE, "polyglot.value", "raw");
    }

}
