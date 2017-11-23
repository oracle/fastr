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
package com.oracle.truffle.r.nodes.function.call;

import java.util.Objects;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.nodes.function.ArgumentMatcher;
import com.oracle.truffle.r.nodes.function.ArgumentMatcher.MatchPermutation;
import com.oracle.truffle.r.nodes.function.FormalArguments;
import com.oracle.truffle.r.nodes.function.RCallNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RArguments.S3DefaultArguments;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

/**
 * Basically the same as {@link PrepareArguments} but for a specific set of internal generic
 * functions using a vararg parameter but expecting a specific amount of parameters by internally
 * matching them.
 */
public abstract class PrepareMatchInternalArguments extends Node {

    protected static final int CACHE_SIZE = 8;

    protected final RBaseNode callingNode;
    protected final FormalArguments formals;

    protected PrepareMatchInternalArguments(FormalArguments formals, RBaseNode callingNode) {
        this.callingNode = Objects.requireNonNull(callingNode);
        this.formals = Objects.requireNonNull(formals);
    }

    protected MatchPermutation createArguments(ArgumentsSignature supplied) {
        return ArgumentMatcher.matchArguments(supplied, formals.getSignature(), callingNode, null);
    }

    @Specialization(limit = "CACHE_SIZE", guards = {"cachedExplicitArgSignature == explicitArgs.getSignature()"})
    public RArgsValuesAndNames prepare(RArgsValuesAndNames explicitArgs, S3DefaultArguments s3DefaultArguments,
                    @SuppressWarnings("unused") @Cached("explicitArgs.getSignature()") ArgumentsSignature cachedExplicitArgSignature,
                    @Cached("createArguments(cachedExplicitArgSignature)") MatchPermutation permutation) {
        return ArgumentMatcher.matchArgumentsEvaluated(permutation, explicitArgs.getArguments(), s3DefaultArguments, formals);
    }

    @TruffleBoundary
    @Specialization
    public RArgsValuesAndNames prepareGeneric(RArgsValuesAndNames evaluatedArgs, S3DefaultArguments s3DefaultArguments) {
        return ArgumentMatcher.matchArgumentsEvaluated(formals, evaluatedArgs, s3DefaultArguments, callingNode);
    }

    /**
     * Returns the argument values and corresponding signature. The signature represents the
     * original call signature reordered in the same way as the arguments. For s3DefaultArguments
     * motivation see {@link RCallNode#callGroupGeneric}.
     */
    public abstract RArgsValuesAndNames execute(RArgsValuesAndNames evaluatedArgs, S3DefaultArguments s3DefaultArguments);
}
