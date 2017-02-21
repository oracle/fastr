/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.nodes.ffi;

import static com.oracle.truffle.r.nodes.ffi.RFFIUtils.unimplemented;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.unary.CastComplexNode;
import com.oracle.truffle.r.nodes.unary.CastDoubleNode;
import com.oracle.truffle.r.nodes.unary.CastExpressionNode;
import com.oracle.truffle.r.nodes.unary.CastIntegerNode;
import com.oracle.truffle.r.nodes.unary.CastListNode;
import com.oracle.truffle.r.nodes.unary.CastLogicalNode;
import com.oracle.truffle.r.nodes.unary.CastNode;
import com.oracle.truffle.r.nodes.unary.CastRawNode;
import com.oracle.truffle.r.nodes.unary.CastStringNode;
import com.oracle.truffle.r.nodes.unary.CastSymbolNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RTypedValue;
import com.oracle.truffle.r.runtime.gnur.SEXPTYPE;

/**
 * Implements Rf_coerceVector.
 */
public abstract class CoerceVectorNode extends FFIUpCallNode.Arg2 {

    public static CoerceVectorNode create() {
        return CoerceVectorNodeGen.create();
    }

    @Specialization(guards = "value.isS4()")
    Object doS4Object(@SuppressWarnings("unused") RTypedValue value, @SuppressWarnings("unused") int mode) {
        throw RError.nyi(RError.NO_CALLER, "Rf_coerceVector for S4 objects.");
    }

    // Note: caches should cover all valid possibilities
    @Specialization(guards = {"!isS4Object(value)", "isNotList(value)", "isValidMode(mode)", "cachedMode == mode"}, limit = "99")
    Object doCached(Object value, @SuppressWarnings("unused") int mode,
                    @Cached("mode") @SuppressWarnings("unused") int cachedMode,
                    @Cached("createCastNode(cachedMode)") CastNode castNode) {
        return castNode.execute(value);
    }

    // Lists are coerced with only preserved names unlike other types
    @Specialization(guards = {"!isS4Object(value)", "isValidMode(mode)", "cachedMode == mode"}, limit = "99")
    Object doCached(RList value, @SuppressWarnings("unused") int mode,
                    @Cached("mode") @SuppressWarnings("unused") int cachedMode,
                    @Cached("createCastNodeForList(cachedMode)") CastNode castNode) {
        return castNode.execute(value);
    }

    @Fallback
    @TruffleBoundary
    Object doFallback(Object value, Object mode) {
        String type = value != null ? value.getClass().getSimpleName() : "null";
        throw unimplemented(String.format("Rf_coerceVector unimplemented for type %s or mode %s.", type, mode));
    }

    static boolean isS4Object(Object obj) {
        return obj instanceof RTypedValue && ((RTypedValue) obj).isS4();
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
                throw unimplemented("Rf_coerceVector called with unimplemented for PairLists.");
            case LANGSXP:
                throw unimplemented("Rf_coerceVector called with unimplemented for RLanguage.");
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
                throw unimplemented(String.format("Rf_coerceVector called with unimplemented mode %d (type %s).", mode, type));
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
            Object value = RRuntime.asAbstractVector(val);
            if (value == null) {
                return "null";
            }
            return value instanceof RTypedValue ? ((RTypedValue) value).getRType().getName() : value.getClass().getSimpleName();
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
