/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995, 1996, 1997  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1995-2014, The R Core Team
 * Copyright (c) 2002-2008, The R Foundation
 * Copyright (c) 2015, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.utilities.BranchProfile;
import com.oracle.truffle.api.utilities.ConditionProfile;
import com.oracle.truffle.r.nodes.RRootNode;
import com.oracle.truffle.r.nodes.access.WriteLocalFrameVariableNode;
import com.oracle.truffle.r.nodes.access.WriteVariableNode;
import com.oracle.truffle.r.nodes.access.variables.ReadVariableNode;
import com.oracle.truffle.r.nodes.access.variables.ReadVariableNode.ReadKind;
import com.oracle.truffle.r.nodes.attributes.AttributeAccess;
import com.oracle.truffle.r.nodes.attributes.AttributeAccessNodeGen;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.function.FormalArguments;
import com.oracle.truffle.r.nodes.function.RMissingHelper;
import com.oracle.truffle.r.nodes.function.signature.RArgumentsNode;
import com.oracle.truffle.r.nodes.objects.CollectGenericArgumentsNode;
import com.oracle.truffle.r.nodes.objects.CollectGenericArgumentsNodeGen;
import com.oracle.truffle.r.nodes.unary.CastIntegerScalarNode;
import com.oracle.truffle.r.nodes.unary.CastStringScalarNode;
import com.oracle.truffle.r.nodes.unary.CastStringScalarNodeGen;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.RAttributes.RAttribute;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.env.frame.FrameSlotChangeMonitor;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
import com.oracle.truffle.r.runtime.nodes.RNode;

// transcribed from src/main/objects.c
@RBuiltin(name = "standardGeneric", kind = PRIMITIVE, parameterNames = {"f", "fdef"})
public abstract class StandardGeneric extends RBuiltinNode {

    // TODO: for now, we always go through generic dispatch

    @Child private AttributeAccess genericAttrAccess;
    @Child private FrameFunctions.SysFunction sysFunction;
    @Child private CastStringScalarNode castStringScalar = CastStringScalarNodeGen.create();
    @Child private ReadVariableNode readMTableFirst = ReadVariableNode.create(RRuntime.DOT_ALL_MTABLE, RType.Any, ReadKind.SilentLocal);
    @Child private ReadVariableNode readMTableSecond = ReadVariableNode.create(RRuntime.DOT_ALL_MTABLE, RType.Any, ReadKind.SilentLocal);
    @Child private ReadVariableNode readSigLength = ReadVariableNode.create(RRuntime.DOT_SIG_LENGTH, RType.Any, ReadKind.SilentLocal);
    @Child private ReadVariableNode readSigARgs = ReadVariableNode.create(RRuntime.DOT_SIG_ARGS, RType.Any, ReadKind.SilentLocal);
    @Child private ReadVariableNode getMethodsTableFind = ReadVariableNode.create(".getMethodsTable", RType.Function, ReadKind.Normal);
    @Child private DirectCallNode getMethodsTableCall;
    @Child private RArgumentsNode argsNode = RArgumentsNode.create();
    @Child private CastIntegerScalarNode castIntScalar = CastIntegerScalarNode.create();
    @Child private CollectGenericArgumentsNode collectArgumentsNode;
    @Child private DispatchGeneric dispatchGeneric = DispatchGenericNodeGen.create();
    @CompilationFinal private RFunction getMethodsTableFunction;
    private final ConditionProfile cached = ConditionProfile.createBinaryProfile();
    private final RCaller caller = RDataFactory.createCaller(this);
    private final BranchProfile initialize = BranchProfile.create();

    private final RAttributeProfiles attrProfiles = RAttributeProfiles.create();

