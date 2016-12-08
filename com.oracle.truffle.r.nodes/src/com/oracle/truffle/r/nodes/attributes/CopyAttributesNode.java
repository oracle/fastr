/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetDimAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetDimNamesAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.SetDimNamesAttributeNode;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RAttributeProfiles;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

/**
 * Copies attributes from two source nodes into one result node.
 *
 * @see UnaryCopyAttributesNode
 * @see CopyOfRegAttributesNode
 */
public abstract class CopyAttributesNode extends RBaseNode {

    private final boolean copyAllAttributes;

    protected final RAttributeProfiles attrLeftProfiles = RAttributeProfiles.create();
    protected final RAttributeProfiles attrRightProfiles = RAttributeProfiles.create();

    @Child protected HasFixedAttributeNode hasDimNode = HasFixedAttributeNode.createDim();
    @Child protected GetDimNamesAttributeNode getDimNamesNode = GetDimNamesAttributeNode.create();

    protected CopyAttributesNode(boolean copyAllAttributes) {
        this.copyAllAttributes = copyAllAttributes;
    }

    public static CopyAttributesNode createCopyAllAttributes() {
        return CopyAttributesNodeGen.create(true);
    }

    public abstract RAbstractVector execute(RAbstractVector target, RAbstractVector left, int leftLength, RAbstractVector right, int rightLength);

