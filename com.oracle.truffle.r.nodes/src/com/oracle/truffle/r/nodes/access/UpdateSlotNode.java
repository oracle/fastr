/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995, 1996, 1997  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1995-2014, The R Core Team
 * Copyright (c) 2002-2008, The R Foundation
 * Copyright (c) 2015, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.access;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.attributes.PutAttributeNode;
import com.oracle.truffle.r.nodes.attributes.PutAttributeNodeGen;
import com.oracle.truffle.r.nodes.function.ClassHierarchyNode;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.nodes.*;

@NodeChildren({@NodeChild(value = "object", type = RNode.class), @NodeChild(value = "name", type = RNode.class), @NodeChild(value = "value", type = RNode.class)})
public abstract class UpdateSlotNode extends RNode {

    public abstract Object executeUpdate(Object object, String name, Object value);

    @CompilationFinal RFunction checkSlotAssign;
    @Child private ClassHierarchyNode objClassHierarchy;
    @Child private ClassHierarchyNode valClassHierarchy;

    protected PutAttributeNode createAttrUpdate(String name) {
        return PutAttributeNodeGen.create(name);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isData(name)", "name == cachedName"})
    protected Object updateSlotS4Cached(RAttributable object, String name, Object value, @Cached("name") String cachedName, @Cached("createAttrUpdate(cachedName)") PutAttributeNode attributeUpdate) {
        attributeUpdate.execute(object.initAttributes(), value);
        return object;
    }

    @Specialization(contains = "updateSlotS4Cached", guards = "!isData(name)")
    protected Object updateSlotS4(RAttributable object, String name, Object value) {
        assert name == name.intern();
        object.setAttr(name, value);
        return object;
    }

    protected RFunction setDataPartFunction(REnvironment methodsNamespace) {
        Object f = methodsNamespace.findFunction("setDataPart");
        return (RFunction) RContext.getRRuntimeASTAccess().forcePromise(f);
    }

    private Object setDataPart(RAttributable object, Object value) {
        // TODO: any way to cache it or use a mechanism similar to overrides?
        REnvironment methodsNamespace = REnvironment.getRegisteredNamespace("methods");
        RFunction dataPart = setDataPartFunction(methodsNamespace);
        return RContext.getEngine().evalFunction(dataPart, methodsNamespace.getFrame(), object, value, RRuntime.LOGICAL_TRUE);
    }

    @Specialization(guards = "isData(name)")
    protected Object updateSlotS4Data(RAttributable object, @SuppressWarnings("unused") String name, Object value) {
        return setDataPart(object, value);
    }

    protected boolean isData(String name) {
        assert name == name.intern();
        return name == RRuntime.DOT_DATA;
    }

}
