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

import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.DSLConfig;
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
 *
 * This node does not have @GenerateUncached because its child nodes do not have uncached variants,
 * so we have to managed cached/uncached variant manually.
 */
public final class ResizeContainer extends RBaseNode {
    private final boolean cached;
    @Child private AbstractContainerLibrary containerLib;
    @Child private CopyResizedNamesWithEmpty copyResizedNamesWithEmpty;
    @Child private GetNamesAttributeNode getNamesAttributeNode;
    @Child private SetNamesAttributeNode setNamesAttributeNode;
    @Child private CopyResized copyResized;
    private final ConditionProfile hasNames;

    public static ResizeContainer create() {
        return new ResizeContainer(true);
    }

    public static ResizeContainer getUncached() {
        return new ResizeContainer(false);
    }

    private ResizeContainer(boolean cached) {
        this.cached = cached;
        this.containerLib = cached ? AbstractContainerLibrary.getFactory().createDispatched(DSLConfig.getGenericDataLibraryCacheSize())
                        : AbstractContainerLibrary.getFactory().getUncached();
        this.copyResizedNamesWithEmpty = cached ? CopyResizedNamesWithEmpty.create() : CopyResizedNamesWithEmpty.getUncached();
        this.getNamesAttributeNode = cached ? GetNamesAttributeNode.create() : GetNamesAttributeNode.getUncached();
        this.setNamesAttributeNode = cached ? SetNamesAttributeNode.create() : null;
        this.copyResized = cached ? CopyResized.create() : CopyResized.getUncached();
        this.hasNames = cached ? ConditionProfile.createBinaryProfile() : ConditionProfile.getUncached();
    }

    public RAbstractContainer resize(RAbstractContainer container, int newLen) {
        int len = containerLib.getLength(container);
        if (len == newLen) {
            return container;
        }
        boolean fillWithNA = len < newLen;
        RAbstractVector res = copyResized.execute(container, newLen, fillWithNA);

        RStringVector names = getNamesAttributeNode.getNames(container);
        if (hasNames.profile(names != null)) {
            RStringVector newNames = copyResizedNamesWithEmpty.execute(names, newLen);
            setNames(res, newNames);
        }
        return res;
    }

    private void setNames(RAbstractVector vector, RStringVector names) {
        if (cached) {
            assert setNamesAttributeNode != null;
            setNamesAttributeNode.setNames(vector, names);
        } else {
            vector.setNames(names);
        }
    }
}
