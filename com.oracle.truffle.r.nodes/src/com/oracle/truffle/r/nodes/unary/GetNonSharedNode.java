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
package com.oracle.truffle.r.nodes.unary;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.data.RSharingAttributeStorage;
import com.oracle.truffle.r.runtime.data.RTypedValue;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

public abstract class GetNonSharedNode extends Node {

    public static final class GetNonSharedSyntaxNode extends RNode implements RSyntaxNode {

        @Child private RNode delegate;
        @Child private GetNonSharedNode nonShared = GetNonSharedNodeGen.create();

        public GetNonSharedSyntaxNode(RNode delegate) {
            this.delegate = delegate;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            return nonShared.execute(delegate.execute(frame));
        }

        @Override
        protected RSyntaxNode getRSyntaxNode() {
            return delegate.asRSyntaxNode();
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

    public abstract Object execute(Object value);

    public static GetNonSharedNode create() {
        return GetNonSharedNodeGen.create();
    }

    @Specialization(guards = "shareable.getClass() == shareableClass")
    protected RTypedValue getNonShared(RSharingAttributeStorage shareable,
                    @Cached("shareable.getClass()") Class<? extends RSharingAttributeStorage> shareableClass) {
        return shareableClass.cast(shareable).getNonShared();
    }

    @Fallback
    protected Object getNonShared(Object o) {
        RSharingAttributeStorage.verify(o);
        return o;
    }
}
