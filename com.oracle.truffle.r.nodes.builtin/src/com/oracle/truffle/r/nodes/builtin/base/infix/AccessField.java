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
package com.oracle.truffle.r.nodes.builtin.base.infix;

import static com.oracle.truffle.r.runtime.RDispatch.INTERNAL_GENERIC;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.access.vector.ElementAccessMode;
import com.oracle.truffle.r.nodes.access.vector.ExtractVectorNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.base.infix.special.AccessFieldSpecialNodeGen;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.nodes.RNode;

@RBuiltin(name = "$", kind = PRIMITIVE, parameterNames = {"", ""}, isFieldAccess = true, dispatch = INTERNAL_GENERIC, behavior = PURE)
public abstract class AccessField extends RBuiltinNode.Arg2 {

    @Child private ExtractVectorNode extract = ExtractVectorNode.create(ElementAccessMode.FIELD_SUBSCRIPT, true);

    private final ConditionProfile invalidAtomicVector = ConditionProfile.createBinaryProfile();
    private final BranchProfile error = BranchProfile.create();

    static {
        Casts.noCasts(AccessField.class);
    }

    public static RNode createSpecial(ArgumentsSignature signature, RNode[] arguments, @SuppressWarnings("unused") boolean inReplacement) {
        return signature.getNonNullCount() == 0 ? AccessFieldSpecialNodeGen.create(arguments) : null;
    }

    @Specialization
    protected Object access(Object container, String field) {
        if (!invalidAtomicVector.profile(container instanceof RAbstractListVector) && container instanceof RAbstractVector) {
            error.enter();
            throw error(RError.Message.DOLLAR_ATOMIC_VECTORS);
        }
        return extract.applyAccessField(container, field);
    }
}
