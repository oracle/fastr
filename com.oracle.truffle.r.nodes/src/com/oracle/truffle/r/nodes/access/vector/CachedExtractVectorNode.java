/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.access.vector;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.nodes.access.vector.PositionsCheckNode.PositionProfile;
import com.oracle.truffle.r.nodes.access.vector.CachedExtractVectorNodeFactory.ExtractDimNamesNodeGen;
import com.oracle.truffle.r.nodes.access.vector.CachedExtractVectorNodeFactory.SetNamesNodeGen;
import com.oracle.truffle.r.nodes.attributes.GetFixedAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SetFixedAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetDimNamesAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetNamesAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.SetDimAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.SetDimNamesAttributeNode;
import com.oracle.truffle.r.nodes.binary.BoxPrimitiveNode;
import com.oracle.truffle.r.nodes.profile.AlwaysOnBranchProfile;
import com.oracle.truffle.r.nodes.profile.VectorLengthProfile;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.RAttributesLayout;
import com.oracle.truffle.r.runtime.data.RBaseObject;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RLogical;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RString;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

final class CachedExtractVectorNode extends CachedVectorNode {

    private static final boolean DEFAULT_EXACT = true;
    private static final boolean DEFAULT_DROP_DIMENSION = true;

    private final Class<? extends RAbstractContainer> targetClass;
    private final Class<? extends RBaseObject> exactClass;
    private final Class<? extends RBaseObject> dropDimensionsClass;
    private final boolean exact;
    private final boolean dropDimensions;

    private final VectorLengthProfile vectorLengthProfile = VectorLengthProfile.create();

    @Child private WriteIndexedVectorNode writeVectorNode;
    @Child private PositionsCheckNode positionsCheckNode;
    @Child private SetNamesNode setNamesNode;
    @Child private SetDimAttributeNode setDimNode;
    @Child private SetDimNamesAttributeNode setDimNamesNode;
    @Child private GetDimNamesAttributeNode getDimNamesNode;
    @Child private GetNamesAttributeNode getNamesNode;
    @Child private GetNamesAttributeNode getNamesFromDimNamesNode;
    @Child private BoxPrimitiveNode boxOldDimNames;
    @Child private BoxPrimitiveNode boxNewDimName;
    @Children private final CachedExtractVectorNode[] extractNames;
    @Children private final CachedExtractVectorNode[] extractNamesAlternative;

    @Child private GetFixedAttributeNode getSrcrefNode;
    @Child private CachedExtractVectorNode extractSrcrefNode;
    @Child private SetFixedAttributeNode setSrcrefNode;

    @Child private ExtractDimNamesNode extractDimNames;

    @CompilationFinal private ConditionProfile resultHasDimensions;

    private final ConditionProfile droppedDimensionProfile;

    /**
     * Profile if any metadata was applied at any point in time. This is useful extract primitive
     * values from the result in case no metadata was ever applied.
     */
    @CompilationFinal private AlwaysOnBranchProfile metadataApplied;

    private final ConditionProfile extractedLengthGTZeroProfile;
    private final ConditionProfile oneDimensionProfile;

    CachedExtractVectorNode(ElementAccessMode mode, RAbstractContainer vector, Object[] positions, RBaseObject exact, RBaseObject dropDimensions, boolean recursive) {
        super(mode, vector, positions, recursive);
        assert vectorType != RType.Null && vectorType != RType.Environment;
        this.targetClass = vector.getClass();
        this.exactClass = exact.getClass();
        this.dropDimensionsClass = dropDimensions.getClass();
        Object[] convertedPositions = filterPositions(positions);
        this.extractNames = new CachedExtractVectorNode[convertedPositions.length];
        this.extractNamesAlternative = new CachedExtractVectorNode[convertedPositions.length];
        this.exact = logicalAsBoolean(exact, DEFAULT_EXACT);
        this.dropDimensions = logicalAsBoolean(dropDimensions, DEFAULT_DROP_DIMENSION);
        this.positionsCheckNode = new PositionsCheckNode(mode, vectorType, convertedPositions, this.exact, false, recursive);
        this.writeVectorNode = WriteIndexedVectorNode.create(vectorType, convertedPositions.length, false, false);
        this.droppedDimensionProfile = this.dropDimensions ? ConditionProfile.createBinaryProfile() : null;
        this.extractedLengthGTZeroProfile = mode.isSubset() ? ConditionProfile.createBinaryProfile() : null;
        this.oneDimensionProfile = mode.isSubset() ? ConditionProfile.createBinaryProfile() : null;
    }

