/*
 * Copyright (c) 1995, 1996, 1997  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1995-2014, The R Core Team
 * Copyright (c) 2002-2008, The R Foundation
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates
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
package com.oracle.truffle.r.nodes.access;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.RASTUtils;
import com.oracle.truffle.r.runtime.data.nodes.attributes.GetPropertyNode;
import com.oracle.truffle.r.runtime.data.nodes.attributes.InitAttributesNode;
import com.oracle.truffle.r.runtime.RCaller;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.env.REnvironment;

// Transcribed from src/main/attrib.c file (R_has_slot function)

/**
 * Perform a check if the specified slot exists. This is almost like the {@code @} operator in R but
 * does not raise an error if the slot does not exist.
 */
public abstract class HasSlotNode extends BaseAccessSlotNode {

    public HasSlotNode(boolean asOperator) {
        super(asOperator);
    }

    public abstract boolean executeAccess(Object o, String name);

    @Specialization
    @SuppressWarnings("unused")
    protected boolean getSlotS4(RNull object, String name) {
        return false;
    }

    @Specialization(guards = {"slotAccessAllowed(object)", "!isDotData(name)"})
    protected boolean getSlotS4Cached(RAttributable object, String name,
                    @Cached("create()") GetPropertyNode attrAccess,
                    @Cached("create()") InitAttributesNode initAttrNode) {
        return attrAccess.execute(initAttrNode.execute(object), name) != null;
    }

    @Specialization(guards = {"slotAccessAllowed(object)", "isDotData(name)"})
    protected boolean getSlotS4Cached(@SuppressWarnings("unused") RAttributable object, @SuppressWarnings("unused") String name) {
        return true;
    }

    @Specialization(guards = {"!slotAccessAllowed(object)", "isDotData(name)"})
    protected boolean getSlotNonS4(RAttributable object, @SuppressWarnings("unused") String name) {
        // TODO: any way to cache it or use a mechanism similar to overrides?
        REnvironment methodsNamespace = REnvironment.getRegisteredNamespace("methods");
        RFunction dataPart = getDataPartFunction(methodsNamespace);

        // TODO will throw an error if ".data" does not exists but should not
        Object result = RContext.getEngine().evalFunction(dataPart, methodsNamespace.getFrame(), RCaller.create(null, RASTUtils.getOriginalCall(this)), true, null, object);
        return result != null;
    }

    @Fallback
    @SuppressWarnings("unused")
    protected boolean getSlot(Object object, String name) {
        return false;
    }

    public static HasSlotNode create(boolean asOperator) {
        return HasSlotNodeGen.create(asOperator);
    }
}
