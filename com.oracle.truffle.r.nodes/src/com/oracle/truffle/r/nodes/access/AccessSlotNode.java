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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.RASTUtils;
import com.oracle.truffle.r.nodes.attributes.GetPropertyNode;
import com.oracle.truffle.r.nodes.attributes.InitAttributesNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetClassAttributeNode;
import com.oracle.truffle.r.nodes.function.ImplicitClassHierarchyNode;
import com.oracle.truffle.r.nodes.unary.InternStringNode;
import com.oracle.truffle.r.runtime.RCaller;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RRuntimeASTAccess.AccessSlotAccess;
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
public abstract class AccessSlotNode extends BaseAccessSlotNode implements AccessSlotAccess {

    public AccessSlotNode(boolean asOperator) {
        super(asOperator);
    }

    public abstract Object executeAccess(Object o, Object name);

    @Specialization
    protected Object getSlotS4(@SuppressWarnings("unused") RNull object, String name) {
        throw error(RError.Message.SLOT_BASIC_CLASS, name, "NULL");
    }

    @Specialization(guards = {"slotAccessAllowed(object)"})
    protected Object getSlotS4Cached(RAttributable object, String name,
                    @Cached("create()") InternStringNode intern,
                    @Cached("create()") GetPropertyNode attrAccess,
                    @Cached("create()") InitAttributesNode initAttrNode,
                    @Cached("create()") GetClassAttributeNode getClassNode) {
        Object value = attrAccess.execute(initAttrNode.execute(object), name);
        return getSlotS4Internal(object, intern.execute(name), value, getClassNode);
    }

    @Specialization(guards = {"!slotAccessAllowed(object)", "isDotData(name)"})
    protected Object getSlotNonS4(RAttributable object, @SuppressWarnings("unused") String name) {
        // TODO: any way to cache it or use a mechanism similar to overrides?
        REnvironment methodsNamespace = REnvironment.getRegisteredNamespace("methods");
        RFunction dataPart = getDataPartFunction(methodsNamespace);
        return RContext.getEngine().evalFunction(dataPart, methodsNamespace.getFrame(), RCaller.create(null, RASTUtils.getOriginalCall(this)), true, null, object);
    }

    @Specialization(guards = {"!slotAccessAllowed(object)", "!isDotData(name)"})
    @TruffleBoundary
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

    @Fallback
    @TruffleBoundary
    protected Object getSlot(Object object, Object name) {
        throw error(RError.Message.SLOT_CANNOT_GET, name, RRuntime.getRTypeName(object));
    }

}
