/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.function;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.r.nodes.function.S3FunctionLookupNode.Result;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.FastROptions;
import com.oracle.truffle.r.runtime.RArguments.S3Args;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.nodes.RNode;

public final class UseMethodInternalNode extends RNode {

    @Child private ClassHierarchyNode classHierarchyNode = ClassHierarchyNodeGen.create(true, true);
    @Child private S3FunctionLookupNode lookup = S3FunctionLookupNode.create(false, false);
    @Child private CallMatcherNode callMatcher = CallMatcherNode.create(false);
    @Child private PreProcessArgumentsNode argPreProcess;

    private final String generic;
    private final ArgumentsSignature signature;
    private final boolean wrap;

    public UseMethodInternalNode(String generic, ArgumentsSignature signature, boolean wrap) {
        this.generic = generic;
        this.signature = signature;
        this.wrap = wrap && FastROptions.InvisibleArgs.getBooleanValue();
    }

    public Object execute(VirtualFrame frame, RStringVector type, Object[] arguments) {
        Result lookupResult = lookup.execute(frame, generic, type, null, frame.materialize(), null);
        if (wrap) {
            assert arguments != null;
            if (argPreProcess == null || argPreProcess.getLength() != arguments.length) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                argPreProcess = insert(PreProcessArgumentsNode.create(arguments.length));
            }
            argPreProcess.execute(frame, arguments);
        }
        S3Args s3Args = new S3Args(generic, lookupResult.clazz, lookupResult.targetFunctionName, frame.materialize(), null, null);
        return callMatcher.execute(frame, signature, arguments, lookupResult.function, lookupResult.targetFunctionName, s3Args);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        throw RInternalError.shouldNotReachHere();
    }
}
