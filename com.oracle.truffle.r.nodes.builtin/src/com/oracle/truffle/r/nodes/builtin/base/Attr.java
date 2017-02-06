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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.singleElement;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.attributes.GetAttributeNode;
import com.oracle.truffle.r.nodes.attributes.IterableAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetRowNamesAttributeNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.base.UpdateAttr.InternStringNode;
import com.oracle.truffle.r.nodes.builtin.base.UpdateAttrNodeGen.InternStringNodeGen;
import com.oracle.truffle.r.nodes.function.opt.UpdateShareableChildValueNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RAttributesLayout;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RInteger;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

@RBuiltin(name = "attr", kind = PRIMITIVE, parameterNames = {"x", "which", "exact"}, behavior = PURE)
public abstract class Attr extends RBuiltinNode {

    private final ConditionProfile searchPartialProfile = ConditionProfile.createBinaryProfile();
    private final BranchProfile errorProfile = BranchProfile.create();

    @Child private UpdateShareableChildValueNode sharedAttrUpdate = UpdateShareableChildValueNode.create();
    @Child private InternStringNode intern = InternStringNodeGen.create();

    @Child private GetAttributeNode attrAccess = GetAttributeNode.create();
    @Child private IterableAttributeNode iterAttrAccess = IterableAttributeNode.create();

    @Override
    public Object[] getDefaultParameterValues() {
        return new Object[]{RMissing.instance, RMissing.instance, RRuntime.asLogical(false)};
    }

    static {
        Casts casts = new Casts(Attr.class);
        // Note: checking RAttributable.class does not work for scalars
        // casts.arg("x").mustBe(RAttributable.class, Message.UNIMPLEMENTED_ARGUMENT_TYPE);
        casts.arg("which").mustBe(stringValue(), Message.MUST_BE_CHARACTER, "which").asStringVector().mustBe(singleElement(), RError.Message.EXACTLY_ONE_WHICH).findFirst();
        casts.arg("exact").asLogicalVector().findFirst().map(toBoolean());
    }

    private Object searchKeyPartial(DynamicObject attributes, String name) {
        Object val = RNull.instance;

        for (RAttributesLayout.RAttribute e : iterAttrAccess.execute(attributes)) {
            if (e.getName().startsWith(name)) {
                if (val == RNull.instance) {
                    val = e.getValue();
                } else {
                    // non-unique match
                    return RNull.instance;
                }
            }
        }
        return val;
    }

    private Object attrRA(RAttributable attributable, String name, boolean exact) {
        DynamicObject attributes = attributable.getAttributes();
        if (attributes == null) {
            return RNull.instance;
        } else {
            Object result = attrAccess.execute(attributes, name);
            if (searchPartialProfile.profile(!exact && result == null)) {
                return searchKeyPartial(attributes, name);
            }
            return result == null ? RNull.instance : sharedAttrUpdate.updateState(attributable, result);
        }
    }

    @Specialization
    protected RNull attr(RNull container, @SuppressWarnings("unused") String name, @SuppressWarnings("unused") boolean exact) {
        return container;
    }

    @Specialization(guards = "!isRowNamesAttr(name)")
    protected Object attr(RAbstractContainer container, String name, boolean exact) {
        return attrRA(container, intern.execute(name), exact);
    }

    @Specialization(guards = "isRowNamesAttr(name)")
    protected Object attrRowNames(RAbstractContainer container, @SuppressWarnings("unused") String name, @SuppressWarnings("unused") boolean exact,
                    @Cached("create()") GetRowNamesAttributeNode getRowNamesNode) {
        // TODO: if exact == false, check for partial match (there is an ignored tests for it)
        DynamicObject attributes = container.getAttributes();
        if (attributes == null) {
            return RNull.instance;
        } else {
            return getFullRowNames(getRowNamesNode.getRowNames(container));
        }
    }

    /**
     * All other, non-performance centric, {@link RAttributable} types.
     */
    @Fallback
    @TruffleBoundary
    protected Object attr(Object object, Object name, Object exact) {
        if (object instanceof RAttributable) {
            return attrRA((RAttributable) object, intern.execute((String) name), (boolean) exact);
        } else {
            errorProfile.enter();
            throw RError.nyi(this, "object cannot be attributed");
        }
    }

    public static Object getFullRowNames(Object a) {
        if (a == RNull.instance) {
            return RNull.instance;
        } else {
            RAbstractVector rowNames = (RAbstractVector) a;
            return rowNames.getElementClass() == RInteger.class && rowNames.getLength() == 2 && RRuntime.isNA(((RAbstractIntVector) rowNames).getDataAt(0)) ? RDataFactory.createIntSequence(1, 1,
                            Math.abs(((RAbstractIntVector) rowNames).getDataAt(1))) : a;
        }
    }

    protected static boolean isRowNamesAttr(String name) {
        return name.equals(RRuntime.ROWNAMES_ATTR_KEY);
    }
}
