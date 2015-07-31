/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.attributes;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

/**
 * Simple attribute access node that specializes on the position at which the attribute was found
 * last time.
 */
public abstract class CopyAttributesNode extends NodeSA {

    private final boolean copyAllAttributes;

    protected final RAttributeProfiles attrLeftProfiles = RAttributeProfiles.create();
    protected final RAttributeProfiles attrRightProfiles = RAttributeProfiles.create();

    protected CopyAttributesNode(boolean copyAllAttributes) {
        this.copyAllAttributes = copyAllAttributes;
    }

    public abstract RAbstractVector execute(RAbstractVector target, RAbstractVector left, int leftLength, RAbstractVector right, int rightLength);

    protected boolean containsMetadata(RAbstractVector vector, RAttributeProfiles attrProfiles) {
        return vector instanceof RVector && vector.hasDimensions() || (copyAllAttributes && vector.getAttributes() != null) || vector.getNames(attrProfiles) != null ||
                        vector.getDimNames(attrProfiles) != null;
    }

    private static int countNo;
    private static int countEquals;
    private static int countSmaller;
    private static int countLarger;
    private static int copyAll;

    private static final boolean LOG = false;

    static {
        if (LOG) {
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    System.out.println("no: " + countNo);
                    System.out.println("==: " + countEquals);
                    System.out.println("<: " + countSmaller);
                    System.out.println(">: " + countLarger);
                    System.out.println("all: " + copyAll);
                }
            });
        }
    }

    @SuppressWarnings("unused")
    @TruffleBoundary
    private void log(String format, Object... args) {
        if (copyAllAttributes) {
            copyAll++;
        }
        // System.out.println(String.format(format, args));
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!containsMetadata(left, attrLeftProfiles)", "!containsMetadata(right, attrRightProfiles)"})
    public RAbstractVector copyNoMetadata(RAbstractVector target, RAbstractVector left, int leftLength, RAbstractVector right, int rightLength) {
        if (LOG) {
            log("copyAttributes: no");
            countNo++;
        }
        return target;
    }

    @Specialization(guards = {"leftLength == rightLength", "containsMetadata(left, attrLeftProfiles) || containsMetadata(right, attrRightProfiles)"})
    public RAbstractVector copySameLength(RAbstractVector target, RAbstractVector left, @SuppressWarnings("unused") int leftLength, RAbstractVector right, @SuppressWarnings("unused") int rightLength, //
                    @Cached("create()") CopyOfRegAttributesNode copyOfRegLeft, //
                    @Cached("create()") CopyOfRegAttributesNode copyOfRegRight, //
                    @Cached("createDim()") RemoveAttributeNode removeDim, //
                    @Cached("createDimNames()") RemoveAttributeNode removeDimNames, //
                    @Cached("create()") InitAttributesNode initAttributes, //
                    @Cached("createNames()") PutAttributeNode putNames, //
                    @Cached("createDim()") PutAttributeNode putDim, //
                    @Cached("create()") BranchProfile leftHasDimensions, //
                    @Cached("create()") BranchProfile rightHasDimensions, //
                    @Cached("create()") BranchProfile noDimensions, //
                    @Cached("createBinaryProfile()") ConditionProfile hasNamesLeft, //
                    @Cached("createBinaryProfile()") ConditionProfile hasNamesRight, //
                    @Cached("createBinaryProfile()") ConditionProfile hasDimNames) {
        if (LOG) {
            log("copyAttributes: ==");
            countEquals++;
        }
        RVector result = target.materialize();
        if (copyAllAttributes) {
            if (result != right) {
                copyOfRegRight.execute(right, result);
            }
            if (result != left) {
                copyOfRegLeft.execute(left, result);
            }
        }

        int[] newDimensions = left.getDimensions();
        if (newDimensions == null) {
            newDimensions = right.getDimensions();
            if (newDimensions == null) {
                noDimensions.enter();
                RAttributes attributes = result.getAttributes();
                if (attributes != null) {
                    removeDim.execute(attributes);
                    removeDimNames.execute(attributes);
                    result.setInternalDimNames(null);
                }
                result.setInternalDimensions(null);

                if (result != left) {
                    RStringVector vecNames = left.getNames(attrLeftProfiles);
                    if (hasNamesLeft.profile(vecNames != null)) {
                        putNames.execute(initAttributes.execute(result), vecNames);
                        result.setInternalNames(vecNames);
                        return result;
                    }
                }
                if (result != right) {
                    RStringVector vecNames = right.getNames(attrRightProfiles);
                    if (hasNamesRight.profile(vecNames != null)) {
                        putNames.execute(initAttributes.execute(result), vecNames);
                        result.setInternalNames(vecNames);
                    }
                }
                return result;
            } else {
                rightHasDimensions.enter();
            }
        } else {
            leftHasDimensions.enter();
        }

        putDim.execute(initAttributes.execute(result), RDataFactory.createIntVector(newDimensions, RDataFactory.COMPLETE_VECTOR));
        result.setInternalDimensions(newDimensions);

        if (result != left) {
            RList newDimNames = left.getDimNames(attrLeftProfiles);
            if (hasDimNames.profile(newDimNames != null)) {
                result.getAttributes().put(RRuntime.DIMNAMES_ATTR_KEY, newDimNames);
                newDimNames.elementNamePrefix = RRuntime.DIMNAMES_LIST_ELEMENT_NAME_PREFIX;
                result.setInternalDimNames(newDimNames);
                return result;
            }
            if (result != right) {
                newDimNames = right.getDimNames(attrRightProfiles);
                if (hasDimNames.profile(newDimNames != null)) {
                    result.setDimNames(newDimNames);
                }
            }
        }
        return result;
    }

    @TruffleBoundary
    @Specialization(guards = {"leftLength < rightLength", "containsMetadata(left, attrLeftProfiles) || containsMetadata(right, attrRightProfiles)"})
    public RAbstractVector copyShorter(RAbstractVector target, RAbstractVector left, @SuppressWarnings("unused") int leftLength, RAbstractVector right, @SuppressWarnings("unused") int rightLength, //
                    @Cached("createBinaryProfile()") ConditionProfile rightNotResultProfile) {
        if (LOG) {
            log("copyAttributes: <");
            countSmaller++;
        }
        boolean rightNotResult = rightNotResultProfile.profile(right != target);
        RVector result = target.materialize();
        if (copyAllAttributes && rightNotResult) {
            result.copyRegAttributesFrom(right);
        }
        int[] newDimensions;
        if (left.hasDimensions()) {
            newDimensions = left.getDimensions();
        } else {
            newDimensions = right.getDimensions();
        }
        assert result.getDimensions() == null || newDimensions != null;
        if (newDimensions != null) {
            RVector.verifyDimensions(result.getLength(), newDimensions, this);
            result.initAttributes().put(RRuntime.DIM_ATTR_KEY, RDataFactory.createIntVector(newDimensions, RDataFactory.COMPLETE_VECTOR));
            result.setInternalDimensions(newDimensions);
            if (rightNotResult) {
                if (right.getDimNames(attrRightProfiles) != null) {
                    result.setDimNames(right.getDimNames(attrRightProfiles));
                }
                result.copyNamesFrom(attrRightProfiles, right);
            }
        } else {
            if (rightNotResult) {
                RStringVector vecNames = right.getNames(attrRightProfiles);
                if (vecNames != null) {
                    result.setNames(vecNames);
                }
                result.copyNamesFrom(attrRightProfiles, right);
            }
        }
        return result;
    }

    @Specialization(guards = {"leftLength > rightLength", "containsMetadata(left, attrLeftProfiles) || containsMetadata(right, attrRightProfiles)"})
    public RAbstractVector copyLonger(RAbstractVector target, RAbstractVector left, @SuppressWarnings("unused") int leftLength, RAbstractVector right, @SuppressWarnings("unused") int rightLength, //
                    @Cached("create()") CopyOfRegAttributesNode copyOfReg, //
                    @Cached("create()") BranchProfile leftHasDimensions, //
                    @Cached("create()") BranchProfile rightHasDimensions, //
                    @Cached("create()") BranchProfile noDimensions, //
                    @Cached("createNames()") PutAttributeNode putNames, //
                    @Cached("createDim()") PutAttributeNode putDim, //
                    @Cached("create()") InitAttributesNode initAttributes, //
                    @Cached("createBinaryProfile()") ConditionProfile hasNames, //
                    @Cached("createBinaryProfile()") ConditionProfile hasDimNames) {
        if (LOG) {
            log("copyAttributes: >");
            countLarger++;
        }
        RVector result = target.materialize();
        if (copyAllAttributes && result != left) {
            copyOfReg.execute(left, result);
        }
        int[] newDimensions = left.getDimensions();
        if (newDimensions == null) {
            newDimensions = right.getDimensions();
            if (newDimensions == null) {
                noDimensions.enter();
                if (left != result) {
                    RStringVector vecNames = left.getNames(attrLeftProfiles);
                    if (hasNames.profile(vecNames != null)) {
                        putNames.execute(initAttributes.execute(result), vecNames);
                        result.setInternalNames(vecNames);
                    }
                }
                return result;
            } else {
                rightHasDimensions.enter();
            }
        } else {
            leftHasDimensions.enter();
        }
        putDim.execute(initAttributes.execute(result), RDataFactory.createIntVector(newDimensions, RDataFactory.COMPLETE_VECTOR));
        result.setInternalDimensions(newDimensions);
        if (left != result) {
            RList newDimNames = left.getDimNames(attrLeftProfiles);
            if (hasDimNames.profile(newDimNames != null)) {
                result.setDimNames(newDimNames);
            }
        }
        return result;
    }
}
