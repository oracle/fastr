/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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

package com.oracle.truffle.r.runtime.data.nodes.attributes;

import java.util.Arrays;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.data.nodes.attributes.SpecialAttributesFunctions.GetDimNamesAttributeNode;
import com.oracle.truffle.r.runtime.data.nodes.attributes.SpecialAttributesFunctions.GetRowNamesAttributeNode;
import com.oracle.truffle.r.runtime.data.nodes.attributes.SpecialAttributesFunctions.SetNamesAttributeNode;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RAttributesLayout;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
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

    /**
     * If <code>row.names</code> are an int sequence, GnuR internally represents them in a compact
     * format - vector(NA, length). Depending on the use case, we either want to keep this or
     * convert to a regular FastR sequence.
     */
    private final boolean keepCompactRowNames;

    protected GetAttributesNode(boolean keepCompactRowNames) {
        this.keepCompactRowNames = keepCompactRowNames;
    }

    public static GetAttributesNode create() {
        return GetAttributesNodeGen.create(false);
    }

    /**
     * The <code>row.names</code> attribute will be kept in the compact format.
     *
     * @return <code>GetAttributesNode</code>
     * @see #keepCompactRowNames
     */
    public static GetAttributesNode createWithCompactRowNames() {
        return GetAttributesNodeGen.create(true);
    }

    public abstract Object execute(RAttributable attributable);

    @Specialization(guards = "!isRPairList(container)")
    protected Object attributesNull(RAbstractContainer container,
                    @Cached("createBinaryProfile()") ConditionProfile hasAttributesProfile) {
        if (hasAttributesProfile.profile(hasAttributes(container))) {
            return createResult(container, (container instanceof RPairList && ((RPairList) container).isLanguage()), null);
        } else {
            return RNull.instance;
        }
    }

    @Specialization
    protected Object attributesPairList(RPairList pairList,
                    @Cached("createBinaryProfile()") ConditionProfile hasNamesProfile) {
        RStringVector names = pairList.getNames();
        if (hasAttributes(pairList)) {
            return createResult(pairList, false, names);
        } else if (hasNamesProfile.profile(names != null)) {
            return RDataFactory.createList(new Object[]{names}, RDataFactory.createStringVector(RRuntime.NAMES_ATTR_KEY));
        }
        return RNull.instance;
    }

    /**
     * Unusual cases that it is not worth specializing on as they are not performance-centric,
     * basically any type that is not an {@link RAbstractContainer} but is {@link RAttributable} ,
     * e.g. {@link REnvironment}.
     */
    @Fallback
    @TruffleBoundary
    protected Object attributes(RAttributable object) {
        if (hasAttributes(object)) {
            return createResult(object, false, null);
        } else {
            return RNull.instance;
        }
    }

    /**
     * {@code language} objects behave differently regarding "names"; they don't get included.
     */
    private Object createResult(RAttributable attributable, boolean ignoreNames, RStringVector explicitNames) {
        DynamicObject attributes = attributable.getAttributes();
        int size = attributes.getShape().getPropertyCount() + (explicitNames == null ? 0 : 1);
        String[] names = new String[size];
        Object[] values = new Object[size];
        int z = 0;
        if (explicitNames != null) {
            values[0] = explicitNames;
            names[0] = RRuntime.NAMES_ATTR_KEY;
            z = 1;
        }
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
                values[z] = keepCompactRowNames ? attr.getValue() : GetRowNamesAttributeNode.convertRowNamesToSeq(attr.getValue());
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
        return attributable.getAttributes() != null && attributable.getAttributes().getShape().getPropertyCount() != 0;
    }
}
