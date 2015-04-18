/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.access.variables.*;
import com.oracle.truffle.r.nodes.access.variables.ReadVariableNode.ReadKind;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

@NodeChildren({@NodeChild("signature")})
public abstract class CollectArgumentsNode extends ArgumentsNode {

    protected static final int CACHE_LIMIT = 3;

    private final ConditionProfile valueMissingProfile = ConditionProfile.createBinaryProfile();

    public abstract Object[] execute(VirtualFrame frame, ArgumentsSignature signature);

    protected ReadVariableNode[] createReads(ArgumentsSignature signature) {
        ReadVariableNode[] reads = new ReadVariableNode[signature.getLength()];
        for (int i = 0; i < signature.getLength(); i++) {
            reads[i] = ReadVariableNode.create(signature.getName(i), RType.Any, ReadKind.SilentLocal);
        }
        return reads;
    }

    @SuppressWarnings("unused")
    @ExplodeLoop
    @Specialization(limit = "CACHE_LIMIT", guards = {"cachedSignature == signature"})
    protected Object[] combineCached(VirtualFrame frame, ArgumentsSignature signature, @Cached("signature") ArgumentsSignature cachedSignature,
                    @Cached("createReads(signature)") ReadVariableNode[] reads) {
        Object[] result = new Object[reads.length];
        for (int i = 0; i < reads.length; i++) {
            Object value = reads[i].execute(frame);
            result[i] = valueMissingProfile.profile(value == null) ? RMissing.instance : value;
        }
        return result;
    }

    @Specialization
    protected Object[] combine(VirtualFrame frame, ArgumentsSignature signature) {
        return readFromMaterialized(frame.materialize(), signature);
    }

    @TruffleBoundary
    private static Object[] readFromMaterialized(MaterializedFrame frame, ArgumentsSignature signature) {
        Object[] result = new Object[signature.getLength()];
        FrameDescriptor desc = frame.getFrameDescriptor();
        for (int i = 0; i < signature.getLength(); i++) {
            FrameSlot slot = desc.findFrameSlot(signature.getName(i));
            if (slot == null) {
                result[i] = RMissing.instance;
            } else {
                result[i] = frame.getValue(slot);
            }
        }
        return result;
    }
}
