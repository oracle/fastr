/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

/**
 * Denotes an (unevaluated) R language element. It is equivalent to a LANGSXP value in GnuR. It
 * would be more correct to be named {@code RCall} since all LANGSXP values in (Gnu)R are in fact
 * function calls (although not currently in FastR). Therefore a {@code call} is represented by an
 * instance of this type, and an {@code expression} ({@link RExpression}) is a list of such
 * instances. R allows a language element to be treated as a list, hence the support for
 * {@link RAbstractContainer}, which is implemented via AST walk operations.
 *
 * {@link RLanguage} instances are almost completely immutable, <i>except</i> for the ability for
 * {@code}names} updates which manifests itself as actually transforming the AST. S we do have to
 * implement the {@link RShareable} interface.
 *
 *
 */
@ValueType
public class RLanguage extends RSharingAttributeStorage implements RAbstractContainer, RAttributable {

    /*
     * Used for RLanguage construction from separate AST components.
     */
    public enum RepType {
        CALL,
        FUNCTION,
        UNKNOWN
    }

    private RBaseNode rep;
    private RPairList list;
    private String callLHSName;

    /**
     * Lazily computed value.
     */
    private int length = -1;

    RLanguage(RBaseNode rep) {
        this.rep = rep;
    }

    private RLanguage(RBaseNode rep, int length) {
        this.rep = rep;
        this.length = length;
    }

    public static Object fromList(Object o, RLanguage.RepType type) {
        RList l;
        if (o instanceof RPairList) {
            l = ((RPairList) o).toRList();
        } else {
            l = (RList) o;
        }
        return RContext.getRRuntimeASTAccess().fromList(l, type);
    }

    public RBaseNode getRep() {
        if (list != null) {
            // we could rest rep but we keep it around to remember type of the language object
            assert rep != null;
            // list must be reset before rep type is obtained
            RPairList oldList = this.list;
            this.list = null;
            RLanguage newLang = (RLanguage) fromList(oldList, RContext.getRRuntimeASTAccess().getRepType(this));
            this.rep = newLang.rep;
            this.length = newLang.length;
            this.attributes = newLang.attributes;
        }
        return rep;
    }

    public void setRep(RBaseNode rep) {
        this.rep = rep;
        this.list = null;
    }

    public String getCallLHSName() {
        return callLHSName;
    }

    public void setCallLHSName(String callLHSName) {
        this.callLHSName = callLHSName;
    }

    @Override
    public RType getRType() {
        return RType.Language;
    }

    @Override
    public boolean isComplete() {
        return true;
    }

    @Override
    public int getLength() {
        // if list representation is present, it takes priority as it might have been updated
        if (list == null) {
            if (length < 0) {
                length = RContext.getRRuntimeASTAccess().getLength(this);
            }
            return length;
        } else {
            return list.getLength();
        }
    }

    @Override
    public RAbstractContainer resize(int size) {
        throw RInternalError.shouldNotReachHere();
    }

    @Override
    public boolean hasDimensions() {
        // TODO
        return false;
    }

    @Override
    public int[] getDimensions() {
        // TODO
        return null;
    }

    @Override
    public void setDimensions(int[] newDimensions) {
        throw RInternalError.unimplemented();
    }

    @Override
    public Class<?> getElementClass() {
        return RLanguage.class;
    }

    @Override
    public RShareable materializeToShareable() {
        // TODO is copy necessary?
        return copy();
    }

    @Override
    public Object getDataAtAsObject(int index) {
        if (list == null) {
            return RContext.getRRuntimeASTAccess().getDataAtAsObject(this, index);
        } else {
            return list.getDataAtAsObject(index);
        }
    }

    @Override
    public RStringVector getNames(RAttributeProfiles attrProfiles) {
        if (list == null) {
            /*
             * "names" for a language object is a special case, that is applicable to calls and
             * returns the names of the actual arguments, if any. E.g. f(x=1, 3) would return c("",
             * "x", ""). GnuR defines it as returning the "tag" values on the pairlist that
             * represents the call. Well, we don't have a pairlist, (we could get one by serializing
             * the expression), so we do it by AST walking.
             */
            RStringVector names = RContext.getRRuntimeASTAccess().getNames(this);
            return names;
        } else {
            return list.getNames(attrProfiles);
        }
    }

    @Override
    public void setNames(RStringVector newNames) {
        if (list == null) {
            /* See getNames */
            RContext.getRRuntimeASTAccess().setNames(this, newNames);
        } else {
            list.setNames(newNames);
        }
    }

    @Override
    public RList getDimNames(RAttributeProfiles attrProfiles) {
        RAttributable attr = list == null ? this : list;
        return (RList) attr.getAttr(attrProfiles, RRuntime.DIMNAMES_ATTR_KEY);
    }

    @Override
    public void setDimNames(RList newDimNames) {
        RAttributable attr = list == null ? this : list;
        attr.setAttr(RRuntime.DIMNAMES_ATTR_KEY, newDimNames);
    }

    @Override
    public Object getRowNames(RAttributeProfiles attrProfiles) {
        RAttributable attr = list == null ? this : list;
        return attr.getAttr(attrProfiles, RRuntime.ROWNAMES_ATTR_KEY);
    }

    @Override
    public void setRowNames(RAbstractVector rowNames) {
        RAttributable attr = list == null ? this : list;
        attr.setAttr(RRuntime.ROWNAMES_ATTR_KEY, rowNames);
    }

    @Override
    public RStringVector getImplicitClass() {
        return RDataFactory.createStringVector(RRuntime.CLASS_LANGUAGE);
    }

    @Override
    public RLanguage copy() {
        RLanguage l = new RLanguage(getRep(), this.length);
        if (this.attributes != null) {
            l.attributes = attributes.copy();
        }
        l.setTypedValueInfo(getTypedValueInfo());
        return l;
    }

    @Override
    public String toString() {
        return String.format("RLanguage(rep=%s)", getRep());
    }

    public RPairList getPairList() {
        if (list == null) {
            Object obj = RNull.instance;
            for (int i = getLength() - 1; i >= 0; i--) {
                Object element = RContext.getRRuntimeASTAccess().getDataAtAsObject(this, i);
                obj = RDataFactory.createPairList(element, obj);
            }
            // names have to be taken before list is assigned
            RStringVector names = RContext.getRRuntimeASTAccess().getNames(this);
            list = (RPairList) obj;
            if (names != null) {
                list.setNames(names);
            }
        }
        return list;
    }
}
