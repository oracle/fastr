/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.ffi;

import com.oracle.truffle.api.nodes.NodeInterface;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

public abstract class RFFIRootNode<T extends NodeInterface> extends RootNode {
    @Child protected T rffiNode;

    @SuppressWarnings("this-escape")
    protected RFFIRootNode(T baseRFFINode) {
        super(RContext.getInstance().getLanguage());
        this.rffiNode = baseRFFINode;
        this.getCallTarget(); // Ensure call target is initialized
    }

    @Override
    public SourceSection getSourceSection() {
        return RSyntaxNode.INTERNAL;
    }
}
