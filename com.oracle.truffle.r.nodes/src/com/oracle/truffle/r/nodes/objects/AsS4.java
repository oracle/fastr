/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995, 1996, 1997  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1995-2014, The R Core Team
 * Copyright (c) 2002-2008, The R Foundation
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.objects;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetClassAttributeNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RShareable;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RTypedValue;

// transcribed from src/main/objects.c
public abstract class AsS4 extends Node {

    public abstract Object executeObject(RAttributable s, boolean flag, int complete);

    private final BranchProfile shareable = BranchProfile.create();
    private final BranchProfile error = BranchProfile.create();
    private final GetClassAttributeNode getClassNode = GetClassAttributeNode.create();

    @Child private GetS4DataSlot getS4DataSlot;

    @Specialization
    protected Object asS4(RAttributable s, boolean flag, int complete) {
        RAttributable obj = s;
        if (flag == obj.isS4()) {
            return obj;
        }
        if (obj instanceof RShareable) {
            shareable.enter();
            RShareable shareableObj = (RShareable) obj;
            obj = (RAttributable) shareableObj.getNonShared();
        }
        if (flag) {
            obj.setS4();
        } else {
            if (complete != 0) {
                if (getS4DataSlot == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    getS4DataSlot = GetS4DataSlotNodeGen.create(RType.Any);

                }
                RTypedValue value = getS4DataSlot.executeObject(obj);
                if (value != RNull.instance && !value.isS4()) {
                    return value;
                } else if (complete == 1) {
                    error.enter();
                    RStringVector classAttr = getClassNode.getClassAttr(obj);
                    throw RError.error(this, RError.Message.CLASS_INVALID_S3, classAttr == null || classAttr.getLength() == 0 ? RRuntime.STRING_NA : classAttr.getDataAt(0));
                } else {
                    return obj;
                }
            }
            obj.unsetS4();
        }
        return obj;
    }
}