    public boolean isSupported(Object target, Object[] positions, Object exactValue, Object dropDimensionsValue) {
        if (targetClass == target.getClass() && exactValue.getClass() == this.exactClass && dropDimensionsValue.getClass() == dropDimensionsClass //
                        && logicalAsBoolean(dropDimensionsClass.cast(dropDimensionsValue), DEFAULT_DROP_DIMENSION) == this.dropDimensions //
                        && logicalAsBoolean(exactClass.cast(exactValue), DEFAULT_EXACT) == this.exact) {
            return positionsCheckNode.isSupported(positions);
        }
        return false;
    }

    public Object apply(RAbstractContainer originalVector, Object[] originalPositions, PositionProfile[] originalProfiles, Object originalExact, Object originalDropDimensions) {
        final Object[] positions = filterPositions(originalPositions);
        assert isSupported(originalVector, positions, originalExact, originalDropDimensions);
        RAbstractContainer vector = targetClass.cast(originalVector);

        int vectorLength = vectorLengthProfile.profile(vector.getLength());

        int[] dimensions = getDimensions(vector);

        PositionProfile[] positionProfiles;
        if (originalProfiles == null) {
            positionProfiles = positionsCheckNode.executeCheck(vector, dimensions, vectorLength, positions);
        } else {
            positionProfiles = originalProfiles;
        }

        if (isMissingSingleDimension()) {
            // subset: special case for x<-matrix(1:4, ncol=2); x[]
            // subscript: special case for l<-list(1,2,3); l[[]]
            return mode.isSubset() ? originalVector : RNull.instance;
        }

        int extractedVectorLength = positionsCheckNode.getSelectedPositionsCount(positionProfiles);
        RAbstractVector extractedVector;
        switch (vectorType) {
            case Language:
            case PairList:
                extractedVector = RType.List.create(extractedVectorLength, false);
                break;
            default:
                extractedVector = vectorType.create(extractedVectorLength, false);
                break;
        }

        if (mode.isSubset()) {
            if (extractedLengthGTZeroProfile.profile(extractedVectorLength > 0)) {
                writeVectorNode.execute(extractedVector, positions, vector, dimensions);
                RBaseNode.reportWork(this, extractedVectorLength);
            }
            if (oneDimensionProfile.profile(numberOfDimensions == 1)) {
                // names and srcref only need to be considered for single dimensional accesses
                if (getNamesNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    getNamesNode = insert(GetNamesAttributeNode.create());
                }
                RStringVector originalNames = getNamesNode.getNames(vector);
                if (originalNames != null) {
                    getMetadataApplied().enter();
                    setNames(extractedVector, extractNames(originalNames, positions, positionProfiles, 0, originalExact, originalDropDimensions));
                }

                Object srcref = getSrcref(vector);
                if (srcref instanceof RList) {
                    getMetadataApplied().enter();
                    setSrcref(extractedVector, extractSrcref(((RList) srcref), positions, positionProfiles, originalExact, originalDropDimensions));
                }
            } else {
                assert numberOfDimensions > 1;
                applyDimensions(vector, extractedVector, extractedVectorLength, positionProfiles, positions);
            }

            switch (vectorType) {
                case Language:
                case PairList:
                    return ((RPairList) originalVector).isLanguage() ? RPairList.asPairList(extractedVector, ((RPairList) originalVector).getType()) : extractedVector;
                default:
                    return trySubsetPrimitive(extractedVector);
            }
        } else {
            writeVectorNode.execute(extractedVector, positions, vector, dimensions);
            RBaseNode.reportWork(this, 1);
            assert extractedVectorLength == 1;
            return extractedVector.getDataAtAsObject(0);
        }
    }

    private int[] getDimensions(final RAbstractContainer vector) {
        int[] dimensions;
        if (numberOfDimensions == 1) {
            dimensions = null;
        } else {
            dimensions = loadVectorDimensions(vector);
        }
        return dimensions;
    }

    private Object trySubsetPrimitive(RAbstractVector extractedVector) {
        if (!getMetadataApplied().isVisited() && positionsCheckNode.getCachedSelectedPositionsCount() == 1 && vectorType != RType.List && vectorType != RType.Expression) {
            /*
             * If the selected count was always 1 and no metadata was ever set we can just extract
             * the primitive value from the vector. This branch has to fold to a constant because we
             * want to avoid the toggling of the return types depending on input values.
             */
            assert extractedVector.getNames() == null;
            assert extractedVector.getDimensions() == null;
            assert extractedVector.getDimNames() == null;
            return extractedVector.getDataAtAsObject(0);
        }
        return extractedVector;
    }

