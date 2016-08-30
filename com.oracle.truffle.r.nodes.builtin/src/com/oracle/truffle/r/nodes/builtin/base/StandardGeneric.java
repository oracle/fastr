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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.*;
import static com.oracle.truffle.r.runtime.RVisibility.CUSTOM;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.COMPLEX;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import java.util.function.Function;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.access.variables.LocalReadVariableNode;
import com.oracle.truffle.r.nodes.access.variables.ReadVariableNode;
import com.oracle.truffle.r.nodes.attributes.AttributeAccess;
import com.oracle.truffle.r.nodes.attributes.AttributeAccessNodeGen;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.function.ClassHierarchyScalarNode;
import com.oracle.truffle.r.nodes.function.ClassHierarchyScalarNodeGen;
import com.oracle.truffle.r.nodes.objects.CollectGenericArgumentsNode;
import com.oracle.truffle.r.nodes.objects.CollectGenericArgumentsNodeGen;
import com.oracle.truffle.r.nodes.objects.DispatchGeneric;
import com.oracle.truffle.r.nodes.objects.DispatchGenericNodeGen;
import com.oracle.truffle.r.nodes.unary.CastNode;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RCaller;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RAttributes;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.env.REnvironment;

// transcribed from src/main/objects.c
@RBuiltin(name = "standardGeneric", visibility = CUSTOM, kind = PRIMITIVE, parameterNames = {"f", "fdef"}, behavior = COMPLEX)
public abstract class StandardGeneric extends RBuiltinNode {

    // TODO: for now, we always go through generic dispatch

    @Child private AttributeAccess genericAttrAccess;
    @Child private FrameFunctions.SysFunction sysFunction;
    @Child private LocalReadVariableNode readMTableFirst = LocalReadVariableNode.create(RRuntime.DOT_ALL_MTABLE, true);
    @Child private LocalReadVariableNode readSigLength = LocalReadVariableNode.create(RRuntime.DOT_SIG_LENGTH, true);
    @Child private LocalReadVariableNode readSigARgs = LocalReadVariableNode.create(RRuntime.DOT_SIG_ARGS, true);
    @Child private CollectGenericArgumentsNode collectArgumentsNode;
    @Child private DispatchGeneric dispatchGeneric = DispatchGenericNodeGen.create();
    @Child private ClassHierarchyScalarNode classNode;

    @Child private CastNode castIntScalar;
    @Child private CastNode castStringScalar;
    {
        CastBuilder builder = new CastBuilder();
        builder.arg(0).asIntegerVector().findFirst(RRuntime.INT_NA);
        builder.arg(1).asStringVector().findFirst(RRuntime.STRING_NA);
        castIntScalar = builder.getCasts()[0];
        castStringScalar = builder.getCasts()[1];
    }

    private final BranchProfile noGenFunFound = BranchProfile.create();
    private final ConditionProfile sameNamesProfile = ConditionProfile.createBinaryProfile();

    private String argClass(Object arg) {
        if (classNode == null) {
            classNode = insert(ClassHierarchyScalarNodeGen.create());
        }
        return classNode.executeString(arg);
    }

    @Override
    protected void createCasts(CastBuilder casts) {
        casts.arg("f").defaultError(RError.Message.GENERIC, "argument to 'standardGeneric' must be a non-empty character string").mustBe(
                        stringValue()).asStringVector().findFirst().mustBe(lengthGt(0));
        Function<Object, Object> argClass = this::argClass;
        casts.arg("fdef").defaultError(RError.SHOW_CALLER, RError.Message.EXPECTED_GENERIC, argClass).allowMissing().asAttributable(true, true, true).mustBe(instanceOf(RFunction.class));
    }

    private Object stdGenericInternal(VirtualFrame frame, String fname, RFunction fdef) {
        MaterializedFrame fnFrame = fdef.getEnclosingFrame();
        REnvironment mtable = (REnvironment) readMTableFirst.execute(frame, fnFrame);
        if (mtable == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            // mtable can be null the first time around, but the following call will initialize it
            // and this slow path should not be executed again
            REnvironment methodsEnv = REnvironment.getRegisteredNamespace("methods");
            RFunction currentFunction = ReadVariableNode.lookupFunction(".getMethodsTable", methodsEnv.getFrame(), true, true);
            mtable = (REnvironment) RContext.getEngine().evalFunction(currentFunction, frame.materialize(), RCaller.create(frame, getOriginalCall()), null, fdef);
        }
        RList sigArgs = (RList) readSigARgs.execute(null, fnFrame);
        int sigLength = (int) castIntScalar.execute(readSigLength.execute(null, fnFrame));
        if (sigLength > sigArgs.getLength()) {
            throw RError.error(this, RError.Message.GENERIC, "'.SigArgs' is shorter than '.SigLength' says it should be");
        }
        if (collectArgumentsNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            collectArgumentsNode = insert(CollectGenericArgumentsNodeGen.create(sigArgs.getDataWithoutCopying(), sigLength));
        }
        RStringVector classes = collectArgumentsNode.execute(frame, sigArgs, sigLength);
        Object ret = dispatchGeneric.executeObject(frame, mtable, classes, fdef, fname);
        return ret;
    }

    private Object getFunction(VirtualFrame frame, String fname, Object fnObj) {
        if (fnObj == RNull.instance) {
            noGenFunFound.enter();
            return null;
        }
        RFunction fn = (RFunction) fnObj;
        Object genObj = null;
        RAttributes attributes = fn.getAttributes();
        if (attributes == null) {
            noGenFunFound.enter();
            return null;
        }
        if (genericAttrAccess == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            genericAttrAccess = insert(AttributeAccessNodeGen.create(RRuntime.GENERIC_ATTR_KEY));
        }
        genObj = genericAttrAccess.execute(attributes);
        if (genObj == null) {
            noGenFunFound.enter();
            return null;
        }
        String gen = (String) castStringScalar.execute(genObj);
        if (sameNamesProfile.profile(gen == fname)) {
            return stdGenericInternal(frame, fname, fn);
        } else {
            // in many cases == is good enough (and this will be the fastest path), but it's not
            // always sufficient
            if (!gen.equals(fname)) {
                noGenFunFound.enter();
                return null;
            }
            return stdGenericInternal(frame, fname, fn);
        }
    }

    @Specialization
    protected Object stdGeneric(VirtualFrame frame, String fname, RFunction fdef) {
        return stdGenericInternal(frame, fname, fdef);
    }

    @Specialization
    protected Object stdGeneric(VirtualFrame frame, String fname, @SuppressWarnings("unused") RMissing fdef) {
        int n = RArguments.getDepth(frame);
        Object fnObj = RArguments.getFunction(frame);
        fnObj = getFunction(frame, fname, fnObj);
        if (fnObj != null) {
            return fnObj;
        }
        if (sysFunction == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            sysFunction = insert(FrameFunctionsFactory.SysFunctionNodeGen.create(null));
            RError.performanceWarning("sys.frame usage in standardGeneric");
        }
        // TODO: GNU R counts to (i < n) - does their equivalent of getDepth return a different
        // value
        // TODO: shouldn't we count from n to 0?
        for (int i = 0; i <= n; i++) {
            fnObj = sysFunction.executeObject(frame, i);
            fnObj = getFunction(frame, fname, fnObj);
            if (fnObj != null) {
                return fnObj;
            }
        }
        throw RError.error(this, RError.Message.STD_GENERIC_WRONG_CALL, fname);
    }

}
