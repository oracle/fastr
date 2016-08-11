/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */

package com.oracle.truffle.r.nodes.binary;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.unary.CastComplexNodeGen;
import com.oracle.truffle.r.nodes.unary.CastDoubleNodeGen;
import com.oracle.truffle.r.nodes.unary.CastIntegerNodeGen;
import com.oracle.truffle.r.nodes.unary.CastListNodeGen;
import com.oracle.truffle.r.nodes.unary.CastLogicalNodeGen;
import com.oracle.truffle.r.nodes.unary.CastNode;
import com.oracle.truffle.r.nodes.unary.CastRawNodeGen;
import com.oracle.truffle.r.nodes.unary.CastStringNodeGen;
import com.oracle.truffle.r.nodes.unary.TypeofNode;
import com.oracle.truffle.r.nodes.unary.TypeofNodeGen;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

public abstract class CastTypeNode extends BinaryNode {

    protected static final int NUMBER_OF_TYPES = RType.values().length;

    @Child protected TypeofNode typeof = TypeofNodeGen.create();

    public abstract Object execute(Object value, RType type);

    @SuppressWarnings("unused")
    @Specialization(guards = "typeof.execute(value) == type")
    protected static RAbstractVector doPass(RAbstractVector value, RType type) {
        return value;
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"typeof.execute(value) != type", "type == cachedType", "!isNull(cast)"}, limit = "NUMBER_OF_TYPES")
    protected static Object doCast(RAbstractVector value, RType type, //
                    @Cached("type") RType cachedType, //
                    @Cached("createCast(cachedType)") CastNode cast) {
        return cast.execute(value);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "isNull(createCast(type))")
    @TruffleBoundary
    protected static Object doCastUnknown(RAbstractVector value, RType type) {
        // FIXME should we really return null here?
        return null;
    }

    @TruffleBoundary
    public static CastNode createCast(RType type) {
        return createCast(type, false, false, false);
    }

    public static CastTypeNode create() {
        return CastTypeNodeGen.create(null, null);
    }

    @TruffleBoundary
    public static CastNode createCast(RType type, boolean preserveNames, boolean preserveDimensions, boolean preserveAttributes) {
        switch (type) {
            case Character:
                return CastStringNodeGen.create(preserveNames, preserveDimensions, preserveAttributes);
            case Complex:
                return CastComplexNodeGen.create(preserveNames, preserveDimensions, preserveAttributes);
            case Double:
                return CastDoubleNodeGen.create(preserveNames, preserveDimensions, preserveAttributes);
            case Integer:
                return CastIntegerNodeGen.create(preserveNames, preserveDimensions, preserveAttributes);
            case Logical:
                return CastLogicalNodeGen.create(preserveNames, preserveDimensions, preserveAttributes);
            case Raw:
                return CastRawNodeGen.create(preserveNames, preserveDimensions, preserveAttributes);
            case List:
                return CastListNodeGen.create(preserveNames, preserveDimensions, preserveAttributes);
            default:
                return null;

        }
    }

    protected static boolean isNull(Object value) {
        return value == null;
    }
}