    private boolean isMissingSingleDimension() {
        return numberOfDimensions == 1 && positionsCheckNode.isMissing();
    }

    private Object extract(int dimensionIndex, RAbstractStringVector vector, Object pos, PositionProfile profile) {
        if (extractDimNames == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            extractDimNames = insert(ExtractDimNamesNodeGen.create(numberOfDimensions));
        }
        return extractDimNames.extract(dimensionIndex, vector, pos, profile);
    }

    private final ConditionProfile dimNamesNull = ConditionProfile.createBinaryProfile();
    private final ValueProfile foundDimNamesProfile = ValueProfile.createClassProfile();
    private final ConditionProfile selectPositionsProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile originalDimNamesPRofile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile foundNamesProfile = ConditionProfile.createBinaryProfile();

    @ExplodeLoop
    private void applyDimensions(RAbstractContainer originalTarget, RAbstractVector extractedTarget, int extractedTargetLength, PositionProfile[] positionProfile, Object[] positions) {
        // TODO speculate on the number of counted dimensions
        int dimCount = countDimensions(positionProfile);

        if (getDimNamesNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            getDimNamesNode = insert(GetDimNamesAttributeNode.create());
        }

        int[] newDimensions = new int[dimCount];
        RList originalDimNames = getDimNamesNode.getDimNames(originalTarget);
        RStringVector originalDimNamesNames;
        Object[] newDimNames;
        String[] newDimNamesNames;
        if (dimNamesNull.profile(originalDimNames == null)) {
            newDimNames = null;
            originalDimNamesNames = null;
            newDimNamesNames = null;
        } else {
            if (getNamesFromDimNamesNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getNamesFromDimNamesNode = insert(GetNamesAttributeNode.create());
            }
            newDimNames = new Object[dimCount];
            originalDimNamesNames = getNamesFromDimNamesNode.getNames(originalDimNames);
            newDimNamesNames = originalDimNamesNames == null ? null : new String[dimCount];
        }

        int dimIndex = -1;
        for (int i = 0; i < numberOfDimensions; i++) {
            int selectedPositionsCount = positionProfile[i].selectedPositionsCount;
            if (!dropDimensions || selectPositionsProfile.profile(selectedPositionsCount != 1)) {
                dimIndex++;
                newDimensions[dimIndex] = selectedPositionsCount;
                if (newDimNames != null) {
                    Object dataAt = originalDimNames.getDataAt(i);
                    Object result;
                    if (dataAt == RNull.instance) {
                        result = RNull.instance;
                    } else if (positionsCheckNode.isEmptyPosition(i, positions[i])) {
                        result = RNull.instance;
                    } else {
                        if (boxOldDimNames == null) {
                            CompilerDirectives.transferToInterpreterAndInvalidate();
                            boxOldDimNames = insert(BoxPrimitiveNode.create());
                        }
                        if (boxNewDimName == null) {
                            CompilerDirectives.transferToInterpreterAndInvalidate();
                            boxNewDimName = insert(BoxPrimitiveNode.create());
                        }
                        RAbstractStringVector originalDimName = (RAbstractStringVector) boxOldDimNames.execute(dataAt);
                        RAbstractStringVector newDimName = (RAbstractStringVector) boxNewDimName.execute(extract(i, originalDimName, positions[i], positionProfile[i]));
                        result = newDimName.materialize();
                    }
                    newDimNames[dimIndex] = result;
                    if (newDimNamesNames != null) {
                        newDimNamesNames[dimIndex] = originalDimNamesNames.getDataAt(i);
                    }
                }
            }
        }

        if (resultHasDimensions == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            resultHasDimensions = ConditionProfile.createBinaryProfile();
        }
        if (resultHasDimensions.profile(dimCount > 1)) {
            getMetadataApplied().enter();

            if (setDimNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                setDimNode = insert(SetDimAttributeNode.create());
            }

            setDimNode.setDimensions(extractedTarget, newDimensions);
            if (newDimNames != null) {
                if (setDimNamesNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    setDimNamesNode = insert(SetDimNamesAttributeNode.create());
                }
                setDimNamesNode.setDimNames(extractedTarget,
                                RDataFactory.createList(newDimNames, newDimNamesNames == null ? null : RDataFactory.createStringVector(newDimNamesNames, originalDimNames.isComplete())));
            }
        } else if (newDimNames != null && originalDimNamesPRofile.profile(originalDimNames.getLength() > 0)) {
            RAbstractStringVector foundNames = translateDimNamesToNames(positionProfile, originalDimNames, extractedTargetLength, positions);
            if (foundNamesProfile.profile(foundNames != null)) {
                foundNames = foundDimNamesProfile.profile(foundNames);
                if (foundNames.getLength() > 0) {
                    getMetadataApplied().enter();
                    setNames(extractedTarget, foundNames);
                }
            }
        }
    }

