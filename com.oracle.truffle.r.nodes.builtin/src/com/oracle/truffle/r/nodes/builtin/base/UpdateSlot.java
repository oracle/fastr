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
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.function.ClassHierarchyNode;
import com.oracle.truffle.r.nodes.function.ClassHierarchyNodeGen;
import com.oracle.truffle.r.nodes.function.call.CallRFunctionNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RCaller;
import com.oracle.truffle.r.runtime.RRuntimeASTAccess.UpdateSlotAccess;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

@RBuiltin(name = "@<-", kind = PRIMITIVE, parameterNames = {"", "", "value"}, nonEvalArgs = 1, behavior = COMPLEX)
public abstract class UpdateSlot extends RBuiltinNode.Arg3 implements UpdateSlotAccess {

    @Child private UpdateSlotNode updateSlotNode = com.oracle.truffle.r.nodes.access.UpdateSlotNodeGen.create();
    @Child private PromiseAsNameNode promiseAsNameNode;

    static {
        Casts casts = new Casts(UpdateSlot.class);
        casts.arg(0).asAttributable(true, true, true);
    }

    @Override
    public abstract Object execute(VirtualFrame frameValue, Object arg0Value, Object arg1Value, Object arg2Value);

    private String getName(Object nameObj) {
        if (promiseAsNameNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            promiseAsNameNode = insert(new PromiseAsNameNode());
        }
        return promiseAsNameNode.execute(nameObj);
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
                checkAtAssignmentCall.execute(frame, checkSlotAssignFunction, RCaller.createInvalid(frame), new Object[]{objClass, name, valClass}, SIGNATURE,
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

    @Specialization
    protected Object updateSlot(VirtualFrame frame, Object object, String name, Object value,
                    @Cached("new()") CheckSlotAssignNode check) {
        check.execute(frame, object, name, value);
        return updateSlotNode.executeUpdate(object, name, value);
    }
}
