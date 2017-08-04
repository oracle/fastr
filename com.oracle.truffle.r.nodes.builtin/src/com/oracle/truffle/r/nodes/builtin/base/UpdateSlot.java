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

package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.builtins.RBehavior.COMPLEX;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.access.UpdateSlotNode;
import com.oracle.truffle.r.nodes.access.variables.ReadVariableNode;
import com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.function.ClassHierarchyNode;
import com.oracle.truffle.r.nodes.function.ClassHierarchyNodeGen;
import com.oracle.truffle.r.nodes.function.call.CallRFunctionNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RCaller;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.data.RPromise.Closure;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxConstant;
import com.oracle.truffle.r.runtime.nodes.RSyntaxElement;
import com.oracle.truffle.r.runtime.nodes.RSyntaxLookup;

@RBuiltin(name = "@<-", kind = PRIMITIVE, parameterNames = {"", "", "value"}, nonEvalArgs = 1, behavior = COMPLEX)
public abstract class UpdateSlot extends RBuiltinNode.Arg3 {

    @Child private UpdateSlotNode updateSlotNode = com.oracle.truffle.r.nodes.access.UpdateSlotNodeGen.create();

    static {
        Casts casts = new Casts(UpdateSlot.class);
        casts.arg(0).asAttributable(true, true, true);
    }

    protected String getName(RPromise nameObj) {
        Closure closure = nameObj.getClosure();
        if (closure.asSymbol() != null) {
            return closure.asSymbol();
        } else if (closure.asStringConstant() != null) {
            return closure.asStringConstant();
        } else {
            CompilerDirectives.transferToInterpreter();
            RSyntaxElement element = closure.getExpr().asRSyntaxNode();
            assert !(element instanceof RSyntaxLookup);
            if (element instanceof RSyntaxConstant) {
                throw error(RError.Message.SLOT_INVALID_TYPE, Predef.typeName().apply(((RSyntaxConstant) element).getValue()));
            } else {
                throw error(RError.Message.SLOT_INVALID_TYPE, "language");
            }
        }
    }

    public static final class CheckSlotAssignNode extends RBaseNode {

        private static final ArgumentsSignature SIGNATURE = ArgumentsSignature.get("cl", "name", "valueClass");

        @CompilationFinal private RFunction checkSlotAssignFunction;
        @Child private ClassHierarchyNode objClassHierarchy;
        @Child private ClassHierarchyNode valClassHierarchy;
        @Child private ReadVariableNode checkAtAssignmentFind = ReadVariableNode.createFunctionLookup("checkAtAssignment");
        @Child private CallRFunctionNode checkAtAssignmentCall;

        private final ConditionProfile cached = ConditionProfile.createBinaryProfile();

        public void execute(VirtualFrame frame, Object object, String name, Object value) {
            if (checkSlotAssignFunction == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                checkSlotAssignFunction = (RFunction) checkAtAssignmentFind.execute(frame);
            }
            if (checkAtAssignmentCall == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                checkAtAssignmentCall = insert(CallRFunctionNode.create(checkSlotAssignFunction.getTarget()));
            }
            if (objClassHierarchy == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                objClassHierarchy = insert(ClassHierarchyNodeGen.create(true, false));
            }
            if (valClassHierarchy == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                valClassHierarchy = insert(ClassHierarchyNodeGen.create(true, false));
            }
            RStringVector objClass = objClassHierarchy.execute(object);
            RStringVector valClass = valClassHierarchy.execute(value);
            RFunction currentFunction = (RFunction) checkAtAssignmentFind.execute(frame);
            if (cached.profile(currentFunction == checkSlotAssignFunction)) {
                // TODO: technically, someone could override checkAtAssignment function and access
                // the caller, but it's rather unlikely
                checkAtAssignmentCall.execute(frame, checkSlotAssignFunction, RCaller.createInvalid(frame), null, new Object[]{objClass, name, valClass}, SIGNATURE,
                                checkSlotAssignFunction.getEnclosingFrame(), null);
            } else {
                // slow path
                RContext.getEngine().evalFunction(currentFunction, frame.materialize(), RCaller.createInvalid(frame), true, null, objClass, name, valClass);
            }
        }
    }

    @Specialization
    protected Object updateSlot(VirtualFrame frame, Object object, RPromise nameObj, Object value,
                    @Cached("new()") CheckSlotAssignNode check) {
        String name = getName(nameObj);
        check.execute(frame, object, name, value);
        return updateSlotNode.executeUpdate(object, name, value);
    }
}
