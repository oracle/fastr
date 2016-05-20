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

package com.oracle.truffle.r.nodes.builtin.base;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.access.ConstantNode;
import com.oracle.truffle.r.nodes.access.UpdateSlotNode;
import com.oracle.truffle.r.nodes.access.variables.ReadVariableNode;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.function.ClassHierarchyNode;
import com.oracle.truffle.r.nodes.function.ClassHierarchyNodeGen;
import com.oracle.truffle.r.nodes.function.RCallNode;
import com.oracle.truffle.r.nodes.function.WrapArgumentNode;
import com.oracle.truffle.r.nodes.function.signature.RArgumentsNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.RBuiltinKind;
import com.oracle.truffle.r.runtime.RCaller;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

@RBuiltin(name = "@<-", kind = RBuiltinKind.PRIMITIVE, parameterNames = {"", "", "value"}, nonEvalArgs = 1)
public abstract class UpdateSlot extends RBuiltinNode {

    private static final ArgumentsSignature SIGNATURE = ArgumentsSignature.get("cl", "name", "valueClass");

    @CompilationFinal private RFunction checkSlotAssignFunction;
    @Child private ClassHierarchyNode objClassHierarchy;
    @Child private ClassHierarchyNode valClassHierarchy;
    @Child private UpdateSlotNode updateSlotNode = com.oracle.truffle.r.nodes.access.UpdateSlotNodeGen.create(null, null, null);
    @Child private ReadVariableNode checkAtAssignmentFind = ReadVariableNode.createFunctionLookup(RSyntaxNode.INTERNAL, "checkAtAssignment");
    @Child private DirectCallNode checkAtAssignmentCall;
    @Child private RArgumentsNode argsNode = RArgumentsNode.create();
    private final ConditionProfile cached = ConditionProfile.createBinaryProfile();

    @Override
    protected void createCasts(CastBuilder casts) {
        casts.toAttributable(0, true, true, true);
    }

    protected String getName(Object nameObj) {
        assert nameObj instanceof RPromise;
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
        // TODO: this is not quite correct, but I wonder if we even reach here (can also be
        // augmented on demand)
        throw RError.error(this, RError.Message.SLOT_INVALID_TYPE, nameObj.getClass().toString());
    }

    private void checkSlotAssign(VirtualFrame frame, Object object, String name, Object value) {
        // TODO: optimize using a mechanism similar to overrides?
        if (checkSlotAssignFunction == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            checkSlotAssignFunction = (RFunction) checkAtAssignmentFind.execute(frame);
            checkAtAssignmentCall = insert(Truffle.getRuntime().createDirectCallNode(checkSlotAssignFunction.getTarget()));
            assert objClassHierarchy == null && valClassHierarchy == null;
            objClassHierarchy = insert(ClassHierarchyNodeGen.create(true, false));
            valClassHierarchy = insert(ClassHierarchyNodeGen.create(true, false));

        }
        RStringVector objClass = objClassHierarchy.execute(object);
        RStringVector valClass = objClassHierarchy.execute(value);
        RFunction currentFunction = (RFunction) checkAtAssignmentFind.execute(frame);
        if (cached.profile(currentFunction == checkSlotAssignFunction)) {
            // TODO: technically, someone could override checkAtAssignment function and access the
            // caller, but it's rather unlikely
            Object[] args = argsNode.execute(checkSlotAssignFunction, RCaller.create(frame, getOriginalCall()), null, new Object[]{objClass, name, valClass}, SIGNATURE, null);
            checkAtAssignmentCall.call(frame, args);
        } else {
            // slow path
            RContext.getEngine().evalFunction(currentFunction, frame.materialize(), objClass, name, valClass);
        }
    }

    /*
     * Motivation for cached version is that in the operator form (foo@bar<-baz), the name is an
     * interned string which allows us to avoid longer lookup
     */
    @Specialization(guards = "sameName(nameObj, nameObjCached)")
    protected Object updateSlotCached(VirtualFrame frame, Object object, @SuppressWarnings("unused") Object nameObj, Object value, @SuppressWarnings("unused") @Cached("nameObj") Object nameObjCached,
                    @Cached("getName(nameObjCached)") String name) {
        checkSlotAssign(frame, object, name, value);
        return updateSlotNode.executeUpdate(object, name, value);
    }

    @Specialization(contains = "updateSlotCached")
    protected Object updateSlot(VirtualFrame frame, Object object, Object nameObj, Object value) {
        String name = getName(nameObj);
        checkSlotAssign(frame, object, name, value);
        return updateSlotNode.executeUpdate(object, name, value);
    }

    protected boolean sameName(Object nameObj, Object nameObjCached) {
        assert nameObj instanceof RPromise;
        assert nameObjCached instanceof RPromise;
        return ((RPromise) nameObj).getRep() == ((RPromise) nameObjCached).getRep();
    }
}
