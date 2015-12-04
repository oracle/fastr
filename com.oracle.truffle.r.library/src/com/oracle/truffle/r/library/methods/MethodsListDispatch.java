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
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.context.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.env.*;

// Transcribed from src/library/methods/methods_list_dispatch.c

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
            if (op == RNull.instance) {
                byte value = RRuntime.asLogical(RContext.getInstance().allowPrimitiveMethods());
                if (codeVecString.charAt(0) == 'C') {
                    RContext.getInstance().setAllowPrimitiveMethods(false);
                } else if (codeVecString.charAt(0) == 'S') {
                    RContext.getInstance().setAllowPrimitiveMethods(true);
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

}
