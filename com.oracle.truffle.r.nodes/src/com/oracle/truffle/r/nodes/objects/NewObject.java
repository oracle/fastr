/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995, 1996, 1997  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1995-2014, The R Core Team
 * Copyright (c) 2002-2008, The R Foundation
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.objects;

import static com.oracle.truffle.r.nodes.builtin.casts.fluent.CastNodeBuilder.newCastBuilder;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.access.AccessSlotNode;
import com.oracle.truffle.r.nodes.access.AccessSlotNodeGen;
import com.oracle.truffle.r.nodes.attributes.GetFixedAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.SetClassAttributeNode;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.nodes.unary.CastNode;
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
    @Child private GetFixedAttributeNode pckgAttrAccess = GetFixedAttributeNode.create(RRuntime.PCKG_ATTR_KEY);
    @Child private SetClassAttributeNode setClassAttrNode;

    @Child private CastNode castStringScalar;
    @Child private CastNode castLogicalScalar;
    {
        castStringScalar = newCastBuilder().asStringVector().findFirst(RRuntime.STRING_NA).buildCastNode();
        castLogicalScalar = newCastBuilder().asLogicalVector().findFirst(RRuntime.LOGICAL_NA).buildCastNode();
    }

    @Override
    protected void createCasts(CastBuilder casts) {
        // TODO: should we change the message to (incompatible) "Java level ..."?
        casts.arg(0).mustNotBeNull(RError.NO_CALLER, RError.Message.GENERIC, "C level NEW macro called with null class definition pointer");
    }

    @Specialization
    protected Object doNewObject(Object classDef) {

        Object e = accessSlotVirtual.executeAccess(classDef, RRuntime.S_VIRTUAL);
        if (((byte) castLogicalScalar.execute(e)) != RRuntime.LOGICAL_FALSE) {
            e = accessSlotClassName.executeAccess(classDef, RRuntime.S_CLASSNAME);
            throw RError.error(this, RError.Message.OBJECT_FROM_VIRTUAL, castStringScalar.execute(e));
        }
        e = accessSlotClassName.executeAccess(classDef, RRuntime.S_CLASSNAME);
        Object prototype = accessSlotPrototypeName.executeAccess(classDef, RRuntime.S_PROTOTYPE);
        Object value = duplicate.executeObject(prototype);
        assert value instanceof RAttributable;
        RAttributable valueAttr = (RAttributable) value;
        if (valueAttr instanceof RS4Object ||
                        (e instanceof RAttributable && ((RAttributable) e).getAttributes() != null && pckgAttrAccess.execute(((RAttributable) e).getAttributes()) != null)) {

            if (setClassAttrNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                setClassAttrNode = insert(SetClassAttributeNode.create());
            }

            setClassAttrNode.execute(valueAttr, e);
            valueAttr.setS4();
        }
        return value;
    }
}
