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
import com.oracle.truffle.r.runtime.context.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.nodes.*;

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
 * {@link RLanguage} instances are almost completely immutable, <i>except</i> for the ability for
 * {@code}names} updates which manifests itself as actually transforming the AST. S we do have to
 * implement the {@link RShareable} interface.
 *
 *
 */
@ValueType
public class RLanguage extends RLanguageRep implements RAbstractContainer, RAttributable, RShareable {

    private RAttributes attributes;

    private int gpbits;

    /**
     * Lazily computed value.
     */
    private int length = -1;

    RLanguage(RNode rep) {
        super(rep);
    }

    RLanguage(RNode rep, int length) {
        super(rep);
        this.length = length;
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

    public final void initAttributes(RAttributes newAttributes) {
        attributes = newAttributes;
    }

    public boolean isComplete() {
        return true;
    }

    @Override
    public int getLength() {
        if (length < 0) {
            length = RContext.getRRuntimeASTAccess().getLength(this);
        }
        return length;
    }

    @Override
    public RAbstractContainer resize(int size) {
        throw RInternalError.shouldNotReachHere();
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

    public RLanguage materializeNonShared() {
        if (this.isShared()) {
            RLanguage res = this.copy();
            res.markNonTemporary();
            return res;
        }
        if (this.isTemporary()) {
            this.markNonTemporary();
        }
        return this;
    }

    public RShareable materializeToShareable() {
        // TODO is copy necessary?
        return copy();
    }

    public Object getDataAtAsObject(int index) {
        return RContext.getRRuntimeASTAccess().getDataAtAsObject(this, index);
    }

    @Override
    public RStringVector getNames(RAttributeProfiles attrProfiles) {
        /*
         * "names" for a language object is a special case, that is applicable to calls and returns
         * the names of the actual arguments, if any. E.g. f(x=1, 3) would return c("", "x", "").
         * GnuR defines it as returning the "tag" values on the pairlist that represents the call.
         * Well, we don't have a pairlist, (we could get one by serializing the expression), so we
         * do it by AST walking.
         */
        RStringVector names = RContext.getRRuntimeASTAccess().getNames(this);
        return names;
    }

    @Override
    public void setNames(RStringVector newNames) {
        /* See getNames */
        RContext.getRRuntimeASTAccess().setNames(this, newNames);
    }

    @Override
    public RList getDimNames(RAttributeProfiles attrProfiles) {
        return (RList) getAttr(attrProfiles, RRuntime.DIMNAMES_ATTR_KEY);
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

    public RStringVector getImplicitClass() {
        return RDataFactory.createStringVector(RRuntime.CLASS_LANGUAGE);
    }

    @Override
    public boolean isObject(RAttributeProfiles attrProfiles) {
        return false;
    }

    @Override
    public RLanguage copy() {
        RLanguage l = new RLanguage((RNode) getRep(), this.length);
        if (this.attributes != null) {
            l.attributes = attributes.copy();
        }
        l.gpbits = gpbits;
        return l;
    }

    /*
     * RShareable support. Code is cloned directly from RVector.
     */
    private boolean shared;
    private boolean temporary = true;
    private int refCount;

    public void markNonTemporary() {
        assert !FastROptions.NewStateTransition.getBooleanValue();
        temporary = false;
    }

    public boolean isTemporary() {
        if (FastROptions.NewStateTransition.getBooleanValue()) {
            return refCount == 0;
        } else {
            return temporary;
        }
    }

    public boolean isShared() {
        if (FastROptions.NewStateTransition.getBooleanValue()) {
            return refCount > 1;
        } else {
            return shared;
        }
    }

    public RShareable makeShared() {
        assert !FastROptions.NewStateTransition.getBooleanValue();
        if (temporary) {
            temporary = false;
        }
        shared = true;
        return this;
    }

    public void incRefCount() {
        refCount++;
    }

    public void decRefCount() {
        assert refCount > 0;
        refCount--;
    }

    @Override
    public boolean isSharedPermanent() {
        return refCount == SHARED_PERMANENT_VAL;
    }

    @Override
    public void makeSharedPermanent() {
        if (FastROptions.NewStateTransition.getBooleanValue()) {
            refCount = SHARED_PERMANENT_VAL;
        } else {
            // old scheme never reverts states
            makeShared();
        }
    }

    @Override
    public String toString() {
        return String.format("RLanguage(rep=%s)", getRep());
    }

    public int getGPBits() {
        return gpbits;
    }

    public void setGPBits(int value) {
        gpbits = value;
    }

}
