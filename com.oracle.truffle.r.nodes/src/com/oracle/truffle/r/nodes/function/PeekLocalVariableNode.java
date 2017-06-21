/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.function;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.nodes.access.variables.LocalReadVariableNode;
import com.oracle.truffle.r.nodes.function.visibility.SetVisibilityNode;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.builtins.RSpecialFactory;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxLookup;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

public final class PeekLocalVariableNode extends RNode implements RSyntaxNode, RSyntaxLookup {

    @Child private LocalReadVariableNode read;
    @Child private SetVisibilityNode visibility;

    private final ValueProfile valueProfile = ValueProfile.createClassProfile();

    public PeekLocalVariableNode(String name) {
        this.read = LocalReadVariableNode.create(Utils.intern(name), true);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        Object value = read.execute(frame);
        if (value == null) {
            throw RSpecialFactory.throwFullCallNeeded();
        }
        return valueProfile.profile(value);
    }

    @Override
    public Object visibleExecute(VirtualFrame frame) {
        try {
            return execute(frame);
        } finally {
            if (visibility == null) {
                CompilerDirectives.transferToInterpreter();
                visibility = insert(SetVisibilityNode.create());
            }
            visibility.execute(frame, true);
        }
    }

    @Override
    public void setSourceSection(SourceSection source) {
        // nothing to do
    }

    @Override
    public String getIdentifier() {
        return (String) read.getIdentifier();
    }

    @Override
    public boolean isFunctionLookup() {
        return false;
    }

    @Override
    public SourceSection getSourceSection() {
        return null;
    }

    @Override
    public SourceSection getLazySourceSection() {
        return null;
    }
}
