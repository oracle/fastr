/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.attributes;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleLogger;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.ExtractDimNamesAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetDimAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetDimNamesAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetNamesAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.SetDimNamesAttributeNode;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.RLogger;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RSharingAttributeStorage;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

/**
 * Copies attributes from two source nodes into one result node.
 *
 * @see UnaryCopyAttributesNode
 * @see CopyOfRegAttributesNode
 */
@ImportStatic(DSLConfig.class)
public abstract class CopyAttributesNode extends RBaseNode {

    private final boolean copyAllAttributes;

    @Child protected HasFixedAttributeNode hasDimNode = HasFixedAttributeNode.createDim();
    @Child protected GetDimNamesAttributeNode getDimNamesNode = GetDimNamesAttributeNode.create();
    @Child protected ExtractDimNamesAttributeNode extractDimNamesNode = ExtractDimNamesAttributeNode.create();
    @Child protected GetNamesAttributeNode getNamesNode = GetNamesAttributeNode.create();

    protected CopyAttributesNode(boolean copyAllAttributes) {
        this.copyAllAttributes = copyAllAttributes;
    }

    public static CopyAttributesNode createCopyAllAttributes() {
        return CopyAttributesNodeGen.create(true);
    }

    public abstract RAbstractVector execute(RAbstractVector target, RAbstractVector left, int leftLength, RAbstractVector right, int rightLength);

    protected boolean containsMetadata(VectorDataLibrary library, RAbstractVector vector) {
        return library.isWriteable(vector.getData()) && hasDimNode.execute(vector) ||
                        (copyAllAttributes && vector.getAttributes() != null) ||
                        getDimNamesNode.getDimNames(vector) != null ||
                        getNamesNode.getNames(vector) != null;
    }

    private static int countNo;
    private static int countEquals;
    private static int countSmaller;
    private static int countLarger;
    private static int copyAll;

    private static final TruffleLogger LOGGER = RLogger.getLogger(CopyAttributesNode.class.getName());
    private static final AtomicBoolean jvmShutdownRegistered = new AtomicBoolean(false);

