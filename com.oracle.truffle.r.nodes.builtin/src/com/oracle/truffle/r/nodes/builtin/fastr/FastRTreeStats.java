/*
 * Copyright (c) 2014, 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.fastr;

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.control.SequenceNode;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import java.util.List;

public class FastRTreeStats {

    @RBuiltin(name = "fastr.seqlengths", kind = PRIMITIVE, parameterNames = {"func"})
    @RBuiltinComment("Show SequenceNode lengths")
    public abstract static class FastRSeqLengths extends RInvisibleBuiltinNode {
        @Override
        public RNode[] getParameterValues() {
            return new RNode[]{ConstantNode.create(RMissing.instance)};
        }

        @Specialization
        protected Object seqLengths(RFunction function) {
            controlVisibility();
            List<SequenceNode> list = NodeUtil.findAllNodeInstances(function.getTarget().getRootNode(), SequenceNode.class);
            int[] counts = new int[11];
            for (SequenceNode s : list) {
                int l = s.getSequence().length;
                if (l > counts.length - 1) {
                    counts[counts.length - 1]++;
                } else {
                    counts[l]++;
                }
            }
            return RDataFactory.createIntVector(counts, RDataFactory.COMPLETE_VECTOR);
        }

        @Specialization
        protected RNull printTree(@SuppressWarnings("unused") RMissing function) {
            controlVisibility();
            throw RError.error(RError.Message.ARGUMENTS_PASSED_0_1);
        }

        @Fallback
        protected RNull printTree(@SuppressWarnings("unused") Object function) {
            controlVisibility();
            throw RError.error(RError.Message.INVALID_ARGUMENT, "func");
        }

    }

}