    protected boolean containsMetadata(RAbstractVector vector, RAttributeProfiles attrProfiles) {
        return vector instanceof RVector && hasDimNode.execute(vector) || (copyAllAttributes && vector.getAttributes() != null) || getDimNamesNode.getDimNames(vector) != null ||
                        vector.getNames(attrProfiles) != null;
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
    protected RAbstractVector copyNoMetadata(RAbstractVector target, RAbstractVector left, int leftLength, RAbstractVector right, int rightLength) {
        if (LOG) {
            log("copyAttributes: no");
            countNo++;
        }
        return target;
    }

    @Specialization(guards = {"leftLength == rightLength", "containsMetadata(left, attrLeftProfiles) || containsMetadata(right, attrRightProfiles)"})
    protected RAbstractVector copySameLength(RAbstractVector target, RAbstractVector left, @SuppressWarnings("unused") int leftLength, RAbstractVector right,
                    @SuppressWarnings("unused") int rightLength,
                    @Cached("create()") CopyOfRegAttributesNode copyOfRegLeft,
                    @Cached("create()") CopyOfRegAttributesNode copyOfRegRight,
                    @Cached("createBinaryProfile()") ConditionProfile hasAttributes,
                    @Cached("createDim()") RemoveFixedAttributeNode removeDim,
                    @Cached("createDimNames()") RemoveFixedAttributeNode removeDimNames,
                    @Cached("create()") InitAttributesNode initAttributes,
                    @Cached("createNames()") SetFixedAttributeNode putNames,
                    @Cached("createDim()") SetFixedAttributeNode putDim,
                    @Cached("createDimNames()") SetFixedAttributeNode putDimNames,
                    @Cached("create()") BranchProfile leftHasDimensions,
                    @Cached("create()") BranchProfile rightHasDimensions,
                    @Cached("create()") BranchProfile noDimensions,
                    @Cached("createBinaryProfile()") ConditionProfile hasNamesLeft,
                    @Cached("createBinaryProfile()") ConditionProfile hasNamesRight,
                    @Cached("createBinaryProfile()") ConditionProfile hasDimNames,
                    @Cached("create()") GetDimAttributeNode getLeftDimsNode,
                    @Cached("create()") GetDimAttributeNode getRightDimsNode,
                    @Cached("create()") SetDimNamesAttributeNode setDimNamesNode) {
        if (LOG) {
            log("copyAttributes: ==");
            countEquals++;
        }
        RVector<?> result = target.materialize();
        if (copyAllAttributes) {
            if (result != right) {
                copyOfRegRight.execute(right, result);
            }
            if (result != left) {
                copyOfRegLeft.execute(left, result);
            }
        }

        int[] newDimensions = getLeftDimsNode.getDimensions(left);
        if (newDimensions == null) {
            newDimensions = getRightDimsNode.getDimensions(right);
            if (newDimensions == null) {
                noDimensions.enter();
                DynamicObject attributes = result.getAttributes();
                if (hasAttributes.profile(attributes != null)) {
                    removeDim.execute(attributes);
                    removeDimNames.execute(attributes);
                }

                RStringVector vecNames = left.getNames(attrLeftProfiles);
                if (hasNamesLeft.profile(vecNames != null)) {
                    if (result != left) {
                        putNames.execute(initAttributes.execute(result), vecNames);
                        result.setInternalNames(vecNames);
                    }
                    return result;
                }
                if (result != right) {
                    vecNames = right.getNames(attrRightProfiles);
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

        if (result != left) {
            RList newDimNames = getDimNamesNode.getDimNames(left);
            if (hasDimNames.profile(newDimNames != null)) {
                putDimNames.execute(result.getAttributes(), newDimNames);

                newDimNames.elementNamePrefix = RRuntime.DIMNAMES_LIST_ELEMENT_NAME_PREFIX;
                return result;
            }
            if (result != right) {
                newDimNames = getDimNamesNode.getDimNames(right);
                if (hasDimNames.profile(newDimNames != null)) {
                    setDimNamesNode.setDimNames(result, newDimNames);
                }
            }
        }
        return result;
    }

    @Specialization(guards = {"leftLength < rightLength", "containsMetadata(left, attrLeftProfiles) || containsMetadata(right, attrRightProfiles)"})
    protected RAbstractVector copyShorter(RAbstractVector target, RAbstractVector left, @SuppressWarnings("unused") int leftLength, RAbstractVector right, @SuppressWarnings("unused") int rightLength, //
                    @Cached("create()") CopyOfRegAttributesNode copyOfReg, //
                    @Cached("createBinaryProfile()") ConditionProfile rightNotResultProfile, //
                    @Cached("create()") BranchProfile leftHasDimensions, //
                    @Cached("create()") BranchProfile rightHasDimensions, //
                    @Cached("create()") BranchProfile noDimensions, //
                    @Cached("createNames()") SetFixedAttributeNode putNames, //
                    @Cached("createDim()") SetFixedAttributeNode putDim, //
                    @Cached("create()") InitAttributesNode initAttributes, //
                    @Cached("createBinaryProfile()") ConditionProfile hasNames, //
                    @Cached("createBinaryProfile()") ConditionProfile hasDimNames,
                    @Cached("create()") GetDimAttributeNode getLeftDimsNode,
                    @Cached("create()") GetDimAttributeNode getRightDimsNode,
                    @Cached("create()") SetDimNamesAttributeNode setDimNamesNode) {
        if (LOG) {
            log("copyAttributes: <");
            countSmaller++;
        }
        boolean rightNotResult = rightNotResultProfile.profile(right != target);
        RVector<?> result = target.materialize();
        if (copyAllAttributes && rightNotResult) {
            copyOfReg.execute(right, result);
        }

        int[] newDimensions = getLeftDimsNode.getDimensions(left);
        if (newDimensions == null || (newDimensions.length == 2 && newDimensions[0] == 1 && newDimensions[1] == 1)) {
            // 1-element matrix should be treated as 1-element vector
            newDimensions = getRightDimsNode.getDimensions(right);
            if (newDimensions == null || (newDimensions.length == 2 && newDimensions[0] == 1 && newDimensions[1] == 1)) {
                // 1-element matrix should be treated as 1-element vector
                noDimensions.enter();
                if (rightNotResult) {
                    RStringVector vecNames = right.getNames(attrRightProfiles);
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

        RVector.verifyDimensions(result.getLength(), newDimensions, this);
        putDim.execute(initAttributes.execute(result), RDataFactory.createIntVector(newDimensions, RDataFactory.COMPLETE_VECTOR));
        if (rightNotResult) {
            RList newDimNames = getDimNamesNode.getDimNames(right);
            if (hasDimNames.profile(newDimNames != null)) {
                setDimNamesNode.setDimNames(result, newDimNames);
            }
        }
        return result;
    }

    @Specialization(guards = {"leftLength > rightLength", "containsMetadata(left, attrLeftProfiles) || containsMetadata(right, attrRightProfiles)"})
    protected RAbstractVector copyLonger(RAbstractVector target, RAbstractVector left, @SuppressWarnings("unused") int leftLength, RAbstractVector right, @SuppressWarnings("unused") int rightLength, //
                    @Cached("create()") CopyOfRegAttributesNode copyOfReg, //
                    @Cached("create()") BranchProfile leftHasDimensions, //
                    @Cached("create()") BranchProfile rightHasDimensions, //
                    @Cached("create()") BranchProfile noDimensions, //
                    @Cached("createNames()") SetFixedAttributeNode putNames, //
                    @Cached("createDim()") SetFixedAttributeNode putDim, //
                    @Cached("create()") InitAttributesNode initAttributes, //
                    @Cached("createBinaryProfile()") ConditionProfile hasNames, //
                    @Cached("createBinaryProfile()") ConditionProfile hasDimNames,
                    @Cached("create()") GetDimAttributeNode getLeftDimsNode,
                    @Cached("create()") GetDimAttributeNode getRightDimsNode,
                    @Cached("create()") SetDimNamesAttributeNode setDimNamesNode) {
        if (LOG) {
            log("copyAttributes: >");
            countLarger++;
        }
        RVector<?> result = target.materialize();
        if (copyAllAttributes && result != left) {
            copyOfReg.execute(left, result);
        }
        int[] newDimensions = getLeftDimsNode.getDimensions(left);
        if (newDimensions == null) {
            newDimensions = getRightDimsNode.getDimensions(right);
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
        if (left != result) {
            RList newDimNames = getDimNamesNode.getDimNames(left);
            if (hasDimNames.profile(newDimNames != null)) {
                setDimNamesNode.setDimNames(result, newDimNames);
            }
        }
        return result;
    }
}
