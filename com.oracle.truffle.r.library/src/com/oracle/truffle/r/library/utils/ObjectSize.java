/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.library.utils;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.library.utils.ObjectSizeNodeGen.RecursiveObjectSizeNodeGen;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.nodes.profile.TruffleBoundaryNode;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RAttributes;
import com.oracle.truffle.r.runtime.data.RAttributes.RAttribute;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RComplexVector;
import com.oracle.truffle.r.runtime.data.RDoubleSequence;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RExpression;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RIntSequence;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RLanguage;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RRaw;
import com.oracle.truffle.r.runtime.data.RRawVector;
import com.oracle.truffle.r.runtime.data.RStringVector;

/*
 * Similarly to GNU R's version, this is very approximate
 * (e.g. overhead related to Java object headers is not included)
 * and is only (semi) accurate for atomic vectors.
 */
public abstract class ObjectSize extends RExternalBuiltinNode.Arg1 {

    protected abstract int executeInt(Object o);

    @Child RecursiveObjectSize recursiveObjectSize;

    protected int recursiveObjectSize(Object o) {
        if (recursiveObjectSize == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            recursiveObjectSize = insert(RecursiveObjectSizeNodeGen.create());
        }
        return recursiveObjectSize.executeInt(o);
    }

    @Specialization
    protected int objectSize(@SuppressWarnings("unused") RNull o) {
        return 64; // pointer?
    }

    @Specialization
    protected int objectSize(@SuppressWarnings("unused") int o) {
        return 32;
    }

    @Specialization
    protected int objectSize(@SuppressWarnings("unused") double o) {
        return 64;
    }

    @Specialization
    protected int objectSize(@SuppressWarnings("unused") byte o) {
        return 8;
    }

    @Specialization
    protected int objectSize(String o) {
        return o.length() * 16;
    }

    @Specialization
    protected int objectSize(@SuppressWarnings("unused") RRaw o) {
        return 8;
    }

    @Specialization
    protected int objectSize(@SuppressWarnings("unused") RComplex o) {
        return 128;
    }

    @Specialization
    protected int objectSize(RIntSequence o) {
        int res = 96; // int length + int start + int stride
        return res + attrSize(o);
    }

    @Specialization
    protected int objectSize(RDoubleSequence o) {
        int res = 160; // int length + double start + double stride
        return res + attrSize(o);
    }

    @Specialization
    protected int objectSize(RIntVector o) {
        return o.getLength() * 32 + attrSize(o);
    }

    @Specialization
    protected int objectSize(RDoubleVector o) {
        return o.getLength() * 64 + attrSize(o);
    }

    @Specialization
    protected int objectSize(RStringVector o) {
        int res = 0;
        for (int i = 0; i < o.getLength(); i++) {
            res += o.getLength() * 16;
        }
        return res + attrSize(o);
    }

    @Specialization
    protected int objectSize(RLogicalVector o) {
        return o.getLength() * 8 + attrSize(o);
    }

    @Specialization
    protected int objectSize(RComplexVector o) {
        return o.getLength() * 128 + attrSize(o);
    }

    @Specialization
    protected int objectSize(RRawVector o) {
        return o.getLength() * 8 + attrSize(o);
    }

    @Specialization
    @TruffleBoundary
    protected int objectSize(RList o) {
        int res = 0;
        for (int i = 0; i < o.getLength(); i++) {
            res += recursiveObjectSize(o.getDataAt(i));
        }
        return res + attrSize(o);
    }

    @Specialization
    @TruffleBoundary
    protected int objectSize(RPairList o) {
        RPairList list = o;
        Object car = list.car();
        int res = 0;
        while (true) {
            res += recursiveObjectSize(car);
            Object cdr = list.cdr();
            if (cdr == RNull.instance) {
                break;
            } else {
                list = (RPairList) cdr;
                car = list.car();
            }
        }
        return res + attrSize(o);
    }

    @Specialization
    protected int objectSize(@SuppressWarnings("unused") RFunction o) {
        return 256; // arbitrary, but does it really matter?
    }

    @Specialization
    protected int objectSize(@SuppressWarnings("unused") RLanguage o) {
        return 256; // arbitrary, but does it really matter?
    }

    @Specialization
    protected int objectSize(@SuppressWarnings("unused") RExpression o) {
        return 256; // arbitrary, but does it really matter?
    }

    protected int attrSize(RAttributable o) {
        return o.getAttributes() == null ? 0 : attrSizeInternal(o.getAttributes());
    }

    @TruffleBoundary
    protected int attrSizeInternal(RAttributes attributes) {
        int size = 0;
        for (RAttribute attr : attributes) {
            size += attr.getName().length() * 16;
            size += recursiveObjectSize(attr.getValue());
        }
        return size;
    }

    protected abstract static class RecursiveObjectSize extends TruffleBoundaryNode {

        protected abstract int executeInt(Object o);

        @Child ObjectSize objectSize = ObjectSizeNodeGen.create();

        @Specialization
        protected int objectSize(Object o) {
            return objectSize.executeInt(o);
        }
    }
}
