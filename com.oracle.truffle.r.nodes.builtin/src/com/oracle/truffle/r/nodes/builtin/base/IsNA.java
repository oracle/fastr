/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.RDispatch.INTERNAL_GENERIC;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.data.nodes.attributes.SpecialAttributesFunctions.ExtractDimNamesAttributeNode;
import com.oracle.truffle.r.runtime.data.nodes.attributes.SpecialAttributesFunctions.ExtractNamesAttributeNode;
import com.oracle.truffle.r.runtime.data.nodes.attributes.SpecialAttributesFunctions.GetDimAttributeNode;
import com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.base.IsNANodeGen.IsListElementNANodeGen;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDataFactory.VectorFactory;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RRaw;
import com.oracle.truffle.r.runtime.data.model.RAbstractAtomicVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess.SequentialIterator;

@ImportStatic({RRuntime.class, DSLConfig.class})
@RBuiltin(name = "is.na", kind = PRIMITIVE, parameterNames = {"x"}, dispatch = INTERNAL_GENERIC, behavior = PURE)
public abstract class IsNA extends RBuiltinNode.Arg1 {

    @Child private IsListElementNA recursiveIsNA;

    static {
        Casts.noCasts(IsNA.class);
    }

    private boolean isNARecursive(Object o) {
        if (recursiveIsNA == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            recursiveIsNA = insert(IsListElementNANodeGen.create());
        }
        return recursiveIsNA.execute(o);
    }

    public abstract Object execute(Object o);

    @Specialization
    protected byte isNA(int value) {
        return RRuntime.asLogical(RRuntime.isNA(value));
    }

    @Specialization
    protected byte isNA(double value) {
        return RRuntime.asLogical(RRuntime.isNAorNaN(value));
    }

    @Specialization
    protected byte isNA(String value) {
        return RRuntime.asLogical(RRuntime.isNA(value));
    }

    @Specialization
    protected byte isNA(byte value) {
        return RRuntime.asLogical(RRuntime.isNA(value));
    }

    @Specialization
    protected byte isNA(RComplex value) {
        return RRuntime.asLogical(RRuntime.isNA(value));
    }

    @Specialization
    protected byte isNA(@SuppressWarnings("unused") RRaw value) {
        return RRuntime.LOGICAL_FALSE;
    }

    @Specialization(guards = "access.supports(vector)", limit = "getVectorAccessCacheSize()")
    protected RLogicalVector isNACached(RAbstractVector vector,
                    @Cached("vector.access()") VectorAccess access,
                    @Cached("create()") VectorFactory factory,
                    @Cached("create()") GetDimAttributeNode getDimsNode,
                    @Cached("create()") ExtractNamesAttributeNode extractNamesNode,
                    @Cached("create()") ExtractDimNamesAttributeNode extractDimNamesNode) {
        SequentialIterator iter = access.access(vector);
        byte[] data = new byte[access.getLength(iter)];
        while (access.next(iter)) {
            boolean isNA;
            switch (access.getType()) {
                case Double:
                    isNA = access.na.checkNAorNaN(access.getDouble(iter));
                    break;
                case Character:
                case Complex:
                case Integer:
                case Logical:
                    isNA = access.isNA(iter);
                    break;
                case Raw:
                    isNA = false;
                    break;
                case List:
                    // result is false unless that element is a length-one vector (incl. lists)
                    // and the single element of that vector is regarded as NA
                    isNA = isNARecursive(access.getListElement(iter));
                    break;
                case Expression:
                    isNA = false;
                    break;
                default:
                    throw RInternalError.shouldNotReachHere();

            }
            data[iter.getIndex()] = RRuntime.asLogical(isNA);
        }
        return factory.createLogicalVector(data, RDataFactory.COMPLETE_VECTOR, getDimsNode.getDimensions(vector), extractNamesNode.execute(vector), extractDimNamesNode.execute(vector));
    }

    @Specialization(replaces = "isNACached")
    protected RLogicalVector isNAGeneric(RAbstractVector vector,
                    @Cached("create()") VectorFactory factory,
                    @Cached("create()") GetDimAttributeNode getDimsNode,
                    @Cached("create()") ExtractNamesAttributeNode extractNamesNode,
                    @Cached("create()") ExtractDimNamesAttributeNode extractDimNamesNode) {
        return isNACached(vector, vector.slowPathAccess(), factory, getDimsNode, extractNamesNode, extractDimNamesNode);
    }

    @Specialization
    protected RLogicalVector isNA(@SuppressWarnings("unused") RNull value) {
        return RDataFactory.createEmptyLogicalVector();
    }

    @Specialization(guards = "isForeignObject(obj)")
    protected byte isNA(@SuppressWarnings("unused") TruffleObject obj) {
        return RRuntime.LOGICAL_FALSE;
    }

    @Fallback
    protected byte isNA(Object value) {
        warning(RError.Message.IS_NA_TO_NON_VECTOR, Predef.getTypeName(value));
        return RRuntime.LOGICAL_FALSE;
    }

    @ImportStatic(DSLConfig.class)
    public abstract static class IsListElementNA extends Node {

        public abstract boolean execute(Object o);

        @Specialization
        protected boolean isNA(int value) {
            return RRuntime.isNA(value);
        }

        @Specialization
        protected boolean isNA(double value) {
            return RRuntime.isNAorNaN(value);
        }

        @Specialization
        protected boolean isNA(String value) {
            return RRuntime.isNA(value);
        }

        @Specialization
        protected boolean isNA(byte value) {
            return RRuntime.isNA(value);
        }

        @Specialization
        protected boolean isNA(RComplex value) {
            return RRuntime.isNA(value);
        }

        @Specialization
        protected boolean isNA(@SuppressWarnings("unused") RRaw value) {
            return false;
        }

        @Specialization(guards = "access.supports(vector)", limit = "getVectorAccessCacheSize()")
        protected boolean isNACached(RAbstractAtomicVector vector,
                        @Cached("vector.access()") VectorAccess access) {
            return isNAVector(vector, access);
        }

        @Specialization(replaces = "isNACached")
        protected boolean isNAGeneric(RAbstractAtomicVector vector) {
            return isNAVector(vector, vector.slowPathAccess());
        }

        @Fallback
        protected boolean doOthers(@SuppressWarnings("unused") Object obj) {
            return false;
        }

        private static boolean isNAVector(RAbstractAtomicVector vector, VectorAccess access) {
            SequentialIterator iter = access.access(vector);
            if (access.getLength(iter) != 1) {
                return false;
            }
            access.next(iter);
            switch (access.getType()) {
                case Double:
                    return access.na.checkNAorNaN(access.getDouble(iter));
                case Character:
                case Complex:
                case Integer:
                case Logical:
                    return access.isNA(iter);
                case Raw:
                    return false;
                default:
                    CompilerDirectives.transferToInterpreter();
                    throw RInternalError.shouldNotReachHere("Unexpected atomic vector type " + access.getType());
            }
        }
    }
}
