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
package com.oracle.truffle.r.nodes.access.vector;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.profile.VectorLengthProfile;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.REmpty;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;

final class PositionsCheckNode extends Node {

    @Children private final PositionCheckNode[] positionsCheck;

    private final ElementAccessMode mode;
    private final BranchProfile errorBranch = BranchProfile.create();
    private final VectorLengthProfile selectedPositionsCountProfile = VectorLengthProfile.create();
    private final VectorLengthProfile maxOutOfBoundsProfile = VectorLengthProfile.create();
    private final ConditionProfile containsNAProfile = ConditionProfile.createBinaryProfile();
    private final boolean replace;
    private final int positionsLength;

    PositionsCheckNode(ElementAccessMode mode, RType containerType, Object[] positions, boolean exact, boolean replace, boolean recursive) {
        this.mode = mode;
        this.replace = replace;
        this.positionsCheck = new PositionCheckNode[positions.length];
        for (int i = 0; i < positions.length; i++) {
            positionsCheck[i] = PositionCheckNode.createNode(mode, containerType, positions[i], i, positions.length, exact, replace, recursive);
        }
        this.positionsLength = positions.length;
    }

    public PositionCheckNode getPositionCheckAt(int index) {
        return positionsCheck[index];
    }

    @ExplodeLoop
    public boolean isSupported(Object[] positions) {
        if (positionsCheck.length != positions.length) {
            return false;
        }
        for (int i = 0; i < positionsCheck.length; i++) {
            if (!positionsCheck[i].isSupported(positions[i])) {
                return false;
            }
        }
        return true;
    }

    public int getDimensions() {
        return positionsCheck.length;
    }

    public boolean isSingleDimension() {
        return positionsCheck.length == 1;
    }

    public boolean isMultiDimension() {
        return positionsCheck.length > 1;
    }

    @ExplodeLoop
    public PositionProfile[] executeCheck(RAbstractContainer vector, int[] vectorDimensions, int vectorLength, Object[] positions) {
        assert isSupported(positions);
        verifyDimensions(vectorDimensions);

        PositionProfile[] statistics = new PositionProfile[positionsLength];
        for (int i = 0; i < positionsLength; i++) {
            Object position = positions[i];
            PositionProfile profile = new PositionProfile();
            positions[i] = positionsCheck[i].execute(profile, vector, vectorDimensions, vectorLength, position);
            statistics[i] = profile;
        }
        return statistics;
    }

    @TruffleBoundary
    private void print() {
        System.out.println(positionsCheck.length);
    }

    private void verifyDimensions(int[] vectorDimensions) {
        if (vectorDimensions == null) {
            if (isMultiDimension()) {
                errorBranch.enter();
                throw dimensionsError();
            }
        } else {
            if (getDimensions() > vectorDimensions.length || getDimensions() < vectorDimensions.length) {
                errorBranch.enter();
                throw dimensionsError();
            }
        }
    }

    private RError dimensionsError() {
        if (replace) {
            if (mode.isSubset()) {
                if (getDimensions() == 2) {
                    return RError.error(this, RError.Message.INCORRECT_SUBSCRIPTS_MATRIX);
                } else {
                    return RError.error(this, RError.Message.INCORRECT_SUBSCRIPTS);
                }
            } else {
                return RError.error(this, RError.Message.IMPROPER_SUBSCRIPT);
            }
        } else {
            return RError.error(this, RError.Message.INCORRECT_DIMENSIONS);
        }
    }

    @ExplodeLoop
    public int getSelectedPositionsCount(PositionProfile[] profiles) {
        if (positionsCheck.length == 1) {
            return selectedPositionsCountProfile.profile(profiles[0].selectedPositionsCount);
        } else {
            int newSize = 1;
            for (int i = 0; i < positionsCheck.length; i++) {
                newSize *= profiles[i].selectedPositionsCount;
            }
            return selectedPositionsCountProfile.profile(newSize);
        }
    }

    public int getCachedSelectedPositionsCount() {
        return selectedPositionsCountProfile.getCachedLength();
    }

    @ExplodeLoop
    public boolean getContainsNA(PositionProfile[] profiles) {
        if (positionsCheck.length == 1) {
            return containsNAProfile.profile(profiles[0].containsNA);
        } else {
            boolean containsNA = false;
            for (int i = 0; i < positionsCheck.length; i++) {
                containsNA |= profiles[i].containsNA;
            }
            return containsNAProfile.profile(containsNA);
        }
    }

    @ExplodeLoop
    public int getMaxOutOfBounds(PositionProfile[] replacementStatistics) {
        if (positionsCheck.length == 1) {
            return maxOutOfBoundsProfile.profile(replacementStatistics[0].maxOutOfBoundsIndex);
        } else {
            // impossible to be relevant as position check will throw an error in this case.
            return 0;
        }
    }

    public boolean isMissing() {
        return positionsCheck.length == 1 && //
                        (positionsCheck[0].getPositionClass() == RMissing.class || positionsCheck[0].getPositionClass() == REmpty.class || //
                        positionsCheck[0].getPositionClass() == RSymbol.class);
    }

    final class PositionProfile {

        int selectedPositionsCount;

        int maxOutOfBoundsIndex;

        boolean containsNA;

    }
}
