/*
 * Copyright (c) 2014, 2014, Oracle and/or its affiliates. All rights reserved.
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

    private RAttributes attributes;
    /**
     * Lazily computed value.
     */
    private int length = -1;

    public RLanguage(Object rep) {
        super(rep);
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

    public Class<?> getElementClass() {
        return RLanguage.class;
    }

    public RVector materializeNonSharedVector() {
        assert false;
        return null;
    }

    public RShareable materializeToShareable() {
        assert false;
        return null;
    }

    public Object getDataAtAsObject(int index) {
        return RContext.getRASTHelper().getDataAtAsObject(this, index);
    }

    public Object getNames() {
        // TODO
        return null;
    }

    public RList getDimNames() {
        // TODO
        return null;
    }

    public Object getRowNames() {
        // TODO
        return null;
    }

    public RStringVector getClassHierarchy() {
        return null;
    }

    public boolean isObject() {
        return false;
    }

    public RList asList() {
        return RContext.getRASTHelper().asList(this);
    }

}
