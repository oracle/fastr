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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.gnur.*;

/**
 * Denotes the (rarely seen) {@code pairlist} type in R.
 */
public class RPairList extends RAttributeStorage implements RAttributable, RAbstractContainer {
    private final Object car;
    private Object cdr;
    private Object tag;

    /**
     * Denotes the (GnuR) typeof entity that the pairlist represents.
     */
    private final SEXPTYPE type;

    public RPairList(Object car, Object cdr, String tag) {
        this(car, cdr, tag, null);
    }

    /**
     * Variant used in unserialization to record the GnuR type the pairlist denotes.
     */
    public RPairList(Object car, Object cdr, Object tag, SEXPTYPE type) {
        this.car = car;
        this.cdr = cdr;
        this.tag = tag;
        this.type = type;
    }

    @Override
    @TruffleBoundary
    public String toString() {
        return String.format("type=%s, tag=%s, car=%s, cdr=%s", type, tag, toStringHelper(car), toStringHelper(cdr));
    }

    private static String toStringHelper(Object obj) {
        return obj == null ? "null" : obj.getClass().getSimpleName();
    }

    public Object car() {
        return car;
    }

    public Object cdr() {
        return cdr;
    }

    public void setCdr(Object newCdr) {
        assert cdr == null;
        cdr = newCdr;
    }

    public Object cadr() {
        RPairList cdrpl = (RPairList) cdr;
        return cdrpl.car;
    }

    public Object cddr() {
        RPairList cdrpl = (RPairList) cdr;
        return cdrpl.cdr;
    }

    public Object caddr() {
        RPairList pl = (RPairList) cddr();
        return pl.car;
    }

    public Object getTag() {
        return tag;
    }

    public void setTag(Object tag) {
        this.tag = tag;
    }

    public SEXPTYPE getType() {
        return type;
    }

    public int getLength() {
        int result = 1;
        Object tcdr = cdr;
        while (tcdr != null && tcdr != RNull.instance) {
            if (tcdr instanceof RPairList) {
                tcdr = ((RPairList) tcdr).cdr;
            }
            result++;
        }
        return result;
    }

    public boolean hasDimensions() {
        return true;
    }

    public int[] getDimensions() {
        return new int[]{1};
    }

    public Class<?> getElementClass() {
        return null;
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
        RPairList pl = this;
        int i = 0;
        while (pl != null && i < index) {
            pl = (RPairList) pl.cdr;
            i++;
        }
        return pl.car;
    }

    public Object getNames() {
        int l = getLength();
        String[] data = new String[l];
        RPairList pl = this;
        boolean complete = RDataFactory.COMPLETE_VECTOR;
        int i = 0;
        while (pl != null) {
            data[i] = (String) pl.tag;
            if (pl.tag == RRuntime.STRING_NA) {
                complete = false;
            }
            pl = (RPairList) pl.cdr;
            i++;
        }
        return RDataFactory.createStringVector(data, complete);
    }

    public RList getDimNames() {
        return null;
    }

    public Object getRowNames() {
        return null;
    }

    public RStringVector getClassHierarchy() {
        return null;
    }

    public boolean isObject() {
        return false;
    }
}
