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
package com.oracle.truffle.r.nodes.objects;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.nodes.RASTUtils;
import com.oracle.truffle.r.nodes.access.WriteLocalFrameVariableNode;
import com.oracle.truffle.r.nodes.access.WriteVariableNode;
import com.oracle.truffle.r.nodes.access.variables.LocalReadVariableNode;
import com.oracle.truffle.r.nodes.access.variables.ReadVariableNode;
import com.oracle.truffle.r.nodes.attributes.GetFixedAttributeNode;
import com.oracle.truffle.r.nodes.function.call.CallRFunctionNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RCaller;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxElement;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

// transcribed from /src/library/methods/src/methods_list_dispatch.c (R_loadMethod function)
abstract class LoadMethod extends RBaseNode {

    private static final ArgumentsSignature SIGNATURE = ArgumentsSignature.get("method", "fname", "envir");

    public abstract RFunction executeRFunction(VirtualFrame frame, RAttributable fdef, String fname);

    @Child private GetFixedAttributeNode targetAttrAccess = GetFixedAttributeNode.create(RRuntime.R_TARGET);
    @Child private GetFixedAttributeNode definedAttrAccess = GetFixedAttributeNode.create(RRuntime.R_DEFINED);
    @Child private GetFixedAttributeNode nextMethodAttrAccess = GetFixedAttributeNode.create(RRuntime.R_NEXT_METHOD);
    @Child private GetFixedAttributeNode sourceAttrAccess = GetFixedAttributeNode.create(RRuntime.R_SOURCE);
    @Child private WriteLocalFrameVariableNode writeRTarget = WriteLocalFrameVariableNode.create(RRuntime.R_DOT_TARGET, WriteVariableNode.Mode.REGULAR, null);
    @Child private WriteLocalFrameVariableNode writeRDefined = WriteLocalFrameVariableNode.create(RRuntime.R_DOT_DEFINED, WriteVariableNode.Mode.REGULAR, null);
    @Child private WriteLocalFrameVariableNode writeRNextMethod = WriteLocalFrameVariableNode.create(RRuntime.R_DOT_NEXT_METHOD, WriteVariableNode.Mode.REGULAR, null);
    @Child private WriteLocalFrameVariableNode writeRMethod = WriteLocalFrameVariableNode.create(RRuntime.R_DOT_METHOD, WriteVariableNode.Mode.REGULAR, null);
    @Child private LocalReadVariableNode methodsEnvRead = LocalReadVariableNode.create("methods", true);
    @Child private ReadVariableNode loadMethodFind;
    @Child private CallRFunctionNode loadMethodCall;
    @CompilationFinal private RFunction loadMethodFunction;
    private final ConditionProfile cached = ConditionProfile.createBinaryProfile();
    private final ConditionProfile moreAttributes = ConditionProfile.createBinaryProfile();
    private final ConditionProfile noNextMethodAttr = ConditionProfile.createBinaryProfile();
    private final ConditionProfile noTargetAttr = ConditionProfile.createBinaryProfile();
    private final ConditionProfile noDefinedAttr = ConditionProfile.createBinaryProfile();
    private final BranchProfile noSourceAttr = BranchProfile.create();

    @Specialization
    protected RFunction loadMethod(VirtualFrame frame, RFunction fdef, String fname,
                    @Cached("createClassProfile()") ValueProfile regFrameAccessProfile,
                    @Cached("createClassProfile()") ValueProfile methodsFrameAccessProfile) {
        DynamicObject attributes = fdef.getAttributes();
        assert fdef.isBuiltin() || attributes != null;
        int found;
        if (attributes != null) {
            Object nextMethodAttr = nextMethodAttrAccess.execute(attributes);
            // it's an optimization only where it's expected that either 2 or 4 attributes total
            // will be
            // present - anything else triggers execution of a generic S4 function
            if (noNextMethodAttr.profile(nextMethodAttr == null)) {
                found = 4; // class attribute plus three others are expected
                Object targetAttr = targetAttrAccess.execute(attributes);
                if (noTargetAttr.profile(targetAttr == null)) {
                    found--;
                } else {
                    writeRTarget.execute(frame, targetAttr);
                }
                Object definedAttr = definedAttrAccess.execute(attributes);
                if (noDefinedAttr.profile(definedAttr == null)) {
                    found--;
                } else {
                    writeRDefined.execute(frame, definedAttr);
                }
                Object sourceAttr = sourceAttrAccess.execute(attributes);
                if (sourceAttr == null) {
                    noSourceAttr.enter();
                    found--;
                }
            } else {
                found = 2; // next method attribute and class attribute
                writeRNextMethod.execute(frame, nextMethodAttr);
            }
        } else {
            found = 0;
        }

        writeRMethod.execute(frame, fdef);
        if (fname == RRuntime.R_LOAD_METHOD_NAME) {
            // the loadMethod function contains the following call:
            // standardGeneric("loadMethod")
            // which we are handling here, so == is fine
            return fdef;
        }
        assert !fname.equals(RRuntime.R_LOAD_METHOD_NAME);
        RFunction ret;
        if (fdef.getAttributes() != null && moreAttributes.profile(found < fdef.getAttributes().size())) {
            RFunction currentFunction;
            REnvironment methodsEnv = (REnvironment) methodsEnvRead.execute(frame, REnvironment.getNamespaceRegistry().getFrame(regFrameAccessProfile));
            if (loadMethodFind == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                loadMethodFind = insert(ReadVariableNode.createFunctionLookup(RSyntaxNode.INTERNAL, RRuntime.R_LOAD_METHOD_NAME));
                currentFunction = (RFunction) loadMethodFind.execute(frame, methodsEnv.getFrame());
                loadMethodFunction = currentFunction;
                loadMethodCall = insert(CallRFunctionNode.create(loadMethodFunction.getTarget()));
                RError.performanceWarning("loadMethod executing slow path");
            } else {
                currentFunction = (RFunction) loadMethodFind.execute(frame, methodsEnv.getFrame(methodsFrameAccessProfile));
            }
            RSyntaxElement originalCall = RASTUtils.getOriginalCall(this);
            RCaller caller = originalCall == null ? RCaller.createInvalid(frame) : RCaller.create(frame, originalCall);
            if (cached.profile(currentFunction == loadMethodFunction)) {
                // TODO: technically, someone could override loadMethod function and access the
                // caller, but it's rather unlikely
                ret = (RFunction) loadMethodCall.execute(frame, loadMethodFunction, caller, null, new Object[]{fdef, fname, REnvironment.frameToEnvironment(frame.materialize())}, SIGNATURE,
                                loadMethodFunction.getEnclosingFrame(), null);
            } else {
                // slow path
                ret = (RFunction) RContext.getEngine().evalFunction(currentFunction, frame.materialize(), caller, true, null, fdef, fname, REnvironment.frameToEnvironment(frame.materialize()));
            }
        } else {
            ret = fdef;
        }
        return ret;
    }
}
