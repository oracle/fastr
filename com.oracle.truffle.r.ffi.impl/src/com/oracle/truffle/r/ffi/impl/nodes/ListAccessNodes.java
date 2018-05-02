/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.r.ffi.impl.nodes.ListAccessNodesFactory.CARNodeGen;
import com.oracle.truffle.r.ffi.impl.nodes.ListAccessNodesFactory.CDRNodeGen;
import com.oracle.truffle.r.ffi.impl.nodes.ListAccessNodesFactory.SETCARNodeGen;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetNamesAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.SetNamesAttributeNode;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.data.CharSXPWrapper;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.data.RTypes;

/**
 * Nodes that implement {@code CAR}, {@code CDR}, etc. N.B. GNU R does not error check the
 * arguments; it will crash (segv) if given, say, a numeric arg.
 */
public final class ListAccessNodes {

    @TypeSystemReference(RTypes.class)
    public abstract static class CARNode extends FFIUpCallNode.Arg1 {
        @Specialization
        protected Object car(RPairList pl) {
            return pl.car();
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
            return CharSXPWrapper.create(sym.getName());
        }

        @Specialization
        protected Object car(RList list) {
            return list.getDataAt(0);
        }

        @Specialization
        protected Object car(@SuppressWarnings("unused") RNull nil) {
            return RNull.instance;
        }

        @Fallback
        @TruffleBoundary
        protected Object car(@SuppressWarnings("unused") Object obj) {
            throw RInternalError.unimplemented("CAR only works on pair lists, language objects, and argument lists, type given: " + Utils.getTypeName(obj));
        }

        public static CARNode create() {
            return CARNodeGen.create();
        }
    }

    @TypeSystemReference(RTypes.class)
    public abstract static class CDRNode extends FFIUpCallNode.Arg1 {
        @Specialization
        protected Object cdr(RPairList pl) {
            return pl.cdr();
        }

        @Specialization
        protected Object cdr(RArgsValuesAndNames args) {
            // TODO: this is too late - "..." should be converted to pairlist earlier
            return ((RPairList) args.toPairlist()).cdr();
        }

        @Specialization
        protected Object cdr(RList list,
                        @Cached("create()") GetNamesAttributeNode getNamesNode,
                        @Cached("create()") SetNamesAttributeNode setNamesNode) {
            if (list.getLength() == 1) {
                return RNull.instance;
            }
            RStringVector names = getNamesNode.getNames(list);
            RList copy = RDataFactory.createList(list.getDataCopy());
            if (names != null) {
                String[] namesDataCopy = names.getDataCopy();
                setNamesNode.setNames(copy, RDataFactory.createStringVector(namesDataCopy, true));
            }
            return copy;
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
        protected Object cdr(@SuppressWarnings("unused") Object obj) {
            throw RInternalError.unimplemented("CDR only works on pair lists, language objects, and argument lists, type given: " + Utils.getTypeName(obj));
        }

        public static CDRNode create() {
            return CDRNodeGen.create();
        }
    }

    @TypeSystemReference(RTypes.class)
    public static final class CAARNode extends FFIUpCallNode.Arg1 {

        @Child private CARNode car1 = CARNode.create();
        @Child private CARNode car2 = CARNode.create();

        @Override
        public Object executeObject(Object x) {
            return car2.executeObject(car1.executeObject(x));
        }

        public static CAARNode create() {
            return new CAARNode();
        }
    }

    @TypeSystemReference(RTypes.class)
    public static final class SETCADRNode extends FFIUpCallNode.Arg2 {
        @Child private SETCARNode setcarNode = SETCARNode.create();
        @Child private CDRNode cdrNode = CDRNode.create();

        @Override
        public Object executeObject(Object x, Object y) {
            return setcarNode.executeObject(cdrNode.executeObject(x), y);
        }
    }

    @TypeSystemReference(RTypes.class)
    public static final class SETCADDRNode extends FFIUpCallNode.Arg2 {
        @Child private CDDRNode cddr = CDDRNode.create();
        @Child private SETCARNode setcarNode = SETCARNode.create();

        @Override
        public Object executeObject(Object x, Object val) {
            return setcarNode.executeObject(cddr.executeObject(x), val);
        }

        public static SETCADDRNode create() {
            return new SETCADDRNode();
        }
    }

    @TypeSystemReference(RTypes.class)
    public static final class SETCADDDRNode extends FFIUpCallNode.Arg2 {
        @Child private CDDDRNode cdddr = CDDDRNode.create();
        @Child private SETCARNode setcarNode = SETCARNode.create();

