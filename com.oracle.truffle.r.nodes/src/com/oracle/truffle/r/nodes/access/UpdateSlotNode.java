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
package com.oracle.truffle.r.nodes.access;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.RASTUtils;
import com.oracle.truffle.r.nodes.attributes.InitAttributesNode;
import com.oracle.truffle.r.nodes.attributes.SetAttributeNode;
import com.oracle.truffle.r.runtime.RCaller;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.nodes.RNode;

// Transcribed from src/main/attrib.c file (R_do_slot_assign function)

@NodeChildren({@NodeChild(value = "object", type = RNode.class), @NodeChild(value = "name", type = RNode.class), @NodeChild(value = "value", type = RNode.class)})
public abstract class UpdateSlotNode extends RNode {

    public abstract Object executeUpdate(Object object, String name, Object value);

    protected SetAttributeNode createAttrUpdate() {
        return SetAttributeNode.create();
    }

    private static Object prepareValue(Object value) {
        return value == RNull.instance ? RRuntime.PSEUDO_NULL : value;
    }

    @Specialization(guards = {"!isData(name)"})
    protected Object updateSlotS4Cached(RAttributable object, String name, Object value, //
                    @Cached("createAttrUpdate()") SetAttributeNode attributeUpdate, //
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
        String identifier = "setDataPart";
        Object f = methodsNamespace.findFunction(identifier);
        RFunction dataPart = (RFunction) RContext.getRRuntimeASTAccess().forcePromise(identifier, f);
        return RContext.getEngine().evalFunction(dataPart, methodsNamespace.getFrame(), RCaller.create(Utils.getActualCurrentFrame(), RASTUtils.getOriginalCall(this)), null, object,
                        prepareValue(value),
                        RRuntime.LOGICAL_TRUE);
    }

    protected boolean isData(String name) {
        assert Utils.isInterned(name);
        return name == RRuntime.DOT_DATA;
    }
}
