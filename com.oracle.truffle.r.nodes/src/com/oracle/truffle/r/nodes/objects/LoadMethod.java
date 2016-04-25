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
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.nodes.access.WriteLocalFrameVariableNode;
import com.oracle.truffle.r.nodes.access.WriteVariableNode;
import com.oracle.truffle.r.nodes.access.variables.LocalReadVariableNode;
import com.oracle.truffle.r.nodes.access.variables.ReadVariableNode;
import com.oracle.truffle.r.nodes.attributes.AttributeAccess;
import com.oracle.truffle.r.nodes.attributes.AttributeAccessNodeGen;
import com.oracle.truffle.r.nodes.function.RCallerHelper;
import com.oracle.truffle.r.nodes.function.signature.RArgumentsNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RCaller;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RAttributes;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

abstract class LoadMethod extends RBaseNode {

    public abstract RFunction executeRFunction(VirtualFrame frame, RAttributable fdef, String fname);

    @Child private AttributeAccess targetAttrAccess = AttributeAccessNodeGen.create(RRuntime.R_TARGET);
    @Child private AttributeAccess definedAttrAccess = AttributeAccessNodeGen.create(RRuntime.R_DEFINED);
    @Child private AttributeAccess nextMethodAttrAccess = AttributeAccessNodeGen.create(RRuntime.R_NEXT_METHOD);
    @Child private AttributeAccess sourceAttrAccess = AttributeAccessNodeGen.create(RRuntime.R_SOURCE);
    @Child private WriteLocalFrameVariableNode writeRTarget = WriteLocalFrameVariableNode.create(RRuntime.R_DOT_TARGET, null, WriteVariableNode.Mode.REGULAR);
    @Child private WriteLocalFrameVariableNode writeRDefined = WriteLocalFrameVariableNode.create(RRuntime.R_DOT_DEFINED, null, WriteVariableNode.Mode.REGULAR);
    @Child private WriteLocalFrameVariableNode writeRNextMethod = WriteLocalFrameVariableNode.create(RRuntime.R_DOT_NEXT_METHOD, null, WriteVariableNode.Mode.REGULAR);
    @Child private WriteLocalFrameVariableNode writeRMethod = WriteLocalFrameVariableNode.create(RRuntime.R_DOT_METHOD, null, WriteVariableNode.Mode.REGULAR);
    @Child private LocalReadVariableNode methodsEnvRead = LocalReadVariableNode.create("methods", true);
    @Child private ReadVariableNode loadMethodFind;
    @Child private DirectCallNode loadMethodCall;
    @CompilationFinal private RFunction loadMethodFunction;
    @Child private RArgumentsNode argsNode = RArgumentsNode.create();
    private final ConditionProfile cached = ConditionProfile.createBinaryProfile();
    private final ConditionProfile moreAttributes = ConditionProfile.createBinaryProfile();
    private final ConditionProfile noNextMethodAttr = ConditionProfile.createBinaryProfile();
    private final ConditionProfile noTargetAttr = ConditionProfile.createBinaryProfile();
    private final ConditionProfile noDefinedAttr = ConditionProfile.createBinaryProfile();
    private final BranchProfile noSourceAttr = BranchProfile.create();
    // TODO: technically, someone could override loadMethod function and access the caller, but it's
    // rather unlikely
    private final RCaller caller = RDataFactory.createCaller(RCallerHelper.InvalidRepresentation.instance);

    @Specialization
    protected RFunction loadMethod(VirtualFrame frame, RFunction fdef, String fname, //
                    @Cached("createClassProfile()") ValueProfile regFrameAccessProfile, //
                    @Cached("createClassProfile()") ValueProfile methodsFrameAccessProfile) {
        RAttributes attributes = fdef.getAttributes();
        assert attributes != null; // should have at least class attribute
        int found;
        Object nextMethodAttr = nextMethodAttrAccess.execute(attributes);
        // it's an optimization only where it's expected that either 2 or 4 attributes total will be
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

        writeRMethod.execute(frame, fdef);
        if (fname == RRuntime.R_LOAD_METHOD_NAME) {
            // the loadMethod function contains the following call:
            // standardGeneric("loadMethod")
            // which we are handling here, so == is fine
            return fdef;
        }
        assert !fname.equals(RRuntime.R_LOAD_METHOD_NAME);
        RFunction ret;
        if (moreAttributes.profile(found < fdef.getAttributes().size())) {
            RFunction currentFunction;
            REnvironment methodsEnv = (REnvironment) methodsEnvRead.execute(frame, REnvironment.getNamespaceRegistry().getFrame(regFrameAccessProfile));
            if (loadMethodFind == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                loadMethodFind = insert(ReadVariableNode.createFunctionLookup(RSyntaxNode.INTERNAL, RRuntime.R_LOAD_METHOD_NAME));
                currentFunction = (RFunction) loadMethodFind.execute(null, methodsEnv.getFrame());
                loadMethodFunction = currentFunction;
                loadMethodCall = insert(Truffle.getRuntime().createDirectCallNode(loadMethodFunction.getTarget()));
                RError.performanceWarning("loadMethod executing slow path");
            } else {
                currentFunction = (RFunction) loadMethodFind.execute(frame, methodsEnv.getFrame(methodsFrameAccessProfile));
            }
            if (cached.profile(currentFunction == loadMethodFunction)) {
                Object[] args = argsNode.execute(loadMethodFunction, caller, null, RArguments.getDepth(frame) + 1, RArguments.getPromiseFrame(frame),
                                new Object[]{fdef, fname, REnvironment.frameToEnvironment(frame.materialize())},
                                ArgumentsSignature.get("method", "fname", "envir"), null);
                ret = (RFunction) loadMethodCall.call(frame, args);
            } else {
                // slow path
                ret = (RFunction) RContext.getEngine().evalFunction(currentFunction, frame.materialize(), fdef, fname, REnvironment.frameToEnvironment(frame.materialize()));
            }

        } else {
            ret = fdef;
        }
        return ret;
    }
}
