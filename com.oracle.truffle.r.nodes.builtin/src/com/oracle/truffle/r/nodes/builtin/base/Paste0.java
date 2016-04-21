/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.data.RList;

/**
 * A straightforward implementation in terms of {@code paste} that doesn't attempt to be more
 * efficient.
 */
@RBuiltin(name = "paste0", kind = INTERNAL, parameterNames = {"list", "collapse"})
public abstract class Paste0 extends RBuiltinNode {

    @Child private Paste pasteNode;

    private Object paste(RList values, Object collapse) {
        if (pasteNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            pasteNode = insert(PasteNodeGen.create(null));
        }
        return pasteNode.executeList(values, "", collapse);
    }

    @Specialization
    protected Object paste0(RList values, Object collapse) {
        controlVisibility();
        return paste(values, collapse);
    }
}
