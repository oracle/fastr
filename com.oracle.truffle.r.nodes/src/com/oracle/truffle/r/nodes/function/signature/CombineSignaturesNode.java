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
package com.oracle.truffle.r.nodes.function.signature;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

public abstract class CombineSignaturesNode extends RBaseNode {

    protected static final int CACHE_LIMIT = 3;

    public abstract ArgumentsSignature execute(ArgumentsSignature left, ArgumentsSignature right);

    @SuppressWarnings("unused")
    @Specialization(limit = "CACHE_LIMIT", guards = {"left == leftCached", "right == rightCached"})
    protected ArgumentsSignature combineCached(ArgumentsSignature left, ArgumentsSignature right, @Cached("left") ArgumentsSignature leftCached, @Cached("right") ArgumentsSignature rightCached,
                    @Cached("combine(left, right)") ArgumentsSignature resultCached) {
        return resultCached;
    }

    @Specialization(guards = "left.isEmpty()")
    protected ArgumentsSignature combineLeftEmpty(@SuppressWarnings("unused") ArgumentsSignature left, ArgumentsSignature right) {
        return right;
    }

    @Specialization(guards = "right.isEmpty()")
    protected ArgumentsSignature combineRightEmpty(ArgumentsSignature left, @SuppressWarnings("unused") ArgumentsSignature right) {
        return left;
    }

    @Specialization
    @TruffleBoundary
    protected ArgumentsSignature combine(ArgumentsSignature left, ArgumentsSignature right) {
        String[] names = new String[left.getLength() + right.getLength()];
        for (int i = 0; i < left.getLength(); i++) {
            names[i] = left.getName(i);
        }
        for (int i = 0; i < right.getLength(); i++) {
            names[left.getLength() + i] = right.getName(i);
        }
        return ArgumentsSignature.get(names);
    }
}
