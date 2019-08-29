/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.ffi.impl.nodes;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.r.ffi.impl.nodes.ListAccessNodesFactory.CAARNodeGen;
import com.oracle.truffle.r.ffi.impl.nodes.ListAccessNodesFactory.CAD4RNodeGen;
import com.oracle.truffle.r.ffi.impl.nodes.ListAccessNodesFactory.CADDDRNodeGen;
import com.oracle.truffle.r.ffi.impl.nodes.ListAccessNodesFactory.CADDRNodeGen;
import com.oracle.truffle.r.ffi.impl.nodes.ListAccessNodesFactory.CADRNodeGen;
import com.oracle.truffle.r.ffi.impl.nodes.ListAccessNodesFactory.CARNodeGen;
import com.oracle.truffle.r.ffi.impl.nodes.ListAccessNodesFactory.CDARNodeGen;
import com.oracle.truffle.r.ffi.impl.nodes.ListAccessNodesFactory.CDDDRNodeGen;
import com.oracle.truffle.r.ffi.impl.nodes.ListAccessNodesFactory.CDDRNodeGen;
import com.oracle.truffle.r.ffi.impl.nodes.ListAccessNodesFactory.CDRNodeGen;
import com.oracle.truffle.r.ffi.impl.nodes.ListAccessNodesFactory.SETCAD4RNodeGen;
import com.oracle.truffle.r.ffi.impl.nodes.ListAccessNodesFactory.SETCADDDRNodeGen;
import com.oracle.truffle.r.ffi.impl.nodes.ListAccessNodesFactory.SETCADDRNodeGen;
import com.oracle.truffle.r.ffi.impl.nodes.ListAccessNodesFactory.SETCADRNodeGen;
import com.oracle.truffle.r.ffi.impl.nodes.ListAccessNodesFactory.SETCARNodeGen;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RExternalPtr;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RPairListLibrary;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.data.RTypes;

/**
 * Nodes that implement {@code CAR}, {@code CDR}, etc. N.B. GNU R does not error check the
 * arguments; it will crash (segv) if given, say, a numeric arg.
 */
public final class ListAccessNodes {

    @TypeSystemReference(RTypes.class)
    @GenerateUncached
    public abstract static class CARNode extends FFIUpCallNode.Arg1 {
        @Specialization
        protected Object car(RPairList pl,
                        @CachedLibrary(limit = "1") RPairListLibrary plLib) {
            return plLib.car(pl);
        }

        @Specialization
        protected Object car(RArgsValuesAndNames args) {
            if (args.isEmpty()) {
                return RNull.instance;
            }
            return args.getArgument(0);
        }

        @Specialization
        protected Object car(RSymbol sym) {
            return sym.getWrappedName();
        }

        @Specialization
        protected Object car(@SuppressWarnings("unused") RNull nil) {
            return RNull.instance;
        }

        @Specialization
        protected Object car(RExternalPtr extPtr) {
            return extPtr.getAddr();
        }

        @Fallback
        @TruffleBoundary
        protected Object car(Object obj) {
            throw RInternalError.unimplemented("CAR only works on pair lists, language objects, and argument lists, type given: " + Utils.getTypeName(obj));
        }

        public static CARNode create() {
            return CARNodeGen.create();
        }
    }

    @TypeSystemReference(RTypes.class)
    @GenerateUncached
    public abstract static class CDRNode extends FFIUpCallNode.Arg1 {
        @Specialization
        protected Object cdr(RPairList pl,
                        @CachedLibrary(limit = "1") RPairListLibrary plLib) {
            return plLib.cdr(pl);
        }

        @Specialization
        protected Object cdr(RArgsValuesAndNames args,
                        @CachedLibrary(limit = "1") RPairListLibrary plLib) {
            // TODO: this is too late - "..." should be converted to pairlist earlier
            return plLib.cdr(args.toPairlist());
        }

        @Specialization
        protected RNull cdr(@SuppressWarnings("unused") RSymbol symbol) {
            return RNull.instance;
        }

