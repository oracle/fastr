/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.util.HashMap;

import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.data.RObjectSize.IgnoreObjectHandler;
import com.oracle.truffle.r.runtime.data.RObjectSize.TypeCustomizer;
import com.oracle.truffle.r.runtime.data.model.RAbstractAtomicVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractRawVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;

/**
 * Very simple object size calculation that does not need an instrumentation agent.
 */
public class SimpleObjectSizeFactory extends ObjectSizeFactory {
    private HashMap<Class<?>, TypeCustomizer> typeCustomizers;

    @Override
    public long getObjectSize(Object obj, IgnoreObjectHandler ignoreObjectHandler) {
        RError.warning(RError.NO_CALLER, Message.OBJECT_SIZE_ESTIMATE);
        // Customizers
        for (Class<?> klass : typeCustomizers.keySet()) {
            if (obj.getClass().equals(klass)) {
                return typeCustomizers.get(klass).getObjectSize(obj);
            }
        }
        // Well known vector types
        if (obj instanceof RAbstractDoubleVector) {
            return Double.BYTES * ((RAbstractDoubleVector) obj).getLength();
        } else if (obj instanceof RAbstractIntVector) {
            return Integer.BYTES * ((RAbstractIntVector) obj).getLength();
        } else if (obj instanceof RAbstractStringVector) {
            int length = 0;
            RAbstractStringVector strVec = (RAbstractStringVector) obj;
            for (int i = 0; i < strVec.getLength(); i++) {
                length += strVec.getDataAt(i).length();
            }
            return length * 2;
        } else if (obj instanceof RAbstractLogicalVector || obj instanceof RAbstractRawVector) {
            return Byte.BYTES * ((RAbstractAtomicVector) obj).getLength();
        } else if (obj instanceof RAbstractListVector) {
            int total = 0;
            RAbstractListVector list = (RAbstractListVector) obj;
            for (int i = 0; i < list.getLength(); i++) {
                // Note: RLists should not be cyclic
                total += getObjectSize(list.getDataAt(i), ignoreObjectHandler);
            }
            return total;
        }
        return 4;
    }

    @Override
    public void registerTypeCustomizer(Class<?> klass, TypeCustomizer typeCustomizer) {
        if (typeCustomizers == null) {
            typeCustomizers = new HashMap<>();
        }
        typeCustomizers.put(klass, typeCustomizer);
    }
}
