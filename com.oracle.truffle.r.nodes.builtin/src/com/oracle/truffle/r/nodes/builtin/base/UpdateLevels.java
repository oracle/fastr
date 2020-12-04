/*
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates
 *
 * All rights reserved.
 */

package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.RDispatch.INTERNAL_GENERIC;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.runtime.data.nodes.attributes.RemoveFixedAttributeNode;
import com.oracle.truffle.r.runtime.data.nodes.attributes.SetFixedAttributeNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

@RBuiltin(name = "levels<-", kind = PRIMITIVE, parameterNames = {"x", "value"}, dispatch = INTERNAL_GENERIC, behavior = PURE)
public abstract class UpdateLevels extends RBuiltinNode.Arg2 {

    static {
        Casts casts = new Casts(UpdateLevels.class);
        casts.arg("value").allowNull().asVector(false);
    }

    protected RemoveFixedAttributeNode createRemoveAttrNode() {
        return RemoveFixedAttributeNode.createFor(RRuntime.LEVELS_ATTR_KEY);
    }

    @Specialization
    protected RAbstractVector updateLevels(RAbstractVector vector, @SuppressWarnings("unused") RNull levels,
                    @Cached("createRemoveAttrNode()") RemoveFixedAttributeNode removeAttrNode) {
        RAbstractVector v = (RAbstractVector) vector.getNonShared();
        removeAttrNode.execute(v);
        return v;
    }

    protected SetFixedAttributeNode createSetLevelsAttrNode() {
        return SetFixedAttributeNode.create(RRuntime.LEVELS_ATTR_KEY);
    }

    @Specialization(guards = "!isRNull(levels)")
    protected RAbstractVector updateLevels(RAbstractVector vector, Object levels,
                    @Cached("createSetLevelsAttrNode()") SetFixedAttributeNode setLevelsAttrNode) {
        RAbstractVector v = (RAbstractVector) vector.getNonShared();
        setLevelsAttrNode.setAttr(v, levels);
        return v;
    }

    @Specialization
    protected RNull updateLevels(@SuppressWarnings("unused") RNull vector, @SuppressWarnings("unused") RNull levels) {
        return RNull.instance;
    }

    @Specialization(guards = "!isRNull(levels)")
    protected RAbstractVector updateLevels(@SuppressWarnings("unused") RNull vector, @SuppressWarnings("unused") Object levels) {
        throw error(Message.SET_ATTRIBUTES_ON_NULL);
    }
}
