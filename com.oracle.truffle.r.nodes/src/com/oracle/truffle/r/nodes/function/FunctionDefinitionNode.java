/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.control.*;
import com.oracle.truffle.r.runtime.*;

public final class FunctionDefinitionNode extends RRootNode {

    /**
     * The "parent" of this environment instance is the lexically enclosing environment when the
     * function was defined.
     */
    private final REnvironment.FunctionDefinition descriptor;
    private final RNode uninitializedBody;
    @Child private RNode body;
    private final String description;

    /**
     * An instance of this node may be called from a global scope with the intention to have its
     * execution leave a footprint behind in that scope, e.g., during library loading. In that case,
     * {@code forGlobal} is {@code true}, and the {@link #execute(VirtualFrame)} method must be
     * invoked with one argument, namely the {@link VirtualFrame} representing the global scope.
     * Execution will then proceed in the context of that frame.
     */
    private final boolean forGlobal;

    public FunctionDefinitionNode(SourceSection src, REnvironment.FunctionDefinition descriptor, RNode body, Object[] parameterNames, String description, boolean forGlobal) {
        super(src, parameterNames, descriptor.getDescriptor());
        this.descriptor = descriptor;
        this.uninitializedBody = NodeUtil.cloneNode(body);
        this.body = body;
        this.description = description;
        this.forGlobal = forGlobal;
    }

    public REnvironment getDescriptor() {
        return descriptor;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        try {
            if (forGlobal) {
                VirtualFrame vf = (VirtualFrame) frame.getArguments()[0];
                Object result = body.execute(vf);
                return result;
            } else {
                return body.execute(frame);
            }
        } catch (ReturnException ex) {
            return ex.getResult();
        }
    }

    @Override
    public String toString() {
        return description;
    }

    @Override
    public boolean isSplittable() {
        // don't bother splitting library-loading nodes
        return !forGlobal;
    }

    @Override
    public RootNode split() {
        return new FunctionDefinitionNode(getSourceSection(), descriptor, NodeUtil.cloneNode(uninitializedBody), getParameterNames(), description, false);
    }

}