        @Specialization
        protected RNull handleNull(@SuppressWarnings("unused") RNull rNull) {
            return RNull.instance;
        }

        @Fallback
        @TruffleBoundary
        protected Object cdr(Object obj) {
            throw RInternalError.unimplemented("CDR only works on pair lists, language objects, and argument lists, type given: " + Utils.getTypeName(obj));
        }

        public static CDRNode create() {
            return CDRNodeGen.create();
        }
    }

    @TypeSystemReference(RTypes.class)
    @GenerateUncached
    public abstract static class CAARNode extends FFIUpCallNode.Arg1 {
        @Specialization
        public Object executeObject(Object x,
                        @Cached() CARNode car1,
                        @Cached() CARNode car2) {
            return car2.executeObject(car1.executeObject(x));
        }

        public static CAARNode create() {
            return CAARNodeGen.create();
        }

        public static CAARNode getUncached() {
            return CAARNodeGen.getUncached();
        }
    }

    @TypeSystemReference(RTypes.class)
    @GenerateUncached
    public abstract static class SETCADRNode extends FFIUpCallNode.Arg2 {
        @Specialization
        public Object executeObject(Object x, Object y,
                        @Cached() SETCARNode setcarNode,
                        @Cached() CDRNode cdrNode) {
            return setcarNode.executeObject(cdrNode.executeObject(x), y);
        }

        public static SETCADRNode create() {
            return SETCADRNodeGen.create();
        }

        public static SETCADRNode getUncached() {
            return SETCADRNodeGen.getUncached();
        }
    }

    @TypeSystemReference(RTypes.class)
    @GenerateUncached
    public abstract static class SETCADDRNode extends FFIUpCallNode.Arg2 {
        @Specialization
        public Object executeObject(Object x, Object val,
                        @Cached() CDDRNode cddr,
                        @Cached() SETCARNode setcarNode) {
            return setcarNode.executeObject(cddr.executeObject(x), val);
        }

        public static SETCADDRNode create() {
            return SETCADDRNodeGen.create();
        }

        public static SETCADDRNode getUncached() {
            return SETCADDRNodeGen.getUncached();
        }
    }

    @TypeSystemReference(RTypes.class)
    @GenerateUncached
    public abstract static class SETCADDDRNode extends FFIUpCallNode.Arg2 {
        @Specialization
        public Object executeObject(Object x, Object val,
                        @Cached() CDDDRNode cdddr,
                        @Cached() SETCARNode setcarNode) {
            return setcarNode.executeObject(cdddr.executeObject(x), val);
        }

        public static SETCADDDRNode create() {
            return SETCADDDRNodeGen.create();
        }

        public static SETCADDDRNode getUncached() {
            return SETCADDDRNodeGen.getUncached();
        }
    }

    @TypeSystemReference(RTypes.class)
    @GenerateUncached
    public abstract static class SETCAD4RNode extends FFIUpCallNode.Arg2 {
        @Specialization
        public Object executeObject(Object x, Object val,
                        @Cached() CDDDRNode cdddr,
                        @Cached() CDRNode cdr,
                        @Cached() SETCARNode setcarNode) {
            return setcarNode.executeObject(cdddr.executeObject(cdr.executeObject(x)), val);
        }

        public static SETCAD4RNode create() {
            return SETCAD4RNodeGen.create();
        }

        public static SETCAD4RNode getUncached() {
            return SETCAD4RNodeGen.getUncached();
        }
    }

    @TypeSystemReference(RTypes.class)
    @GenerateUncached
    public abstract static class SETCARNode extends FFIUpCallNode.Arg2 {
        public static SETCARNode create() {
            return SETCARNodeGen.create();
        }

        @Specialization
        protected Object doRPairList(RPairList x, Object y) {
            x.setCar(y);
            return y;
        }

        @Fallback
        @TruffleBoundary
        protected Object car(Object x, Object y) {
            throw RInternalError.unimplemented("SETCAR only works on pair lists or language objects, types given: " + Utils.getTypeName(x) + ',' + Utils.getTypeName(y));
        }
    }

