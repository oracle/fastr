/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.access.array;

import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.env.*;

public class PositionsArrayNodeAdapter extends RNode {

    @Children public final RNode[] positions;

    public RNode[] getPositions() {
        return positions;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        RInternalError.shouldNotReachHere();
        return null;
    }

    public Object executePos(VirtualFrame frame, int i) {
        return positions[i].execute(frame);
    }

    @Override
    public void deparse(RDeparse.State state) {
        for (int i = 0; i < positions.length; i++) {
            positions[i].deparse(state);
            if (i != positions.length - 1) {
                state.append(", ");
            }
        }
    }

    @Override
    public void serialize(RSerialize.State state) {
        if (positions.length == 0 || (positions.length == 1 && ConstantNode.isMissing(positions[0]))) {
            state.setPositionsLength(0);
        } else {
            state.setPositionsLength(positions.length);
            for (int i = 0; i < positions.length; i++) {
                state.serializeNodeSetCar(positions[i]);
                if (i != positions.length - 1) {
                    state.openPairList();
                }
            }
            /*
             * N.B. We do not call linkPairList as per other argument lists because the "drop" and
             * "exact" arguments must append to the list and we don't have access to them here.
             */
        }
    }

    public RNode[] substitutePositions(REnvironment env) {
        RNode[] subPositions = new RNode[positions.length];
        for (int i = 0; i < positions.length; i++) {
            subPositions[i] = positions[i].substitute(env);
        }
        return subPositions;
    }

    public PositionsArrayNodeAdapter(RNode[] positions) {
        this.positions = insert(positions);
    }
}
