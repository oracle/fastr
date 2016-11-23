/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.methods;

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
import com.oracle.truffle.r.runtime.data.RAttributeProfiles;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RExternalPtr;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RLanguage;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.data.RS4Object;
import com.oracle.truffle.r.runtime.data.RTypedValue;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.ffi.DLL;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

// Transcribed (unless otherwise noted) from src/library/methods/methods_list_dispatch.c

public class MethodsListDispatch {

    public abstract static class R_initMethodDispatch extends RExternalBuiltinNode.Arg1 {

        @Specialization
        @TruffleBoundary
        protected REnvironment initMethodDispatch(REnvironment env) {
            RContext.getInstance().setMethodTableDispatchOn(true);
            // TBD what should we actually do here
            return env;
        }
    }

    public abstract static class R_methodsPackageMetaName extends RExternalBuiltinNode.Arg3 {

        @Specialization
        @TruffleBoundary
        protected String callMethodsPackageMetaName(RAbstractStringVector prefixStringVector, RAbstractStringVector nameStringVector, RAbstractStringVector pkgStringVector) {
            // TODO: proper error messages
            assert prefixStringVector.getLength() == 1 && nameStringVector.getLength() == 1 && pkgStringVector.getLength() == 1;
            String prefixString = prefixStringVector.getDataAt(0);
            String nameString = nameStringVector.getDataAt(0);
            String pkgString = pkgStringVector.getDataAt(0);

            if (pkgString.length() == 0) {
                return ".__" + prefixString + "__" + nameString;
            } else {
                return ".__" + prefixString + "__" + nameString + ":" + pkgString;
            }
        }
    }

    public abstract static class R_getClassFromCache extends RExternalBuiltinNode.Arg2 {

        protected GetFixedAttributeNode createPckgAttrAccess() {
            return GetFixedAttributeNode.create(RRuntime.PCKG_ATTR_KEY);
        }

        @Specialization
        @TruffleBoundary
        protected Object callGetClassFromCache(RAbstractStringVector klass, REnvironment table, //
                        @Cached("createPckgAttrAccess()") GetFixedAttributeNode klassPckgAttrAccess, //
                        @Cached("createPckgAttrAccess()") GetFixedAttributeNode valPckgAttrAccess) {
            String klassString = klass.getLength() == 0 ? RRuntime.STRING_NA : klass.getDataAt(0);

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

        @SuppressWarnings("unused")
        @Fallback
        protected Object callGetClassFromCache(Object klass, Object table) {
            throw RError.error(this, RError.Message.GENERIC, "class should be either a character-string name or a class definition");
        }
    }

    public abstract static class R_set_method_dispatch extends RExternalBuiltinNode.Arg1 {

        @Specialization
        @TruffleBoundary
        protected Object callSetMethodDispatch(RAbstractLogicalVector onOffVector) {
            boolean prev = RContext.getInstance().isMethodTableDispatchOn();
            byte onOff = castLogical(onOffVector);

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

        private AccessSlotNode initAccessSlotNode() {
            if (accessSlotNode == null) {
                accessSlotNode = insert(AccessSlotNodeGen.create(true, null, null));
            }
            return accessSlotNode;
        }

        @Specialization
        @TruffleBoundary
        protected Object setPrimitiveMethods(Object fname, Object op, Object codeVec, RTypedValue fundef, Object mlist) {
            String fnameString = RRuntime.asString(fname);
            String codeVecString = RRuntime.asString(codeVec);
            if (codeVecString == null) {
                throw RError.error(this, RError.Message.GENERIC, "argument 'code' must be a character string");
            }

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
                    throw RError.error(this, RError.Message.GENERIC, "'internal' slot does not name an internal function: " + internalName);
                }
            }

            setPrimitiveMethodsInternal(opx, codeVecString, fundef, mlist);
            return fnameString;
        }

