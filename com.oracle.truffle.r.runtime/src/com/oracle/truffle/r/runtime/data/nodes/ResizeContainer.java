/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.data.nodes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.data.AbstractContainerLibrary;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.data.nodes.attributes.SpecialAttributesFunctions.GetNamesAttributeNode;
import com.oracle.truffle.r.runtime.data.nodes.attributes.SpecialAttributesFunctions.SetNamesAttributeNode;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

/**
 * A node that updates length of a vector (as in "length<-" statement). Only names attribute is
 * preserved. If the new length is greater than current length, the new vector is filled with NA.
 */
public abstract class ResizeContainer extends RBaseNode {
    public static ResizeContainer create() {
        return ResizeContainerNodeGen.create();
    }

    public static ResizeContainer getUncached() {
        return ResizeContainerNodeGen.getUncached();
    }

    public abstract RAbstractContainer execute(RAbstractContainer container, int newLen);

    @Specialization(limit = "getGenericDataLibraryCacheSize()")
    public RAbstractContainer resize(RAbstractContainer container, int newLen,
                    @CachedLibrary("container") AbstractContainerLibrary containerLib,
                    @Cached CopyResizedNamesWithEmpty copyResizedNamesWithEmpty,
                    @Cached GetNamesAttributeNode getNamesAttributeNode,
                    @Cached SetNamesAttributeNode setNamesAttributeNode,
                    @Cached CopyResized copyResized,
                    @Cached ConditionProfile hasNames) {
        int len = containerLib.getLength(container);
        if (len == newLen) {
            return container;
        }
        boolean fillWithNA = len < newLen;
        RAbstractVector res = copyResized.execute(container, newLen, fillWithNA);

        RStringVector names = getNamesAttributeNode.getNames(container);
        if (hasNames.profile(names != null)) {
            RStringVector newNames = copyResizedNamesWithEmpty.execute(names, newLen);
            setNamesAttributeNode.setNames(res, newNames);
        }
        return res;
    }
}
