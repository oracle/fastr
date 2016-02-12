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
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.access.WriteLocalFrameVariableNode;
import com.oracle.truffle.r.nodes.access.WriteVariableNode;
import com.oracle.truffle.r.nodes.access.variables.LocalReadVariableNode;
import com.oracle.truffle.r.nodes.access.variables.ReadVariableNode;
import com.oracle.truffle.r.nodes.function.signature.RArgumentsNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RCaller;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RAttributes.RAttribute;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

public abstract class LoadMethod extends RBaseNode {

    public abstract RFunction executeRFunction(VirtualFrame frame, RAttributable fdef, String fname);

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
    private final RCaller caller = RDataFactory.createCaller(this);

    @Specialization
    protected RFunction loadMethod(VirtualFrame frame, RFunction fdef, String fname) {
        assert fdef.getAttributes() != null; // should have at least class attribute
        int found = 1;
        for (RAttribute attr : fdef.getAttributes()) {
            String name = attr.getName();
            assert name == name.intern();
            if (name == RRuntime.R_TARGET) {
                writeRTarget.execute(frame, attr.getValue());
                found++;
            } else if (name == RRuntime.R_DEFINED) {
                writeRDefined.execute(frame, attr.getValue());
                found++;
            } else if (name == RRuntime.R_NEXT_METHOD) {
                writeRNextMethod.execute(frame, attr.getValue());
                found++;
            } else if (name == RRuntime.R_SOURCE) {
                found++;
            }
        }
        writeRMethod.execute(frame, fdef);
        if ("loadMethod".equals(fname)) {
            // the loadMethod function contains the following call:
            // standardGeneric("loadMethod")
            // which we are handling here, so == is fine
            return fdef;
        }
        assert !fname.equals("loadMethod");
        RFunction ret;
        if (moreAttributes.profile(found < fdef.getAttributes().size())) {
            RFunction currentFunction;
            REnvironment methodsEnv = (REnvironment) methodsEnvRead.execute(frame, REnvironment.getNamespaceRegistry().getFrame());
            if (loadMethodFind == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                loadMethodFind = insert(ReadVariableNode.createFunctionLookup(null, "loadMethod"));
                currentFunction = (RFunction) loadMethodFind.execute(null, methodsEnv.getFrame());
                loadMethodFunction = currentFunction;
                loadMethodCall = insert(Truffle.getRuntime().createDirectCallNode(loadMethodFunction.getTarget()));
                RError.performanceWarning("loadMethod executing slow path");
            }
            else {
                currentFunction = (RFunction) loadMethodFind.execute(frame, methodsEnv.getFrame());
            }
            if (cached.profile(currentFunction == loadMethodFunction)) {
                Object[] args = argsNode.execute(loadMethodFunction, caller, null, RArguments.getDepth(frame) + 1, new Object[]{fdef, fname, REnvironment.frameToEnvironment(frame.materialize())},
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