    @ExplodeLoop
    private int countDimensions(PositionProfile[] boundsProfile) {
        if (dropDimensions) {
            int dimCount = numberOfDimensions;
            for (int i = 0; i < numberOfDimensions; i++) {
                int selectedPositionsCount = boundsProfile[i].selectedPositionsCount;
                if (droppedDimensionProfile.profile(selectedPositionsCount == 1)) {
                    dimCount--;
                }
            }
            return dimCount;
        } else {
            return numberOfDimensions;
        }
    }

    @CompilationFinal private ConditionProfile srcNamesProfile;
    @CompilationFinal private ValueProfile srcNamesValueProfile;
    @CompilationFinal private ConditionProfile newNamesProfile;

    @ExplodeLoop
    private RAbstractStringVector translateDimNamesToNames(PositionProfile[] positionProfile, RList originalDimNames, int newVectorLength, Object[] positions) {
        RAbstractStringVector foundNames = null;
        for (int currentDimIndex = numberOfDimensions - 1; currentDimIndex >= 0; currentDimIndex--) {
            PositionProfile profile = positionProfile[currentDimIndex];
            if (profile.selectedPositionsCount != newVectorLength) {
                continue;
            }

            if (srcNamesProfile == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                srcNamesProfile = ConditionProfile.createBinaryProfile();
            }
            if (srcNamesValueProfile == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                srcNamesValueProfile = ValueProfile.createClassProfile();
            }

            Object srcNames = srcNamesValueProfile.profile(originalDimNames.getDataAt(currentDimIndex));
            if (srcNamesProfile.profile(srcNames != RNull.instance)) {
                Object position = positions[currentDimIndex];

                Object newNames = extractNames((RAbstractStringVector) RRuntime.asAbstractVector(srcNames), new Object[]{position}, new PositionProfile[]{profile}, currentDimIndex,
                                RLogical.valueOf(true), RLogical.valueOf(dropDimensions));
                if (newNames != RNull.instance) {
                    if (newNamesProfile == null) {
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                        newNamesProfile = ConditionProfile.createBinaryProfile();
                    }
                    if (newNamesProfile.profile(newNames instanceof String)) {
                        newNames = RDataFactory.createStringVector((String) newNames);
                    }
                    RAbstractStringVector castFoundNames = (RAbstractStringVector) newNames;
                    if (castFoundNames.getLength() == newVectorLength) {
                        if (foundNames != null) {
                            /*
                             * the idea here is that you can get names from dimnames only if the
                             * name of of an item can be unambiguously identified (there can be only
                             * one matching name in all dimensions - if "name" has already been set,
                             * we might as well return null already)
                             */
                            foundNames = null;
                            break;
                        }
                        foundNames = (RAbstractStringVector) newNames;
                    }
                }
            }
        }
        return foundNames;
    }

    private Object extractNames(RAbstractStringVector originalNames, Object[] positions, PositionProfile[] profiles, int dimension, Object originalExact, Object originalDropDimensions) {
        if (extractNames[dimension] == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            extractNames[dimension] = insert(new CachedExtractVectorNode(mode, originalNames, positions, (RBaseObject) originalExact, (RBaseObject) originalDropDimensions, recursive));
        }

        if (extractNames[dimension].isSupported(originalNames, positions, originalExact, originalDropDimensions)) {
            return extractNames[dimension].apply(originalNames, positions, profiles, originalExact, originalDropDimensions);
        } else {
            // necessary because the positions might change to logical in case of negative indices
            if (extractNamesAlternative[dimension] == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                extractNamesAlternative[dimension] = insert(new CachedExtractVectorNode(mode, originalNames, positions, (RBaseObject) originalExact, (RBaseObject) originalDropDimensions, recursive));
            }
            assert extractNamesAlternative[dimension].isSupported(originalNames, positions, originalExact, originalDropDimensions);
            return extractNamesAlternative[dimension].apply(originalNames, positions, profiles, originalExact, originalDropDimensions);
        }
    }

    private void setNames(RAbstractVector vector, Object newNames) {
        if (setNamesNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            setNamesNode = insert(SetNamesNodeGen.create());
        }
        setNamesNode.execute(vector, newNames);
    }

    protected abstract static class SetNamesNode extends Node {

        @Child private GetFixedAttributeNode namesAttrGetter = GetFixedAttributeNode.createNames();

        public abstract void execute(RAbstractVector container, Object newNames);

