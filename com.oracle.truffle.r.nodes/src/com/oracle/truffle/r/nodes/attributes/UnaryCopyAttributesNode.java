/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.attributes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetDimAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetDimNamesAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetNamesAttributeNode;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

/**
 * Copies all attributes from source to target including 'names', 'dimNames' and 'dim' (unlike
 * {@link CopyOfRegAttributesNode}), additionally removes the 'dim' from the result if it is not
 * present in the source.
 *
 * TODO: this logic is duplicated in RVector#copyRegAttributesFrom and UnaryMapNode, but behind
 * TruffleBoundary, does it have a reason for TruffleBoundary? Can we replace it with this node?
 */
public abstract class UnaryCopyAttributesNode extends RBaseNode {

    protected final boolean copyAllAttributes;

    @Child protected HasFixedAttributeNode hasDimNode = HasFixedAttributeNode.createDim();
    @Child protected GetDimNamesAttributeNode getDimNamesNode = GetDimNamesAttributeNode.create();
    @Child protected GetNamesAttributeNode getNamesNode = GetNamesAttributeNode.create();

    protected UnaryCopyAttributesNode(boolean copyAllAttributes) {
        this.copyAllAttributes = copyAllAttributes;
    }

    public static UnaryCopyAttributesNode create() {
        return UnaryCopyAttributesNodeGen.create(true);
    }

    public abstract RAbstractVector execute(RAbstractVector target, RAbstractVector left);

    protected boolean containsMetadata(RAbstractVector vector) {
        return vector instanceof RVector && hasDimNode.execute(vector) || (copyAllAttributes && vector.getAttributes() != null) || getNamesNode.getNames(vector) != null ||
                        getDimNamesNode.getDimNames(vector) != null;
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "!containsMetadata(source)")
    protected RAbstractVector copyNoMetadata(RAbstractVector target, RAbstractVector source) {
        return target;
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"copyAllAttributes", "target == source"})
    protected RAbstractVector copySameVector(RAbstractVector target, RAbstractVector source) {
        return target;
    }

    @Specialization(guards = {"!copyAllAttributes || target != source", "containsMetadata(source)"})
    protected RAbstractVector copySameLength(RAbstractVector target, RAbstractVector source, //
                    @Cached("create()") CopyOfRegAttributesNode copyOfReg, //
                    @Cached("createDim()") RemoveFixedAttributeNode removeDim, //
                    @Cached("createDimNames()") RemoveFixedAttributeNode removeDimNames, //
                    @Cached("create()") InitAttributesNode initAttributes, //
                    @Cached("createNames()") SetFixedAttributeNode putNames, //
                    @Cached("createDim()") SetFixedAttributeNode putDim, //
                    @Cached("createDimNames()") SetFixedAttributeNode putDimNames, //
                    @Cached("createBinaryProfile()") ConditionProfile noDimensions, //
                    @Cached("createBinaryProfile()") ConditionProfile hasNamesSource, //
                    @Cached("createBinaryProfile()") ConditionProfile hasDimNames,
                    @Cached("create()") GetDimAttributeNode getDimsNode) {
        RVector<?> result = target.materialize();

        if (copyAllAttributes) {
            copyOfReg.execute(source, result);
        }

        int[] newDimensions = getDimsNode.getDimensions(source);
        if (noDimensions.profile(newDimensions == null)) {
            DynamicObject attributes = result.getAttributes();
            if (attributes != null) {
                removeDim.execute(attributes);
                removeDimNames.execute(attributes);
            }

            RStringVector vecNames = getNamesNode.getNames(source);
            if (hasNamesSource.profile(vecNames != null)) {
                putNames.execute(initAttributes.execute(result), vecNames);
                return result;
            }
            return result;
        }

        putDim.execute(initAttributes.execute(result), RDataFactory.createIntVector(newDimensions, RDataFactory.COMPLETE_VECTOR));

        RList newDimNames = getDimNamesNode.getDimNames(source);
        if (hasDimNames.profile(newDimNames != null)) {
            putDimNames.execute(result.getAttributes(), newDimNames);
            newDimNames.elementNamePrefix = RRuntime.DIMNAMES_LIST_ELEMENT_NAME_PREFIX;
            return result;
        }
        return result;
    }
}