    @TypeSystemReference(RTypes.class)
    @GenerateUncached
    public abstract static class CADRNode extends FFIUpCallNode.Arg1 {
        @Specialization
        public Object executeObject(Object x,
                        @Cached() CDRNode cdr,
                        @Cached() CARNode car) {
            return car.executeObject(cdr.executeObject(x));
        }

        public static CADRNode create() {
            return CADRNodeGen.create();
        }

        public static CADRNode getUncached() {
            return CADRNodeGen.getUncached();
        }
    }

    @TypeSystemReference(RTypes.class)
    @GenerateUncached
    public abstract static class CDARNode extends FFIUpCallNode.Arg1 {
        @Specialization
        public Object executeObject(Object x,
                        @Cached() CARNode car,
                        @Cached() CDRNode cdr) {
            return cdr.executeObject(car.executeObject(x));
        }

        public static CDARNode create() {
            return CDARNodeGen.create();
        }

        public static CDARNode getUncached() {
            return CDARNodeGen.getUncached();
        }
    }

    @TypeSystemReference(RTypes.class)
    @GenerateUncached
    public abstract static class CADDRNode extends FFIUpCallNode.Arg1 {
        @Specialization
        public Object executeObject(Object x,
                        @Cached() CARNode car,
                        @Cached() CDRNode cdr1,
                        @Cached() CDRNode cdr2) {
            return car.executeObject(cdr1.executeObject(cdr2.executeObject(x)));
        }

        public static CADDRNode create() {
            return CADDRNodeGen.create();
        }

        public static CADDRNode getUncached() {
            return CADDRNodeGen.getUncached();
        }
    }

    @TypeSystemReference(RTypes.class)
    @GenerateUncached
    public abstract static class CADDDRNode extends FFIUpCallNode.Arg1 {
        @Specialization
        public Object executeObject(Object x,
                        @Cached() CARNode car,
                        @Cached() CDRNode cdr1,
                        @Cached() CDRNode cdr2,
                        @Cached() CDRNode cdr3) {
            return car.executeObject(cdr1.executeObject(cdr2.executeObject(cdr3.executeObject(x))));
        }

        public static CADDDRNode create() {
            return CADDDRNodeGen.create();
        }

        public static CADDDRNode getUncached() {
            return CADDDRNodeGen.getUncached();
        }
    }

    @TypeSystemReference(RTypes.class)
    @GenerateUncached
    public abstract static class CAD4RNode extends FFIUpCallNode.Arg1 {
        @Specialization
        public Object executeObject(Object x,
                        @Cached() CADDDRNode cadddr,
                        @Cached() CDRNode cdr) {
            return cadddr.executeObject(cdr.executeObject(x));
        }

        public static CAD4RNode create() {
            return CAD4RNodeGen.create();
        }

        public static CAD4RNode getUncached() {
            return CAD4RNodeGen.getUncached();
        }
    }

    @TypeSystemReference(RTypes.class)
    @GenerateUncached
    public abstract static class CDDRNode extends FFIUpCallNode.Arg1 {
        @Specialization
        public Object executeObject(Object x,
                        @Cached() CDRNode cdr1,
                        @Cached() CDRNode cdr2) {
            return cdr1.executeObject(cdr2.executeObject(x));
        }

        public static CDDRNode create() {
            return CDDRNodeGen.create();
        }

        public static CDDRNode getUncached() {
            return CDDRNodeGen.getUncached();
        }
    }

    @TypeSystemReference(RTypes.class)
    @GenerateUncached
    public abstract static class CDDDRNode extends FFIUpCallNode.Arg1 {
        @Specialization
        public Object executeObject(Object x,
                        @Cached() CDDRNode cddr,
                        @Cached() CDRNode cdr) {
            return cdr.executeObject(cddr.executeObject(x));
        }

        public static CDDDRNode create() {
            return CDDDRNodeGen.create();
        }

        public static CDDDRNode getUncached() {
            return CDDDRNodeGen.getUncached();
        }
    }
}
