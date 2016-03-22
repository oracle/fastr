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
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.r.nodes.RRootNode;
import com.oracle.truffle.r.nodes.access.variables.LocalReadVariableNode;
import com.oracle.truffle.r.nodes.access.variables.ReadVariableNode;
import com.oracle.truffle.r.nodes.function.CallMatcherNode;
import com.oracle.truffle.r.nodes.function.FormalArguments;
import com.oracle.truffle.r.nodes.function.signature.CollectArgumentsNode;
import com.oracle.truffle.r.nodes.function.signature.CollectArgumentsNodeGen;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RArguments.S4Args;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

final class ExecuteMethod extends RBaseNode {

    @Child private LocalReadVariableNode readDefined = LocalReadVariableNode.create(RRuntime.R_DOT_DEFINED, true);
    @Child private LocalReadVariableNode readMethod = LocalReadVariableNode.create(RRuntime.R_DOT_METHOD, true);
    @Child private LocalReadVariableNode readTarget = LocalReadVariableNode.create(RRuntime.R_DOT_TARGET, true);
    @Child private ReadVariableNode readGeneric = ReadVariableNode.create(RRuntime.R_DOT_GENERIC);
    @Child private ReadVariableNode readMethods = ReadVariableNode.create(RRuntime.R_DOT_METHODS);

    @Child private CollectArgumentsNode collectArgs;
    @Child private CallMatcherNode callMatcher;

    public Object executeObject(VirtualFrame frame, RFunction fdef) {
        if (collectArgs == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            collectArgs = insert(CollectArgumentsNodeGen.create());
            callMatcher = insert(CallMatcherNode.create(false, false));
        }

        FormalArguments formals = ((RRootNode) fdef.getRootNode()).getFormalArguments();
        ArgumentsSignature signature = formals.getSignature();
        Object[] oldArgs = collectArgs.execute(frame, signature);

        S4Args s4Args = new S4Args(readDefined.execute(frame), readMethod.execute(frame), readTarget.execute(frame), readGeneric.execute(frame), readMethods.execute(frame));

        return callMatcher.execute(frame, signature, oldArgs, fdef, s4Args);
    }
}
