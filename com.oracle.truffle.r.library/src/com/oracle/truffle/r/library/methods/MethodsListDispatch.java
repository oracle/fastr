/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.methods;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.instanceOf;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.lengthGt;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.lengthGte;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.singleElement;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;

import java.util.function.Function;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.r.library.methods.MethodsListDispatchFactory.GetGenericInternalNodeGen;
import com.oracle.truffle.r.nodes.access.AccessSlotNode;
import com.oracle.truffle.r.nodes.access.AccessSlotNodeGen;
import com.oracle.truffle.r.nodes.access.variables.LocalReadVariableNode;
import com.oracle.truffle.r.nodes.access.variables.ReadVariableNode;
import com.oracle.truffle.r.nodes.attributes.GetFixedAttributeNode;
import com.oracle.truffle.r.nodes.builtin.NodeWithArgumentCasts.Casts;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.nodes.function.ClassHierarchyScalarNode;
import com.oracle.truffle.r.nodes.function.ClassHierarchyScalarNodeGen;
import com.oracle.truffle.r.nodes.function.PromiseHelperNode;
import com.oracle.truffle.r.nodes.function.RCallNode;
import com.oracle.truffle.r.nodes.unary.CastToVectorNode;
import com.oracle.truffle.r.nodes.unary.CastToVectorNodeGen;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.PrimitiveMethodsInfo;
import com.oracle.truffle.r.runtime.PrimitiveMethodsInfo.MethodCode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RExternalPtr;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RLanguage;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.data.RS4Object;
import com.oracle.truffle.r.runtime.data.RTypedValue;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.env.frame.FrameSlotChangeMonitor;
import com.oracle.truffle.r.runtime.ffi.DLL;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

// Transcribed (unless otherwise noted) from src/library/methods/methods_list_dispatch.c

public class MethodsListDispatch {

    private static void checkSingleString(Casts casts, int argNum, String argName, String msg, boolean nonEmpty, Function<Object, String> clsHierFn,
                    Function<Object, Integer> vecLenFn) {
        //@formatter:off
        casts.arg(argNum, argName).
            defaultError(RError.Message.SINGLE_STRING_WRONG_TYPE, msg, clsHierFn).
            mustBe(stringValue()).
            asStringVector().
            mustBe(singleElement(),  RError.Message.SINGLE_STRING_TOO_LONG, msg, vecLenFn).
            findFirst().
            mustBe(nonEmpty ? lengthGt(0) : lengthGte(0),  RError.Message.NON_EMPTY_STRING, msg);
        //@formatter:on
    }

    public abstract static class R_initMethodDispatch extends RExternalBuiltinNode.Arg1 {

        static {
            Casts.noCasts(R_initMethodDispatch.class);
        }

        @Specialization
        @TruffleBoundary
        protected REnvironment initMethodDispatch(REnvironment env) {
            RContext.getInstance().setMethodTableDispatchOn(true);
            // TBD what should we actually do here
            return env;
        }

        @Fallback
        protected Object initMethodFallback(@SuppressWarnings("unused") Object x) {
            return RNull.instance;
        }

    }

    public abstract static class R_methodsPackageMetaName extends RExternalBuiltinNode.Arg3 {

        static {
            Casts casts = new Casts(R_methodsPackageMetaName.class);
            Function<Object, String> clsHierFn = ClassHierarchyScalarNode::get;
            Function<Object, Integer> vecLenFn = arg -> ((RAbstractStringVector) arg).getLength();

            checkSingleString(casts, 0, "prefix", "The internal prefix (e.g., \"C\") for a meta-data object", true, clsHierFn, vecLenFn);
            checkSingleString(casts, 1, "name", "The name of the object (e.g,. a class or generic function) to find in the meta-data", false, clsHierFn, vecLenFn);
            checkSingleString(casts, 2, "pkg", "The name of the package for a meta-data object", false, clsHierFn, vecLenFn);
        }

