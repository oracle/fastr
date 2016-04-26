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
package com.oracle.truffle.r.nodes.objects;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.nodes.access.variables.LocalReadVariableNode;
import com.oracle.truffle.r.nodes.access.variables.ReadVariableNode;
import com.oracle.truffle.r.nodes.function.signature.RArgumentsNode;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

public abstract class DispatchGeneric extends RBaseNode {

    public abstract Object executeObject(VirtualFrame frame, REnvironment mtable, RStringVector classes, RFunction fdef, String fname);

    private final ConditionProfile singleStringProfile = ConditionProfile.createBinaryProfile();
    private final BranchProfile equalsMethodRequired = BranchProfile.create();
    @Child private RArgumentsNode argsNode = RArgumentsNode.create();
    @Child private LoadMethod loadMethod = LoadMethodNodeGen.create();
    @Child private ExecuteMethod executeMethod = new ExecuteMethod();

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

    protected LocalReadVariableNode createTableRead(String dispatchString) {
        return LocalReadVariableNode.create(dispatchString, true);
    }

    private Object dispatchInternal(VirtualFrame frame, REnvironment mtable, RStringVector classes, RFunction fdef, String fname, RFunction f) {
        RFunction method = f;
        if (method == null) {
            // if method has not been found, it will be retrieved by the following R function call
            // and installed in the methods table so that the slow path does not have to be executed
            // again
            CompilerDirectives.transferToInterpreterAndInvalidate();
            REnvironment methodsEnv = REnvironment.getRegisteredNamespace("methods");
            RFunction currentFunction = ReadVariableNode.lookupFunction(".InheritForDispatch", methodsEnv.getFrame(), true);
            method = (RFunction) RContext.getEngine().evalFunction(currentFunction, frame.materialize(), classes, fdef, mtable);
        }
        method = loadMethod.executeRFunction(frame, method, fname);
        Object ret = executeMethod.executeObject(frame, method, fname);
        return ret;
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "equalClasses(classes, cachedClasses)")
    protected Object dispatchCached(VirtualFrame frame, REnvironment mtable, RStringVector classes, RFunction fdef, String fname,
                    @Cached("classes") RStringVector cachedClasses, //
                    @Cached("createDispatchString(cachedClasses)") String dispatchString, //
                    @Cached("createTableRead(dispatchString)") LocalReadVariableNode tableRead, //
                    @Cached("createClassProfile()") ValueProfile frameAccessProfile) {
        RFunction method = (RFunction) tableRead.execute(frame, mtable.getFrame(frameAccessProfile));
        return dispatchInternal(frame, mtable, classes, fdef, fname, method);
    }

    @Specialization(contains = "dispatchCached")
    protected Object dispatch(VirtualFrame frame, REnvironment mtable, RStringVector classes, RFunction fdef, String fname) {
        String dispatchString = createDispatchString(classes);
        RFunction method = (RFunction) mtable.get(dispatchString);
        return dispatchInternal(frame, mtable, classes, fdef, fname, method);
    }

    protected boolean equalClasses(RStringVector classes, RStringVector cachedClasses) {
        if (cachedClasses.getLength() == classes.getLength()) {
            for (int i = 0; i < cachedClasses.getLength(); i++) {
                // TODO: makes sure equality is good enough here, but it's for optimization only
                // anwyay
                if (cachedClasses.getDataAt(i) != classes.getDataAt(i)) {
                    equalsMethodRequired.enter();
                    if (cachedClasses.getDataAt(i).equals(classes.getDataAt(i))) {
                        return true;
                    } else {
                        return false;
                    }
                }
            }
            return true;
        }
        return false;
    }
}
