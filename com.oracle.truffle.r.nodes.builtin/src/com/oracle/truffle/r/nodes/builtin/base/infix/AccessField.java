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
package com.oracle.truffle.r.nodes.builtin.base.infix;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.runtime.RDispatch.INTERNAL_GENERIC;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.EmptyTypeSystemFlatLayout;
import com.oracle.truffle.r.nodes.access.vector.ElementAccessMode;
import com.oracle.truffle.r.nodes.access.vector.ExtractListElement;
import com.oracle.truffle.r.nodes.access.vector.ExtractVectorNode;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.builtins.RSpecialFactory;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.nodes.RNode;

@TypeSystemReference(EmptyTypeSystemFlatLayout.class)
@NodeChild(value = "arguments", type = RNode[].class)
abstract class AccessFieldSpecial extends RNode {

    @CompilationFinal String cachedField;
    @CompilationFinal RStringVector cachedNames;
    @Child private ExtractListElement extractListElement = ExtractListElement.create();

    @Specialization(guards = {"isCached(list, field)", "list.getNames() != null"})
    @SuppressWarnings("unused")
    public Object doList(RList list, String field, @Cached("getIndex(list.getNames(), field)") int index) {
        if (index == -1) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            throw RSpecialFactory.FULL_CALL_NEEDED;
        }
        if (cachedField == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            cachedField = field;
            cachedNames = list.getNames();
        }
        return extractListElement.execute(list, index);
    }

    @Fallback
    @SuppressWarnings("unused")
    public void doFallback(Object container, Object field) {
        throw RSpecialFactory.FULL_CALL_NEEDED;
    }

    protected boolean isCached(RList list, String field) {
        return cachedField == null || (cachedField == field && list.getNames() == cachedNames);
    }

    protected int getIndex(RAbstractStringVector names, String field) {
        for (int i = 0; i < names.getLength(); i++) {
            String current = names.getDataAt(i);
            if (current == field || current.equals(field)) {
                return i;
            }
        }
        return -1;
    }
}

@RBuiltin(name = "$", kind = PRIMITIVE, parameterNames = {"", ""}, dispatch = INTERNAL_GENERIC, behavior = PURE)
public abstract class AccessField extends RBuiltinNode {

    @Child private ExtractVectorNode extract = ExtractVectorNode.create(ElementAccessMode.SUBSCRIPT, true);

    private final ConditionProfile invalidAtomicVector = ConditionProfile.createBinaryProfile();
    private final BranchProfile error = BranchProfile.create();

    @Override
    protected void createCasts(CastBuilder casts) {
        casts.arg(1).defaultError(Message.INVALID_SUBSCRIPT_TYPE, RType.Language.getName()).mustBe(stringValue()).asStringVector().findFirst();
    }

    public static RNode createSpecial(ArgumentsSignature signature, RNode[] arguments) {
        return signature.getNonNullCount() == 0 ? AccessFieldSpecialNodeGen.create(arguments) : null;
    }

    @Specialization
    protected Object access(VirtualFrame frame, Object container, String field) {
        if (!invalidAtomicVector.profile(container instanceof RAbstractListVector) && container instanceof RAbstractVector) {
            error.enter();
            throw RError.error(this, RError.Message.DOLLAR_ATOMIC_VECTORS);
        }
        return extract.applyAccessField(frame, container, field);
    }
}