        @Specialization
        @TruffleBoundary
        protected String callMethodsPackageMetaName(String prefix, String name, String pkg) {
            if (pkg.length() == 0) {
                return ".__" + prefix + "__" + name;
            } else {
                return ".__" + prefix + "__" + name + ":" + pkg;
            }
        }
    }

    public abstract static class R_getClassFromCache extends RExternalBuiltinNode.Arg2 {

        static {
            Casts casts = new Casts(R_getClassFromCache.class);
            casts.arg(0, "klass").defaultError(RError.Message.GENERIC, "class should be either a character-string name or a class definition").mustBe(stringValue().or(instanceOf(RS4Object.class)));
            casts.arg(1, "table").mustNotBeNull(RError.Message.USE_NULL_ENV_DEFUNCT).mustBe(instanceOf(REnvironment.class));
        }

        protected GetFixedAttributeNode createPckgAttrAccess() {
            return GetFixedAttributeNode.create(RRuntime.PCKG_ATTR_KEY);
        }

        @Specialization
        @TruffleBoundary
        protected Object callGetClassFromCache(RAbstractStringVector klass, REnvironment table,
                        @Cached("createPckgAttrAccess()") GetFixedAttributeNode klassPckgAttrAccess,
                        @Cached("createPckgAttrAccess()") GetFixedAttributeNode valPckgAttrAccess) {
            String klassString = klass.getLength() == 0 ? RRuntime.STRING_NA : klass.getDataAt(0);

            if (klassString.length() == 0) {
                throw error(RError.Message.ZERO_LENGTH_VARIABLE);
            }

            Object value = table.get(klassString);
            if (value == null) {
                return RNull.instance;
            } else {
                Object pckgAttrObj = klass.getAttributes() == null ? null : klassPckgAttrAccess.execute(klass.getAttributes());
                String pckgAttr = RRuntime.asStringLengthOne(pckgAttrObj);
                if (pckgAttr != null && value instanceof RAttributable) {
                    RAttributable attributableValue = (RAttributable) value;
                    Object valAttrObj = attributableValue.getAttributes() == null ? null : valPckgAttrAccess.execute(attributableValue.getAttributes());
                    String valAttr = RRuntime.asStringLengthOne(valAttrObj);
                    // GNUR uses == to compare strings here
                    if (valAttr != null && valAttr != pckgAttr) {
                        return RNull.instance;
                    }
                }
                return value;
            }
        }

        @Specialization
        protected RS4Object callGetClassFromCache(RS4Object klass, @SuppressWarnings("unused") REnvironment table) {
            return klass;
        }

    }

    public abstract static class R_set_method_dispatch extends RExternalBuiltinNode.Arg1 {

        static {
            Casts casts = new Casts(R_set_method_dispatch.class);
            casts.arg(0).asLogicalVector().findFirst(RRuntime.LOGICAL_NA);
        }

        @Specialization
        @TruffleBoundary
        protected Object callSetMethodDispatch(byte onOff) {
            boolean prev = RContext.getInstance().isMethodTableDispatchOn();

            if (onOff == RRuntime.LOGICAL_NA) {
                return RRuntime.asLogical(prev);
            }
            boolean value = RRuntime.fromLogical(onOff);
            RContext.getInstance().setMethodTableDispatchOn(value);
            if (value != prev) {
                // TODO
            }
            return RRuntime.asLogical(prev);
        }
    }

    public abstract static class R_M_setPrimitiveMethods extends RExternalBuiltinNode.Arg5 {
        @Child private AccessSlotNode accessSlotNode;

        static {
            Casts casts = new Casts(R_M_setPrimitiveMethods.class);
            casts.arg(0, "fname").asStringVector().findFirst();
            casts.arg(1, "op");
            casts.arg(2, "code").mustBe(stringValue()).asStringVector().findFirst();
            casts.arg(3, "fundef");
            casts.arg(4, "mlist");
        }

        private AccessSlotNode initAccessSlotNode() {
            if (accessSlotNode == null) {
                accessSlotNode = insert(AccessSlotNodeGen.create(true));
            }
            return accessSlotNode;
        }

