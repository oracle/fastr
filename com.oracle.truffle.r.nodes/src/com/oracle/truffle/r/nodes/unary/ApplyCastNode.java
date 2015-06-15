/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.unary;

import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.env.*;

@NodeInfo(cost = NodeCost.NONE)
public final class ApplyCastNode extends RNode implements RSyntaxNode {

    @Child private CastNode cast;
    @Child private RNode value;

    public ApplyCastNode(CastNode cast, RNode value) {
        this.cast = cast;
        this.value = value;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return cast.execute(value.execute(frame));
    }

    @Override
    public void deparse(RDeparse.State state) {
        ((RSyntaxNode) value).deparse(state);
    }

    public RSyntaxNode substitute(REnvironment env) {
        return new ApplyCastNode(NodeUtil.cloneNode(cast), (RNode) ((RSyntaxNode) value).substitute(env));
    }

    @Override
    public void serialize(RSerialize.State state) {
        ((RSyntaxNode) value).serialize(state);
    }
}
