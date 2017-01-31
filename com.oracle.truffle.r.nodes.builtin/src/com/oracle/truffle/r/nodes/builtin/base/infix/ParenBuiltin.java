/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.base.infix;

import static com.oracle.truffle.r.runtime.RVisibility.ON;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.nodes.RNode;

@NodeInfo(cost = NodeCost.NONE)
final class ParensSpecial extends RNode {

    @Child private RNode delegate;

    protected ParensSpecial(RNode delegate) {
        this.delegate = delegate;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return delegate.execute(frame);
    }
}

@RBuiltin(name = "(", kind = PRIMITIVE, parameterNames = {""}, visibility = ON, behavior = PURE)
public final class ParenBuiltin extends RBuiltinNode {

    static {
        Casts.noCasts(ParenBuiltin.class);
    }

    public static RNode special(ArgumentsSignature signature, RNode[] args, @SuppressWarnings("unused") boolean inReplacement) {
        if (signature == ArgumentsSignature.empty(1)) {
            return new ParensSpecial(args[0]);
        }
        return null;
    }

    @Override
    public Object executeBuiltin(VirtualFrame frame, Object... args) {
        return args[0];
    }
}
