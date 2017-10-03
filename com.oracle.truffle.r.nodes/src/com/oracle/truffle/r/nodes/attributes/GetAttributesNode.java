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

package com.oracle.truffle.r.nodes.attributes;

import java.util.Arrays;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetDimNamesAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.SetNamesAttributeNode;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RAttributesLayout;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RLanguage;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

/**
 * Transforms the attributes from {@link DynamicObject} into {@link RList}.
 */
public abstract class GetAttributesNode extends RBaseNode {

    private final BranchProfile rownamesBranch = BranchProfile.create();
    private final BranchProfile namesBranch = BranchProfile.create();
    private final ConditionProfile namesOneDimTest = ConditionProfile.createBinaryProfile();
    private final ConditionProfile dimNamesNullTest = ConditionProfile.createBinaryProfile();
    @Child private ArrayAttributeNode arrayAttrAccess = ArrayAttributeNode.create();
    @Child private SetNamesAttributeNode setNamesNode = SetNamesAttributeNode.create();
    @Child private GetDimNamesAttributeNode getDimNamesNode = GetDimNamesAttributeNode.create();

    public static GetAttributesNode create() {
        return GetAttributesNodeGen.create();
    }

    public abstract Object execute(RAttributable attributable);

    @Specialization
    protected Object attributesNull(RAbstractContainer container,
                    @Cached("createBinaryProfile()") ConditionProfile hasAttributesProfile) {
        if (hasAttributesProfile.profile(hasAttributes(container))) {
            return createResult(container, container instanceof RLanguage);
        } else {
            return RNull.instance;
        }
    }

    /**
     * Unusual cases that it is not worth specializing on as they are not performance-centric,
     * basically any type that is not an {@link RAbstractContainer} but is {@link RAttributable},
     * e.g. {@link REnvironment}.
     */
    @Fallback
    @TruffleBoundary
    protected Object attributes(RAttributable object) {
        if (hasAttributes(object)) {
            return createResult(object, false);
        } else {
            return RNull.instance;
        }
    }

    public static Object getFullRowNames(Object a) {
        if (a == RNull.instance) {
            return RNull.instance;
        } else {
            if (a instanceof RAbstractIntVector) {
                RAbstractIntVector rowNames = (RAbstractIntVector) a;
                if (rowNames.getLength() == 2 && RRuntime.isNA(rowNames.getDataAt(0))) {
                    return RDataFactory.createIntSequence(1, 1, Math.abs(rowNames.getDataAt(1)));
                }
            }
            return a;
        }
    }

    /**
     * {@code language} objects behave differently regarding "names"; they don't get included.
     */
    private Object createResult(RAttributable attributable, boolean ignoreNames) {
        DynamicObject attributes = attributable.getAttributes();
        int size = attributes.size();
        String[] names = new String[size];
        Object[] values = new Object[size];
        int z = 0;
        for (RAttributesLayout.RAttribute attr : arrayAttrAccess.execute(attributes)) {
            String name = attr.getName();
            if (ignoreNames && name.equals(RRuntime.NAMES_ATTR_KEY)) {
                continue;
            }
            names[z] = name;
            if (name.equals(RRuntime.NAMES_ATTR_KEY)) {
                namesBranch.enter();
                RList dimNames = getDimNamesNode.getDimNames(attributable);
                if (namesOneDimTest.profile(dimNames != null && dimNames.getLength() == 1)) {
                    // "dimnames" shadow "names" as long as the vector is one-dimensional
                    Object dimNamesOneDim = dimNames.getDataAt(0);
                    if (dimNamesNullTest.profile(dimNamesOneDim == null)) {
                        values[z] = attr.getValue();
                    } else {
                        values[z] = dimNamesOneDim;
                    }
                } else {
                    values[z] = attr.getValue();
                }
            } else if (name.equals(RRuntime.ROWNAMES_ATTR_KEY)) {
                rownamesBranch.enter();
                values[z] = getFullRowNames(attr.getValue());
            } else {
                values[z] = attr.getValue();
            }
            z++;
        }
        if (ignoreNames && z != names.length) {
            if (z == 0) {
                return RNull.instance;
            } else {
                names = Arrays.copyOfRange(names, 0, names.length - 1);
                values = Arrays.copyOfRange(values, 0, values.length - 1);
            }
        }
        RList result = RDataFactory.createList(values);
        setNamesNode.setNames(result, RDataFactory.createStringVector(names, true));
        return result;
    }

    private static boolean hasAttributes(RAttributable attributable) {
        return attributable.getAttributes() != null && !attributable.getAttributes().isEmpty();
    }
}
