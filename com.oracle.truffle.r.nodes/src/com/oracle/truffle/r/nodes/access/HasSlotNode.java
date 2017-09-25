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
package com.oracle.truffle.r.nodes.access;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.RASTUtils;
import com.oracle.truffle.r.nodes.attributes.GetAttributeNode;
import com.oracle.truffle.r.nodes.attributes.InitAttributesNode;
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

    @Specialization(guards = {"slotAccessAllowed(object)"})
    protected boolean getSlotS4Cached(RAttributable object, String name,
                    @Cached("createAttrAccess()") GetAttributeNode attrAccess,
                    @Cached("create()") InitAttributesNode initAttrNode) {
        return attrAccess.execute(initAttrNode.execute(object), name) != null;
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

    // this is really a fallback specialization but @Fallback does not work here (because of the
    // type of "object"?)
    @Specialization(guards = {"!slotAccessAllowed(object)", "!isDotData(name)"})
    @SuppressWarnings("unused")
    protected boolean getSlot(RAttributable object, String name) {
        return false;
    }

    public static HasSlotNode create(boolean asOperator) {
        return HasSlotNodeGen.create(asOperator);
    }
}
