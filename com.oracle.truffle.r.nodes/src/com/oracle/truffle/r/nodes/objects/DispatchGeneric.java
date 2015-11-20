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
package com.oracle.truffle.r.nodes.objects;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.utilities.ConditionProfile;
import com.oracle.truffle.r.nodes.access.variables.ReadVariableNode;
import com.oracle.truffle.r.nodes.access.variables.ReadVariableNode.ReadKind;
import com.oracle.truffle.r.nodes.function.signature.RArgumentsNode;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

public abstract class DispatchGeneric extends RBaseNode {

    public abstract Object executeObject(VirtualFrame frame, REnvironment methodsEnv, REnvironment mtable, RStringVector classes, RFunction fdef, String fname);

    private final ConditionProfile singleStringProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile cached = ConditionProfile.createBinaryProfile();
    private final RCaller caller = RDataFactory.createCaller(this);
    @Child private ReadVariableNode inheritForDispatchFind;
    @Child private DirectCallNode inheritForDispatchCall;
    @CompilationFinal private RFunction inheritForDispatchFunction;
    @Child private RArgumentsNode argsNode = RArgumentsNode.create();
    @Child private LoadMethod loadMethod = LoadMethodNodeGen.create();
    @Child private ExecuteMethod executeMethod = ExecuteMethodNodeGen.create();

    @TruffleBoundary
    private static String createMultiDispatchString(RStringVector classes) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < classes.getLength(); i++) {
            if (i > 0) {
                sb.append('#');
            }
            sb.append(classes.getDataAt(i));
        }
        return sb.toString();
    }

    protected String createDispatchString(RStringVector classes) {
        if (singleStringProfile.profile(classes.getLength() == 1)) {
            return classes.getDataAt(0);
        } else {
            return createMultiDispatchString(classes);
        }
    }

    protected ReadVariableNode createTableRead(String dispatchString) {
        return ReadVariableNode.create(dispatchString, RType.Any, ReadKind.SilentLocal);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "equalClasses(classes, cachedClasses)")
    protected Object dispatch(VirtualFrame frame, REnvironment methodsEnv, REnvironment mtable, RStringVector classes, RFunction fdef, String fname, @Cached("classes") RStringVector cachedClasses,
                    @Cached("createDispatchString(cachedClasses)") String dispatchString, @Cached("createTableRead(dispatchString)") ReadVariableNode tableRead) {
        RFunction method = (RFunction) tableRead.execute(null, mtable.getFrame());
        if (method == null) {
            if (inheritForDispatchFind == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                inheritForDispatchFind = insert(ReadVariableNode.create(".InheritForDispatch", RType.Function, ReadKind.Normal));
                inheritForDispatchFunction = (RFunction) inheritForDispatchFind.execute(null, methodsEnv.getFrame());
                inheritForDispatchCall = insert(Truffle.getRuntime().createDirectCallNode(inheritForDispatchFunction.getTarget()));

            }
            RFunction currentFunction = (RFunction) inheritForDispatchFind.execute(null, methodsEnv.getFrame());
            if (cached.profile(currentFunction == inheritForDispatchFunction)) {
                Object[] args = argsNode.execute(inheritForDispatchFunction, caller, null, RArguments.getDepth(frame) + 1, new Object[]{classes, fdef, mtable},
                                ArgumentsSignature.get("classes", "fdef", "mtable"), null);
                method = (RFunction) inheritForDispatchCall.call(frame, args);
            } else {
                // slow path
                method = (RFunction) RContext.getEngine().evalFunction(currentFunction, frame.materialize(), classes, fdef, mtable);
            }
        }
        method = loadMethod.executeRFunction(frame, methodsEnv, method, fname);
        Object ret = executeMethod.executeMethod(frame, method);
        return ret;
    }

    protected boolean equalClasses(RStringVector classes, RStringVector cachedClasses) {
        if (cachedClasses.getLength() == classes.getLength()) {
            for (int i = 0; i < cachedClasses.getLength(); i++) {
                // TODO: makes sure equality is good enough here, but it's for optimization only
                // anwyay
                if (cachedClasses.getDataAt(i) != classes.getDataAt(i)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
}