    @SuppressWarnings("unused")
    @TruffleBoundary
    private void log(String format, Object... args) {
        assert LOGGER.isLoggable(Level.FINE);

        if (copyAllAttributes) {
            copyAll++;
        }
        if (jvmShutdownRegistered.compareAndSet(false, true)) {
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    StringBuilder sb = new StringBuilder();
                    sb.append("no: ").append(countNo);
                    sb.append("==: ").append(countEquals);
                    sb.append("<: ").append(countSmaller);
                    sb.append(">: ").append(countLarger);
                    sb.append("all: ").append(copyAll);
                    LOGGER.fine(sb.toString());
                }
            });
        }
        LOGGER.finer(String.format(format, args));
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!containsMetadata(leftLibrary, left)", "!containsMetadata(rightLibrary, right)"}, limit = "getGenericVectorAccessCacheSize()")
    protected RAbstractVector copyNoMetadata(RAbstractVector target, RAbstractVector left, int leftLength, RAbstractVector right, int rightLength,
                    @CachedLibrary("left.getData()") VectorDataLibrary leftLibrary,
                    @CachedLibrary("right.getData()") VectorDataLibrary rightLibrary) {
        if (LOGGER.isLoggable(Level.FINE)) {
            log("copyAttributes: no");
            countNo++;
        }
        return target;
    }

    @Specialization(guards = {"leftLength == rightLength", "containsMetadata(leftLibrary,left) || containsMetadata(rightLibrary,right)"}, limit = "getGenericVectorAccessCacheSize()")
    protected RAbstractVector copySameLength(RAbstractVector target, RAbstractVector left, @SuppressWarnings("unused") int leftLength, RAbstractVector right,
                    @SuppressWarnings("unused") int rightLength,
                    @Cached("create()") CopyOfRegAttributesNode copyOfRegLeft,
                    @Cached("create()") CopyOfRegAttributesNode copyOfRegRight,
                    @Cached("createDim()") RemoveFixedAttributeNode removeDim,
                    @Cached("createDimNames()") RemoveFixedAttributeNode removeDimNames,
                    @Cached("create()") InitAttributesNode initAttributes,
                    @Cached("createNames()") SetFixedPropertyNode putNames,
                    @Cached("createDim()") SetFixedPropertyNode putDim,
                    @Cached("createDimNames()") SetFixedPropertyNode putDimNames,
                    @Cached("create()") BranchProfile leftHasDimensions,
                    @Cached("create()") BranchProfile rightHasDimensions,
                    @Cached("create()") BranchProfile noDimensions,
                    @Cached("createBinaryProfile()") ConditionProfile hasNamesLeft,
                    @Cached("createBinaryProfile()") ConditionProfile hasNamesRight,
                    @Cached("createBinaryProfile()") ConditionProfile hasDimNames,
                    @Cached("create()") GetDimAttributeNode getLeftDimsNode,
                    @Cached("create()") GetDimAttributeNode getRightDimsNode,
                    @Cached("create()") SetDimNamesAttributeNode setDimNamesNode,
                    @SuppressWarnings("unused") @CachedLibrary("left.getData()") VectorDataLibrary leftLibrary,
                    @SuppressWarnings("unused") @CachedLibrary("right.getData()") VectorDataLibrary rightLibrary) {
        if (LOGGER.isLoggable(Level.FINE)) {
            log("copyAttributes: ==");
            countEquals++;
        }
        RAbstractVector result = target.materialize();
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
                removeDim.execute(result);
                removeDimNames.execute(result);
                RStringVector vecNames = getNamesNode.getNames(left);
                if (hasNamesLeft.profile(vecNames != null)) {
                    if (result != left) {
                        putNames.execute(initAttributes.execute(result), vecNames);
                    }
                    return result;
                }
                if (result != right) {
                    vecNames = getNamesNode.getNames(right);
                    if (hasNamesRight.profile(vecNames != null)) {
                        putNames.execute(initAttributes.execute(result), vecNames);
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

        if (result != left || (RSharingAttributeStorage.isShareable(left) && ((RSharingAttributeStorage) left).isTemporary())) {
            RList newDimNames = extractDimNamesNode.execute(left);
            if (hasDimNames.profile(newDimNames != null)) {
                putDimNames.execute(result.getAttributes(), newDimNames);

                return result;
            }
            if (result != right) {
                newDimNames = extractDimNamesNode.execute(right);
                if (hasDimNames.profile(newDimNames != null)) {
                    setDimNamesNode.setDimNames(result, newDimNames);
                }
            }
        }
        return result;
    }

    @Specialization(guards = {"leftLength < rightLength", "containsMetadata(leftLibrary,left) || containsMetadata(rightLibrary,right)"}, limit = "getGenericVectorAccessCacheSize()")
    protected RAbstractVector copyShorter(RAbstractVector target, RAbstractVector left, @SuppressWarnings("unused") int leftLength, RAbstractVector right, @SuppressWarnings("unused") int rightLength,
                    @Cached("create()") CopyOfRegAttributesNode copyOfReg,
                    @Cached("createBinaryProfile()") ConditionProfile rightNotResultProfile,
                    @Cached("create()") BranchProfile leftHasDimensions,
                    @Cached("create()") BranchProfile rightHasDimensions,
                    @Cached("create()") BranchProfile noDimensions,
                    @Cached("createNames()") SetFixedPropertyNode putNames,
                    @Cached("createDim()") SetFixedPropertyNode putDim,
                    @Cached("create()") InitAttributesNode initAttributes,
                    @Cached("createBinaryProfile()") ConditionProfile hasNames,
                    @Cached("createBinaryProfile()") ConditionProfile hasDimNames,
                    @Cached("create()") GetDimAttributeNode getLeftDimsNode,
                    @Cached("create()") GetDimAttributeNode getRightDimsNode,
                    @Cached("create()") SetDimNamesAttributeNode setDimNamesNode,
                    @SuppressWarnings("unused") @CachedLibrary("left.getData()") VectorDataLibrary leftLibrary,
                    @SuppressWarnings("unused") @CachedLibrary("right.getData()") VectorDataLibrary rightLibrary) {
        if (LOGGER.isLoggable(Level.FINE)) {
            log("copyAttributes: <");
            countSmaller++;
        }
        boolean rightNotResult = rightNotResultProfile.profile(right != target);
        RAbstractVector result = target.materialize();
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
                    RStringVector vecNames = getNamesNode.getNames(right);
                    if (hasNames.profile(vecNames != null)) {
                        putNames.execute(initAttributes.execute(result), vecNames);
                    }
                }
                return result;
            } else {
                rightHasDimensions.enter();
            }
        } else {
            leftHasDimensions.enter();
        }

        RAbstractVector.verifyDimensions(result.getLength(), newDimensions, this);
        putDim.execute(initAttributes.execute(result), RDataFactory.createIntVector(newDimensions, RDataFactory.COMPLETE_VECTOR));
        if (rightNotResult) {
            RList newDimNames = extractDimNamesNode.execute(right);
            if (hasDimNames.profile(newDimNames != null)) {
                setDimNamesNode.setDimNames(result, newDimNames);
            }
        }
        return result;
    }

    @Specialization(guards = {"leftLength > rightLength", "containsMetadata(leftLibrary,left) || containsMetadata(rightLibrary,right)"}, limit = "getGenericVectorAccessCacheSize()")
    protected RAbstractVector copyLonger(RAbstractVector target, RAbstractVector left, @SuppressWarnings("unused") int leftLength, RAbstractVector right, @SuppressWarnings("unused") int rightLength,
                    @Cached("create()") CopyOfRegAttributesNode copyOfReg,
                    @Cached("create()") BranchProfile leftHasDimensions,
                    @Cached("create()") BranchProfile rightHasDimensions,
                    @Cached("create()") BranchProfile noDimensions,
                    @Cached("createNames()") SetFixedPropertyNode putNames,
                    @Cached("createDim()") SetFixedPropertyNode putDim,
                    @Cached("create()") InitAttributesNode initAttributes,
                    @Cached("createBinaryProfile()") ConditionProfile hasNames,
                    @Cached("createBinaryProfile()") ConditionProfile hasDimNames,
                    @Cached("create()") GetDimAttributeNode getLeftDimsNode,
                    @Cached("create()") GetDimAttributeNode getRightDimsNode,
                    @Cached("create()") SetDimNamesAttributeNode setDimNamesNode,
                    @SuppressWarnings("unused") @CachedLibrary("left.getData()") VectorDataLibrary leftLibrary,
                    @SuppressWarnings("unused") @CachedLibrary("right.getData()") VectorDataLibrary rightLibrary) {
        if (LOGGER.isLoggable(Level.FINE)) {
            log("copyAttributes: >");
            countLarger++;
        }
        RAbstractVector result = target.materialize();
        if (copyAllAttributes && result != left) {
            copyOfReg.execute(left, result);
        }
        int[] newDimensions = getLeftDimsNode.getDimensions(left);
        if (newDimensions == null) {
            newDimensions = getRightDimsNode.getDimensions(right);
            if (newDimensions == null) {
                noDimensions.enter();
                if (left != result) {
                    RStringVector vecNames = getNamesNode.getNames(left);
                    if (hasNames.profile(vecNames != null)) {
                        putNames.execute(initAttributes.execute(result), vecNames);
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
            RList newDimNames = extractDimNamesNode.execute(left);
            if (hasDimNames.profile(newDimNames != null)) {
                setDimNamesNode.setDimNames(result, newDimNames);
            }
        }
        return result;
    }
}
