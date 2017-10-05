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
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetClassAttributeNode;
import com.oracle.truffle.r.nodes.function.ImplicitClassHierarchyNode;
import com.oracle.truffle.r.runtime.RCaller;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.env.REnvironment;

// Transcribed from src/main/attrib.c file (R_do_slot function)

/**
 * Perform a slot access. This node represents the {@code @} operator in R.
 */
public abstract class AccessSlotNode extends BaseAccessSlotNode {

    public AccessSlotNode(boolean asOperator) {
        super(asOperator);
    }

    public abstract Object executeAccess(Object o, String name);

    @Specialization
    protected Object getSlotS4(@SuppressWarnings("unused") RNull object, String name) {
        throw error(RError.Message.SLOT_BASIC_CLASS, name, "NULL");
    }

    @Specialization(guards = {"slotAccessAllowed(object)"})
    protected Object getSlotS4Cached(RAttributable object, String name,
                    @Cached("createAttrAccess()") GetAttributeNode attrAccess,
                    @Cached("create()") InitAttributesNode initAttrNode,
                    @Cached("create()") GetClassAttributeNode getClassNode) {
        Object value = attrAccess.execute(initAttrNode.execute(object), name);
        String internedName = Utils.intern(name);
        return getSlotS4Internal(object, internedName, value, getClassNode);
    }

    @Specialization(guards = {"!slotAccessAllowed(object)", "isDotData(name)"})
    protected Object getSlotNonS4(RAttributable object, @SuppressWarnings("unused") String name) {
        // TODO: any way to cache it or use a mechanism similar to overrides?
        REnvironment methodsNamespace = REnvironment.getRegisteredNamespace("methods");
        RFunction dataPart = getDataPartFunction(methodsNamespace);
        return RContext.getEngine().evalFunction(dataPart, methodsNamespace.getFrame(), RCaller.create(null, RASTUtils.getOriginalCall(this)), true, null, object);
    }

    // this is really a fallback specialization but @Fallback does not work here (because of the
    // type of "object"?)
    @Specialization(guards = {"!slotAccessAllowed(object)", "!isDotData(name)"})
    protected Object getSlot(RAttributable object, String name,
                    @Cached("create()") GetClassAttributeNode getClassNode) {
        RStringVector classAttr = getClassNode.getClassAttr(object);
        if (classAttr == null) {
            RStringVector implicitClassVec = ImplicitClassHierarchyNode.getImplicitClass(object, false);
            assert implicitClassVec.getLength() > 0;
            throw error(RError.Message.SLOT_BASIC_CLASS, name, implicitClassVec.getDataAt(0));
        } else {
            assert classAttr.getLength() > 0;
            throw error(RError.Message.SLOT_NON_S4, name, classAttr.getDataAt(0));
        }
    }
}
