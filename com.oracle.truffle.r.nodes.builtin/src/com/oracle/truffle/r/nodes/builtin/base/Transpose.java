/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995, 1996, 1997  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1995-2014, The R Core Team
 * Copyright (c) 2002-2008, The R Foundation
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.RBuiltinKind.SUBSTITUTE;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.data.RAttributeProfiles;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.nodes.RNode;

@RBuiltin(name = "t.default", kind = SUBSTITUTE, parameterNames = {"x"})
// TODO INTERNAL
public abstract class Transpose extends RBuiltinNode {

    private final RAttributeProfiles attrProfiles = RAttributeProfiles.create();
    private final BranchProfile hasDimNamesProfile = BranchProfile.create();

    public abstract Object execute(Object o);

    @Specialization
    protected RNull transpose(RNull value) {
        controlVisibility();
        return value;
    }

    @Specialization
    protected int transpose(int value) {
        controlVisibility();
        return value;
    }

    @Specialization
    protected double transpose(double value) {
        controlVisibility();
        return value;
    }

    @Specialization
    protected byte transpose(byte value) {
        controlVisibility();
        return value;
    }

    @Specialization(guards = "isEmpty2D(vector)")
    @TruffleBoundary
    protected RAbstractVector transpose(RAbstractVector vector) {
        controlVisibility();
        int[] dim = vector.getDimensions();
        return vector.copyWithNewDimensions(new int[]{dim[1], dim[0]});
    }

    @Specialization(guards = "!isEmpty2D(vector)")
    @TruffleBoundary
    protected RIntVector transpose(RAbstractIntVector vector) {
        controlVisibility();
        return performAbstractIntVector(vector, vector.isMatrix() ? vector.getDimensions() : new int[]{vector.getLength(), 1});
    }

    private RIntVector performAbstractIntVector(RAbstractIntVector vector, int[] dim) {
        int firstDim = dim[0]; // rows
        int secondDim = dim[1];
        int[] result = new int[vector.getLength()];
        int j = 0;

        RNode.reportWork(this, vector.getLength());
        for (int i = 0; i < result.length; i++, j += firstDim) {
            if (j > (result.length - 1)) {
                j -= (result.length - 1);
            }
            result[i] = vector.getDataAt(j);
        }
        int[] newDim = new int[]{secondDim, firstDim};
        RIntVector r = RDataFactory.createIntVector(result, vector.isComplete());
        r.copyAttributesFrom(attrProfiles, vector);
        r.setDimensions(newDim);
        setDimNames(r, vector);
        return r;
    }

    @Specialization(guards = "!isEmpty2D(vector)")
    @TruffleBoundary
    protected RDoubleVector transpose(RAbstractDoubleVector vector) {
        controlVisibility();
        return performAbstractDoubleVector(vector, vector.isMatrix() ? vector.getDimensions() : new int[]{vector.getLength(), 1});
    }

    private RDoubleVector performAbstractDoubleVector(RAbstractDoubleVector vector, int[] dim) {
        int firstDim = dim[0];
        int secondDim = dim[1];
        double[] result = new double[vector.getLength()];
        int j = 0;

        RNode.reportWork(this, vector.getLength());
        for (int i = 0; i < result.length; i++, j += firstDim) {
            if (j > (result.length - 1)) {
                j -= (result.length - 1);
            }
            result[i] = vector.getDataAt(j);
        }
        int[] newDim = new int[]{secondDim, firstDim};
        RDoubleVector r = RDataFactory.createDoubleVector(result, vector.isComplete());
        r.copyAttributesFrom(attrProfiles, vector);
        r.setDimensions(newDim);
        setDimNames(r, vector);
        return r;
    }

    @Specialization(guards = "!isEmpty2D(vector)")
    @TruffleBoundary
    protected RStringVector transpose(RAbstractStringVector vector) {
        controlVisibility();
        return performAbstractStringVector(vector, vector.isMatrix() ? vector.getDimensions() : new int[]{vector.getLength(), 1});
    }

    private RStringVector performAbstractStringVector(RAbstractStringVector vector, int[] dim) {
        int firstDim = dim[0];
        int secondDim = dim[1];
        String[] result = new String[vector.getLength()];
        int j = 0;

        RNode.reportWork(this, vector.getLength());
        for (int i = 0; i < result.length; i++, j += firstDim) {
            if (j > (result.length - 1)) {
                j -= (result.length - 1);
            }
            result[i] = vector.getDataAt(j);
        }
        int[] newDim = new int[]{secondDim, firstDim};
        RStringVector r = RDataFactory.createStringVector(result, vector.isComplete());
        r.copyAttributesFrom(attrProfiles, vector);
        r.setDimensions(newDim);
        setDimNames(r, vector);
        return r;
    }

    private void setDimNames(RVector newVector, RAbstractVector oldVector) {
        RList dimNames = oldVector.getDimNames(attrProfiles);
        if (dimNames != null) {
            hasDimNamesProfile.enter();
            assert dimNames.getLength() == 2;
            newVector.setDimNames(RDataFactory.createList(new Object[]{dimNames.getDataAt(1), dimNames.getDataAt(0)}));
        }
    }

    protected static boolean isEmpty2D(RAbstractVector vector) {
        if (!vector.hasDimensions()) {
            return false;
        }
        return vector.getDimensions().length == 2 && vector.getLength() == 0;
    }
}
