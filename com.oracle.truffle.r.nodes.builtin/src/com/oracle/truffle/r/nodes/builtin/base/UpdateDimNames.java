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

import static com.oracle.truffle.r.runtime.RDispatch.INTERNAL_GENERIC;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.attributes.RemoveFixedAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.SetDimNamesAttributeNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.unary.CastStringNode;
import com.oracle.truffle.r.nodes.unary.CastStringNodeGen;
import com.oracle.truffle.r.nodes.unary.CastToVectorNode;
import com.oracle.truffle.r.nodes.unary.CastToVectorNodeGen;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

@RBuiltin(name = "dimnames<-", kind = PRIMITIVE, parameterNames = {"x", "value"}, dispatch = INTERNAL_GENERIC, behavior = PURE)
public abstract class UpdateDimNames extends RBuiltinNode {

    protected static final String DIMNAMES_ATTR_KEY = RRuntime.DIMNAMES_ATTR_KEY;

    private final ConditionProfile shareListProfile = ConditionProfile.createBinaryProfile();

    @Child private CastStringNode castStringNode;
    @Child private CastToVectorNode castVectorNode;

    private Object castString(Object o) {
        if (castStringNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castStringNode = insert(CastStringNodeGen.create(true, true, true));
        }
        return castStringNode.execute(o);
    }

    private RAbstractVector castVector(Object value) {
        if (castVectorNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castVectorNode = insert(CastToVectorNodeGen.create(false));
        }
        return ((RAbstractVector) castVectorNode.execute(value)).materialize();
    }

    public abstract RAbstractContainer executeRAbstractContainer(RAbstractContainer container, Object o);

    private RList convertToListOfStrings(RList oldList) {
        RList list = oldList;
        if (shareListProfile.profile(list.isShared())) {
            list = (RList) list.copy();
        }
        for (int i = 0; i < list.getLength(); i++) {
            Object element = list.getDataAt(i);
            if (element != RNull.instance) {
                Object s = castString(castVector(element));
                list.updateDataAt(i, s, null);
            }
        }
        return list;
    }

    @Specialization
    protected RAbstractContainer updateDimnamesNull(RAbstractContainer container, @SuppressWarnings("unused") RNull list,
                    @Cached("createDimNames()") RemoveFixedAttributeNode remove) {
        RAbstractContainer result = (RAbstractContainer) container.getNonShared();
        remove.execute(result);
        return result;
    }

    @Specialization(guards = "list.getLength() == 0")
    protected RAbstractContainer updateDimnamesEmpty(RAbstractContainer container, @SuppressWarnings("unused") RList list,
                    @Cached("createDimNames()") RemoveFixedAttributeNode remove) {
        return updateDimnamesNull(container, RNull.instance, remove);
    }

    @Specialization(guards = "list.getLength() > 0")
    protected RAbstractContainer updateDimnames(RAbstractContainer container, RList list,
                    @Cached("create()") SetDimNamesAttributeNode setDimNamesNode) {
        RAbstractContainer result = (RAbstractContainer) container.getNonShared();
        setDimNamesNode.setDimNames(result, convertToListOfStrings(list));
        return result;
    }

    @Specialization(guards = "!isRList(c)")
    protected RAbstractContainer updateDimnamesError(@SuppressWarnings("unused") RAbstractContainer container, @SuppressWarnings("unused") Object c) {
        CompilerDirectives.transferToInterpreter();
        throw RError.error(this, RError.Message.DIMNAMES_LIST);
    }
}
