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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.attributes.PutAttributeNode;
import com.oracle.truffle.r.nodes.attributes.RemoveAttributeNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.unary.CastStringNode;
import com.oracle.truffle.r.nodes.unary.CastStringNodeGen;
import com.oracle.truffle.r.nodes.unary.CastToVectorNode;
import com.oracle.truffle.r.nodes.unary.CastToVectorNodeGen;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RVisibility;
import com.oracle.truffle.r.runtime.data.RAttributes;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

@RBuiltin(name = "dimnames<-", visibility = RVisibility.ON, kind = PRIMITIVE, parameterNames = {"x", "value"})
public abstract class UpdateDimNames extends RBuiltinNode {

    protected static final String DIMNAMES_ATTR_KEY = RRuntime.DIMNAMES_ATTR_KEY;

    private final ConditionProfile shareListProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile isRVectorProfile = ConditionProfile.createBinaryProfile();

    @Child private CastStringNode castStringNode;
    @Child private CastToVectorNode castVectorNode;

    private Object castString(Object o) {
        if (castStringNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castStringNode = insert(CastStringNodeGen.create(true, true, false, false));
        }
        return castStringNode.execute(o);
    }

    private RAbstractVector castVector(Object value) {
        if (castVectorNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castVectorNode = insert(CastToVectorNodeGen.create(false));
        }
        return ((RAbstractVector) castVectorNode.execute(value)).materialize();
    }

    public abstract RAbstractContainer executeRAbstractContainer(RAbstractContainer container, Object o);

    private RList convertToListOfStrings(RList oldList) {
        RList list = oldList;
        if (shareListProfile.profile(list.isShared())) {
            list = (RList) list.copy();
        }
        for (int i = 0; i < list.getLength(); i++) {
            Object element = list.getDataAt(i);
            if (element != RNull.instance) {
                Object s = castString(castVector(element));
                list.updateDataAt(i, s, null);
            }
        }
        return list;
    }

    @Specialization
    protected RAbstractContainer updateDimnamesNull(RAbstractContainer container, @SuppressWarnings("unused") RNull list, //
                    @Cached("create(DIMNAMES_ATTR_KEY)") RemoveAttributeNode remove) {
        controlVisibility();
        RAbstractContainer result = (RAbstractContainer) container.getNonShared();
        if (isRVectorProfile.profile(container instanceof RVector)) {
            RVector vector = (RVector) container;
            if (vector.getInternalDimNames() != null) {
                vector.setInternalDimNames(null);
                remove.execute(vector.getAttributes());
            }
        } else {
            container.setDimNames(null);
        }
        return result;
    }

    @Specialization(guards = "list.getLength() == 0")
    protected RAbstractContainer updateDimnamesEmpty(RAbstractContainer container, @SuppressWarnings("unused") RList list, //
                    @Cached("create(DIMNAMES_ATTR_KEY)") RemoveAttributeNode remove) {
        return updateDimnamesNull(container, RNull.instance, remove);
    }

    @Specialization(guards = "list.getLength() > 0")
    protected RAbstractContainer updateDimnames(RAbstractContainer container, RList list, //
                    @Cached("create(DIMNAMES_ATTR_KEY)") PutAttributeNode put) {
        controlVisibility();
        RAbstractContainer result = (RAbstractContainer) container.getNonShared();
        setDimNames(result, convertToListOfStrings(list), put);
        return result;
    }

    @Specialization(guards = "!isRList(c)")
    protected RAbstractContainer updateDimnamesError(@SuppressWarnings("unused") RAbstractContainer container, @SuppressWarnings("unused") Object c) {
        controlVisibility();
        CompilerDirectives.transferToInterpreter();
        throw RError.error(this, RError.Message.DIMNAMES_LIST);
    }

    private void setDimNames(RAbstractContainer container, RList newDimNames, PutAttributeNode put) {
        assert newDimNames != null;
        if (isRVectorProfile.profile(container instanceof RVector)) {
            RVector vector = (RVector) container;
            int[] dimensions = vector.getDimensions();
            if (dimensions == null) {
                CompilerDirectives.transferToInterpreter();
                throw RError.error(this, RError.Message.DIMNAMES_NONARRAY);
            }
            int newDimNamesLength = newDimNames.getLength();
            if (newDimNamesLength > dimensions.length) {
                CompilerDirectives.transferToInterpreter();
                throw RError.error(this, RError.Message.DIMNAMES_DONT_MATCH_DIMS, newDimNamesLength, dimensions.length);
            }
            for (int i = 0; i < newDimNamesLength; i++) {
                Object dimObject = newDimNames.getDataAt(i);
                if (dimObject != RNull.instance) {
                    if (dimObject instanceof String) {
                        if (dimensions[i] != 1) {
                            CompilerDirectives.transferToInterpreter();
                            throw RError.error(this, RError.Message.DIMNAMES_DONT_MATCH_EXTENT, i + 1);
                        }
                    } else {
                        RStringVector dimVector = (RStringVector) dimObject;
                        if (dimVector == null || dimVector.getLength() == 0) {
                            newDimNames.updateDataAt(i, RNull.instance, null);
                        } else if (dimVector.getLength() != dimensions[i]) {
                            CompilerDirectives.transferToInterpreter();
                            throw RError.error(this, RError.Message.DIMNAMES_DONT_MATCH_EXTENT, i + 1);
                        }
                    }
                }
            }

            RList resDimNames = newDimNames;
            if (newDimNamesLength < dimensions.length) {
                // resize the array and fill the missing entries with NULL-s
                resDimNames = resDimNames.copyResized(dimensions.length, true);
                resDimNames.setAttributes(newDimNames);
                for (int i = newDimNamesLength; i < dimensions.length; i++) {
                    resDimNames.updateDataAt(i, RNull.instance, null);
                }
            }
            if (vector.getAttributes() == null) {
                vector.initAttributes(RAttributes.createInitialized(new String[]{RRuntime.DIMNAMES_ATTR_KEY}, new Object[]{resDimNames}));
            } else {
                put.execute(vector.getAttributes(), resDimNames);
            }
            resDimNames.elementNamePrefix = RRuntime.DIMNAMES_LIST_ELEMENT_NAME_PREFIX;
            vector.setInternalDimNames(resDimNames);
        } else {
            container.setDimNames(newDimNames);
        }
    }
}
