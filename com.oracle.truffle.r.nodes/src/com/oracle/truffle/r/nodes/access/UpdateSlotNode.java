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
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.attributes.InitAttributesNode;
import com.oracle.truffle.r.nodes.attributes.SetAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SetPropertyNode;
import com.oracle.truffle.r.runtime.RCaller;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

// Transcribed from src/main/attrib.c file (R_do_slot_assign function)
@GenerateUncached
public abstract class UpdateSlotNode extends RBaseNode {

    private static final String SET_DATA_PART = "setDataPart";

    public abstract Object executeUpdate(Object object, String name, Object value);

    protected SetAttributeNode createAttrUpdate() {
        return SetAttributeNode.create();
    }

    private static Object prepareValue(Object value) {
        return value == RNull.instance ? RRuntime.PSEUDO_NULL : value;
    }

    @Specialization(guards = {"!isData(name)"})
    protected Object updateSlotS4Cached(RAttributable object, String name, Object value,
                    @Cached("create()") SetPropertyNode attributeUpdate,
                    @Cached("create()") InitAttributesNode initAttributes) {
        assert Utils.isInterned(name);
        attributeUpdate.execute(initAttributes.execute(object), name, prepareValue(value));
        return object;
    }

    @Specialization(guards = "isData(name)")
    @TruffleBoundary
    protected Object updateSlotS4Data(RAttributable object, @SuppressWarnings("unused") String name, Object value) {
        // TODO: any way to cache it or use a mechanism similar to overrides?
        REnvironment methodsNamespace = REnvironment.getRegisteredNamespace("methods");
        Object f = methodsNamespace.findFunction(SET_DATA_PART);
        RFunction dataPart = (RFunction) RContext.getRRuntimeASTAccess().forcePromise(SET_DATA_PART, f);
        return RContext.getEngine().evalFunction(dataPart, methodsNamespace.getFrame(), RCaller.createInvalid(null), true, null, object, prepareValue(value), RRuntime.LOGICAL_TRUE);
    }

    protected static boolean isData(String name) {
        return Utils.identityEquals(name, RRuntime.DOT_DATA);
    }
}
