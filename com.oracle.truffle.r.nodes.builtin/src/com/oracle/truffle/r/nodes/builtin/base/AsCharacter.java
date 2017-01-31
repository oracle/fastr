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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.asStringVector;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.instanceOf;
import static com.oracle.truffle.r.runtime.RDispatch.INTERNAL_GENERIC;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RDeparse;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;

@RBuiltin(name = "as.character", kind = PRIMITIVE, parameterNames = {"x", "..."}, dispatch = INTERNAL_GENERIC, behavior = PURE)
public abstract class AsCharacter extends RBuiltinNode {

    private final ConditionProfile noAttributes = ConditionProfile.createBinaryProfile();

    @Override
    protected void createCasts(CastBuilder casts) {
        casts.arg("x").mapIf(instanceOf(RAbstractListVector.class).not(), asStringVector());
    }

    @Specialization
    protected RAbstractStringVector asCharacter(@SuppressWarnings("unused") RNull n) {
        return RDataFactory.createEmptyStringVector();
    }

    @Specialization
    protected RAbstractStringVector asCharacter(RAbstractStringVector v) {
        if (noAttributes.profile(v.getAttributes() == null)) {
            return v;
        } else {
            return (RAbstractStringVector) v.copyDropAttributes();
        }
    }

    @Specialization
    protected RStringVector asCharacter(RAbstractListVector list) {
        int len = list.getLength();
        boolean complete = RDataFactory.COMPLETE_VECTOR;
        String[] data = new String[len];
        for (int i = 0; i < len; i++) {
            Object elem = list.getDataAt(i);
            if (elem instanceof String) {
                data[i] = (String) elem;
            } else if (elem instanceof RStringVector && ((RStringVector) elem).getLength() == 1) {
                data[i] = ((RStringVector) elem).getDataAt(0);
            } else {
                data[i] = RDeparse.deparse(elem, RDeparse.MAX_Cutoff, true, RDeparse.SIMPLEDEPARSE, -1);
            }
            if (RRuntime.isNA(data[i])) {
                complete = RDataFactory.INCOMPLETE_VECTOR;
            }
        }
        return RDataFactory.createStringVector(data, complete);
    }
}
