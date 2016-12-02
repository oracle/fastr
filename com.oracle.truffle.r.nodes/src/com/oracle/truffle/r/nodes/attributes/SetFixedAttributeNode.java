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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.FinalLocationException;
import com.oracle.truffle.api.object.IncompatibleLocationException;
import com.oracle.truffle.api.object.Location;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.SetSpecialAttributeNode;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RAttributable;

public abstract class SetFixedAttributeNode extends FixedAttributeAccessNode {

    @Child private SetFixedAttributeNode recursive;
    @Child private SetSpecialAttributeNode setSpecialAttrNode;

    private final boolean isSpecialAttribute;

    protected SetFixedAttributeNode(String name) {
        super(name);
        this.isSpecialAttribute = SpecialAttributesFunctions.IsSpecialAttributeNode.isSpecialAttribute(name);
    }

    public static SetFixedAttributeNode create(String name) {
        return SetFixedAttributeNodeGen.create(name);
    }

    public static SetFixedAttributeNode createNames() {
        return SetFixedAttributeNode.create(RRuntime.NAMES_ATTR_KEY);
    }

    public static SetFixedAttributeNode createDim() {
        return SetFixedAttributeNode.create(RRuntime.DIM_ATTR_KEY);
    }

    public static SetFixedAttributeNode createDimNames() {
        return SetFixedAttributeNode.create(RRuntime.DIMNAMES_ATTR_KEY);
    }

    public static SetFixedAttributeNode createClass() {
        return SetFixedAttributeNode.create(RRuntime.CLASS_ATTR_KEY);
    }

    public abstract void execute(Object attr, Object value);

    @Specialization(limit = "3", //
                    guards = {"shapeCheck(shape, attrs)", "location != null"}, //
                    assumptions = {"shape.getValidAssumption()"})
    protected void setAttrCached(DynamicObject attrs, Object value,
                    @Cached("lookupShape(attrs)") Shape shape,
                    @Cached("lookupLocation(shape, name)") Location location) {
        try {
            location.set(attrs, value, shape);
        } catch (IncompatibleLocationException | FinalLocationException ex) {
            RInternalError.reportError(ex);
        }
    }

    @Specialization(contains = "setAttrCached")
    @TruffleBoundary
    protected void setFallback(DynamicObject attrs, Object value) {
        attrs.define(name, value);
    }

    @Specialization
    protected void setAttrInAttributable(RAttributable x, Object value,
                    @Cached("create()") BranchProfile attrNullProfile) {
        if (isSpecialAttribute) {
            if (setSpecialAttrNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                setSpecialAttrNode = insert(SpecialAttributesFunctions.createSpecialAttributeNode(name));
            }
            setSpecialAttrNode.execute(x, value);
        } else {
            DynamicObject attributes = x.getAttributes();
            if (attributes == null) {
                attrNullProfile.enter();
                attributes = x.initAttributes();
            }

            if (recursive == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                recursive = insert(create(name));
            }
            recursive.execute(attributes, value);
        }

    }

}
