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

package com.oracle.truffle.r.nodes.builtin.base;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.r.nodes.access.ConstantNode;
import com.oracle.truffle.r.nodes.access.UpdateSlotNode;
import com.oracle.truffle.r.nodes.access.variables.ReadVariableNode;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.function.ClassHierarchyNode;
import com.oracle.truffle.r.nodes.function.ClassHierarchyNodeGen;
import com.oracle.truffle.r.nodes.function.RCallNode;
import com.oracle.truffle.r.nodes.function.WrapArgumentNode;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.data.RS4Object;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.env.REnvironment;

@RBuiltin(name = "@<-", kind = RBuiltinKind.PRIMITIVE, parameterNames = {"", "", ""}, nonEvalArgs = 1)
public abstract class UpdateSlot extends RBuiltinNode {

    @CompilationFinal RFunction checkSlotAssign;
    @Child private ClassHierarchyNode objClassHierarchy;
    @Child private ClassHierarchyNode valClassHierarchy;
    @Child UpdateSlotNode updateSlotNode = com.oracle.truffle.r.nodes.access.UpdateSlotNodeGen.create(null, null, null);

    protected String getName(Object nameObj) {
        if (nameObj instanceof RPromise) {
            Object rep = ((RPromise) nameObj).getRep();
            if (rep instanceof WrapArgumentNode) {
                rep = ((WrapArgumentNode) rep).getOperand();
            }
            if (rep instanceof ConstantNode) {
                Object val = ((ConstantNode) rep).getValue();
                if (val instanceof String) {
                    return (String) val;
                }
                if (val instanceof RSymbol) {
                    return ((RSymbol) val).getName();
                }
            } else if (rep instanceof ReadVariableNode) {
                return ((ReadVariableNode) rep).getIdentifier();
            } else if (rep instanceof RCallNode) {
                throw RError.error(this, RError.Message.SLOT_INVALID_TYPE, "language");
            }
        }
        // TODO: this is not quite correct, but I wonder if we even reach here (can also be
        // augmented on demand)
        throw RError.error(this, RError.Message.SLOT_INVALID_TYPE, nameObj.getClass().toString());
    }

    private void checkSlotAssign(VirtualFrame frame, RAttributable object, String name, Object value) {
        // TODO: optimize using a mechanism similar to overrides?
        if (checkSlotAssign == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            REnvironment methodsNamespace = REnvironment.getRegisteredNamespace("methods");
            Object f = methodsNamespace.findFunction("checkAtAssignment");
            checkSlotAssign = (RFunction) RContext.getRRuntimeASTAccess().forcePromise(f);
            assert objClassHierarchy == null && valClassHierarchy == null;
            objClassHierarchy = insert(ClassHierarchyNodeGen.create(true));
            valClassHierarchy = insert(ClassHierarchyNodeGen.create(true));

        }
        RStringVector objClass = objClassHierarchy.execute(object);
        RStringVector valClass = objClassHierarchy.execute(value);
        RContext.getEngine().evalFunction(checkSlotAssign, frame.materialize(), objClass, name, valClass);
    }

    @Specialization
    protected Object updateSlot(VirtualFrame frame, RS4Object object, Object nameObj, Object value) {
        String name = getName(nameObj);
        checkSlotAssign(frame, object, name, value);
        return updateSlotNode.executeUpdate(object, name, value);
    }

    @Specialization
    protected Object updateSlot(VirtualFrame frame, RAbstractContainer object, Object nameObj, Object value) {
        String name = getName(nameObj);
        checkSlotAssign(frame, object, name, value);
        return updateSlotNode.executeUpdate(object, name, value);
    }
}
