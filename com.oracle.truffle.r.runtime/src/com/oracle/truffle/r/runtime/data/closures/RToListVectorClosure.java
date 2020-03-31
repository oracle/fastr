/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.runtime.data.closures;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RRaw;
import static com.oracle.truffle.r.runtime.data.closures.RClosures.initRegAttributes;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractRawVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;

abstract class RToListVectorClosure extends RAbstractListVector {

    protected final boolean keepAttributes;
    protected final RAbstractVector vector;

    protected RToListVectorClosure(RAbstractVector vector, boolean keepAttributes) {
        super(vector.isComplete());
        this.keepAttributes = keepAttributes;
        this.vector = vector;

        if (isMaterialized()) {
            if (keepAttributes) {
                initAttributes(vector.getAttributes());
            } else {
                initRegAttributes(this, vector);
            }
        }
    }

    @Override
    public boolean isMaterialized() {
        return vector.isMaterialized();
    }

    @Override
    public Object getDataAtAsObject(int index) {
        return getDataAt(index);
    }

    @Override
    public Object getInternalStore() {
        return vector.getInternalStore();
    }

    @Override
    public int getLength() {
        return vector.getLength();
    }

    @Override
    public void setLength(int l) {
        vector.setLength(l);
    }

    @Override
    public int getTrueLength() {
        return vector.getTrueLength();
    }

    @Override
    public void setTrueLength(int l) {
        vector.setTrueLength(l);
    }

    @TruffleBoundary
    protected void copyAttributes(RList materialized) {
        if (keepAttributes) {
            materialized.copyAttributesFrom(this);
        }
    }

    @Override
    public VectorAccess access() {
        throw RInternalError.shouldNotReachHere("access() for " + getClass().getSimpleName());
    }

    @Override
    public VectorAccess slowPathAccess() {
        throw RInternalError.shouldNotReachHere("slowPathAccess() for " + getClass().getSimpleName());
    }
}

final class RLogicalToListVectorClosure extends RToListVectorClosure {
    RLogicalToListVectorClosure(RAbstractLogicalVector vector, boolean keepAttributes) {
        super(vector, keepAttributes);
    }

    @Override
    public Byte getDataAt(int index) {
        return ((RAbstractLogicalVector) vector).getDataAt(index);
    }
}

final class RIntToListVectorClosure extends RToListVectorClosure {
    RIntToListVectorClosure(RIntVector vector, boolean keepAttributes) {
        super(vector, keepAttributes);
    }

    @Override
    public Integer getDataAt(int index) {
        return ((RIntVector) vector).getDataAt(index);
    }
}

final class RDoubleToListVectorClosure extends RToListVectorClosure {

    RDoubleToListVectorClosure(RDoubleVector vector, boolean keepAttributes) {
        super(vector, keepAttributes);
    }

    @Override
    public Double getDataAt(int index) {
        return ((RDoubleVector) vector).getDataAt(index);
    }
}

final class RComplexToListVectorClosure extends RToListVectorClosure {
    RComplexToListVectorClosure(RAbstractComplexVector vector, boolean keepAttributes) {
        super(vector, keepAttributes);
    }

    @Override
    public RComplex getDataAt(int index) {
        return ((RAbstractComplexVector) vector).getDataAt(index);
    }
}

final class RStringToListVectorClosure extends RToListVectorClosure {
    RStringToListVectorClosure(RAbstractStringVector vector, boolean keepAttributes) {
        super(vector, keepAttributes);
    }

    @Override
    public String getDataAt(int index) {
        return ((RAbstractStringVector) vector).getDataAt(index);
    }
}

final class RRawToListVectorClosure extends RToListVectorClosure {
    RRawToListVectorClosure(RAbstractRawVector vector, boolean keepAttributes) {
        super(vector, keepAttributes);
    }

    @Override
    public Object getDataAt(int index) {
        return RRaw.valueOf(((RAbstractRawVector) vector).getRawDataAt(index));
    }
}
