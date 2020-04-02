/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.attributes.CopyOfRegAttributesNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetNamesAttributeNode;
import com.oracle.truffle.r.nodes.helpers.InheritsCheckNode;
import com.oracle.truffle.r.nodes.helpers.RFactorNodes;
import com.oracle.truffle.r.nodes.unary.CastComplexNode;
import com.oracle.truffle.r.nodes.unary.CastDoubleNode;
import com.oracle.truffle.r.nodes.unary.CastExpressionNode;
import com.oracle.truffle.r.nodes.unary.CastIntegerNode;
import com.oracle.truffle.r.nodes.unary.CastListNode;
import com.oracle.truffle.r.nodes.unary.CastLogicalNode;
import com.oracle.truffle.r.runtime.nodes.unary.CastNode;
import com.oracle.truffle.r.nodes.unary.CastRawNode;
import com.oracle.truffle.r.nodes.unary.CastStringNode;
import com.oracle.truffle.r.nodes.unary.CastSymbolNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.data.RBaseObject;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RSharingAttributeStorage;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.gnur.SEXPTYPE;

public final class CoerceNodes {

    public static final class VectorToPairListNode extends FFIUpCallNode.Arg1 {

        @Child private CastPairListNode cast = CastPairListNode.create(SEXPTYPE.LISTSXP);

        @Override
        public Object executeObject(Object value) {
            return cast.doCast(value);
        }

        public static VectorToPairListNode create() {
            return new VectorToPairListNode();
        }
    }

    public abstract static class CastPairListNode extends CastNode {

        private final SEXPTYPE type;

        @Child private CopyOfRegAttributesNode copyRegAttributesNode = CopyOfRegAttributesNode.create();
        @Child private GetNamesAttributeNode getNamesAttributeNode = GetNamesAttributeNode.create();

        protected CastPairListNode(SEXPTYPE type) {
            this.type = type;
        }

        @Specialization
        protected Object convert(RAbstractVector v) {
            RStringVector names = getNamesAttributeNode.getNames(v);
            SEXPTYPE gnurType = SEXPTYPE.gnuRTypeForObject(v);

            Object current = RNull.instance;
            assert names == null || names.getLength() == v.getLength();
            for (int i = v.getLength() - 1; i >= 0; i--) {
                Object element = v.getDataAtAsObject(i);
                adjustSharing(v, element);
                current = RDataFactory.createPairList(element, current, names != null ? RDataFactory.createSymbol(Utils.intern(names.getDataAt(i))) : RNull.instance, gnurType);
            }
            if (current != RNull.instance) {
                // also copy regular attributes
                RPairList pl = (RPairList) current;
                copyRegAttributesNode.execute(v, pl);
                pl.setType(type);
                pl.allowClosure();
            }
            return current;
        }

        @Specialization
        protected Object convert(RNull x) {
            if (type == SEXPTYPE.LISTSXP) {
                return x;
            } else {
                return doFallback(x);
            }
        }

        @Specialization
        protected Object convert(RPairList list) {
            if (list.isLanguage() || type == SEXPTYPE.LISTSXP) {
                return list;
            } else {
                return doFallback(list);
            }
        }

        @Fallback
        @TruffleBoundary
        protected Object doFallback(Object x) {
            throw error(Message.UNIMPLEMENTED_TYPE_IN_FUNCTION, Utils.getTypeName(x), "coercePairList");
        }

        private static void adjustSharing(RAbstractVector origin, Object element) {
            if (RSharingAttributeStorage.isShareable(origin)) {

                int v = getSharingLevel(origin);
                if (element instanceof RSharingAttributeStorage) {
                    RSharingAttributeStorage r = (RSharingAttributeStorage) element;
                    if (v >= 2) {
                        // we play it safe: if the caller wants this instance to be shared, they may
                        // expect it to never become non-shared again, which could happen in FastR
                        r.makeSharedPermanent();
                    }
                    if (v == 1 && r.isTemporary()) {
                        r.incRefCount();
                    }
                }
            }
        }

        public static CastPairListNode create(SEXPTYPE type) {
            return CoerceNodesFactory.CastPairListNodeGen.create(type);
        }

        private static int getSharingLevel(RSharingAttributeStorage r) {
            return r.isTemporary() ? 0 : r.isShared() ? 2 : 1;
        }
    }

    public abstract static class AsCharacterFactor extends FFIUpCallNode.Arg1 {

        @Child private InheritsCheckNode inheritsFactorNode = InheritsCheckNode.createFactor();
        @Child private RFactorNodes.GetLevels getLevels = RFactorNodes.GetLevels.create();

        @Specialization
        protected Object doFactor(RIntVector o) {
            if (!inheritsFactorNode.execute(o)) {
                throw RError.error(RError.SHOW_CALLER2, RError.Message.GENERIC, "attempting to coerce non-factor");
            }

            RStringVector levels = getLevels.execute(o);
            if (levels == null) {
                throw RError.error(RError.SHOW_CALLER2, RError.Message.GENERIC, "malformed factor");
            }

            String[] data = new String[o.getLength()];
            boolean isComplete = true;
            int nl = levels.getLength();
            for (int i = 0; i < o.getLength(); i++) {
                assert !o.isComplete() || o.getDataAt(i) != RRuntime.INT_NA;
                int idx = o.getDataAt(i);
                if (idx == RRuntime.INT_NA) {
                    data[i] = RRuntime.STRING_NA;
                    isComplete = false;
                } else if (idx >= 1 && idx <= nl) {
                    data[i] = levels.getDataAt(idx - 1);
                } else {
                    throw RError.error(RError.SHOW_CALLER2, RError.Message.GENERIC, "malformed factor");
                }
            }

            return RDataFactory.createStringVector(data, isComplete);
        }

