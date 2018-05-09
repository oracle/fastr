/*
 * Copyright (c) 1995, 1996, 1997  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1995-2014, The R Core Team
 * Copyright (c) 2002-2008, The R Foundation
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates
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
package com.oracle.truffle.r.nodes.objects;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.nodes.access.variables.LocalReadVariableNode;
import com.oracle.truffle.r.nodes.access.variables.ReadVariableNode;
import com.oracle.truffle.r.nodes.function.CallMatcherNode;
import com.oracle.truffle.r.nodes.function.signature.CollectArgumentsNode;
import com.oracle.truffle.r.nodes.function.signature.CollectArgumentsNodeGen;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RArguments;
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

    private final ValueProfile functionProfile = ValueProfile.createClassProfile();
    private final ValueProfile signatureProfile = ValueProfile.createIdentityProfile();

    public Object executeObject(VirtualFrame frame, RFunction fdef, String fname) {
        if (collectArgs == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            collectArgs = insert(CollectArgumentsNodeGen.create());
        }
        ArgumentsSignature signature = signatureProfile.profile(RArguments.getSignature(frame, functionProfile));

        // Collect arguments; we cannot use the arguments of the original call because there might
        // be overriding default arguments.
        Object[] cumulativeArgs = collectArgs.execute(frame, signature);

        if (callMatcher == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            callMatcher = insert(CallMatcherNode.create(false));
        }

        S4Args s4Args = new S4Args(readDefined.execute(frame), readMethod.execute(frame), readTarget.execute(frame), readGeneric.execute(frame), readMethods.execute(frame));
        return callMatcher.execute(frame, signature, cumulativeArgs, fdef, fname, s4Args);
    }
}
