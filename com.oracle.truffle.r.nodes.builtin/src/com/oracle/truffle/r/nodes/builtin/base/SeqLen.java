/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.base;

import com.oracle.truffle.api.dsl.CreateCast;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.utilities.BranchProfile;
import com.oracle.truffle.r.nodes.RNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.unary.CastIntegerNodeFactory;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RIntSequence;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;

import static com.oracle.truffle.api.CompilerDirectives.SlowPath;
import static com.oracle.truffle.r.runtime.RBuiltinKind.PRIMITIVE;

@RBuiltin(name = "seq_len", kind = PRIMITIVE, parameterNames = {"length.out"})
public abstract class SeqLen extends RBuiltinNode {

    private final BranchProfile lengthProblem = BranchProfile.create();

    @CreateCast("arguments")
    public RNode[] createCastValue(RNode[] children) {
        return new RNode[]{CastIntegerNodeFactory.create(children[0], false, false, false)};
    }

    @Specialization
    @SlowPath
    protected RIntSequence seqLen(RAbstractIntVector length) {
        boolean zeroLength = length.getLength() == 0;
        if (zeroLength || length.getLength() > 1) {
            lengthProblem.enter();
            RError.warning(getEncapsulatingSourceSection(), RError.Message.FIRST_ELEMENT_USED, "length.out");
            if (zeroLength) {
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.MUST_BE_COERCIBLE_INTEGER);
            }
        }
        return RDataFactory.createIntSequence(1, 1, length.getDataAt(0));
    }

}
