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
package com.oracle.truffle.r.nodes.unary;

import java.util.EnumMap;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.function.ClassHierarchyNode;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RRaw;
import com.oracle.truffle.r.runtime.data.RS4Object;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.data.RTypes;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess.SequentialIterator;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

@TypeSystemReference(RTypes.class)
public abstract class ToStringNode extends RBaseNode {

    static final String DEFAULT_SEPARATOR = ", ";

    @Child private ToStringNode recursiveToString;

    private String toStringRecursive(Object o, String separator) {
        if (recursiveToString == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            recursiveToString = insert(ToStringNodeGen.create());
        }
        return recursiveToString.executeString(o, separator);
    }

    public abstract String executeString(Object o, String separator);

    @Specialization
    protected String toString(String value, @SuppressWarnings("unused") String separator) {
        return value;

    }

    @Specialization
    protected String toString(@SuppressWarnings("unused") RNull vector, @SuppressWarnings("unused") String separator) {
        return "NULL";
    }

    @Specialization
    protected String toString(RFunction function, @SuppressWarnings("unused") String separator) {
        return RRuntime.toString(function);
    }

    @Specialization
    protected String toString(RSymbol symbol, @SuppressWarnings("unused") String separator) {
        return symbol.getName();
    }

    @Specialization
    protected String toString(RComplex complex, @SuppressWarnings("unused") String separator,
                    @Cached("create()") NACheck naCheck) {
        naCheck.enable(complex);
        return naCheck.convertComplexToString(complex);
    }

    @Specialization
    protected String toString(RRaw raw, @SuppressWarnings("unused") String separator) {
        return RRuntime.rawToHexString(raw.getValue());
    }

    @Specialization
    protected String toString(int operand, @SuppressWarnings("unused") String separator) {
        return RRuntime.intToString(operand);
    }

    @Specialization
    protected String toString(double operand, @SuppressWarnings("unused") String separator,
                    @Cached("createBinaryProfile()") ConditionProfile isCachedIntProfile,
                    @Cached("create()") NACheck naCheck) {
        int intValue = (int) operand;
        if (isCachedIntProfile.profile(intValue == operand && RRuntime.isCachedNumberString(intValue))) {
            return RRuntime.getCachedNumberString(intValue);
        }
        naCheck.enable(operand);
        return naCheck.convertDoubleToString(operand);
    }

    @Specialization
    protected String toString(byte operand, @SuppressWarnings("unused") String separator) {
        return RRuntime.logicalToString(operand);
    }

    @Specialization
    @TruffleBoundary
    protected String toString(RS4Object obj, @SuppressWarnings("unused") String separator,
                    @Cached("createWithImplicit()") ClassHierarchyNode hierarchy) {
        RStringVector classHierarchy = hierarchy.execute(obj);
        if (classHierarchy.getLength() == 0) {
            throw RInternalError.shouldNotReachHere("S4 object has no class");
        }
        return "<S4 object of class \"" + classHierarchy.getDataAt(0) + "\">";
    }

    private static final EnumMap<RType, String> EMPTY = new EnumMap<>(RType.class);

    static {
        EMPTY.put(RType.Integer, "integer(0)");
        EMPTY.put(RType.Double, "numeric(0)");
        EMPTY.put(RType.Character, "character(0)");
        EMPTY.put(RType.Logical, "logical(0)");
        EMPTY.put(RType.Raw, "raw(0)");
        EMPTY.put(RType.Complex, "complex(0)");
        EMPTY.put(RType.List, "list()");
    }

    @TruffleBoundary
    private String vectorToString(RAbstractVector vector, String separator, VectorAccess vectorAccess) {
        try (SequentialIterator iter = vectorAccess.access(vector)) {
            int length = vectorAccess.getLength(iter);
            if (length == 0) {
                return EMPTY.get(vectorAccess.getType());
            }
            StringBuilder b = new StringBuilder();
            if (vectorAccess.next(iter)) {
                while (true) {
                    if (vectorAccess.getType() == RType.List) {
                        Object value = vectorAccess.getListElement(iter);
                        if (value instanceof RAbstractListVector) {
                            RAbstractListVector l = (RAbstractListVector) value;
                            if (l.getLength() == 0) {
                                b.append("list()");
                            } else {
                                b.append("list(").append(toStringRecursive(l, separator)).append(')');
                            }
                        } else {
                            b.append(toStringRecursive(value, separator));
                        }
                    } else {
                        b.append(vectorAccess.getString(iter));
                    }
                    if (!vectorAccess.next(iter)) {
                        break;
                    }
                    b.append(separator);
                }
            }
            return b.toString();
        }
    }

    @Specialization(guards = "vectorAccess.supports(vector)")
    protected String toStringVectorCached(RAbstractVector vector, String separator,
                    @Cached("vector.access()") VectorAccess vectorAccess) {
        return vectorToString(vector, separator, vectorAccess);
    }

    @Specialization(replaces = "toStringVectorCached")
    protected String toStringVectorGeneric(RAbstractVector vector, String separator) {
        return vectorToString(vector, separator, vector.slowPathAccess());
    }

    @SuppressWarnings("unused")
    @Specialization
    @TruffleBoundary
    protected String toString(REnvironment env, String separator) {
        return env.toString();
    }
}
