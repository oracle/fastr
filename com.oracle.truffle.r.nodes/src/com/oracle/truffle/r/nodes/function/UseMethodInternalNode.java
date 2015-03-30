/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.function;

import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.function.S3FunctionLookupNode.Result;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.RArguments.S3Args;
import com.oracle.truffle.r.runtime.data.*;

public final class UseMethodInternalNode extends RNode implements VisibilityController {

    @Child private S3FunctionLookupNode lookup = S3FunctionLookupNode.create(false, false);
    @Child private CallMatcherNode callMatcher = CallMatcherNode.create(false, false);

    private final String generic;
    private final ArgumentsSignature signature;

    public UseMethodInternalNode(String generic, ArgumentsSignature signature) {
        this.generic = generic;
        this.signature = signature;
    }

    public Object execute(VirtualFrame frame, RStringVector type, Object[] arguments) {
        controlVisibility();
        Result lookupResult = lookup.execute(frame, generic, type, null, frame.materialize(), null);
        S3Args s3Args = new S3Args(generic, lookupResult.clazz, lookupResult.targetFunctionName, frame.materialize(), null, null);
        return callMatcher.execute(frame, signature, arguments, lookupResult.function, s3Args);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        throw RInternalError.shouldNotReachHere();
    }
}
