/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.engine.interop;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.r.engine.TruffleRLanguage;
import com.oracle.truffle.r.nodes.access.vector.ElementAccessMode;
import com.oracle.truffle.r.nodes.access.vector.ExtractVectorNode;
import com.oracle.truffle.r.nodes.builtin.base.InfixEmulationFunctions.AccessArraySubscriptBuiltin;
import com.oracle.truffle.r.nodes.control.RLengthNode;
import com.oracle.truffle.r.runtime.data.RLogical;
import com.oracle.truffle.r.runtime.data.RNull;

abstract class VectorReadNode extends RootNode {

    @CompilationFinal private boolean lengthAccess;
    @Child private AccessArraySubscriptBuiltin builtin;
    @Child private ExtractVectorNode extract = ExtractVectorNode.create(ElementAccessMode.SUBSCRIPT, true);
    @Child private RLengthNode lengthNode = RLengthNode.create();

    VectorReadNode() {
        super(TruffleRLanguage.class, null, null);
        this.lengthAccess = false;
    }

    @Override
    public final Object execute(VirtualFrame frame) {
        Object label = ForeignAccess.getArguments(frame).get(0);
        Object receiver = ForeignAccess.getReceiver(frame);
        return execute(frame, receiver, label);
    }

    protected abstract Object execute(VirtualFrame frame, Object reciever, Object label);

    @Specialization
    protected Object readIndexed(VirtualFrame frame, Object receiver, int label) {
        return extract.apply(frame, receiver, new Object[]{label + 1}, RLogical.TRUE, RLogical.TRUE);
    }

    @Specialization
    protected Object readProperty(VirtualFrame frame, Object receiver, String label) {
        if (label.equals("length")) {
            return lengthNode.executeInteger(frame, receiver);
        } else {
            return RNull.instance;
        }
    }
}