        @Specialization
        @TruffleBoundary
        protected Object setPrimitiveMethods(String fnameString, Object op, String codeVecString, Object fundefObj, Object mlist) {
            RTypedValue fundef = (RTypedValue) fundefObj;

            if (op == RNull.instance) {
                byte value = RRuntime.asLogical(RContext.getInstance().allowPrimitiveMethods());
                if (codeVecString.length() > 0) {
                    if (codeVecString.charAt(0) == 'c' || codeVecString.charAt(0) == 'C') {
                        RContext.getInstance().setAllowPrimitiveMethods(false);
                    } else if (codeVecString.charAt(0) == 's' || codeVecString.charAt(0) == 'S') {
                        RContext.getInstance().setAllowPrimitiveMethods(true);
                    }
                }
                return value;
            }

            Object opx = op;
            if ((op instanceof RFunction) && !((RFunction) op).isBuiltin()) {
                String internalName = RRuntime.asString(initAccessSlotNode().executeAccess(op, "internal"));
                opx = RContext.lookupBuiltin(internalName);
                if (opx == null) {
                    throw error(RError.Message.GENERIC, "'internal' slot does not name an internal function: " + internalName);
                }
            }

            setPrimitiveMethodsInternal(opx, codeVecString, fundef, mlist);
            return fnameString;
        }

        private void setPrimitiveMethodsInternal(Object op, String codeVec, RTypedValue fundef, Object mlist) {
            MethodCode code;
            if (codeVec.charAt(0) == 'c') {
                code = MethodCode.NO_METHODS;
            } else if (codeVec.charAt(0) == 'r') {
                code = MethodCode.NEEDS_RESET;
            } else if (codeVec.startsWith("se")) {
                code = MethodCode.HAS_METHODS;
            } else if (codeVec.startsWith("su")) {
                code = MethodCode.SUPPRESSED;
            } else {
                throw error(RError.Message.INVALID_PRIM_METHOD_CODE, codeVec);
            }
            if (!(op instanceof RFunction) || !((RFunction) op).isBuiltin()) {
                throw error(RError.Message.GENERIC, "invalid object: must be a primitive function");
            }
            int primMethodIndex = ((RFunction) op).getRBuiltin().getPrimMethodIndex();
            assert primMethodIndex != PrimitiveMethodsInfo.INVALID_INDEX;

            PrimitiveMethodsInfo primMethodsInfo = RContext.getInstance().getPrimitiveMethodsInfo();
            if (primMethodIndex >= primMethodsInfo.getSize()) {
                primMethodsInfo = primMethodsInfo.resize(primMethodIndex + 1);
            }
            primMethodsInfo.setPrimMethodCode(primMethodIndex, code);
            RFunction value = primMethodsInfo.getPrimGeneric(primMethodIndex);
            if (code != MethodCode.SUPPRESSED) {
                assert fundef != null; // explicitly checked in GNU R
                if (code == MethodCode.NO_METHODS && value != null) {
                    primMethodsInfo.setPrimGeneric(primMethodIndex, null);
                    primMethodsInfo.setPrimMethodList(primMethodIndex, null);
                } else if (fundef != RNull.instance && value == null) {
                    if (!(fundef instanceof RFunction)) {
                        throw error(RError.Message.PRIM_GENERIC_NOT_FUNCTION, fundef.getRType().getName());
                    }
                    primMethodsInfo.setPrimGeneric(primMethodIndex, (RFunction) fundef);
                }
            }
            if (code == MethodCode.HAS_METHODS) {
                assert mlist != null; // explicitly checked in GNU R
                if (mlist != RNull.instance) {
                    primMethodsInfo.setPrimMethodList(primMethodIndex, (REnvironment) mlist);
                }
            }
        }
    }

    public abstract static class R_identC extends RExternalBuiltinNode.Arg2 {

        static {
            Casts.noCasts(R_identC.class);
        }

