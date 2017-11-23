/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetDimAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetDimNamesAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetNamesAttributeNode;
import com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
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
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess.SequentialIterator;

@ImportStatic(RRuntime.class)
@RBuiltin(name = "is.na", kind = PRIMITIVE, parameterNames = {"x"}, dispatch = INTERNAL_GENERIC, behavior = PURE)
public abstract class IsNA extends RBuiltinNode.Arg1 {

    @Child private IsNA recursiveIsNA;

    @Child private VectorFactory factory = VectorFactory.create();
    @Child private GetDimAttributeNode getDimsNode = GetDimAttributeNode.create();
    @Child private GetNamesAttributeNode getNamesNode = GetNamesAttributeNode.create();
    @Child private GetDimNamesAttributeNode getDimNamesNode = GetDimNamesAttributeNode.create();

    static {
        Casts.noCasts(IsNA.class);
    }

    private Object isNARecursive(Object o) {
        if (recursiveIsNA == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            recursiveIsNA = insert(IsNANodeGen.create());
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

    private RLogicalVector isNAVector(RAbstractVector vector, VectorAccess access) {
        try (SequentialIterator iter = access.access(vector)) {
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
                        Object result = isNARecursive(access.getListElement(iter));
                        if (result instanceof Byte) {
                            isNA = ((byte) result) == RRuntime.LOGICAL_TRUE;
                        } else if (result instanceof RLogicalVector) {
                            RLogicalVector recVector = (RLogicalVector) result;
                            // result is false unless that element is a length-one atomic vector
                            // and the single element of that vector is regarded as NA
                            isNA = (recVector.getLength() == 1) ? recVector.getDataAt(0) == RRuntime.LOGICAL_TRUE : false;
                        } else {
                            throw RInternalError.shouldNotReachHere("unhandled return type in isNA(list)");
                        }
                        break;
                    default:
                        throw RInternalError.shouldNotReachHere();

                }
                data[iter.getIndex()] = RRuntime.asLogical(isNA);
            }
            return factory.createLogicalVector(data, RDataFactory.COMPLETE_VECTOR, getDimsNode.getDimensions(vector), getNamesNode.getNames(vector), getDimNamesNode.getDimNames(vector));
        }
    }

    @Specialization(guards = "access.supports(vector)")
    protected RLogicalVector isNACached(RAbstractVector vector,
                    @Cached("vector.access()") VectorAccess access) {
        return isNAVector(vector, access);
    }

    @Specialization(replaces = "isNACached")
    protected RLogicalVector isNAGeneric(RAbstractVector vector) {
        return isNAVector(vector, vector.slowPathAccess());
    }

    @Specialization
    protected RLogicalVector isNA(RNull value) {
        warning(RError.Message.IS_NA_TO_NON_VECTOR, value.getRType().getName());
        return factory.createEmptyLogicalVector();
    }

    @Specialization(guards = "isForeignObject(obj)")
    protected byte isNA(@SuppressWarnings("unused") TruffleObject obj) {
        return RRuntime.LOGICAL_FALSE;
    }

    @Fallback
    protected byte isNA(Object value) {
        warning(RError.Message.IS_NA_TO_NON_VECTOR, Predef.typeName().apply(value));
        return RRuntime.LOGICAL_FALSE;
    }
}