        private static void setPrimitiveMethodsInternal(Object op, String codeVec, RTypedValue fundef, Object mlist) {
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
                throw RError.error(RError.SHOW_CALLER, RError.Message.INVALID_PRIM_METHOD_CODE, codeVec);
            }
            if (!(op instanceof RFunction) || !((RFunction) op).isBuiltin()) {
                throw RError.error(RError.SHOW_CALLER, RError.Message.GENERIC, "invalid object: must be a primitive function");
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
                        throw RError.error(RError.SHOW_CALLER, RError.Message.PRIM_GENERIC_NOT_FUNCTION, fundef.getRType().getName());
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

    private static String checkSingleString(Object o, boolean nonEmpty, String what, RBaseNode node, ClassHierarchyScalarNode classHierarchyNode) {
        if (o instanceof RAbstractStringVector) {
            RAbstractStringVector vec = (RAbstractStringVector) o;
            if (vec.getLength() != 1) {
                throw RError.error(node, RError.Message.SINGLE_STRING_TOO_LONG, what, vec.getLength());
            }
            String s = vec.getDataAt(0);
            if (nonEmpty && s.length() == 0) {
                throw RError.error(node, RError.Message.NON_EMPTY_STRING, what);
            }
            return s;
        } else {
            throw RError.error(node, RError.Message.SINGLE_STRING_WRONG_TYPE, what, classHierarchyNode.executeString(o));
        }
    }

    public abstract static class R_getGeneric extends RExternalBuiltinNode.Arg4 {

        @Child private ClassHierarchyScalarNode classHierarchyNode = ClassHierarchyScalarNodeGen.create();
        @Child private GetGenericInternal getGenericInternal = GetGenericInternalNodeGen.create();

        @Specialization
        protected Object getGeneric(RAbstractVector nameVec, RAbstractVector mustFindVec, REnvironment env, RAbstractVector packageVec) {
            String name = checkSingleString(nameVec, true, "The argument \"f\" to getGeneric", this, classHierarchyNode);
            byte mustFind = castLogical(mustFindVec);
            String pckg = checkSingleString(packageVec, false, "The argument \"package\" to getGeneric", this, classHierarchyNode);
            Object value = getGenericInternal.executeObject(name, env, pckg);
            if (value == RNull.instance) {
                if (mustFind == RRuntime.LOGICAL_TRUE) {
                    if (env == RContext.getInstance().stateREnvironment.getGlobalEnv()) {
                        throw RError.error(this, RError.Message.NO_GENERIC_FUN, name);
                    } else {
                        throw RError.error(this, RError.Message.NO_GENERIC_FUN_IN_ENV, name);
                    }
                }
            }
            return value;
        }
    }

    abstract static class GetGenericInternal extends RBaseNode {

        public abstract Object executeObject(String name, REnvironment rho, String pckg);

        private final RAttributeProfiles attrProfiles = RAttributeProfiles.create();
        @Child private CastToVectorNode castToVector = CastToVectorNodeGen.create(false);
        @Child private ClassHierarchyScalarNode classHierarchyNode = ClassHierarchyScalarNodeGen.create();
        @Child private PromiseHelperNode promiseHelper;

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
                    if (vl instanceof RFunction && vl.getAttr(attrProfiles, RRuntime.GENERIC_ATTR_KEY) != null) {
                        if (pckg.length() > 0) {
                            Object gpckgObj = vl.getAttr(attrProfiles, RRuntime.PCKG_ATTR_KEY);
                            if (gpckgObj != null) {
                                String gpckg = checkSingleString(castToVector.execute(gpckgObj), false, "The \"package\" slot in generic function object", this, classHierarchyNode);
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

        @TruffleBoundary
        private static Object slotRead(MaterializedFrame currentFrame, FrameDescriptor desc, String name) {
            FrameSlot slot = desc.findFrameSlot(name);
            if (slot != null) {
                Object res = currentFrame.getValue(slot);
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
                throw RError.error(this, RError.Message.GENERIC, "internal error in 'callNextMethod': '.nextMethod' was not assigned in the frame of the method call");
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
