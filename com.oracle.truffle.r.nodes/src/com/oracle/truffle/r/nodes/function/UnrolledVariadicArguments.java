/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.util.IdentityHashMap;

import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.r.runtime.Arguments;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.data.RPromise.Closure;
import com.oracle.truffle.r.runtime.nodes.RNode;

/**
 * This is a simple container class for arguments whose "..." have been unrolled and inserted into
 * the original arguments, as it happens in {@link CallArgumentsNode#executeFlatten(Frame)}.
 */
public final class UnrolledVariadicArguments extends Arguments<RNode> implements UnmatchedArguments {

    private final IdentityHashMap<RNode, Closure> closureCache;

    private UnrolledVariadicArguments(RNode[] arguments, ArgumentsSignature signature, ClosureCache closureCache) {
        super(arguments, signature);
        this.closureCache = closureCache.getContent();
    }

    static UnrolledVariadicArguments create(RNode[] arguments, ArgumentsSignature signature, ClosureCache closureCache) {
        return new UnrolledVariadicArguments(arguments, signature, closureCache);
    }

    @Override
    public IdentityHashMap<RNode, Closure> getContent() {
        return closureCache;
    }
}
