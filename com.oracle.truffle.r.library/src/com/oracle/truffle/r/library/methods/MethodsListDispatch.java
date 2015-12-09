/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2015, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.methods;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.r.library.methods.MethodsListDispatchFactory.GetGenericInternalNodeGen;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.function.ClassHierarchyScalarNode;
import com.oracle.truffle.r.nodes.function.ClassHierarchyScalarNodeGen;
import com.oracle.truffle.r.nodes.function.PromiseHelperNode;
import com.oracle.truffle.r.nodes.function.PromiseHelperNode.PromiseCheckHelperNode;
import com.oracle.truffle.r.nodes.objects.ExecuteMethod;
import com.oracle.truffle.r.nodes.unary.CastToVectorNode;
import com.oracle.truffle.r.nodes.unary.CastToVectorNodeGen;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.context.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.env.*;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

// Transcribed (unless otherwise noted) from src/library/methods/methods_list_dispatch.c

public class MethodsListDispatch {

    public abstract static class R_initMethodDispatch extends RExternalBuiltinNode.Arg1 {

        @Specialization
        @TruffleBoundary
        protected REnvironment initMethodDispatch(REnvironment env) {
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

        @Specialization
        @TruffleBoundary
        protected Object callGetClassFromCache(RAbstractStringVector klass, REnvironment table) {
            String klassString = klass.getLength() == 0 ? RRuntime.STRING_NA : klass.getDataAt(0);

            Object value = table.get(klassString);
            if (value == null) {
                return RNull.instance;
            } else {
                // TODO check PACKAGE equality
                return value;
            }
        }

        @Specialization
        protected RS4Object callGetClassFromCache(RS4Object klass, @SuppressWarnings("unused") REnvironment table) {
            return klass;
        }

        @SuppressWarnings("unused")
        @Fallback
        protected Object callGetClassFromCache(Object klass, REnvironment table) {
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

        @Specialization
        @TruffleBoundary
        protected byte setPrimitiveMethods(Object fname, Object op, Object codeVec, @SuppressWarnings("unused") Object fundef, @SuppressWarnings("unused") Object mlist) {
            String fnameString = RRuntime.asString(fname);
            String codeVecString = RRuntime.asString(codeVec);
            if (codeVecString == null) {
                throw RError.error(this, RError.Message.GENERIC, "argument 'code' must be a character string");
            }
            // TODO: implement proper primitive method setting
            if (op == RNull.instance) {
                byte value = RRuntime.asLogical(RContext.getInstance().allowPrimitiveMethods());
                if (codeVecString.length() > 0) {
                    if (codeVecString.charAt(0) == 'C') {
                        RContext.getInstance().setAllowPrimitiveMethods(false);
                    } else if (codeVecString.charAt(0) == 'S') {
                        RContext.getInstance().setAllowPrimitiveMethods(true);
                    }
                }
                return RRuntime.LOGICAL_FALSE; // value;
            }
            return RRuntime.LOGICAL_FALSE;
            // throw RInternalError.unimplemented();
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

    static String checkSingleString(Object o, boolean nonEmpty, String what, RBaseNode node, ClassHierarchyScalarNode classHierarchyNode) {
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

        @Specialization
        protected Object getGeneric(String name, REnvironment env, String pckg) {
            REnvironment rho = env;
            RAttributable generic = null;
            while (rho != null) {
                // TODO: make it faster
                MaterializedFrame currentFrame = rho.getFrame();
                FrameDescriptor currentFrameDesc = currentFrame.getFrameDescriptor();
                Object o = ExecuteMethod.slotRead(currentFrame, currentFrameDesc, name);
                if (o != null) {
                    if (o instanceof RPromise) {
                        o = PromiseHelperNode.evaluateSlowPath(null, (RPromise) o);
                    }
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
    }

    // Transcribed from src/library/methods/class_support.c
    public abstract static class R_externalPtrPrototypeObject extends RExternalBuiltinNode.Arg0 {

        @Specialization
        protected RExternalPtr extPrototypeObj() {
            // in GNU R, first argument is a pointer to a dummy C function
            // whose only purpose is to throw an error indicating that it shouldn't be called
            // TODO: finesse error handling in case a function stored in this pointer is actually
            // called
            return RDataFactory.createExternalPtr(0, RNull.instance, RNull.instance);
        }
    }

}
