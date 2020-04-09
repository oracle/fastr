/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.notEmpty;
import static com.oracle.truffle.r.runtime.RError.Message.LENGTH_ZERO_DIM_INVALID;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.data.AbstractContainerLibrary;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary;
import com.oracle.truffle.r.runtime.data.nodes.attributes.RemoveFixedAttributeNode;
import com.oracle.truffle.r.runtime.data.nodes.attributes.SetFixedAttributeNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RDispatch;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RAttributesLayout;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.data.nodes.VectorReuse;

@RBuiltin(name = "dim<-", kind = PRIMITIVE, dispatch = RDispatch.INTERNAL_GENERIC, parameterNames = {"x", "value"}, behavior = PURE)
public abstract class UpdateDim extends RBuiltinNode.Arg2 {

    static {
        Casts casts = new Casts(UpdateDim.class);
        casts.arg("x"); // disallows null
        casts.arg("value").allowNull().asIntegerVector().mustBe(notEmpty(), LENGTH_ZERO_DIM_INVALID);
    }

    @Specialization(guards = "reuseNonSharedNode.supports(vector)", limit = "getVectorAccessCacheSize()")
    protected RAbstractVector updateDimNull(RAbstractVector vector, @SuppressWarnings("unused") RNull dimensions,
                    @Cached("createNonShared(vector)") VectorReuse reuseNonSharedNode) {
        RAbstractVector result = reuseNonSharedNode.getMaterializedResult(vector);
        // Note: resetDimensions already removes names and dimnames
        result.resetDimensions(null);
        return result;
    }

    @Specialization(replaces = "updateDimNull")
    protected RAbstractVector updateDimNullGeneric(RAbstractVector vector, RNull dimensions,
                    @Cached("createNonSharedGeneric()") VectorReuse reuseNonSharedNode) {
        return updateDimNull(vector, dimensions, reuseNonSharedNode);
    }

    @Specialization(guards = "reuseNonSharedNode.supports(vector)", limit = "getVectorAccessCacheSize()")
    protected RAbstractVector updateDim(RAbstractVector vector, RIntVector dimensions,
                    @CachedLibrary("dimensions") AbstractContainerLibrary dimsLib,
                    @CachedLibrary("vector") AbstractContainerLibrary vectorLib,
                    @CachedLibrary("dimensions.getData()") VectorDataLibrary dimsDataLib,
                    @Cached("createBinaryProfile()") ConditionProfile initAttrProfile,
                    @Cached("createDim()") SetFixedAttributeNode putDimensions,
                    @Cached("createNames()") RemoveFixedAttributeNode removeNames,
                    @Cached("createDimNames()") RemoveFixedAttributeNode removeDimNames,
                    @Cached("createNonShared(vector)") VectorReuse reuseNonSharedNode) {
        // TODO: materialization of the dims may not be necessary anymore when all integer vectors
        // are collapsed into RIntVector, the only issues may be if some code assumes that
        // dimensions are always writeable
        RIntVector dimensionsMaterialized = (RIntVector) dimsLib.materialize(dimensions);
        int[] dimsData = dimsDataLib.getReadonlyIntData(dimensions.getData());
        RAbstractVector.verifyDimensions(vectorLib.getLength(vector), dimsData, this);
        RAbstractVector result = reuseNonSharedNode.getMaterializedResult(vector);
        removeNames.execute(result);
        removeDimNames.execute(result);

        DynamicObject attrs = result.getAttributes();
        if (initAttrProfile.profile(attrs == null)) {
            attrs = RAttributesLayout.createDim(dimensionsMaterialized);
            result.initAttributes(attrs);
        } else {
            putDimensions.setAttr(result, dimensionsMaterialized);
        }
        return result;
    }

    @Specialization(replaces = "updateDim", limit = "getVectorAccessCacheSize()")
    protected RAbstractVector updateDimGeneric(RAbstractVector vector, RIntVector dimensions,
                    @CachedLibrary("dimensions") AbstractContainerLibrary dimsLib,
                    @CachedLibrary("vector") AbstractContainerLibrary vectorLib,
                    @CachedLibrary("dimensions.getData()") VectorDataLibrary dimsDataLib,
                    @Cached("createBinaryProfile()") ConditionProfile initAttrProfile,
                    @Cached("createDim()") SetFixedAttributeNode putDimensions,
                    @Cached("createNames()") RemoveFixedAttributeNode removeNames,
                    @Cached("createDimNames()") RemoveFixedAttributeNode removeDimNames,
                    @Cached("createNonSharedGeneric()") VectorReuse reuseNonSharedNode) {
        return updateDim(vector, dimensions, dimsLib, vectorLib, dimsDataLib, initAttrProfile, putDimensions, removeNames, removeDimNames, reuseNonSharedNode);
    }

    @Specialization(guards = "!isRAbstractVector(obj)")
    protected RAbstractVector noVector(@SuppressWarnings("unused") Object obj, @SuppressWarnings("unused") RIntVector dimensions) {
        throw error(RError.Message.INVALID_FIRST_ARGUMENT);
    }
}