    @Specialization(guards = "fVec.getLength() > 0")
    protected Object stdGeneric(VirtualFrame frame, RAbstractStringVector fVec, RFunction fdef) {
        controlVisibility();
        String fname = fVec.getDataAt(0);
        REnvironment methodsEnv = REnvironment.getRegisteredNamespace("methods");
        MaterializedFrame fnFrame = fdef.getEnclosingFrame();
        REnvironment mtable = (REnvironment) readMTableFirst.execute(null, fnFrame);
        if (mtable == null) {
            // sometimes this branch seems to be never taken (in particular when initializing a
            // freshly created object) which happens via a generic
            initialize.enter();
            if (getMethodsTableFunction == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getMethodsTableFunction = (RFunction) getMethodsTableFind.execute(null, methodsEnv.getFrame());
                getMethodsTableCall = insert(Truffle.getRuntime().createDirectCallNode(getMethodsTableFunction.getTarget()));
            }
            RFunction currentFunction = (RFunction) getMethodsTableFind.execute(null, methodsEnv.getFrame());
            if (cached.profile(currentFunction == getMethodsTableFunction)) {
                Object[] args = argsNode.execute(getMethodsTableFunction, caller, null, RArguments.getDepth(frame) + 1, new Object[]{fdef}, ArgumentsSignature.get("fdef"), null);
                getMethodsTableCall.call(frame, args);
            } else {
                // slow path
                RContext.getEngine().evalFunction(currentFunction, frame.materialize(), fdef);
            }
            // TODO: can we use a single ReadVariableNode for getting mtable?
            mtable = (REnvironment) readMTableSecond.execute(null, fnFrame);
        }
        RList sigArgs = (RList) readSigARgs.execute(null, fnFrame);
        int sigLength = castIntScalar.executeInt(readSigLength.execute(null, fnFrame));
        if (sigLength > sigArgs.getLength()) {
            throw RError.error(this, RError.Message.GENERIC, "'.SigArgs' is shorter than '.SigLength' says it should be");
        }
        if (collectArgumentsNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            collectArgumentsNode = insert(CollectGenericArgumentsNodeGen.create(sigArgs.getDataWithoutCopying(), sigLength));
        }

        String[] classes = collectArgumentsNode.execute(frame, sigArgs, sigLength);
        Object ret = dispatchGeneric.executeObject(frame, methodsEnv, mtable, RDataFactory.createStringVector(classes, RDataFactory.COMPLETE_VECTOR), fdef, fname);
        return ret;
    }

    @Specialization(guards = "fVec.getLength() > 0")
    protected Object stdGeneric(VirtualFrame frame, RAbstractStringVector fVec, @SuppressWarnings("unused") RMissing fdef) {
        String fname = fVec.getDataAt(0);
        int n = RArguments.getDepth(frame);
        if (genericAttrAccess == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            assert sysFunction == null;
            genericAttrAccess = insert(AttributeAccessNodeGen.create(RRuntime.GENERIC_ATTR_KEY));
            sysFunction = insert(FrameFunctionsFactory.SysFunctionNodeGen.create(new RNode[1], null, null));
        }
        // TODO: GNU R counts to (i < n) - does their equivalent of getDepth return a different
        // value
        // TODO; shouldn't we count from n to 0?
        for (int i = 0; i <= n; i++) {
            Object fnObj = sysFunction.executeObject(frame, i);
            if (fnObj != RNull.instance) {
                RFunction fn = (RFunction) fnObj;
                Object genObj = null;
                RAttributes attributes = fn.getAttributes();
                if (attributes == null) {
                    continue;
                }
                genObj = genericAttrAccess.execute(attributes);
                if (genObj == null) {
                    continue;
                }
                String gen = castStringScalar.executeString(genObj);
                if (gen.equals(fname)) {
                    return stdGeneric(frame, fVec, fn);
                }
            }
        }
        controlVisibility();
        throw RError.error(this, RError.Message.STD_GENERIC_WRONG_CALL, fname);
    }

    @Specialization
    protected Object stdGeneric(Object fVec, RAttributable fdef) {
        controlVisibility();
        if (!(fVec instanceof String || (fVec instanceof RAbstractStringVector && ((RAbstractStringVector) fVec).getLength() > 0))) {
            throw RError.error(this, RError.Message.GENERIC, "argument to 'standardGeneric' must be a non-empty character string");
        } else {
            RStringVector cl = fdef.getClassAttr(attrProfiles);
            // not a GNU R error message
            throw RError.error(this, RError.Message.EXPECTED_GENERIC, cl.getLength() == 0 ? RRuntime.STRING_NA : cl.getDataAt(0));
        }
    }

}

abstract class DispatchGeneric extends RBaseNode {

    public abstract Object executeObject(VirtualFrame frame, REnvironment methodsEnv, REnvironment mtable, RStringVector classes, RFunction fdef, String fname);

    private final ConditionProfile singleStringProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile cached = ConditionProfile.createBinaryProfile();
    private final RCaller caller = RDataFactory.createCaller(this);
    @Child private ReadVariableNode inheritForDispatchFind;
    @Child private DirectCallNode inheritForDispatchCall;
    @CompilationFinal private RFunction inheritForDispatchFunction;
    @Child private RArgumentsNode argsNode = RArgumentsNode.create();
    @Child private LoadMethod loadMethod = LoadMethodNodeGen.create();
    @Child private ExecuteMethod executeMethod = ExecuteMethodNodeGen.create();

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

