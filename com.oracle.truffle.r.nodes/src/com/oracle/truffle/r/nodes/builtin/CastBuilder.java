/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin;

import java.util.Arrays;

import com.oracle.truffle.r.nodes.binary.BoxPrimitiveNodeGen;
import com.oracle.truffle.r.nodes.unary.CastDoubleNodeGen;
import com.oracle.truffle.r.nodes.unary.CastIntegerNodeGen;
import com.oracle.truffle.r.nodes.unary.CastLogicalNodeGen;
import com.oracle.truffle.r.nodes.unary.CastNode;
import com.oracle.truffle.r.nodes.unary.CastStringNodeGen;
import com.oracle.truffle.r.nodes.unary.CastToAttributableNodeGen;
import com.oracle.truffle.r.nodes.unary.CastToVectorNodeGen;
import com.oracle.truffle.r.nodes.unary.ChainedCastNode;
import com.oracle.truffle.r.nodes.unary.ConvertIntNodeGen;
import com.oracle.truffle.r.nodes.unary.FirstBooleanNodeGen;
import com.oracle.truffle.r.nodes.unary.FirstIntNode;
import com.oracle.truffle.r.nodes.unary.FirstStringNode;
import com.oracle.truffle.r.runtime.RError;

public final class CastBuilder {

    private static final CastNode[] EMPTY_CASTS_ARRAY = new CastNode[0];

    private CastNode[] casts = EMPTY_CASTS_ARRAY;

    private CastBuilder insert(int index, CastNode cast) {
        if (index >= casts.length) {
            casts = Arrays.copyOf(casts, index + 1);
        }
        if (casts[index] == null) {
            casts[index] = cast;
        } else {
            casts[index] = new ChainedCastNode(casts[index], cast);
        }
        return this;
    }

    public CastNode[] getCasts() {
        return casts;
    }

    public CastBuilder toAttributable(int index, boolean preserveNames, boolean dimensionsPreservation, boolean attrPreservation) {
        return insert(index, CastToAttributableNodeGen.create(preserveNames, dimensionsPreservation, attrPreservation));
    }

    public CastBuilder toVector(int index) {
        return toVector(index, false);
    }

    public CastBuilder toVector(int index, boolean nonVectorPreserved) {
        return insert(index, CastToVectorNodeGen.create(nonVectorPreserved));
    }

    public CastBuilder toInteger(int index) {
        return toInteger(index, false, false, false);
    }

    public CastBuilder toInteger(int index, boolean preserveNames, boolean dimensionsPreservation, boolean attrPreservation) {
        return insert(index, CastIntegerNodeGen.create(preserveNames, dimensionsPreservation, attrPreservation));
    }

    public CastBuilder toDouble(int index) {
        return toDouble(index, false, false, false);
    }

    public CastBuilder toDouble(int index, boolean preserveNames, boolean dimensionsPreservation, boolean attrPreservation) {
        return insert(index, CastDoubleNodeGen.create(preserveNames, dimensionsPreservation, attrPreservation));
    }

    public CastBuilder toLogical(int index) {
        return toLogical(index, false, false, false);
    }

    public CastBuilder toLogical(int index, boolean preserveNames, boolean dimensionsPreservation, boolean attrPreservation) {
        return insert(index, CastLogicalNodeGen.create(preserveNames, dimensionsPreservation, attrPreservation));
    }

    public CastBuilder toCharacter(int index) {
        return toCharacter(index, false, false, false, false);
    }

    public CastBuilder toCharacter(int index, boolean preserveNames, boolean dimensionsPreservation, boolean attrPreservation, boolean emptyVectorConvertedToNull) {
        return insert(index, CastStringNodeGen.create(preserveNames, dimensionsPreservation, attrPreservation, emptyVectorConvertedToNull));
    }

    public CastBuilder boxPrimitive(int index) {
        return insert(index, BoxPrimitiveNodeGen.create());
    }

    public CastBuilder custom(int index, CastNode cast) {
        return insert(index, cast);
    }

    public CastBuilder firstIntegerWithWarning(int index, int intNa, String name) {
        insert(index, CastIntegerNodeGen.create(false, false, false));
        return insert(index, FirstIntNode.createWithWarning(RError.Message.FIRST_ELEMENT_USED, name, intNa));
    }

    public CastBuilder convertToInteger(int index) {
        return insert(index, ConvertIntNodeGen.create());
    }

    public CastBuilder firstIntegerWithError(int index, RError.Message error, String name) {
        insert(index, CastIntegerNodeGen.create(false, false, false));
        return insert(index, FirstIntNode.createWithError(error, name));
    }

    public CastBuilder firstStringWithError(int index, RError.Message error, String name) {
        return insert(index, FirstStringNode.createWithError(error, name));
    }

    public CastBuilder firstBoolean(int index) {
        return insert(index, FirstBooleanNodeGen.create());
    }
}
