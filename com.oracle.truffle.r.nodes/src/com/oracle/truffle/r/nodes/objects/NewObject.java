/*
 * Copyright (c) 1995, 1996, 1997  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1995-2014, The R Core Team
 * Copyright (c) 2002-2008, The R Foundation
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, a copy is available at
 * https://www.R-project.org/Licenses/
 */
package com.oracle.truffle.r.nodes.objects;

import static com.oracle.truffle.r.nodes.builtin.casts.fluent.CastNodeBuilder.newCastBuilder;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.access.AccessSlotNode;
import com.oracle.truffle.r.nodes.access.AccessSlotNodeGen;
import com.oracle.truffle.r.runtime.data.nodes.attributes.GetFixedAttributeNode;
import com.oracle.truffle.r.runtime.data.nodes.attributes.SpecialAttributesFunctions.SetClassAttributeNode;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.nodes.unary.CastNode;
import com.oracle.truffle.r.nodes.unary.DuplicateNode;
import com.oracle.truffle.r.nodes.unary.DuplicateNodeGen;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RS4Object;

// transcribed from src/main/objects.c
public abstract class NewObject extends RExternalBuiltinNode.Arg1 {

    @Child private AccessSlotNode accessSlotVirtual = AccessSlotNodeGen.create(true);
    @Child private AccessSlotNode accessSlotClassName = AccessSlotNodeGen.create(true);
    @Child private AccessSlotNode accessSlotPrototypeName = AccessSlotNodeGen.create(true);
    @Child private DuplicateNode duplicate = DuplicateNodeGen.create(true);
    @Child private GetFixedAttributeNode pckgAttrAccess = GetFixedAttributeNode.createFor(RRuntime.PCKG_ATTR_KEY);
    @Child private SetClassAttributeNode setClassAttrNode;

    @Child private CastNode castStringScalar = newCastBuilder().asStringVector().findFirst(RRuntime.STRING_NA).buildCastNode();
    @Child private CastNode castLogicalScalar = newCastBuilder().asLogicalVector().findFirst(RRuntime.LOGICAL_NA).buildCastNode();

    static {
        Casts casts = new Casts(NewObject.class);
        // TODO: should we change the message to (incompatible) "Java level ..."?
        casts.arg(0).mustNotBeNull(RError.Message.GENERIC, "C level NEW macro called with null class definition pointer");
    }

    @Specialization
    protected Object doNewObject(Object classDef) {

        Object e = accessSlotVirtual.executeAccess(classDef, RRuntime.S_VIRTUAL);
        if (((byte) castLogicalScalar.doCast(e)) != RRuntime.LOGICAL_FALSE) {
            e = accessSlotClassName.executeAccess(classDef, RRuntime.S_CLASSNAME);
            throw error(RError.Message.OBJECT_FROM_VIRTUAL, castStringScalar.doCast(e));
        }
        e = accessSlotClassName.executeAccess(classDef, RRuntime.S_CLASSNAME);
        Object prototype = accessSlotPrototypeName.executeAccess(classDef, RRuntime.S_PROTOTYPE);
        Object value = duplicate.executeObject(prototype);
        assert value instanceof RAttributable;
        RAttributable valueAttr = (RAttributable) value;
        if (valueAttr instanceof RS4Object || (e instanceof RAttributable && ((RAttributable) e).getAttributes() != null && pckgAttrAccess.execute(((RAttributable) e)) != null)) {

            if (setClassAttrNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                setClassAttrNode = insert(SetClassAttributeNode.create());
            }

            setClassAttrNode.setAttr(valueAttr, e);
            valueAttr.setS4();
        }
        return value;
    }
}
