/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetDimAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.SetDimAttributeNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RAttributeProfiles;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

@RBuiltin(name = "drop", kind = INTERNAL, parameterNames = {"x"}, behavior = PURE)
public abstract class Drop extends RBuiltinNode {

    private final ConditionProfile nullDimensions = ConditionProfile.createBinaryProfile();
    private final RAttributeProfiles dimNamesAttrProfile = RAttributeProfiles.create();
    private final ConditionProfile resultIsVector = ConditionProfile.createBinaryProfile();
    private final ConditionProfile resultIsScalarProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile noDimNamesProfile = ConditionProfile.createBinaryProfile();

    @Specialization
    protected RAbstractVector doDrop(RAbstractVector x,
                    @Cached("create()") GetDimAttributeNode getDimsNode,
                    @Cached("create()") SetDimAttributeNode setDimsNode) {
        int[] dims = getDimsNode.getDimensions(x);
        if (nullDimensions.profile(dims == null)) {
            return x;
        }

        // check the size of new dimensions
        int newDimsLength = 0;
        int lastNonOneIndex = -1;
        for (int i = 0; i < dims.length; ++i) {
            if (dims[i] != 1) {
                newDimsLength++;
                lastNonOneIndex = i;
            }
        }

        // the result is single value, all dims == 1
        if (resultIsScalarProfile.profile(lastNonOneIndex == -1)) {
            @SuppressWarnings("unused")
            RAbstractVector r = x.copy();
            setDimsNode.setDimensions(x, null);
            x.setDimNames(null);
            x.setNames(null);
            return x;
        }

        // the result is vector
        if (resultIsVector.profile(newDimsLength <= 1)) {
            return toVector(x, lastNonOneIndex, setDimsNode);
        }

        // else: the result will be a matrix, copy non-1 dimensions
        int[] newDims = new int[newDimsLength];
        int newDimsIdx = 0;
        for (int i = 0; i < dims.length; i++) {
            if (dims[i] != 1) {
                newDims[newDimsIdx++] = dims[i];
            }
        }

        RAbstractVector result = x.copy();
        setDimsNode.setDimensions(result, newDims);

        // if necessary, copy corresponding dimnames
        RList oldDimNames = x.getDimNames(dimNamesAttrProfile);
        if (noDimNamesProfile.profile(oldDimNames != null)) {
            newDimsIdx = 0;
            Object[] newDimNames = new Object[newDimsLength];
            for (int i = 0; i < dims.length; i++) {
                if (dims[i] != 1 && i < oldDimNames.getLength()) {
                    newDimNames[newDimsIdx++] = oldDimNames.getDataAt(i);
                }
            }
            result.setDimNames(RDataFactory.createList(newDimNames));
        } else {
            result.setDimNames(null);
        }

        return result;
    }

    /**
     * Handles the case when result is just a vector. The only catch is that we might have to copy
     * corresponding index from dimnames to names attribute of the new vector.
     */
    private RAbstractVector toVector(RAbstractVector x, int nonOneIndex, SetDimAttributeNode setDimsNode) {
        RAbstractVector result = x.copy(); // share?
        setDimsNode.setDimensions(result, null);

        // copy dimnames to names if possible
        RList dimNames = x.getDimNames(dimNamesAttrProfile);
        if (noDimNamesProfile.profile(dimNames != null) && nonOneIndex < dimNames.getLength()) {
            result.setNames(ensureStringVector(dimNames.getDataAt(nonOneIndex)));
        }

        return result;
    }

    private static RStringVector ensureStringVector(Object value) {
        if (value instanceof RAbstractStringVector) {
            return ((RAbstractStringVector) value).materialize();
        } else {
            assert value instanceof String : "Drop: expected String or RAbstractStringVector in dimnames";
            return RDataFactory.createStringVector(new String[]{(String) value}, true);
        }
    }

    @Fallback
    protected Object doDrop(Object x) {
        return x;
    }
}
