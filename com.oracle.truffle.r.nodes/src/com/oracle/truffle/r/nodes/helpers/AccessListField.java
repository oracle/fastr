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

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.access.vector.ExtractListElement;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RStringVector;

public abstract class AccessListField extends ListFieldNodeBase {
    @Child private ExtractListElement extractListElement = ExtractListElement.create();

    public static AccessListField create() {
        return AccessListFieldNodeGen.create();
    }

    public abstract Object execute(RList list, Object field);

    @Specialization(limit = "2", guards = {"getNamesNode.getNames(list) == cachedNames", "field == cachedField"})
    Object doList(RList list, @SuppressWarnings("unused") String field,
                    @SuppressWarnings("unused") @Cached("list.getNames()") RStringVector cachedNames,
                    @SuppressWarnings("unused") @Cached("field") String cachedField,
                    @Cached("getIndex(cachedNames, field)") int index) {
        if (index == -1) {
            return null;
        }
        return extractListElement.execute(list, index);
    }

    @Specialization(replaces = "doList")
    Object doListDynamic(RList list, String field) {
        int index = getIndex(getNamesNode.getNames(list), field);
        if (index == -1) {
            return null;
        }
        return extractListElement.execute(list, index);
    }
}
