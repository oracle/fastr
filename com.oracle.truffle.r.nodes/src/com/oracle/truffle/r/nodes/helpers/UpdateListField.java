/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.helpers;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.function.opt.ShareObjectNode;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RStringVector;

public abstract class UpdateListField extends ListFieldNodeBase {
    @Child private ShareObjectNode shareObject = ShareObjectNode.create();

    public abstract boolean execute(RList target, String field, Object value);

    @Specialization(limit = "2", guards = {"getNamesNode.getNames(list) == cachedNames", "field == cachedField"})
    boolean doList(RList list, @SuppressWarnings("unused") String field, Object value,
                    @SuppressWarnings("unused") @Cached("list.getNames()") RStringVector cachedNames,
                    @SuppressWarnings("unused") @Cached("field") String cachedField,
                    @Cached("getIndex(cachedNames, field)") int index) {
        assert value != RNull.instance && !(value instanceof RList) && value != null;
        if (index == -1) {
            return false;
        }
        Object sharedValue = value;
        // share only when necessary:
        if (list.getDataAt(index) != value) {
            sharedValue = getShareObjectNode().execute(value);
        }
        list.setElement(index, sharedValue);
        return true;
    }

    @Specialization(replaces = "doList", guards = {"list.getNames() != null"})
    boolean doListDynamic(RList list, String field, Object value) {
        int index = getIndex(getNamesNode.getNames(list), field);
        if (index == -1) {
            return false;
        }
        Object sharedValue = value;
        // share only when necessary:
        if (list.getDataAt(index) != value) {
            sharedValue = getShareObjectNode().execute(value);
        }
        list.setElement(index, sharedValue);
        return true;
    }

    private ShareObjectNode getShareObjectNode() {
        if (shareObject == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            shareObject = insert(ShareObjectNode.create());
        }
        return shareObject;
    }

    public static UpdateListField create() {
        return UpdateListFieldNodeGen.create();
    }
}
