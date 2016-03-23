/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995, 1996, 1997  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1995-2014, The R Core Team
 * Copyright (c) 2002-2008, The R Foundation
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.objects;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.access.AccessSlotNode;
import com.oracle.truffle.r.nodes.access.AccessSlotNodeGen;
import com.oracle.truffle.r.nodes.attributes.AttributeAccess;
import com.oracle.truffle.r.nodes.attributes.AttributeAccessNodeGen;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.nodes.unary.CastLogicalScalarNode;
import com.oracle.truffle.r.nodes.unary.CastStringScalarNode;
import com.oracle.truffle.r.nodes.unary.DuplicateNode;
import com.oracle.truffle.r.nodes.unary.DuplicateNodeGen;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RS4Object;
import com.oracle.truffle.r.runtime.data.RStringVector;

// transcribed from src/main/objects.c
public abstract class NewObject extends RExternalBuiltinNode.Arg1 {

    @Child private AccessSlotNode accessSlotVirtual = AccessSlotNodeGen.create(true, null, null);
    @Child private AccessSlotNode accessSlotClassName = AccessSlotNodeGen.create(true, null, null);
    @Child private AccessSlotNode accessSlotPrototypeName = AccessSlotNodeGen.create(true, null, null);
    @Child private CastStringScalarNode castStringScalar;
    @Child private CastLogicalScalarNode castLogicalScalar = CastLogicalScalarNode.create();
    @Child private DuplicateNode duplicate = DuplicateNodeGen.create(true);
    @Child private AttributeAccess pckgAttrAccess = AttributeAccessNodeGen.create(RRuntime.PCKG_ATTR_KEY);

    @Specialization(guards = "!isNull(classDef)")
    protected Object doNewObject(Object classDef) {

        Object e = accessSlotVirtual.executeAccess(classDef, RRuntime.S_VIRTUAL);
        if (castLogicalScalar.executeByte(e) != RRuntime.LOGICAL_FALSE) {
            if (castStringScalar == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                castStringScalar = insert(CastStringScalarNode.create());
            }
            e = accessSlotClassName.executeAccess(classDef, RRuntime.S_CLASSNAME);
            throw RError.error(this, RError.Message.OBJECT_FROM_VIRTUAL, castStringScalar.executeString(e));
        }
        e = accessSlotClassName.executeAccess(classDef, RRuntime.S_CLASSNAME);
        Object prototype = accessSlotPrototypeName.executeAccess(classDef, RRuntime.S_PROTOTYPE);
        Object value = duplicate.executeObject(prototype);
        assert value instanceof RAttributable;
        RAttributable valueAttr = (RAttributable) value;
        if (valueAttr instanceof RS4Object || (e instanceof RAttributable && ((RAttributable) e).getAttributes() != null && pckgAttrAccess.execute(((RAttributable) e).getAttributes()) != null)) {
            valueAttr = valueAttr.setClassAttr((RStringVector) e, false);
            valueAttr.setS4();
        }
        return value;
    }

    @Specialization
    protected Object doNewObject(@SuppressWarnings("unused") RNull classDef) {
        // TODO: should we change the message to (incompatible) "Java level ..."?
        throw RError.error(this, RError.Message.GENERIC, "C level NEW macro called with null class definition pointer");
    }

    protected boolean isNull(Object o) {
        return o == RNull.instance;
    }
}