    protected ReadVariableNode createTableRead(String dispatchString) {
        return ReadVariableNode.create(dispatchString, RType.Any, ReadKind.SilentLocal);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "equalClasses(classes, cachedClasses)")
    protected Object dispatch(VirtualFrame frame, REnvironment methodsEnv, REnvironment mtable, RStringVector classes, RFunction fdef, String fname, @Cached("classes") RStringVector cachedClasses,
                    @Cached("createDispatchString(cachedClasses)") String dispatchString, @Cached("createTableRead(dispatchString)") ReadVariableNode tableRead) {
        RFunction method = (RFunction) tableRead.execute(null, mtable.getFrame());
        if (method == null) {
            if (inheritForDispatchFind == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                inheritForDispatchFind = insert(ReadVariableNode.create(".InheritForDispatch", RType.Function, ReadKind.Normal));
                inheritForDispatchFunction = (RFunction) inheritForDispatchFind.execute(null, methodsEnv.getFrame());
                inheritForDispatchCall = insert(Truffle.getRuntime().createDirectCallNode(inheritForDispatchFunction.getTarget()));

            }
            RFunction currentFunction = (RFunction) inheritForDispatchFind.execute(null, methodsEnv.getFrame());
            if (cached.profile(currentFunction == inheritForDispatchFunction)) {
                Object[] args = argsNode.execute(inheritForDispatchFunction, caller, null, RArguments.getDepth(frame) + 1, new Object[]{classes, fdef, mtable},
                                ArgumentsSignature.get("classes", "fdef", "mtable"), null);
                method = (RFunction) inheritForDispatchCall.call(frame, args);
            } else {
                // slow path
                method = (RFunction) RContext.getEngine().evalFunction(currentFunction, frame.materialize(), classes, fdef, mtable);
            }
        }
        method = loadMethod.executeRFunction(frame, methodsEnv, method, fname);
        Object ret = executeMethod.executeMethod(frame, method);
        return ret;
    }

