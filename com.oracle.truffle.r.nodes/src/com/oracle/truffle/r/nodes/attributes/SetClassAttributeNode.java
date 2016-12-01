/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RInteger;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RVector;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

public abstract class SetClassAttributeNode extends RBaseNode {

    public static SetClassAttributeNode create() {
        return SetClassAttributeNodeGen.create();
    }

    public abstract void execute(RAttributable x, Object classAttr);

    public void reset(RAttributable x) {
        execute(x, RNull.instance);
    }

    @Specialization
    protected <T> void handleVectorNullClass(RVector<T> vector, @SuppressWarnings("unused") RNull classAttr,
                    @Cached("createClass()") RemoveFixedAttributeNode removeClassAttrNode,
                    @Cached("createClass()") SetFixedAttributeNode setClassAttrNode,
                    @Cached("create()") BranchProfile nullAttrProfile,
                    @Cached("createBinaryProfile()") ConditionProfile nullClassProfile,
                    @Cached("createBinaryProfile()") ConditionProfile notNullClassProfile) {
        handleVector(vector, null, removeClassAttrNode, setClassAttrNode, nullAttrProfile, nullClassProfile, notNullClassProfile);
    }

    @Specialization
    protected <T> void handleVector(RVector<T> vector, RStringVector classAttr,
                    @Cached("createClass()") RemoveFixedAttributeNode removeClassAttrNode,
                    @Cached("createClass()") SetFixedAttributeNode setClassAttrNode,
                    @Cached("create()") BranchProfile nullAttrProfile,
                    @Cached("createBinaryProfile()") ConditionProfile nullClassProfile,
                    @Cached("createBinaryProfile()") ConditionProfile notNullClassProfile) {

        DynamicObject attrs = vector.getAttributes();
        if (attrs == null && classAttr != null && classAttr.getLength() != 0) {
            nullAttrProfile.enter();
            attrs = vector.initAttributes();
        }
        if (nullClassProfile.profile(attrs != null && (classAttr == null || classAttr.getLength() == 0))) {
            removeAttributeMapping(vector, attrs, removeClassAttrNode);
        } else if (notNullClassProfile.profile(classAttr != null && classAttr.getLength() != 0)) {
            for (int i = 0; i < classAttr.getLength(); i++) {
                String attr = classAttr.getDataAt(i);
                if (RRuntime.CLASS_FACTOR.equals(attr)) {
                    // TODO: Isn't this redundant when the same operation is done after the loop?
                    setClassAttrNode.execute(attrs, classAttr);
                    if (vector.getElementClass() != RInteger.class) {
                        // N.B. there used to be conversion to integer under certain circumstances.
                        // However, it seems that it was dead/obsolete code so it was removed.
                        // Notes: this can only happen if the class is set by hand to some
                        // non-integral vector, i.e. attr(doubles, 'class') <- 'factor'. GnuR also
                        // does not update the 'class' attr with other, possibly
                        // valid classes when it reaches this error.
                        throw RError.error(RError.SHOW_CALLER2, RError.Message.ADDING_INVALID_CLASS, "factor");
                    }
                }
            }
            setClassAttrNode.execute(attrs, classAttr);
        }
    }

    @Specialization
    protected void handleAttributable(RAttributable x, @SuppressWarnings("unused") RNull classAttr) {
        x.setClassAttr(null);
    }

    @Specialization
    protected void handleAttributable(RAttributable x, RStringVector classAttr) {
        x.setClassAttr(classAttr);
    }

    private static void removeAttributeMapping(RAttributable x, DynamicObject attrs, RemoveFixedAttributeNode removeClassAttrNode) {
        if (attrs != null) {
            removeClassAttrNode.execute(attrs);
            if (attrs.isEmpty()) {
                x.initAttributes(null);
            }
        }
    }

}