        @Override
        public Object executeObject(Object x, Object val) {
            return setcarNode.executeObject(cdddr.executeObject(x), val);
        }

        public static SETCADDDRNode create() {
            return new SETCADDDRNode();
        }
    }

    @TypeSystemReference(RTypes.class)
    public static final class SETCAD4RNode extends FFIUpCallNode.Arg2 {
        @Child private CDDDRNode cdddr = CDDDRNode.create();
        @Child private CDRNode cdr = CDRNode.create();
        @Child private SETCARNode setcarNode = SETCARNode.create();

        @Override
        public Object executeObject(Object x, Object val) {
            return setcarNode.executeObject(cdddr.executeObject(cdr.executeObject(x)), val);
        }

        public static SETCAD4RNode create() {
            return new SETCAD4RNode();
        }
    }

    @TypeSystemReference(RTypes.class)
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
        protected Object car(@SuppressWarnings("unused") Object x, @SuppressWarnings("unused") Object y) {
            throw RInternalError.unimplemented("SETCAR only works on pair lists or language objects, types given: " + Utils.getTypeName(x) + ',' + Utils.getTypeName(y));
        }
    }

    @TypeSystemReference(RTypes.class)
    public static final class CADRNode extends FFIUpCallNode.Arg1 {
        @Child private CDRNode cdr = CDRNode.create();
        @Child private CARNode car = CARNode.create();

        @Override
        public Object executeObject(Object x) {
            return car.executeObject(cdr.executeObject(x));
        }

        public static CADRNode create() {
            return new CADRNode();
        }
    }

    @TypeSystemReference(RTypes.class)
    public static final class CDARNode extends FFIUpCallNode.Arg1 {
        @Child private CARNode car = CARNode.create();
        @Child private CDRNode cdr = CDRNode.create();

        @Override
        public Object executeObject(Object x) {
            return cdr.executeObject(car.executeObject(x));
        }

        public static CDARNode create() {
            return new CDARNode();
        }
    }

    @TypeSystemReference(RTypes.class)
    public static final class CADDRNode extends FFIUpCallNode.Arg1 {
        @Child private CARNode car = CARNode.create();
        @Child private CDRNode cdr1 = CDRNode.create();
        @Child private CDRNode cdr2 = CDRNode.create();

        @Override
        public Object executeObject(Object x) {
            return car.executeObject(cdr1.executeObject(cdr2.executeObject(x)));
        }

        public static CADDRNode create() {
            return new CADDRNode();
        }
    }

    @TypeSystemReference(RTypes.class)
    public static final class CADDDRNode extends FFIUpCallNode.Arg1 {
        @Child private CARNode car = CARNode.create();
        @Child private CDRNode cdr1 = CDRNode.create();
        @Child private CDRNode cdr2 = CDRNode.create();
        @Child private CDRNode cdr3 = CDRNode.create();

        @Override
        public Object executeObject(Object x) {
            return car.executeObject(cdr1.executeObject(cdr2.executeObject(cdr3.executeObject(x))));
        }

        public static CADDDRNode create() {
            return new CADDDRNode();
        }
    }

    @TypeSystemReference(RTypes.class)
    public static final class CAD4RNode extends FFIUpCallNode.Arg1 {
        @Child private CADDDRNode cadddr = CADDDRNode.create();
        @Child private CDRNode cdr = CDRNode.create();

        @Override
        public Object executeObject(Object x) {
            return cadddr.executeObject(cdr.executeObject(x));
        }

        public static CAD4RNode create() {
            return new CAD4RNode();
        }
    }

    @TypeSystemReference(RTypes.class)
    public static final class CDDRNode extends FFIUpCallNode.Arg1 {
        @Child private CDRNode cdr1 = CDRNode.create();
        @Child private CDRNode cdr2 = CDRNode.create();

        @Override
        public Object executeObject(Object x) {
            return cdr1.executeObject(cdr2.executeObject(x));
        }

        public static CDDRNode create() {
            return new CDDRNode();
        }
    }

    @TypeSystemReference(RTypes.class)
    public static final class CDDDRNode extends FFIUpCallNode.Arg1 {
        @Child private CDDRNode cddr = CDDRNode.create();
        @Child private CDRNode cdr = CDRNode.create();

        @Override
        public Object executeObject(Object x) {
            return cdr.executeObject(cddr.executeObject(x));
        }

        public static CDDDRNode create() {
            return new CDDDRNode();
        }
    }
}
