/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.data;

import com.oracle.truffle.api.CompilerDirectives.ValueType;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.model.*;

/**
 * Denotes an (unevaluated) R language element. It is equivalent to a LANGSXP value in GnuR. It
 * would be more correct to be named {@code RCall} since all LANGSXP values in (Gnu)R are in fact
 * function calls (although not currently in FastR). Therefore a {@code call} is represented by an
 * instance of this type, and an {@code expression} ({@link RExpression}) is a list of such
 * instances. R allows a language element to be treated as a list, hence the support for
 * {@link RAbstractContainer}, which is implemented via AST walk operations.
 *
 * The representation is inherited from {@link RLanguageRep}. This is a Truffle AST ({@code RNode}),
 * although that type is not statically used here due to project circularities. A related
 * consequence is the the implementation of the {@link RAbstractContainer} methods are delegated to
 * a helper class from a project that can access {@code RNode}.
 *
 *
 */
@ValueType
public class RLanguage extends RLanguageRep implements RAbstractContainer, RAttributable {

    private final RAttributeProfiles localAttrProfiles = RAttributeProfiles.create();

    private RAttributes attributes;
    /**
     * Lazily computed value.
     */
    private int length = -1;

    RLanguage(Object rep) {
        super(rep);
    }

    public RType getRType() {
        return RType.Language;
    }

    public RAttributes getAttributes() {
        return attributes;
    }

    public RAttributes initAttributes() {
        if (attributes == null) {
            attributes = RAttributes.create();
        }
        return attributes;
    }

    public boolean isComplete() {
        throw RInternalError.shouldNotReachHere();
    }

    public int getLength() {
        if (length < 0) {
            length = RContext.getRASTHelper().getLength(this);
        }
        return length;
    }

    public boolean hasDimensions() {
        // TODO
        return false;
    }

    public int[] getDimensions() {
        // TODO
        return null;
    }

    @Override
    public void setDimensions(int[] newDimensions) {
        throw RInternalError.unimplemented();
    }

    public Class<?> getElementClass() {
        return RLanguage.class;
    }

    public RVector materializeNonSharedVector() {
        throw RInternalError.shouldNotReachHere();
    }

    public RShareable materializeToShareable() {
        throw RInternalError.shouldNotReachHere();
    }

    public Object getDataAtAsObject(int index) {
        return RContext.getRASTHelper().getDataAtAsObject(this, index);
    }

    @Override
    public RStringVector getNames(RAttributeProfiles attrProfiles) {
        return (RStringVector) getAttr(attrProfiles, RRuntime.NAMES_ATTR_KEY);
    }

    @Override
    public void setNames(RStringVector newNames) {
        setAttr(RRuntime.NAMES_ATTR_KEY, newNames);
    }

    @Override
    public RList getDimNames() {
        return (RList) getAttr(localAttrProfiles, RRuntime.DIMNAMES_ATTR_KEY);
    }

    @Override
    public void setDimNames(RList newDimNames) {
        setAttr(RRuntime.DIMNAMES_ATTR_KEY, newDimNames);
    }

    @Override
    public Object getRowNames(RAttributeProfiles attrProfiles) {
        return getAttr(attrProfiles, RRuntime.ROWNAMES_ATTR_KEY);
    }

    @Override
    public void setRowNames(RAbstractVector rowNames) {
        setAttr(RRuntime.ROWNAMES_ATTR_KEY, rowNames);
    }

    public RStringVector getClassHierarchy() {
        return RDataFactory.createStringVector(RRuntime.CLASS_LANGUAGE);
    }

    @Override
    public boolean isObject(RAttributeProfiles attrProfiles) {
        return false;
    }

    public RLanguage copy() {
        RLanguage l = new RLanguage(getRep());
        l.attributes = attributes;
        return l;
    }

}
