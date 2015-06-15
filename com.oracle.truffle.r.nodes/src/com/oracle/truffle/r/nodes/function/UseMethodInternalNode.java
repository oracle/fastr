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

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.function.S3FunctionLookupNode.Result;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.RArguments.S3Args;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

public final class UseMethodInternalNode extends RNode implements VisibilityController {

    @Child private ClassHierarchyNode classHierarchyNode = ClassHierarchyNodeGen.create();
    @Child private S3FunctionLookupNode lookup = S3FunctionLookupNode.create(false, false);
    @Child private CallMatcherNode callMatcher = CallMatcherNode.create(false, false);

    @CompilationFinal private final BranchProfile[] everSeenShared;
    @CompilationFinal private final BranchProfile[] everSeenTemporary;
    @CompilationFinal private final BranchProfile[] everSeenNonTemporary;

    private final String generic;
    private final ArgumentsSignature signature;
    private final boolean wrap;

    public UseMethodInternalNode(String generic, ArgumentsSignature signature, boolean wrap) {
        this.generic = generic;
        this.signature = signature;
        this.wrap = wrap && FastROptions.InvisibleArgs.getValue();
        if (this.wrap) {
            int len = signature.getLength();
            everSeenShared = new BranchProfile[len];
            everSeenTemporary = new BranchProfile[len];
            everSeenNonTemporary = new BranchProfile[len];
            for (int i = 0; i < signature.getLength(); i++) {
                everSeenShared[i] = BranchProfile.create();
                everSeenTemporary[i] = BranchProfile.create();
                everSeenNonTemporary[i] = BranchProfile.create();
            }
        } else {
            everSeenShared = null;
            everSeenTemporary = null;
            everSeenNonTemporary = null;
        }
    }

    public Object execute(VirtualFrame frame, RAbstractContainer dispatchOn, Object[] arguments) {
        RStringVector type = classHierarchyNode.execute(dispatchOn);
        controlVisibility();
        Result lookupResult = lookup.execute(frame, generic, type, null, frame.materialize(), null);
        if (wrap) {
            assert arguments != null && arguments.length == signature.getLength();
            for (int i = 0; i < arguments.length; i++) {
                Utils.transitionState(arguments[i], everSeenShared[i], everSeenTemporary[i], everSeenNonTemporary[i]);
            }
        }
        S3Args s3Args = new S3Args(generic, lookupResult.clazz, lookupResult.targetFunctionName, frame.materialize(), null, null);
        return callMatcher.execute(frame, signature, arguments, lookupResult.function, s3Args);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        throw RInternalError.shouldNotReachHere();
    }
}