        public static AsCharacterFactor create() {
            return CoerceNodesFactory.AsCharacterFactorNodeGen.create();
        }

    }

    /**
     * Implements Rf_coerceVector.
     */
    @GenerateUncached
    public abstract static class CoerceVectorNode extends FFIUpCallNode.Arg2 {

        public static CoerceVectorNode create() {
            return CoerceNodesFactory.CoerceVectorNodeGen.create();
        }

        public static CoerceVectorNode getUncached() {
            return CoerceNodesFactory.CoerceVectorNodeGen.getUncached();
        }

        @Specialization(guards = "value.isS4()")
        Object doS4Object(@SuppressWarnings("unused") RBaseObject value, @SuppressWarnings("unused") int mode) {
            throw RError.nyi(RError.NO_CALLER, "Rf_coerceVector for S4 objects.");
        }

        // Note: caches should cover all valid possibilities
        @Specialization(guards = {"!isS4Object(value)", "cachedMode == mode", "isValidMode(cachedMode)", "isNotList(value)"}, limit = "99")
        Object doCachedNotList(Object value, @SuppressWarnings("unused") int mode,
                        @Cached("mode") @SuppressWarnings("unused") int cachedMode,
                        @Cached("createCastNode(cachedMode)") CastNode castNode) {
            return castNode.doCast(value);
        }

        // Lists are coerced with only preserved names unlike other types
        @Specialization(guards = {"!isS4Object(value)", "cachedMode == mode", "isValidMode(cachedMode)"}, limit = "99")
        Object doCached(RList value, @SuppressWarnings("unused") int mode,
                        @Cached("mode") @SuppressWarnings("unused") int cachedMode,
                        @Cached("createCastNodeForList(cachedMode)") CastNode castNode) {
            return castNode.doCast(value);
        }

        @Specialization(replaces = {"doCachedNotList", "doCached"}, guards = {"!isS4Object(value)", "isValidMode(mode)"})
        Object doGeneric(Object value, int mode) {
            CompilerDirectives.transferToInterpreter();
            String type = value != null ? value.getClass().getSimpleName() : "null";
            throw RInternalError.unimplemented("Rf_coerceVector unimplemented for type %s or mode %s.", type, mode);
        }

        @Fallback
        Object doFallback(Object value, Object mode) {
            CompilerDirectives.transferToInterpreter();
            String type = value != null ? value.getClass().getSimpleName() : "null";
            throw RInternalError.unimplemented("Rf_coerceVector unimplemented for type %s or mode %s.", type, mode);
        }

        static boolean isS4Object(Object obj) {
            return obj instanceof RBaseObject && ((RBaseObject) obj).isS4();
        }

        static boolean isNotList(Object obj) {
            return !(obj instanceof RList);
        }

        static boolean isValidMode(int mode) {
            return mode >= SEXPTYPE.NILSXP.code && mode <= SEXPTYPE.RAWSXP.code;
        }

        static CastNode createCastNode(int mode) {
            return createCastNode(mode, false);
        }

        static CastNode createCastNodeForList(int mode) {
            return createCastNode(mode, true);
        }

        private static CastNode createCastNode(int mode, boolean forList) {
            SEXPTYPE type = SEXPTYPE.mapInt(mode);
            boolean preserveDims = !forList;
            boolean preserveAttrs = !forList;
            switch (type) {
                case SYMSXP:
                    return CastSymbolNode.createForRFFI(false, false, false);
                case NILSXP:
                    return new CastNullNode();
                case LISTSXP:
                case LANGSXP:
                    return CastPairListNode.create(type);
                case ENVSXP:
                    return new EnvironmentCast();
                case VECSXP:
                    return CastListNode.createForRFFI(true, forList, forList);
                case EXPRSXP:
                    return CastExpressionNode.createForRFFI();
                case INTSXP:
                    return CastIntegerNode.createForRFFI(true, preserveDims, preserveAttrs);
                case REALSXP:
                    return CastDoubleNode.createForRFFI(true, preserveDims, preserveAttrs);
                case LGLSXP:
                    return CastLogicalNode.createForRFFI(true, preserveDims, preserveAttrs);
                case STRSXP:
                    return CastStringNode.createForRFFI(true, preserveDims, preserveAttrs);
                case CPLXSXP:
                    return CastComplexNode.createForRFFI(true, preserveDims, preserveAttrs);
                case RAWSXP:
                    return CastRawNode.createForRFFI(true, preserveDims, preserveAttrs);
                default:
                    throw RInternalError.unimplemented("Rf_coerceVector called with unimplemented mode %d (type %s).", mode, type);
            }
        }

        private static final class CastNullNode extends CastNode {
            @Override
            @TruffleBoundary
            public Object execute(Object value) {
                if (value instanceof RList) {
                    throw RError.error(RError.NO_CALLER, Message.UNIMPLEMENTED_TYPE_IN_FUNCTION, "list", "coerceVectorList");
                } else {
                    throw RError.error(RError.NO_CALLER, Message.CANNOT_COERCE, getTypeName(value), "NULL");
                }
            }

            private static String getTypeName(Object val) {
                Object value = RRuntime.convertScalarVectors(val);
                if (value == null) {
                    return "null";
                }
                return value instanceof RBaseObject ? ((RBaseObject) value).getRType().getName() : value.getClass().getSimpleName();
            }
        }

        private static final class EnvironmentCast extends CastNode {
            @Override
            @TruffleBoundary
            public Object execute(Object value) {
                throw RError.error(RError.NO_CALLER, Message.ENVIRONMENTS_COERCE);
            }
        }
    }
}