        @Specialization
        protected void setNames(RAbstractVector container, RAbstractStringVector newNames) {
            RStringVector newNames1 = newNames.materialize();
            assert newNames1.getLength() <= container.getLength();
            assert container.getAttr(RRuntime.DIM_ATTR_KEY) == null;
            if (container.getAttributes() == null) {
                // usual case
                container.initAttributes(RAttributesLayout.createNames(newNames1));
            } else {
                // from an RPairList extraction that set a name
                RStringVector oldNames = (RStringVector) namesAttrGetter.execute(container);
                assert oldNames.getLength() == newNames.getLength();
                assert oldNames.toString().equals(newNames1.toString());
                // i.e. nothing actually needs to be done
            }
        }

        @Specialization
        protected void setNames(RAbstractVector container, String newNames) {
            // TODO: why materialize()?
            setNames(container, RString.valueOf(newNames).materialize());
        }

        @Specialization
        protected void setNames(RAbstractVector container, @SuppressWarnings("unused") RNull newNames) {
            assert container.getAttributes() == null;
        }
    }

    abstract static class ExtractDimNamesNode extends Node {

        protected final int limit;

        @Child protected ExtractVectorNode fallbackExtractNode;

        ExtractDimNamesNode(int dimensions) {
            // Support at most 2 different kinds of cached extract nodes per dimension.
            limit = DSLConfig.getCacheSize(dimensions * 2);
        }

        protected abstract Object execute(int dimensionIndex, RAbstractStringVector vector, Object position, PositionProfile profile);

        protected boolean isSupported(CachedExtractVectorNode cachedExtractNode, RAbstractStringVector vector, Object position) {
            return cachedExtractNode.isSupported(vector, new Object[]{position}, RLogical.TRUE, RLogical.TRUE);
        }

        protected CachedExtractVectorNode createCached(RAbstractStringVector vector, Object position) {
            return new CachedExtractVectorNode(ElementAccessMode.SUBSET, vector, new Object[]{position}, RLogical.TRUE, RLogical.TRUE, true);
        }

        @Specialization(limit = "limit", guards = {"dimensionIndex == cachedIndex", "isSupported(cachedExtractNode, vector, position)"})
        public Object extractDimNamesCached(int dimensionIndex, RAbstractStringVector vector, Object position, PositionProfile profile,
                        @Cached("createCached(vector, position)") CachedExtractVectorNode cachedExtractNode,
                        @SuppressWarnings("unused") @Cached("dimensionIndex") int cachedIndex) {
            PositionProfile[] profiles = new PositionProfile[]{profile};
            CompilerAsserts.partialEvaluationConstant(dimensionIndex);
            return cachedExtractNode.apply(vector, new Object[]{position}, profiles, RLogical.TRUE, RLogical.TRUE);
        }

        @Specialization(replaces = "extractDimNamesCached")
        public Object extract(int dimensionIndex, RAbstractStringVector vector, Object position, @SuppressWarnings("unused") PositionProfile profile) {
            if (fallbackExtractNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                fallbackExtractNode = insert(ExtractVectorNode.createRecursive(ElementAccessMode.SUBSET));
            }
            CompilerAsserts.partialEvaluationConstant(dimensionIndex);
            Object[] positions = new Object[]{position};
            return fallbackExtractNode.apply(vector, positions, RLogical.TRUE, RLogical.TRUE);
        }
    }

    private RList extractSrcref(RList originalSrcref, Object[] positions, PositionProfile[] profiles, Object originalExact, Object originalDropDimensions) {
        if (extractSrcrefNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            extractSrcrefNode = insert(new CachedExtractVectorNode(mode, originalSrcref, positions, (RBaseObject) originalExact, (RBaseObject) originalDropDimensions, recursive));
        }
        return (RList) extractSrcrefNode.apply(originalSrcref, positions, profiles, originalExact, originalDropDimensions);
    }

    private void setSrcref(RAbstractContainer vector, RList newSrcref) {
        if (setSrcrefNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            setSrcrefNode = insert(SetFixedAttributeNode.create(RRuntime.R_SRCREF));
        }
        setSrcrefNode.setAttr(vector, newSrcref);
    }

    public Object getSrcref(RAbstractContainer vector) {
        if (getSrcrefNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            getSrcrefNode = insert(GetFixedAttributeNode.create(RRuntime.R_SRCREF));
        }
        return getSrcrefNode.execute(vector);
    }

    public AlwaysOnBranchProfile getMetadataApplied() {
        if (metadataApplied == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            metadataApplied = AlwaysOnBranchProfile.create();
        }
        return metadataApplied;
    }
}
