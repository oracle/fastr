/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractListBaseVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess.RandomIterator;

/**
 * Internal node that extracts data under given index from any RAbstractContainer. In the case of
 * RListBase, it also invokes {@link UpdateShareableChildValueNode} on the element before returning
 * it.
 *
 * There are two reasons for why one accesses an element of a list: to peek at it, possibly
 * calculate some values from it, and then forget it. In such case, it is OK to access the element
 * directly through {@link RAbstractListVector#getDataAt(int)} or
 * {@link RAbstractContainer#getDataAtAsObject(int)}. However, if the object is going to be returned
 * to the user either as return value of a built-in, put inside a list, put as an attribute, or its
 * true reference count matters for some other reason, then its reference count must be put into a
 * consistent state, which is done by {@link UpdateShareableChildValueNode}. This node is a
 * convenient wrapper that performs the extraction as well as invocation of
 * {@link UpdateShareableChildValueNode}. See also the documentation of
 * {@link RAbstractListBaseVector}.
 */
@ImportStatic(DSLConfig.class)
@GenerateUncached
public abstract class ExtractListElement extends Node {

    public abstract Object execute(RAbstractContainer container, int index);

    public static ExtractListElement create() {
        return ExtractListElementNodeGen.create();
    }

    @Specialization(guards = "listAccess.supports(list)", limit = "getVectorAccessCacheSize()")
    protected Object doList(RAbstractListBaseVector list, int index,
                    @Cached("create(list)") VectorAccess listAccess,
                    @Cached("create()") UpdateShareableChildValueNode updateStateNode) {
        Object element;
        RandomIterator it = listAccess.randomAccess(list);
        element = listAccess.getListElement(it, index);
        return updateStateNode.updateState(list, element);
    }

    @Specialization(replaces = "doList")
    protected Object doListUncached(RAbstractListBaseVector list, int index,
                    @Cached("create()") UpdateShareableChildValueNode updateStateNode) {
        return doList(list, index, list.slowPathAccess(), updateStateNode);
    }

    @Specialization(guards = "isNotList(container)")
    protected Object doOthers(RAbstractContainer container, int index) {
        return container.getDataAtAsObject(index);
    }

    protected static boolean isNotList(RAbstractContainer x) {
        return !(x instanceof RAbstractListVector);
    }
}
