/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.data.RShareable;
import com.oracle.truffle.r.runtime.data.RTypedValue;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

@NodeChild(value = "n", type = RNode.class)
public abstract class GetNonSharedNode extends RNode implements RSyntaxNode {
    // TODO This should not be an RSyntaxNode

    private final ValueProfile shareableTypeProfile = ValueProfile.createClassProfile();

    protected abstract RNode getN();

    @Override
    protected RSyntaxNode getRSyntaxNode() {
        return getN().asRSyntaxNode();
    }

    @Specialization
    protected RTypedValue getNonShared(RShareable shareable) {
        return shareableTypeProfile.profile(shareable).getNonShared();
    }

    protected static boolean isRShareable(Object o) {
        return o instanceof RShareable;
    }

    @Specialization(guards = "!isRShareable(o)")
    protected Object getNonShared(Object o) {
        return o;
    }

    @Override
    public void setSourceSection(SourceSection sourceSection) {
        throw RInternalError.shouldNotReachHere();
    }

    @Override
    public SourceSection getLazySourceSection() {
        return RSyntaxNode.INTERNAL;
    }

    @Override
    public SourceSection getSourceSection() {
        return RSyntaxNode.INTERNAL;
    }
}
