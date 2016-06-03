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

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.attributes.CopyOfRegAttributesNode;
import com.oracle.truffle.r.nodes.attributes.CopyOfRegAttributesNodeGen;
import com.oracle.truffle.r.nodes.attributes.InitAttributesNode;
import com.oracle.truffle.r.nodes.attributes.PutAttributeNode;
import com.oracle.truffle.r.nodes.attributes.PutAttributeNodeGen;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.data.RAttributeProfiles;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
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
    private final ConditionProfile isMatrixProfile = ConditionProfile.createBinaryProfile();

    @Child private CopyOfRegAttributesNode copyRegAttributes = CopyOfRegAttributesNodeGen.create();
    @Child private InitAttributesNode initAttributes = InitAttributesNode.create();
    @Child private PutAttributeNode putDimensions = PutAttributeNodeGen.createDim();
    @Child private PutAttributeNode putDimNames = PutAttributeNodeGen.createDimNames();

    public abstract Object execute(Object o);

    @Specialization
    protected RNull transpose(RNull value) {
        return value;
    }

    @Specialization
    protected int transpose(int value) {
        return value;
    }

    @Specialization
    protected double transpose(double value) {
        return value;
    }

    @Specialization
    protected byte transpose(byte value) {
        return value;
    }

    @Specialization(guards = "isEmpty2D(vector)")
    protected RAbstractVector transpose(RAbstractVector vector) {
        int[] dim = vector.getDimensions();
        return vector.copyWithNewDimensions(new int[]{dim[1], dim[0]});
    }

    @FunctionalInterface
    private interface InnerLoop<T extends RAbstractVector> {
        RVector apply(T vector, int firstDim);
    }

    protected <T extends RAbstractVector> RVector transposeInternal(T vector, InnerLoop<T> innerLoop) {
        int firstDim;
        int secondDim;
        if (isMatrixProfile.profile(vector.isMatrix())) {
            firstDim = vector.getDimensions()[0];
            secondDim = vector.getDimensions()[1];
        } else {
            firstDim = vector.getLength();
            secondDim = 1;
        }
        RNode.reportWork(this, vector.getLength());

        RVector r = innerLoop.apply(vector, firstDim);
        // copy attributes
        copyRegAttributes.execute(vector, r);
        // set new dimensions
        int[] newDim = new int[]{secondDim, firstDim};
        r.setInternalDimensions(newDim);
        putDimensions.execute(initAttributes.execute(r), RDataFactory.createIntVector(newDim, RDataFactory.COMPLETE_VECTOR));
        // set new dim names
        RList dimNames = vector.getDimNames(attrProfiles);
        if (dimNames != null) {
            hasDimNamesProfile.enter();
            assert dimNames.getLength() == 2;
            RList newDimNames = RDataFactory.createList(new Object[]{dimNames.getDataAt(1), dimNames.getDataAt(0)});
            r.setInternalDimNames(newDimNames);
            putDimNames.execute(r.getAttributes(), newDimNames);
        }
        return r;
    }

    private static RVector innerLoopInt(RAbstractIntVector vector, int firstDim) {
        int[] result = new int[vector.getLength()];
        int j = 0;
        for (int i = 0; i < result.length; i++, j += firstDim) {
            if (j > (result.length - 1)) {
                j -= (result.length - 1);
            }
            result[i] = vector.getDataAt(j);
        }
        return RDataFactory.createIntVector(result, vector.isComplete());
    }

    private static RVector innerLoopDouble(RAbstractDoubleVector vector, int firstDim) {
        double[] result = new double[vector.getLength()];
        int j = 0;
        for (int i = 0; i < result.length; i++, j += firstDim) {
            if (j > (result.length - 1)) {
                j -= (result.length - 1);
            }
            result[i] = vector.getDataAt(j);
        }
        return RDataFactory.createDoubleVector(result, vector.isComplete());
    }

    private static RVector innerLoopString(RAbstractStringVector vector, int firstDim) {
        String[] result = new String[vector.getLength()];
        int j = 0;
        for (int i = 0; i < result.length; i++, j += firstDim) {
            if (j > (result.length - 1)) {
                j -= (result.length - 1);
            }
            result[i] = vector.getDataAt(j);
        }
        return RDataFactory.createStringVector(result, vector.isComplete());
    }

    @Specialization(guards = "!isEmpty2D(vector)")
    protected RVector transpose(RAbstractIntVector vector) {
        return transposeInternal(vector, Transpose::innerLoopInt);
    }

    @Specialization(guards = "!isEmpty2D(vector)")
    protected RVector transpose(RAbstractDoubleVector vector) {
        return transposeInternal(vector, Transpose::innerLoopDouble);
    }

    @Specialization(guards = "!isEmpty2D(vector)")
    protected RVector transpose(RAbstractStringVector vector) {
        return transposeInternal(vector, Transpose::innerLoopString);
    }

    protected static boolean isEmpty2D(RAbstractVector vector) {
        if (!vector.hasDimensions()) {
            return false;
        }
        return vector.getDimensions().length == 2 && vector.getLength() == 0;
    }
}