        @Specialization
        protected Object identC(RAbstractStringVector e1, RAbstractStringVector e2) {
            if (e1.getLength() == 1 && e2.getLength() == 1 && e1.getDataAt(0).equals(e2.getDataAt(0))) {
                return RRuntime.LOGICAL_TRUE;
            } else {
                return RRuntime.LOGICAL_FALSE;
            }
        }

        @SuppressWarnings("unused")
        @Fallback
        protected Object identC(Object e1, Object e2) {
            return RRuntime.LOGICAL_FALSE;
        }
    }

    public abstract static class R_getGeneric extends RExternalBuiltinNode.Arg4 {

        @Child private GetGenericInternal getGenericInternal = GetGenericInternalNodeGen.create();

        static {
            Casts casts = new Casts(R_getGeneric.class);
            Function<Object, String> clsHierFn = ClassHierarchyScalarNode::get;
            Function<Object, Integer> vecLenFn = arg -> ((RAbstractStringVector) arg).getLength();

            checkSingleString(casts, 0, "f", "The argument \"f\" to getGeneric", true, clsHierFn, vecLenFn);

            casts.arg(1, "mustFind").asLogicalVector().findFirst(RRuntime.LOGICAL_FALSE).map(toBoolean());

            casts.arg(2, "env").mustBe(instanceOf(REnvironment.class));

            checkSingleString(casts, 3, "package", "The argument \"package\" to getGeneric", false, clsHierFn, vecLenFn);
        }

        @Specialization
        protected Object getGeneric(String name, boolean mustFind, REnvironment env, String pckg) {
            Object value = getGenericInternal.executeObject(name, env, pckg);
            if (value == RNull.instance) {
                if (mustFind) {
                    if (env == RContext.getInstance().stateREnvironment.getGlobalEnv()) {
                        throw error(RError.Message.NO_GENERIC_FUN, name);
                    } else {
                        throw error(RError.Message.NO_GENERIC_FUN_IN_ENV, name);
                    }
                }
            }
            return value;
        }
    }

    abstract static class GetGenericInternal extends RBaseNode {

        public abstract Object executeObject(String name, REnvironment rho, String pckg);

        @Child private CastToVectorNode castToVector = CastToVectorNodeGen.create(false);
        @Child private ClassHierarchyScalarNode classHierarchyNode = ClassHierarchyScalarNodeGen.create();
        @Child private PromiseHelperNode promiseHelper;
        @Child private GetFixedAttributeNode getGenericAttrNode = GetFixedAttributeNode.create(RRuntime.GENERIC_ATTR_KEY);
        @Child private GetFixedAttributeNode getPckgAttrNode = GetFixedAttributeNode.create(RRuntime.PCKG_ATTR_KEY);

        @Specialization
        protected Object getGeneric(String name, REnvironment env, String pckg) {
            REnvironment rho = env;
            RAttributable generic = null;
            while (rho != null) {
                // TODO: make it faster
                MaterializedFrame currentFrame = rho.getFrame();
                if (currentFrame == null) {
                    break;
                }
                FrameDescriptor currentFrameDesc = currentFrame.getFrameDescriptor();
                Object o = slotRead(currentFrame, currentFrameDesc, name);
                if (o != null) {
                    RAttributable vl = (RAttributable) o;
                    boolean ok = false;
                    if (vl instanceof RFunction && getGenericAttrNode.execute(vl) != null) {
                        if (pckg.length() > 0) {
                            Object gpckgObj = getPckgAttrNode.execute(vl);
                            if (gpckgObj != null) {
                                String gpckg = checkSingleString(castToVector.doCast(gpckgObj), false, "The \"package\" slot in generic function object", this, classHierarchyNode);
                                ok = pckg.equals(gpckg);
                            }
                        } else {
                            ok = true;
                        }
                    }
                    if (ok) {
                        generic = vl;
                        break;
                    }
                }
                rho = rho.getParent();
            }

            // TODO: in GNU R there is additional code here that deals with the case of "name"
            // being a symbol but at this point this case is not handled (even possible?) in
            // FastR
            return generic == null ? RNull.instance : generic;
        }