    protected boolean equalClasses(RStringVector classes, RStringVector cachedClasses) {
        if (cachedClasses.getLength() == classes.getLength()) {
            for (int i = 0; i < cachedClasses.getLength(); i++) {
                // TODO: makes sure equality is good enough here, but it's for optimization only
                // anwyay
                if (cachedClasses.getDataAt(i) != classes.getDataAt(i)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
}

abstract class LoadMethod extends RBaseNode {

    public abstract RFunction executeRFunction(VirtualFrame frame, REnvironment methodsEnv, RAttributable fdef, String fname);

    @Child private WriteLocalFrameVariableNode writeRTarget = WriteLocalFrameVariableNode.create(RRuntime.R_DOT_TARGET, null, WriteVariableNode.Mode.REGULAR);
    @Child private WriteLocalFrameVariableNode writeRDefined = WriteLocalFrameVariableNode.create(RRuntime.R_DOT_DEFINED, null, WriteVariableNode.Mode.REGULAR);
    @Child private WriteLocalFrameVariableNode writeRNextMethod = WriteLocalFrameVariableNode.create(RRuntime.R_DOT_NEXT_METHOD, null, WriteVariableNode.Mode.REGULAR);
    @Child private WriteLocalFrameVariableNode writeRMethod = WriteLocalFrameVariableNode.create(RRuntime.RDotMethod, null, WriteVariableNode.Mode.REGULAR);
    @Child private ReadVariableNode loadMethodFind;
    @Child private DirectCallNode loadMethodCall;
    @CompilationFinal private RFunction loadMethodFunction;
    @Child private RArgumentsNode argsNode = RArgumentsNode.create();
    private final ConditionProfile cached = ConditionProfile.createBinaryProfile();
    private final ConditionProfile moreAttributes = ConditionProfile.createBinaryProfile();
    private final RCaller caller = RDataFactory.createCaller(this);

    @Specialization
    protected RFunction loadMethod(VirtualFrame frame, REnvironment methodsEnv, RFunction fdef, String fname) {
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
            if (loadMethodFind == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                loadMethodFind = insert(ReadVariableNode.create("loadMethod", RType.Function, ReadKind.Normal));
                loadMethodFunction = (RFunction) loadMethodFind.execute(null, methodsEnv.getFrame());
                loadMethodCall = insert(Truffle.getRuntime().createDirectCallNode(loadMethodFunction.getTarget()));

            }
            RFunction currentFunction = (RFunction) loadMethodFind.execute(null, methodsEnv.getFrame());
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

abstract class ExecuteMethod extends RBaseNode {

    public abstract Object executeObject(VirtualFrame frame, RFunction fdef);

    @Child private ReadVariableNode readDefined = ReadVariableNode.create(RRuntime.R_DOT_DEFINED, RType.Any, ReadKind.SilentLocal);
    @Child private ReadVariableNode readMethod = ReadVariableNode.create(RRuntime.RDotMethod, RType.Any, ReadKind.SilentLocal);
    @Child private ReadVariableNode readTarget = ReadVariableNode.create(RRuntime.R_DOT_TARGET, RType.Any, ReadKind.SilentLocal);
    @Child private ReadVariableNode readGeneric = ReadVariableNode.create(RRuntime.RDotGeneric, RType.Any, ReadKind.SilentLocal);
    @Child private ReadVariableNode readMethods = ReadVariableNode.create(RRuntime.R_DOT_METHODS, RType.Any, ReadKind.SilentLocal);
    @Child private RArgumentsNode argsNode = RArgumentsNode.create();

    @Specialization
    protected Object executeMethod(VirtualFrame frame, RFunction fdef) {

        Object[] args = argsNode.execute(RArguments.getFunction(frame), RArguments.getCall(frame), null, RArguments.getDepth(frame) + 1, RArguments.getArguments(frame),
                        RArguments.getSignature(frame), null);
        MaterializedFrame newFrame = Truffle.getRuntime().createMaterializedFrame(args);
        FrameDescriptor desc = newFrame.getFrameDescriptor();
        FrameSlotChangeMonitor.initializeFunctionFrameDescriptor(desc);
        FormalArguments formals = ((RRootNode) fdef.getRootNode()).getFormalArguments();
        if (formals != null) {
            ArgumentsSignature signature = formals.getSignature();
            MaterializedFrame currentFrame = frame.materialize();
            FrameDescriptor currentFrameDesc = currentFrame.getFrameDescriptor();
            for (int i = 0; i < signature.getLength(); i++) {
                String argName = signature.getName(i);
                boolean missing = RMissingHelper.isMissingArgument(frame, argName);
                Object val = slotRead(currentFrame, currentFrameDesc, argName);
                slotInit(newFrame, desc, argName, val);
                if (missing && !(val instanceof RArgsValuesAndNames)) {
                    throw RInternalError.unimplemented();
                }
            }
        }

        slotInit(newFrame, desc, RRuntime.R_DOT_DEFINED, readDefined.execute(frame));
        slotInit(newFrame, desc, RRuntime.RDotMethod, readDefined.execute(frame));
        slotInit(newFrame, desc, RRuntime.R_DOT_TARGET, readDefined.execute(frame));
        slotInit(newFrame, desc, RRuntime.RDotGeneric, readDefined.execute(frame));
        slotInit(newFrame, desc, RRuntime.R_DOT_METHODS, readDefined.execute(frame));

        Object ret = callMethod(fdef, newFrame);
        return ret;
    }

    @TruffleBoundary
    static Object callMethod(RFunction fdef, MaterializedFrame newFrame) {
        return RContext.getEngine().evalGeneric(fdef, newFrame);
    }

    @TruffleBoundary
    static Object slotRead(MaterializedFrame currentFrame, FrameDescriptor desc, String name) {
        FrameSlot slot = desc.findFrameSlot(name);
        return currentFrame.getValue(slot);
    }

    @TruffleBoundary
    static void slotInit(MaterializedFrame newFrame, FrameDescriptor desc, String name, Object value) {
        if (value instanceof Byte) {
            FrameSlot frameSlot = FrameSlotChangeMonitor.findOrAddFrameSlot(desc, name, FrameSlotKind.Byte);
            newFrame.setByte(frameSlot, (byte) value);
        } else if (value instanceof Integer) {
            FrameSlot frameSlot = FrameSlotChangeMonitor.findOrAddFrameSlot(desc, name, FrameSlotKind.Int);
            newFrame.setInt(frameSlot, (int) value);
        } else if (value instanceof Double) {
            FrameSlot frameSlot = FrameSlotChangeMonitor.findOrAddFrameSlot(desc, name, FrameSlotKind.Double);
            newFrame.setDouble(frameSlot, (double) value);
        } else {
            FrameSlot frameSlot = FrameSlotChangeMonitor.findOrAddFrameSlot(desc, name, FrameSlotKind.Object);
            newFrame.setObject(frameSlot, value);

        }
    }
}