        private static String checkSingleString(Object o, boolean nonEmpty, String what, RBaseNode node, ClassHierarchyScalarNode classHierarchyNode) {
            if (o instanceof RAbstractStringVector) {
                RAbstractStringVector vec = (RAbstractStringVector) o;
                if (vec.getLength() != 1) {
                    throw RError.error(node, RError.Message.SINGLE_STRING_TOO_LONG, what, vec.getLength());
                }
                String s = vec.getDataAt(0);
                if (nonEmpty && s.length() == 0) {
                    throw node.error(RError.Message.NON_EMPTY_STRING, what);
                }
                return s;
            } else {
                throw node.error(RError.Message.SINGLE_STRING_WRONG_TYPE, what, classHierarchyNode.executeString(o));
            }
        }

        @TruffleBoundary
        private static Object slotRead(MaterializedFrame currentFrame, FrameDescriptor desc, String name) {
            FrameSlot slot = desc.findFrameSlot(name);
            if (slot != null) {
                Object res = FrameSlotChangeMonitor.getValue(slot, currentFrame);
                if (res != null) {
                    if (res instanceof RPromise) {
                        res = PromiseHelperNode.evaluateSlowPath(null, (RPromise) res);
                    }
                }
                return res;
            } else {
                return null;
            }
        }
    }

    public abstract static class R_nextMethodCall extends RExternalBuiltinNode.Arg2 {

        @Child private LocalReadVariableNode readDotNextMethod;
        @Child private LocalReadVariableNode readDots;

        static {
            Casts.noCasts(R_nextMethodCall.class);
        }

        @Specialization
        @TruffleBoundary
        protected Object nextMethodCall(RLanguage matchedCall, REnvironment ev) {
            // TODO: we can't create LocalReadVariableNode-s once and for all because ev may change
            // leading to a problem if contains a different frame; should we finesse implementation
            // of LocalReadVariableNode to handle this?
            readDotNextMethod = insert(LocalReadVariableNode.create(RRuntime.R_DOT_NEXT_METHOD, false));
            readDots = insert(LocalReadVariableNode.create("...", false));

            RFunction op = (RFunction) readDotNextMethod.execute(null, ev.getFrame());
            if (op == null) {
                throw error(RError.Message.GENERIC, "internal error in 'callNextMethod': '.nextMethod' was not assigned in the frame of the method call");
            }
            boolean primCase = op.isBuiltin();
            if (primCase) {
                throw RInternalError.unimplemented();
            }
            if (!(matchedCall.getRep() instanceof RCallNode)) {
                throw RInternalError.unimplemented();

            }
            RCallNode callNode = (RCallNode) matchedCall.getRep();
            RNode f = ReadVariableNode.create(RRuntime.R_DOT_NEXT_METHOD);
            ArgumentsSignature sig = callNode.getSyntaxSignature();
            RSyntaxNode[] args = new RSyntaxNode[sig.getLength()];
            for (int i = 0; i < args.length; i++) {
                args[i] = ReadVariableNode.create(sig.getName(i));
            }
            RLanguage newCall = RDataFactory.createLanguage(RCallNode.createCall(RSyntaxNode.SOURCE_UNAVAILABLE, f, sig, args));
            Object res = RContext.getEngine().eval(newCall, ev.getFrame());
            return res;
        }
    }

    // Transcribed from src/library/methods/class_support.c
    public abstract static class R_externalPtrPrototypeObject extends RExternalBuiltinNode.Arg0 {

        @Specialization
        protected RExternalPtr extPrototypeObj() {
            // in GNU R, first argument is a pointer to a dummy C function
            // whose only purpose is to throw an error indicating that it shouldn't be called
            // TODO: finesse error handling in case a function stored in this pointer is actually
            // called
            return RDataFactory.createExternalPtr(new DLL.SymbolHandle(0L), RNull.instance, RNull.instance);
        }
    }
}
